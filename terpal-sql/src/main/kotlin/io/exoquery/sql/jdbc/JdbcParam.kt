package io.exoquery.sql.jdbc

import io.exoquery.sql.Encoder
import io.exoquery.sql.Param
import java.sql.Connection
import java.sql.PreparedStatement

data class JdbcParam<T: Any>(override val value: T, override val encoder: Encoder<Connection, PreparedStatement, T>): Param<Connection, PreparedStatement, T>