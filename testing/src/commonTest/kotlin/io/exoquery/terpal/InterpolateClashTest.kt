package io.exoquery.terpal

import kotlin.test.Test


data class InClash(val value: String)
data class OutClash(val parts: List<String>, val params: List<InClash>, val info: String)

object StaticTerpClash: ProtoInterpolator<InClash, OutClash> {
  operator fun invoke(string: String): OutClash = Messages.throwPluginNotExecuted()
  @InterpolatorBackend
  fun interpolate(parts: () -> List<String>, params: () -> List<InClash>): OutClash =
    OutClash(parts(), params(), "Static")
}

class InterpolateClashTest: InterpolateTestBase {

  @Test
  fun inClassTest() {
    func() shouldBe StaticTerpClash("A${InClash("x")}A")
  }
}


/*
e: file:///home/alexi/git/Terpal/testing/src/commonTest/kotlin/io/exoquery/terpal/InterpolateClashTest.kt:1:1 Platform declaration clash: The following declarations have the same JVM signature (func$0()Lio/exoquery/terpal/InClash;):
    fun `func$0`(): InClash defined in io.exoquery.terpal
    fun `func$0`(): InClash defined in io.exoquery.terpal
 */
fun func(): OutClash {
  val x = 123
  return when (x) {
    123 -> StaticTerpClash("A${InClash("x")}A")
    else -> StaticTerpClash("B${InClash("x")}}B")
  }
}
