package io.exoquery.sql

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import java.sql.PreparedStatement

data class Param<T>(val value: T, val serializer: KSerializer<T>) {
  fun write(index: Int, ps: PreparedStatement) =
    serializer.serialize(PreparedStatementElementEncoder(ps, index+1), value)

  companion object {
    inline operator fun <reified T> invoke(value: T) = Param<T>(value, serializer<T>())
  }
}