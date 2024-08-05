package io.exoquery.terpal.plugin

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

data class OptionKey<T>(val name: String) {
  val compilerKey = CompilerConfigurationKey<Boolean>(name)
}

object OptionKeys {
  val TraceWrappers = OptionKey<Boolean>("traceWrappers")
}

class Options(val configuration: CompilerConfiguration) {
  val traceWrappers = configuration.get(OptionKeys.TraceWrappers.compilerKey) ?: false
}
