package io.exoquery.sql

import io.kotest.core.config.AbstractProjectConfig
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres

object KotestProjectConfig : AbstractProjectConfig() {
  override suspend fun beforeProject() {
    super.beforeProject()
  }
}
