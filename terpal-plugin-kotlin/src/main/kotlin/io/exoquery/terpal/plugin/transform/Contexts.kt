package io.exoquery.terpal.plugin.transform

import io.exoquery.terpal.plugin.location
import io.exoquery.terpal.plugin.logging.CompileLogger
import io.exoquery.terpal.plugin.trees.Lifter
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import java.io.File

data class BuilderContext(
  val pluginCtx: IrPluginContext,
  val compilerConfig: CompilerConfiguration,
  val scopeOwner: IrSymbol,
  val logger: CompileLogger,
  val currentFile: IrFile,
  val currentExpr: IrExpression
) {
  val builder = DeclarationIrBuilder(pluginCtx, scopeOwner, currentExpr.startOffset, currentExpr.endOffset)
  fun makeLifter() = Lifter(this)
}

fun <R> BuilderContext.withCtxAndLogger(f: context(BuilderContext, CompileLogger) () -> R): R = f(this, logger)

data class TransformerOrigin(
  val pluginCtx: IrPluginContext,
  val config: CompilerConfiguration,
  val currentFile: IrFile,
  val currentExpr: IrExpression
) {
  val logger = CompileLogger(config, currentFile, currentExpr)
  fun makeBuilderContext(expr: IrExpression, scopeOwner: IrSymbol) =
    BuilderContext(pluginCtx, config, scopeOwner, logger, currentFile, expr)
}
