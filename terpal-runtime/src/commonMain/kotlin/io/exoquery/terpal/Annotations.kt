package io.exoquery.terpal

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class InterpolatorFunction<T: Any>(val cls: KClass<T>, val customReciever: Boolean = false)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class WrapFailureMessage(val msg: String)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class StrictType

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class InterpolatorBackend

@RequiresOptIn("Use the `inline` function in an interpolator can possibly introduce the possibility of SQL injection errors. USE WITH CAUTION.", level = RequiresOptIn.Level.ERROR)
annotation class PotentiallyDangerous
