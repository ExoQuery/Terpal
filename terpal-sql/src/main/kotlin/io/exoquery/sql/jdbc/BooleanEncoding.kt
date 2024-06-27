package io.exoquery.sql.jdbc

import io.exoquery.sql.BooleanDecoders
import io.exoquery.sql.BooleanEncoders
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

object AsObjectBooleanEncoding: BooleanEncoders<Connection, PreparedStatement> {
  override val BooleanEncoder: JdbcEncoderAny<Boolean> = JdbcEncoderAny.fromFunction(Types.BOOLEAN) { ctx, v, i -> ctx.stmt.setBoolean(i, v) }
}

object AsObjectBooleanDecoding: BooleanDecoders<Connection, ResultSet> {
  override val BooleanDecoder: JdbcDecoderAny<Boolean> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getBoolean(i) }
}

object AsIntBooleanEncoding: BooleanEncoders<Connection, PreparedStatement> {
  override val BooleanEncoder: JdbcEncoderAny<Boolean> = JdbcEncoderAny.fromFunction(Types.INTEGER) { ctx, v, i -> ctx.stmt.setInt(i, if (v) 1 else 0) }
}

object AsIntBooleanDecoding: BooleanDecoders<Connection, ResultSet>{
  override val BooleanDecoder: JdbcDecoderAny<Boolean> = JdbcDecoderAny.fromFunction { ctx, i -> ctx.row.getInt(i) == 1 }
}