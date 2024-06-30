package io.exoquery.sql.examples

import io.exoquery.sql.jdbc.JdbcContext
import io.exoquery.sql.jdbc.Sql
import io.exoquery.sql.jdbc.TerpalContext
import io.exoquery.sql.run
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

// TODO needed otherwise splices will be directly joined to parent string by Kotlin compiler.
//      need to talk about it in the docs.
fun <T> id(t: T) = t

object RowSurrogate {

  object DateAsLongSerialzier: KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeLong(value.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
    override fun deserialize(decoder: Decoder): LocalDate = Instant.ofEpochMilli(decoder.decodeLong()).atZone(ZoneOffset.UTC).toLocalDate()
  }

  // Use this Class/serialziation for Json
  @Serializable
  data class Customer(val id: Int, val firstName: String, val lastName: String, @Serializable(with = DateAsLongSerialzier::class) val createdAt: LocalDate)

  // Use this Class/serialization for SQL
  @Serializable
  data class CustomerSurrogate(val id: Int, val firstName: String, val lastName: String, @Contextual val createdAt: LocalDate) {
    fun toCustomer() = Customer(id, firstName, lastName, createdAt)
    companion object {
      fun fromCustomer(customer: Customer): CustomerSurrogate {
        return CustomerSurrogate(customer.id, customer.firstName, customer.lastName, customer.createdAt)
      }
    }
  }

  object CustomerSurrogateSerializer: KSerializer<Customer> {
    override val descriptor = CustomerSurrogate.serializer().descriptor
    override fun serialize(encoder: Encoder, value: Customer) = encoder.encodeSerializableValue(CustomerSurrogate.serializer(), CustomerSurrogate.fromCustomer(value))
    override fun deserialize(decoder: Decoder): Customer = decoder.decodeSerializableValue(CustomerSurrogate.serializer()).toCustomer()
  }

  suspend fun main() {
    val postgres = EmbeddedPostgres.start()
    postgres.run("CREATE TABLE customers (id SERIAL PRIMARY KEY, first_name TEXT, last_name TEXT, created_at DATE)")
    val ctx = TerpalContext.Postgres(postgres.postgresDatabase)
    ctx.run(Sql("INSERT INTO customers (first_name, last_name, created_at) VALUES (${id("Alice")}, ${id("Smith")}, ${id(LocalDate.of(2021, 1, 1))})").action())
    val customers = ctx.run(Sql("SELECT * FROM customers").queryOf<Customer>(CustomerSurrogateSerializer))
    println(Json.encodeToString(ListSerializer(Customer.serializer()), customers))
  }
}

suspend fun main() {
  RowSurrogate.main()
}