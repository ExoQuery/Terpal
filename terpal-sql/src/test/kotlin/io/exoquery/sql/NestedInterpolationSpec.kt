package io.exoquery.sql

import io.exoquery.sql.examples.id
import io.exoquery.sql.jdbc.Sql
import io.exoquery.sql.jdbc.TerpalContext
import io.exoquery.sql.jdbc.runOn
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable

class NestedInterpolationSpec: FreeSpec({

  @Serializable
  data class Person(val id: Int, val firstName: String, val lastName: String, val age: Int)

  val ds = TestDatabases.postgres
  val ctx by lazy { TerpalContext.Postgres(ds)  }

  beforeSpec {
    ds.run(
      """
      DELETE FROM person;
      INSERT INTO person (id, firstName, lastName, age) VALUES (1, 'John', 'Bloogs', 222);
      INSERT INTO person (id, firstName, lastName, age) VALUES (2, 'Ron', 'Bloogs', 222);
      """
    )
  }

  "nested Sql clauses" {
    val select = Sql("SELECT id, na, nb, a FROM (${Sql("SELECT id, firstName AS na, lastName AS nb, age AS a FROM person WHERE lastName = ${id("Bloogs")}")}) WHERE id = ${id(1)}")
    val ir =
      IR.Splice(
        parts = listOf(IR.Part("SELECT id, na, nb, a FROM ("), IR.Part(") WHERE id = "), IR.Part("")),
        params = listOf(
          IR.Splice(parts = listOf(IR.Part("SELECT id, firstName AS na, lastName AS nb, age AS a FROM person WHERE lastName = "), IR.Part("")), params = listOf(IR.Param(Param("Bloogs")))),
          IR.Param(Param(1.toInt())),
        )
      )

    ir shouldBe select.ir

    select.queryOf<Person>().runOn(ctx) shouldBe listOf(Person(1, "John", "Bloogs", 222))
  }
})