package io.exoquery.sql

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Types
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

open class EncodingContext<Session, Stmt>(open val session: Session, open val stmt: Stmt, open val timeZone: TimeZone)
open class DecodingContext<Session, Row>(open val session: Session, open val row: Row, open val timeZone: TimeZone)
