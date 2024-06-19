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
      override fun decode(session: Connection, row: ResultSet, index: Int) =
        f(this@JdbcDecoder.decode(session, row, index))
    }

  override fun asNullable(): SqlDecoder<Connection, ResultSet, T?> =
    object: SqlDecoder<Connection, ResultSet, T?>() {
      override fun asNullable(): SqlDecoder<Connection, ResultSet, T?> = this
      override val type = this@JdbcDecoder.type
      override fun decode(session: Connection, row: ResultSet, index: Int): T? {
        row.getObject(index)
        return if (row.wasNull())
          null
        else
          this@JdbcDecoder.decode(session, row, index)
      }
    }

  companion object {
    inline fun <reified T: Any> fromFunction(crossinline f: (Connection, ResultSet, Int) -> T?): JdbcDecoder<T> =
      object: JdbcDecoder<T>() {
        override val type = T::class
        override fun decode(session: Connection, row: ResultSet, index: Int) =
          f(session, row, index) ?:
          throw NullPointerException("Non-nullable Decoder returned null for index $index, column: ${row.metaData.getColumnName(index)} and expected type: ${T::class}")
      }
  }
}

abstract class JdbcDecodersBasic: SqlDecoders<Connection, ResultSet>() {
  abstract val dateTimeZone: TimeZone

  override fun preview(index: Int, row: ResultSet): String? = row.getObject(index)?.let { it.toString() }
  override fun isNull(index: Int, row: ResultSet): Boolean {
    row.getObject(index)
    return row.wasNull()
  }

  override val BooleanDecoder: JdbcDecoder<Boolean> = JdbcDecoder.fromFunction { _, rs, i -> rs.getBoolean(i) }
  override val ByteDecoder: JdbcDecoder<Byte> = JdbcDecoder.fromFunction { _, rs, i -> rs.getByte(i) }
  override val CharDecoder: JdbcDecoder<Char> = JdbcDecoder.fromFunction { _, rs, i -> rs.getString(i)[0] }
  override val DoubleDecoder: JdbcDecoder<Double> = JdbcDecoder.fromFunction { _, rs, i -> rs.getDouble(i) }
  override val FloatDecoder: JdbcDecoder<Float> = JdbcDecoder.fromFunction { _, rs, i -> rs.getFloat(i) }
  override val IntDecoder: JdbcDecoder<Int> = JdbcDecoder.fromFunction { _, rs, i -> rs.getInt(i) }
  override val LongDecoder: JdbcDecoder<Long> = JdbcDecoder.fromFunction { _, rs, i -> rs.getLong(i) }
  override val ShortDecoder: JdbcDecoder<Short> = JdbcDecoder.fromFunction { _, rs, i -> rs.getShort(i) }
  override val StringDecoder: JdbcDecoder<String> = JdbcDecoder.fromFunction { _, rs, i -> rs.getString(i) }
  override val BigDecimalDecoder: JdbcDecoder<BigDecimal> = JdbcDecoder.fromFunction { _, rs, i -> rs.getBigDecimal(i) }
  override val ByteArrayDecoder: JdbcDecoder<ByteArray> = JdbcDecoder.fromFunction { _, rs, i -> rs.getBytes(i) }
  override val DateDecoder: JdbcDecoder<java.util.Date> =
    JdbcDecoder.fromFunction { _, rs, i ->
      java.util.Date(rs.getTimestamp(i, Calendar.getInstance(dateTimeZone)).getTime())
    }
}

open class JdbcDecodersWithTime(override val dateTimeZone: TimeZone, val additionalDecoders: Set<SqlDecoder<Connection, ResultSet, out Any>>): JdbcDecodersBasic() {
  /** Convenience instance so you can reference decoders that are not dependent on dateTimeZone/additionalDecoders */
  companion object: JdbcDecodersWithTime(TimeZone.getDefault(), emptySet())

  override val LocalDateDecoder: JdbcDecoder<LocalDate> = JdbcDecoder.fromFunction { _, rs, i -> rs.getObject(i, LocalDate::class.java) }
  override val LocalTimeDecoder: JdbcDecoder<LocalTime> = JdbcDecoder.fromFunction { _, rs, i -> rs.getObject(i, LocalTime::class.java) }
  override val LocalDateTimeDecoder: JdbcDecoder<LocalDateTime> = JdbcDecoder.fromFunction { _, rs, i -> rs.getObject(i, LocalDateTime::class.java) }
  override val ZonedDateTimeDecoder: JdbcDecoder<ZonedDateTime> = JdbcDecoder.fromFunction { _, rs, i -> rs.getObject(i, OffsetDateTime::class.java).toZonedDateTime() }
  val SqlDateDecoder: JdbcDecoder<java.sql.Date> = JdbcDecoder.fromFunction { _, rs, i -> rs.getDate(i) }

  override val InstantDecoder: JdbcDecoder<Instant> = JdbcDecoder.fromFunction { _, rs, i -> rs.getObject(i, OffsetDateTime::class.java).toInstant() }
  override val OffsetTimeDecoder: JdbcDecoder<OffsetTime> = JdbcDecoder.fromFunction { _, rs, i -> rs.getObject(i, OffsetTime::class.java) }
  override val OffsetDateTimeDecoder: JdbcDecoder<OffsetDateTime> = JdbcDecoder.fromFunction { _, rs, i -> rs.getObject(i, OffsetDateTime::class.java) }

  override val decoders by lazy { super.decoders + setOf(SqlDateDecoder) + additionalDecoders }
}

open class JdbcDecodersWithTimeLegacy(override val dateTimeZone: TimeZone, val additionalDecoders: Set<SqlDecoder<Connection, ResultSet, out Any>>): JdbcDecodersBasic() {
  /** Convenience instance so you can reference decoders that are not dependent on dateTimeZone/additionalDecoders */
  companion object: JdbcDecodersWithTimeLegacy(TimeZone.getDefault(), emptySet())

  override val LocalDateDecoder: JdbcDecoder<LocalDate> = JdbcDecoder.fromFunction { _, rs, i -> rs.getDate(i).toLocalDate() }
  override val LocalTimeDecoder: JdbcDecoder<LocalTime> = JdbcDecoder.fromFunction { _, rs, i -> rs.getTime(i).toLocalTime() }
  override val LocalDateTimeDecoder: JdbcDecoder<LocalDateTime> = JdbcDecoder.fromFunction { _, rs, i -> rs.getTimestamp(i).toLocalDateTime() }
  override val ZonedDateTimeDecoder: JdbcDecoder<ZonedDateTime> = JdbcDecoder.fromFunction { _, rs, i -> ZonedDateTime.ofInstant(rs.getTimestamp(i).toInstant(), dateTimeZone.toZoneId()) }
  val sqlDateEncoder = JdbcDecoder.fromFunction { _, rs, i -> rs.getDate(i) }

  override val InstantDecoder: JdbcDecoder<Instant> = JdbcDecoder.fromFunction { _, rs, i -> rs.getTimestamp(i).toInstant() }
  override val OffsetTimeDecoder: JdbcDecoder<OffsetTime> = JdbcDecoder.fromFunction { _, rs, i -> OffsetTime.of(rs.getTime(i).toLocalTime(), ZoneOffset.UTC) }
  override val OffsetDateTimeDecoder: JdbcDecoder<OffsetDateTime> = JdbcDecoder.fromFunction { _, rs, i -> OffsetDateTime.ofInstant(rs.getTimestamp(i).toInstant(), dateTimeZone.toZoneId()) }

  override val decoders by lazy { super.decoders + setOf(sqlDateEncoder) + additionalDecoders }
}
