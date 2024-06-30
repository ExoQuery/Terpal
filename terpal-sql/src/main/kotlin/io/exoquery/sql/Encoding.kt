package io.exoquery.sql

import java.math.BigDecimal
import java.time.*
import java.util.UUID
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

interface BooleanEncoding<Session, Stmt, Row> {
  val BooleanEncoder: SqlEncoder<Session, Stmt, Boolean>
  val BooleanDecoder: SqlDecoder<Session, Row, Boolean>
}

interface UuidEncoding<Session, Stmt, Row> {
  val UuidEncoder: SqlEncoder<Session, Stmt, UUID>
  val UuidDecoder: SqlDecoder<Session, Row, UUID>
}

// Used by the PreparedStatementEncoder
interface ApiEncoders<Session, Stmt> {
  val BooleanEncoder: SqlEncoder<Session, Stmt, Boolean>
  val ByteEncoder: SqlEncoder<Session, Stmt, Byte>
  val CharEncoder: SqlEncoder<Session, Stmt, Char>
  val DoubleEncoder: SqlEncoder<Session, Stmt, Double>
  val FloatEncoder: SqlEncoder<Session, Stmt, Float>
  val IntEncoder: SqlEncoder<Session, Stmt, Int>
  val LongEncoder: SqlEncoder<Session, Stmt, Long>
  val ShortEncoder: SqlEncoder<Session, Stmt, Short>
  val StringEncoder: SqlEncoder<Session, Stmt, String>
  val BigDecimalEncoder: SqlEncoder<Session, Stmt, BigDecimal>
  val ByteArrayEncoder: SqlEncoder<Session, Stmt, ByteArray>
  val DateEncoder: SqlEncoder<Session, Stmt, java.util.Date>
}
// Used by the RowDecoder
interface ApiDecoders<Session, Row> {
  val BooleanDecoder: SqlDecoder<Session, Row, Boolean>
  val ByteDecoder: SqlDecoder<Session, Row, Byte>
  val CharDecoder: SqlDecoder<Session, Row, Char>
  val DoubleDecoder: SqlDecoder<Session, Row, Double>
  val FloatDecoder: SqlDecoder<Session, Row, Float>
  val IntDecoder: SqlDecoder<Session, Row, Int>
  val LongDecoder: SqlDecoder<Session, Row, Long>
  val ShortDecoder: SqlDecoder<Session, Row, Short>
  val StringDecoder: SqlDecoder<Session, Row, String>
  val BigDecimalDecoder: SqlDecoder<Session, Row, BigDecimal>
  val ByteArrayDecoder: SqlDecoder<Session, Row, ByteArray>
  val DateDecoder: SqlDecoder<Session, Row, java.util.Date>

  abstract fun isNull(index: Int, row: Row): Boolean
  abstract fun preview(index: Int, row: Row): String?
}

interface BasicEncoding<Session, Stmt, Row> {
  val ByteEncoder: SqlEncoder<Session, Stmt, Byte>
  val CharEncoder: SqlEncoder<Session, Stmt, Char>
  val DoubleEncoder: SqlEncoder<Session, Stmt, Double>
  val FloatEncoder: SqlEncoder<Session, Stmt, Float>
  val IntEncoder: SqlEncoder<Session, Stmt, Int>
  val LongEncoder: SqlEncoder<Session, Stmt, Long>
  val ShortEncoder: SqlEncoder<Session, Stmt, Short>
  val StringEncoder: SqlEncoder<Session, Stmt, String>
  val BigDecimalEncoder: SqlEncoder<Session, Stmt, BigDecimal>
  val ByteArrayEncoder: SqlEncoder<Session, Stmt, ByteArray>
  val DateEncoder: SqlEncoder<Session, Stmt, java.util.Date>

  val ByteDecoder: SqlDecoder<Session, Row, Byte>
  val CharDecoder: SqlDecoder<Session, Row, Char>
  val DoubleDecoder: SqlDecoder<Session, Row, Double>
  val FloatDecoder: SqlDecoder<Session, Row, Float>
  val IntDecoder: SqlDecoder<Session, Row, Int>
  val LongDecoder: SqlDecoder<Session, Row, Long>
  val ShortDecoder: SqlDecoder<Session, Row, Short>
  val StringDecoder: SqlDecoder<Session, Row, String>
  val BigDecimalDecoder: SqlDecoder<Session, Row, BigDecimal>
  val ByteArrayDecoder: SqlDecoder<Session, Row, ByteArray>
  val DateDecoder: SqlDecoder<Session, Row, java.util.Date>

  abstract fun isNull(index: Int, row: Row): Boolean
  abstract fun preview(index: Int, row: Row): String?
}

interface TimeEncoding<Session, Stmt, Row> {
  val LocalDateEncoder: SqlEncoder<Session, Stmt, LocalDate>
  val LocalTimeEncoder: SqlEncoder<Session, Stmt, LocalTime>
  val LocalDateTimeEncoder: SqlEncoder<Session, Stmt, LocalDateTime>
  val ZonedDateTimeEncoder: SqlEncoder<Session, Stmt, ZonedDateTime>
  val InstantEncoder: SqlEncoder<Session, Stmt, Instant>
  val OffsetTimeEncoder: SqlEncoder<Session, Stmt, OffsetTime>
  val OffsetDateTimeEncoder: SqlEncoder<Session, Stmt, OffsetDateTime>

  val LocalDateDecoder: SqlDecoder<Session, Row, LocalDate>
  val LocalTimeDecoder: SqlDecoder<Session, Row, LocalTime>
  val LocalDateTimeDecoder: SqlDecoder<Session, Row, LocalDateTime>
  val ZonedDateTimeDecoder: SqlDecoder<Session, Row, ZonedDateTime>
  val InstantDecoder: SqlDecoder<Session, Row, Instant>
  val OffsetTimeDecoder: SqlDecoder<Session, Row, OffsetTime>
  val OffsetDateTimeDecoder: SqlDecoder<Session, Row, OffsetDateTime>
}

interface SqlEncoding<Session, Stmt, Row>:
  BasicEncoding<Session, Stmt, Row>,
  BooleanEncoding<Session, Stmt, Row>,
  TimeEncoding<Session, Stmt, Row>,
  UuidEncoding<Session, Stmt, Row>,
  // Use by the PreparedStatementElementEncoder and the RowDecoder
  ApiEncoders<Session, Stmt>,
  ApiDecoders<Session, Row>{

  fun computeEncoders(): Set<SqlEncoder<Session, Stmt, out Any>> =
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
      OffsetDateTimeEncoder,
      UuidEncoder
    )

  fun computeDecoders(): Set<SqlDecoder<Session, Row, out Any>> =
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
      OffsetDateTimeDecoder,
      UuidDecoder
    )
}

//interface FooFoo {
//  fun xx(): String
//}
//
//interface Foo: FooFoo {
//  fun x(): String
//  fun all() = listOf(x(), xx())
//}
//class FooFooImpl: FooFoo {
//  override fun xx(): String = "FooFooImpl"
//}
//class FooImpl: Foo, FooFoo by FooFooImpl() {
//  override fun x(): String = "FooImpl"
//}
//
//interface Bar {
//  fun y(): String
//  fun all() = listOf(y())
//}
//class BarImpl: Bar {
//  override fun y(): String = "BarImpl"
//}
//interface FooBar: Foo, Bar {
//  override fun all() = listOf(super<Foo>.all(), super<Bar>.all()).flatten()
//}
//
//class FooBarImpl: FooBar, Foo by FooImpl(), Bar by BarImpl() {
//  override fun all() = super<FooBar>.all()
//}








