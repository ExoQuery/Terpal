package io.exoquery.sql.jdbc

import io.exoquery.sql.SqlDecoder
import java.math.BigDecimal
import java.sql.Connection
import java.sql.ResultSet
import java.time.*
import java.util.*

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
          f(ctx, index) ?:
          throw NullPointerException("Non-nullable Decoder returned null for index $index, column: ${ctx.row.metaData.getColumnName(index)} and expected type: ${T::class}")
      }
  }
}
