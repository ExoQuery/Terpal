package io.exoquery.sql.jdbc

import io.exoquery.sql.*
import java.sql.Connection
import java.sql.PreparedStatement
import kotlin.reflect.full.isSubclassOf

object Sql: SqlJdbcBase(object: JdbcEncodersWithTime() {})
object SqlBatch: SqlJdbcBatchBase(object: JdbcEncodersWithTime() {})


// The Jdbc Specific Sql implemenation which will use the Jdbc wrapping functions to auto-wrap things
abstract class SqlJdbcBase(val encoders: SqlEncoders<Connection, PreparedStatement>): SqlBase() {
  override fun <V: Any> wrap(value: V): SqlFragment {
    val cls = value::class
    // Very not idea. We want to be able to just compare KClass instances directly by getting the
    // IrClass/IrClassSymbol as a KClass when the splicing happened
    val encoder =
      encoders.encoders.find { cls.isSubclassOf(it.type) } ?: throw IllegalArgumentException("Could not find an encoder for the type: ${cls.qualifiedName}")
    @Suppress("UNCHECKED_CAST")
    return run {
      JdbcParam<V>(value, encoder as SqlEncoder<Connection, PreparedStatement, V>)
    }
  }
}

abstract class SqlJdbcBatchBase(val encoders: SqlEncoders<Connection, PreparedStatement>): SqlBatchBase() {
  override fun <V: Any> wrap(value: V): Param<*, *, *> {
    val cls = value::class
    // Very not idea. We want to be able to just compare KClass instances directly by getting the
    // IrClass/IrClassSymbol as a KClass when the splicing happened
    val encoder =
      encoders.encoders.find { cls.isSubclassOf(it.type) } ?: throw IllegalArgumentException("Could not find an encoder for the type: ${cls.qualifiedName}")
    @Suppress("UNCHECKED_CAST")
    return run {
      JdbcParam<V>(value, encoder as SqlEncoder<Connection, PreparedStatement, V>)
    }
  }
}

