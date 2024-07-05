package io.exoquery.sql

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

data class Statement(val ir: IR.Splice): SqlFragment {
  operator fun plus(other: Statement) = Statement(IR.Splice(listOf(IR.Part.Empty, IR.Part.Empty, IR.Part.Empty), listOf(this.ir, other.ir)))

  data class QueryData(val sql: String, val params: List<Param<*>>)

  companion object {
    fun constructQuery(ir: IR.Splice): QueryData {
      // At this point we should only have Parts and Params. Otherwise there's some kind of error
      val flatIr = ir.flatten()
      val parts = flatIr.parts
      val params = flatIr.params.map {
        when (it) {
          is IR.Param -> it.value
          else -> throw IllegalStateException("Unexpected IR type in params: $it.\nParams: ${flatIr.params}")
        }
      }

      if (parts.size != params.size + 1)
        throw IllegalStateException(
          """|Mismatched parts (${parts.size})  and params (${params.size}) in query:
             |Parts: ${parts.map { it.value }}
             |Params: ${params.map { it.value }}
        """.trimMargin())

      return QueryData(parts.map { it.value }.joinToString("?"), params)
    }
  }

  inline fun <reified T> queryOf(): Query<T> {
    val (sql, params) = constructQuery(ir)
    val resultMaker = serializer<T>()
    return Query(sql, params, resultMaker)
  }

  fun <T> queryOf(serializer: KSerializer<T>): Query<T> {
    val (sql, params) = constructQuery(ir)
    return Query(sql, params, serializer)
  }

  fun action(): Action {
    val (sql, params) = constructQuery(ir)
    return Action(sql, params)
  }

  inline fun <reified T> actionReturning(vararg returningColumns: String): ActionReturning<T> {
    val (sql, params) = constructQuery(ir)
    val resultMaker = serializer<T>()
    return ActionReturning(sql, params, resultMaker, returningColumns.toList())
  }
}

data class Query<T>(val sql: String, val params: List<Param<*>>, val resultMaker: KSerializer<T>)
data class Action(val sql: String, val params: List<Param<*>>)
data class ActionReturning<T>(val sql: String, val params: List<Param<*>>, val resultMaker: KSerializer<T>, val returningColumns: List<String>)

data class BatchAction(val sql: String, val params: Sequence<List<Param<*>>>)
data class BatchActionReturning<T>(val sql: String, val params: Sequence<List<Param<*>>>, val resultMaker: KSerializer<T>, val returningColumns: List<String>)
