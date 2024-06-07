package io.exoquery.sql

import java.time.*
import kotlin.reflect.KClass

interface Decoder<Session, Row, T: Any> {
  val type: KClass<T>
  fun decode(session: Session, row: Row, index: Int): T
}

interface Encoder<Session, Statement, T: Any> {
  val type: KClass<T>
  fun encode(session: Session, statement: Statement, value: T, index: Int): Unit
}

abstract class Decoders<Session, Row> {
  abstract fun isNull(index: Int, row: Row): Boolean

  abstract val BooleanDecoder: Decoder<Session, Row, Boolean>
  abstract val ByteDecoder: Decoder<Session, Row, Byte>
  abstract val CharDecoder: Decoder<Session, Row, Char>
  abstract val DoubleDecoder: Decoder<Session, Row, Double>
  abstract val FloatDecoder: Decoder<Session, Row, Float>
  abstract val IntDecoder: Decoder<Session, Row, Int>
  abstract val LongDecoder: Decoder<Session, Row, Long>
  abstract val ShortDecoder: Decoder<Session, Row, Short>
  abstract val StringDecoder: Decoder<Session, Row, String>

  abstract val LocalDateDecoder: Decoder<Session, Row, LocalDate>
  abstract val LocalTimeDecoder: Decoder<Session, Row, LocalTime>
  abstract val LocalDateTimeDecoder: Decoder<Session, Row, LocalDateTime>
  abstract val ZonedDateTimeDecoder: Decoder<Session, Row, ZonedDateTime>

  abstract val InstantDecoder: Decoder<Session, Row, Instant>
  abstract val OffsetTimeDecoder: Decoder<Session, Row, OffsetTime>
  abstract val OffsetDateTimeDecoder: Decoder<Session, Row, OffsetDateTime>

  open val decoders by lazy {
    listOf(
      BooleanDecoder,
      ByteDecoder,
      CharDecoder,
      DoubleDecoder,
      FloatDecoder,
      IntDecoder,
      LongDecoder,
      ShortDecoder,
      StringDecoder,
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

abstract class Encoders<Session, Stmt> {
  abstract val BooleanEncoder: Encoder<Session, Stmt, Boolean>
  abstract val ByteEncoder: Encoder<Session, Stmt, Byte>
  abstract val CharEncoder: Encoder<Session, Stmt, Char>
  abstract val DoubleEncoder: Encoder<Session, Stmt, Double>
  abstract val FloatEncoder: Encoder<Session, Stmt, Float>
  abstract val IntEncoder: Encoder<Session, Stmt, Int>
  abstract val LongEncoder: Encoder<Session, Stmt, Long>
  abstract val ShortEncoder: Encoder<Session, Stmt, Short>
  abstract val StringEncoder: Encoder<Session, Stmt, String>

  abstract val LocalDateEncoder: Encoder<Session, Stmt, LocalDate>
  abstract val LocalTimeEncoder: Encoder<Session, Stmt, LocalTime>
  abstract val LocalDateTimeEncoder: Encoder<Session, Stmt, LocalDateTime>
  abstract val ZonedDateTimeEncoder: Encoder<Session, Stmt, ZonedDateTime>

  abstract val InstantEncoder: Encoder<Session, Stmt, Instant>
  abstract val OffsetTimeEncoder: Encoder<Session, Stmt, OffsetTime>
  abstract val OffsetDateTimeEncoder: Encoder<Session, Stmt, OffsetDateTime>

  val encoders by lazy {
    listOf(
      BooleanEncoder,
      ByteEncoder,
      CharEncoder,
      DoubleEncoder,
      FloatEncoder,
      IntEncoder,
      LongEncoder,
      ShortEncoder,
      StringEncoder,
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
