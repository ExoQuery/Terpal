package io.exoquery.sql.jdbc

import io.exoquery.sql.SqlDecoder
import io.exoquery.sql.SqlDecoders
import java.math.BigDecimal
import java.sql.Connection
import java.sql.ResultSet
import java.time.*
import java.util.*

/** Represents a Jdbc Decoder with a nullable or non-nullable output value */
typealias JdbcDecoder<T> = SqlDecoder<Connection, ResultSet, T>

/** Represents a Jdbc Decoder with a non-nullable output value */
abstract class JdbcDecoderAny<T: Any>: JdbcDecoder<T>() {
  inline fun <reified R: Any> map(crossinline f: (T) -> R): JdbcDecoderAny<R> =
    object: JdbcDecoderAny<R>() {
      override val type = R::class
      override fun decode(ctx: JdbcDecodingContext, index: Int) =
        f(this@JdbcDecoderAny.decode(ctx, index))
    }

  override fun asNullable(): SqlDecoder<Connection, ResultSet, T?> =
    object: SqlDecoder<Connection, ResultSet, T?>() {
      override fun asNullable(): SqlDecoder<Connection, ResultSet, T?> = this
      override val type = this@JdbcDecoderAny.type
      override fun decode(ctx: JdbcDecodingContext, index: Int): T? {
        ctx.row.getObject(index)
        return if (ctx.row.wasNull())
          null
        else
          this@JdbcDecoderAny.decode(ctx, index)
      }
    }

  companion object {
    inline fun <reified T: Any> fromFunction(crossinline f: (JdbcDecodingContext, Int) -> T?): JdbcDecoderAny<T> =
      object: JdbcDecoderAny<T>() {
        override val type = T::class
        override fun decode(ctx: JdbcDecodingContext, index: Int) =
          f(ctx, index) ?:
          throw NullPointerException("Non-nullable Decoder returned null for index $index, column: ${ctx.row.metaData.getColumnName(index)} and expected type: ${T::class}")
      }
  }
}

abstract class JdbcDecodersBasic: SqlDecoders<Connection, ResultSet>() {
  override fun preview(index: Int, row: ResultSet): String? = row.getObject(index)?.let { it.toString() }
  override fun isNull(index: Int, row: ResultSet): Boolean {
    row.getObject(index)
    return row.wasNull()
  }


  override val ByteDecoder: JdbcDecoderAny<Byte> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getByte(i) }
  override val CharDecoder: JdbcDecoderAny<Char> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getString(i)[0] }
  override val DoubleDecoder: JdbcDecoderAny<Double> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getDouble(i) }
  override val FloatDecoder: JdbcDecoderAny<Float> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getFloat(i) }
  override val IntDecoder: JdbcDecoderAny<Int> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getInt(i) }
  override val LongDecoder: JdbcDecoderAny<Long> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getLong(i) }
  override val ShortDecoder: JdbcDecoderAny<Short> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getShort(i) }
  override val StringDecoder: JdbcDecoderAny<String> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getString(i) }
  override val BigDecimalDecoder: JdbcDecoderAny<BigDecimal> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getBigDecimal(i) }
  override val ByteArrayDecoder: JdbcDecoderAny<ByteArray> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getBytes(i) }
  override val DateDecoder: JdbcDecoderAny<java.util.Date> =
    JdbcDecoderAny.fromFunction { ctx, i ->
      java.util.Date(ctx.row.getTimestamp(i, Calendar.getInstance(ctx.timeZone)).getTime())
    }
}

open class JdbcDecodersWithTime(val additionalDecoders: Set<SqlDecoder<Connection, ResultSet, out Any>>): JdbcDecodersBasic() {
  companion object: JdbcDecodersWithTime(setOf())

  override val LocalDateDecoder: JdbcDecoderAny<LocalDate> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getObject(i, LocalDate::class.java) }
  override val LocalTimeDecoder: JdbcDecoderAny<LocalTime> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getObject(i, LocalTime::class.java) }
  override val LocalDateTimeDecoder: JdbcDecoderAny<LocalDateTime> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getObject(i, LocalDateTime::class.java) }
  override val ZonedDateTimeDecoder: JdbcDecoderAny<ZonedDateTime> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getObject(i, OffsetDateTime::class.java).toZonedDateTime() }
  val SqlDateDecoder: JdbcDecoderAny<java.sql.Date> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getDate(i) }
  val SqlTimeDecoder: JdbcDecoderAny<java.sql.Time> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getTime(i) }
  val SqlTimestampDecoder: JdbcDecoderAny<java.sql.Timestamp> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getTimestamp(i) }

  override val InstantDecoder: JdbcDecoderAny<Instant> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getObject(i, OffsetDateTime::class.java).toInstant() }
  override val OffsetTimeDecoder: JdbcDecoderAny<OffsetTime> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getObject(i, OffsetTime::class.java) }
  override val OffsetDateTimeDecoder: JdbcDecoderAny<OffsetDateTime> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getObject(i, OffsetDateTime::class.java) }

  override val decoders by lazy { super.decoders + setOf(SqlDateDecoder, SqlTimeDecoder, SqlTimestampDecoder) + additionalDecoders }
}

open class JdbcDecodersWithTimeLegacy(val additionalDecoders: Set<SqlDecoder<Connection, ResultSet, out Any>>): JdbcDecodersBasic() {
  companion object: JdbcDecodersWithTimeLegacy(setOf())

  override val LocalDateDecoder: JdbcDecoderAny<LocalDate> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getDate(i).toLocalDate() }
  override val LocalTimeDecoder: JdbcDecoderAny<LocalTime> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getTime(i).toLocalTime() }
  override val LocalDateTimeDecoder: JdbcDecoderAny<LocalDateTime> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getTimestamp(i).toLocalDateTime() }
  override val ZonedDateTimeDecoder: JdbcDecoderAny<ZonedDateTime> = JdbcDecoderAny.fromFunction { ctx, i -> ZonedDateTime.ofInstant(ctx.row.getTimestamp(i).toInstant(), ctx.timeZone.toZoneId()) }
  val SqlDateEncoder = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getDate(i) }
  val SqlTimeEncoder = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getTime(i) }
  val SqlTimestampEncoder = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getTimestamp(i) }

  override val InstantDecoder: JdbcDecoderAny<Instant> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getTimestamp(i).toInstant() }
  override val OffsetTimeDecoder: JdbcDecoderAny<OffsetTime> = JdbcDecoderAny.fromFunction { ctx, i -> OffsetTime.of(ctx.row.getTime(i).toLocalTime(), ZoneOffset.UTC) }
  override val OffsetDateTimeDecoder: JdbcDecoderAny<OffsetDateTime> = JdbcDecoderAny.fromFunction { ctx, i -> OffsetDateTime.ofInstant(ctx.row.getTimestamp(i).toInstant(), ctx.timeZone.toZoneId()) }

  override val decoders by lazy { super.decoders + setOf(SqlDateEncoder, SqlTimeEncoder, SqlTimestampEncoder) + additionalDecoders }
}
