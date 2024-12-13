package io.exoquery.terpal

fun stuff(str: String): String = str

fun main() { // hello
  printSource {
    stuff("""
      |hello
      |world
    """.trimMargin())
  }
}