package io.exoquery.terpal

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class InterpolatorFunction<T: Any>(val cls: KClass<T>)
