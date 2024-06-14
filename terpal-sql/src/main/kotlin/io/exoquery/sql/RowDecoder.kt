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
import kotlin.reflect.KClass

interface ColumnDecoder<Session, Row, T> {
  val cls: KClass<*>
  val decodeType: (Session, Row, Int) -> T
}

interface ContextDecoders {

}

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
  decoders: Decoders<Connection, ResultSet>,
  columnInfos: List<ColumnInfo>,
  endCallback: (Int) -> Unit
): RowDecoder<Connection, ResultSet>(sess, rs, initialRowIndex, decoders, columnInfos, endCallback) {

  companion object {
    operator fun invoke(sess: Connection, rs: ResultSet, descriptor: SerialDescriptor): JdbcRowDecoder {
      fun metaColumnData(meta: ResultSetMetaData) =
        (1..meta.columnCount).map { ColumnInfo(meta.getColumnName(it), meta.getColumnTypeName(it)) }
      val metaColumns = metaColumnData(rs.metaData)
      descriptor.verifyColumns(metaColumns)
      return JdbcRowDecoder(sess, rs, 1, JdbcDecodersWithTime(), metaColumns, {})
    }
  }

  override fun cloneSelf(rs: ResultSet, initialRowIndex: Int, endCallback: (Int) -> Unit): RowDecoder<Connection, ResultSet> =
    JdbcRowDecoder(sess, rs, initialRowIndex, decoders, columnInfos, endCallback)
}

@OptIn(ExperimentalSerializationApi::class)
abstract class RowDecoder<Session, Row>(val sess: Session, val rs: Row, val initialRowIndex: Int, val decoders: Decoders<Session, Row>, val columnInfos: List<ColumnInfo>, val endCallback: (Int) -> Unit): Decoder, CompositeDecoder {

  abstract fun cloneSelf(rs: Row, initialRowIndex: Int, endCallback: (Int) -> Unit): RowDecoder<Session, Row>

  var rowIndex: Int = initialRowIndex
  var classIndex: Int = 0

  fun nextRowIndex(desc: SerialDescriptor): Int {
    val curr = rowIndex
    println("---- Get Row Index: ${curr} - ${desc}")
    rowIndex += 1
    return curr
  }

  override val serializersModule: SerializersModule = EmptySerializersModule()

  override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean = decoders.BooleanDecoder.decode(sess, rs, nextRowIndex(descriptor)) ?: error("Boolean Element ${index} in ${descriptor} cannot be null")
  override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte = decoders.ByteDecoder.decode(sess, rs, nextRowIndex(descriptor)) ?: error("Byte Element ${index} in ${descriptor} cannot be null")
  override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char = decoders.CharDecoder.decode(sess, rs, nextRowIndex(descriptor)) ?: error("Char Element ${index} in ${descriptor} cannot be null")
  override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double = decoders.DoubleDecoder.decode(sess, rs, nextRowIndex(descriptor)) ?: error("Double Element ${index} in ${descriptor} cannot be null")
  override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float = decoders.FloatDecoder.decode(sess, rs, nextRowIndex(descriptor)) ?: error("Float Element ${index} in ${descriptor} cannot be null")
  override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int = decoders.IntDecoder.decode(sess, rs, nextRowIndex(descriptor)) ?: error("Int Element ${index} in ${descriptor} cannot be null")
  override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long = decoders.LongDecoder.decode(sess, rs, nextRowIndex(descriptor)) ?: error("Long Element ${index} in ${descriptor} cannot be null")
  override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short = decoders.ShortDecoder.decode(sess, rs, nextRowIndex(descriptor)) ?: error("Short Element ${index} in ${descriptor} cannot be null")
  override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String =
    decoders.StringDecoder.decode(sess, rs, nextRowIndex(descriptor)) ?:
    error("String Element ${index} in ${descriptor} cannot be null")

  override fun decodeBoolean(): Boolean = decoders.BooleanDecoder.decode(sess, rs, 1) ?: error("Boolean Element cannot be null")
  override fun decodeByte(): Byte = decoders.ByteDecoder.decode(sess, rs, 1) ?: error("Byte Element cannot be null")
  override fun decodeChar(): Char = decoders.CharDecoder.decode(sess, rs, 1) ?: error("Char Element cannot be null")
  override fun decodeDouble(): Double = decoders.DoubleDecoder.decode(sess, rs, 1) ?: error("Double Element cannot be null")
  override fun decodeFloat(): Float = decoders.FloatDecoder.decode(sess, rs, 1) ?: error("Float Element cannot be null")
  override fun decodeShort(): Short = decoders.ShortDecoder.decode(sess, rs, 1) ?: error("Short Element cannot be null")
  override fun decodeString(): String = decoders.StringDecoder.decode(sess, rs, 1) ?: error("String Element cannot be null")
  override fun decodeInt(): Int = decoders.IntDecoder.decode(sess, rs, 1) ?: error("Int Element cannot be null")
  override fun decodeLong(): Long = decoders.LongDecoder.decode(sess, rs, 1) ?: error("Long Element cannot be null")


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
    return when (childDesc.kind) {
      StructureKind.LIST -> {
        // val elementDescriptor = descriptor.elementDescriptors.first()
        // Need to use this to summon the right mapper
        // elementDescriptor.capturedKClass

        // Can't use element deserializer because it acts on a ResultSet. Need to implement mappers
        //rs.getArray(index.nx) as T
        TODO()
      }
      StructureKind.CLASS -> {
        // Only if all the columns are null (and the returned element can be null) can we assume that the decoded element should be null
        // Since we're always at the current index (e.g. (Person(name,age),Address(street,zip)) when we're on `street` the index will be 3
        // so we need to check [street..<zip] indexes i.e. [3..<(3+2)] for nullity
        val allRowsNull =
          (rowIndex until (rowIndex + childDesc.elementsCount)).all {
            decoders.isNull(it, rs)
          }

        if (allRowsNull) {
          decodeNull()
        } else {
          deserializer.deserialize(cloneSelf(rs, rowIndex, { childIndex -> this.rowIndex = childIndex }))
        }
      }
      SerialKind.CONTEXTUAL -> {
        val decoder = decoders.decoders.find { it.type == childDesc.capturedKClass }
        val columnName = try { columnInfos.get(index) } catch (e: Throwable) { "<UNKNOWN>" }
        val decodedValueRaw =
          decoder?.decode(sess, rs, nextRowIndex(descriptor)) ?:
          throw IllegalArgumentException("Could not decode the contextual column ${columnName} (index: ${rowIndex}) whose expected class was: ${childDesc.capturedKClass}")

        @Suppress("UNCHECKED_CAST")
        run { decodedValueRaw as T? }
      }
      else -> {
        when {
          childDesc.kind is PrimitiveKind ->
            if (decoders.isNull(rowIndex, rs)) {
              // Advance to the next row (since we know the current one is null)
              // otherwise the next row lookup will think the element is still this one (i.e. null)
              rowIndex += 1
              null
            } else
              deserializer.deserialize(this)
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