package io.exoquery.sql.postgres

import io.exoquery.sql.*
import io.exoquery.sql.examples.id
import io.exoquery.sql.jdbc.Sql
import io.exoquery.sql.jdbc.SqlBatch
import io.exoquery.sql.jdbc.TerpalContext
import io.exoquery.sql.jdbc.runOn
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class BatchValuesSpec: FreeSpec ({
  val ds = TestDatabases.postgres
  val ctx by lazy { TerpalContext.Postgres(ds)  }

  beforeEach {
    ds.run("TRUNCATE TABLE Product RESTART IDENTITY CASCADE")
  }

  "Ex 1 - Batch Insert Normal" {
    Ex1_BatchInsertNormal.op.runOn(ctx)
    Ex1_BatchInsertNormal.get.runOn(ctx) shouldBe Ex1_BatchInsertNormal.result
  }

  "Ex 2 - Batch Insert Returning" {
    Ex2_BatchInsertReturning.op.runOn(ctx) shouldBe Ex2_BatchInsertReturning.expectedIds
    Ex2_BatchInsertReturning.get.runOn(ctx) shouldBe Ex2_BatchInsertReturning.result
  }

  "Ex 3 - Batch Insert Mixed" {
    Ex3_BatchInsertMixed.op.runOn(ctx)
    Ex3_BatchInsertMixed.get.runOn(ctx) shouldBe Ex3_BatchInsertMixed.result
  }

  "Ex 4 - Batch Return Ids" {
    Ex4_BatchReturnIds.op.runOn(ctx) shouldBe Ex4_BatchReturnIds.opResult
    Ex4_BatchReturnIds.get.runOn(ctx) shouldBe Ex4_BatchReturnIds.result
  }

  "Ex 5 - Batch Return Record" {
    Ex5_BatchReturnRecord.op.runOn(ctx) shouldBe Ex5_BatchReturnRecord.opResult
    Ex5_BatchReturnRecord.get.runOn(ctx) shouldBe Ex5_BatchReturnRecord.result
  }
})
