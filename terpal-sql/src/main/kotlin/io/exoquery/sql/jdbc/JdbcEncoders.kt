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

/** Represents a Jdbc encoder with a nullable or non-nulalble input value */
typealias JdbcEncoder<T> = SqlEncoder<Connection, PreparedStatement, T>

/** Represents a Jdbc Encoder with a non-nullable input value */
abstract class JdbcEncoderAny<T: Any>: JdbcEncoder<T>() {
  abstract val jdbcType: Int

  override fun asNullable(): JdbcEncoder<T?> =
    object: JdbcEncoder<T?>() {
      override val type = this@JdbcEncoderAny.type
      val jdbcType = this@JdbcEncoderAny.jdbcType
      override fun asNullable(): SqlEncoder<Connection, PreparedStatement, T?> = this

      override fun encode(ctx: JdbcEncodingContext, value: T?, index: Int) =
        if (value != null)
          this@JdbcEncoderAny.encode(ctx, value, index)
        else
          ctx.stmt.setNull(index, jdbcType)
    }

  inline fun <reified R: Any> contramap(crossinline f: (R) -> T):JdbcEncoderAny<R> =
    object: JdbcEncoderAny<R>() {
      override val type = R::class
      // Get the JDBC type from the parent. This makes sense because most of the time contramapped encoders are from primivites
      // e.g. StringDecoder.contramap { ... } so we want the jdbc type from the parent.
      override val jdbcType = this@JdbcEncoderAny.jdbcType
      override fun encode(ctx: JdbcEncodingContext, value: R, index: Int) =
        this@JdbcEncoderAny.encode(ctx, f(value), index)
    }

  /*
  expected:<[EncodingTestEntity(v1=s, v2=1.1, v3=true, v4=11, v5=23, v6=33, v7=431, v8=34.4, v9=42.0, v10=[1, 2], v11=Fri Nov 22 19:00:00 EST 2013, v12=EncodingTestType(value=s), v13=2013-11-23, v14=173fb134-c84b-4bb2-be5c-85b2552f60b5, o1=s, o2=1.1, o3=true, o4=11, o5=23, o6=33, o7=431, o8=34.4, o9=42.0, o10=[1, 2], o11=Fri Nov 22 19:00:00 EST 2013, o12=EncodingTestType(value=s), o13=2013-11-23, o14=348e85a2-d953-4cb6-a2ff-a90f02006eb4),
             EncodingTestEntity(v1=, v2=0, v3=false, v4=0, v5=0, v6=0, v7=0, v8=0.0, v9=0.0, v10=[], v11=1969-12-31, v12=EncodingTestType(value=), v13=1970-01-01, v14=00000000-0000-0000-0000-000000000000, o1=null, o2=null, o3=null, o4=null, o5=null, o6=null, o7=null, o8=null, o9=null, o10=null, o11=null, o12=null, o13=null, o14=null)]> but was:<[EncodingTestEntity(v1=s, v2=1.10, v3=true, v4=11, v5=23, v6=33, v7=431, v8=34.4, v9=42.0, v10=[1, 2], v11=Fri Nov 22 19:00:00 EST 2013, v12=EncodingTestType(value=s), v13=2013-11-23, v14=173fb134-c84b-4bb2-be5c-85b2552f60b5, o1=s, o2=1.10, o3=true, o4=null, o5=null, o6=null, o7=null, o8=null, o9=null, o10=null, o11=null, o12=null, o13=null, o14=null), EncodingTestEntity(v1=, v2=0.00, v3=false, v4=0, v5=0, v6=0, v7=0, v8=0.0, v9=0.0, v10=[], v11=Wed Dec 31 19:00:00 EST 1969, v12=EncodingTestType(value=), v13=1970-01-01, v14=00000000-0000-0000-0000-000000000000, o1=null, o2=null, o3=null, o4=null, o5=null, o6=null, o7=null, o8=null, o9=null, o10=null, o11=null, o12=null, o13=null, o14=null)]>
   */

  companion object {
    inline fun <reified T: Any> fromFunction(jdbcTypeNum: Int, crossinline f: (JdbcEncodingContext, T, Int) -> Unit): JdbcEncoderAny<T> =
      object: JdbcEncoderAny<T>() {
        override val jdbcType: Int = jdbcTypeNum
        override val type = T::class
        override fun encode(ctx: JdbcEncodingContext, value: T, index: Int) =
          f(ctx, value, index)
      }
  }
}

abstract class JdbcEncodersBasic: SqlEncoders<Connection, PreparedStatement> {
  override val BooleanEncoder: JdbcEncoderAny<Boolean> = JdbcEncoderAny.fromFunction(Types.BOOLEAN) { ctx, v, i -> ctx.stmt.setBoolean(i, v) }
  override val ByteEncoder: JdbcEncoderAny<Byte> = JdbcEncoderAny.fromFunction(Types.TINYINT) { ctx, v, i -> ctx.stmt.setByte(i, v) }
  override val CharEncoder: JdbcEncoderAny<Char> = JdbcEncoderAny.fromFunction(Types.VARCHAR) { ctx, v, i -> ctx.stmt.setString(i, v.toString()) }
  override val DoubleEncoder: JdbcEncoderAny<Double> = JdbcEncoderAny.fromFunction(Types.DOUBLE) { ctx, v, i -> ctx.stmt.setDouble(i, v) }
  override val FloatEncoder: JdbcEncoderAny<Float> = JdbcEncoderAny.fromFunction(Types.FLOAT) { ctx, v, i -> ctx.stmt.setFloat(i, v) }
  override val IntEncoder: JdbcEncoderAny<Int> = JdbcEncoderAny.fromFunction(Types.INTEGER) { ctx, v, i -> ctx.stmt.setInt(i, v) }
  override val LongEncoder: JdbcEncoderAny<Long> = JdbcEncoderAny.fromFunction(Types.BIGINT) { ctx, v, i -> ctx.stmt.setLong(i, v) }
  override val ShortEncoder: JdbcEncoderAny<Short> = JdbcEncoderAny.fromFunction(Types.SMALLINT) { ctx, v, i -> ctx.stmt.setShort(i, v) }
  override val StringEncoder: JdbcEncoderAny<String> = JdbcEncoderAny.fromFunction(Types.VARCHAR) { ctx, v, i -> ctx.stmt.setString(i, v) }
  override val BigDecimalEncoder: JdbcEncoderAny<BigDecimal> = JdbcEncoderAny.fromFunction(Types.NUMERIC) { ctx, v, i -> ctx.stmt.setBigDecimal(i, v) }
  override val ByteArrayEncoder: JdbcEncoderAny<ByteArray> = JdbcEncoderAny.fromFunction(Types.VARBINARY) { ctx, v, i -> ctx.stmt.setBytes(i, v) }
  override val DateEncoder: JdbcEncoderAny<java.util.Date> =
    JdbcEncoderAny.fromFunction(Types.TIMESTAMP) { ctx, v, i ->
      ctx.stmt.setTimestamp(i, java.sql.Timestamp(v.getTime()), Calendar.getInstance(ctx.timeZone))
    }
}

abstract class JdbcEncodersWithTime: JdbcEncodersBasic() {
  companion object: JdbcEncodersWithTime() { override val encoders = computeEncoders() }

  open val jdbcTypeOfLocalDate     = Types.DATE
  open val jdbcTypeOfLocalTime     = Types.TIME
  open val jdbcTypeOfLocalDateTime = Types.TIMESTAMP
  open val jdbcTypeOfZonedDateTime = Types.TIMESTAMP_WITH_TIMEZONE
  open val jdbcTypeOfInstant                      = Types.TIMESTAMP_WITH_TIMEZONE
  open val jdbcTypeOfOffsetTime                   = Types.TIME_WITH_TIMEZONE
  open val jdbcTypeOfOffsetDateTime               = Types.TIMESTAMP_WITH_TIMEZONE
  open val timezone: TimeZone = TimeZone.getDefault()
  open fun jdbcEncodeInstant(value: Instant): Any = value.atOffset(ZoneOffset.UTC)

  override val LocalDateEncoder: JdbcEncoderAny<LocalDate> = JdbcEncoderAny.fromFunction(jdbcTypeOfLocalDate) { ctx, v, i -> ctx.stmt.setObject(i, v, jdbcTypeOfLocalDate) }
  override val LocalTimeEncoder: JdbcEncoderAny<LocalTime> = JdbcEncoderAny.fromFunction(jdbcTypeOfLocalTime) { ctx, v, i -> ctx.stmt.setObject(i, v, jdbcTypeOfLocalTime) }
  override val LocalDateTimeEncoder: JdbcEncoderAny<LocalDateTime> = JdbcEncoderAny.fromFunction(jdbcTypeOfLocalDateTime) { ctx, v, i -> ctx.stmt.setObject(i, v, jdbcTypeOfLocalDateTime) }
  override val ZonedDateTimeEncoder: JdbcEncoderAny<ZonedDateTime> = JdbcEncoderAny.fromFunction(jdbcTypeOfZonedDateTime) { ctx, v, i -> ctx.stmt.setObject(i, v, jdbcTypeOfZonedDateTime) }
  val SqlDateEncoder: JdbcEncoderAny<java.sql.Date> = JdbcEncoderAny.fromFunction(Types.DATE) { ctx, v, i -> ctx.stmt.setDate(i, v) }
  val SqlTimeEncoder: JdbcEncoderAny<java.sql.Time> = JdbcEncoderAny.fromFunction(Types.TIME) { ctx, v, i -> ctx.stmt.setTime(i, v) }
  val SqlTimestampEncoder: JdbcEncoderAny<Timestamp> = JdbcEncoderAny.fromFunction(Types.TIMESTAMP) { ctx, v, i -> ctx.stmt.setTimestamp(i, v) }

  override val InstantEncoder: JdbcEncoderAny<Instant> = JdbcEncoderAny.fromFunction(jdbcTypeOfInstant) { ctx, v, i -> ctx.stmt.setObject(i, jdbcEncodeInstant(v), jdbcTypeOfInstant) }
  override val OffsetTimeEncoder: JdbcEncoderAny<OffsetTime> = JdbcEncoderAny.fromFunction(jdbcTypeOfOffsetTime) { ctx, v, i -> ctx.stmt.setObject(i, v, jdbcTypeOfOffsetTime) }
  override val OffsetDateTimeEncoder: JdbcEncoderAny<OffsetDateTime> = JdbcEncoderAny.fromFunction(jdbcTypeOfOffsetDateTime) { ctx, v, i -> ctx.stmt.setObject(i, v, jdbcTypeOfOffsetDateTime) }

  override fun computeEncoders() = super.computeEncoders() + setOf(SqlDateEncoder, SqlTimeEncoder, SqlTimestampEncoder)
}

abstract class JdbcEncodersWithTimeLegacy: JdbcEncodersBasic() {
  companion object: JdbcEncodersWithTimeLegacy() { override val encoders = computeEncoders() }

  override val LocalDateEncoder: JdbcEncoderAny<LocalDate> = JdbcEncoderAny.fromFunction(Types.DATE) { ctx, v, i -> ctx.stmt.setDate(i, java.sql.Date.valueOf(v)) }
  override val LocalTimeEncoder: JdbcEncoderAny<LocalTime> = JdbcEncoderAny.fromFunction(Types.TIME) { ctx, v, i -> ctx.stmt.setTime(i, java.sql.Time.valueOf(v)) }
  override val LocalDateTimeEncoder: JdbcEncoderAny<LocalDateTime> = JdbcEncoderAny.fromFunction(Types.TIMESTAMP) { ctx, v, i -> ctx.stmt.setTimestamp(i, Timestamp.valueOf(v)) }
  override val ZonedDateTimeEncoder: JdbcEncoderAny<ZonedDateTime> = JdbcEncoderAny.fromFunction(Types.TIMESTAMP) { ctx, v, i -> ctx.stmt.setTimestamp(i, Timestamp.from(v.toInstant())) }
  val SqlDateEncoder: JdbcEncoderAny<java.sql.Date> = JdbcEncoderAny.fromFunction(Types.DATE) { ctx, v, i -> ctx.stmt.setDate(i, v) }
  val SqlTimeEncoder: JdbcEncoderAny<java.sql.Time> = JdbcEncoderAny.fromFunction(Types.TIME) { ctx, v, i -> ctx.stmt.setTime(i, v) }
  val SqlTimestampEncoder: JdbcEncoderAny<Timestamp> = JdbcEncoderAny.fromFunction(Types.TIMESTAMP) { ctx, v, i -> ctx.stmt.setTimestamp(i, v) }

  override val InstantEncoder: JdbcEncoderAny<Instant> = JdbcEncoderAny.fromFunction(Types.TIMESTAMP) { ctx, v, i -> ctx.stmt.setTimestamp(i, Timestamp.from(v)) }
  override val OffsetTimeEncoder: JdbcEncoderAny<OffsetTime> = JdbcEncoderAny.fromFunction(Types.TIME) { ctx, v, i -> ctx.stmt.setTime(i, java.sql.Time.valueOf(v.withOffsetSameInstant(ZoneOffset.UTC).toLocalTime()))  }
  override val OffsetDateTimeEncoder: JdbcEncoderAny<OffsetDateTime> = JdbcEncoderAny.fromFunction(Types.TIMESTAMP) { ctx, v, i -> ctx.stmt.setTimestamp(i, java.sql.Timestamp.from(v.toInstant())) }

  override fun computeEncoders() = super.computeEncoders() + setOf(SqlDateEncoder, SqlTimeEncoder, SqlTimestampEncoder)
}
