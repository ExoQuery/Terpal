package io.exoquery.terpal

import kotlin.test.assertEquals

interface InterpolateTestBase {
  infix fun <T> T.shouldBe(other: T) = assertEquals(other, this)
}

// Need to have either a string or empty-space both before all the characters and after
// That means that if there's nothing after the last param we need to add an empty space
// character (which in this case we make into a "-") in order to easily understand what's happening.

fun interlace(parts: List<String>, params: List<String>): String {
  val a = parts.iterator()
  val b = params.iterator()
  val out = StringBuilder()
  while (a.hasNext() || b.hasNext()) {
    if (a.hasNext()) {
      out.append(a.next())
      if (b.hasNext()) {
        out.append(b.next())
      }
    }
  }
  return out.toString()
}