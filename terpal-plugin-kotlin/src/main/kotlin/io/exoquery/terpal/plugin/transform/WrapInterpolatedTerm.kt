package io.exoquery.terpal.plugin.transform

import io.exoquery.terpal.StrictType
import io.exoquery.terpal.WrapFailureMessage
import io.exoquery.terpal.plugin.classOrFail
import io.exoquery.terpal.plugin.isValidWrapFunction
import io.exoquery.terpal.plugin.location
import io.exoquery.terpal.plugin.source
import io.exoquery.terpal.plugin.trees.isSubclassOf
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*

fun wrapWithExceptionHandler(ctx: BuilderContext, expr: IrExpression, parent: IrDeclarationParent, spliceTermNumber: Int, totalTerms: Int): IrExpression =
  with (ctx) {
    // then we take that and put it into a zero-arg lambda in order to be able to make it lazy
    // () -> wrapString(person.name)
    val invokeCallLambda = createLambda0(expr, parent)

    val (code, isApproximate) =
      with(ctx) { expr.source }?.let { codeStr -> codeStr to false } ?: expr.dumpKotlinLike() to true

    val codeExpr = ctx.builder.irString(code)
    val codeIsApproximateExpr = ctx.builder.irBoolean(isApproximate)
    val termNumber = ctx.builder.irInt(spliceTermNumber + 1)
    val loc = expr.location(ctx.currentFile.fileEntry)
    val locationPath = ctx.builder.irString("file://${loc.path}:${loc.line}:${loc.column}")
    val totalTermsExpr = ctx.builder.irInt(totalTerms)
    return callGlobalMethod("io.exoquery.terpal", "wrapSplice")(locationPath, codeExpr, codeIsApproximateExpr, termNumber, totalTermsExpr, invokeCallLambda)
  }


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

    fun IrSimpleFunctionSymbol.isWrapForExprType(): Boolean {
      val func = this
      val isStrict = func.owner.annotations.any { it.isSubclassOf<StrictType>() }
      val firstParamType = func.owner.valueParameters.first().type
      return if (isStrict) {
        // If strict, just compare the types independently of nullability
        firstParamType.makeNullable() == expr.type.makeNullable()
      } else {
        // Say we've we're wrapping: Sql("... ${expr:FirstName} ...")
        // where `data class FirstName(...): Name { ... }`
        // we want a wrap function that is either Sql.wrap(value:FirstName) or Sql.wrap(value:Name)
        // in the 2nd case, we want the `expr` to be sub-type of the Sql.wrap value (i.e. contravariance).
        expr.type.isSubtypeOfClass(firstParamType.classOrFail)
      }
    }

    val invokeFunction =
      caller.type.classOrFail.functions.find { it.isValidWrapFunction(interpolateType) && it.isWrapForExprType() }
        ?: Messages.errorFailedToFindWrapper(ctx, caller, expr, interpolateType, annotationMessage)

    val invokeCall = caller.callMethodTyped(invokeFunction)().invoke(expr)

    if (ctx.options.traceWrappers) ctx.logger.warn("==== Calling wrapper function `${invokeFunction.printInvokeFunctionSignature()}` on the expression `${invokeCall.dumpKotlinLike()}` typed as: `${expr.type.dumpKotlinLike()}`")
    return invokeCall
  }

fun IrSimpleFunctionSymbol.printInvokeFunctionSignature() =
  owner.dumpKotlinLike(KotlinLikeDumpOptions(bodyPrintingStrategy = BodyPrintingStrategy.NO_BODIES))
    .replace("/* fake */", "")
    .trim()

