package io.exoquery.terpal

data class In(val value: String)
data class Out(val parts: List<String>, val params: List<In?>)

object StaticTerp: Interpolator<In?, Out> {
  override fun interpolate(parts: () -> List<String>, params: () -> List<In?>): Out =
    Out(parts(), params())
}

class InstanceTerp: Interpolator<In?, Out> {
  override fun interpolate(parts: () -> List<String>, params: () -> List<In?>): Out =
    Out(parts(), params())
}


fun main() {
//  printSource {
//    StaticTerp.interpolate({listOf("foo, bar")}, {listOf(In("baz"))})
//  }


  val inExample: In? = null

  //StaticTerp("foo_${In("One")}_bar_${In("Two")}${In("Three")}")
  // TODO need to have a case for a single string const
  //println(StaticTerp("foo ${In("123")}"))
  println(StaticTerp("foo ${inExample}"))
}