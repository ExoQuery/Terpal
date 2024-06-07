package io.exoquery.sql.jdbc

import io.exoquery.sql.Decoder
import io.exoquery.sql.Decoders
import java.sql.Connection
import java.sql.ResultSet
import java.time.*
import java.util.*

abstract class JdbcDecoder<T: Any>: Decoder<Connection, ResultSet, T> {
  companion object {
    inline fun <reified T: Any> fromFunction(crossinline f: (Connection, ResultSet, Int) -> T): JdbcDecoder<T> =
      object: JdbcDecoder<T>() {
        override val type = T::class
        override fun decode(session: Connection, row: ResultSet, index: Int) = f(session, row, index)
      }
  }
}

abstract class JdbcDecodersBasic: Decoders<Connection, ResultSet>() {
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
}

open class JdbcDecodersWithTime: JdbcDecodersBasic() {
  override val LocalDateDecoder: JdbcDecoder<LocalDate> = JdbcDecoder.fromFunction { _, rs, i -> rs.getObject(i, LocalDate::class.java) }
  override val LocalTimeDecoder: JdbcDecoder<LocalTime> = JdbcDecoder.fromFunction { _, rs, i -> rs.getObject(i, LocalTime::class.java) }
  override val LocalDateTimeDecoder: JdbcDecoder<LocalDateTime> = JdbcDecoder.fromFunction { _, rs, i -> rs.getObject(i, LocalDateTime::class.java) }
  override val ZonedDateTimeDecoder: JdbcDecoder<ZonedDateTime> = JdbcDecoder.fromFunction { _, rs, i -> rs.getObject(i, OffsetDateTime::class.java).toZonedDateTime() }

  override val InstantDecoder: JdbcDecoder<Instant> = JdbcDecoder.fromFunction { _, rs, i -> rs.getObject(i, OffsetDateTime::class.java).toInstant() }
  override val OffsetTimeDecoder: JdbcDecoder<OffsetTime> = JdbcDecoder.fromFunction { _, rs, i -> rs.getObject(i, OffsetTime::class.java) }
  override val OffsetDateTimeDecoder: JdbcDecoder<OffsetDateTime> = JdbcDecoder.fromFunction { _, rs, i -> rs.getObject(i, OffsetDateTime::class.java) }
}

open class JdbcDecodersWithTimeLegacy: JdbcDecodersBasic() {
  open val dateTimeZone: TimeZone get() = TimeZone.getTimeZone("UTC")

  override val LocalDateDecoder: JdbcDecoder<LocalDate> = JdbcDecoder.fromFunction { _, rs, i -> rs.getDate(i).toLocalDate() }
  override val LocalTimeDecoder: JdbcDecoder<LocalTime> = JdbcDecoder.fromFunction { _, rs, i -> rs.getTime(i).toLocalTime() }
  override val LocalDateTimeDecoder: JdbcDecoder<LocalDateTime> = JdbcDecoder.fromFunction { _, rs, i -> rs.getTimestamp(i).toLocalDateTime() }
  override val ZonedDateTimeDecoder: JdbcDecoder<ZonedDateTime> = JdbcDecoder.fromFunction { _, rs, i -> ZonedDateTime.ofInstant(rs.getTimestamp(i).toInstant(), dateTimeZone.toZoneId()) }

  override val InstantDecoder: JdbcDecoder<Instant> = JdbcDecoder.fromFunction { _, rs, i -> rs.getTimestamp(i).toInstant() }
  override val OffsetTimeDecoder: JdbcDecoder<OffsetTime> = JdbcDecoder.fromFunction { _, rs, i -> OffsetTime.of(rs.getTime(i).toLocalTime(), ZoneOffset.UTC) }
  override val OffsetDateTimeDecoder: JdbcDecoder<OffsetDateTime> = JdbcDecoder.fromFunction { _, rs, i -> OffsetDateTime.ofInstant(rs.getTimestamp(i).toInstant(), dateTimeZone.toZoneId()) }
}
