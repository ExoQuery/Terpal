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

  override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean = decoders.BooleanDecoder.decode(sess, rs, nextRowIndex(descriptor))
  override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte = decoders.ByteDecoder.decode(sess, rs, nextRowIndex(descriptor))
  override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char = decoders.CharDecoder.decode(sess, rs, nextRowIndex(descriptor))
  override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double = decoders.DoubleDecoder.decode(sess, rs, nextRowIndex(descriptor))
  override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float = decoders.FloatDecoder.decode(sess, rs, nextRowIndex(descriptor))
  override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int = decoders.IntDecoder.decode(sess, rs, nextRowIndex(descriptor))
  override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long = decoders.LongDecoder.decode(sess, rs, nextRowIndex(descriptor))
  override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short = decoders.ShortDecoder.decode(sess, rs, nextRowIndex(descriptor))
  override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String = decoders.StringDecoder.decode(sess, rs, nextRowIndex(descriptor))

  override fun decodeBoolean(): Boolean = decoders.BooleanDecoder.decode(sess, rs, 1)
  override fun decodeByte(): Byte = decoders.ByteDecoder.decode(sess, rs, 1)
  override fun decodeChar(): Char = decoders.CharDecoder.decode(sess, rs, 1)
  override fun decodeDouble(): Double = decoders.DoubleDecoder.decode(sess, rs, 1)
  override fun decodeFloat(): Float = decoders.FloatDecoder.decode(sess, rs, 1)
  override fun decodeShort(): Short = decoders.ShortDecoder.decode(sess, rs, 1)
  override fun decodeString(): String = decoders.StringDecoder.decode(sess, rs, 1)
  override fun decodeInt(): Int = decoders.IntDecoder.decode(sess, rs, 1)
  override fun decodeLong(): Long = decoders.LongDecoder.decode(sess, rs, 1)


  @ExperimentalSerializationApi
  override fun decodeNull(): Nothing? = null
  //override fun decodeSequentially(): Boolean = true

  @OptIn(ExperimentalSerializationApi::class)
  override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder = this
  override fun endStructure(descriptor: SerialDescriptor) {
    // Update the rowIndex of the parent
    endCallback(rowIndex)
  }

  @ExperimentalSerializationApi
  override fun <T : Any> decodeNullableSerializableElement(descriptor: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T?>, previousValue: T?): T? {
    return if (decoders.isNull(rowIndex, rs)) {
      rowIndex += 1
      null
    }
    else deserializer.deserialize(this)
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
        //deserializer.deserialize(cloneSelf(rs, offset))
        deserializer.deserialize(cloneSelf(rs, rowIndex, { childIndex -> this.rowIndex = childIndex }))
      }
      SerialKind.CONTEXTUAL -> {
        val decoder = decoders.decoders.find { it.type == childDesc.capturedKClass }
        val columnName = try { columnInfos.get(index) } catch (e: Throwable) { "<UNKNOWN>" }
        val decodedValueRaw =
          decoder?.decode(sess, rs, nextRowIndex(descriptor)) ?:
            throw IllegalArgumentException("Could not decode the contextual column ${columnName} (index: ${rowIndex}) whose expected class was: ${childDesc.capturedKClass}")

        decodedValueRaw as T
      }
      else ->
        throw IllegalArgumentException("Unsupported serial structure-kind: ${childDesc.kind}")
    }
  }

  override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
    TODO("Not yet implemented")
  }

  override fun decodeInline(descriptor: SerialDescriptor): Decoder = this

  @ExperimentalSerializationApi
  override fun decodeNotNullMark(): Boolean {
    return decoders.isNull(rowIndex, rs)
  }


}