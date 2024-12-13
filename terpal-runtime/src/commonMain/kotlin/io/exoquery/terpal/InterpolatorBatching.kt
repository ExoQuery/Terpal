package io.exoquery.terpal

import kotlin.reflect.KClass


interface InterpolatorBatchingWithWrapper<T>: InterpolatorBatching<T> {
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

// TODO in the macro need to make sure invoke and interpolate have same type signature
/**
 * NOTE: Normally in Interpoator we have a type T that defines the input and a type R
 * that defines the output. However since the batch interpolator desires to allow
 * the user to returnt he param function which takes a generic A (which is deinfed
 * on the invoke/interploate functions) any R paramter defined on the InterpolatorBatching-level
 * would need to somehow have this information. For example, if we wanted invoke/interploate to
 * return some kind of Out<A> then we would need to have a way to define this on the InterpolatorBatching
 * interface. This would likely require some other assumptions such as the R (i.e. Out) parameter
 * being a higher-order type Out<_> which is not even supported in Kotlin. So for now we will just
 * assume that invoke and interpolate have the same type in the macro and treat
 * whatever type comes out from invoke as the same type that comes out of interpolate.
 */
interface InterpolatorBatching<T> {
  // TODO better error message
  operator fun <A: Any> invoke(create: (A) -> String): Any = Messages.throwPluginNotExecuted()
  fun <A: Any> interpolate(parts: () -> List<String>, params: (A) -> List<T>): Any
}

/*
for example:
SqlBatch { p: Person -> "insert into person (name, age) values (${p.name}, ${p.age})" }
 */
