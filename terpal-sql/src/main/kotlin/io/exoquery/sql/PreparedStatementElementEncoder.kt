package io.exoquery.sql

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.capturedKClass
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

class PreparedStatementElementEncoder<Session, Stmt>(val ctx: EncodingContext<Session, Stmt>, val index: Int, val encoders: SqlEncoders<Session, Stmt>): Encoder {

  override val serializersModule: SerializersModule = EmptySerializersModule()

  override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder =
    throw IllegalArgumentException("Structure encoding is not supported (TODO Message about how to use derived (i.e. contramapped) encoders)")

  override fun encodeBoolean(value: Boolean) = encoders.BooleanEncoder.encode(ctx, value, index)
  override fun encodeByte(value: Byte) = encoders.ByteEncoder.encode(ctx, value, index)
  override fun encodeChar(value: Char) = encoders.CharEncoder.encode(ctx, value, index)
  override fun encodeDouble(value: Double) = encoders.DoubleEncoder.encode(ctx, value, index)
  override fun encodeFloat(value: Float) = encoders.FloatEncoder.encode(ctx, value, index)
  override fun encodeInt(value: Int) = encoders.IntEncoder.encode(ctx, value, index)
  override fun encodeLong(value: Long) = encoders.LongEncoder.encode(ctx, value, index)
  override fun encodeShort(value: Short) = encoders.ShortEncoder.encode(ctx, value, index)
  override fun encodeString(value: String) = encoders.StringEncoder.encode(ctx, value, index)

  override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) =
    TODO("Enum encoding not yet supported")

  @ExperimentalSerializationApi
  override fun encodeNull() =
    throw IllegalArgumentException("Need to know the type of the column to encode a null value")

  override fun encodeInline(descriptor: SerialDescriptor): Encoder = this

  override public fun <T : Any?> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
    encodeNullableSerializableValue(serializer, value)
  }

  @OptIn(ExperimentalSerializationApi::class)
  override fun <T : Any> encodeNullableSerializableValue(serializer: SerializationStrategy<T>, value: T?) {
    val desc = serializer.descriptor

    // If the actual decoded element is supposed to be nullable then make the decoder for it nullable
    fun SqlEncoder<Session, Stmt, out Any>.asNullableIfSpecified() = if (desc.isNullable) asNullable() else this

    return when (desc.kind) {
      SerialKind.CONTEXTUAL -> {
        val encoder = encoders.encoders.find { it.type == desc.capturedKClass }?.asNullableIfSpecified()
        if (encoder == null) throw IllegalArgumentException("Could not find a decoder for the contextual type ${desc.capturedKClass}")
        @Suppress("UNCHECKED_CAST")
        run { (encoder as SqlEncoder<Session, Stmt, T?>).encode(ctx, value, index) }
      }
      else -> {
        if (value == null) {
          when (desc.kind) {
            is PrimitiveKind.BYTE -> encoders.ByteEncoder.asNullable().encode(ctx, null, index)
            is PrimitiveKind.SHORT -> encoders.ShortEncoder.asNullable().encode(ctx, null, index)
            is PrimitiveKind.INT -> encoders.IntEncoder.asNullable().encode(ctx, null, index)
            is PrimitiveKind.LONG -> encoders.LongEncoder.asNullable().encode(ctx, null, index)
            is PrimitiveKind.FLOAT -> encoders.FloatEncoder.asNullable().encode(ctx, null, index)
            is PrimitiveKind.DOUBLE -> encoders.DoubleEncoder.asNullable().encode(ctx, null, index)
            is PrimitiveKind.BOOLEAN -> encoders.BooleanEncoder.asNullable().encode(ctx, null, index)
            is PrimitiveKind.CHAR -> encoders.CharEncoder.asNullable().encode(ctx, null, index)
            is PrimitiveKind.STRING -> encoders.StringEncoder.asNullable().encode(ctx, null, index)
            else -> throw IllegalArgumentException("Unsupported null primitive kind: ${desc.kind}")
          }
        } else {
          // if it is a primitive type then use the encoder defined in the serialization. Note that
          // if it is a wrappedType (e.g. NewTypeInt(value: Int) then this serializer will be the wrapped one
          serializer.serialize(this, value)
        }
      }
    }
  }
}