package io.exoquery.sql.jdbc

import io.exoquery.sql.*
import java.sql.Connection
import java.sql.PreparedStatement
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

object Sql: SqlJdbcBase()
object SqlBatch: SqlJdbcBatchBase()


// The Jdbc Specific Sql implemenation which will use the Jdbc wrapping functions to auto-wrap things
abstract class SqlJdbcBase(): SqlBase() {
  override fun <V: Any> wrap(value: V, cls: KClass<V>): SqlFragment {
    // Very not idea. We want to be able to just compare KClass instances directly by getting the
    // IrClass/IrClassSymbol as a KClass when the splicing happened
    return Param<V>(cls, value)
  }
}

abstract class SqlJdbcBatchBase(): SqlBatchBase() {
  override fun <V: Any> wrap(value: V, cls: KClass<V>): Param<*> {
    // Very not idea. We want to be able to just compare KClass instances directly by getting the
    // IrClass/IrClassSymbol as a KClass when the splicing happened
    @Suppress("UNCHECKED_CAST")
    return Param<V>(cls, value)
  }
}

