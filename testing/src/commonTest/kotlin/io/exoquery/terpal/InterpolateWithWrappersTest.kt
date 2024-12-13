package io.exoquery.terpal

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

data class SomeOtherType(val value: String)
// Note, the name `In` is used in another test so using it here will cuase this wrapper not to be found
fun InterpolateWithWrappersTest.Companion.StaticTerp.wrap(string: SomeOtherType?): InterpolateWithWrappersTest.Companion.In =
  InterpolateWithWrappersTest.Companion.In("(SomeOtherType)" + string?.value)

class InterpolateWithWrappersTest: InterpolateTestBase {
  companion object {
    data class In(val value: String)
    data class Out(val parts: List<String>, val params: List<In>, val info: String)

    object StaticTerp: InterpolatorWithWrapper<In, Out> {
      operator fun invoke(string: String): Out = Messages.throwPluginNotExecuted()
      @InterpolatorBackend
      fun interpolate(parts: () -> List<String>, params: () -> List<In>): Out =
        Out(parts(), params(), "Static")

      override fun inlined(value: String?): In = In("(String-inline)" + value.toString())
      override fun wrap(value: String?): In = In("(String)" + value.toString())
      override fun wrap(value: Int?): In = In("(Int)" + value.toString())
      override fun wrap(value: Long?): In = In("(Long)" + value.toString())
      override fun wrap(value: Short?): In = In("(Short)" + value.toString())
      override fun wrap(value: Byte?): In = In("(Byte)" + value.toString())
      override fun wrap(value: Float?): In = In("(Float)" + value.toString())
      override fun wrap(value: Double?): In = In("(Double)" + value.toString())
      override fun wrap(value: Boolean?): In = In("(Boolean)" + value.toString())
    }

    class InstanceTerp(val info: String): ProtoInterpolator<In, Out> {
      operator fun invoke(string: String): Out = Messages.throwPluginNotExecuted()
      fun interpolate(parts: () -> List<String>, params: () -> List<In>): Out =
        Out(parts(), params(), info)
    }

    // Not doing extension functions for wrappers inside the companion object. This is defined on a package level.
    //fun StaticTerp.wrap(string: SomeOtherType?): In = In("(SomeOtherType)" + string?.value)
  }

  val instanceTerp = InstanceTerp("Dynamic")

  val A = In("A")
  val B = In("B")
  val C = In("C")
  val E: In by lazy { throw IllegalArgumentException("blah") }

  @Test
  fun exceptionTest() {
    val ex = assertFailsWith<InterpolationException> {
      StaticTerp("foo_${A}${E}${C}_baz")
    }

    println("====== `InterpolateWithWrappersTest.exceptionTest` message: ${ex.msg}")
    assertContains(ex.msg, "Error in spliced")
  }

  @Test
  fun exceptionTest2() {
    val ex = assertFailsWith<InterpolationException> {
      StaticTerp("foo_${A}${
        "foo" +
          throw IllegalArgumentException("blah blah")
      }${C}_baz")
    }

    println("====== `InterpolateWithWrappers.exceptionTest2` message: ${ex.msg}")
    assertContains(ex.msg, "Error in spliced")
  }

  fun <T> id(value: T): T = value

  @PotentiallyDangerous
  @Test
  fun simpleStaticTest_WithInline() {
    val middle = "str"
    StaticTerp("foo_${A}${inline(middle)}${C}") shouldBe
      Out(listOf("foo_", "", "", ""), listOf(A, In("(String-inline)str"), C), "Static")
  }

  @Test
  fun simpleStaticTest2() {
    // need to have an id function because splicing same thing as a wrap will make kotlin try to "optimize" it by treating the string part as a constant
    StaticTerp("foo_${id("str")}_bar") shouldBe
      Out(listOf("foo_", "_bar"), listOf(In("(String)str")), "Static")
  }

  @Test
  fun simpleStaticTest3() {
    StaticTerp("foo_${A}${id("str")}${C}") shouldBe
      Out(listOf("foo_", "", "", ""), listOf(A, In("(String)str"), C), "Static")
  }

  @Test
  fun staticTestExtensionRecieverWrapper() {
    val someOther = SomeOtherType("someValue")
    StaticTerp("foo_${A}${someOther}${C}") shouldBe
      Out(listOf("foo_", "", "", ""), listOf(A, In("(SomeOtherType)someValue"), C), "Static")
  }
}
