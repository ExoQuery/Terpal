package io.exoquery.terpal

import kotlin.test.Test

class InterpolateNested: InterpolateTestBase {
  data class Stmt(val parts: List<String>, val params: List<Stmt>)

  companion object {
    object StaticTerp: ProtoInterpolator<Stmt, Stmt> {
      operator fun invoke(string: String): Stmt = Messages.throwPluginNotExecuted()
      fun interpolate(parts: () -> List<String>, params: () -> List<Stmt>): Stmt =
        Stmt(parts(), params())
    }

    class InstanceTerp(val info: String): ProtoInterpolator<Stmt, Stmt> {
      fun interpolate(parts: () -> List<String>, params: () -> List<Stmt>): Stmt =
        Stmt(parts(), params())
    }
  }

  @Test
  fun nestedStmt() {
    StaticTerp("foo_${StaticTerp("middle")}_bar") shouldBe
      Stmt(listOf("foo_", "_bar"), listOf(Stmt(listOf("middle"), listOf())))
  }
}
