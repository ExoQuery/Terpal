package io.exoquery.sql

import io.exoquery.sql.jdbc.Sql
import io.exoquery.sql.jdbc.SqlBatch
import io.exoquery.sql.jdbc.TerpalContext
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.sql.Connection
import java.time.LocalDate

suspend fun main() {
  EmbeddedPostgres.start()

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

    val ctx = TerpalContext.Postgres(ds)

    val d = LocalDate.now()
    val par = Param(d)

    // TODO figure out the serializersModule part with this
    //  AND lastUpdate = ${par}"

    @Serializable
    data class Name(val firstName: String, val lastName: String)

    @Serializable
    data class Person(val id: Int, val name: Name, val age: Int, @Contextual val lastUpdated: LocalDate)



    val batch =
      SqlBatch { p: Person -> "INSERT INTO person (id, firstName, lastName, age) VALUES (${Param(p.id)}, ${Param(p.name.firstName)}, ${Param(p.name.lastName)}, ${Param(p.age)}) RETURNING *" }.values(
        Person(11, Name("Joe", "Bloggs"), 111, LocalDate.ofEpochDay(0)),
        Person(22, Name("Jim", "Roogs"), 222, LocalDate.ofEpochDay(0))
      ).actionReturning<Person>()

    val ret = ctx.run(batch)
    println("-------------- Inserted: ${ret}")

    val param = Param("Joe") //
    val query = Sql("SELECT id, firstName, lastName, age, lastUpdated FROM person ${Sql("WHERE firstName = ${param}")} AND lastUpdated > ${Param(d)}").queryOf<Person>()




    val result = ctx.run(query)
    //val result = ctx.run(query)

    println(result)


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