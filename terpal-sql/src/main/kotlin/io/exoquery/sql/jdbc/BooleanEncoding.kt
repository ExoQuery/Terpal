package io.exoquery.sql.jdbc

import io.exoquery.sql.BooleanEncoding
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

object JdbcBooleanObjectEncoding: BooleanEncoding<Connection, PreparedStatement, ResultSet> {
  override val BooleanEncoder: JdbcEncoderAny<Boolean> = JdbcEncoderAny.fromFunction(Types.BOOLEAN) { ctx, v, i -> ctx.stmt.setBoolean(i, v) }
  override val BooleanDecoder: JdbcDecoderAny<Boolean> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getBoolean(i) }
}

object JdbcBooleanIntEncoding: BooleanEncoding<Connection, PreparedStatement, ResultSet> {
  override val BooleanEncoder: JdbcEncoderAny<Boolean> = JdbcEncoderAny.fromFunction(Types.INTEGER) { ctx, v, i -> ctx.stmt.setInt(i, if (v) 1 else 0) }
  override val BooleanDecoder: JdbcDecoderAny<Boolean> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getInt(i) == 1 }
}
