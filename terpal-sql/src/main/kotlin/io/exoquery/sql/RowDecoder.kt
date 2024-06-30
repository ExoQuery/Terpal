package io.exoquery.sql

import io.exoquery.sql.jdbc.JdbcDecodingContext
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
  ctx: JdbcDecodingContext,
  initialRowIndex: Int,
  api: ApiDecoders<Connection, ResultSet>,
  decoders: Set<SqlDecoder<Connection, ResultSet, out Any>>,
  columnInfos: List<ColumnInfo>,
  endCallback: (Int) -> Unit
): RowDecoder<Connection, ResultSet>(ctx, initialRowIndex, api, decoders, columnInfos, endCallback) {

  companion object {
    operator fun invoke(
      ctx: JdbcDecodingContext,
      api: ApiDecoders<Connection, ResultSet>,
      decoders: Set<SqlDecoder<Connection, ResultSet, out Any>>,
      descriptor: SerialDescriptor
    ): JdbcRowDecoder {
      fun metaColumnData(meta: ResultSetMetaData) =
        (1..meta.columnCount).map { ColumnInfo(meta.getColumnName(it), meta.getColumnTypeName(it)) }
      val metaColumns = metaColumnData(ctx.row.metaData)
      descriptor.verifyColumns(metaColumns)
      return JdbcRowDecoder(ctx, 1, api, decoders, metaColumns, {})
    }
  }

  override fun cloneSelf(ctx: JdbcDecodingContext, initialRowIndex: Int, endCallback: (Int) -> Unit): RowDecoder<Connection, ResultSet> =
    JdbcRowDecoder(ctx, initialRowIndex, api, decoders, columnInfos, endCallback)
}

@OptIn(ExperimentalSerializationApi::class)
abstract class RowDecoder<Session, Row>(
  val ctx: DecodingContext<Session, Row>,
  val initialRowIndex: Int,
  val api: ApiDecoders<Session, Row>,
  val decoders: Set<SqlDecoder<Session, Row, out Any>>,
  val columnInfos: List<ColumnInfo>,
  val endCallback: (Int) -> Unit
): Decoder, CompositeDecoder {

  abstract fun cloneSelf(ctx: DecodingContext<Session, Row>, initialRowIndex: Int, endCallback: (Int) -> Unit): RowDecoder<Session, Row>

  var rowIndex: Int = initialRowIndex
  var classIndex: Int = 0

  fun nextRowIndex(desc: SerialDescriptor, descIndex: Int, note: String = ""): Int {
    val curr = rowIndex
    println("---- Get Row ${columnInfos[rowIndex-1].name}, Index: ${curr} - (${descIndex}) ${desc.getElementDescriptor(descIndex)} - (Preview:${api.preview(rowIndex, ctx.row)})" + (if (note != "") " - ${note}" else ""))
    rowIndex += 1
    return curr
  }

  fun nextRowIndex(note: String = ""): Int {
    val curr = rowIndex
    println("---- Get Row ${columnInfos[rowIndex-1].name} - (Preview:${api.preview(rowIndex, ctx.row)})" + (if (note != "") " - ${note}" else ""))
    rowIndex += 1
    return curr
  }

  override val serializersModule: SerializersModule = EmptySerializersModule()

  override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean = api.BooleanDecoder.decode(ctx, nextRowIndex(descriptor, index))
  override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte = api.ByteDecoder.decode(ctx, nextRowIndex(descriptor, index))
  override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char = api.CharDecoder.decode(ctx, nextRowIndex(descriptor, index))
  override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double = api.DoubleDecoder.decode(ctx, nextRowIndex(descriptor, index))
  override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float = api.FloatDecoder.decode(ctx, nextRowIndex(descriptor, index))
  override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int = api.IntDecoder.decode(ctx, nextRowIndex(descriptor, index))
  override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long = api.LongDecoder.decode(ctx, nextRowIndex(descriptor, index))
  override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short = api.ShortDecoder.decode(ctx, nextRowIndex(descriptor, index))
  override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String = api.StringDecoder.decode(ctx, nextRowIndex(descriptor, index))

  // These are primarily used when there is some kind of encoder delegation used e.g. if it is a new-type or wrapped-type e.g. DateToLongSerializer
  // they will be invoked from `deserializer.deserialize(this)` in decodeNullableSerializableElement in the last clause.
  override fun decodeBoolean(): Boolean = api.BooleanDecoder.decode(ctx, nextRowIndex())
  override fun decodeByte(): Byte = api.ByteDecoder.decode(ctx, nextRowIndex())
  override fun decodeChar(): Char = api.CharDecoder.decode(ctx, nextRowIndex())
  override fun decodeDouble(): Double = api.DoubleDecoder.decode(ctx, nextRowIndex())
  override fun decodeFloat(): Float = api.FloatDecoder.decode(ctx, nextRowIndex())
  override fun decodeShort(): Short = api.ShortDecoder.decode(ctx, nextRowIndex())
  override fun decodeString(): String = api.StringDecoder.decode(ctx, nextRowIndex())
  override fun decodeInt(): Int = api.IntDecoder.decode(ctx, nextRowIndex())
  override fun decodeLong(): Long = api.LongDecoder.decode(ctx, nextRowIndex())


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
            // When its contextual, get the decoder for that base on the capturedKClass
            childDesc.capturedKClass != null ->
              decoders.find { it.type == childDesc.capturedKClass }
                ?: throw IllegalArgumentException("Could not find a decoder for the (contextual) structural list type ${childDesc.capturedKClass} with the descriptor: ${childDesc} because not decoder for ${childDesc.capturedKClass} was found")

            childDesc.elementDescriptors.toList().size == 1 && childDesc.elementDescriptors.first().kind is PrimitiveKind.BYTE ->
              // When its not contextual there wont be a captured class, in that case get the first type-parameter from the List descriptor and decode some known types based on that
              decoders.find { it.type == ByteArray::class }
                ?: throw IllegalArgumentException("Could not find a byte array decoder in the database-context for the list type ${childDesc.capturedKClass}")

            else ->
              throw IllegalArgumentException("Could not find a decoder for the structural list type ${childDesc.capturedKClass} with the descriptor: ${childDesc}. It had an invalid form.")
          }.asNullableIfSpecified()

        // if there is a decoder for the specific array-type use that, otherwise
        run { decodeWithDecoder(decoder as SqlDecoder<Session, Row, T>) }
      }
      StructureKind.CLASS -> {
        // Only if all the columns are null (and the returned element can be null) can we assume that the decoded element should be null
        // Since we're always at the current index (e.g. (Person(name,age),Address(street,zip)) when we're on `street` the index will be 3
        // so we need to check [street..<zip] indexes i.e. [3..<(3+2)] for nullity
        val allRowsNull =
          (rowIndex until (rowIndex + childDesc.elementsCount)).all {
            api.isNull(it, ctx.row)
          }

        if (allRowsNull) {
          decodeNull()
        } else {
          deserializer.deserialize(cloneSelf(ctx, rowIndex, { childIndex -> this.rowIndex = childIndex }))
        }
      }
      SerialKind.CONTEXTUAL -> {
        val decoder = decoders.find { it.type == childDesc.capturedKClass }?.asNullableIfSpecified()
        if (decoder == null) throw IllegalArgumentException("Could not find a decoder for the contextual type ${childDesc.capturedKClass}")
        @Suppress("UNCHECKED_CAST")
        run { decodeWithDecoder(decoder as SqlDecoder<Session, Row, T>) }
      }
      else -> {
        when {
          childDesc.kind is PrimitiveKind ->
            if (api.isNull(rowIndex, ctx.row)) {
              // Advance to the next row (since we know the current one is null)
              // otherwise the next row lookup will think the element is still this one (i.e. null)
              //rowIndex += 1
              // increment to the next index
              nextRowIndex(descriptor, index, "Skipping Null Value")
              null
            } else {
              val descKind = childDesc.kind
              val serialName = childDesc.serialName
              // just doing deserializer.deserialize(this) at this point will just call the non-element decoders e.g. decodeString, decodeInt, etc... we
              // want to call the decoders that have element information in them (e.g. decodeByteElement, decodeShortElement, etc...) if this is possible
              when {
                 descKind is PrimitiveKind.BYTE  && serialName == "kotlin.Byte" -> decodeByteElement(descriptor, index)
                 descKind is PrimitiveKind.SHORT  && serialName == "kotlin.Short" -> decodeShortElement(descriptor, index)
                 descKind is PrimitiveKind.INT  && serialName == "kotlin.Int" -> decodeIntElement(descriptor, index)
                 descKind is PrimitiveKind.LONG  && serialName == "kotlin.Long" -> decodeLongElement(descriptor, index)
                 descKind is PrimitiveKind.FLOAT  && serialName == "kotlin.Float" -> decodeFloatElement(descriptor, index)
                 descKind is PrimitiveKind.DOUBLE  && serialName == "kotlin.Double" -> decodeDoubleElement(descriptor, index)
                 descKind is PrimitiveKind.BOOLEAN  && serialName == "kotlin.Boolean"  -> decodeBooleanElement(descriptor, index)
                 descKind is PrimitiveKind.CHAR && serialName == "kotlin.Char" -> decodeCharElement(descriptor, index)
                 descKind is PrimitiveKind.STRING && serialName == "kotlin.String" -> decodeStringElement(descriptor, index)
                else -> {
                  //if it is a primitive type wrapped into a non-primitive type (e.g. DateToLongSerializer) then use the encoder defined in the serialization. Note that
                  //also known as a new-type (e.g. NewTypeInt(value: Int) then this serializer will be the wrapped one
                  deserializer.deserialize(this)
                }
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