//@file:UseSerializers(DateAsLongSerializer::class)

package io.exoquery.sql

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.serializer
import java.time.LocalDate

@Serializable
data class Name(val firstName: String, val lastName: String)


//typealias DateAsLong = @Serializable(DateAsLongSerializer::class) LocalDate

@Serializable
data class Person(val id: Int, val name: Name, val age: Int, @Contextual val lastUpdated: LocalDate)