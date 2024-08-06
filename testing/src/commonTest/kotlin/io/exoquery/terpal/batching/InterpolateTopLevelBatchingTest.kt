package io.exoquery.terpal.batching

import io.exoquery.terpal.InterpolatorBatching
import io.exoquery.terpal.InterpolateTestBase
import io.exoquery.terpal.Messages
import io.exoquery.terpal.InterpolationException
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

data class In(val value: String)
data class Out<T>(val parts: List<String>, val params: (T) -> List<In>, val info: String) {
  val comps = OutComps(parts, info)
}

data class OutComps(val parts: List<String>, val info: String)

object StaticTerp: InterpolatorBatching<In> {
  override fun <T: Any> invoke(create: (T) -> String): Out<T> = Messages.throwPluginNotExecuted()
  override fun <T: Any> interpolate(parts: () -> List<String>, params: (T) -> List<In>): Out<T> =
    Out<T>(parts(), params, "Static")
}

class InstanceTerp(val info: String): InterpolatorBatching<In> {
  override fun <T: Any> invoke(create: (T) -> String): Out<T> = Messages.throwPluginNotExecuted()
  override fun <T: Any> interpolate(parts: () -> List<String>, params: (T) -> List<In>): Out<T> =
    Out(parts(), params, info)
}


val instanceTerp = InstanceTerp("Dynamic")

val A = In("A")
val B = In("B")
val C = In("C")
val E: In by lazy { throw IllegalArgumentException("blah") }

class InterpolateTopLevelTest: InterpolateTestBase {

  data class Person(val first: String, val last: String, val age: Int)

  @Test
  fun simpleConstantTest() {
    StaticTerp { p: Person -> "foo" }.comps shouldBe
      OutComps(listOf("foo"), "Static")
  }

  @Test
  fun onePartTestStatic() {
    val terp = StaticTerp { p: Person -> "foo_${In(p.first)}_bar" }
    terp.comps shouldBe
      OutComps(listOf("foo_", "_bar"), "Static")
    terp.params(Person("A", "B", 1)) shouldBe
      listOf(In("A"))
  }

  @Test
  fun exceptionTest() {
    val ex = assertFailsWith<InterpolationException> {
      StaticTerp { p: Person -> "foo_${A}${E}${C}_baz" }.params(Person("AA", "BB", 11))
    }
    assertContains(ex.message, "<get-E>")
    assertContains(ex.message, "Error in spliced term #2 (of 3)")
  }

  @Test
  fun exceptionTest2() {
    class Item {
      val value: In by @Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION") lazy { throw IllegalArgumentException("blah") }
    }
    val ex = assertFailsWith<InterpolationException> {
      StaticTerp { item: Item -> "foo_${item.value}_baz" }.params(Item())
    }
    assertContains(ex.message, "item.<get-value>()")
    assertContains(ex.message, "Error in spliced term #1 (of 1)")
  }

  @Test
  fun multiPartStatic() {
    val terp = StaticTerp { p: Person -> "foo_${In(p.first)}${In(p.last)}${In("${p.age}")}_bar" }
    terp.comps shouldBe
      OutComps(listOf("foo_", "", "", "_bar"), "Static")
    terp.params(Person("A", "B", 1)) shouldBe
      listOf(In("A"), In("B"), In("1"))
  }

  @Test
  fun onePartTestInstance() {
    val terp = instanceTerp { p: Person -> "foo_${In(p.first)}_bar" }
    terp.comps shouldBe
      OutComps(listOf("foo_", "_bar"), "Dynamic")
    terp.params(Person("A", "B", 1)) shouldBe
      listOf(In("A"))
  }

  @Test
  fun multiPartInstance() {
    val terp = instanceTerp { p: Person -> "foo_${In(p.first)}${In(p.last)}${In("${p.age}")}_bar" }
    terp.comps shouldBe
      OutComps(listOf("foo_", "", "", "_bar"), "Dynamic")
    terp.params(Person("A", "B", 1)) shouldBe
      listOf(In("A"), In("B"), In("1"))
  }


}
