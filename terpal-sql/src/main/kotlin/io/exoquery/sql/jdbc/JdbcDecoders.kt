package io.exoquery.sql.jdbc

import io.exoquery.sql.SqlDecoder
import io.exoquery.sql.SqlDecoders
import java.math.BigDecimal
import java.sql.Connection
import java.sql.ResultSet
import java.time.*
import java.util.*

abstract class JdbcDecoder<T: Any>: SqlDecoder<Connection, ResultSet, T>() {
  inline fun <reified R: Any> map(crossinline f: (T) -> R): JdbcDecoder<R> =
    object: JdbcDecoder<R>() {
      override val type = R::class
      override fun decode(ctx: JdbcDecodingContext, index: Int) =
        f(this@JdbcDecoder.decode(ctx, index))
    }

  override fun asNullable(): SqlDecoder<Connection, ResultSet, T?> =
    object: SqlDecoder<Connection, ResultSet, T?>() {
      override fun asNullable(): SqlDecoder<Connection, ResultSet, T?> = this
      override val type = this@JdbcDecoder.type
      override fun decode(ctx: JdbcDecodingContext, index: Int): T? {
        ctx.row.getObject(index)
        return if (ctx.row.wasNull())
          null
        else
          this@JdbcDecoder.decode(ctx, index)
      }
    }

  companion object {
    inline fun <reified T: Any> fromFunction(crossinline f: (JdbcDecodingContext, Int) -> T?): JdbcDecoder<T> =
      object: JdbcDecoder<T>() {
        override val type = T::class
        override fun decode(ctx: JdbcDecodingContext, index: Int) =
          f(ctx, index) ?:
          throw NullPointerException("Non-nullable Decoder returned null for index $index, column: ${ctx.row.metaData.getColumnName(index)} and expected type: ${T::class}")
      }
  }
}

interface JdbcDecodersBasic: SqlDecoders<Connection, ResultSet> {
  companion object {
    val BooleanDecoder: JdbcDecoder<Boolean> = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getBoolean(i) }
    val ByteDecoder: JdbcDecoder<Byte> = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getByte(i) }
    val CharDecoder: JdbcDecoder<Char> = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getString(i)[0] }
    val DoubleDecoder: JdbcDecoder<Double> = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getDouble(i) }
    val FloatDecoder: JdbcDecoder<Float> = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getFloat(i) }
    val IntDecoder: JdbcDecoder<Int> = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getInt(i) }
    val LongDecoder: JdbcDecoder<Long> = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getLong(i) }
    val ShortDecoder: JdbcDecoder<Short> = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getShort(i) }
    val StringDecoder: JdbcDecoder<String> = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getString(i) }
    val BigDecimalDecoder: JdbcDecoder<BigDecimal> = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getBigDecimal(i) }
    val ByteArrayDecoder: JdbcDecoder<ByteArray> = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getBytes(i) }
    val DateDecoder: JdbcDecoder<java.util.Date> =
      JdbcDecoder.fromFunction { ctx, i ->
        java.util.Date(ctx.row.getTimestamp(i, Calendar.getInstance(ctx.timezone)).getTime())
      }
  }

  override fun preview(index: Int, row: ResultSet): String? = row.getObject(index)?.let { it.toString() }
  override fun isNull(index: Int, row: ResultSet): Boolean {
    row.getObject(index)
    return row.wasNull()
  }

  override val BooleanDecoder get() = JdbcDecodersBasic.BooleanDecoder
  override val ByteDecoder get() = JdbcDecodersBasic.ByteDecoder
  override val CharDecoder get() = JdbcDecodersBasic.CharDecoder
  override val DoubleDecoder get() = JdbcDecodersBasic.DoubleDecoder
  override val FloatDecoder get() = JdbcDecodersBasic.FloatDecoder
  override val IntDecoder get() = JdbcDecodersBasic.IntDecoder
  override val LongDecoder get() = JdbcDecodersBasic.LongDecoder
  override val ShortDecoder get() = JdbcDecodersBasic.ShortDecoder
  override val StringDecoder get() = JdbcDecodersBasic.StringDecoder
  override val BigDecimalDecoder get() = JdbcDecodersBasic.BigDecimalDecoder
  override val ByteArrayDecoder get() = JdbcDecodersBasic.ByteArrayDecoder
  override val DateDecoder get() = JdbcDecodersBasic.DateDecoder
}

interface JdbcDecodersWithTime: JdbcDecodersBasic {
  companion object {
    val LocalDateDecoder: JdbcDecoder<LocalDate> = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getObject(i, LocalDate::class.java) }
    val LocalTimeDecoder: JdbcDecoder<LocalTime> = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getObject(i, LocalTime::class.java) }
    val LocalDateTimeDecoder: JdbcDecoder<LocalDateTime> = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getObject(i, LocalDateTime::class.java) }
    val ZonedDateTimeDecoder: JdbcDecoder<ZonedDateTime> = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getObject(i, OffsetDateTime::class.java).toZonedDateTime() }
    val SqlDateDecoder: JdbcDecoder<java.sql.Date> = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getDate(i) }

    val InstantDecoder: JdbcDecoder<Instant> = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getObject(i, OffsetDateTime::class.java).toInstant() }
    val OffsetTimeDecoder: JdbcDecoder<OffsetTime> = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getObject(i, OffsetTime::class.java) }
    val OffsetDateTimeDecoder: JdbcDecoder<OffsetDateTime> = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getObject(i, OffsetDateTime::class.java) }
  }

  override val LocalDateDecoder: JdbcDecoder<LocalDate> get() = JdbcDecodersWithTime.LocalDateDecoder
  override val LocalTimeDecoder: JdbcDecoder<LocalTime> get() = JdbcDecodersWithTime.LocalTimeDecoder
  override val LocalDateTimeDecoder: JdbcDecoder<LocalDateTime> get() = JdbcDecodersWithTime.LocalDateTimeDecoder
  override val ZonedDateTimeDecoder: JdbcDecoder<ZonedDateTime> get() = JdbcDecodersWithTime.ZonedDateTimeDecoder
  val SqlDateDecoder: JdbcDecoder<java.sql.Date> get() = JdbcDecodersWithTime.SqlDateDecoder

  override val InstantDecoder: JdbcDecoder<Instant> get() = JdbcDecodersWithTime.InstantDecoder
  override val OffsetTimeDecoder: JdbcDecoder<OffsetTime> get() = JdbcDecodersWithTime.OffsetTimeDecoder
  override val OffsetDateTimeDecoder: JdbcDecoder<OffsetDateTime> get() = JdbcDecodersWithTime.OffsetDateTimeDecoder

  override fun computeDecoders() = super.computeDecoders() + setOf(SqlDateDecoder)
}

interface JdbcDecodersWithTimeLegacy: JdbcDecodersBasic {
  companion object {
    val LocalDateDecoder: JdbcDecoder<LocalDate> = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getDate(i).toLocalDate() }
    val LocalTimeDecoder: JdbcDecoder<LocalTime> = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getTime(i).toLocalTime() }
    val LocalDateTimeDecoder: JdbcDecoder<LocalDateTime> = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getTimestamp(i).toLocalDateTime() }
    val ZonedDateTimeDecoder: JdbcDecoder<ZonedDateTime> = JdbcDecoder.fromFunction { ctx, i -> ZonedDateTime.ofInstant(ctx.row.getTimestamp(i).toInstant(), ctx.timezone.toZoneId()) }
    val sqlDateEncoder = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getDate(i) }

    val InstantDecoder: JdbcDecoder<Instant> = JdbcDecoder.fromFunction { ctx, i -> ctx.row.getTimestamp(i).toInstant() }
    val OffsetTimeDecoder: JdbcDecoder<OffsetTime> = JdbcDecoder.fromFunction { ctx, i -> OffsetTime.of(ctx.row.getTime(i).toLocalTime(), ZoneOffset.UTC) }
    val OffsetDateTimeDecoder: JdbcDecoder<OffsetDateTime> = JdbcDecoder.fromFunction { ctx, i -> OffsetDateTime.ofInstant(ctx.row.getTimestamp(i).toInstant(), ctx.timezone.toZoneId()) }
  }

  override val LocalDateDecoder get() = JdbcDecodersWithTimeLegacy.LocalDateDecoder
  override val LocalTimeDecoder get() = JdbcDecodersWithTimeLegacy.LocalTimeDecoder
  override val LocalDateTimeDecoder get() = JdbcDecodersWithTimeLegacy.LocalDateTimeDecoder
  override val ZonedDateTimeDecoder get() = JdbcDecodersWithTimeLegacy.ZonedDateTimeDecoder
  val sqlDateEncoder get() = JdbcDecodersWithTimeLegacy.sqlDateEncoder

  override val InstantDecoder get() = JdbcDecodersWithTimeLegacy.InstantDecoder
  override val OffsetTimeDecoder get() = JdbcDecodersWithTimeLegacy.OffsetTimeDecoder
  override val OffsetDateTimeDecoder get() = JdbcDecodersWithTimeLegacy.OffsetDateTimeDecoder

  override fun computeDecoders() = super.computeDecoders() + setOf(sqlDateEncoder)
}
