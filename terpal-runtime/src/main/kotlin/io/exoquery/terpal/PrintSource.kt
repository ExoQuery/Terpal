package io.exoquery.terpal

fun <R> printSource(f: () -> R): String = error("Compile time plugin did not transform the tree")
fun printSourceExpr(input: String): String = input
