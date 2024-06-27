package io.exoquery.sql

import com.zaxxer.hikari.HikariDataSource
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import io.zonky.test.db.postgres.embedded.FlywayPreparer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.shaded.org.bouncycastle.cms.RecipientId.password
import javax.sql.DataSource

object TestDatabases {
  val embeddedPostgres by lazy {
    val started = EmbeddedPostgres.start()
    FlywayPreparer.forClasspathLocation("db/postgres").prepare(started.getPostgresDatabase())
    started
  }
  val postgres by lazy { embeddedPostgres.getPostgresDatabase() }

  val mysql: DataSource by lazy {
    HikariDataSource().apply {
      driverClassName = "com.mysql.cj.jdbc.MysqlDataSource"
      jdbcUrl = "jdbc:mysql://localhost:33306/terpal_test"
      username = "root"
      password = "root"
    }
  }
}