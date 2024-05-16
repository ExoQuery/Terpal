package io.exoquery.sql

import kotlinx.serialization.Serializable

@Serializable
data class Name(val firstName: String, val lastName: String)

@Serializable
data class Person(val id: Int, val name: Name, val age: Int)