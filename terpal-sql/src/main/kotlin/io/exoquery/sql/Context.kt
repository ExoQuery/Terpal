package io.exoquery.sql

import io.exoquery.sql.jdbc.CoroutineTransaction
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.experimental.ExperimentalTypeInference


abstract class Context<Session, Database> {
  abstract val database: Database
  abstract fun newSession(): Session

  abstract fun closeSession(session: Session): Unit
  abstract fun isClosedSession(session: Session): Boolean
  abstract internal suspend fun <T> runTransactionally(block: suspend CoroutineScope.() -> T): T

  abstract val sessionKey: CoroutineContext.Key<CoroutineSession<Session>>

  inner class CoroutineSession<Session>(val session: Session) : AbstractCoroutineContextElement(sessionKey) {
    override fun toString() = "CoroutineSession($sessionKey)"
  }

  protected suspend inline fun <T> withConnection(crossinline block: suspend CoroutineScope.() -> T): T {
    return if (coroutineContext.hasOpenConnection()) {
      withContext(coroutineContext + Dispatchers.IO) { block() }
    } else {
      val session = newSession()
      try {
        withContext(CoroutineSession(session) + Dispatchers.IO) { block() }
      } finally { closeSession(session) }
    }
  }

  protected fun CoroutineContext.hasOpenConnection(): Boolean {
    val session = get(sessionKey)?.session
    return session != null && !isClosedSession(session)
  }

  suspend fun <T> withTransactionScope(block: suspend CoroutineScope.() -> T): T {
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

  protected suspend fun localConnection() =
    coroutineContext.get(sessionKey)?.session ?: error("No connection detected in withConnection scope. This should be impossible.")

  @OptIn(ExperimentalTypeInference::class)
  protected suspend fun <T> flowWithConnection(@BuilderInference block: suspend FlowCollector<T>.() -> Unit): Flow<T> {
    val flowInvoke = flow(block)
    return if (coroutineContext.hasOpenConnection()) {
      flowInvoke.flowOn(CoroutineSession(localConnection()) + Dispatchers.IO)
    } else {
      val session = newSession()
      flowInvoke.flowOn(CoroutineSession(session) + Dispatchers.IO).onCompletion { _ ->
        // It is interesting to note that in some implemenations of JDBC (e.g. MySQL) a connection will be returned to the pool
        // when transaction commit() happens (given that the actual transaction that occured BEFORE it succeeds and the commit() IO command itself succeeds).
        // In these sams implementations (e.g. MySQL), if a rollback() happens, the connection will not be returned to the pool and needs to be manually closed.
        // This behavior happens only when AutoCommit is disabled (which makes sense given that when AutoCommit=true the commit()/rollback() commands are moot).
        // Therefore it is very important to close the session here.
        // An interesting question to explore is if we can use session.use{} here instead of closeSession(session) and how that interacts with the mechanics
        // of the flowOn operator. Currently we cannot do this because session does not implement Closeable.
        closeSession(session)
      }
    }
  }
}
