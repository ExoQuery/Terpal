package io.exoquery.sql.jdbc

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.sql.PreparedStatement

interface SwitchingContextSerializer<T, Session>: KSerializer<T> {
  val alternate: KSerializer<T>
  val encoder: Session.(T, Int) -> Unit

  override val descriptor: SerialDescriptor get() = alternate.descriptor
  override fun deserialize(decoder: Decoder): T = alternate.deserialize(decoder)
  override fun serialize(encoder: Encoder, value: T) = alternate.serialize(encoder, value)
}

data class SwitchingJdbcSerializer<T>(
  override val alternate: KSerializer<T>,
  override val encoder: PreparedStatement.(T, Int) -> Unit
): SwitchingContextSerializer<T, PreparedStatement> {
  companion object {
    operator fun <T> invoke(encoder: PreparedStatement.(T, Int) -> Unit) =
      SwitchingJdbcSerializer<T>(UnusedSerializer(), encoder)
  }
}

class UnusedSerializer<T>: KSerializer<T> {
  override val descriptor: SerialDescriptor = error("Cannot use a UnusedSerializer instance")
  override fun deserialize(decoder: Decoder): T = error("Cannot call `deserialize` from UnusedSerializer instance")
  override fun serialize(encoder: Encoder, value: T) = error("Cannot call `serialize` from UnusedSerializer instance")
}
