package io.exoquery.sql.jdbc

import io.exoquery.sql.Param
import io.exoquery.sql.SqlEncoders
import kotlinx.coroutines.flow.Flow
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import javax.sql.DataSource

object TerpalContext {
  open class Postgres(override val database: DataSource): JdbcContext(database) {
    override val additionalEncoders = setOf(UUIDEncoding.AsObjectEncoder, BooleanEncoding.AsObjectEncoder)
    override val additionalDecoders = setOf(UUIDEncoding.AsObjectDecoder, BooleanEncoding.AsObjectDecoder)

    override protected open val encoders: SqlEncoders<Connection, PreparedStatement> by lazy {
      // Unless you do `this@PostgresJdbcContext.additionalEncoders` it will use the encoders for the parent class which is incorrect (i.e. doesn't contain the UUIDObjectEncoder)
      object : JdbcEncodersWithTime(this@Postgres.additionalEncoders) {
        // Postgres does not support Types.TIME_WITH_TIMEZONE as a JDBC type but does have a `TIME WITH TIMEZONE` datatype this is puzzling.
        override val jdbcTypeOfOffsetTime = Types.TIME
        //override val encoders by lazy { super.encoders + additionalEncoders }
      }
    }
  }

  open class PostgresLegacy(override val database: DataSource): Postgres(database) {
    override protected open val encoders by lazy { JdbcEncodersWithTimeLegacy(additionalEncoders) }
    override protected open val decoders by lazy { JdbcDecodersWithTimeLegacy(additionalDecoders) }
  }

  open class Mysql(override val database: DataSource): JdbcContext(database) {
    override val additionalEncoders = setOf(UUIDEncoding.AsObjectEncoder, BooleanEncoding.AsObjectEncoder)
    override val additionalDecoders = setOf(UUIDEncoding.AsObjectDecoder, BooleanEncoding.AsObjectDecoder)
  }

  open class Oracle(override val database: DataSource): JdbcContext(database) {
    override val additionalEncoders = setOf(UUIDEncoding.AsStringEncoder, BooleanEncoding.AsIntEncoder)
    override val additionalDecoders = setOf(UUIDEncoding.AsStringDecoder, BooleanEncoding.AsIntDecoder)
  }

  open class SqlServer(override val database: DataSource): JdbcContext(database) {
    override val additionalEncoders = setOf(UUIDEncoding.AsStringEncoder, BooleanEncoding.AsObjectEncoder)
    override val additionalDecoders = setOf(UUIDEncoding.AsStringDecoder, BooleanEncoding.AsObjectDecoder)

    override suspend fun <T> runBatchActionReturningScoped(sql: String, batches: Sequence<List<Param<*>>>, returningBehavior: ReturnAction, extract: (Connection, ResultSet) -> T): Flow<T> =
      flowWithConnection {
        val conn = localConnection()
        makeStmtReturning(sql, conn, returningBehavior).use { stmt ->
          batches.forEach { batch ->
            prepare(stmt, conn, batch)
            // The SQL Server drive has no ability to either go getGeneratedKeys or executeQuery
            // at the end of a sequence of addBatch calls to get all inserted keys/executed queries
            // (whether a `OUTPUT` clause is used in the Query or not). That means that in order
            // be able to get any results, we need to use extractResult(ps.executeQuery, ...)
            // on every single inserted batch! See the following mssql-jdbc issues for more detail:
            // https://github.com/microsoft/mssql-jdbc/issues/358
            // https://github.com/Microsoft/mssql-jdbc/issues/245
            stmt.addBatch()
            emitResultSet(conn, stmt.executeQuery(), extract)
          }
        }
      }



    /*
    // Scala

      def executeBatchAction(groups: List[BatchGroup])(info: ExecutionInfo, dc: Runner): Result[List[Long]] =
    withConnectionWrapped { conn =>
      groups.flatMap { case BatchGroup(sql, prepare) =>
        val ps = conn.prepareStatement(sql)
        logger.underlying.debug("Batch: {}", sql)
        prepare.foreach { f =>
          val (params, _) = f(ps, conn)
          logger.logBatchItem(sql, params)
          ps.addBatch()
        }
        ps.executeBatch().map(_.toLong)
      }
    }

  def executeBatchActionReturning[T](
    groups: List[BatchGroupReturning],
    extractor: Extractor[T]
  )(info: ExecutionInfo, dc: Runner): Result[List[T]] =
    withConnectionWrapped { conn =>
      groups.flatMap { case BatchGroupReturning(sql, returningBehavior, prepare) =>
        val ps = prepareWithReturning(sql, conn, returningBehavior)
        logger.underlying.debug("Batch: {}", sql)
        prepare.foreach { f =>
          val (params, _) = f(ps, conn)
          logger.logBatchItem(sql, params)
          ps.addBatch()
        }
        ps.executeBatch()
        extractResult(ps.getGeneratedKeys, conn, extractor)
      }
    }
     */
  }



}