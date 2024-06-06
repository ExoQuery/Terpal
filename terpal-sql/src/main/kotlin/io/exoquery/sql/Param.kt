package io.exoquery.sql

import java.sql.Connection
import java.sql.PreparedStatement
import javax.sql.DataSource
import kotlin.reflect.KClass


interface Decoder<Session, Row, T: Any> {
  val type: KClass<T>
  fun decode(session: Session, row: Row, index: Int): T
}

interface Encoder<Session, Statement, T: Any> {
  val type: KClass<T>
  fun encode(session: Session, statement: Statement, value: T, index: Int): Unit
}

interface Param<Session, Statement, T: Any> {
  val value: T
  val encoder: Encoder<Session, Statement, T>
}

data class JdbcParam<T: Any>(override val value: T, override val encoder: Encoder<Connection, PreparedStatement, T>): Param<Connection, PreparedStatement, T> {

}
