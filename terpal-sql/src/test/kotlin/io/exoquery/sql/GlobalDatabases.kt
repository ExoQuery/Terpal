package io.exoquery.sql

import io.kotest.core.extensions.install
import io.kotest.core.spec.Spec
import io.kotest.extensions.testcontainers.ContainerLifecycleMode
import io.kotest.extensions.testcontainers.JdbcDatabaseContainerExtension
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

fun DataSource.run(query: String) =
  this.getConnection().use { conn -> conn.createStatement().use { stmt ->
    stmt.execute(query)
  } }