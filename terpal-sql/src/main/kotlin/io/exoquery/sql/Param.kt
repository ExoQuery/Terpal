package io.exoquery.sql

import kotlin.reflect.KClass


data class Param<T: Any>(val cls: KClass<T>, val value: T): SqlFragment {
  companion object {
    inline operator fun <reified T: Any> invoke(value: T): Param<T> = Param(T::class, value)
  }
}
