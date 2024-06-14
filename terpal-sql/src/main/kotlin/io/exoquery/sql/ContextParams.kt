package io.exoquery.sql

import java.time.*

interface ContextParams<Session, Stmt> {
  fun param(value: Boolean): Param<Session, Stmt, Boolean>
  fun param(value: Byte): Param<Session, Stmt, Byte>
  fun param(value: Char): Param<Session, Stmt, Char>
  fun param(value: Double): Param<Session, Stmt, Double>
  fun param(value: Float): Param<Session, Stmt, Float>
  fun param(value: Int): Param<Session, Stmt, Int>
  fun param(value: Long): Param<Session, Stmt, Long>
  fun param(value: Short): Param<Session, Stmt, Short>
  fun param(value: String): Param<Session, Stmt, String>

  fun param(value: LocalDate): Param<Session, Stmt, LocalDate>
  fun param(value: LocalTime): Param<Session, Stmt, LocalTime>
  fun param(value: LocalDateTime): Param<Session, Stmt, LocalDateTime>
  fun param(value: ZonedDateTime): Param<Session, Stmt, ZonedDateTime>

  fun param(value: Instant): Param<Session, Stmt, Instant>
  fun param(value: OffsetTime): Param<Session, Stmt, OffsetTime>
  fun param(value: OffsetDateTime): Param<Session, Stmt, OffsetDateTime>
}