package io.exoquery.terpal

import kotlin.reflect.KClass

fun interpolatorBody(): Nothing = throw IllegalStateException(Messages.PluginWasNotExecuted)

interface InterpolatorWithWrapper<T, R>: Interpolator<T, R> {
  fun <V: Any> wrap(value: V, cls: KClass<V>): T
}

interface Interpolator<T, R> {
  // TODO better error message
  operator fun invoke(string: String): R = interpolatorBody()


  fun interpolate(parts: () -> List<String>, params: () -> List<T>): R

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
