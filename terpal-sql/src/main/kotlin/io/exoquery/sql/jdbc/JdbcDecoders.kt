package io.exoquery.sql.jdbc

import io.exoquery.sql.SqlDecoder
import java.awt.event.FocusEvent.Cause
import java.math.BigDecimal
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.time.*
import java.util.*

data class EncodingException(val msg: String, val errorCause: Throwable? = null): SQLException(msg.toString(), errorCause) {
  override fun toString(): String = msg
}

/** Represents a Jdbc Decoder with a nullable or non-nullable output value */
typealias JdbcDecoder<T> = SqlDecoder<Connection, ResultSet, T>

/** Represents a Jdbc Decoder with a non-nullable output value */
abstract class JdbcDecoderAny<T: Any>: JdbcDecoder<T>() {
  inline fun <reified R: Any> map(crossinline f: (T) -> R): JdbcDecoderAny<R> =
    object: JdbcDecoderAny<R>() {
      override val type = R::class
      override fun decode(ctx: JdbcDecodingContext, index: Int) =
        f(this@JdbcDecoderAny.decode(ctx, index))
    }

  override fun asNullable(): SqlDecoder<Connection, ResultSet, T?> =
    object: SqlDecoder<Connection, ResultSet, T?>() {
      override fun asNullable(): SqlDecoder<Connection, ResultSet, T?> = this
      override val type = this@JdbcDecoderAny.type
      override fun decode(ctx: JdbcDecodingContext, index: Int): T? {
        ctx.row.getObject(index)
        return if (ctx.row.wasNull())
          null
        else
          this@JdbcDecoderAny.decode(ctx, index)
      }
    }

  companion object {
    inline fun <reified T: Any> fromFunction(crossinline f: (JdbcDecodingContext, Int) -> T?): JdbcDecoderAny<T> =
      object: JdbcDecoderAny<T>() {
        override val type = T::class
        override fun decode(ctx: JdbcDecodingContext, index: Int) =
          try {
            f(ctx, index) ?: throw EncodingException("Non-nullable Decoder returned null")
          } catch (e: Throwable) {
            throw EncodingException("Error decoding index: $index, column: ${ctx.row.metaData.getColumnName(index)} and expected type: ${T::class}", e)
          }

      }
  }
}
