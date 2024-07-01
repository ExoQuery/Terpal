//@file:UseSerializers(DateAsLongSerializer::class)

package io.exoquery.sql

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.serializer
import java.time.LocalDate

@Serializable
data class Product(val id: Int, val description: String, val sku: Long)

fun makeProducts(num: Int): List<Product> = (1..num).map { Product(it, "Product-$it", it.toLong()) }
