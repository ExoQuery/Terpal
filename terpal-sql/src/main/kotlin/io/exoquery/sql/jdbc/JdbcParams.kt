package io.exoquery.sql.jdbc

import io.exoquery.sql.ContextParams
import io.exoquery.sql.SqlEncoders
import java.sql.*
import java.time.*


abstract class JdbcParams(val encoders: SqlEncoders<Connection, PreparedStatement>): ContextParams<Connection, PreparedStatement> {
  inline fun <reified T: Any> param(value: T, crossinline encoder: (Connection, PreparedStatement, T, Int) -> Unit): JdbcParam<T> = JdbcParam<T>(value, JdbcEncoder.fromFunction(encoder))

  override fun param(value: Boolean): JdbcParam<Boolean> = JdbcParam<Boolean>(value, encoders.BooleanEncoder)
  override fun param(value: Byte): JdbcParam<Byte> = JdbcParam<Byte>(value, encoders.ByteEncoder)
  override fun param(value: Char): JdbcParam<Char> = JdbcParam<Char>(value, encoders.CharEncoder)
  override fun param(value: Double): JdbcParam<Double> = JdbcParam<Double>(value, encoders.DoubleEncoder)
  override fun param(value: Float): JdbcParam<Float> = JdbcParam<Float>(value, encoders.FloatEncoder)
  override fun param(value: Int): JdbcParam<Int> = JdbcParam<Int>(value, encoders.IntEncoder)
  override fun param(value: Long): JdbcParam<Long> = JdbcParam<Long>(value, encoders.LongEncoder)
  override fun param(value: Short): JdbcParam<Short> = JdbcParam<Short>(value, encoders.ShortEncoder)
  override fun param(value: String): JdbcParam<String> = JdbcParam<String>(value, encoders.StringEncoder)

  override fun param(value: LocalDate): JdbcParam<LocalDate> = JdbcParam(value, encoders.LocalDateEncoder)
  override fun param(value: LocalTime): JdbcParam<LocalTime> = JdbcParam(value, encoders.LocalTimeEncoder)
  override fun param(value: LocalDateTime): JdbcParam<LocalDateTime> = JdbcParam(value, encoders.LocalDateTimeEncoder)
  override fun param(value: ZonedDateTime): JdbcParam<ZonedDateTime> = JdbcParam(value, encoders.ZonedDateTimeEncoder)

  override fun param(value: Instant): JdbcParam<Instant> = JdbcParam(value, encoders.InstantEncoder)
  override fun param(value: OffsetTime): JdbcParam<OffsetTime> = JdbcParam(value, encoders.OffsetTimeEncoder)
  override fun param(value: OffsetDateTime): JdbcParam<OffsetDateTime> = JdbcParam(value, encoders.OffsetDateTimeEncoder)
}
