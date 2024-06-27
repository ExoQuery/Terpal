package io.exoquery.sql

import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.ExperimentalSerializationApi as SerApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.serializer
import java.math.BigDecimal
import java.time.*
import kotlin.reflect.KClass

// Note that T can't extend Any because then T will not be allowed to be null when it is being decoded
// that is why we have KClass<*> and not KClass<T>
data class Param<T>(val serializer: SerializationStrategy<T>, val cls: KClass<*>, val value: T?): SqlFragment {
  companion object {
    /**
     * Crete a contextual parameter. This relies database context having a specific encoder for the specified type.
     * In order to do that, extend your desired database context add the corresponding encoder to the "additionalEncoders" parameter.
     **/
    @OptIn(SerApi::class)
    inline fun <reified T: Any> contextual(value: T?): Param<T> = Param(ContextualSerializer(T::class), T::class, value)
    /** Alias for Param.contextual */
    inline fun <reified T: Any> ctx(value: T?): Param<T> = contextual(value)

    inline fun <reified T> withSerializer(value: T?, serializer: SerializationStrategy<T>): Param<T> = Param(serializer, T::class, value)

    /** Alias for Param.withSerializer */
    inline fun <reified T> withSer(value: T?, serializer: SerializationStrategy<T>): Param<T> = withSerializer(value, serializer)

    operator fun invoke(value: String?): Param<String> = Param(String.serializer(), String::class, value)
    operator fun invoke(value: Int?): Param<Int> = Param(Int.serializer(), Int::class, value)
    operator fun invoke(value: Long?): Param<Long> = Param(Long.serializer(), Long::class, value)
    operator fun invoke(value: Short?): Param<Short> = Param(Short.serializer(), Short::class, value)
    operator fun invoke(value: Byte?): Param<Byte> = Param(Byte.serializer(), Byte::class, value)
    operator fun invoke(value: Float?): Param<Float> = Param(Float.serializer(), Float::class, value)
    operator fun invoke(value: Double?): Param<Double> = Param(Double.serializer(), Double::class, value)
    operator fun invoke(value: Boolean?): Param<Boolean> = Param(Boolean.serializer(), Boolean::class, value)
    operator fun invoke(value: ByteArray?): Param<ByteArray> = Param(serializer<ByteArray>(), ByteArray::class, value)

    @OptIn(SerApi::class) operator fun invoke(value: java.util.Date?): Param<java.util.Date> = Param(ContextualSerializer(java.util.Date::class), java.util.Date::class, value)
    @OptIn(SerApi::class) operator fun invoke(value: java.sql.Date?): Param<java.sql.Date> = Param(ContextualSerializer(java.sql.Date::class), java.sql.Date::class, value)
    @OptIn(SerApi::class) operator fun invoke(value: java.sql.Time?): Param<java.sql.Time> = Param(ContextualSerializer(java.sql.Time::class), java.sql.Time::class, value)
    @OptIn(SerApi::class) operator fun invoke(value: java.sql.Timestamp?): Param<java.sql.Timestamp> = Param(ContextualSerializer(java.sql.Timestamp::class), java.sql.Timestamp::class, value)
    @OptIn(SerApi::class) operator fun invoke(value: BigDecimal?): Param<BigDecimal> = Param(ContextualSerializer(BigDecimal::class), BigDecimal::class, value)
    @OptIn(SerApi::class) operator fun invoke(value: LocalDate?): Param<LocalDate> = Param(ContextualSerializer(LocalDate::class), LocalDate::class, value)
    @OptIn(SerApi::class) operator fun invoke(value: LocalTime?): Param<LocalTime> = Param(ContextualSerializer(LocalTime::class), LocalTime::class, value)
    @OptIn(SerApi::class) operator fun invoke(value: LocalDateTime?): Param<LocalDateTime> = Param(ContextualSerializer(LocalDateTime::class), LocalDateTime::class, value)
    @OptIn(SerApi::class) operator fun invoke(value: ZonedDateTime?): Param<ZonedDateTime> = Param(ContextualSerializer(ZonedDateTime::class), ZonedDateTime::class, value)
    @OptIn(SerApi::class) operator fun invoke(value: Instant?): Param<Instant> = Param(ContextualSerializer(Instant::class), Instant::class, value)
    @OptIn(SerApi::class) operator fun invoke(value: OffsetTime?): Param<OffsetTime> = Param(ContextualSerializer(OffsetTime::class), OffsetTime::class, value)
    @OptIn(SerApi::class) operator fun invoke(value: OffsetDateTime?): Param<OffsetDateTime> = Param(ContextualSerializer(OffsetDateTime::class), OffsetDateTime::class, value)
  }
}
