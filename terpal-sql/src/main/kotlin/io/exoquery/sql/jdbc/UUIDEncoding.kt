package io.exoquery.sql.jdbc

import io.exoquery.sql.SqlDecoder
import io.exoquery.sql.SqlEncoder
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.*

object UUIDStringEncoder: SqlEncoder<Connection, PreparedStatement, UUID>() {
  override val type = UUID::class
  override fun encode(conn: Connection, ps: PreparedStatement, value: UUID, index: Int) {
    ps.setString(index, value.toString())
  }
}

object UUIDStringDecoder: SqlDecoder<Connection, ResultSet, UUID>() {
  override val type = UUID::class
  override fun decode(conn: Connection, rs: ResultSet, index: Int): UUID? {
    return UUID.fromString(rs.getString(index))
  }
}

object UUIDObjectEncoder: SqlEncoder<Connection, PreparedStatement, UUID>() {
  override val type = UUID::class
  override fun encode(conn: Connection, ps: PreparedStatement, value: UUID, index: Int) {
    ps.setObject(index, value)
  }
}

object UUIDObjectDecoder: SqlDecoder<Connection, ResultSet, UUID>() {
  override val type = UUID::class
  override fun decode(conn: Connection, rs: ResultSet, index: Int): UUID? {
    return rs.getObject(index, UUID::class.java)
  }
}
