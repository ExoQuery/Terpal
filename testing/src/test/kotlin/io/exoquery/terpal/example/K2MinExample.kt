package io.exoquery.terpal.example

import io.exoquery.terpal.*

import kotlin.test.Test

data class In(val value: String)
data class Out(val parts: List<String>, val params: List<In>, val info: String)

object StaticTerp: Interpolator<In, Out> {
  override fun interpolate(parts: () -> List<String>, params: () -> List<In>): Out =
    Out(parts(), params(), "Static")
}

val A = In("A")
val B = In("B")
val C = In("C")

fun main() {
  StaticTerp("foo_${A}${B}${C}_baz")
}
