package io.exoquery.sql

import io.kotest.core.config.AbstractProjectConfig
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import io.zonky.test.db.postgres.embedded.FlywayPreparer
import javax.sql.DataSource

object KotestProjectConfig : AbstractProjectConfig() {
  lateinit var mysql: DataSource

  override suspend fun beforeProject() {
    super.beforeProject()
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
    return embeddedPostgres ?: run {
      val started = EmbeddedPostgres.start().also { embeddedPostgres = it }
      FlywayPreparer.forClasspathLocation("db/postgres").prepare(started.getPostgresDatabase())
      started
    }
  }
}

fun EmbeddedPostgres.run(str: String) {
  this.getPostgresDatabase()?.connection?.use { conn ->
    conn.createStatement().use { stmt ->
      stmt.execute(str)
    }
  }
}
