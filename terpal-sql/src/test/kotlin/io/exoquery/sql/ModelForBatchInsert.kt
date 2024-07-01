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

/*
  object `Ex 1 - Batch Insert Normal` {
val products      = makeProducts(22)
val batchSize     = 5
def opExt = quote { (transform: Insert[Product] => Insert[Product]) =>
  liftQuery(products).foreach(p => transform(query[Product].insertValue(p)))
}
def op = quote {
  liftQuery(products).foreach(p => query[Product].insertValue(p))
}
def get    = quote(query[Product])
def result = products
}
 */

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

/*
object `Ex 2 - Batch Insert Returning` {
    val productsOriginal = makeProducts(22)
    // want to populate them from DB
    val products    = productsOriginal.map(p => p.copy(id = 0))
    val expectedIds = productsOriginal.map(_.id)
    val batchSize   = 10
    def op = quote {
      liftQuery(products).foreach(p => query[Product].insertValue(p).returningGenerated(p => p.id))
    }
    def get    = quote(query[Product])
    def result = productsOriginal
  }

 */


object Ex3_BatchInsertMixed {
  val products  = makeProducts(20)
  val op =
    SqlBatch { p: Product -> "INSERT INTO product (id, description, sku) VALUES (${p.id}, ${id("BlahBlah")}, ${p.sku})" }
      .values(products.asSequence()).action()
  val get = Sql("SELECT id, description, sku FROM product").queryOf<Product>()
  val result = products.map { it.copy(description = "BlahBlah") }
}



/*
object `Ex 3 - Batch Insert Mixed` {
    val products  = makeProducts(20)
    val batchSize = 40
    def op = quote {
      liftQuery(products).foreach(p =>
        query[Product].insert(_.id -> p.id, _.description -> lift("BlahBlah"), _.sku -> p.sku)
      )
    }
    def opExt = quote { (transform: Insert[Product] => Insert[Product]) =>
      liftQuery(products).foreach(p =>
        transform(query[Product].insert(_.id -> p.id, _.description -> lift("BlahBlah"), _.sku -> p.sku))
      )
    }
    def get    = quote(query[Product])
    def result = products.map(_.copy(description = "BlahBlah"))
  }
 */
