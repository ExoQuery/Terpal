package io.exoquery.terpal.plugin

object Globals {

  // This needs to be at the top for some reason, even if it is lazy
  private val cacheMap: MutableMap<String, Any> by lazy { mutableMapOf() }

  // TODO for KMP use kotlinx.cinterop.* from kotlin-stdlib-common
  //      see https://stackoverflow.com/a/55002326
  private fun variable(propName: String, envName: String, default: String) =
    System.getProperty(propName) ?: System.getenv(envName) ?: default

  val logWrappers get() = cache("terpal.trace.wrappers", variable("terpal.trace.wrappers", "terpal_trace_wrappers", "false").toBoolean())

  fun resetCache(): Unit = cacheMap.clear()

  @Suppress("UNCHECKED_CAST")
  private fun <T> cache(name: String, value: T): T =
    cacheMap.getOrPut(name, { value as Any }) as T
}