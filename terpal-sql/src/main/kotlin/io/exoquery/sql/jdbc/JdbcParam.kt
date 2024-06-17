package io.exoquery.sql.jdbc

import io.exoquery.sql.SqlEncoder
import io.exoquery.sql.Param
import java.sql.Connection
import java.sql.PreparedStatement

data class JdbcParam<T: Any>(override val value: T, override val encoder: SqlEncoder<Connection, PreparedStatement, T>): Param<Connection, PreparedStatement, T>