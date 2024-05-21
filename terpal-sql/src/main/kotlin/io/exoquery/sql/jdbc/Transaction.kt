package io.exoquery.sql.jdbc

import io.exoquery.sql.jdbc.context.CoroutineTransaction
import io.exoquery.sql.jdbc.context.connection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import java.sql.Connection
import kotlin.coroutines.coroutineContext

suspend fun <T> transaction(block: suspend CoroutineScope.() -> T): T {
  val existingTransaction = coroutineContext[CoroutineTransaction]

  return when {
    existingTransaction == null ->
      withConnection { runTransactionally { block() } }

    // This must mean it's a transaction { stuff... transaction { ... } } so let the outer transaction do the committing
    existingTransaction.incomplete ->
      withContext(coroutineContext) { block() }

    else -> error("Attempted to start new transaction within: $existingTransaction")
  }
}

@PublishedApi
internal inline suspend fun <T> runTransactionally(crossinline block: suspend CoroutineScope.() -> T): T {
  coroutineContext.connection.runWithManualCommit {
    val transaction = CoroutineTransaction()
    try {
      val result = withContext(transaction) { block() }
      commit()
      return result
    } catch (ex: Throwable) {
      rollback()
      throw ex
    } finally {
      transaction.complete()
    }
  }
}

@PublishedApi
internal inline fun <T> Connection.runWithManualCommit(block: Connection.() -> T): T {
  val before = autoCommit

  return try {
    autoCommit = false
    this.run(block)
  } finally {
    autoCommit = before
  }
}
