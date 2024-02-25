package io.exoquery.terpal.plugin.transform

import io.decomat.Is
import io.decomat.case
import io.decomat.on
import io.exoquery.terpal.parseError
import io.exoquery.terpal.plugin.trees.Ir
import io.exoquery.terpal.plugin.logging.CompileLogger
import io.exoquery.terpal.plugin.logging.CompileMessages
import io.exoquery.terpal.plugin.printing.dumpSimple
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class TransformPrintSource(
  private val context: BuilderContext
) {
  private val compileLogger = context.logger

  private val printSourceFqn: String = "io.exoquery.terpal.printSource"

  fun matches(expression: IrCall): Boolean =
    expression.symbol.owner.kotlinFqName.asString()
      .let { it == printSourceFqn }

  fun transform(expression: IrCall): IrCall {
    val args =
      with(compileLogger) {
        on(expression).match(
          case(Ir.Call.FunctionUntethered1[Ir.FunctionExpression.withBlockStatements[Is(), Is()]]).then { (_, args) ->
            args
          }
        )
      } ?: parseError("Parsing Failed\n================== The expresson was not a Global Function (with one argument-block): ==================\n" + expression.dumpKotlinLike() + "\n--------------------------\n" + expression.dumpSimple())

    val printSourceExpr = context.pluginCtx
      .referenceFunctions(
        CallableId(FqName("io.exoquery.terpal"), Name.identifier("printSourceExpr"))
      ).first()

    val message = CompileMessages.PrintingMessageMulti(args)

    compileLogger.warn(message)

    return with(context.builder) {
      this.irCall(printSourceExpr).apply {
        putValueArgument(0, irString(message))
      }
    }
  }


}