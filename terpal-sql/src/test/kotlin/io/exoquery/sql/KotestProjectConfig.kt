package io.exoquery.sql

import io.kotest.core.config.AbstractProjectConfig
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import io.zonky.test.db.postgres.embedded.FlywayPreparer
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules

object KotestProjectConfig : AbstractProjectConfig() {
  override suspend fun beforeProject() {
    super.beforeProject()
    FlywayPreparer.forClasspathLocation("db/postgres").prepare(GlobalEmbeddedPostgres.get().getPostgresDatabase())
  }
}

object GlobalEmbeddedPostgres {
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
