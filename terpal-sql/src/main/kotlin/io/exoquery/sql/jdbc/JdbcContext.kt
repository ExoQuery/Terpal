package io.exoquery.sql.jdbc

import io.exoquery.sql.JdbcParam
import io.exoquery.sql.Query
import io.exoquery.sql.RowDecoder
import kotlinx.coroutines.*
import java.sql.Connection
import java.sql.PreparedStatement
import javax.sql.DataSource
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

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
            val decoder = RowDecoder(rs, query.resultMaker.descriptor)
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