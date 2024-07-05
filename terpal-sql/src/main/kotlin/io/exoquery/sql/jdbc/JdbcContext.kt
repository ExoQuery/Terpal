package io.exoquery.sql.jdbc

import io.exoquery.sql.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.KSerializer
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*
import javax.sql.DataSource
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

abstract class JdbcContext(override val database: DataSource): Context<Connection, DataSource>() {
  // Maybe should just have all the encdoers from the base SqlEncoders class an everything introduced after should be added via additionalEncoders.
  // that would make it much easier to reason about what encoders fome from where

  // Need to do this first in iniitalization
  protected open val additionalEncoders: Set<SqlEncoder<Connection, PreparedStatement, out Any>> = AdditionaJdbcTimeEncoding.encoders
  protected open val additionalDecoders: Set<SqlDecoder<Connection, ResultSet, out Any>> = AdditionaJdbcTimeEncoding.decoders
  protected open val timezone: TimeZone = TimeZone.getDefault()

  protected abstract val encodingApi: SqlEncoding<Connection, PreparedStatement, ResultSet>

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
    // TODO logging integration
    //println("----- Preparing parameter $index - $value - using $serializer")
    PreparedStatementElementEncoder(createEncodingContext(conn, ps), index+1, encodingApi, allEncoders).encodeNullableSerializableValue(serializer, value)
  }

  protected open fun makeStmtReturning(sql: String, conn: Connection, returningColumns: List<String>) =
    if (returningColumns.isNotEmpty())
      conn.prepareStatement(sql, returningColumns.toTypedArray())
    else
      conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)

  protected open fun makeStmt(sql: String, conn: Connection) =
    conn.prepareStatement(sql)

  protected open fun prepare(stmt: PreparedStatement, conn: Connection, params: List<Param<*>>) =
    params.withIndex().forEach { (idx, param) ->
      param.write(idx, conn, stmt)
    }

  suspend fun <T> FlowCollector<T>.emitResultSet(conn: Connection, rs: ResultSet, extract: (Connection, ResultSet) -> T) {
    while (rs.next()) {
      //val meta = rs.metaData
      //println("--- Emit: ${(1..meta.columnCount).map { rs.getObject(it) }.joinToString(",")}")
      emit(extract(conn, rs))
    }
  }

  protected open suspend fun <T> runActionReturningScoped(act: ActionReturning<T>): Flow<T> =
    flowWithConnection {
      val conn = localConnection()
      makeStmtReturning(act.sql, conn, act.returningColumns).use { stmt ->
        prepare(stmt, conn, act.params)
        stmt.executeUpdate()
        emitResultSet(conn, stmt.generatedKeys, act.resultMaker.makeExtractor())
      }
    }

  protected open suspend fun runBatchActionScoped(query: BatchAction): List<Int> =
    withConnection {
      val conn = localConnection()
      makeStmt(query.sql, conn).use { stmt ->
        // Each set of params is a batch
        query.params.forEach { batch ->
          prepare(stmt, conn, batch)
          stmt.addBatch()
        }
        stmt.executeBatch().toList()
      }
    }

  protected open suspend fun <T> runBatchActionReturningScoped(act: BatchActionReturning<T>): Flow<T> =
    flowWithConnection {
      val conn = localConnection()
      makeStmtReturning(act.sql, conn, act.returningColumns).use { stmt ->
        // Each set of params is a batch
        act.params.forEach { batch ->
          prepare(stmt, conn, batch)
          stmt.addBatch()
        }
        stmt.executeBatch()
        emitResultSet(conn, stmt.generatedKeys, act.resultMaker.makeExtractor())
      }
    }

  protected open suspend fun runActionScoped(sql: String, params: List<Param<*>>): Int =
    withConnection {
      val conn = localConnection()
       makeStmt(sql, conn).use { stmt ->
        prepare(stmt, conn, params)
        tryCatchQuery(sql) {
          stmt.executeUpdate()
        }
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
        tryCatchQuery(query.sql) {
          stmt.executeQuery().use { rs ->
            emitResultSet(conn, rs, query.resultMaker.makeExtractor())
          }
        }
      }
    }

  private inline fun <T> tryCatchQuery(sql: String, op: () -> T): T =
    try {
      op()
    } catch (e: SQLException) {
      throw SQLException("Error executing query: ${sql}", e)
    }

  internal open suspend fun <T> stream(query: BatchActionReturning<T>): Flow<T> = runBatchActionReturningScoped(query)
  internal open suspend fun <T> stream(query: ActionReturning<T>): Flow<T> = runActionReturningScoped(query)
  internal open suspend fun <T> run(query: Query<T>): List<T> = stream(query).toList()
  internal open suspend fun run(query: Action): Int = runActionScoped(query.sql, query.params)
  internal open suspend fun run(query: BatchAction): List<Int> = runBatchActionScoped(query)
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
