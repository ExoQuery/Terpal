package io.exoquery.sql.examples

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres

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
