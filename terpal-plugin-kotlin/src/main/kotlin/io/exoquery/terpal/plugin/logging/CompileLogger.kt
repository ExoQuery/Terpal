package io.exoquery.terpal.plugin.logging

import io.exoquery.terpal.plugin.location
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocationWithRange
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation

data class CompileLogger(val messageCollector: MessageCollector, val currentFile: IrFile, val macroCallSite: IrElement) {
  fun foo() {
    macroCallSite.location(currentFile.fileEntry).lineContent
  }

  fun warn(msg: String) =
    messageCollector.report(CompilerMessageSeverity.WARNING, msg, macroCallSite.location(currentFile.fileEntry))

  fun error(msg: String) =
    messageCollector.report(CompilerMessageSeverity.ERROR, msg, macroCallSite.location(currentFile.fileEntry))

  companion object {
    operator fun invoke(config: CompilerConfiguration, currentFile: IrFile, macroCallSite: IrElement) =
      config.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE).let {
        CompileLogger(it, currentFile, macroCallSite)
      }
  }
}
