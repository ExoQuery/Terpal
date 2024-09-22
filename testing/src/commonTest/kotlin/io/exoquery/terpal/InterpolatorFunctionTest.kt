package io.exoquery.terpal

import kotlin.test.Test

@InterpolatorFunction<InterpolatorFunctionTest.StaticTerp>(InterpolatorFunctionTest.StaticTerp::class)
fun staticTerp(str: String): InterpolatorFunctionTest.Stmt = Messages.throwPluginNotExecuted()

@InterpolatorFunction<InterpolatorFunctionTest.StaticTerp>(InterpolatorFunctionTest.StaticTerp::class)
operator fun String.unaryPlus(): InterpolatorFunctionTest.Stmt = Messages.throwPluginNotExecuted()

class InterpolatorFunctionTest: InterpolateTestBase {
  data class Stmt(val parts: List<String>, val params: List<Stmt>)

  object StaticTerp: Interpolator<Stmt, Stmt> {
    operator fun invoke(string: String): Stmt = Messages.throwPluginNotExecuted()
    override fun interpolate(parts: () -> List<String>, params: () -> List<Stmt>): Stmt =
      Stmt(parts(), params())
  }

  val A = Stmt(listOf("A"), listOf())
  val B = Stmt(listOf("B"), listOf())
  val C = Stmt(listOf("C"), listOf())

  @Test
  fun simpleConstantTestFunc() {
    staticTerp("foo") shouldBe
      Stmt(listOf("foo"), listOf())
  }

  @Test
  fun simpleConstantTest() {
    +"foo" shouldBe
      Stmt(listOf("foo"), listOf())
  }

  @Test
  fun complexStaticTest1() {
    +"foo_${A}_bar_${B}${C}_baz" shouldBe
      Stmt(listOf("foo_", "_bar_", "", "_baz"), listOf(A, B, C))
  }

  @Test
  fun nestedTest() {
    +"foo_${+"middle"}_bar" shouldBe
      Stmt(listOf("foo_", "_bar"), listOf(Stmt(listOf("middle"), listOf())))
  }
}
