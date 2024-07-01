package io.exoquery.terpal.plugin.transform

import io.exoquery.terpal.plugin.isValidWrapFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.functions

@OptIn(UnsafeDuringIrConstructionAPI::class)
fun wrapInterpolatedTerm(ctx: BuilderContext, caller: IrExpression, expr: IrExpression, interpolateType: IrType): IrExpression =
  with (ctx) {
    val invokeFunction =
      caller.type.classOrFail.functions.find { it.isValidWrapFunction(interpolateType) && it.owner.valueParameters.first().type.isSubtypeOfClass(expr.type.classOrFail) }
        ?: Messages.errorFailedToFindWrapper(ctx, caller, expr, interpolateType)

    val invokeCall = caller.callMethodTyped(invokeFunction).invoke().invoke(expr)
    ctx.logger.warn("============ Calling Wrap ${expr.dumpKotlinLike()} with type: ${expr.type.dumpKotlinLike()} - ${invokeCall.dumpKotlinLike()}")
    return invokeCall
  }
