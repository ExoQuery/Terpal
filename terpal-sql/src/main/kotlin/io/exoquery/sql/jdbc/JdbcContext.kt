package io.exoquery.sql.jdbc

import io.exoquery.sql.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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

  suspend fun <T> FlowCollector<T>.emitResultSet(conn: Connection, rs: ResultSet, extract: (Connection, ResultSet) -> T) {
    while (rs.next()) {
      emit(extract(conn, rs))
    }
  }

  protected suspend fun localConnection() =
    coroutineContext.get(sessionKey)?.session ?: error("No connection detected in withConnection scope. This should be impossible.")


  private fun <T> runQueryScoped(conn: Connection, sql: String, params: List<Param<*, *, *>>, extract: (Connection, ResultSet) -> T): Flow<T> =
    flow {
      makeStmt(sql, conn).use { stmt ->
        prepare(stmt, conn, params)
        stmt.executeQuery().use { rs ->
          emitResultSet(conn, rs, extract)
        }
      }
    }

  private suspend fun <T> runUpdateReturningScoped(sql: String, params: List<Param<*, *, *>>, returningBehavior: ReturnAction, extract: (Connection, ResultSet) -> T): Flow<T> =
    flow {
      withConnection {
        val conn = localConnection()
        makeStmtReturning(sql, conn, returningBehavior).use { stmt ->
          prepare(stmt, conn, params)
          stmt.executeUpdate()
          emitResultSet(conn, stmt.generatedKeys, extract)
        }
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
      flow {
        makeStmtReturning(sql, conn, returningBehavior).use { stmt ->
          batches.forEach { makeBatch ->
            val batch = makeBatch()
            prepare(stmt, conn, batch)
            stmt.addBatch()
          }
          stmt.executeBatch()
          emitResultSet(conn, stmt.generatedKeys, extract)
        }
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
      withConnection {
        val conn = localConnection()
        runQueryScoped(conn, query.sql, query.params, query.makeExtractor())
      }
    }

  suspend fun <T> run(query: Query<T>): List<T> =
    withContext(Dispatchers.IO) {
      withConnection {
        val conn = localConnection()
        runQueryScoped(conn, query.sql, query.params, query.makeExtractor()).toList()
      }
    }

  // This will cause the former to run: flow.flowOn(CoroutineSession(localConnection()) + Dispatchers.IO)
  suspend fun <T> run2(query: Query<T>): List<T> {
    return withConnection {
      run3(query)
    }
  }

  suspend fun <T> run3(query: Query<T>): List<T> {
    val flow =
      flow {
        val conn = localConnection()
        makeStmt(query.sql, conn).use { stmt ->
          prepare(stmt, conn, query.params)
          stmt.executeQuery().use { rs ->
            emitResultSet(conn, rs, query.makeExtractor())
          }
        }
      }
    val flowOn = if (coroutineContext.hasOpenConnection()) {
      flow.flowOn(CoroutineSession(localConnection()) + Dispatchers.IO)
    } else {
      flow.flowOn(CoroutineSession(newSession()) + Dispatchers.IO)
    }
    return flowOn.toList()
  }

  suspend fun <T> run(query: Action<T>): Int =
    withContext(Dispatchers.IO) {
      runUpdateScoped(query.sql, query.params)
    }
}