package io.exoquery.sql.jdbc

import io.exoquery.sql.*
import io.exoquery.sql.jdbc.TerpalContext.Mysql.MysqlTimeEncoding
import kotlinx.coroutines.flow.Flow
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import javax.sql.DataSource

object TerpalContext {
  open class Postgres(override val database: DataSource): JdbcContext(database) {
    // TODO this setting AdditionaJdbcTimeEncoding.encoders/decoders is already set in the parent class. Try to remove it.
    override val additionalEncoders = super.additionalEncoders + AdditionaJdbcTimeEncoding.encoders
    override val additionalDecoders = super.additionalDecoders + AdditionaJdbcTimeEncoding.decoders

    // Postgres does not support Types.TIME_WITH_TIMEZONE as a JDBC type but does have a `TIME WITH TIMEZONE` datatype this is puzzling.
    object PostgresTimeEncoding: JdbcTimeEncoding() {
      override val jdbcTypeOfOffsetTime = Types.TIME
    }
    override protected open val encodingApi: SqlEncoding<Connection, PreparedStatement, ResultSet> =
      object : SqlEncoding<Connection, PreparedStatement, ResultSet>,
        BasicEncoding<Connection, PreparedStatement, ResultSet> by JdbcEncodingBasic,
        BooleanEncoding<Connection, PreparedStatement, ResultSet> by JdbcBooleanObjectEncoding,
        TimeEncoding<Connection, PreparedStatement, ResultSet> by PostgresTimeEncoding,
        UuidEncoding<Connection, PreparedStatement, ResultSet> by JdbcUuidObjectEncoding {}
  }

  open class PostgresLegacy(override val database: DataSource): Postgres(database) {
    override protected open val encodingApi: SqlEncoding<Connection, PreparedStatement, ResultSet> =
      object : SqlEncoding<Connection, PreparedStatement, ResultSet>,
        BasicEncoding<Connection, PreparedStatement, ResultSet> by JdbcEncodingBasic,
        BooleanEncoding<Connection, PreparedStatement, ResultSet> by JdbcBooleanObjectEncoding,
        TimeEncoding<Connection, PreparedStatement, ResultSet> by JdbcTimeEncodingLegacy,
        UuidEncoding<Connection, PreparedStatement, ResultSet> by JdbcUuidObjectEncoding {}
  }

  open class H2(override val database: DataSource): JdbcContext(database) {
    override protected open val encodingApi: SqlEncoding<Connection, PreparedStatement, ResultSet> =
      object : SqlEncoding<Connection, PreparedStatement, ResultSet>,
        BasicEncoding<Connection, PreparedStatement, ResultSet> by JdbcEncodingBasic,
        BooleanEncoding<Connection, PreparedStatement, ResultSet> by JdbcBooleanObjectEncoding,
        TimeEncoding<Connection, PreparedStatement, ResultSet> by JdbcTimeEncoding(),
        UuidEncoding<Connection, PreparedStatement, ResultSet> by JdbcUuidObjectEncoding {}
  }

  open class Mysql(override val database: DataSource): JdbcContext(database) {
    object MysqlTimeEncoding: JdbcTimeEncoding() {
      override val jdbcTypeOfZonedDateTime  = Types.TIMESTAMP
      override val jdbcTypeOfInstant        = Types.TIMESTAMP
      override val jdbcTypeOfOffsetTime     = Types.TIME
      override val jdbcTypeOfOffsetDateTime = Types.TIMESTAMP
    }
    override protected open val encodingApi: SqlEncoding<Connection, PreparedStatement, ResultSet> =
      object : SqlEncoding<Connection, PreparedStatement, ResultSet>,
        BasicEncoding<Connection, PreparedStatement, ResultSet> by JdbcEncodingBasic,
        BooleanEncoding<Connection, PreparedStatement, ResultSet> by JdbcBooleanObjectEncoding,
        TimeEncoding<Connection, PreparedStatement, ResultSet> by MysqlTimeEncoding,
        UuidEncoding<Connection, PreparedStatement, ResultSet> by JdbcUuidStringEncoding {}
  }

  open class Sqlite(override val database: DataSource): JdbcContext(database) {
    override protected open val encodingApi: SqlEncoding<Connection, PreparedStatement, ResultSet> =
      object : SqlEncoding<Connection, PreparedStatement, ResultSet>,
        BasicEncoding<Connection, PreparedStatement, ResultSet> by JdbcEncodingBasic,
        BooleanEncoding<Connection, PreparedStatement, ResultSet> by JdbcBooleanObjectEncoding,
        TimeEncoding<Connection, PreparedStatement, ResultSet> by JdbcTimeEncodingLegacy,
        UuidEncoding<Connection, PreparedStatement, ResultSet> by JdbcUuidObjectEncoding {}

    protected override open suspend fun <T> runBatchActionReturningScoped(sql: String, batches: Sequence<List<Param<*>>>, returningBehavior: ReturnAction, extract: (Connection, ResultSet) -> T): Flow<T> =
      flowWithConnection {
        val conn = localConnection()
        batches.forEach { batch ->
          // Sqlite does not support Batch-Actions with returning-keys. So we attempt to emulate this function with single-row inserts inside a transaction but using this API is not recommended.
          makeStmtReturning(sql, conn, returningBehavior).use { stmt ->
            prepare(stmt, conn, batch)
            emitResultSet(conn, stmt.executeQuery(), extract)
          }
        }
      }
  }

  open class Oracle(override val database: DataSource): JdbcContext(database) {
    override protected open val encodingApi: SqlEncoding<Connection, PreparedStatement, ResultSet> =
      object : SqlEncoding<Connection, PreparedStatement, ResultSet>,
        BasicEncoding<Connection, PreparedStatement, ResultSet> by JdbcEncodingBasic,
        BooleanEncoding<Connection, PreparedStatement, ResultSet> by JdbcBooleanIntEncoding,
        TimeEncoding<Connection, PreparedStatement, ResultSet> by MysqlTimeEncoding,
        UuidEncoding<Connection, PreparedStatement, ResultSet> by JdbcUuidStringEncoding {}
  }

  open class SqlServer(override val database: DataSource): JdbcContext(database) {
    override protected open val encodingApi: SqlEncoding<Connection, PreparedStatement, ResultSet> =
      object : SqlEncoding<Connection, PreparedStatement, ResultSet>,
        BasicEncoding<Connection, PreparedStatement, ResultSet> by JdbcEncodingBasic,
        BooleanEncoding<Connection, PreparedStatement, ResultSet> by JdbcBooleanObjectEncoding,
        TimeEncoding<Connection, PreparedStatement, ResultSet> by JdbcTimeEncoding(),
        UuidEncoding<Connection, PreparedStatement, ResultSet> by JdbcUuidStringEncoding {}

    override suspend fun <T> runActionReturningScoped(sql: String, params: List<Param<*>>, returningBehavior: ReturnAction, extract: (Connection, ResultSet) -> T): Flow<T> =
      flowWithConnection {
        val conn = localConnection()
        makeStmtReturning(sql, conn, returningBehavior).use { stmt ->
          prepare(stmt, conn, params)
          // See comment about SQL Server not supporting getGeneratedKeys below
          emitResultSet(conn, stmt.executeQuery(), extract)
        }
      }

    override suspend fun <T> runBatchActionReturningScoped(sql: String, batches: Sequence<List<Param<*>>>, returningBehavior: ReturnAction, extract: (Connection, ResultSet) -> T): Flow<T> =
      flowWithConnection {
        val conn = localConnection()
        makeStmtReturning(sql, conn, returningBehavior).use { stmt ->
          batches.forEach { batch ->
            prepare(stmt, conn, batch)
            // The SQL Server driver has no ability to either do getGeneratedKeys or executeQuery
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