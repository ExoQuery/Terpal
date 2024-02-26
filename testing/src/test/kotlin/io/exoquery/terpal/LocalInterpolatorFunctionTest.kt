package io.exoquery.terpal

import kotlin.test.Test

class LocalInterpolatorFunctionTest: InterpolateTestBase {
  data class Stmt(val parts: List<String>, val params: List<Stmt>, val info: String)

  object StaticTerp: Interpolator<Stmt, Stmt> {
    override fun interpolate(parts: () -> List<String>, params: () -> List<Stmt>): Stmt =
      Stmt(parts(), params(), "local")
  }

  @InterpolatorFunction<StaticTerp>(StaticTerp::class)
  fun staticTerp(str: String): Stmt = interpolatorBody()

  @InterpolatorFunction<StaticTerp>(StaticTerp::class)
  operator fun String.unaryPlus(): Stmt = interpolatorBody()

  val A = Stmt(listOf("A"), listOf(), "local")
  val B = Stmt(listOf("B"), listOf(), "local")
  val C = Stmt(listOf("C"), listOf(), "local")

  @Test
  fun simpleConstantTest() {
    +"foo" shouldBe
      Stmt(listOf("foo"), listOf(), "local")
  }

  @Test
  fun complexStaticTest1() {
    +"foo_${A}_bar_${B}${C}_baz" shouldBe
      Stmt(listOf("foo_", "_bar_", "", "_baz"), listOf(A, B, C), "local")
  }

  @Test
  fun nestedTest() {
    +"foo_${+"middle"}_bar" shouldBe
      Stmt(listOf("foo_", "_bar"), listOf(Stmt(listOf("middle"), listOf(), "local")), "local")
  }
}
