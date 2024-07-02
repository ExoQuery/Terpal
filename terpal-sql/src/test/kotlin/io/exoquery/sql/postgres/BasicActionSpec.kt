package io.exoquery.sql.postgres

import io.exoquery.sql.TestDatabases
import io.exoquery.sql.jdbc.TerpalContext
import io.exoquery.sql.jdbc.Sql
import io.exoquery.sql.jdbc.runOn
import io.exoquery.sql.run
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable

class BasicActionSpec : FreeSpec({

  val ds = TestDatabases.postgres
  val ctx by lazy { TerpalContext.Postgres(ds)  }

  beforeEach {
    ds.run(
      """
      TRUNCATE TABLE person RESTART IDENTITY CASCADE;
      TRUNCATE TABLE address RESTART IDENTITY CASCADE;
      """
    )
  }

  @Serializable
  data class Person(val id: Int, val firstName: String, val lastName: String, val age: Int)

  val joe = Person(1, "Joe", "Bloggs", 111)
  val jim = Person(2, "Jim", "Roogs", 222)

  "Basic Insert" {
    Sql("INSERT INTO person (id, firstName, lastName, age) VALUES (${joe.id}, ${joe.firstName}, ${joe.lastName}, ${joe.age})").action().runOn(ctx);
    Sql("INSERT INTO person (id, firstName, lastName, age) VALUES (${jim.id}, ${jim.firstName}, ${jim.lastName}, ${jim.age})").action().runOn(ctx);
    Sql("SELECT id, firstName, lastName, age FROM person").queryOf<Person>().runOn(ctx) shouldBe listOf(joe, jim)
  }

  "Insert Returning" {
    val id1 = Sql("INSERT INTO person (firstName, lastName, age) VALUES (${joe.firstName}, ${joe.lastName}, ${joe.age}) RETURNING id").actionReturning<Int>().runOn(ctx);
    val id2 = Sql("INSERT INTO person (firstName, lastName, age) VALUES (${jim.firstName}, ${jim.lastName}, ${jim.age}) RETURNING id").actionReturning<Int>().runOn(ctx);
    id1 shouldBe 1
    id2 shouldBe 2
    Sql("SELECT id, firstName, lastName, age FROM person").queryOf<Person>().runOn(ctx) shouldBe listOf(joe, jim)
  }

  "Insert Returning Record" {
    val person1 = Sql("INSERT INTO person (firstName, lastName, age) VALUES (${joe.firstName}, ${joe.lastName}, ${joe.age}) RETURNING id, firstName, lastName, age").actionReturning<Person>().runOn(ctx)
    val person2 = Sql("INSERT INTO person (firstName, lastName, age) VALUES (${jim.firstName}, ${jim.lastName}, ${jim.age}) RETURNING id, firstName, lastName, age").actionReturning<Person>().runOn(ctx)
    person1 shouldBe joe
    person2 shouldBe jim
    Sql("SELECT id, firstName, lastName, age FROM person").queryOf<Person>().runOn(ctx) shouldBe listOf(joe, jim)
  }

})