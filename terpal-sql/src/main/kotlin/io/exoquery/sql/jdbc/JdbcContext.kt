package io.exoquery.sql.jdbc

import io.exoquery.sql.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Encoder
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


class JdbcContextBuilder {
  var encoders: SqlEncoders<Connection, PreparedStatement> = JdbcEncodersWithTime()
  val decdoers: SqlDecoders<Connection, ResultSet> = JdbcDecodersWithTime()
}


open class JdbcContext(override val database: DataSource): Context<Connection, DataSource>() {
  companion object {
    val Encoders = JdbcEncodersWithTime()
    val Decoders = JdbcDecodersWithTime()
  }

  protected open val encoders: SqlEncoders<Connection, PreparedStatement> = Encoders
  protected open val decoders: SqlDecoders<Connection, ResultSet> = Decoders

  protected open val batchReturnBehavior = ReturnAction.ReturnRecord

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
  @Suppress("UNCHECKED_CAST")
  fun <T: Any> Param<T>.write(index: Int, conn: Connection, ps: PreparedStatement): Unit =
    ((encoders.encoders.find { it.type == this.cls } ?: error("No encoder found for ${this.cls}")) as SqlEncoder<Connection, PreparedStatement, T>)
      .encode(conn, ps, this.value, index+1)

  protected fun makeStmtReturning(sql: String, conn: Connection, returningBehavior: ReturnAction) =
    when(returningBehavior) {
      is ReturnAction.ReturnDefault -> conn.prepareStatement(sql)
      is ReturnAction.ReturnColumns -> conn.prepareStatement(sql, returningBehavior.columns.toTypedArray())
      is ReturnAction.ReturnRecord -> conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)
    }

  protected fun makeStmt(sql: String, conn: Connection) =
    makeStmtReturning(sql, conn, ReturnAction.ReturnDefault)

  protected fun prepare(stmt: PreparedStatement, conn: Connection, params: List<Param<*>>) =
    params.withIndex().forEach { (idx, param) ->
      param.write(idx, conn, stmt)
    }

  suspend fun <T> FlowCollector<T>.emitResultSet(conn: Connection, rs: ResultSet, extract: (Connection, ResultSet) -> T) {
    while (rs.next()) {
      val meta = rs.metaData
      //println("--- Emit: ${(1..meta.columnCount).map { rs.getObject(it) }.joinToString(",")}")
      emit(extract(conn, rs))
    }
  }

  private fun <T> runQueryScoped(conn: Connection, sql: String, params: List<Param<*>>, extract: (Connection, ResultSet) -> T): Flow<T> =
    flow {
      makeStmt(sql, conn).use { stmt ->
        prepare(stmt, conn, params)
        stmt.executeQuery().use { rs ->
          emitResultSet(conn, rs, extract)
        }
      }
    }

  private suspend fun <T> runActionReturningScoped(sql: String, params: List<Param<*>>, returningBehavior: ReturnAction, extract: (Connection, ResultSet) -> T): Flow<T> =
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

  private suspend fun runBatchActionScoped(sql: String, batches: Sequence<List<Param<*>>>): List<Int> =
    withConnection {
      val conn = localConnection()
      makeStmt(sql, conn).use { stmt ->
        batches.forEach { batch ->
          prepare(stmt, conn, batch)
          stmt.addBatch()
        }
        stmt.executeBatch().toList()
      }
    }

  private suspend fun <T> runBatchActionReturningScoped(sql: String, batches: Sequence<List<Param<*>>>, returningBehavior: ReturnAction, extract: (Connection, ResultSet) -> T): Flow<T> =
    flowWithConnection {
      val conn = localConnection()
      makeStmtReturning(sql, conn, returningBehavior).use { stmt ->
        batches.forEach { batch ->
          prepare(stmt, conn, batch)
          stmt.addBatch()
        }
        stmt.executeBatch()
        emitResultSet(conn, stmt.generatedKeys, extract)
      }
    }

  private suspend fun runActionScoped(sql: String, params: List<Param<*>>): Int =
    withConnection {
      val conn = localConnection()
       makeStmt(sql, conn).use { stmt ->
        prepare(stmt, conn, params)
        stmt.executeUpdate()
      }
    }

  protected fun <T> KSerializer<T>.makeExtractor() =
    { conn: Connection, rs: ResultSet ->
      val decoder = JdbcRowDecoder(conn, rs, JdbcContext.Decoders, descriptor)
      deserialize(decoder)
    }

  suspend fun <T> stream(query: Query<T>): Flow<T> =
    flowWithConnection {
      val conn = localConnection()
      makeStmt(query.sql, conn).use { stmt ->
        prepare(stmt, conn, query.params)
        stmt.executeQuery().use { rs ->
          emitResultSet(conn, rs, query.resultMaker.makeExtractor())
        }
      }
    }

  suspend fun <T> stream(query: BatchActionReturning<T>): Flow<T> =
    runBatchActionReturningScoped(query.sql, query.params, batchReturnBehavior, query.resultMaker.makeExtractor())

  suspend fun <T> stream(query: ActionReturning<T>): Flow<T> =
    runActionReturningScoped(query.sql, query.params, batchReturnBehavior, query.resultMaker.makeExtractor())

  suspend fun <T> run(query: Query<T>): List<T> =
    stream(query).toList()

  suspend fun run(query: Action): Int =
    runActionScoped(query.sql, query.params)

  suspend fun run(query: BatchAction): List<Int> =
    runBatchActionScoped(query.sql, query.params)

  suspend fun <T> run(query: ActionReturning<T>): T =
    stream(query).first()

  suspend fun <T> run(query: BatchActionReturning<T>): List<T> =
    stream(query).toList()
}