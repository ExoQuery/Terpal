package io.exoquery.sql.mysql

import io.exoquery.sql.TestDatabases
import io.exoquery.sql.jdbc.PostgresJdbcContext
import io.exoquery.sql.jdbc.Sql
import io.exoquery.sql.run
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FreeSpec
import io.kotest.extensions.testcontainers.JdbcDatabaseContainerExtension
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.testcontainers.containers.PostgreSQLContainer

class TransactionSpec: FreeSpec({
  val ds = TestDatabases.mysql
  val ctx by lazy { PostgresJdbcContext(ds) }
  beforeEach {
    ds.run(
      """
      DELETE FROM person;
      DELETE FROM address;
      """
    )
  }

  @Serializable
  data class Person(val id: Int, val firstName: String, val lastName: String, val age: Int)

  "provides transaction support" - {
    val joe = Person(1, "Joe", "Bloggs", 111)
    val jack = Person(2, "Jack", "Roogs", 222)

    // Note the string elements ${...} should not have quotes around them or else they are interpreted as literals
    fun insert(p: Person) =
      Sql("INSERT INTO person (id, firstName, lastName, age) VALUES (${p.id}, ${p.firstName}, ${p.lastName}, ${p.age})").action()


    fun select() = Sql("SELECT id, firstName, lastName, age FROM person").queryOf<Person>()

    "success" {
      ctx.transaction {
        insert(joe).run()
      }
      ctx.run(select()) shouldBe listOf(joe)
    }
    "failure" {
      ctx.run(insert(joe))
      shouldThrow<IllegalStateException> {
        ctx.transaction {
          insert(jack).run()
          throw IllegalStateException()
        }
      }
      ctx.run(select()) shouldBe listOf(joe)
    }
    "nested" {
      ctx.transaction {
        ctx.transaction {
          insert(joe).run()
        }
      }
      ctx.run(select()) shouldBe listOf(joe)
    }
  }
})