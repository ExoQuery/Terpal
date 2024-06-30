package io.exoquery.sql

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

class PreparedStatementElementEncoder<Session, Stmt>(
  val ctx: EncodingContext<Session, Stmt>,
  val index: Int,
  val api: ApiEncoders<Session, Stmt>,
  val encoders: Set<SqlEncoder<Session, Stmt, out Any>>
): Encoder {

  override val serializersModule: SerializersModule = EmptySerializersModule()

  override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder =
    if (descriptor.kind == StructureKind.CLASS)
      throw IllegalArgumentException(
        """|Cannot encode the structural type `${descriptor.serialName}`, Encoding of structural types is not allowed to be encoded in terpal-sql.
           |Only atomic-values are allowed to be encoded. If `${descriptor.serialName}` has only one field then it should be can be encoded
           |as a primitive representing the underlying field. For example:
           |  
           |  @Serializable(with = MyTypeSerializer::class) 
           |  data class MyType(val value: Int)
           |  
           |  object MyTypeSerializer: KSerializer<MyType> {
           |    override val descriptor = PrimitiveSerialDescriptor("MyType", PrimitiveKind.INT)
           |    override fun serialize(encoder: Encoder, value: MyType) = encoder.encodeInt(value.value)
           |    override fun deserialize(decoder: Decoder): MyType = MyType(decoder.decodeInt())
           |  }
           | 
           |""".trimMargin())
    else
      throw IllegalArgumentException("Illegal descriptor kind: ${descriptor.kind} was attempted for structural decoding. This should be impossible.")

  override fun encodeBoolean(value: Boolean) = api.BooleanEncoder.encode(ctx, value, index)
  override fun encodeByte(value: Byte) = api.ByteEncoder.encode(ctx, value, index)
  override fun encodeChar(value: Char) = api.CharEncoder.encode(ctx, value, index)
  override fun encodeDouble(value: Double) = api.DoubleEncoder.encode(ctx, value, index)
  override fun encodeFloat(value: Float) = api.FloatEncoder.encode(ctx, value, index)
  override fun encodeInt(value: Int) = api.IntEncoder.encode(ctx, value, index)
  override fun encodeLong(value: Long) = api.LongEncoder.encode(ctx, value, index)
  override fun encodeShort(value: Short) = api.ShortEncoder.encode(ctx, value, index)
  override fun encodeString(value: String) = api.StringEncoder.encode(ctx, value, index)

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


    // Note, for decoders I do not think it is possible to know on the level of types whether something is nullable. Need to investigate further.
    //fun SqlEncoder<Session, Stmt, out Any>.asNullableIfSpecified() = if (desc.isNullable) asNullable() else this

    return when (desc.kind) {
      StructureKind.LIST -> {
        val encoder =
          when {
            desc.capturedKClass != null -> {
              encoders.find { it.type == desc.capturedKClass } ?: throw IllegalArgumentException("Could not find a decoder for the list type ${desc.capturedKClass}")
            }
            desc.elementDescriptors.toList().size == 1 && desc.elementDescriptors.first().kind is PrimitiveKind.BYTE ->
              encoders.find { it.type == ByteArray::class }
            else ->
              null
          }

        encoder?.let { (it.asNullable() as SqlEncoder<Session, Stmt, T?> ).encode(ctx, value, index) }
          ?: throw IllegalArgumentException("Could not find a encoder for the structural list type ${desc.capturedKClass} with the descriptor: ${desc}")
      }
      SerialKind.CONTEXTUAL -> {
        val encoder = encoders.find { it.type == desc.capturedKClass }?.asNullable()
        if (encoder == null) throw IllegalArgumentException("Could not find a encoder for the contextual type ${desc.capturedKClass}")
        @Suppress("UNCHECKED_CAST")
        run { (encoder as SqlEncoder<Session, Stmt, T?>).encode(ctx, value, index) }
      }
      else -> {
        if (value == null) {
          when (desc.kind) {
            is PrimitiveKind.BYTE -> api.ByteEncoder.asNullable().encode(ctx, null, index)
            is PrimitiveKind.SHORT -> api.ShortEncoder.asNullable().encode(ctx, null, index)
            is PrimitiveKind.INT -> api.IntEncoder.asNullable().encode(ctx, null, index)
            is PrimitiveKind.LONG -> api.LongEncoder.asNullable().encode(ctx, null, index)
            is PrimitiveKind.FLOAT -> api.FloatEncoder.asNullable().encode(ctx, null, index)
            is PrimitiveKind.DOUBLE -> api.DoubleEncoder.asNullable().encode(ctx, null, index)
            is PrimitiveKind.BOOLEAN -> api.BooleanEncoder.asNullable().encode(ctx, null, index)
            is PrimitiveKind.CHAR -> api.CharEncoder.asNullable().encode(ctx, null, index)
            is PrimitiveKind.STRING -> api.StringEncoder.asNullable().encode(ctx, null, index)
            else -> throw IllegalArgumentException("Unsupported null primitive kind: ${desc.kind}")
          }
        } else if (desc.kind is PrimitiveKind) {
          // if it is a primitive type then use the encoder defined in the serialization. Note that
          // if it is a wrappedType (e.g. NewTypeInt(value: Int) then this serializer will be the wrapped one
          serializer.serialize(this, value)
        }
        else {
          throw IllegalArgumentException("Unsupported serial-kind: ${desc.kind} could notbe decoded as a Array, Contextual, or Primitive value")
        }
      }
    }
  }
}