package io.exoquery.sql.postgres

import io.exoquery.sql.*
import io.exoquery.sql.jdbc.Sql
import io.exoquery.sql.EncodingSpecData.insert
import io.exoquery.sql.jdbc.JdbcEncodingBasic.Companion.StringEncoder
import io.exoquery.sql.jdbc.TerpalContext
import io.exoquery.sql.jdbc.runOn
import io.kotest.core.spec.style.FreeSpec
import java.time.ZoneId
import io.exoquery.sql.EncodingSpecData.TimeEntity
import io.exoquery.sql.EncodingSpecData.insertBatch

class EncodingSpec: FreeSpec({
  val ds = TestDatabases.postgres
  val ctx by lazy {
    object: TerpalContext.Postgres(ds) {
      override val additionalEncoders = super.additionalEncoders + StringEncoder.contramap { ett: EncodingSpecData.SerializeableTestType -> ett.value }
    }
  }

  beforeEach {
    ds.run("DELETE FROM EncodingTestEntity")
  }

  "encodes and decodes nullables - not nulls" {
    ctx.run(insert(EncodingSpecData.regularEntity))
    val res = ctx.run(Sql("SELECT * FROM EncodingTestEntity").queryOf<EncodingSpecData.EncodingTestEntity>())
    EncodingSpecData.verify(res.first(), EncodingSpecData.regularEntity)
  }

  "encodes and decodes batch" {
    insertBatch(listOf(EncodingSpecData.regularEntity, EncodingSpecData.regularEntity)).runOn(ctx)
    val res = Sql("SELECT * FROM EncodingTestEntity").queryOf<EncodingSpecData.EncodingTestEntity>().runOn(ctx)
    EncodingSpecData.verify(res.get(0), EncodingSpecData.regularEntity)
    EncodingSpecData.verify(res.get(1), EncodingSpecData.regularEntity)
  }

  "encodes and decodes nullables - nulls" {
    insert(EncodingSpecData.nullEntity).runOn(ctx)
    val res = Sql("SELECT * FROM EncodingTestEntity").queryOf<EncodingSpecData.EncodingTestEntity>().runOn(ctx)
    EncodingSpecData.verify(res.first(), EncodingSpecData.nullEntity)
  }

  "Encode/Decode Other Time Types" {
    Sql("DELETE FROM TimeEntity").action().runOn(ctx)
    val zid = ZoneId.systemDefault()
    val timeEntity = TimeEntity.make(zid)
    ctx.run(insert(timeEntity))
    val actual = ctx.run(Sql("SELECT * FROM TimeEntity").queryOf<TimeEntity>()).first()
    assert(timeEntity == actual)
  }

  "Encode/Decode Other Time Types ordering" {
    Sql("DELETE FROM TimeEntity").action().runOn(ctx)

    val zid = ZoneId.systemDefault()
    val timeEntityA = TimeEntity.make(zid, TimeEntity.TimeEntityInput(2022, 1, 1, 1, 1, 1, 0))
    val timeEntityB = TimeEntity.make(zid, TimeEntity.TimeEntityInput(2022, 2, 2, 2, 2, 2, 0))

    // Importing extras messes around with the quto-quote, need to look into why
    ctx.run(insert(timeEntityA))
    ctx.run(insert(timeEntityB))

    assert(timeEntityB.sqlDate > timeEntityA.sqlDate)
    assert(timeEntityB.sqlTime > timeEntityA.sqlTime)
    assert(timeEntityB.sqlTimestamp > timeEntityA.sqlTimestamp)
    assert(timeEntityB.timeLocalDate > timeEntityA.timeLocalDate)
    assert(timeEntityB.timeLocalTime > timeEntityA.timeLocalTime)
    assert(timeEntityB.timeLocalDateTime > timeEntityA.timeLocalDateTime)
    assert(timeEntityB.timeZonedDateTime > timeEntityA.timeZonedDateTime)
    assert(timeEntityB.timeInstant > timeEntityA.timeInstant)
    assert(timeEntityB.timeOffsetTime > timeEntityA.timeOffsetTime)
    assert(timeEntityB.timeOffsetDateTime > timeEntityA.timeOffsetDateTime)

    val actual =
      Sql("""
          SELECT * FROM TimeEntity 
          WHERE 
            sqlDate > ${timeEntityA.sqlDate} 
            AND sqlTime > ${timeEntityA.sqlTime}
            AND sqlTimestamp > ${timeEntityA.sqlTimestamp}
            AND timeLocalDate > ${timeEntityA.timeLocalDate}
            AND timeLocalTime > ${timeEntityA.timeLocalTime}
            AND timeLocalDateTime > ${timeEntityA.timeLocalDateTime}
            AND timeZonedDateTime > ${timeEntityA.timeZonedDateTime}
            AND timeInstant > ${timeEntityA.timeInstant}
            AND timeOffsetTime > ${timeEntityA.timeOffsetTime}
            AND timeOffsetDateTime > ${timeEntityA.timeOffsetDateTime}
          """
      ).queryOf<TimeEntity>().runOn(ctx).first()

    assert(actual == timeEntityB)
  }


})
