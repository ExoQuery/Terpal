package io.exoquery.sql.sqlite

import io.exoquery.sql.TestDatabases
import io.exoquery.sql.jdbc.TerpalContext
import io.exoquery.sql.jdbc.Sql
import io.exoquery.sql.run
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable

class BasicQuerySpec : FreeSpec({

  val ds = TestDatabases.sqlite
  val ctx by lazy { TerpalContext.Sqlite(ds)  }

  beforeSpec {
    ds.run(
      """
      DELETE FROM Person;
      DELETE FROM Address;
      INSERT INTO Person (id, firstName, lastName, age) VALUES (1, 'Joe', 'Bloggs', 111);
      INSERT INTO Person (id, firstName, lastName, age) VALUES (2, 'Jim', 'Roogs', 222);
      INSERT INTO Address (ownerId, street, zip) VALUES (1, '123 Main St', '12345');
      """
    )
  }

  "SELECT Person - simple" {
    @Serializable
    data class Person(val id: Int, val firstName: String, val lastName: String, val age: Int)

    ctx.run(Sql("SELECT id, firstName, lastName, age FROM Person").queryOf<Person>()) shouldBe listOf(
      Person(1, "Joe", "Bloggs", 111),
      Person(2, "Jim", "Roogs", 222)
    )
  }

  "joins" - {
    @Serializable
    data class Person(val id: Int, val firstName: String, val lastName: String, val age: Int)
    @Serializable
    data class Address(val ownerId: Int, val street: String, val zip: String)

    "SELECT Person, Address - join" {
      ctx.run(Sql("SELECT p.id, p.firstName, p.lastName, p.age, a.ownerId, a.street, a.zip FROM Person p JOIN Address a ON p.id = a.ownerId").queryOf<Pair<Person, Address>>()) shouldBe listOf(
        Person(1, "Joe", "Bloggs", 111) to Address(1, "123 Main St", "12345")
      )
    }

    "SELECT Person, Address - leftJoin + null" {
      ctx.run(Sql("SELECT p.id, p.firstName, p.lastName, p.age, a.ownerId, a.street, a.zip FROM Person p LEFT JOIN Address a ON p.id = a.ownerId").queryOf<Pair<Person, Address?>>()) shouldBe listOf(
        Person(1, "Joe", "Bloggs", 111) to Address(1, "123 Main St", "12345"),
        Person(2, "Jim", "Roogs", 222) to null
      )
    }

    @Serializable
    data class CustomRow1(val Person: Person, val Address: Address)
    @Serializable
    data class CustomRow2(val Person: Person, val Address: Address?)

    "SELECT Person, Address - join - custom row" {
      ctx.run(Sql("SELECT p.id, p.firstName, p.lastName, p.age, a.ownerId, a.street, a.zip FROM Person p JOIN Address a ON p.id = a.ownerId").queryOf<CustomRow1>()) shouldBe listOf(
        CustomRow1(Person(1, "Joe", "Bloggs", 111), Address(1, "123 Main St", "12345"))
      )
    }

    "SELECT Person, Address - leftJoin + null - custom row" {
      ctx.run(Sql("SELECT p.id, p.firstName, p.lastName, p.age, a.ownerId, a.street, a.zip FROM Person p LEFT JOIN Address a ON p.id = a.ownerId").queryOf<CustomRow2>()) shouldBe listOf(
        CustomRow2(Person(1, "Joe", "Bloggs", 111), Address(1, "123 Main St", "12345")),
        CustomRow2(Person(2, "Jim", "Roogs", 222), null)
      )
    }
  }

  "joins + null complex" - {
    @Serializable
    data class Person(val id: Int, val firstName: String?, val lastName: String, val age: Int)
    @Serializable
    data class Address(val ownerId: Int?, val street: String, val zip: String)

    "SELECT Person, Address - join" {
      ctx.run(Sql("SELECT p.id, null as firstName, p.lastName, p.age, null as ownerId, a.street, a.zip FROM Person p JOIN Address a ON p.id = a.ownerId").queryOf<Pair<Person, Address>>()) shouldBe listOf(
        Person(1, null, "Bloggs", 111) to Address(null, "123 Main St", "12345")
      )
    }

    "SELECT Person, Address - leftJoin + null" {
      ctx.run(Sql("SELECT p.id, null as firstName, p.lastName, p.age, null as ownerId, a.street, a.zip FROM Person p LEFT JOIN Address a ON p.id = a.ownerId").queryOf<Pair<Person, Address?>>()) shouldBe listOf(
        Person(1, null, "Bloggs", 111) to Address(null, "123 Main St", "12345"),
        Person(2, null, "Roogs", 222) to null
      )
    }
  }

  "SELECT Person - nested" {
    @Serializable
    data class Name(val firstName: String, val lastName: String)
    @Serializable
    data class Person(val id: Int, val name: Name, val age: Int)

    ctx.run(Sql("SELECT id, firstName, lastName, age FROM Person").queryOf<Person>()) shouldBe listOf(
      Person(1, Name("Joe", "Bloggs"), 111),
      Person(2, Name("Jim", "Roogs"), 222)
    )
  }

  "SELECT Person - nested with join" {
    @Serializable
    data class Name(val firstName: String, val lastName: String)
    @Serializable
    data class Person(val id: Int, val name: Name, val age: Int)
    @Serializable
    data class Address(val street: String, val zip: String)

    ctx.run(Sql("SELECT p.id, p.firstName, p.lastName, p.age, a.street, a.zip FROM Person p JOIN Address a ON p.id = a.ownerId").queryOf<Pair<Person, Address>>()) shouldBe listOf(
      Person(1, Name("Joe", "Bloggs"), 111) to Address("123 Main St", "12345")
    )
  }
})