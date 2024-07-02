package io.exoquery.sql

import io.exoquery.sql.examples.id
import io.exoquery.sql.jdbc.Sql
import io.exoquery.sql.jdbc.SqlBatch


object Ex1_BatchInsertNormal {
  val products = makeProducts(22)
  val op =
    SqlBatch { p: Product -> "INSERT INTO product (id, description, sku) VALUES (${p.id}, ${p.description}, ${p.sku})" }
      .values(products.asSequence()).action()
  val get = Sql("SELECT id, description, sku FROM product").queryOf<Product>()
  val result = products
}

object Ex2_BatchInsertReturning {
  val productsOriginal = makeProducts(22)
  // want to populate them from DB
  val products    = productsOriginal.map { it.copy(id = 0) }
  val expectedIds = productsOriginal.map { it.id }
  val op =
    SqlBatch { p: Product -> "INSERT INTO product (description, sku) VALUES (${p.description}, ${p.sku}) RETURNING id" }
      .values(products.asSequence()).actionReturning<Int>()
  val get = Sql("SELECT id, description, sku FROM product").queryOf<Product>()
  val result = productsOriginal
}

object Ex3_BatchInsertMixed {
  val products  = makeProducts(20)
  val op =
    SqlBatch { p: Product -> "INSERT INTO product (id, description, sku) VALUES (${p.id}, ${id("BlahBlah")}, ${p.sku})" }
      .values(products.asSequence()).action()
  val get = Sql("SELECT id, description, sku FROM product").queryOf<Product>()
  val result = products.map { it.copy(description = "BlahBlah") }
}

object Ex4_BatchReturnIds {
  val products = makeProducts(20)
  val op =
    SqlBatch { p: Product -> "INSERT INTO product (description, sku) VALUES (${p.description}, ${p.sku}) RETURNING id" }
      .values(products.asSequence()).actionReturning<Int>()
  val get = Sql("SELECT id, description, sku FROM product").queryOf<Product>()
  val opResult = (1..20).toList()
  val result = products.mapIndexed { i, p -> p.copy(id = i + 1) }
}

object Ex5_BatchReturnRecord {
  val products = makeProducts(20)
  val op =
    SqlBatch { p: Product -> "INSERT INTO product (description, sku) VALUES (${p.description}, ${p.sku}) RETURNING id, description, sku" }
      .values(products.asSequence()).actionReturning<Product>()
  val get = Sql("SELECT id, description, sku FROM product").queryOf<Product>()
  val opResult = products.mapIndexed { i, p -> p.copy(id = i + 1) }
  val result = opResult
}
