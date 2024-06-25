package io.exoquery.sql

import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialKind
import java.time.LocalDate
import kotlin.reflect.KClass

// Note that T can't extend Any because then T will not be allowed to be null when it is being decoded
// that is why we have KClass<*> and not KClass<T>
data class Param<T>(val serializer: SerializationStrategy<T>, val cls: KClass<*>, val value: T): SqlFragment {
  companion object {
    /** Crete a contextual parameter. This relies database context having a specific encoder for the specified type. */
    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T: Any> contextual(value: T): Param<T> = Param(ContextualSerializer(T::class), T::class, value)
    inline fun <reified T> withSerializer(value: T, serializer: SerializationStrategy<T>): Param<T> = Param(serializer, T::class, value)

    operator fun invoke(value: String): Param<String> = Param(String.serializer(), String::class, value)
    operator fun invoke(value: Int): Param<Int> = Param(Int.serializer(), Int::class, value)
    operator fun invoke(value: Long): Param<Long> = Param(Long.serializer(), Long::class, value)
    operator fun invoke(value: Short): Param<Short> = Param(Short.serializer(), Short::class, value)
    operator fun invoke(value: Byte): Param<Byte> = Param(Byte.serializer(), Byte::class, value)
    operator fun invoke(value: Float): Param<Float> = Param(Float.serializer(), Float::class, value)
    operator fun invoke(value: Double): Param<Double> = Param(Double.serializer(), Double::class, value)
    operator fun invoke(value: Boolean): Param<Boolean> = Param(Boolean.serializer(), Boolean::class, value)

    @OptIn(ExperimentalSerializationApi::class)
    operator fun invoke(value: LocalDate): Param<LocalDate> = Param(ContextualSerializer(LocalDate::class), LocalDate::class, value)

  }
}
