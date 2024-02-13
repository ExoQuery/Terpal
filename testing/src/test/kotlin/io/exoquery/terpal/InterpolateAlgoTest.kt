package io.exoquery.terpal

import io.exoquery.terpal.InterlacePartsParams

class InterpolateAlgoTest {

}



// TODO Move this to testing area
fun test() {
  val initial = listOf("_", "_", "A", "B", "C", "_", "D")
  println("Initial: $initial")

  val comps =
    InterlacePartsParams<String>({t -> t == "_"}, { a, b -> a + b }, { "_" }).invoke(initial)

  println(comps)
  val (parts, params) = comps

  val a = parts.iterator()
  val b = params.iterator()
  val out = StringBuffer()
  while (a.hasNext() || b.hasNext()) {
    if (a.hasNext()) {
      out.append(a.next())
      if (b.hasNext()) {
        out.append(b.next())
      }
    }
  }
  println()
  println(out.toString())
}


