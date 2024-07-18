package io.exoquery.terpal

import kotlin.test.Test

class InterpolateAlgoTest: InterpolateTestBase {
  val unzip = UnzipPartsParams<String>({ t -> t == "_"}, { a, b -> a + b }, { "-" })

  @Test
  fun simple0() {
    val (parts, params) = unzip(listOf("A", "B"))
    parts shouldBe listOf("-", "-", "-")
    params shouldBe listOf("A", "B")
    interlace(parts, params) shouldBe "-A-B-"
  }

  @Test
  fun simple1() {
    val (parts, params) = unzip(listOf("_", "A", "_", "B", "_"))
    parts shouldBe listOf("_", "_", "_")
    params shouldBe listOf("A", "B")
    interlace(parts, params) shouldBe "_A_B_"
  }

  @Test
  fun insertSpace1() {
    val (parts, params) = unzip(listOf("_", "A", "B", "_"))
    parts shouldBe listOf("_", "-", "_")
    params shouldBe listOf("A", "B")
    interlace(parts, params) shouldBe "_A-B_"
  }

  @Test
  fun insertSpace2() {
    val (parts, params) = unzip(listOf("_", "A", "B"))
    parts shouldBe listOf("_", "-", "-")
    params shouldBe listOf("A", "B")
    interlace(parts, params) shouldBe "_A-B-"
  }

  @Test
  fun insertSpace3() {
    val (parts, params) = unzip(listOf("A", "B", "_"))
    parts shouldBe listOf("-", "-", "_")
    params shouldBe listOf("A", "B")
    interlace(parts, params) shouldBe "-A-B_"
  }

  @Test
  fun complex1() {
    val (parts, params) = unzip(listOf("_", "_", "A", "B", "C", "_", "D"))
    parts shouldBe listOf("__", "-", "-", "_", "-")
    params shouldBe listOf("A", "B", "C", "D")
    interlace(parts, params) shouldBe "__A-B-C_D-"
  }

  @Test
  fun complex2() {
    val (parts, params) = unzip(listOf("A", "_", "B", "C", "D", "_", "_"))
    parts shouldBe listOf("-", "_", "-", "-", "__")
    params shouldBe listOf("A", "B", "C", "D")
    interlace(parts, params) shouldBe "-A_B-C-D__"
  }

  @Test
  fun complex3() {
    val (parts, params) = unzip(listOf("_", "_", "A", "B", "C", "_", "D", "_", "_"))
    parts shouldBe listOf("__", "-", "-", "_", "__")
    params shouldBe listOf("A", "B", "C", "D")
    interlace(parts, params) shouldBe "__A-B-C_D__"
  }
}
