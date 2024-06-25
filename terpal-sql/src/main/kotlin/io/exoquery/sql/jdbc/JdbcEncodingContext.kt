package io.exoquery.sql.jdbc

import io.exoquery.sql.DecodingContext
import io.exoquery.sql.EncodingContext
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Types
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

typealias JdbcEncodingContext = EncodingContext<Connection, PreparedStatement>
typealias JdbcDecodingContext = DecodingContext<Connection, java.sql.ResultSet>
