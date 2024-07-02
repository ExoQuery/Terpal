package io.exoquery.sql.jdbc

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariDataSource

object HikariHelper {
  fun makeDataSource(configPrefix: String): HikariDataSource {
    val factory = ConfigFactory.load(this::class.java.classLoader)
    val config =
      if (factory.hasPath(configPrefix))
        factory.getConfig(configPrefix)
      else
        ConfigFactory.empty()
    return JdbcContextConfig(config).dataSource()
  }
}