package io.exoquery.sql

import javax.sql.DataSource

fun DataSource.run(sql: String) =
  this.getConnection().use { conn ->
    sql.split(";").map { it.trim() }.filter { !it.isEmpty() }.forEach { sqlSplit ->
      conn.createStatement().use { stmt ->
        stmt.execute(sqlSplit)
      }
    }
  }