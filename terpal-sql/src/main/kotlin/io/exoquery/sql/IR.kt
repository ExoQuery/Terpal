package io.exoquery.sql

sealed interface IR {
  data class Part(val value: String): IR {
    companion object {
      val Empty = Part("")
    }
  }

  sealed interface Var: IR
  data class Param(val value: io.exoquery.sql.Param<*>): Var
  data class Splice(val parts: List<Part>, val params: List<Var>): Var {
    fun flatten(): Splice {
      val partsAccum = mutableListOf<IR.Part>()
      val paramsAccum = mutableListOf<IR.Var>()

      fun MutableList<IR.Part>.interlaceEdges(other: List<IR.Part>) {
        if (this.isEmpty())
          this.addAll(other)
        else {
          if (other.isNotEmpty()) {
            val last = this.last()
            val firstOther = other.first()
            this.set(this.size-1, IR.Part(last.value + firstOther.value))
            this.addAll(other.drop(1))
          }
        }
      }

      val partsIter = parts.iterator()
      val paramsIter = params.iterator()

      var afterSplice: Boolean = false

      while(partsIter.hasNext()) {

        if (!afterSplice) {
          partsAccum += partsIter.next()
        } else {
          partsAccum.interlaceEdges(listOf(partsIter.next()))
        }

        if (paramsIter.hasNext()) {
          when (val nextParam = paramsIter.next()) {
            is Param -> {
              paramsAccum += nextParam
              afterSplice = false
            }
            // recursively flatten the inner splice, the grab out it's contents
            // for example, Splice("--$A-${Splice("_$B_$C_")}-$D--) at the point of reaching ${Splice(...)}
            // should be: (parts:["--", "-"], params:[A].
            // When the the splice is complete it should be: (parts:["--", ("-" + "_"), "_", "_"], params:[A, B, C])
            is Splice -> {
              partsAccum.interlaceEdges(nextParam.parts)
              paramsAccum.addAll(nextParam.params)
              afterSplice = true
            }
          }
        }
      }

      return IR.Splice(partsAccum, paramsAccum)
    }
  }
}