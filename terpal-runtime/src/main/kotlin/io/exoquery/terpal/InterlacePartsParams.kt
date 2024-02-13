package io.exoquery.terpal

class InterlacePartsParams<T>(val isPart: (T) -> Boolean, val concatPart: (T, T) -> T, val emptyPart: () -> T) {
  data class Components<T>(val parts: List<T>, val params: List<T>)

  fun invoke(values: List<T>): Components<T> {
    val identifyIsPart = isPart
    fun T.isPart(): Boolean = identifyIsPart(this@isPart)
    fun T.isParam(): Boolean = !this.isPart()

    val partsAccum = mutableListOf<T>()
    val paramsAccum = mutableListOf<T>()
    fun addPart(value: T) = partsAccum.add(value)
    fun addParam(value: T) = paramsAccum.add(value)
    fun appendToLastPart(value: T): Unit {
      if (partsAccum.isNotEmpty())
        partsAccum.set(partsAccum.size-1, concatPart(partsAccum.last(), value))
      else
        partsAccum.add(value)
    }

    val iter = values.iterator()
    var mustHavePart = true

    while (iter.hasNext()) {
      val next = iter.next()
      // If the next thing to insert is a part
      if (mustHavePart) {
        // if we need to have a string-part here see if the next thing is a string
        // if it is, then insert it, otherwise we need to insert an empty-string
        if (next.isPart()) {
          addPart(next)
          mustHavePart = false
        } else {
          addPart(emptyPart())
          addParam(next)
          // The next thing that we expect should STILL be a part
          // since we inserted a param
        }
      }
      // Otherwise the next thing to insert is a param
      else {
        if (next.isParam()) {
          addParam(next)
          // If we actually got a param the next thing to insert is a part
          mustHavePart = true
        }
        else
        // Otherwise just append it ot the last part. The thing expected next is STILL a param
          appendToLastPart(next)

        // if we need a param but don't have one that usually means we're at the last component
        // and we just need one last part
        // at this point keep flipping the switch and inserting parts so long as they exist
      }
    }

    // now we have inserted everything in the list, if we still need to have a part insert an extra one here
    if (mustHavePart) {
      addPart(emptyPart())
    }

    return Components(partsAccum, paramsAccum)
  }
}