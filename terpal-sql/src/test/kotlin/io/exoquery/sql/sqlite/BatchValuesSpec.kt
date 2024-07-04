package io.exoquery.sql.sqlite

import io.exoquery.sql.*
import io.exoquery.sql.jdbc.TerpalContext
import io.exoquery.sql.jdbc.runOn
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class BatchValuesSpec: FreeSpec ({
  val ds = TestDatabases.sqlite
  val ctx by lazy { TerpalContext.Sqlite(ds)  }

  beforeEach {
    ds.run("DELETE FROM Product")
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
    Ex3_BatchReturnIds.op.runOn(ctx) shouldBe Ex3_BatchReturnIds.opResult
    Ex3_BatchReturnIds.get.runOn(ctx) shouldBe Ex3_BatchReturnIds.result
  }

  "Ex 4 - Batch Return Record" {
    Ex4_BatchReturnRecord.op.runOn(ctx) shouldBe Ex4_BatchReturnRecord.opResult
    Ex4_BatchReturnRecord.get.runOn(ctx) shouldBe Ex4_BatchReturnRecord.result
  }
})
