package io.exoquery.terpal

class ParseError(msg: String) : Exception(msg)
fun parseError(msg: String): Nothing = throw ParseError(msg)
