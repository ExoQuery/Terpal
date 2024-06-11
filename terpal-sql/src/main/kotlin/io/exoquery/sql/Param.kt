package io.exoquery.sql


interface Param<Session, Statement, T: Any>: SqlFragment {
  val value: T
  val encoder: Encoder<Session, Statement, T>
}
