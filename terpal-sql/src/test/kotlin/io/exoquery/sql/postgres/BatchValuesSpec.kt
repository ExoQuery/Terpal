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

  /*
  "Ex 1 - Batch Insert Normal" in {
    import `Ex 1 - Batch Insert Normal`._
    testContext.run(op, batchSize)
    testContext.run(get) mustEqual result
  }*/

  "Ex 1 - Batch Insert Normal" {
    Ex1_BatchInsertNormal.op.runOn(ctx)
    Ex1_BatchInsertNormal.get.runOn(ctx) shouldBe Ex1_BatchInsertNormal.result
  }

  /*
    "Ex 2 - Batch Insert Returning" in {
    import `Ex 2 - Batch Insert Returning`._
    val ids = testContext.run(op, batchSize)
    ids mustEqual expectedIds
    testContext.run(get) mustEqual result
  }
   */

  "Ex 2 - Batch Insert Returning" {
    Ex2_BatchInsertReturning.op.runOn(ctx) shouldBe Ex2_BatchInsertReturning.expectedIds
    Ex2_BatchInsertReturning.get.runOn(ctx) shouldBe Ex2_BatchInsertReturning.result
  }

  /*
  "Ex 3 - Batch Insert Mixed" in {
    import `Ex 3 - Batch Insert Mixed`._
    testContext.run(op, batchSize)
    testContext.run(get) mustEqual result
  }
   */

  "Ex 3 - Batch Insert Mixed" {
    Ex3_BatchInsertMixed.op.runOn(ctx)
    Ex3_BatchInsertMixed.get.runOn(ctx) shouldBe Ex3_BatchInsertMixed.result
  }

})
