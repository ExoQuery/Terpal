package io.exoquery.sql.sqlserver

import io.exoquery.sql.*
import io.exoquery.sql.Ex3_BatchReturnIds.products
import io.exoquery.sql.examples.id
import io.exoquery.sql.jdbc.SqlBatch
import io.exoquery.sql.jdbc.TerpalContext
import io.exoquery.sql.jdbc.runOn
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class BatchValuesSpec: FreeSpec ({
  val ds = TestDatabases.sqlServer
  val ctx by lazy { TerpalContext.SqlServer(ds)  }

  beforeEach {
    ds.run("TRUNCATE TABLE Product; DBCC CHECKIDENT ('Person', RESEED, 1);")
  }

  "Ex 1 - Batch Insert Normal" {
    // Don't insert the id since that requires a special setting in SQL Server
    SqlBatch { p: Product -> "INSERT INTO Product (description, sku) VALUES (${p.description}, ${p.sku})" }
      .values(Ex1_BatchInsertNormal.products.asSequence()).action().runOn(ctx)
    Ex1_BatchInsertNormal.get.runOn(ctx) shouldBe Ex1_BatchInsertNormal.result
  }

  "Ex 2 - Batch Insert Mixed" {
    SqlBatch { p: Product -> "INSERT INTO Product (description, sku) VALUES (${id("BlahBlah")}, ${p.sku})" }
      .values(Ex2_BatchInsertMixed.products.asSequence()).action().runOn(ctx)
    Ex2_BatchInsertMixed.get.runOn(ctx) shouldBe Ex2_BatchInsertMixed.result
  }

  "Ex 3 - Batch Return Ids" {
    SqlBatch { p: Product -> "INSERT INTO Product (description, sku) VALUES (${p.description}, ${p.sku})" }
      .values(products.asSequence()).actionReturning<Int>().runOn(ctx) shouldBe Ex3_BatchReturnIds.opResult
    Ex3_BatchReturnIds.get.runOn(ctx) shouldBe Ex3_BatchReturnIds.result
  }

  "Ex 4 - Batch Return Record" {
    SqlBatch { p: Product -> "INSERT INTO Product (description, sku) OUTPUT INSERTED.id, INSERTED.description, INSERTED.sku VALUES (${p.description}, ${p.sku})" }
      .values(Ex4_BatchReturnRecord.products.asSequence()).actionReturning<Product>().runOn(ctx) shouldBe Ex4_BatchReturnRecord.opResult
    Ex4_BatchReturnRecord.get.runOn(ctx) shouldBe Ex4_BatchReturnRecord.result
  }
})
