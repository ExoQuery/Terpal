package io.exoquery.sql

import io.exoquery.sql.jdbc.AtomEncoder
import io.exoquery.sql.jdbc.JdbcAtomEncoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlinx.serialization.serializerOrNull
import java.sql.Connection
import java.sql.PreparedStatement
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

data class Param<T>(val value: T, val payload: Payload<T>) {
  sealed interface Payload<T> {
    data class Serial<T>(val serializer: KSerializer<T>): Payload<T>
    data class Atomic<T>(val atomKind: Atom<T>): Payload<T>
  }

  @Suppress("UNCHECKED_CAST")
  companion object {
    inline operator fun <reified T> invoke(value: T): Param<T> {
      val tpe = typeOf<T>()
      return Param<T>(
        value,
        serializerOrNull(tpe)?.let { ser -> Payload.Serial<T>(ser as KSerializer<T>) } ?: run {
          CommonAtoms.values.find { tpe.isSubtypeOf(it.type) }?.let { atom -> Payload.Atomic(atom as Atom<T>) }
            ?: error("Cannot find a encodeable CommonAtom whose type (or-subtype) is ${tpe}. Possible types are: ${CommonAtoms.values.map { it.type }}")
        }
      )
    }
  }
}

class AtomEncoders<Session, Stmt> private constructor (val map: Map<Atom<*>, AtomEncoder<Session, Stmt, *>>) {

  @Suppress("UNCHECKED_CAST")
  fun <T> get(kind: Atom<T>): AtomEncoder<Session, Stmt, T>? =
    map.get(kind)?.let { (it as AtomEncoder<Session, Stmt, T> ) }

  operator fun plus(other: AtomEncoders<Session, Stmt>) = AtomEncoders(this.map + other.map)

  companion object {
    fun <T, Session, Stmt> single(atom: Atom<T>, encoder: AtomEncoder<Session, Stmt, T>) =
      AtomEncoders(mapOf(atom to encoder))
  }
}

interface Atom<T> {
  val type: KType
}
object CommonAtoms {
  object LocalDate: Atom<java.time.LocalDate> { override val type: KType = typeOf<java.time.LocalDate>() }
  object LocalTime: Atom<java.time.LocalTime> { override val type: KType = typeOf<java.time.LocalTime>() }

  val values = listOf(LocalDate, LocalTime)
}