package io.exoquery.sql

import io.exoquery.terpal.Interpolator

sealed interface IR {
  data class Part(val value: String): IR
  data class Param(val value: io.exoquery.sql.Param<*, *, *>): IR
  data class Splice(val values: List<IR>): IR
}

// The Jdbc Specific Sql implemenation which will use the Jdbc wrapping functions to auto-wrap things
object Sql: SqlBase()

// TODO Extend InterpolatorWithWrapping
abstract class SqlBase: Interpolator<Any, Statement> {
  override fun interpolate(parts: () -> List<String>, params: () -> List<Any>): Statement {
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