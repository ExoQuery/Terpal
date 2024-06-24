package io.exoquery.sql.jdbc

import io.exoquery.sql.DecodingContext
import io.exoquery.sql.EncodingContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Types

typealias JdbcEncodingContext = EncodingContext<Connection, PreparedStatement>
typealias JdbcDecodingContext = DecodingContext<Connection, java.sql.ResultSet>

object StandardJdbcTypes {
  val Boolean = Boolean::class to Types.BOOLEAN
  val Byte = Byte::class to Types.TINYINT
  val Char = Char::class to Types.VARCHAR
  val Double = Double::class to Types.DOUBLE
  val Float = Float::class to Types.FLOAT
  val Int = Int::class to Types.INTEGER
  val Long = Long::class to Types.BIGINT
  val Short = Short::class to Types.SMALLINT
  val String = String::class to Types.VARCHAR
  val BigDecimal = java.math.BigDecimal::class to Types.NUMERIC
  val ByteArray = ByteArray::class to Types.VARBINARY
  val Date = java.util.Date::class to Types.TIMESTAMP
}