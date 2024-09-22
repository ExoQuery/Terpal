package io.exoquery.terpal

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

class InterpolateSimpleTest: InterpolateTestBase {
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
  val E: In by lazy { throw IllegalArgumentException("blah") }

  @Test
  fun simpleConstantTest() {
    StaticTerp("foo") shouldBe
      Out(listOf("foo"), listOf(), "Static")
  }

  @Test
  fun simpleStaticTest1() {
    StaticTerp("foo_${A}${B}${C}_baz") shouldBe
      Out(listOf("foo_", "", "", "_baz"), listOf(A, B, C), "Static")
  }

  @Test
  fun exceptionTest() {
    val ex = assertFailsWith<InterpolationException> {
      StaticTerp("foo_${A}${E}${C}_baz")
    }
    println("====== `InterpolateTopLevelBatchingTest.exceptionTest2` message: ${ex.msg}")
    assertContains(ex.msg, "Error in spliced")
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
