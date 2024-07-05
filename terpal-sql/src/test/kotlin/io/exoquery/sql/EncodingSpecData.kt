package io.exoquery.sql

import io.exoquery.sql.jdbc.Sql
import io.exoquery.sql.jdbc.SqlBatch
import io.kotest.matchers.bigdecimal.shouldBeEqualIgnoringScale
import io.kotest.matchers.equals.shouldBeEqual
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal
import java.sql.Date
import java.time.*
import java.util.*
import kotlin.test.assertEquals

object EncodingSpecData {

  @Serializable
  data class TimeEntity(
    @Contextual val sqlDate: java.sql.Date,                      // DATE
    @Contextual val sqlTime: java.sql.Time,                      // TIME
    @Contextual val sqlTimestamp: java.sql.Timestamp,            // DATETIME
    @Contextual val timeLocalDate: java.time.LocalDate,          // DATE
    @Contextual val timeLocalTime: java.time.LocalTime,          // TIME
    @Contextual val timeLocalDateTime: java.time.LocalDateTime,  // DATETIME
    @Contextual val timeZonedDateTime: java.time.ZonedDateTime,  // DATETIMEOFFSET
    @Contextual val timeInstant: java.time.Instant,              // DATETIMEOFFSET
    @Contextual val timeOffsetTime: java.time.OffsetTime,        // TIME
    @Contextual val timeOffsetDateTime: java.time.OffsetDateTime // DATETIMEOFFSET
  ) {
    override fun equals(other: Any?): Boolean =
      when (other) {
        is TimeEntity ->
          this.sqlDate == other.sqlDate &&
            this.sqlTime == other.sqlTime &&
            this.sqlTimestamp == other.sqlTimestamp &&
            this.timeLocalDate == other.timeLocalDate &&
            this.timeLocalTime == other.timeLocalTime &&
            this.timeLocalDateTime == other.timeLocalDateTime &&
            this.timeZonedDateTime.isEqual(other.timeZonedDateTime) &&
            this.timeInstant == other.timeInstant &&
            this.timeOffsetTime.isEqual(other.timeOffsetTime) &&
            this.timeOffsetDateTime.isEqual(other.timeOffsetDateTime)
        else -> false
      }

    data class TimeEntityInput(val year: Int, val month: Int, val day: Int, val hour: Int, val minute: Int, val second: Int, val nano: Int) {
      fun toLocalDate() = LocalDateTime.of(year, month, day, hour, minute, second, nano)
      companion object {
        val default = TimeEntityInput(2022, 1, 2, 3, 4, 6, 0)
      }
    }

    companion object {
      fun make(zoneIdRaw: ZoneId, timeEntity: TimeEntityInput = TimeEntityInput.default) = run {
        val zoneId = zoneIdRaw.normalized()
        // Millisecond precisions in SQL Server and many contexts are wrong so not using them
        val nowInstant = timeEntity.toLocalDate().atZone(zoneId).toInstant()
        val nowDateTime = LocalDateTime.ofInstant(nowInstant, zoneId)
        val nowDate = nowDateTime.toLocalDate()
        val nowTime = nowDateTime.toLocalTime()
        val nowZoned = ZonedDateTime.of(nowDateTime, zoneId)
        TimeEntity(
          java.sql.Date.valueOf(nowDate),
          java.sql.Time.valueOf(nowTime),
          java.sql.Timestamp.valueOf(nowDateTime),
          nowDate,
          nowTime,
          nowDateTime,
          nowZoned,
          nowInstant,
          OffsetTime.ofInstant(nowInstant, zoneId),
          OffsetDateTime.ofInstant(nowInstant, zoneId)
        )
      }
    }
  }

  fun insert(e: TimeEntity): Action {
    return Sql("INSERT INTO TimeEntity VALUES (${e.sqlDate}, ${e.sqlTime}, ${e.sqlTimestamp}, ${e.timeLocalDate}, ${e.timeLocalTime}, ${e.timeLocalDateTime}, ${e.timeZonedDateTime}, ${e.timeInstant}, ${e.timeOffsetTime}, ${e.timeOffsetDateTime})").action()
  }

  object TestTypeSerialzier: KSerializer<SerializeableTestType> {
    override val descriptor = PrimitiveSerialDescriptor("SerializeableTestType", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: SerializeableTestType) = encoder.encodeString(value.value)
    override fun deserialize(decoder: Decoder): SerializeableTestType = SerializeableTestType(decoder.decodeString())
  }

  @Serializable(with = TestTypeSerialzier::class)
  data class SerializeableTestType(val value: String)

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
    val v12: SerializeableTestType,
    @Contextual val v13: LocalDate,
    @Contextual val v14: UUID,
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
    val o12: SerializeableTestType?,
    @Contextual val o13: LocalDate?,
    @Contextual val o14: UUID?
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

  val regularEntity =
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
      Date.from(LocalDateTime.of(2013, 11, 23, 0, 0, 0, 0).toInstant(ZoneOffset.UTC)),
      SerializeableTestType("s"),
      LocalDate.of(2013, 11, 23),
      UUID.randomUUID(),
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
      Date.from(LocalDateTime.of(2013, 11, 23, 0, 0, 0, 0).toInstant(ZoneOffset.UTC)),
      SerializeableTestType("s"),
      LocalDate.of(2013, 11, 23),
      UUID.randomUUID()
    )

  val nullEntity =
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
      Date(0),
      SerializeableTestType(""),
      LocalDate.ofEpochDay(0),
      UUID(0, 0),
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

  fun insert(e: EncodingTestEntity): Action {
    val v12: Param<SerializeableTestType> = Param.withSer(e.v12, SerializeableTestType.serializer())
    val v14: Param<UUID> = Param.ctx(e.v14)
    val o12: Param<SerializeableTestType> = Param.withSer(e.o12, SerializeableTestType.serializer())
    val o14: Param<UUID> = Param.ctx(e.o14)
    return Sql("INSERT INTO EncodingTestEntity VALUES (${e.v1}, ${e.v2}, ${e.v3}, ${e.v4}, ${e.v5}, ${e.v6}, ${e.v7}, ${e.v8}, ${e.v9}, ${e.v10}, ${e.v11}, ${v12}, ${e.v13}, ${v14}, ${e.o1}, ${e.o2}, ${e.o3}, ${e.o4}, ${e.o5}, ${e.o6}, ${e.o7}, ${e.o8}, ${e.o9}, ${e.o10}, ${e.o11}, ${o12}, ${e.o13}, ${o14})").action()
  }

  fun insertBatch(es: List<EncodingTestEntity>): BatchAction {
    fun v12(v12: SerializeableTestType): Param<SerializeableTestType> = Param.withSer(v12, SerializeableTestType.serializer())
    fun v14(v14: UUID): Param<UUID> = Param.ctx(v14)
    fun o12(o12: SerializeableTestType?): Param<SerializeableTestType> = Param.withSer(o12, SerializeableTestType.serializer())
    fun o14(o14: UUID?): Param<UUID> = Param.ctx(o14)

    return SqlBatch { e: EncodingTestEntity ->
      "INSERT INTO EncodingTestEntity VALUES (${e.v1}, ${e.v2}, ${e.v3}, ${e.v4}, ${e.v5}, ${e.v6}, ${e.v7}, ${e.v8}, ${e.v9}, ${e.v10}, ${e.v11}, ${v12(e.v12)}, ${e.v13}, ${v14(e.v14)}, ${e.o1}, ${e.o2}, ${e.o3}, ${e.o4}, ${e.o5}, ${e.o6}, ${e.o7}, ${e.o8}, ${e.o9}, ${e.o10}, ${e.o11}, ${o12(e.o12)}, ${e.o13}, ${o14(e.o14)})"
    }.values(es.asSequence()).action()
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

    e1.o1 shouldBeEqualNullable e2.o1
    e1.o2 shouldBeEqualNullableIgnoringScale e2.o2
    e1.o3 shouldBeEqualNullable e2.o3
    e1.o4 shouldBeEqualNullable e2.o4
    e1.o5 shouldBeEqualNullable e2.o5
    e1.o6 shouldBeEqualNullable e2.o6
    e1.o7 shouldBeEqualNullable e2.o7
    e1.o8 shouldBeEqualNullable e2.o8
    e1.o9 shouldBeEqualNullable e2.o9
    (e1.o10?.let { it.toList() } ?: listOf()) shouldBeEqual (e2.o10?.let { it.toList() } ?: listOf())
    e1.o11 shouldBeEqualNullable e2.o11
    e1.o12 shouldBeEqualNullable e2.o12
    e1.o13 shouldBeEqualNullable e2.o13
    e1.o14 shouldBeEqualNullable e2.o14
    //e1.o15 mustEqual e2.o15
  }

  public infix fun <A> A?.shouldBeEqualNullable(expected: A?) =
    assertEquals(expected, this)

  public infix fun BigDecimal?.shouldBeEqualNullableIgnoringScale(expected: BigDecimal?) =
    if (this == null && expected == null) Unit
    else if (this == null || expected == null) assertEquals(this, expected) // i.e. will always be false
    else this.shouldBeEqualIgnoringScale(expected) // otherwise they are both not null and we compare by scale
}