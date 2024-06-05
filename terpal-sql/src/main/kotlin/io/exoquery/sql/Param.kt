package io.exoquery.sql

import java.sql.Connection
import java.sql.PreparedStatement
import javax.sql.DataSource

typealias Encoder<Session, Statement, T> = (Session, Statement, T, Int) -> Unit

interface Param<Session, Statement, T> {
  val value: T
  val encoder: Encoder<Session, Statement, T>
}

data class JdbcParam<T>(override val value: T, override val encoder: Encoder<Connection, PreparedStatement, T>): Param<Connection, PreparedStatement, T> {

}
