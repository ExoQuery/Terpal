package io.exoquery.sql.jdbc.context

import java.sql.Connection
import javax.sql.DataSource
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

@PublishedApi
internal class CoroutineTransaction(
  private var completed: Boolean = false
) : AbstractCoroutineContextElement(CoroutineTransaction) {

  companion object Key : CoroutineContext.Key<CoroutineTransaction>

  val incomplete: Boolean
    get() = !completed

  fun complete() {
    completed = true
  }

  override fun toString(): String = "CoroutineTransaction(completed=$completed)"
}

val CoroutineContext.dataSource: DataSource
  get() = get(CoroutineDataSource)?.dataSource ?: error("No data source in context")

class CoroutineDataSource(
  val dataSource: DataSource
) : AbstractCoroutineContextElement(CoroutineDataSource) {

  companion object Key : CoroutineContext.Key<CoroutineDataSource>

  override fun toString() = "CoroutineDataSource($dataSource)"
}

val CoroutineContext.connection: Connection
  get() = get(CoroutineConnection)?.connection ?: error("No connection in context")

class CoroutineConnection(
  val connection: Connection
) : AbstractCoroutineContextElement(CoroutineConnection) {

  companion object Key : CoroutineContext.Key<CoroutineConnection>

  override fun toString() = "CoroutineConnection($connection)"
}


