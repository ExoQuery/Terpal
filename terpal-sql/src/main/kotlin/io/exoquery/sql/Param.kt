package io.exoquery.sql

import kotlin.reflect.KClass

// Note that T can't extend Any because then T will not be allowed to be null when it is being decoded
// that is why we have KClass<*> and not KClass<T>
data class Param<T>(val cls: KClass<*>, val value: T): SqlFragment {
  companion object {
    inline operator fun <reified T: Any> invoke(value: T): Param<T> = Param(T::class, value)
  }
}
