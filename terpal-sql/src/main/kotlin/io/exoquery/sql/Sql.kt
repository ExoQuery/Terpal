package io.exoquery.sql

import io.exoquery.terpal.InterpolatorBatchingWithWrapper
import io.exoquery.terpal.InterpolatorWithWrapper
import io.exoquery.terpal.Messages
import kotlinx.serialization.serializer

interface SqlFragment

data class SqlBatchCallWithValues<A: Any>(internal val batch: SqlBatchCall<A>, internal val values: Sequence<A>) {
  // Note that we don't actually care about the value of element of the batch anymore
  // because the parameters have been prepared and it just needs to be excuted
  // We only need type-data when there is a value returned
  fun action(): BatchAction {
    val sql = batch.parts.joinToString("?")
    val paramSeq = values.map { batch.params(it) }
    return BatchAction(sql, paramSeq)
  }

  fun batchCallValues() = values
  fun batchCall() = batch

  inline fun <reified T> actionReturning(vararg returningColumns: String): BatchActionReturning<T> {
    val sql = batchCall().parts.joinToString("?")
    val paramSeq = batchCallValues().map { batchCall().params(it) }
    val resultMaker = serializer<T>()
    return BatchActionReturning(sql, paramSeq, resultMaker, returningColumns.toList())
  }
}

data class SqlBatchCall<T: Any>(val parts: List<String>, val params: (T) -> List<Param<T>>) {
  fun values(values: Sequence<T>) = SqlBatchCallWithValues(this, values)
  fun values(values: Iterable<T>) = SqlBatchCallWithValues(this, values.asSequence())
  fun values(vararg values: T) = SqlBatchCallWithValues(this, sequenceOf(*values))

  /*
  ----- Optimization -----
  actually what we ant the interface to be is List<(T) -> Fragment> where each param is an element of T
  need to change InterpolatorBatching macro to do that

  make the assumption that all dollar signs in batch queries are params,
  i.e. no Fragements so we don't actually need to flatten anything to make
  that work. Go through the List<(T) -> Fragement> once the macro is updated
  and create a List<Param<T>> from each List<(T) -> Fragment>
  that will give us our List<List<Param<T>> that we can use with the batch query
  }
   */
}

abstract class SqlBatchBase: InterpolatorBatchingWithWrapper<Param<*>> {
  override fun <A : Any> invoke(create: (A) -> String): SqlBatchCall<A> = Messages.throwPluginNotExecuted()
  @Suppress("UNCHECKED_CAST")
  override fun <A : Any> interpolate(parts: () -> List<String>, params: (A) -> List<Param<*>>): SqlBatchCall<A> =
    SqlBatchCall<A>(parts(), params as (A) -> List<Param<A>>)
}

abstract class SqlBase: InterpolatorWithWrapper<SqlFragment, Statement> {
  override fun interpolate(parts: () -> List<String>, params: () -> List<SqlFragment>): Statement {
    val partsList = parts().map { IR.Part(it) }
    val paramsList = params().map {
      when (it) {
        is Param<*> -> IR.Param(it)
        // if it's a statement need to splice everything we've seen in that statement here
        // including the params that we saw in order
        is Statement -> it.ir
        else -> throw IllegalArgumentException("Value $it was not a part or param")
      }
    }

    return Statement(IR.Splice(partsList, paramsList))
  }
}