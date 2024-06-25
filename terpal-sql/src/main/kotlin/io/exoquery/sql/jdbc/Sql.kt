package io.exoquery.sql.jdbc

import io.exoquery.sql.*
import java.sql.Connection
import java.sql.PreparedStatement
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

object Sql: SqlJdbcBase()
object SqlBatch: SqlJdbcBatchBase()


// The Jdbc Specific Sql implemenation which will use the Jdbc wrapping functions to auto-wrap things
abstract class SqlJdbcBase(): SqlBase() {
  // TODO Should check this at compile-time
  override fun <V> wrap(value: V, cls: KClass<*>): SqlFragment =
    when (cls) {
      String::class -> Param(value as String)
      Int::class -> Param(value as Int)
      Long::class -> Param(value as Long)
      Short::class -> Param(value as Short)
      Byte::class -> Param(value as Byte)
      Float::class -> Param(value as Float)
      Double::class -> Param(value as Double)
      Boolean::class -> Param(value as Boolean)
      else ->
        throw IllegalArgumentException(
          """|Attempted to wrap the type ${cls} but wrapped types are only allow to be the primitives: (String, Int, Long, Short, Byte, Float, Double, Boolean)
             |If you are attempint to splice one of these into a Sql string please use the Param(...) constructor on the value first
        """.trimMargin())
    }
}

abstract class SqlJdbcBatchBase(): SqlBatchBase() {
  // TODO Should check this at compile-time
  override fun <V> wrap(value: V, cls: KClass<*>): Param<*> =
    when (cls) {
      String::class -> Param(value as String)
      Int::class -> Param(value as Int)
      Long::class -> Param(value as Long)
      Short::class -> Param(value as Short)
      Byte::class -> Param(value as Byte)
      Float::class -> Param(value as Float)
      Double::class -> Param(value as Double)
      Boolean::class -> Param(value as Boolean)
      else ->
        throw IllegalArgumentException(
          """|Wrapped types are only allow to be the primitives: (String, Int, Long, Short, Byte, Float, Double, Boolean)
             |If you are attempint to splice one of these into a Sql string please use the Param(...) constructor on the value first
        """.trimMargin())
    }
}

