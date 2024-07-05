package io.exoquery.sql

import com.zaxxer.hikari.HikariDataSource
import io.exoquery.sql.jdbc.HikariHelper
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import javax.sql.DataSource

object TestDatabases {
  val embeddedPostgres by lazy {
    val started = EmbeddedPostgres.start()
    val postgresScriptsPath = "/db/postgres-schema.sql"
    val resource = this::class.java.getResource(postgresScriptsPath)
    if (resource == null) throw NullPointerException("The postgres script path `$postgresScriptsPath` was not found")
    val postgresScript = resource.readText()
    started.getPostgresDatabase().run(postgresScript)
    started
  }
  val postgres by lazy { embeddedPostgres.getPostgresDatabase() }

  val mysql: DataSource by lazy {
    HikariHelper.makeDataSource("testMysqlDB")
  }

  val sqlServer: DataSource by lazy {
    HikariHelper.makeDataSource("testSqlServerDB")
  }

  val h2: DataSource by lazy {
    HikariHelper.makeDataSource("testH2DB")
  }

  val sqlite: DataSource by lazy {
    HikariHelper.makeDataSource("testSqliteDB")
  }

  val oracle: DataSource by lazy {
    HikariHelper.makeDataSource("testOracleDB")
  }
}