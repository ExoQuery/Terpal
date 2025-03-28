package io.exoquery.terpal

interface InterpolatorWithWrapper<T, R>: ProtoInterpolator<T, R> {
  fun inlined(value: String?): T
  fun wrap(value: String?): T
  fun wrap(value: Int?): T
  fun wrap(value: Long?): T
  fun wrap(value: Short?): T
  fun wrap(value: Byte?): T
  fun wrap(value: Float?): T
  fun wrap(value: Double?): T
  fun wrap(value: Boolean?): T
}

// TODO also detect static constants in SQL(...) and warn about them

@PotentiallyDangerous
fun inline(value: String): String =
  throw IllegalStateException("This function can only be used inside of a Terpal interpolator invocation.")

interface Interpolator<T, R>: ProtoInterpolator<T, R> {
  operator fun invoke(string: String): R = Messages.throwPluginNotExecuted()
  @InterpolatorBackend
  fun interpolate(parts: () -> List<String>, params: () -> List<T>): R
}

interface ProtoInterpolator<T, R> {
  companion object {
    fun <T> interlace(parts: List<String>, params: List<T>, empty: () -> T, lift: (String) -> T, combine: (T, T) -> T): T {
      val partsIter = parts.iterator()
      val paramsIter = params.iterator()
      var curr = empty()
      while (partsIter.hasNext() || paramsIter.hasNext()) {
        if (partsIter.hasNext()) {
          curr = combine(curr, lift(partsIter.next()))
          if (paramsIter.hasNext()) {
            curr = combine(curr, paramsIter.next())
          }
        }
      }
      return curr
    }
  }
}
