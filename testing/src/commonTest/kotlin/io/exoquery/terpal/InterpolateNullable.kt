package io.exoquery.terpal

import kotlin.test.Test

class InterpolateNullable: InterpolateTestBase {
  companion object {
    data class In(val value: String)
    data class Out(val parts: List<String>, val params: List<In?>, val info: String)

    object StaticTerp: Interpolator<In?, Out> {
      operator fun invoke(string: String): Out = Messages.throwPluginNotExecuted()
      override fun interpolate(parts: () -> List<String>, params: () -> List<In?>): Out =
        Out(parts(), params(), "Static")
    }

    class InstanceTerp(val info: String): Interpolator<In?, Out> {
      override fun interpolate(parts: () -> List<String>, params: () -> List<In?>): Out =
        Out(parts(), params(), info)
    }
  }

  val instanceTerp = InstanceTerp("Dynamic")

  val A: In? = null
  val B: In? = In("B")
  val C: In? = null

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
    StaticTerp("foo_${B}_bar") shouldBe
      Out(listOf("foo_", "_bar"), listOf(B), "Static")
  }

  @Test
  fun complexStaticTest1() {
    StaticTerp("foo_${A}_bar_${B}${C}_baz") shouldBe
      Out(listOf("foo_", "_bar_", "", "_baz"), listOf(A, B, C), "Static")
  }
}
