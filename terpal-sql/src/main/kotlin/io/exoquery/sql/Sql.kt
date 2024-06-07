package io.exoquery.sql

import io.exoquery.sql.jdbc.JdbcEncodersBasic
import io.exoquery.sql.jdbc.JdbcEncodersWithTime
import io.exoquery.sql.jdbc.JdbcParam
import io.exoquery.terpal.Interpolator
import io.exoquery.terpal.InterpolatorWithWrapper
import java.sql.Connection
import java.sql.PreparedStatement
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

sealed interface IR {
  data class Part(val value: String): IR
  data class Param(val value: io.exoquery.sql.Param<*, *, *>): IR
  data class Splice(val values: List<IR>): IR
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

// TODO Extend InterpolatorWithWrapping
abstract class SqlBase: InterpolatorWithWrapper<Fragment, Statement> {
  override fun interpolate(parts: () -> List<String>, params: () -> List<Fragment>): Statement {
    val irs = mutableListOf<IR>()

    val partsIter = parts().iterator()
    val paramsIter = params().iterator()

    // We will always have one more part than param
    while (partsIter.hasNext()) {
      irs += IR.Part(partsIter.next())
      // if there is a next-parameter
      if (paramsIter.hasNext()) {
        when (val nextParam = paramsIter.next()) {
          is Param<*, *, *> -> {
            irs += IR.Param(nextParam)
          }
          // if it's a statement need to splice everything we've seen in that statement here
          // including the params that we saw in order
          is Statement -> {
            irs += nextParam.ir
          }
          else -> throw IllegalArgumentException("Value $nextParam was not a part or param")
        }
      }
    }

    return Statement(IR.Splice(irs))
  }
}