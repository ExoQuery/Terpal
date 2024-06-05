package io.exoquery.sql.jdbc

import io.exoquery.sql.*
import java.sql.Connection
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.*
import java.sql.PreparedStatement
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.sql.DataSource
import kotlin.coroutines.AbstractCoroutineContextElement

class JdbcContext(override val database: DataSource): Context<Connection, DataSource>() {
  companion object Params: JdbcParams

  override fun newSession(): Connection = database.connection
  override fun closeSession(session: Connection): Unit = session.close()
  override fun isClosedSession(session: Connection): Boolean = session.isClosed

  private val JdbcCoroutineContext = object: CoroutineContext.Key<CoroutineSession<Connection>> {}
  override val sessionKey: CoroutineContext.Key<CoroutineSession<Connection>> = JdbcCoroutineContext

  // TODO override by inline despite the warning?
  override internal suspend fun <T> runTransactionally(block: suspend CoroutineScope.() -> T): T {
    val session = coroutineContext.get(sessionKey)?.session ?: error("No connection found")
    session.runWithManualCommit {
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

  internal inline fun <T> Connection.runWithManualCommit(block: Connection.() -> T): T {
    val before = autoCommit

    return try {
      autoCommit = false
      this.run(block)
    } finally {
      autoCommit = before
    }
  }

  // Do it this way so we can avoid value casting in the runScoped function
  fun <T> JdbcParam<T>.write(index: Int, conn: Connection, ps: PreparedStatement): Unit =
    encoder(conn, ps, value, index+1)

  private suspend fun <T> runScoped(query: Query<T>): List<T> {
    val outputs = mutableListOf<T>()
    withConnection {
      val conn = coroutineContext.get(sessionKey)?.session ?: error("No connection detected in withConnection scope. This should be impossible.")
      conn.prepareStatement(query.sql).use { stmt ->
        // prepare params
        query.params.withIndex().forEach { (idx, param) ->
          when (param) {
            is JdbcParam<*> -> param.write(idx, conn, stmt)
            else -> throw IllegalArgumentException("The parameter $param needs to be a JdbcParam type but it is ${param::class}.")
          }
        }
        // execute the query and encode results
        stmt.executeQuery().use { rs ->
          while (rs.next()) {
            val decoder = ResultDecoder(rs, query.resultMaker.descriptor)
            outputs += query.resultMaker.deserialize(decoder)
          }
        }
      }
    }
    return outputs
  }

  suspend fun <T> run(query: Query<T>): Deferred<List<T>> =
    CoroutineScope(Dispatchers.IO).async {
      runScoped(query)
    }
}

interface JdbcParams: ContextParams<Connection, PreparedStatement> {
  override fun <T> param(value: T, encoder: Encoder<Connection, PreparedStatement, T>): Param<Connection, PreparedStatement, T> =
    JdbcParam<T>(value, encoder)

  override fun param(value: Boolean): JdbcParam<Boolean> = JdbcParam<Boolean>(value, { _, ps, v, i -> ps.setBoolean(i, v) } )
  override fun param(value: Byte): JdbcParam<Byte> = JdbcParam<Byte>(value, { _, ps, v, i -> ps.setByte(i, v) } )
  override fun param(value: Char): JdbcParam<Char> = JdbcParam<Char>(value, { _, ps, v, i -> ps.setString(i, v.toString()) } )
  override fun param(value: Double): JdbcParam<Double> = JdbcParam<Double>(value, { _, ps, v, i -> ps.setDouble(i, v) } )
  override fun param(value: Float): JdbcParam<Float> = JdbcParam<Float>(value, { _, ps, v, i -> ps.setFloat(i, v) } )
  override fun param(value: Int): JdbcParam<Int> = JdbcParam<Int>(value, { _, ps, v, i -> ps.setInt(i, v) } )
  override fun param(value: Long): JdbcParam<Long> = JdbcParam<Long>(value, { _, ps, v, i -> ps.setLong(i, v) } )
  override fun param(value: Short): JdbcParam<Short> = JdbcParam<Short>(value, { _, ps, v, i -> ps.setShort(i, v) } )
  override fun param(value: String): JdbcParam<String> = JdbcParam<String>(value, { _, ps, v, i -> ps.setString(i, v) } )

  fun param(value: LocalDate): JdbcParam<LocalDate> = JdbcParam(value, { _, ps, v, i -> ps.setObject(i, v) } )
  fun param(value: LocalTime): JdbcParam<LocalTime> = JdbcParam(value, { _, ps, v, i -> ps.setObject(i, v) } )
  fun param(value: LocalDateTime): JdbcParam<LocalDateTime> = JdbcParam(value, { _, ps, v, i -> ps.setObject(i, v) } )
}


interface ContextParams<Session, Stmt> {
  fun <T> param(value: T, encoder: Encoder<Session, Stmt, T>): Param<Session, Stmt, T>

  fun param(value: Boolean): Param<Session, Stmt, Boolean>
  fun param(value: Byte): Param<Session, Stmt, Byte>
  fun param(value: Char): Param<Session, Stmt, Char>
  fun param(value: Double): Param<Session, Stmt, Double>
  fun param(value: Float): Param<Session, Stmt, Float>
  fun param(value: Int): Param<Session, Stmt, Int>
  fun param(value: Long): Param<Session, Stmt, Long>
  fun param(value: Short): Param<Session, Stmt, Short>
  fun param(value: String): Param<Session, Stmt, String>
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
