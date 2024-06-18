package io.exoquery.sql.jdbc

import io.exoquery.sql.SqlEncoder
import io.exoquery.sql.SqlEncoders
import java.math.BigDecimal
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.sql.Types
import java.time.*
import java.util.*

// Note that T can't extend Any because then T will not be allowed to be null when it is being decoded
abstract class JdbcEncoder<T>: SqlEncoder<Connection, PreparedStatement, T>() {
  inline fun <reified R> contramap(crossinline f: (R) -> T):JdbcEncoder<R> =
    object: JdbcEncoder<R>() {
      override val type = R::class
      override fun encode(session: Connection, statement: PreparedStatement, value: R, index: Int) =
        this@JdbcEncoder.encode(session, statement, f(value), index)
    }

  companion object {
    inline fun <reified T> fromFunction(crossinline f: (Connection, PreparedStatement, T, Int) -> Unit): JdbcEncoder<T> =
      object: JdbcEncoder<T>() {
        override val type = T::class
        override fun encode(session: Connection, statement: PreparedStatement, value: T, index: Int) = f(session, statement, value, index)
      }
  }
}

abstract class JdbcEncodersBasic: SqlEncoders<Connection, PreparedStatement>() {
  abstract val dateTimeZone: TimeZone

  override val BooleanEncoder: JdbcEncoder<Boolean> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setBoolean(i, v) }
  override val ByteEncoder: JdbcEncoder<Byte> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setByte(i, v) }
  override val CharEncoder: JdbcEncoder<Char> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setString(i, v.toString()) }
  override val DoubleEncoder: JdbcEncoder<Double> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setDouble(i, v) }
  override val FloatEncoder: JdbcEncoder<Float> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setFloat(i, v) }
  override val IntEncoder: JdbcEncoder<Int> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setInt(i, v) }
  override val LongEncoder: JdbcEncoder<Long> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setLong(i, v) }
  override val ShortEncoder: JdbcEncoder<Short> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setShort(i, v) }
  override val StringEncoder: JdbcEncoder<String> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setString(i, v) }
  override val BigDecimalEncoder: JdbcEncoder<BigDecimal> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setBigDecimal(i, v) }
  override val ByteArrayEncoder: JdbcEncoder<ByteArray> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setBytes(i, v) }
  override val DateEncoder: JdbcEncoder<java.util.Date> =
    JdbcEncoder.fromFunction { _, ps, v, i ->
      ps.setTimestamp(i, java.sql.Timestamp(v.getTime()), Calendar.getInstance(dateTimeZone))
    }
}

open class JdbcEncodersWithTime(override val dateTimeZone: TimeZone, val additionalEncoders: Set<SqlEncoder<Connection, PreparedStatement, *>>): JdbcEncodersBasic() {
  /** Convenience instance so you can reference encoders that are not dependent on dateTimeZone/additionalEncoders */
  companion object: JdbcEncodersWithTime(TimeZone.getDefault(), emptySet())

  val jdbcTypeOfLocalDate     = Types.DATE
  val jdbcTypeOfLocalTime     = Types.TIME
  val jdbcTypeOfLocalDateTime = Types.TIMESTAMP
  val jdbcTypeOfZonedDateTime = Types.TIMESTAMP_WITH_TIMEZONE
  val jdbcTypeOfInstant                      = Types.TIMESTAMP_WITH_TIMEZONE
  val jdbcTypeOfOffsetTime                   = Types.TIME_WITH_TIMEZONE
  val jdbcTypeOfOffsetDateTime               = Types.TIMESTAMP_WITH_TIMEZONE
  fun jdbcEncodeInstant(value: Instant): Any = value.atOffset(ZoneOffset.UTC)

  override val LocalDateEncoder: JdbcEncoder<LocalDate> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setObject(i, v, jdbcTypeOfLocalDate) }
  override val LocalTimeEncoder: JdbcEncoder<LocalTime> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setObject(i, v, jdbcTypeOfLocalTime) }
  override val LocalDateTimeEncoder: JdbcEncoder<LocalDateTime> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setObject(i, v, jdbcTypeOfLocalDateTime) }
  override val ZonedDateTimeEncoder: JdbcEncoder<ZonedDateTime> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setObject(i, v, jdbcTypeOfZonedDateTime) }
  val SqlDateEncoder: JdbcEncoder<java.sql.Date> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setDate(i, v) }

  override val InstantEncoder: JdbcEncoder<Instant> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setObject(i, jdbcEncodeInstant(v), jdbcTypeOfInstant) }
  override val OffsetTimeEncoder: JdbcEncoder<OffsetTime> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setObject(i, v, jdbcTypeOfOffsetTime) }
  override val OffsetDateTimeEncoder: JdbcEncoder<OffsetDateTime> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setObject(i, v, jdbcTypeOfOffsetDateTime) }

  override val encoders by lazy { super.encoders + setOf(SqlDateEncoder) + (additionalEncoders as Set<SqlEncoder<Connection, PreparedStatement, Any>>) }
}

open class JdbcEncodersWithTimeLegacy(override val dateTimeZone: TimeZone, val additionalEncoders: Set<SqlEncoder<Connection, PreparedStatement, *>>): JdbcEncodersBasic() {
  /** Convenience instance so you can reference encoders that are not dependent on dateTimeZone/additionalEncoders */
  companion object: JdbcEncodersWithTimeLegacy(TimeZone.getDefault(), emptySet())

  override val LocalDateEncoder: JdbcEncoder<LocalDate> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setDate(i, java.sql.Date.valueOf(v)) }
  override val LocalTimeEncoder: JdbcEncoder<LocalTime> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setTime(i, java.sql.Time.valueOf(v)) }
  override val LocalDateTimeEncoder: JdbcEncoder<LocalDateTime> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setTimestamp(i, Timestamp.valueOf(v)) }
  override val ZonedDateTimeEncoder: JdbcEncoder<ZonedDateTime> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setTimestamp(i, Timestamp.from(v.toInstant())) }
  val SqlDateEncoder: JdbcEncoder<java.sql.Date> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setDate(i, v) }

  override val InstantEncoder: JdbcEncoder<Instant> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setTimestamp(i, Timestamp.from(v)) }
  override val OffsetTimeEncoder: JdbcEncoder<OffsetTime> = JdbcEncoder.fromFunction { _, ps, v, i -> java.sql.Time.valueOf(v.withOffsetSameInstant(ZoneOffset.UTC).toLocalTime()) }
  override val OffsetDateTimeEncoder: JdbcEncoder<OffsetDateTime> = JdbcEncoder.fromFunction { _, ps, v, i -> ps.setTimestamp(i, java.sql.Timestamp.from(v.toInstant())) }

  override val encoders by lazy { super.encoders + setOf(SqlDateEncoder) + (additionalEncoders as Set<SqlEncoder<Connection, PreparedStatement, Any>>) }
}
