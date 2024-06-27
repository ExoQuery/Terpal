package io.exoquery.sql.postgres

import io.exoquery.sql.*
import io.exoquery.sql.jdbc.Sql
import io.exoquery.sql.EncodingSpecData.insert
import io.exoquery.sql.jdbc.JdbcEncodersWithTimeLegacy.Companion.StringEncoder
import io.exoquery.sql.jdbc.TerpalContext
import io.exoquery.sql.jdbc.runOn
import io.kotest.core.spec.style.FreeSpec
import java.time.ZoneId
import io.exoquery.sql.EncodingSpecData.TimeEntity

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

  "encodes and decodes nullables - nulls" {
    ctx.run(insert(EncodingSpecData.nullEntity))
    val res = ctx.run(Sql("SELECT * FROM EncodingTestEntity").queryOf<EncodingSpecData.EncodingTestEntity>())
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


  /*
  // Scala/Quill

  "Encode/Decode Other Time Types ordering" in {
    context.run(query[TimeEntity].delete)

    val zid         = ZoneId.systemDefault()
    val timeEntityA = TimeEntity.make(zid, TimeEntity.TimeEntityInput(2022, 1, 1, 1, 1, 1, 0))
    val timeEntityB = TimeEntity.make(zid, TimeEntity.TimeEntityInput(2022, 2, 2, 2, 2, 2, 0))

    // Importing extras messes around with the quto-quote, need to look into why
    import context.extras._
    context.run(quote(query[TimeEntity].insertValue(lift(timeEntityA))))
    context.run(quote(query[TimeEntity].insertValue(lift(timeEntityB))))



    val actual =
      context
        .run(
          query[TimeEntity].filter(t =>
            t.sqlDate > lift(timeEntityA.sqlDate) &&
              t.sqlTime > lift(timeEntityA.sqlTime) &&
              t.sqlTimestamp > lift(timeEntityA.sqlTimestamp) &&
              t.timeLocalDate > lift(timeEntityA.timeLocalDate) &&
              t.timeLocalTime > lift(timeEntityA.timeLocalTime) &&
              t.timeLocalDateTime > lift(timeEntityA.timeLocalDateTime) &&
              t.timeZonedDateTime > lift(timeEntityA.timeZonedDateTime) &&
              t.timeInstant > lift(timeEntityA.timeInstant) &&
              t.timeOffsetTime > lift(timeEntityA.timeOffsetTime) &&
              t.timeOffsetDateTime > lift(timeEntityA.timeOffsetDateTime)
          )
        )
        .head

    assert(actual == timeEntityB)
  }
   */


})
