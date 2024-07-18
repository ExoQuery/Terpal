package io.exoquery.terpal

class ParseError(val msg: String) : Throwable(msg)
fun parseError(msg: String): Nothing = throw ParseError(msg)
