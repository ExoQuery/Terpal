package io.exoquery.sql

import io.kotest.core.config.AbstractProjectConfig
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import io.zonky.test.db.postgres.embedded.FlywayPreparer
import javax.sql.DataSource

object KotestProjectConfig : AbstractProjectConfig() {
  override suspend fun beforeProject() {
    super.beforeProject()
  }
}

object QuickPostgres {
  val embeddedPostgres by lazy {
    val started = EmbeddedPostgres.start()
    FlywayPreparer.forClasspathLocation("db/postgres").prepare(started.getPostgresDatabase())
    started
  }

  val postgres by lazy { embeddedPostgres.getPostgresDatabase() }
}

fun EmbeddedPostgres.run(str: String) {
  this.getPostgresDatabase()?.connection?.use { conn ->
    conn.createStatement().use { stmt ->
      stmt.execute(str)
    }
  }
}
