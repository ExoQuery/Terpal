package io.exoquery.sql

import io.exoquery.sql.examples.id
import io.exoquery.sql.jdbc.Sql
import io.exoquery.sql.jdbc.SqlBatch


object Ex1_BatchInsertNormal {
  val products = makeProducts(22)
  val op =
    SqlBatch { p: Product -> "INSERT INTO Product (id, description, sku) VALUES (${p.id}, ${p.description}, ${p.sku})" }
      .values(products.asSequence()).action()
  val get = Sql("SELECT id, description, sku FROM Product").queryOf<Product>()
  val result = products
}

object Ex2_BatchInsertMixed {
  val products  = makeProducts(20)
  val op =
    SqlBatch { p: Product -> "INSERT INTO Product (id, description, sku) VALUES (${p.id}, ${id("BlahBlah")}, ${p.sku})" }
      .values(products.asSequence()).action()
  val get = Sql("SELECT id, description, sku FROM Product").queryOf<Product>()
  val result = products.map { it.copy(description = "BlahBlah") }
}

object Ex3_BatchReturnIds {
  val products = makeProducts(20)
  val op =
    SqlBatch { p: Product -> "INSERT INTO Product (description, sku) VALUES (${p.description}, ${p.sku}) RETURNING id" }
      .values(products.asSequence()).actionReturning<Int>()
  val get = Sql("SELECT id, description, sku FROM Product").queryOf<Product>()
  val opResult = (1..20).toList()
  val result = products.mapIndexed { i, p -> p.copy(id = i + 1) }
}

object Ex4_BatchReturnRecord {
  val products = makeProducts(20)
  val op =
    SqlBatch { p: Product -> "INSERT INTO Product (description, sku) VALUES (${p.description}, ${p.sku}) RETURNING id, description, sku" }
      .values(products.asSequence()).actionReturning<Product>()
  val get = Sql("SELECT id, description, sku FROM Product").queryOf<Product>()
  val opResult = products.mapIndexed { i, p -> p.copy(id = i + 1) }
  val result = opResult
}
