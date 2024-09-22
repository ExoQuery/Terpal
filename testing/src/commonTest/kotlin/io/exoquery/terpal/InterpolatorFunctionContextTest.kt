package io.exoquery.terpal

import kotlin.test.Test

@InterpolatorFunction<McTerp>(McTerp::class, customReciever = true)
fun MyConext.staticContextTerp(str: String): Wrapped = Messages.throwPluginNotExecuted()

data class Wrapped(val parts: List<String>, val params: List<Wrapped>, val mc: MyConext)

data class MyConext(val stuff: String)

object McTerp: ProtoInterpolator<Wrapped, Wrapped> {
  operator fun MyConext.invoke(string: String): Wrapped = Messages.throwPluginNotExecuted()
  @InterpolatorBackend
  fun interpolate(parts: () -> List<String>, params: () -> List<Wrapped>, ctx: MyConext): Wrapped =
    Wrapped(parts(), params(), ctx)
}

class InterpolatorFunctionContextTest: InterpolateTestBase {
  val ctxOuter = MyConext("stuffOuter")
  val ctxInner = MyConext("stuffOuter")
  val ctxFoo = MyConext("stuffFoo")
  val ctxBar = MyConext("stuffBar")
  val ctxBaz = MyConext("stuffBaz")

  val A = Wrapped(listOf("A"), listOf(), ctxFoo)
  val B = Wrapped(listOf("B"), listOf(), ctxBar)
  val C = Wrapped(listOf("C"), listOf(), ctxBaz)

  @Test
  fun simpleConstantTestFunc() {
    ctxOuter.staticContextTerp("one") shouldBe
      Wrapped(listOf("one"), listOf(), ctxOuter)
  }

  @Test
  fun complexStaticTest1() {
    ctxOuter.staticContextTerp("foo_${A}_bar_${B}${C}_baz") shouldBe
      Wrapped(listOf("foo_", "_bar_", "", "_baz"), listOf(A, B, C), ctxOuter)
  }

  @Test
  fun nestedTest() {
    ctxOuter.staticContextTerp("foo_${ctxInner.staticContextTerp("middle")}_bar") shouldBe
      Wrapped(listOf("foo_", "_bar"), listOf(Wrapped(listOf("middle"), listOf(), ctxInner)), ctxOuter)
  }
}