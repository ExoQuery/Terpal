package io.exoquery.sql.jdbc

import io.exoquery.sql.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.KSerializer
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
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


open class PostgresJdbcContext(override val database: DataSource): JdbcContext(database) {
  override val additionalEncoders = setOf<SqlEncoder<Connection, PreparedStatement, out Any>>(UUIDObjectEncoding.UUIDObjectEncoder)
  override val additionalDecoders = setOf<SqlDecoder<Connection, ResultSet, out Any>>(UUIDObjectEncoding.UUIDObjectDecoder)
  override protected open val encoders: SqlEncoders<Connection, PreparedStatement> by lazy {
    // Unless you do `this@PostgresJdbcContext.additionalEncoders` it will use the encoders for the parent class which is incorrect (i.e. doesn't contain the UUIDObjectEncoder)
    object: JdbcEncodersWithTime(this@PostgresJdbcContext.additionalEncoders) {
      // Postgres does not support Types.TIME_WITH_TIMEZONE as a JDBC type but does have a `TIME WITH TIMEZONE` datatype this is puzzling.
      override val jdbcTypeOfOffsetTime = Types.TIME
      //override val encoders by lazy { super.encoders + additionalEncoders }
    }
  }

  open class Legacy(override val database: DataSource): PostgresJdbcContext(database) {
    override protected open val encoders: SqlEncoders<Connection, PreparedStatement> by lazy { JdbcEncodersWithTimeLegacy(additionalEncoders) }
    override protected open val decoders: SqlDecoders<Connection, ResultSet> by lazy { JdbcDecodersWithTimeLegacy(additionalDecoders) }
  }
}


abstract class JdbcContext(override val database: DataSource): Context<Connection, DataSource>() {
  // Need to do this first in iniitalization
  protected open val additionalEncoders = setOf<SqlEncoder<Connection, PreparedStatement, out Any>>()
  protected open val additionalDecoders = setOf<SqlDecoder<Connection, ResultSet, out Any>>()
  protected open val timezone: TimeZone = TimeZone.getDefault()

  protected open val encoders: SqlEncoders<Connection, PreparedStatement> by lazy { JdbcEncodersWithTime(additionalEncoders) }
  protected open val decoders: SqlDecoders<Connection, ResultSet> by lazy { JdbcDecodersWithTime(additionalDecoders) }
  protected open val batchReturnBehavior: ReturnAction = ReturnAction.ReturnRecord

  override fun newSession(): Connection = database.connection
  override fun closeSession(session: Connection): Unit = session.close()
  override fun isClosedSession(session: Connection): Boolean = session.isClosed

  protected fun createEncodingContext(session: Connection, stmt: PreparedStatement) = EncodingContext(session, stmt, timezone)
  protected fun createDecodingContext(session: Connection, row: ResultSet) = DecodingContext(session, row, timezone)

  private val JdbcCoroutineContext = object: CoroutineContext.Key<CoroutineSession<Connection>> {}
  override val sessionKey: CoroutineContext.Key<CoroutineSession<Connection>> = JdbcCoroutineContext

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
  fun <T> Param<T>.write(index: Int, conn: Connection, ps: PreparedStatement): Unit {
    println("----- Preparing parameter $index - $value - using $serializer")
    PreparedStatementElementEncoder(createEncodingContext(conn, ps), index+1, encoders).encodeNullableSerializableValue(serializer, value)
  }

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
      val decoder = JdbcRowDecoder(createDecodingContext(conn, rs), decoders, descriptor)
      deserialize(decoder)
    }

  internal suspend fun <T> stream(query: Query<T>): Flow<T> =
    flowWithConnection {
      val conn = localConnection()
      makeStmt(query.sql, conn).use { stmt ->
        prepare(stmt, conn, query.params)
        stmt.executeQuery().use { rs ->
          emitResultSet(conn, rs, query.resultMaker.makeExtractor())
        }
      }
    }

  internal suspend fun <T> stream(query: BatchActionReturning<T>): Flow<T> = runBatchActionReturningScoped(query.sql, query.params, batchReturnBehavior, query.resultMaker.makeExtractor())
  internal suspend fun <T> stream(query: ActionReturning<T>): Flow<T> = runActionReturningScoped(query.sql, query.params, batchReturnBehavior, query.resultMaker.makeExtractor())
  internal suspend fun <T> run(query: Query<T>): List<T> = stream(query).toList()
  internal suspend fun run(query: Action): Int = runActionScoped(query.sql, query.params)
  internal suspend fun run(query: BatchAction): List<Int> = runBatchActionScoped(query.sql, query.params)
  internal suspend fun <T> run(query: ActionReturning<T>): T = stream(query).first()
  internal suspend fun <T> run(query: BatchActionReturning<T>): List<T> = stream(query).toList()

  suspend fun <T> transaction(block: suspend ExternalTransactionScope.() -> T): T =
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
