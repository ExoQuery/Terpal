package io.exoquery.terpal.plugin.transform

import io.exoquery.terpal.WrapFailureMessage
import io.exoquery.terpal.plugin.Globals
import io.exoquery.terpal.plugin.isValidWrapFunction
import io.exoquery.terpal.plugin.location
import io.exoquery.terpal.plugin.trees.isSubclassOf
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.functions

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun wrapInterpolatedTerm(ctx: BuilderContext, caller: IrExpression, expr: IrExpression, interpolateType: IrType): IrExpression =
  with (ctx) {
    val classAnnotations = caller.type.classOrFail.owner.annotations.find { it.isSubclassOf<WrapFailureMessage>() }
    val annotationMessage =
      if (classAnnotations != null) {
        val arg = classAnnotations.valueArguments[0]
        when {
          arg is IrConst<*> && arg.kind == IrConstKind.String -> arg.value as String
          else -> {
            ctx.logger.warn("Annotation WrapFailureMessage must have a Static-constant string argument but found: ${arg?.dumpKotlinLike()}. Will not be able to use this user-defined message in wrapping errors.")
            ""
          }
        }
      }
      else ""

    val invokeFunction =
      caller.type.classOrFail.functions.find { it.isValidWrapFunction(interpolateType) && it.owner.valueParameters.first().type.isSubtypeOfClass(expr.type.classOrFail) }
        ?: Messages.errorFailedToFindWrapper(ctx, caller, expr, interpolateType, annotationMessage)

    val invokeCall = caller.callMethodTyped(invokeFunction).invoke().invoke(expr)
    if (Globals.logWrappers) ctx.logger.warn("==== Calling Wrap ${expr.dumpKotlinLike()} with type: ${expr.type.dumpKotlinLike()} - ${invokeCall.dumpKotlinLike()}")
    return invokeCall
  }
