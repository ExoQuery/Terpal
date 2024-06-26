package io.exoquery.sql

import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import io.kotest.core.config.AbstractProjectConfig
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import io.zonky.test.db.postgres.embedded.FlywayPreparer
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import java.util.function.Consumer
import javax.sql.DataSource

object KotestProjectConfig : AbstractProjectConfig() {
  lateinit var postgres: DataSource

  override suspend fun beforeProject() {
    super.beforeProject()

    val postgresContainer by lazy {
      val hostPort = 25432
      val containerExposedPort = 5432
      val cmd: Consumer<CreateContainerCmd> = Consumer { e: CreateContainerCmd -> e.withPortBindings(PortBinding(Ports.Binding.bindPort(hostPort), ExposedPort(containerExposedPort))); Unit }

      PostgreSQLContainer("postgres:16.3-alpine3.20").apply {
        withInitScript("db/postgres-schema.sql")
        withReuse(true)
        withLabel("reuse.UUID", "e06d7a87-7d7d-472e-a047-e6c81f61d2a4")
        withUsername("postgres")
        withPassword("postgres")
        withExposedPorts(containerExposedPort)
        withCreateContainerCmdModifier(cmd)

        startupAttempts = 1
        start()
      }
    }
    postgres = PGSimpleDataSource().apply {
      setUrl(postgresContainer.getJdbcUrl())
      user = postgresContainer.getUsername()
      password = postgresContainer.getPassword()
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
