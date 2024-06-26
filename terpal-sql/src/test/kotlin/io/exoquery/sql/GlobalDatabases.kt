package io.exoquery.sql

import io.exoquery.sql.GlobalDatabases.postgres
import io.kotest.core.extensions.install
import io.kotest.core.spec.Spec
import io.kotest.extensions.testcontainers.ContainerLifecycleMode
import io.kotest.extensions.testcontainers.JdbcDatabaseContainerExtension
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

object GlobalDatabases {
  val postgres by lazy {
    PostgreSQLContainer("postgres:16.3-alpine3.20").apply {
      withInitScript("db/postgres-schema.sql")
      startupAttempts = 1
    }
  }
}


fun Spec.installPostgres(): DataSource = install(JdbcDatabaseContainerExtension(GlobalDatabases.postgres, ContainerLifecycleMode.Project))