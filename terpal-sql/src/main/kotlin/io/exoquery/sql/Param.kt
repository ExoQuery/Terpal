package io.exoquery.sql

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.serializer
import kotlin.reflect.KClass

sealed interface ParamType<out T> {
  data class Serialized<T>(val value: SerializationStrategy<T>): ParamType<T>
  data object Contextual: ParamType<Nothing>
}

// Note that T can't extend Any because then T will not be allowed to be null when it is being decoded
// that is why we have KClass<*> and not KClass<T>
data class Param<T>(val type: ParamType<T>, val cls: KClass<*>, val value: T): SqlFragment {
  companion object {
    /** Crete a contextual parameter. This relies database context having a specific encoder for the specified type. */
    inline fun <reified T: Any> contextual(value: T): Param<T> = Param(ParamType.Contextual, T::class, value)
    inline fun <reified T> withSerializer(value: T, serializer: SerializationStrategy<T>): Param<T> = Param(ParamType.Serialized(serializer), T::class, value)

    operator fun invoke(value: String): Param<String> = Param(ParamType.Serialized(String.serializer()), String::class, value)
    operator fun invoke(value: Int): Param<Int> = Param(ParamType.Serialized(Int.serializer()), Int::class, value)
    operator fun invoke(value: Long): Param<Long> = Param(ParamType.Serialized(Long.serializer()), Long::class, value)
    operator fun invoke(value: Short): Param<Short> = Param(ParamType.Serialized(Short.serializer()), Short::class, value)
    operator fun invoke(value: Byte): Param<Byte> = Param(ParamType.Serialized(Byte.serializer()), Byte::class, value)
    operator fun invoke(value: Float): Param<Float> = Param(ParamType.Serialized(Float.serializer()), Float::class, value)
    operator fun invoke(value: Double): Param<Double> = Param(ParamType.Serialized(Double.serializer()), Double::class, value)
    operator fun invoke(value: Boolean): Param<Boolean> = Param(ParamType.Serialized(Boolean.serializer()), Boolean::class, value)

  }
}
