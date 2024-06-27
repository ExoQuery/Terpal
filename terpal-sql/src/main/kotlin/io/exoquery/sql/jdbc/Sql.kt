package io.exoquery.sql.jdbc

import io.exoquery.sql.*
import java.math.BigDecimal
import java.sql.Connection
import java.sql.PreparedStatement
import java.time.*
import java.time.LocalTime
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

object Sql: SqlJdbcBase()
object SqlBatch: SqlJdbcBatchBase()


// The Jdbc Specific Sql implemenation which will use the Jdbc wrapping functions to auto-wrap things
abstract class SqlJdbcBase(): SqlBase() {
  // TODO Should check this at compile-time
  override fun wrap(value: String?): SqlFragment = Param(value)
  override fun wrap(value: Int?): SqlFragment = Param(value)
  override fun wrap(value: Long?): SqlFragment = Param(value)
  override fun wrap(value: Short?): SqlFragment = Param(value)
  override fun wrap(value: Byte?): SqlFragment = Param(value)
  override fun wrap(value: Float?): SqlFragment = Param(value)
  override fun wrap(value: Double?): SqlFragment = Param(value)
  override fun wrap(value: Boolean?): SqlFragment = Param(value)
  fun wrap(value: BigDecimal?): SqlFragment = Param.contextual(value)
  fun wrap(value: ByteArray?): SqlFragment = Param(value)

  fun wrap(value: java.util.Date?): SqlFragment = Param(value)
  fun wrap(value: java.sql.Date?): SqlFragment = Param(value)
  fun wrap(value: java.sql.Time?): SqlFragment = Param(value)
  fun wrap(value: java.sql.Timestamp?): SqlFragment = Param(value)

  fun wrap(value: LocalDate?): SqlFragment = Param.contextual(value)
  fun wrap(value: LocalTime?): SqlFragment = Param.contextual(value)
  fun wrap(value: LocalDateTime?): SqlFragment = Param.contextual(value)
  fun wrap(value: ZonedDateTime?): SqlFragment = Param.contextual(value)
  fun wrap(value: Instant?): SqlFragment = Param.contextual(value)
  fun wrap(value: OffsetTime?): SqlFragment = Param.contextual(value)
  fun wrap(value: OffsetDateTime?): SqlFragment = Param.contextual(value)
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

