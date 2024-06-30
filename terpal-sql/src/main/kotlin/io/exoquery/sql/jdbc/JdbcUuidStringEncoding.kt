package io.exoquery.sql.jdbc

import io.exoquery.sql.SqlDecoder
import io.exoquery.sql.SqlEncoder
import io.exoquery.sql.UuidEncoding
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.*

object JdbcUuidStringEncoding: UuidEncoding<Connection, PreparedStatement, ResultSet> {
  override val UuidEncoder: SqlEncoder<Connection, PreparedStatement, UUID> =
    JdbcEncoderAny.fromFunction(java.sql.Types.VARCHAR) { ctx, v, i -> ctx.stmt.setString(i, v.toString()) }
  override val UuidDecoder: SqlDecoder<Connection, ResultSet, UUID> =
    JdbcDecoderAny.fromFunction { ctx, i -> UUID.fromString(ctx.row.getString(i)) }
}

object JdbcUuidObjectEncoding: UuidEncoding<Connection, PreparedStatement, ResultSet> {
  override val UuidEncoder: SqlEncoder<Connection, PreparedStatement, UUID> =
    JdbcEncoderAny.fromFunction(java.sql.Types.OTHER) { ctx, v, i -> ctx.stmt.setObject(i, v) }
  override val UuidDecoder: SqlDecoder<Connection, ResultSet, UUID> =
    JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getObject(i, UUID::class.java) }
}
