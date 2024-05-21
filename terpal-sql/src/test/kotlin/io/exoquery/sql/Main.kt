package io.exoquery.sql

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import kotlinx.serialization.serializer
import java.sql.Connection
import java.time.LocalDate

suspend fun main() {
  EmbeddedPostgres.start().use { postgres ->
    val ds = postgres.getPostgresDatabase()
    val conn = ds.getConnection()
    conn.runUpdate(
      """
      CREATE TABLE person (
        id SERIAL PRIMARY KEY,
        firstName VARCHAR,
        lastName VARCHAR,
        age INT,
        lastUpdated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )  
      """.trimIndent()
    )
    conn.runUpdate(
      """
      INSERT INTO person (firstName, lastName, age) VALUES ('Joe', 'Bloggs', 123);
      INSERT INTO person (firstName, lastName, age) VALUES ('Jim', 'Roogs', 123);
      """.trimIndent()
    )
    conn.runSelect("SELECT * FROM person")

    //val ps = conn.prepareStatement("SELECT id, firstName, lastName, age, lastUpdated FROM person WHERE firstName = ?")
    //val param = Param("Joe")
    //param.write(1, ps)
    //val rs = ps.executeQuery()
    //val serializer = Person.serializer()
    //println("----- Person -----")
    //while (rs.next()) {
    //  val decoder = ResultDecoder(rs, serializer.descriptor)
    //  val p = serializer.deserialize(decoder)
    //  println(p)
    //}

    val d = LocalDate.now()

    val ser = serializer<LocalDate>()

    val par = Param(d)

    val param = Param("Joe")
    val q =
      Sql("SELECT id, firstName, lastName, age, lastUpdated FROM person WHERE firstName = ${param} AND lastUpdate = ${par}").queryOf<Person>().run(ds).await()

    println(q)


  }
}

fun Connection.runUpdate(sql: String) {
  val stmt = this.createStatement()
  stmt.executeUpdate(sql)
}

fun Connection.runSelect(sql: String) {
  val stmt = this.createStatement()
  val rs = stmt.executeQuery(sql)
  val meta = rs.metaData
  val numColumns = meta.columnCount
  val colNames = (1..numColumns).map { meta.getColumnName(it) }
  while (rs.next()) {
    val colStr =
      (1..numColumns).map { col ->
        "${colNames[col-1]}=${rs.getString(col)}"
      }.joinToString(", ")

    println(colStr)
  }
}