package io.exoquery.sql.jdbc

import io.exoquery.sql.Action
import io.exoquery.sql.JdbcRowDecoder
import io.exoquery.sql.Param
import io.exoquery.sql.Query
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import javax.sql.DataSource
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

sealed interface ReturnAction {
  // Used for Query and non-returning actions
  data object ReturnDefault: ReturnAction
  data class ReturnColumns(val columns: List<String>): ReturnAction
  data object ReturnRecord: ReturnAction
}


open class JdbcContext(override val database: DataSource): Context<Connection, DataSource>() {
  object JdbcContextEncoders: JdbcEncodersWithTime()
  companion object Params: JdbcParams(JdbcContextEncoders)

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
  fun <T: Any> JdbcParam<T>.write(index: Int, conn: Connection, ps: PreparedStatement): Unit =
    encoder.encode(conn, ps, value, index+1)

  protected fun makeStmtReturning(sql: String, conn: Connection, returningBehavior: ReturnAction) =
    when(returningBehavior) {
      is ReturnAction.ReturnDefault -> conn.prepareStatement(sql)
      is ReturnAction.ReturnColumns -> conn.prepareStatement(sql, returningBehavior.columns.toTypedArray())
      is ReturnAction.ReturnRecord -> conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)
    }

  protected fun makeStmt(sql: String, conn: Connection) =
    makeStmtReturning(sql, conn, ReturnAction.ReturnDefault)

  protected fun prepare(stmt: PreparedStatement, conn: Connection, params: List<Param<*, *, *>>) =
    params.withIndex().forEach { (idx, param) ->
      when (param) {
        is JdbcParam<*> -> param.write(idx, conn, stmt)
        else -> throw IllegalArgumentException("The parameter $param needs to be a JdbcParam type but it is ${param::class}.")
      }
    }

  private fun <T> ResultSet.toFlow(conn: Connection, extract: (Connection, ResultSet) -> T): Flow<T> =
    flow {
      while (this@ResultSet.next()) {
        emit(extract(conn, this@ResultSet))
      }
    }

  protected suspend fun localConnection() =
    coroutineContext.get(sessionKey)?.session ?: error("No connection detected in withConnection scope. This should be impossible.")

  private suspend fun <T> runQueryScoped(sql: String, params: List<Param<*, *, *>>, extract: (Connection, ResultSet) -> T): Flow<T> =
    withConnection {
      val conn = localConnection()
      makeStmt(sql, conn).use { stmt ->
        prepare(stmt, conn, params)
        stmt.executeQuery().use { rs ->
          rs.toFlow(conn, extract)
        }
      }
    }

  private suspend fun <T> runUpdateReturningScoped(sql: String, params: List<Param<*, *, *>>, returningBehavior: ReturnAction, extract: (Connection, ResultSet) -> T): Flow<T> =
    withConnection {
      val conn = localConnection()
      makeStmtReturning(sql, conn, returningBehavior).use { stmt ->
        prepare(stmt, conn, params)
        stmt.executeUpdate()
        stmt.generatedKeys.toFlow(conn, extract)
      }
    }

  private suspend fun updateBatchScoped(sql: String, batches: List<() -> List<Param<*, *, *>>>): List<Int> =
    withConnection {
      val conn = localConnection()
      makeStmt(sql, conn).use { stmt ->
        batches.forEach { makeBatch ->
          val batch = makeBatch()
          prepare(stmt, conn, batch)
          stmt.addBatch()
        }
        stmt.executeBatch().toList()
      }
    }

  private suspend fun <T> updateBatchReturningScoped(sql: String, batches: List<() -> List<Param<*, *, *>>>, returningBehavior: ReturnAction, extract: (Connection, ResultSet) -> T): Flow<T> =
    withConnection {
      val conn = localConnection()
      makeStmtReturning(sql, conn, returningBehavior).use { stmt ->
        batches.forEach { makeBatch ->
          val batch = makeBatch()
          prepare(stmt, conn, batch)
          stmt.addBatch()
        }
        stmt.executeBatch()
        stmt.generatedKeys.toFlow(conn, extract)
      }
    }

  private suspend fun runUpdateScoped(sql: String, params: List<Param<*, *, *>>): Int =
    withConnection {
      val conn = localConnection()
       makeStmt(sql, conn).use { stmt ->
        stmt.executeUpdate()
      }
    }

  protected fun <T> Query<T>.makeExtractor() =
    { conn: Connection, rs: ResultSet ->
      val decoder = JdbcRowDecoder(conn, rs, this.resultMaker.descriptor)
      this.resultMaker.deserialize(decoder)
    }

  suspend fun <T> stream(query: Query<T>): Flow<T> =
    withContext(Dispatchers.IO) {
      runQueryScoped(query.sql, query.params, query.makeExtractor())
    }

  suspend fun <T> run(query: Query<T>): List<T> =
    withContext(Dispatchers.IO) {
      runQueryScoped(query.sql, query.params, query.makeExtractor()).toList()
    }

  suspend fun <T> run(query: Action<T>): Int =
    withContext(Dispatchers.IO) {
      runUpdateScoped(query.sql, query.params)
    }
}