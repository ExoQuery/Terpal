package io.exoquery.sql.mysql

import io.exoquery.sql.*
import io.exoquery.sql.Ex3_BatchReturnIds.products
import io.exoquery.sql.jdbc.SqlBatch
import io.exoquery.sql.jdbc.TerpalContext
import io.exoquery.sql.jdbc.runOn
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class BatchValuesSpec: FreeSpec ({
  val ds = TestDatabases.mysql
  val ctx by lazy { TerpalContext.Mysql(ds)  }

  beforeEach {
    ds.run("""
      DELETE FROM Product;
      ALTER TABLE Product AUTO_INCREMENT = 1;
    """)
  }

  "Ex 1 - Batch Insert Normal" {
    Ex1_BatchInsertNormal.op.runOn(ctx)
    Ex1_BatchInsertNormal.get.runOn(ctx) shouldBe Ex1_BatchInsertNormal.result
  }

  "Ex 2 - Batch Insert Mixed" {
    Ex2_BatchInsertMixed.op.runOn(ctx)
    Ex2_BatchInsertMixed.get.runOn(ctx) shouldBe Ex2_BatchInsertMixed.result
  }

  "Ex 3 - Batch Return Ids" {
    SqlBatch { p: Product -> "INSERT INTO Product (description, sku) VALUES (${p.description}, ${p.sku})" }
      .values(products.asSequence()).actionReturning<Int>().runOn(ctx) shouldBe Ex3_BatchReturnIds.opResult
    Ex3_BatchReturnIds.get.runOn(ctx) shouldBe Ex3_BatchReturnIds.result
  }
})
