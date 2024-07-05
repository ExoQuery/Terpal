package io.exoquery.terpal

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class InterpolatorFunction<T: Any>(val cls: KClass<T>)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class WrapFailureMessage(val msg: String)
