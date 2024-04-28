package io.exoquery.terpal

import kotlin.test.Test

class InterpolateTest: InterpolateTestBase {
  companion object {
    data class In(val value: String)
    data class Out(val parts: List<String>, val params: List<In>, val info: String)

    object StaticTerp: Interpolator<In, Out> {
      override fun interpolate(parts: () -> List<String>, params: () -> List<In>): Out =
        Out(parts(), params(), "Static")
    }

    class InstanceTerp(val info: String): Interpolator<In, Out> {
      override fun interpolate(parts: () -> List<String>, params: () -> List<In>): Out =
        Out(parts(), params(), info)
    }
  }

  val instanceTerp = InstanceTerp("Dynamic")

  val A = In("A")
  val B = In("B")
  val C = In("C")

  @Test
  fun simpleStaticTest1() {
    StaticTerp("foo_${A}${B}${C}_baz") shouldBe
      Out(listOf("foo_", "", "", "_baz"), listOf(A, B, C), "Static")
  }

  @Test
  fun simpleStaticTest2() {
    StaticTerp("foo_${A}_bar") shouldBe
      Out(listOf("foo_", "_bar"), listOf(A), "Static")
  }

  @Test
  fun simpleStaticTest3() {
    StaticTerp("foo_${A}${B}${C}") shouldBe
      Out(listOf("foo_", "", "", ""), listOf(A, B, C), "Static")
  }

  @Test
  fun simpleStaticTest4() {
    StaticTerp("${A}${B}${C}_foo") shouldBe
      Out(listOf("", "", "", "_foo"), listOf(A, B, C), "Static")
  }

  @Test
  fun complexStaticTest1() {
    StaticTerp("foo_${A}_bar_${B}${C}_baz") shouldBe
      Out(listOf("foo_", "_bar_", "", "_baz"), listOf(A, B, C), "Static")
  }

  @Test
  fun complexDynamicTest1() {
    instanceTerp("foo_${A}_bar_${B}${C}_baz") shouldBe
      Out(listOf("foo_", "_bar_", "", "_baz"), listOf(A, B, C), "Dynamic")
  }
}
