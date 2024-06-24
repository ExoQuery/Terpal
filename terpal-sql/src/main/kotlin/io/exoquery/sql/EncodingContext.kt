package io.exoquery.sql

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Types
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

open class EncodingContext<Session, Stmt>(open val session: Session, open val stmt: Stmt, open val timezone: TimeZone, open val jdbcType: Int) {
  // TODO extract these into a parametrized object on this thing i.e. EncodingContext<AdditionalData>
  val jdbcTypeOfLocalDate     = Types.DATE
  val jdbcTypeOfLocalTime     = Types.TIME
  val jdbcTypeOfLocalDateTime = Types.TIMESTAMP
  val jdbcTypeOfZonedDateTime = Types.TIMESTAMP_WITH_TIMEZONE
  val jdbcTypeOfInstant                      = Types.TIMESTAMP_WITH_TIMEZONE
  val jdbcTypeOfOffsetTime                   = Types.TIME_WITH_TIMEZONE
  val jdbcTypeOfOffsetDateTime               = Types.TIMESTAMP_WITH_TIMEZONE
  fun jdbcEncodeInstant(value: Instant): Any = value.atOffset(ZoneOffset.UTC)
}
data class DecodingContext<Session, Row>(val session: Session, val row: Row, val timezone: TimeZone, val jdbcType: Int)
