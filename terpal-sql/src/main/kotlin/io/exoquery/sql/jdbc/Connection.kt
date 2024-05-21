package io.exoquery.sql.jdbc

import java.sql.Connection
import java.sql.SQLException
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import io.exoquery.sql.jdbc.context.*

suspend inline fun <T> withConnection(crossinline block: suspend CoroutineScope.() -> T): T {
  return if (coroutineContext.hasOpenConnection()) {
    withContext(coroutineContext) { block() }
  } else {
    val connection = coroutineContext.dataSource.connection
    try {
      withContext(CoroutineConnection(connection)) { block() }
    } finally { connection.closeCatching() }
  }
}

@PublishedApi
internal fun CoroutineContext.hasOpenConnection(): Boolean {
  val connection = get(CoroutineConnection)?.connection
  return connection != null && !connection.isClosedCatching()
}

@PublishedApi
internal fun Connection.closeCatching() {
  try {
    close()
  } catch (ex: SQLException) {
    //logger.warn(ex) { "Failed to close database connection cleanly:" }
  }
}

@PublishedApi
internal fun Connection.isClosedCatching(): Boolean {
  return try {
    isClosed
  } catch (ex: SQLException) {
    //logger.warn(ex) { "Connection isClosedCatching check failed, assuming closed:" }
    true
  }
}
