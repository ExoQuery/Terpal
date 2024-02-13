package io.exoquery.terpal.plugin.logging

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocationWithRange
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation

data class CompileLogger(val messageCollector: MessageCollector) {
  fun warn(msg: String) =
    messageCollector.report(CompilerMessageSeverity.WARNING, msg)

  fun error(msg: String) =
    messageCollector.report(CompilerMessageSeverity.ERROR, msg)

  fun error(msg: String, loc: CompilerMessageSourceLocation) {
    messageCollector.report(CompilerMessageSeverity.ERROR, msg, loc)
  }

  companion object {
    operator fun invoke(config: CompilerConfiguration) =
      config.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE).let {
        CompileLogger(it)
      }
  }
}