package io.exoquery.sql

import io.exoquery.sql.jdbc.JdbcEncodersWithTime
import io.exoquery.sql.jdbc.JdbcParam
import io.exoquery.terpal.InterpolatorBatchingWithWrapper
import io.exoquery.terpal.InterpolatorWithWrapper
import java.sql.Connection
import java.sql.PreparedStatement
import kotlin.reflect.full.isSubclassOf

sealed interface IR {
  data class Part(val value: String): IR {
    companion object {
      val Empty = Part("")
    }
  }

  sealed interface Var: IR
  data class Param(val value: io.exoquery.sql.Param<*, *, *>): Var
  data class Splice(val parts: List<Part>, val params: List<Var>): Var {
    fun flatten(): Splice {
      val partsAccum = mutableListOf<IR.Part>()
      val paramsAccum = mutableListOf<IR.Var>()

      fun MutableList<IR.Part>.interlaceEdges(other: List<IR.Part>) {
        if (this.isEmpty())
          this.addAll(other)
        else {
          if (other.isNotEmpty()) {
            val last = this.last()
            val firstOther = other.first()
            this.set(this.size-1, IR.Part(last.value + firstOther.value))
            this.addAll(other.drop(1))
          }
        }
      }

      val partsIter = parts.iterator()
      val paramsIter = params.iterator()

      var afterSplice: Boolean = false

      while(partsIter.hasNext()) {

        if (!afterSplice) {
          partsAccum += partsIter.next()
        } else {
          partsAccum.interlaceEdges(listOf(partsIter.next()))
        }

        if (paramsIter.hasNext()) {
          when (val nextParam = paramsIter.next()) {
            is Param -> {
              paramsAccum += nextParam
              afterSplice = false
            }
            // recursively flatten the inner splice, the grab out it's contents
            // for example, Splice("--$A-${Splice("_$B_$C_")}-$D--) at the point of reaching ${Splice(...)}
            // should be: (parts:["--", "-"], params:[A].
            // When the the splice is complete it should be: (parts:["--", ("-" + "_"), "_", "_"], params:[A, B, C])
            is Splice -> {
              partsAccum.interlaceEdges(nextParam.parts)
              paramsAccum.addAll(nextParam.params)
              afterSplice = true
            }
          }
        }
      }

      return IR.Splice(partsAccum, paramsAccum)
    }
  }
}

object Sql: SqlJdbcBase(object: JdbcEncodersWithTime() {})

// The Jdbc Specific Sql implemenation which will use the Jdbc wrapping functions to auto-wrap things
abstract class SqlJdbcBase(val encoders: Encoders<Connection, PreparedStatement>): SqlBase() {
  override fun <V: Any> wrap(value: V): Fragment {
    val cls = value::class
    // Very not idea. We want to be able to just compare KClass instances directly by getting the
    // IrClass/IrClassSymbol as a KClass when the splicing happened
    val encoder =
      encoders.encoders.find { cls.isSubclassOf(it.type) } ?: throw IllegalArgumentException("Could not find an encoder for the type: ${cls.qualifiedName}")
    @Suppress("UNCHECKED_CAST")
    return run {
      JdbcParam<V>(value, encoder as Encoder<Connection, PreparedStatement, V>)
    }
  }
}


interface Fragment

data class SqlBatch<T>(val parts: List<String>, val params: (T) -> List<Fragment>): Fragment {

  /*
  ----- Optimization -----
  actually what we ant the interface to be is List<(T) -> Fragment> where each param is an element of T
  need to change InterpolatorBatching macro to do that

  actually we can't do that because even fi we create a param/fragement that will be able to have a lambda
  we won't actually be able to to flatten that fragment without providing it a runtime value so that optimization
  is not very useful

  }
   */
}

abstract class SqlBatchBase: InterpolatorBatchingWithWrapper<Fragment> {
  override abstract fun <A : Any> invoke(create: (A) -> String): SqlBatch<A>
  override fun <A : Any> interpolate(parts: () -> List<String>, params: (A) -> List<Fragment>): SqlBatch<A> =
    SqlBatch(parts(), params)
}

abstract class SqlBase: InterpolatorWithWrapper<Fragment, Statement> {
  override fun interpolate(parts: () -> List<String>, params: () -> List<Fragment>): Statement {
    val partsList = parts().map { IR.Part(it) }
    val paramsList = params().map {
      when (it) {
        is Param<*, *, *> -> IR.Param(it)
        // if it's a statement need to splice everything we've seen in that statement here
        // including the params that we saw in order
        is Statement -> it.ir
        else -> throw IllegalArgumentException("Value $it was not a part or param")
      }
    }

    return Statement(IR.Splice(partsList, paramsList))
  }
}