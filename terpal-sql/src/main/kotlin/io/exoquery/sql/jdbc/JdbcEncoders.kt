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
abstract class JdbcEncoder<T: Any>: SqlEncoder<Connection, PreparedStatement, T>() {
  abstract val jdbcType: Int

  override fun asNullable(): SqlEncoder<Connection, PreparedStatement, T?> =
    object: SqlEncoder<Connection, PreparedStatement, T?>() {
      override val type = this@JdbcEncoder.type
      val jdbcType = this@JdbcEncoder.jdbcType
      override fun asNullable(): SqlEncoder<Connection, PreparedStatement, T?> = this

      override fun encode(session: Connection, statement: PreparedStatement, value: T?, index: Int) =
        if (value != null)
          this@JdbcEncoder.encode(session, statement, value, index)
        else
          statement.setNull(index, jdbcType)
    }

  inline fun <reified R: Any> contramap(crossinline f: (R) -> T):JdbcEncoder<R> =
    object: JdbcEncoder<R>() {
      override val type = R::class
      // Get the JDBC type from the parent. This makes sense because most of the time contramapped encoders are from primivites
      // e.g. StringDecoder.contramap { ... } so we want the jdbc type from the parent.
      override val jdbcType = this@JdbcEncoder.jdbcType
      override fun encode(session: Connection, statement: PreparedStatement, value: R, index: Int) =
        this@JdbcEncoder.encode(session, statement, f(value), index)
    }

  /*
  expected:<[EncodingTestEntity(v1=s, v2=1.1, v3=true, v4=11, v5=23, v6=33, v7=431, v8=34.4, v9=42.0, v10=[1, 2], v11=Fri Nov 22 19:00:00 EST 2013, v12=EncodingTestType(value=s), v13=2013-11-23, v14=173fb134-c84b-4bb2-be5c-85b2552f60b5, o1=s, o2=1.1, o3=true, o4=11, o5=23, o6=33, o7=431, o8=34.4, o9=42.0, o10=[1, 2], o11=Fri Nov 22 19:00:00 EST 2013, o12=EncodingTestType(value=s), o13=2013-11-23, o14=348e85a2-d953-4cb6-a2ff-a90f02006eb4),
             EncodingTestEntity(v1=, v2=0, v3=false, v4=0, v5=0, v6=0, v7=0, v8=0.0, v9=0.0, v10=[], v11=1969-12-31, v12=EncodingTestType(value=), v13=1970-01-01, v14=00000000-0000-0000-0000-000000000000, o1=null, o2=null, o3=null, o4=null, o5=null, o6=null, o7=null, o8=null, o9=null, o10=null, o11=null, o12=null, o13=null, o14=null)]> but was:<[EncodingTestEntity(v1=s, v2=1.10, v3=true, v4=11, v5=23, v6=33, v7=431, v8=34.4, v9=42.0, v10=[1, 2], v11=Fri Nov 22 19:00:00 EST 2013, v12=EncodingTestType(value=s), v13=2013-11-23, v14=173fb134-c84b-4bb2-be5c-85b2552f60b5, o1=s, o2=1.10, o3=true, o4=null, o5=null, o6=null, o7=null, o8=null, o9=null, o10=null, o11=null, o12=null, o13=null, o14=null), EncodingTestEntity(v1=, v2=0.00, v3=false, v4=0, v5=0, v6=0, v7=0, v8=0.0, v9=0.0, v10=[], v11=Wed Dec 31 19:00:00 EST 1969, v12=EncodingTestType(value=), v13=1970-01-01, v14=00000000-0000-0000-0000-000000000000, o1=null, o2=null, o3=null, o4=null, o5=null, o6=null, o7=null, o8=null, o9=null, o10=null, o11=null, o12=null, o13=null, o14=null)]>
   */

  companion object {
    inline fun <reified T: Any> fromFunction(jdbcTypeNum: Int, crossinline f: (Connection, PreparedStatement, T, Int) -> Unit): JdbcEncoder<T> =
      object: JdbcEncoder<T>() {
        override val jdbcType: Int = jdbcTypeNum
        override val type = T::class
        override fun encode(session: Connection, statement: PreparedStatement, value: T, index: Int) =
          f(session, statement, value, index)
      }
  }
}

abstract class JdbcEncodersBasic: SqlEncoders<Connection, PreparedStatement>() {
  abstract val dateTimeZone: TimeZone

  override val BooleanEncoder: JdbcEncoder<Boolean> = JdbcEncoder.fromFunction(Types.BOOLEAN) { _, ps, v, i -> ps.setBoolean(i, v) }
  override val ByteEncoder: JdbcEncoder<Byte> = JdbcEncoder.fromFunction(Types.TINYINT) { _, ps, v, i -> ps.setByte(i, v) }
  override val CharEncoder: JdbcEncoder<Char> = JdbcEncoder.fromFunction(Types.VARCHAR) { _, ps, v, i -> ps.setString(i, v.toString()) }
  override val DoubleEncoder: JdbcEncoder<Double> = JdbcEncoder.fromFunction(Types.DOUBLE) { _, ps, v, i -> ps.setDouble(i, v) }
  override val FloatEncoder: JdbcEncoder<Float> = JdbcEncoder.fromFunction(Types.FLOAT) { _, ps, v, i -> ps.setFloat(i, v) }
  override val IntEncoder: JdbcEncoder<Int> = JdbcEncoder.fromFunction(Types.INTEGER) { _, ps, v, i -> ps.setInt(i, v) }
  override val LongEncoder: JdbcEncoder<Long> = JdbcEncoder.fromFunction(Types.BIGINT) { _, ps, v, i -> ps.setLong(i, v) }
  override val ShortEncoder: JdbcEncoder<Short> = JdbcEncoder.fromFunction(Types.SMALLINT) { _, ps, v, i -> ps.setShort(i, v) }
  override val StringEncoder: JdbcEncoder<String> = JdbcEncoder.fromFunction(Types.VARCHAR) { _, ps, v, i -> ps.setString(i, v) }
  override val BigDecimalEncoder: JdbcEncoder<BigDecimal> = JdbcEncoder.fromFunction(Types.NUMERIC) { _, ps, v, i -> ps.setBigDecimal(i, v) }
  override val ByteArrayEncoder: JdbcEncoder<ByteArray> = JdbcEncoder.fromFunction(Types.VARBINARY) { _, ps, v, i -> ps.setBytes(i, v) }
  override val DateEncoder: JdbcEncoder<java.util.Date> =
    JdbcEncoder.fromFunction(Types.TIMESTAMP) { _, ps, v, i ->
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

  override val LocalDateEncoder: JdbcEncoder<LocalDate> = JdbcEncoder.fromFunction(jdbcTypeOfLocalDate) { _, ps, v, i -> ps.setObject(i, v, jdbcTypeOfLocalDate) }
  override val LocalTimeEncoder: JdbcEncoder<LocalTime> = JdbcEncoder.fromFunction(jdbcTypeOfLocalTime) { _, ps, v, i -> ps.setObject(i, v, jdbcTypeOfLocalTime) }
  override val LocalDateTimeEncoder: JdbcEncoder<LocalDateTime> = JdbcEncoder.fromFunction(jdbcTypeOfLocalDateTime) { _, ps, v, i -> ps.setObject(i, v, jdbcTypeOfLocalDateTime) }
  override val ZonedDateTimeEncoder: JdbcEncoder<ZonedDateTime> = JdbcEncoder.fromFunction(jdbcTypeOfZonedDateTime) { _, ps, v, i -> ps.setObject(i, v, jdbcTypeOfZonedDateTime) }
  val SqlDateEncoder: JdbcEncoder<java.sql.Date> = JdbcEncoder.fromFunction(Types.DATE) { _, ps, v, i -> ps.setDate(i, v) }

  override val InstantEncoder: JdbcEncoder<Instant> = JdbcEncoder.fromFunction(jdbcTypeOfInstant) { _, ps, v, i -> ps.setObject(i, jdbcEncodeInstant(v), jdbcTypeOfInstant) }
  override val OffsetTimeEncoder: JdbcEncoder<OffsetTime> = JdbcEncoder.fromFunction(jdbcTypeOfOffsetTime) { _, ps, v, i -> ps.setObject(i, v, jdbcTypeOfOffsetTime) }
  override val OffsetDateTimeEncoder: JdbcEncoder<OffsetDateTime> = JdbcEncoder.fromFunction(jdbcTypeOfOffsetDateTime) { _, ps, v, i -> ps.setObject(i, v, jdbcTypeOfOffsetDateTime) }

  override val encoders by lazy { super.encoders + setOf(SqlDateEncoder) + (additionalEncoders as Set<SqlEncoder<Connection, PreparedStatement, Any>>) }
}

open class JdbcEncodersWithTimeLegacy(override val dateTimeZone: TimeZone, val additionalEncoders: Set<SqlEncoder<Connection, PreparedStatement, *>>): JdbcEncodersBasic() {
  /** Convenience instance so you can reference encoders that are not dependent on dateTimeZone/additionalEncoders */
  companion object: JdbcEncodersWithTimeLegacy(TimeZone.getDefault(), emptySet())

  override val LocalDateEncoder: JdbcEncoder<LocalDate> = JdbcEncoder.fromFunction(Types.DATE) { _, ps, v, i -> ps.setDate(i, java.sql.Date.valueOf(v)) }
  override val LocalTimeEncoder: JdbcEncoder<LocalTime> = JdbcEncoder.fromFunction(Types.TIME) { _, ps, v, i -> ps.setTime(i, java.sql.Time.valueOf(v)) }
  override val LocalDateTimeEncoder: JdbcEncoder<LocalDateTime> = JdbcEncoder.fromFunction(Types.TIMESTAMP) { _, ps, v, i -> ps.setTimestamp(i, Timestamp.valueOf(v)) }
  override val ZonedDateTimeEncoder: JdbcEncoder<ZonedDateTime> = JdbcEncoder.fromFunction(Types.TIMESTAMP) { _, ps, v, i -> ps.setTimestamp(i, Timestamp.from(v.toInstant())) }
  val SqlDateEncoder: JdbcEncoder<java.sql.Date> = JdbcEncoder.fromFunction(Types.DATE) { _, ps, v, i -> ps.setDate(i, v) }

  override val InstantEncoder: JdbcEncoder<Instant> = JdbcEncoder.fromFunction(Types.TIMESTAMP) { _, ps, v, i -> ps.setTimestamp(i, Timestamp.from(v)) }
  override val OffsetTimeEncoder: JdbcEncoder<OffsetTime> = JdbcEncoder.fromFunction(Types.TIME) { _, ps, v, i -> java.sql.Time.valueOf(v.withOffsetSameInstant(ZoneOffset.UTC).toLocalTime()) }
  override val OffsetDateTimeEncoder: JdbcEncoder<OffsetDateTime> = JdbcEncoder.fromFunction(Types.TIMESTAMP) { _, ps, v, i -> ps.setTimestamp(i, java.sql.Timestamp.from(v.toInstant())) }

  override val encoders by lazy { super.encoders + setOf(SqlDateEncoder) + (additionalEncoders as Set<SqlEncoder<Connection, PreparedStatement, Any>>) }
}
