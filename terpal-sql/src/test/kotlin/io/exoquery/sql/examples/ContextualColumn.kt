package io.exoquery.sql.examples

import io.exoquery.sql.jdbc.JdbcContext
import io.exoquery.sql.jdbc.Sql
import io.exoquery.sql.jdbc.TerpalContext
import io.exoquery.sql.jdbc.runOn
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object ContextualColumn {

  object DateAsStringSerialzier: KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.format(DateTimeFormatter.ISO_DATE))
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.from(DateTimeFormatter.ISO_DATE.parse(decoder.decodeString()))
  }

  @Serializable
  data class Customer(val id: Int, val firstName: String, val lastName: String, @Contextual val createdAt: LocalDate)

  suspend fun main() {
    val postgres = EmbeddedPostgres.start()
    postgres.run("CREATE TABLE customers (id SERIAL PRIMARY KEY, first_name TEXT, last_name TEXT, created_at DATE)")
    val ctx = TerpalContext.Postgres(postgres.postgresDatabase)
    Sql("INSERT INTO customers (first_name, last_name, created_at) VALUES (${("Alice")}, ${("Smith")}, ${(LocalDate.of(2021, 1, 1))})").action().runOn(ctx)
    val customers = ctx.run(Sql("SELECT * FROM customers").queryOf<Customer>())
    val module = SerializersModule { contextual(LocalDate::class, DateAsStringSerialzier) }
    val json = Json { serializersModule = module }
    println(json.encodeToString(ListSerializer(Customer.serializer()), customers))
  }
}

suspend fun main() {
  ContextualColumn.main()
}