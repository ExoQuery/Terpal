package io.exoquery.sql.jdbc

import io.exoquery.sql.SqlDecoder
import io.exoquery.sql.SqlEncoder
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.*

object UUIDStringEncoding {
  val UUIDStringEncoder: SqlEncoder<Connection, PreparedStatement, UUID> =
    JdbcEncoderAny.fromFunction(java.sql.Types.VARCHAR) { ctx, v, i -> ctx.stmt.setString(i, v.toString()) }
  val UUIDStringDecoder: SqlDecoder<Connection, ResultSet, UUID> =
    JdbcDecoderAny.fromFunction { ctx, i -> UUID.fromString(ctx.row.getString(i)) }
}

object UUIDObjectEncoding {
  val UUIDObjectEncoder: SqlEncoder<Connection, PreparedStatement, UUID> =
    JdbcEncoderAny.fromFunction(java.sql.Types.OTHER) { ctx, v, i -> ctx.stmt.setObject(i, v) }
  val UUIDObjectDecoder: SqlDecoder<Connection, ResultSet, UUID> =
    JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getObject(i, UUID::class.java) }
}
