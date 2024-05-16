package io.exoquery.sql

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import java.sql.ResultSet
import java.sql.ResultSetMetaData

// Maybe implement MappedResultDecoder<V, T>(mapper:V -> T) {

class OffsetCollector {
  private var curr: Int = 0
  // So basically when there's an element-in-element we need to offset every element that the inner
  // thing has above one so for example Person(id, Name(first, last), age) when we're on name it would be 2-1=1 so 1 would
  // be the additional offset we would need to have when returning from serialization of the Name object back to the Person object
  fun advance(value: Int) {
    curr += value-1
  }
  fun curr() = curr
  override fun toString(): String = "OffsetCollector(${curr})"
}

open class ResultDecoder private constructor (val rs: ResultSet, val offset: OffsetCollector): Decoder, CompositeDecoder {
  companion object {
    operator fun invoke(rs: ResultSet, descriptor: SerialDescriptor): ResultDecoder {
      fun metaColumnData(meta: ResultSetMetaData) =
        (1..meta.columnCount).map { meta.getColumnName(it) to meta.getColumnTypeName(it) }
      val metaColumns = metaColumnData(rs.metaData)

      fun flatDescriptorColumnData(desc: SerialDescriptor): List<Pair<String, String>> =
        when (desc.kind) {
          StructureKind.CLASS -> (desc.elementDescriptors.toList().zip(desc.elementNames.toList())) .flatMap { (fieldDesc, fieldName) ->
            when (fieldDesc.kind) {
              StructureKind.CLASS -> flatDescriptorColumnData(fieldDesc)
              else -> listOf(fieldName to fieldDesc.serialName)
            }
          }
          else -> listOf("<unamed>" to desc.serialName)
        }
      val descriptorColumns = flatDescriptorColumnData(descriptor)
      if (metaColumns.size != descriptorColumns.size) {
        throw IllegalArgumentException(
          """|Column mismatch. The columns from the SQL ResultSet metadata did not match the expected columns from the deserialized type: ${descriptor.serialName}
             |SQL Columns (${metaColumns.size}): ${metaColumns.withIndex().map { (i, kv) -> "($i)${kv.first}:${kv.second}" }}
             |Class Columns (${descriptorColumns.size}): ${descriptorColumns.withIndex().map { (i, kv) -> "($i)${kv.first}:${kv.second}" }}
          """.trimMargin())
      }
      return ResultDecoder(rs, OffsetCollector())
    }
  }

  var fieldIndex: Int = 0

  override val serializersModule: SerializersModule = EmptySerializersModule()

  val Int.nx get() = this+1 + offset.curr()

  override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean = rs.getBoolean(index.nx)
  override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte = rs.getByte(index.nx)
  override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char = rs.getString(index)[0.nx]
  override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double = rs.getDouble(index.nx)
  override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float = rs.getFloat(index.nx)
  override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int = rs.getInt(index.nx)
  override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long = rs.getLong(index.nx)
  override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short = rs.getShort(index.nx)
  override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String = rs.getString(index.nx)

  override fun decodeBoolean(): Boolean = rs.getBoolean(1)
  override fun decodeByte(): Byte = rs.getByte(1)
  override fun decodeChar(): Char = rs.getString(1)[0]
  override fun decodeDouble(): Double = rs.getDouble(1)
  override fun decodeFloat(): Float = rs.getFloat(1)
  override fun decodeShort(): Short = rs.getShort(1)
  override fun decodeString(): String = rs.getString(1)
  override fun decodeInt(): Int = rs.getInt(1)
  override fun decodeLong(): Long = rs.getLong(1)


  @ExperimentalSerializationApi
  override fun decodeNull(): Nothing? = null
  //override fun decodeSequentially(): Boolean = true

  @OptIn(ExperimentalSerializationApi::class)
  override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
    this
    //ResultDecoder(rs, descriptor.elementsCount, elementIndex)

  override fun endStructure(descriptor: SerialDescriptor) {
    offset.advance(descriptor.elementsCount)
  }

  @ExperimentalSerializationApi
  override fun <T : Any> decodeNullableSerializableElement(descriptor: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T?>, previousValue: T?): T? {
    rs.getObject(index.nx)
    return if (rs.wasNull()) null else deserializer.deserialize(this)
  }

  override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder = this
  @OptIn(ExperimentalSerializationApi::class)
  override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
    if (fieldIndex >= descriptor.elementsCount) return CompositeDecoder.DECODE_DONE
    //val childDesc = descriptor.elementDescriptors.toList()[fieldIndex]
//    return when (childDesc.kind) {
//      StructureKind.CLASS -> elementIndex
//      else -> elementIndex++
//    }
    val currFieldIndex = fieldIndex
    fieldIndex += 1
    return currFieldIndex
  }

  @OptIn(ExperimentalSerializationApi::class)
  override fun <T> decodeSerializableElement(descriptor: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>, previousValue: T?): T {
    val childDesc = descriptor.elementDescriptors.toList()[index]
    return when (childDesc.kind) {
      StructureKind.LIST -> {
        // val elementDescriptor = descriptor.elementDescriptors.first()
        // Need to use this to summon the right mapper
        // elementDescriptor.capturedKClass

        // Can't use element deserializer because it acts on a ResultSet. Need to implement mappers
        rs.getArray(index.nx) as T
      }
      StructureKind.CLASS -> {
        deserializer.deserialize(ResultDecoder(rs, offset))
      }
      else ->
        throw IllegalArgumentException("Unsupported serial kind: ${descriptor.kind}")
    }
  }





  override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
    TODO("Not yet implemented")
  }

  override fun decodeInline(descriptor: SerialDescriptor): Decoder = this

  @ExperimentalSerializationApi
  override fun decodeNotNullMark(): Boolean {
    rs.getObject(fieldIndex.nx)
    return !rs.wasNull()
  }


}