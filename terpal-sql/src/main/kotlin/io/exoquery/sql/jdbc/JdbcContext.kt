package io.exoquery.sql.jdbc

import io.exoquery.sql.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.KSerializer
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.*
import javax.sql.DataSource
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

sealed interface ReturnAction {
  // Used for Query and non-returning actions
  data object ReturnDefault: ReturnAction
  data class ReturnColumns(val columns: List<String>): ReturnAction
  data object ReturnRecord: ReturnAction
}

abstract class JdbcContext(override val database: DataSource): Context<Connection, DataSource>() {
  // Maybe should just have all the encdoers from the base SqlEncoders class an everything introduced after should be added via additionalEncoders.
  // that would make it much easier to reason about what encoders fome from where

  // Need to do this first in iniitalization
  protected open val additionalEncoders: Set<SqlEncoder<Connection, PreparedStatement, out Any>> = setOf()
  protected open val additionalDecoders: Set<SqlDecoder<Connection, ResultSet, out Any>> = setOf()
  protected open val timezone: TimeZone = TimeZone.getDefault()

  protected abstract val encodingApi: SqlEncoding<Connection, PreparedStatement, ResultSet>
  protected open val batchReturnBehavior: ReturnAction = ReturnAction.ReturnRecord

  override open fun newSession(): Connection = database.connection
  override open fun closeSession(session: Connection): Unit = session.close()
  override open fun isClosedSession(session: Connection): Boolean = session.isClosed

  protected open fun createEncodingContext(session: Connection, stmt: PreparedStatement) = EncodingContext(session, stmt, timezone)
  protected open fun createDecodingContext(session: Connection, row: ResultSet) = DecodingContext(session, row, timezone)

  protected val JdbcCoroutineContext = object: CoroutineContext.Key<CoroutineSession<Connection>> {}
  override val sessionKey: CoroutineContext.Key<CoroutineSession<Connection>> = JdbcCoroutineContext

  override open internal suspend fun <T> runTransactionally(block: suspend CoroutineScope.() -> T): T {
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

  protected val allEncoders by lazy { encodingApi.computeEncoders() + additionalEncoders }
  protected val allDecoders by lazy { encodingApi.computeDecoders() + additionalDecoders }

  // Do it this way so we can avoid value casting in the runScoped function
  @Suppress("UNCHECKED_CAST")
  fun <T> Param<T>.write(index: Int, conn: Connection, ps: PreparedStatement): Unit {
    println("----- Preparing parameter $index - $value - using $serializer")
    PreparedStatementElementEncoder(createEncodingContext(conn, ps), index+1, encodingApi, allEncoders).encodeNullableSerializableValue(serializer, value)
  }

  protected open fun makeStmtReturning(sql: String, conn: Connection, returningBehavior: ReturnAction) =
    when(returningBehavior) {
      is ReturnAction.ReturnDefault -> conn.prepareStatement(sql)
      is ReturnAction.ReturnColumns -> conn.prepareStatement(sql, returningBehavior.columns.toTypedArray())
      is ReturnAction.ReturnRecord -> conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)
    }

  protected open fun makeStmt(sql: String, conn: Connection) =
    makeStmtReturning(sql, conn, ReturnAction.ReturnDefault)

  protected open fun prepare(stmt: PreparedStatement, conn: Connection, params: List<Param<*>>) =
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

  protected open fun <T> runQueryScoped(conn: Connection, sql: String, params: List<Param<*>>, extract: (Connection, ResultSet) -> T): Flow<T> =
    flow {
      makeStmt(sql, conn).use { stmt ->
        prepare(stmt, conn, params)
        stmt.executeQuery().use { rs ->
          emitResultSet(conn, rs, extract)
        }
      }
    }

  protected open suspend fun <T> runActionReturningScoped(sql: String, params: List<Param<*>>, returningBehavior: ReturnAction, extract: (Connection, ResultSet) -> T): Flow<T> =
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

  protected open suspend fun runBatchActionScoped(sql: String, batches: Sequence<List<Param<*>>>): List<Int> =
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

  protected open suspend fun <T> runBatchActionReturningScoped(sql: String, batches: Sequence<List<Param<*>>>, returningBehavior: ReturnAction, extract: (Connection, ResultSet) -> T): Flow<T> =
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

  protected open suspend fun runActionScoped(sql: String, params: List<Param<*>>): Int =
    withConnection {
      val conn = localConnection()
       makeStmt(sql, conn).use { stmt ->
        prepare(stmt, conn, params)
        stmt.executeUpdate()
      }
    }

  protected fun <T> KSerializer<T>.makeExtractor() =
    { conn: Connection, rs: ResultSet ->
      val decoder = JdbcRowDecoder(createDecodingContext(conn, rs), encodingApi, allDecoders, descriptor)
      deserialize(decoder)
    }

  internal open suspend fun <T> stream(query: Query<T>): Flow<T> =
    flowWithConnection {
      val conn = localConnection()
      makeStmt(query.sql, conn).use { stmt ->
        prepare(stmt, conn, query.params)
        stmt.executeQuery().use { rs ->
          emitResultSet(conn, rs, query.resultMaker.makeExtractor())
        }
      }
    }

  internal open suspend fun <T> stream(query: BatchActionReturning<T>): Flow<T> = runBatchActionReturningScoped(query.sql, query.params, batchReturnBehavior, query.resultMaker.makeExtractor())
  internal open suspend fun <T> stream(query: ActionReturning<T>): Flow<T> = runActionReturningScoped(query.sql, query.params, batchReturnBehavior, query.resultMaker.makeExtractor())
  internal open suspend fun <T> run(query: Query<T>): List<T> = stream(query).toList()
  internal open suspend fun run(query: Action): Int = runActionScoped(query.sql, query.params)
  internal open suspend fun run(query: BatchAction): List<Int> = runBatchActionScoped(query.sql, query.params)
  internal open suspend fun <T> run(query: ActionReturning<T>): T = stream(query).first()
  internal open suspend fun <T> run(query: BatchActionReturning<T>): List<T> = stream(query).toList()

  suspend open fun <T> transaction(block: suspend ExternalTransactionScope.() -> T): T =
    withTransactionScope {
      val coroutineScope = this
      block(ExternalTransactionScope(coroutineScope, this@JdbcContext))
    }
}

suspend fun <T> Query<T>.runOn(ctx: JdbcContext) = ctx.run(this)
suspend fun <T> Query<T>.streamOn(ctx: JdbcContext) = ctx.stream(this)
suspend fun Action.runOn(ctx: JdbcContext) = ctx.run(this)
suspend fun <T> ActionReturning<T>.runOn(ctx: JdbcContext) = ctx.run(this)
suspend fun BatchAction.runOn(ctx: JdbcContext) = ctx.run(this)
suspend fun <T> BatchActionReturning<T>.runOn(ctx: JdbcContext) = ctx.run(this)
suspend fun <T> BatchActionReturning<T>.streamOn(ctx: JdbcContext) = ctx.stream(this)

data class ExternalTransactionScope(val scope: CoroutineScope, val ctx: JdbcContext) {
  suspend fun <T> Query<T>.run(): List<T> = ctx.run(this)
  suspend fun Action.run(): Int = ctx.run(this)
  suspend fun BatchAction.run(): List<Int> = ctx.run(this)
  suspend fun <T> ActionReturning<T>.run(): T = ctx.run(this)
  suspend fun <T> BatchActionReturning<T>.run(): List<T> = ctx.run(this)
}
