package io.exoquery.sql.jdbc

import io.exoquery.sql.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.*
import java.time.*
import kotlin.coroutines.AbstractCoroutineContextElement


interface ContextParams<Session, Stmt> {
  fun param(value: Boolean): Param<Session, Stmt, Boolean>
  fun param(value: Byte): Param<Session, Stmt, Byte>
  fun param(value: Char): Param<Session, Stmt, Char>
  fun param(value: Double): Param<Session, Stmt, Double>
  fun param(value: Float): Param<Session, Stmt, Float>
  fun param(value: Int): Param<Session, Stmt, Int>
  fun param(value: Long): Param<Session, Stmt, Long>
  fun param(value: Short): Param<Session, Stmt, Short>
  fun param(value: String): Param<Session, Stmt, String>

  fun param(value: LocalDate): Param<Session, Stmt, LocalDate>
  fun param(value: LocalTime): Param<Session, Stmt, LocalTime>
  fun param(value: LocalDateTime): Param<Session, Stmt, LocalDateTime>
  fun param(value: ZonedDateTime): Param<Session, Stmt, ZonedDateTime>

  fun param(value: Instant): Param<Session, Stmt, Instant>
  fun param(value: OffsetTime): Param<Session, Stmt, OffsetTime>
  fun param(value: OffsetDateTime): Param<Session, Stmt, OffsetDateTime>
}


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
      withContext(coroutineContext) { block() }
    } else {
      val session = newSession()
      try {
        withContext(CoroutineSession(session)) { block() }
      } finally { closeSession(session) }
    }
  }

  protected fun CoroutineContext.hasOpenConnection(): Boolean {
    val session = get(sessionKey)?.session
    return session != null && !isClosedSession(session)
  }

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
}
