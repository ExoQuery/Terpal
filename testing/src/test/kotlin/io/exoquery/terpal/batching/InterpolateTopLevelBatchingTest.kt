package io.exoquery.terpal.batching

import io.exoquery.terpal.InterpolatorBatching
import io.exoquery.terpal.InterpolateTestBase
import io.exoquery.terpal.interpolatorBody
import kotlin.test.Test

data class In(val value: String)
data class Out<T>(val parts: List<String>, val params: (T) -> List<In>, val info: String) {
  val comps = OutComps(parts, info)
}

data class OutComps(val parts: List<String>, val info: String)

object StaticTerp: InterpolatorBatching<In> {
  override fun <T: Any> invoke(create: (T) -> String): Out<T> = interpolatorBody()
  override fun <T: Any> interpolate(parts: () -> List<String>, params: (T) -> List<In>): Out<T> =
    Out<T>(parts(), params, "Static")
}

class InstanceTerp(val info: String): InterpolatorBatching<In> {
  override fun <T: Any> invoke(create: (T) -> String): Out<T> = interpolatorBody()
  override fun <T: Any> interpolate(parts: () -> List<String>, params: (T) -> List<In>): Out<T> =
    Out(parts(), params, info)
}


val instanceTerp = InstanceTerp("Dynamic")

val A = In("A")
val B = In("B")
val C = In("C")

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
