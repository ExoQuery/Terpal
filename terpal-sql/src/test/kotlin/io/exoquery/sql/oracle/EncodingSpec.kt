package io.exoquery.sql.oracle

import io.exoquery.sql.*
import io.exoquery.sql.EncodingSpecData.EncodingTestEntity
import io.exoquery.sql.EncodingSpecData.SerializeableTestType
import io.exoquery.sql.jdbc.Sql
import io.exoquery.sql.EncodingSpecData.insert
import io.exoquery.sql.jdbc.JdbcEncodingBasic.Companion.StringEncoder
import io.exoquery.sql.jdbc.TerpalContext
import io.exoquery.sql.jdbc.runOn
import io.kotest.core.spec.style.FreeSpec
import java.time.ZoneId
import io.exoquery.sql.EncodingSpecData.TimeEntity
import io.exoquery.sql.EncodingSpecData.insertBatch
import io.exoquery.sql.EncodingSpecData.shouldBeEqualNullable
import io.exoquery.sql.EncodingSpecData.shouldBeEqualNullableIgnoringScale
import io.kotest.matchers.bigdecimal.shouldBeEqualIgnoringScale
import io.kotest.matchers.equals.shouldBeEqual
import kotlin.test.assertEquals

class EncodingSpec: FreeSpec({
  val ds = TestDatabases.oracle
  val ctx by lazy {
    object: TerpalContext.Oracle(ds) {
      override val additionalEncoders = super.additionalEncoders + StringEncoder.contramap { ett: EncodingSpecData.SerializeableTestType -> ett.value }
    }
  }


  infix fun String?.shouldBeEqualEmptyNullable(expected: String?) {
    val actualOrEmpty = this ?: ""
    val expectedOrEmpty = expected ?: ""
    assertEquals(expectedOrEmpty, actualOrEmpty)
  }

  infix fun ByteArray?.shouldBeEqualEmptyNullable(expected: ByteArray?) {
    val actualOrEmpty = this ?: byteArrayOf()
    val expectedOrEmpty = expected ?: byteArrayOf()
    assertEquals(expectedOrEmpty.toList(), actualOrEmpty.toList())
  }

  infix fun SerializeableTestType?.shouldBeEqualEmptyNullable(expected: SerializeableTestType?) {
    val actualOrEmpty = this ?: SerializeableTestType("")
    val expectedOrEmpty = expected ?: SerializeableTestType("")
    assertEquals(expectedOrEmpty, actualOrEmpty)
  }

  fun verify(e1: EncodingTestEntity, e2: EncodingTestEntity) {
    e1.v1 shouldBeEqual e2.v1
    e1.v2 shouldBeEqualIgnoringScale e2.v2
    e1.v3 shouldBeEqual e2.v3
    e1.v4 shouldBeEqual e2.v4
    e1.v5 shouldBeEqual e2.v5
    e1.v6 shouldBeEqual e2.v6
    e1.v7 shouldBeEqual e2.v7
    e1.v8 shouldBeEqual e2.v8
    e1.v9 shouldBeEqual e2.v9
    e1.v10.toList() shouldBeEqual e2.v10.toList()
    e1.v11 shouldBeEqual e2.v11
    e1.v12 shouldBeEqual e2.v12
    e1.v13 shouldBeEqual e2.v13
    e1.v14 shouldBeEqual e2.v14

    e1.o1 shouldBeEqualEmptyNullable e2.o1
    e1.o2 shouldBeEqualNullableIgnoringScale e2.o2
    e1.o3 shouldBeEqualNullable e2.o3
    e1.o4 shouldBeEqualNullable e2.o4
    e1.o5 shouldBeEqualNullable e2.o5
    e1.o6 shouldBeEqualNullable e2.o6
    e1.o7 shouldBeEqualNullable e2.o7
    e1.o8 shouldBeEqualNullable e2.o8
    e1.o9 shouldBeEqualNullable e2.o9
    e1.o10 shouldBeEqualEmptyNullable e2.o10
    e1.o11 shouldBeEqualNullable e2.o11
    e1.o12 shouldBeEqualEmptyNullable e2.o12
    e1.o13 shouldBeEqualNullable e2.o13
    e1.o14 shouldBeEqualNullable e2.o14
    //e1.o15 mustEqual e2.o15
  }

  beforeEach {
    ds.run("DELETE FROM EncodingTestEntity")
  }

  "encodes and decodes nullables - not nulls" {
    ctx.run(insert(EncodingSpecData.regularEntity))
    val res = ctx.run(Sql("SELECT * FROM EncodingTestEntity").queryOf<EncodingSpecData.EncodingTestEntity>())
    verify(res.first(), EncodingSpecData.regularEntity)
  }

  "encodes and decodes batch" {
    insertBatch(listOf(EncodingSpecData.regularEntity, EncodingSpecData.regularEntity)).runOn(ctx)
    val res = Sql("SELECT * FROM EncodingTestEntity").queryOf<EncodingSpecData.EncodingTestEntity>().runOn(ctx)
    verify(res.get(0), EncodingSpecData.regularEntity)
    verify(res.get(1), EncodingSpecData.regularEntity)
  }



  "encodes and decodes nullables - nulls" {
    insert(EncodingSpecData.nullEntity).runOn(ctx)
    val res = Sql("SELECT * FROM EncodingTestEntity").queryOf<EncodingSpecData.EncodingTestEntity>().runOn(ctx)
    verify(res.first(), EncodingSpecData.nullEntity)
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
