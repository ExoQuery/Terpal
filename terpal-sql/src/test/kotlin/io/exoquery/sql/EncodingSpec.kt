package io.exoquery.sql

import io.exoquery.sql.jdbc.JdbcContext
import io.exoquery.sql.jdbc.JdbcEncodersWithTime.Companion.StringEncoder
import io.exoquery.sql.jdbc.Sql
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/*
  case class EncodingTestEntity(
    v1: String,
    v2: BigDecimal,
    v3: Boolean,
    v4: Byte,
    v5: Short,
    v6: Int,
    v7: Long,
    v8: Float,
    v9: Double,
    v10: Array[Byte],
    v11: Date,
    v12: EncodingTestType,
    v13: LocalDate,
    v14: UUID,
    o1: Option[String],
    o2: Option[BigDecimal],
    o3: Option[Boolean],
    o4: Option[Byte],
    o5: Option[Short],
    o6: Option[Int],
    o7: Option[Long],
    o8: Option[Float],
    o9: Option[Double],
    o10: Option[Array[Byte]],
    o11: Option[Date]
    o12: Option[EncodingTestType],
    o13: Option[LocalDate],
    o14: Option[UUID],
    o15: Option[Number]
  )
 */

object EncodingSpecData {
  @Serializable
  data class EncodingTestType(val value: String)

  @Serializable
  data class EncodingTestEntity(
    val v1: String,
    @Contextual val v2: BigDecimal,
    val v3: Boolean,
    val v4: Byte,
    val v5: Short,
    val v6: Int,
    val v7: Long,
    val v8: Float,
    val v9: Double,
    val v10: ByteArray,
    @Contextual val v11: java.util.Date,
    val v12: EncodingTestType,
    @Contextual val v13: java.time.LocalDate,
    @Contextual val v14: java.util.UUID,
    val o1: String?,
    @Contextual val o2: BigDecimal?,
    val o3: Boolean?,
    val o4: Byte?,
    val o5: Short?,
    val o6: Int?,
    val o7: Long?,
    val o8: Float?,
    val o9: Double?,
    val o10: ByteArray?,
    @Contextual val o11: java.util.Date?,
    val o12: EncodingTestType?,
    @Contextual val o13: java.time.LocalDate?,
    @Contextual val o14: java.util.UUID?
  )

  /*
  val insertValues =
    List[
      (
        String,
        BigDecimal,
        Boolean,
        Byte,
        Short,
        Int,
        Long,
        Float,
        Double,
        Array[Byte],
        java.util.Date,
        Option[String],
        Option[BigDecimal],
        Option[Boolean],
        Option[Byte],
        Option[Short],
        Option[Int],
        Option[Long],
        Option[Float],
        Option[Double],
        Option[Array[Byte]],
        Option[java.util.Date]
      )
    ](
      (
        "s",
        BigDecimal(1.1),
        true,
        11.toByte,
        23.toShort,
        33,
        431L,
        34.4f,
        42d,
        Array(1.toByte, 2.toByte),
        new Date(31200000),
        Some("s"),
        Some(BigDecimal(1.1)),
        Some(true),
        Some(11.toByte),
        Some(23.toShort),
        Some(33),
        Some(431L),
        Some(34.4f),
        Some(42d),
        Some(Array(1.toByte, 2.toByte)),
        Some(new Date(31200000))
      ),
      (
        "",
        BigDecimal(0),
        false,
        0.toByte,
        0.toShort,
        0,
        0L,
        0f,
        0d,
        Array(),
        new Date(0),
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        None
      )
    )
   */

  val insertValues =
    listOf<EncodingTestEntity>(
      EncodingTestEntity(
        "s",
        BigDecimal("1.1"),
        true,
        11.toByte(),
        23.toShort(),
        33,
        431L,
        34.4f,
        42.0,
        byteArrayOf(1.toByte(), 2.toByte()),
        java.sql.Date.from(LocalDateTime.of(2013, 11, 23, 0, 0, 0, 0).toInstant(ZoneOffset.UTC)),
        EncodingTestType("s"),
        LocalDate.of(2013, 11, 23),
        java.util.UUID.randomUUID(),
        "s",
        BigDecimal("1.1"),
        true,
        11.toByte(),
        23.toShort(),
        33,
        431L,
        34.4f,
        42.0,
        byteArrayOf(1.toByte(), 2.toByte()),
        java.sql.Date.from(LocalDateTime.of(2013, 11, 23, 0, 0, 0, 0).toInstant(ZoneOffset.UTC)),
        EncodingTestType("s"),
        LocalDate.of(2013, 11, 23),
        java.util.UUID.randomUUID()
      ),
      EncodingTestEntity(
        "",
        BigDecimal.ZERO,
        false,
        0.toByte(),
        0.toShort(),
        0,
        0L,
        0f,
        0.0,
        byteArrayOf(),
        java.sql.Date(0),
        EncodingTestType(""),
        LocalDate.ofEpochDay(0),
        java.util.UUID(0, 0),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
      )
    )

  fun insert(e: EncodingTestEntity) =
    Sql("INSERT INTO EncodingTestEntity VALUES (${e.v1}, ${e.v2}, ${e.v3}, ${e.v4}, ${e.v5}, ${e.v6}, ${e.v7}, ${e.v8}, ${e.v9}, ${e.v10}, ${e.v11}, ${e.v12}, ${e.v13}, ${e.v14}, ${e.o1})").action() // , ${e.o2}, ${e.o3}, ${e.o4}, ${e.o5}, ${e.o6}, ${e.o7}, ${e.o8}, ${e.o9}, ${e.o10}, ${e.o11}, ${e.o12}, ${e.o13}, ${e.o14})
}



/*

 */

class EncodingSpec: FreeSpec({
  val ctx by lazy {
    JdbcContext(GlobalEmbeddedPostgres.get().getPostgresDatabase()) {
      additionalEncoders = additionalEncoders + StringEncoder.contramap { ett: EncodingSpecData.EncodingTestType -> ett.value }
    }
  }

  beforeEach {
    GlobalEmbeddedPostgres.run("DELETE FROM EncodingTestEntity")
  }

  "encodes and decodes types" {
    EncodingSpecData.insertValues.forEach {
      ctx.run(EncodingSpecData.insert(it))
    }
    val res = ctx.run(Sql("SELECT * FROM EncodingTestEntity").queryOf<EncodingSpecData.EncodingTestEntity>())
    res shouldBe EncodingSpecData.insertValues
  }
})

