package io.exoquery.sql.jdbc

import io.exoquery.sql.SqlEncoder
import io.exoquery.sql.SqlEncoders
import java.math.BigDecimal
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.*
import java.util.*

// Note that T can't extend Any because then T will not be allowed to be null when it is being decoded
abstract class JdbcEncoder<T: Any>: SqlEncoder<Connection, PreparedStatement, T>() {

  override fun asNullable(): SqlEncoder<Connection, PreparedStatement, T?> =
    object: SqlEncoder<Connection, PreparedStatement, T?>() {
      override val type = this@JdbcEncoder.type
      override fun asNullable(): SqlEncoder<Connection, PreparedStatement, T?> = this

      override fun encode(ctx: JdbcEncodingContext, value: T?, index: Int) =
        if (value != null)
          this@JdbcEncoder.encode(ctx, value, index)
        else
          ctx.stmt.setNull(index, ctx.jdbcType)
    }

  inline fun <reified R: Any> contramap(crossinline f: (R) -> T):JdbcEncoder<R> =
    object: JdbcEncoder<R>() {
      override val type = R::class
      // Get the JDBC type from the parent. This makes sense because most of the time contramapped encoders are from primivites
      // e.g. StringDecoder.contramap { ... } so we want the jdbc type from the parent.
      override fun encode(ctx: JdbcEncodingContext, value: R, index: Int) =
        this@JdbcEncoder.encode(ctx, f(value), index)
    }

  /*
  expected:<[EncodingTestEntity(v1=s, v2=1.1, v3=true, v4=11, v5=23, v6=33, v7=431, v8=34.4, v9=42.0, v10=[1, 2], v11=Fri Nov 22 19:00:00 EST 2013, v12=EncodingTestType(value=s), v13=2013-11-23, v14=173fb134-c84b-4bb2-be5c-85b2552f60b5, o1=s, o2=1.1, o3=true, o4=11, o5=23, o6=33, o7=431, o8=34.4, o9=42.0, o10=[1, 2], o11=Fri Nov 22 19:00:00 EST 2013, o12=EncodingTestType(value=s), o13=2013-11-23, o14=348e85a2-d953-4cb6-a2ff-a90f02006eb4),
             EncodingTestEntity(v1=, v2=0, v3=false, v4=0, v5=0, v6=0, v7=0, v8=0.0, v9=0.0, v10=[], v11=1969-12-31, v12=EncodingTestType(value=), v13=1970-01-01, v14=00000000-0000-0000-0000-000000000000, o1=null, o2=null, o3=null, o4=null, o5=null, o6=null, o7=null, o8=null, o9=null, o10=null, o11=null, o12=null, o13=null, o14=null)]> but was:<[EncodingTestEntity(v1=s, v2=1.10, v3=true, v4=11, v5=23, v6=33, v7=431, v8=34.4, v9=42.0, v10=[1, 2], v11=Fri Nov 22 19:00:00 EST 2013, v12=EncodingTestType(value=s), v13=2013-11-23, v14=173fb134-c84b-4bb2-be5c-85b2552f60b5, o1=s, o2=1.10, o3=true, o4=null, o5=null, o6=null, o7=null, o8=null, o9=null, o10=null, o11=null, o12=null, o13=null, o14=null), EncodingTestEntity(v1=, v2=0.00, v3=false, v4=0, v5=0, v6=0, v7=0, v8=0.0, v9=0.0, v10=[], v11=Wed Dec 31 19:00:00 EST 1969, v12=EncodingTestType(value=), v13=1970-01-01, v14=00000000-0000-0000-0000-000000000000, o1=null, o2=null, o3=null, o4=null, o5=null, o6=null, o7=null, o8=null, o9=null, o10=null, o11=null, o12=null, o13=null, o14=null)]>
   */

  companion object {
    inline fun <reified T: Any> fromFunction(crossinline f: (JdbcEncodingContext, T, Int) -> Unit): JdbcEncoder<T> =
      object: JdbcEncoder<T>() {
        override val type = T::class
        override fun encode(ctx: JdbcEncodingContext, value: T, index: Int) =
          f(ctx, value, index)
      }
  }
}

interface JdbcEncodersBasic: SqlEncoders<Connection, PreparedStatement> {
  companion object {
    val BooleanEncoder: JdbcEncoder<Boolean> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setBoolean(i, v) }
    val ByteEncoder: JdbcEncoder<Byte> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setByte(i, v) }
    val CharEncoder: JdbcEncoder<Char> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setString(i, v.toString()) }
    val DoubleEncoder: JdbcEncoder<Double> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setDouble(i, v) }
    val FloatEncoder: JdbcEncoder<Float> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setFloat(i, v) }
    val IntEncoder: JdbcEncoder<Int> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setInt(i, v) }
    val LongEncoder: JdbcEncoder<Long> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setLong(i, v) }
    val ShortEncoder: JdbcEncoder<Short> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setShort(i, v) }
    val StringEncoder: JdbcEncoder<String> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setString(i, v) }
    val BigDecimalEncoder: JdbcEncoder<BigDecimal> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setBigDecimal(i, v) }
    val ByteArrayEncoder: JdbcEncoder<ByteArray> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setBytes(i, v) }
    val DateEncoder: JdbcEncoder<java.util.Date> =
      JdbcEncoder.fromFunction { ctx, v, i ->
        ctx.stmt.setTimestamp(i, java.sql.Timestamp(v.getTime()), Calendar.getInstance(ctx.timezone))
      }
  }

  override val BooleanEncoder: JdbcEncoder<Boolean> get() = JdbcEncodersBasic.BooleanEncoder
  override val ByteEncoder: JdbcEncoder<Byte> get() = JdbcEncodersBasic.ByteEncoder
  override val CharEncoder: JdbcEncoder<Char> get() = JdbcEncodersBasic.CharEncoder
  override val DoubleEncoder: JdbcEncoder<Double> get() = JdbcEncodersBasic.DoubleEncoder
  override val FloatEncoder: JdbcEncoder<Float> get() = JdbcEncodersBasic.FloatEncoder
  override val IntEncoder: JdbcEncoder<Int> get() = JdbcEncodersBasic.IntEncoder
  override val LongEncoder: JdbcEncoder<Long> get() = JdbcEncodersBasic.LongEncoder
  override val ShortEncoder: JdbcEncoder<Short> get() = JdbcEncodersBasic.ShortEncoder
  override val StringEncoder: JdbcEncoder<String> get() = JdbcEncodersBasic.StringEncoder
  override val BigDecimalEncoder: JdbcEncoder<BigDecimal> get() = JdbcEncodersBasic.BigDecimalEncoder
  override val ByteArrayEncoder: JdbcEncoder<ByteArray> get() = JdbcEncodersBasic.ByteArrayEncoder
  override val DateEncoder: JdbcEncoder<java.util.Date> get() = JdbcEncodersBasic.DateEncoder
}

interface JdbcEncodersWithTime: JdbcEncodersBasic {
  companion object {
    val LocalDateEncoder: JdbcEncoder<LocalDate> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setObject(i, v, ctx.jdbcTypeOfLocalDate) }
    val LocalTimeEncoder: JdbcEncoder<LocalTime> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setObject(i, v, ctx.jdbcTypeOfLocalTime) }
    val LocalDateTimeEncoder: JdbcEncoder<LocalDateTime> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setObject(i, v, ctx.jdbcTypeOfLocalDateTime) }
    val ZonedDateTimeEncoder: JdbcEncoder<ZonedDateTime> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setObject(i, v, ctx.jdbcTypeOfZonedDateTime) }
    val SqlDateEncoder: JdbcEncoder<java.sql.Date> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setDate(i, v) }

    val InstantEncoder: JdbcEncoder<Instant> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setObject(i, ctx.jdbcEncodeInstant(v), ctx.jdbcTypeOfInstant) }
    val OffsetTimeEncoder: JdbcEncoder<OffsetTime> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setObject(i, v, ctx.jdbcTypeOfOffsetTime) }
    val OffsetDateTimeEncoder: JdbcEncoder<OffsetDateTime> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setObject(i, v, ctx.jdbcTypeOfOffsetDateTime) }
  }

  override val LocalDateEncoder get() = JdbcEncodersWithTime.LocalDateEncoder
  override val LocalTimeEncoder get() = JdbcEncodersWithTime.LocalTimeEncoder
  override val LocalDateTimeEncoder get() = JdbcEncodersWithTime.LocalDateTimeEncoder
  override val ZonedDateTimeEncoder get() = JdbcEncodersWithTime.ZonedDateTimeEncoder
  val SqlDateEncoder: JdbcEncoder<java.sql.Date> get() = JdbcEncodersWithTime.SqlDateEncoder

  override val InstantEncoder: JdbcEncoder<Instant> get() = JdbcEncodersWithTime.InstantEncoder
  override val OffsetTimeEncoder: JdbcEncoder<OffsetTime> get() = JdbcEncodersWithTime.OffsetTimeEncoder
  override val OffsetDateTimeEncoder: JdbcEncoder<OffsetDateTime> get() = JdbcEncodersWithTime.OffsetDateTimeEncoder

  override fun computeEncoders() = super.computeEncoders() + setOf(SqlDateEncoder)
}

interface JdbcEncodersWithTimeLegacy: JdbcEncodersBasic {
  companion object {
    val LocalDateEncoder: JdbcEncoder<LocalDate> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setDate(i, java.sql.Date.valueOf(v)) }
    val LocalTimeEncoder: JdbcEncoder<LocalTime> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setTime(i, java.sql.Time.valueOf(v)) }
    val LocalDateTimeEncoder: JdbcEncoder<LocalDateTime> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setTimestamp(i, Timestamp.valueOf(v)) }
    val ZonedDateTimeEncoder: JdbcEncoder<ZonedDateTime> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setTimestamp(i, Timestamp.from(v.toInstant())) }
    val SqlDateEncoder: JdbcEncoder<java.sql.Date> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setDate(i, v) }

    val InstantEncoder: JdbcEncoder<Instant> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setTimestamp(i, Timestamp.from(v)) }
    val OffsetTimeEncoder: JdbcEncoder<OffsetTime> = JdbcEncoder.fromFunction { ctx, v, i -> java.sql.Time.valueOf(v.withOffsetSameInstant(ZoneOffset.UTC).toLocalTime()) }
    val OffsetDateTimeEncoder: JdbcEncoder<OffsetDateTime> = JdbcEncoder.fromFunction { ctx, v, i -> ctx.stmt.setTimestamp(i, java.sql.Timestamp.from(v.toInstant())) }
  }

  override val LocalDateEncoder get() = JdbcEncodersWithTimeLegacy.LocalDateEncoder
  override val LocalTimeEncoder get() = JdbcEncodersWithTimeLegacy.LocalTimeEncoder
  override val LocalDateTimeEncoder get() = JdbcEncodersWithTimeLegacy.LocalDateTimeEncoder
  override val ZonedDateTimeEncoder get() = JdbcEncodersWithTimeLegacy.ZonedDateTimeEncoder
  val SqlDateEncoder: JdbcEncoder<java.sql.Date> get() = JdbcEncodersWithTimeLegacy.SqlDateEncoder

  override val InstantEncoder: JdbcEncoder<Instant> get() = JdbcEncodersWithTimeLegacy.InstantEncoder
  override val OffsetTimeEncoder: JdbcEncoder<OffsetTime> get() = JdbcEncodersWithTimeLegacy.OffsetTimeEncoder
  override val OffsetDateTimeEncoder: JdbcEncoder<OffsetDateTime> get() = JdbcEncodersWithTimeLegacy.OffsetDateTimeEncoder

  override fun computeEncoders() = super.computeEncoders() + setOf(SqlDateEncoder)
}
