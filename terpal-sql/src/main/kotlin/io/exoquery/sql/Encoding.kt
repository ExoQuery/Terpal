package io.exoquery.sql

import java.math.BigDecimal
import java.sql.Connection
import java.sql.PreparedStatement
import java.time.*
import kotlin.reflect.KClass

abstract class SqlDecoder<Session, Row, T> {
  abstract val type: KClass<*> // Don't want to force T to be non-nullable so using KClass instead of KClass<T>
  abstract fun decode(ctx: DecodingContext<Session, Row>, index: Int): T
  abstract fun asNullable(): SqlDecoder<Session, Row, T?>

  val id by lazy { Id(type) }
  override fun hashCode(): Int = id.hashCode()
  override fun equals(other: Any?): Boolean = other is SqlDecoder<*, *, *> && other.id == id

  companion object {
    data class Id(val type: KClass<*>)
  }
}

abstract class SqlEncoder<Session, Statement, T> {
  abstract val type: KClass<*>
  abstract fun encode(ctx: EncodingContext<Session, Statement>, value: T, index: Int): Unit
  abstract fun asNullable(): SqlEncoder<Session, Statement, T?>

  // Id should only be based on the type so that SqlDecoders composition works
  val id by lazy { Id(type) }
  override fun hashCode(): Int = id.hashCode()
  override fun equals(other: Any?): Boolean = other is SqlEncoder<*, *, *> && other.id == id

  companion object {
    data class Id(val type: KClass<*>)
  }
}

abstract class SqlDecoders<Session, Row> {
  abstract fun isNull(index: Int, row: Row): Boolean
  abstract fun preview(index: Int, row: Row): String?

  abstract val BooleanDecoder: SqlDecoder<Session, Row, Boolean>
  abstract val ByteDecoder: SqlDecoder<Session, Row, Byte>
  abstract val CharDecoder: SqlDecoder<Session, Row, Char>
  abstract val DoubleDecoder: SqlDecoder<Session, Row, Double>
  abstract val FloatDecoder: SqlDecoder<Session, Row, Float>
  abstract val IntDecoder: SqlDecoder<Session, Row, Int>
  abstract val LongDecoder: SqlDecoder<Session, Row, Long>
  abstract val ShortDecoder: SqlDecoder<Session, Row, Short>
  abstract val StringDecoder: SqlDecoder<Session, Row, String>
  abstract val BigDecimalDecoder: SqlDecoder<Session, Row, BigDecimal>
  abstract val ByteArrayDecoder: SqlDecoder<Session, Row, ByteArray>
  abstract val DateDecoder: SqlDecoder<Session, Row, java.util.Date>

  abstract val LocalDateDecoder: SqlDecoder<Session, Row, LocalDate>
  abstract val LocalTimeDecoder: SqlDecoder<Session, Row, LocalTime>
  abstract val LocalDateTimeDecoder: SqlDecoder<Session, Row, LocalDateTime>
  abstract val ZonedDateTimeDecoder: SqlDecoder<Session, Row, ZonedDateTime>

  abstract val InstantDecoder: SqlDecoder<Session, Row, Instant>
  abstract val OffsetTimeDecoder: SqlDecoder<Session, Row, OffsetTime>
  abstract val OffsetDateTimeDecoder: SqlDecoder<Session, Row, OffsetDateTime>

  open val decoders: Set<SqlDecoder<Session, Row, out Any>> by lazy {
    setOf(
      BooleanDecoder,
      ByteDecoder,
      CharDecoder,
      DoubleDecoder,
      FloatDecoder,
      IntDecoder,
      LongDecoder,
      ShortDecoder,
      StringDecoder,
      BigDecimalDecoder,
      ByteArrayDecoder,
      DateDecoder,
      LocalDateDecoder,
      LocalTimeDecoder,
      LocalDateTimeDecoder,
      ZonedDateTimeDecoder,
      InstantDecoder,
      OffsetTimeDecoder,
      OffsetDateTimeDecoder
    )
  }
}

abstract class SqlEncoders<Session, Stmt> {
  abstract val BooleanEncoder: SqlEncoder<Session, Stmt, Boolean>
  abstract val ByteEncoder: SqlEncoder<Session, Stmt, Byte>
  abstract val CharEncoder: SqlEncoder<Session, Stmt, Char>
  abstract val DoubleEncoder: SqlEncoder<Session, Stmt, Double>
  abstract val FloatEncoder: SqlEncoder<Session, Stmt, Float>
  abstract val IntEncoder: SqlEncoder<Session, Stmt, Int>
  abstract val LongEncoder: SqlEncoder<Session, Stmt, Long>
  abstract val ShortEncoder: SqlEncoder<Session, Stmt, Short>
  abstract val StringEncoder: SqlEncoder<Session, Stmt, String>
  abstract val BigDecimalEncoder: SqlEncoder<Session, Stmt, BigDecimal>
  abstract val ByteArrayEncoder: SqlEncoder<Session, Stmt, ByteArray>
  abstract val DateEncoder: SqlEncoder<Session, Stmt, java.util.Date>

  abstract val LocalDateEncoder: SqlEncoder<Session, Stmt, LocalDate>
  abstract val LocalTimeEncoder: SqlEncoder<Session, Stmt, LocalTime>
  abstract val LocalDateTimeEncoder: SqlEncoder<Session, Stmt, LocalDateTime>
  abstract val ZonedDateTimeEncoder: SqlEncoder<Session, Stmt, ZonedDateTime>

  abstract val InstantEncoder: SqlEncoder<Session, Stmt, Instant>
  abstract val OffsetTimeEncoder: SqlEncoder<Session, Stmt, OffsetTime>
  abstract val OffsetDateTimeEncoder: SqlEncoder<Session, Stmt, OffsetDateTime>

  /** Implement this in the final class/object using computeEncoders() to have a stable list of them */
  open val encoders: Set<SqlEncoder<Session, Stmt, out Any>> by lazy {
    setOf(
      BooleanEncoder,
      ByteEncoder,
      CharEncoder,
      DoubleEncoder,
      FloatEncoder,
      IntEncoder,
      LongEncoder,
      ShortEncoder,
      StringEncoder,
      BigDecimalEncoder,
      ByteArrayEncoder,
      DateEncoder,
      LocalDateEncoder,
      LocalTimeEncoder,
      LocalDateTimeEncoder,
      ZonedDateTimeEncoder,
      InstantEncoder,
      OffsetTimeEncoder,
      OffsetDateTimeEncoder
    )
  }
}
