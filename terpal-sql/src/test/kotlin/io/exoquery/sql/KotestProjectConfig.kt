package io.exoquery.sql

import io.kotest.core.config.AbstractProjectConfig
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import io.zonky.test.db.postgres.embedded.FlywayPreparer
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.MySQLContainer
import javax.sql.DataSource

object KotestProjectConfig : AbstractProjectConfig() {
  lateinit var mysql: DataSource

  override suspend fun beforeProject() {
    super.beforeProject()

    val mysqlContainer by lazy {
      MySQLContainer("8.4.0").apply {
        withInitScript("db/mysql-schema.sql")
        withReuse(true)
        startupAttempts = 1
        start()
      }
    }
    mysql = PGSimpleDataSource().apply {
      setUrl(mysqlContainer.getJdbcUrl())
      user = mysqlContainer.getUsername()
      password = mysqlContainer.getPassword()
    }

    FlywayPreparer.forClasspathLocation("db/postgres-schema.sql").prepare(QuickPostgres.get().getPostgresDatabase())
  }
}

object QuickPostgres {
  private var embeddedPostgres: EmbeddedPostgres? = null

  fun run(sql: String) {
    get().getPostgresDatabase()?.connection?.use { conn ->
      conn.createStatement().use { stmt ->
        stmt.execute(sql)
      }
    }
  }

  fun get(): EmbeddedPostgres {
    return embeddedPostgres ?: EmbeddedPostgres.start().also { embeddedPostgres = it }
  }
}

fun EmbeddedPostgres.run(str: String) {
  this.getPostgresDatabase()?.connection?.use { conn ->
    conn.createStatement().use { stmt ->
      stmt.execute(str)
    }
  }
}
