package io.exoquery.sql.examples

import io.exoquery.sql.jdbc.*
import io.exoquery.sql.jdbc.JdbcDecodersWithTimeLegacy.Companion.ZonedDateTimeDecoder
import io.exoquery.sql.run
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone
import javax.sql.DataSource

object ContextualColumnCustom {

  data class MyDateTime(val year: Int, val day: Int, val month: Int, val timeZone: TimeZone) {
    override fun toString(): String = ZonedDateTime.of(year, month, day, 0, 0, 0, 0, timeZone.toZoneId()).format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
    companion object {
      fun fromString(s: String): MyDateTime {
        val zdt = ZonedDateTime.from(DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(s))
        return MyDateTime(zdt.year, zdt.dayOfMonth, zdt.monthValue, TimeZone.getTimeZone(zdt.zone))
      }
      fun fromLocalDate(ld: LocalDate): MyDateTime = MyDateTime(ld.year, ld.dayOfMonth, ld.monthValue, TimeZone.getTimeZone("UTC"))
    }
  }

  object MyDateTimeAsStringSerialzier: KSerializer<MyDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: MyDateTime) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): MyDateTime = MyDateTime.fromString(decoder.decodeString())
  }

  @Serializable
  data class Customer(val id: Int, val firstName: String, val lastName: String, @Contextual val createdAt: MyDateTime)

  class MyContext(ds: DataSource): JdbcContext(ds) {
    companion object {
      val Decoders = JdbcDecodersWithTimeLegacy + ZonedDateTimeDecoder.map { zd -> MyDateTime(zd.year, zd.dayOfMonth, zd.monthValue, TimeZone.getTimeZone(zd.zone)) }
      val Encoders = JdbcEncodersWithTimeLegacy()
    }
    override val decoders = Decoders
    override val encoders = Encoders
  }

  suspend fun main() {
    val postgres = EmbeddedPostgres.start()
    postgres.run("CREATE TABLE customers (id SERIAL PRIMARY KEY, first_name TEXT, last_name TEXT, created_at TIMESTAMP WITH TIME ZONE)")
    val ctx = MyContext(postgres.postgresDatabase)
    ctx.run(Sql("INSERT INTO customers (first_name, last_name, created_at) VALUES (${id("Alice")}, ${id("Smith")}, ${id(ZonedDateTime.of(2021, 1, 1, 1, 2, 3, 0, ZoneOffset.UTC))})").action())
    val customers = ctx.run(Sql("SELECT * FROM customers").queryOf<Customer>())
    val module = SerializersModule { contextual(MyDateTime::class, MyDateTimeAsStringSerialzier) }
    val json = Json { serializersModule = module }
    println(json.encodeToString(ListSerializer(Customer.serializer()), customers))
  }
}

suspend fun main() {
  ContextualColumnCustom.main()
}