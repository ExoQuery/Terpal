package io.exoquery.sql

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

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