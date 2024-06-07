package io.exoquery.sql


interface Param<Session, Statement, T: Any>: Fragment {
  val value: T
  val encoder: Encoder<Session, Statement, T>
}
