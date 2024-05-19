package io.exoquery.sql

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import java.sql.PreparedStatement

class PreparedStatementElementEncoder(val ps: PreparedStatement, val index: Int): Encoder {

  override val serializersModule: SerializersModule = EmptySerializersModule()

  override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder =
    throw IllegalArgumentException("Structure encoding is not supported (TODO Message about how to use derived (i.e. contramapped) encoders)")

  override fun encodeBoolean(value: Boolean) = ps.setBoolean(index, value)
  override fun encodeByte(value: Byte) = ps.setByte(index, value)
  override fun encodeChar(value: Char) = ps.setString(index, value + "")
  override fun encodeDouble(value: Double) = ps.setDouble(index, value)
  override fun encodeFloat(value: Float) = ps.setFloat(index, value)
  override fun encodeInt(value: Int) = ps.setInt(index, value)
  override fun encodeLong(value: Long) = ps.setLong(index, value)
  override fun encodeShort(value: Short) = ps.setShort(index, value)
  override fun encodeString(value: String) = ps.setString(index, value)

  override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) =
    TODO("Enum encoding not yet supported")

  @ExperimentalSerializationApi
  override fun encodeNull() =
    throw IllegalArgumentException("Need to know the type of the column to encode a null value")

  override fun encodeInline(descriptor: SerialDescriptor): Encoder = this
}