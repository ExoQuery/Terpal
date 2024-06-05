package io.exoquery.sql

import io.exoquery.terpal.Interpolator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import javax.sql.DataSource
import kotlin.coroutines.coroutineContext

data class Statement(val ir: IR.Splice) {
  operator fun plus(other: Statement) = Statement(IR.Splice(listOf(this.ir, other.ir)))

  data class QueryData(val sql: String, val params: List<Param<*, *, *>>)

  companion object {
    fun constructQuery(irs: IR.Splice): QueryData {
      val partsAccum = mutableListOf<String>()
      val paramsAccum = mutableListOf<Param<*, *, *>>()
      for (ir in irs.values) {
        when (ir) {
          is IR.Part -> partsAccum += ir.value
          is IR.Param -> {
            paramsAccum += ir.value
            partsAccum += "?"
          }
          is IR.Splice -> {
            val (sql, params) = constructQuery(ir)
            partsAccum += sql
            paramsAccum += params
          }
        }
      }
      return QueryData(partsAccum.joinToString(""), paramsAccum)
    }
  }

  inline fun <reified T> queryOf(): Query<T> {
    val (sql, params) = constructQuery(ir)
    val resultMaker = serializer<T>()
    return Query(sql, params, resultMaker)
  }
}

data class Query<T>(val sql: String, val params: List<Param<*, *, *>>, val resultMaker: KSerializer<T>)

sealed interface IR {
  data class Part(val value: String): IR
  data class Param(val value: io.exoquery.sql.Param<*, *, *>): IR
  data class Splice(val values: List<IR>): IR
}


object Sql: Interpolator<Any, Statement> {
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