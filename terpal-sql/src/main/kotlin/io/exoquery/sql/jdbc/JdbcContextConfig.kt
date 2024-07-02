package io.exoquery.sql.jdbc

import java.util.*

import com.typesafe.config.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.util.Properties

data class JdbcContextConfig(val config: Config) {
    fun configProperties(): Properties {
        val p = Properties()
        for ((key, value) in config.entrySet()) {
            p.setProperty(key, value.unwrapped().toString())
        }
        return p
    }

    fun dataSource(): HikariDataSource {
        return try {
            HikariDataSource(HikariConfig(configProperties()))
        } catch (ex: Exception) {
            throw IllegalStateException("Failed to load data source", ex)
        }
    }
}

/*
// scala
case class JdbcContextConfig(config: Config) {

  def configProperties = {
    import scala.jdk.CollectionConverters._
    val p = new Properties
    for (entry <- config.entrySet.asScala)
      p.setProperty(entry.getKey, entry.getValue.unwrapped.toString)
    p
  }

  def dataSource =
    try
      new HikariDataSource(new HikariConfig(configProperties))
    catch {
      case NonFatal(ex) =>
        throw new IllegalStateException("Failed to load data source", ex)
    }
}

 */