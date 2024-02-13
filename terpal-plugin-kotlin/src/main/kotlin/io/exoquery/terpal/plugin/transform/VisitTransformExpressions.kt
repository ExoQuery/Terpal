package io.exoquery.terpal.plugin.transform

import io.exoquery.terpal.plugin.trees.ExtractorsDomain
import io.exoquery.terpal.plugin.logging.CompileLogger
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocationWithRange
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import java.nio.file.Path
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid


class VisitTransformExpressions(
  private val context: IrPluginContext,
  private val config: CompilerConfiguration,
  private val projectDir: Path
) : IrElementTransformerVoidWithContext() {

  val logger = CompileLogger(config)

  private fun typeIsFqn(type: IrType, fqn: String): Boolean {
    if (type !is IrSimpleType) return false

    return when (val owner = type.classifier.owner) {
      is IrClass -> owner.kotlinFqName.asString() == fqn
      else -> false
    }
  }

  override fun visitCall(expression: IrCall): IrExpression {
    val scopeOwner = currentScope!!.scope.scopeOwnerSymbol
    val compileLogger = CompileLogger(config)

    val transformPrint = TransformPrintSource(context, config, scopeOwner)
    val transformerCtx = TransformerOrigin(context, config, this.currentFile)

    val builderContext = transformerCtx.makeBuilderContext(expression, scopeOwner)
    val transformInterpolations = TransformInterepolatorInvoke(builderContext)

    val out = when {
      // 1st that that runs here because printed stuff should not be transformed
      // (and this does not recursively transform stuff inside)
      transformPrint.matches(expression) -> {
        transformPrint.transform(expression)
      }

      transformInterpolations.matches(expression) -> {
        transformInterpolations.transform(expression, this)
      }

      else ->
        super.visitCall(expression)
    }
    return out
  }
}
