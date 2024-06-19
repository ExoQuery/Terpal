package io.exoquery.sql.jdbc

import io.exoquery.sql.SqlDecoder
import io.exoquery.sql.SqlEncoder
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.*

object UUIDStringEncoding {
  val UUIDStringEncoder: SqlEncoder<Connection, PreparedStatement, UUID> =
    JdbcEncoder.fromFunction(java.sql.Types.VARCHAR) { _, ps, v, i -> ps.setString(i, v.toString()) }
  val UUIDStringDecoder: SqlDecoder<Connection, ResultSet, UUID> =
    JdbcDecoder.fromFunction { _, rs, i -> UUID.fromString(rs.getString(i)) }
}

object UUIDObjectEncoding {
  val UUIDObjectEncoder: SqlEncoder<Connection, PreparedStatement, UUID> =
    JdbcEncoder.fromFunction(java.sql.Types.OTHER) { _, ps, v, i -> ps.setObject(i, v) }
  val UUIDObjectDecoder: SqlDecoder<Connection, ResultSet, UUID> =
    JdbcDecoder.fromFunction { _, rs, i -> rs.getObject(i, UUID::class.java) }
}
