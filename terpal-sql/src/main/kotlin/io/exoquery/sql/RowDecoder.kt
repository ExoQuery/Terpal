package io.exoquery.sql

import io.exoquery.sql.jdbc.JdbcDecodersWithTime
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import java.sql.Connection
import java.sql.ResultSet
import java.sql.ResultSetMetaData

data class ColumnInfo(val name: String, val type: String)

@OptIn(ExperimentalSerializationApi::class)
fun SerialDescriptor.verifyColumns(columns: List<ColumnInfo>): Unit {

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

  val descriptorColumns = flatDescriptorColumnData(this)
  if (columns.size != descriptorColumns.size) {
    throw IllegalArgumentException(
          """|Column mismatch. The columns from the SQL ResultSet metadata did not match the expected columns from the deserialized type: ${serialName}
             |SQL Columns (${columns.size}): ${columns.withIndex().map { (i, kv) -> "($i)${kv.name}:${kv.type}" }}
             |Class Columns (${descriptorColumns.size}): ${descriptorColumns.withIndex().map { (i, kv) -> "($i)${kv.first}:${kv.second}" }}
          """.trimMargin())
  }
}

class JdbcRowDecoder(
  sess: Connection,
  rs: ResultSet,
  initialRowIndex: Int,
  decoders: SqlDecoders<Connection, ResultSet>,
  columnInfos: List<ColumnInfo>,
  endCallback: (Int) -> Unit
): RowDecoder<Connection, ResultSet>(sess, rs, initialRowIndex, decoders, columnInfos, endCallback) {

  companion object {
    operator fun invoke(sess: Connection, rs: ResultSet, decoders: SqlDecoders<Connection, ResultSet>, descriptor: SerialDescriptor): JdbcRowDecoder {
      fun metaColumnData(meta: ResultSetMetaData) =
        (1..meta.columnCount).map { ColumnInfo(meta.getColumnName(it), meta.getColumnTypeName(it)) }
      val metaColumns = metaColumnData(rs.metaData)
      descriptor.verifyColumns(metaColumns)
      return JdbcRowDecoder(sess, rs, 1, decoders, metaColumns, {})
    }
  }

  override fun cloneSelf(rs: ResultSet, initialRowIndex: Int, endCallback: (Int) -> Unit): RowDecoder<Connection, ResultSet> =
    JdbcRowDecoder(sess, rs, initialRowIndex, decoders, columnInfos, endCallback)
}

@OptIn(ExperimentalSerializationApi::class)
abstract class RowDecoder<Session, Row>(val sess: Session, val rs: Row, val initialRowIndex: Int, val decoders: SqlDecoders<Session, Row>, val columnInfos: List<ColumnInfo>, val endCallback: (Int) -> Unit): Decoder, CompositeDecoder {

  abstract fun cloneSelf(ctx: DecodingContext<Session, Row>, initialRowIndex: Int, endCallback: (Int) -> Unit): RowDecoder<Session, Row>

  var rowIndex: Int = initialRowIndex
  var classIndex: Int = 0

  fun nextRowIndex(desc: SerialDescriptor, descIndex: Int, note: String = ""): Int {
    val curr = rowIndex
    println("---- Get Row ${columnInfos[rowIndex-1].name}, Index: ${curr} - (${descIndex}) ${desc.getElementDescriptor(descIndex)} - (Preview:${decoders.preview(rowIndex, rs)})" + (if (note != "") " - ${note}" else ""))
    rowIndex += 1
    return curr
  }

  override val serializersModule: SerializersModule = EmptySerializersModule()

  override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean = decoders.BooleanDecoder.decode(ctx, nextRowIndex(descriptor, index)) ?: error("Boolean Element ${index} in ${descriptor} cannot be null")
  override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte = decoders.ByteDecoder.decode(ctx, nextRowIndex(descriptor, index)) ?: error("Byte Element ${index} in ${descriptor} cannot be null")
  override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char = decoders.CharDecoder.decode(ctx, nextRowIndex(descriptor, index)) ?: error("Char Element ${index} in ${descriptor} cannot be null")
  override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double = decoders.DoubleDecoder.decode(ctx, nextRowIndex(descriptor, index)) ?: error("Double Element ${index} in ${descriptor} cannot be null")
  override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float = decoders.FloatDecoder.decode(ctx, nextRowIndex(descriptor, index)) ?: error("Float Element ${index} in ${descriptor} cannot be null")
  override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int = decoders.IntDecoder.decode(ctx, nextRowIndex(descriptor, index)) ?: error("Int Element ${index} in ${descriptor} cannot be null")
  override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long = decoders.LongDecoder.decode(ctx, nextRowIndex(descriptor, index)) ?: error("Long Element ${index} in ${descriptor} cannot be null")
  override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short = decoders.ShortDecoder.decode(ctx, nextRowIndex(descriptor, index)) ?: error("Short Element ${index} in ${descriptor} cannot be null")
  override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String = decoders.StringDecoder.decode(ctx, nextRowIndex(descriptor, index)) ?: error("String Element ${index} in ${descriptor} cannot be null")

  override fun decodeBoolean(): Boolean = decoders.BooleanDecoder.decode(ctx, 1) ?: error("Boolean Element cannot be null")
  override fun decodeByte(): Byte = decoders.ByteDecoder.decode(ctx, 1) ?: error("Byte Element cannot be null")
  override fun decodeChar(): Char = decoders.CharDecoder.decode(ctx, 1) ?: error("Char Element cannot be null")
  override fun decodeDouble(): Double = decoders.DoubleDecoder.decode(ctx, 1) ?: error("Double Element cannot be null")
  override fun decodeFloat(): Float = decoders.FloatDecoder.decode(ctx, 1) ?: error("Float Element cannot be null")
  override fun decodeShort(): Short = decoders.ShortDecoder.decode(ctx, 1) ?: error("Short Element cannot be null")
  override fun decodeString(): String = decoders.StringDecoder.decode(ctx, 1) ?: error("String Element cannot be null")
  override fun decodeInt(): Int = decoders.IntDecoder.decode(ctx, 1) ?: error("Int Element cannot be null")
  override fun decodeLong(): Long = decoders.LongDecoder.decode(ctx, 1) ?: error("Long Element cannot be null")


  @ExperimentalSerializationApi
  override fun decodeNull(): Nothing? = null
  //override fun decodeSequentially(): Boolean = true

  @OptIn(ExperimentalSerializationApi::class)
  override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
    return this
  }
  override fun endStructure(descriptor: SerialDescriptor) {
    // Update the rowIndex of the parent
    endCallback(rowIndex)
  }

  @ExperimentalSerializationApi
  override fun <T : Any> decodeNullableSerializableElement(descriptor: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T?>, previousValue: T?): T? {
    val childDesc = descriptor.elementDescriptors.toList()[index]

    fun decodeWithDecoder(decoder: SqlDecoder<Session, Row, T>): T? {
      val rowIndex = nextRowIndex(descriptor, index)
      val decoded = decoder.decode(ctx, rowIndex)
      return decoded
    }

    // If the actual decoded element is supposed to be nullable then make the decoder for it nullable
    fun SqlDecoder<Session, Row, out Any>.asNullableIfSpecified() = if (childDesc.isNullable) asNullable() else this

    return when (childDesc.kind) {
      StructureKind.LIST -> {
        val decoder =
          when {
            childDesc.capturedKClass != null ->
              decoders.decoders.find { it.type == childDesc.capturedKClass }
            childDesc.elementDescriptors.toList().size == 1 && childDesc.elementDescriptors.first().kind is PrimitiveKind.BYTE ->
              decoders.decoders.find { it.type == ByteArray::class }
            else -> null
          }?.asNullableIfSpecified()


        // if there is a decoder for the specific array-type use that, otherwise
        if (decoder != null) {
          @Suppress("UNCHECKED_CAST")
          run { decodeWithDecoder(decoder as SqlDecoder<Session, Row, T>) }
        } else {

          // val elementDescriptor = descriptor.elementDescriptors.first()
          // Need to use this to summon the right mapper
          // elementDescriptor.capturedKClass

          // Can't use element deserializer because it acts on a ResultSet. Need to implement mappers
          //rs.getArray(index.nx) as T
          TODO("Generic collections not implemented yet (found the class: ${childDesc.capturedKClass})")
        }
      }
      StructureKind.CLASS -> {
        // Only if all the columns are null (and the returned element can be null) can we assume that the decoded element should be null
        // Since we're always at the current index (e.g. (Person(name,age),Address(street,zip)) when we're on `street` the index will be 3
        // so we need to check [street..<zip] indexes i.e. [3..<(3+2)] for nullity
        val allRowsNull =
          (rowIndex until (rowIndex + childDesc.elementsCount)).all {
            decoders.isNull(it, ctx.row)
          }

        if (allRowsNull) {
          decodeNull()
        } else {
          deserializer.deserialize(cloneSelf(ctx, rowIndex, { childIndex -> this.rowIndex = childIndex }))
        }
      }
      SerialKind.CONTEXTUAL -> {
        val decoder = decoders.decoders.find { it.type == childDesc.capturedKClass }?.asNullableIfSpecified()
        if (decoder == null) throw IllegalArgumentException("Could not find a decoder for the contextual type ${childDesc.capturedKClass}")
        @Suppress("UNCHECKED_CAST")
        run { decodeWithDecoder(decoder as SqlDecoder<Session, Row, T>) }
      }
      else -> {
        when {
          childDesc.kind is PrimitiveKind ->
            if (decoders.isNull(rowIndex, ctx.row)) {
              // Advance to the next row (since we know the current one is null)
              // otherwise the next row lookup will think the element is still this one (i.e. null)
              //rowIndex += 1
              // increment to the next index
              nextRowIndex(descriptor, index, "Skipping Null Value")
              null
            } else {
              // just doing deserializer.deserialize(this) at this point will just call the non-element decoders e.g. decodeString, decodeInt, etc... we
              // want to call the decoders that have element information in them (e.g. decodeByteElement, decodeShortElement, etc...) so we need to do it manually
              when (childDesc.kind) {
                is PrimitiveKind.BYTE -> decodeByteElement(descriptor, index)
                is PrimitiveKind.SHORT -> decodeShortElement(descriptor, index)
                is PrimitiveKind.INT -> decodeIntElement(descriptor, index)
                is PrimitiveKind.LONG -> decodeLongElement(descriptor, index)
                is PrimitiveKind.FLOAT -> decodeFloatElement(descriptor, index)
                is PrimitiveKind.DOUBLE -> decodeDoubleElement(descriptor, index)
                is PrimitiveKind.BOOLEAN -> decodeBooleanElement(descriptor, index)
                is PrimitiveKind.CHAR -> decodeCharElement(descriptor, index)
                is PrimitiveKind.STRING -> decodeStringElement(descriptor, index)
                else -> throw IllegalArgumentException("Unsupported primitive kind: ${childDesc.kind}")
              } as T?
            }
          else ->
            throw IllegalArgumentException("Unsupported kind: ${childDesc.kind}")
        }
      }
    }
  }


  override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder = this
  @OptIn(ExperimentalSerializationApi::class)
  override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
    if (classIndex >= descriptor.elementsCount) return CompositeDecoder.DECODE_DONE
    //val childDesc = descriptor.elementDescriptors.toList()[fieldIndex]
//    return when (childDesc.kind) {
//      StructureKind.CLASS -> elementIndex
//      else -> elementIndex++
//    }
    val currClassIndex = classIndex
    classIndex += 1
    return currClassIndex
  }

  @OptIn(ExperimentalSerializationApi::class)
  @Suppress("UNCHECKED_CAST")
  override fun <T> decodeSerializableElement(descriptor: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>, previousValue: T?): T {
    val element = decodeNullableSerializableElement(descriptor, index, deserializer, previousValue)
    return when {
      element != null -> element
      //now: element == null must be true
      descriptor.getElementDescriptor(index).isNullable -> null as T
      else -> throw IllegalArgumentException("Found null element at index ${index} of descriptor ${descriptor.getElementDescriptor(index)} (of ${descriptor}) where null values are not allowed.")
    }
  }

  override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
    TODO("Not yet implemented")
  }

  override fun decodeInline(descriptor: SerialDescriptor): Decoder = this

  /**
   * Checks to see if the element is null before calling an actual deserialzier. Can't use this for nested classes because
   * we need to check all upcoming rows to see if all of them are null, only then is the parent element considered null
   * so instead we just opt to return true and check for nullity in the parent call of decodeNullableSerializableElement.
   */
  @ExperimentalSerializationApi
  override fun decodeNotNullMark(): Boolean {
    return true
  }


}