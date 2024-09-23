package io.exoquery.terpal.plugin.transform

import io.exoquery.terpal.WrapFailureMessage
import io.exoquery.terpal.plugin.*
import io.exoquery.terpal.plugin.classOrFail
import io.exoquery.terpal.plugin.isValidWrapFunction
import io.exoquery.terpal.plugin.trees.isSubclassOf
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name


fun wrapWithExceptionHandler(ctx: BuilderContext, expr: IrExpression, parent: IrDeclarationParent, spliceTermNumber: Int, totalTerms: Int): IrExpression =
  with(ctx) {
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

class WrapperMaker(val ctx: BuilderContext, val caller: IrExpression, val interpolateType: IrType) {

  // Pre-compute several values (of the interpolator package, wrapper functions available etc... so that we don't need to do that over and over again)
  private val classAnnotationsOpt by lazy { caller.type.classOrFail.owner.annotations.find { it.isSubclassOf<WrapFailureMessage>() } }
  // Find the base package that the interpolator class is defined in. I.e. that will be the FqName of the file
  private val parentPackageOpt by lazy { caller.type.classOrFail.owner.parentsCompat.toList().find { it is IrFile }?.kotlinFqName }
  // Find all the `wrap(T)` defined as extensions of the interpolator class
  private val wrapExtensionFunctions by lazy {
    parentPackageOpt?.let { parentPackage ->
      ctx.pluginCtx.referenceFunctions(CallableId(parentPackage, Name.identifier("wrap")))
    }
  }

  fun wrapInterpolatedTerm(expr: IrExpression): IrExpression =
    with(ctx) {
      val annotationMessage =
        if (classAnnotationsOpt != null) {
          val arg = classAnnotationsOpt!!.valueArguments[0]
          when {
            arg is IrConst<*> && arg.kind == IrConstKind.String -> arg.value as String
            else -> {
              ctx.logger.warn("Annotation WrapFailureMessage must have a Static-constant string argument but found: ${arg?.dumpKotlinLike()}. Will not be able to use this user-defined message in wrapping errors.")
              ""
            }
          }
        } else ""

      // Find all the `wrap(T)` functions in Interpolator class
      val dispatchWrappers = caller.type.classOrFail.functions

      val (invokeFunction, invokeCall) =
        // If there is a dispatch `wrap` function (i.e. defined directly in the interpolator class) then try to invoke that
        dispatchWrappers.find { it.isValidWrapFunction(interpolateType) && it.isWrapForExprType(expr) }?.let { dispatchFunction ->
          dispatchFunction to caller.callMethodTyped(dispatchFunction)().invoke(expr)
        } ?: run {
          val possibleExtensionWrappers = wrapExtensionFunctions?.filter {
            val extension = it.owner.extensionReceiverParameter
            // e.g. the extension reciever of the wrapper class has to be the same or a subtype of the interpolator class
            // e.g. `fun Interpolator<Foo, Bar>.wrap(value: T) = ...` would be a valid extension function
            // for some class `FooBarInterpolator: Interpolator<Foo, Bar>`
            // Typically they should be the same thing e.g. the wrapper function would be defined as
            // `fun FooBarInterpolator.wrap(value: Foo) = ...`
            val useIt = extension != null && run {
              val isSubtype = caller.type.isSubtypeOfClass(extension.type.classOrFail)
              // If `wrap` functions were found but they could not be used because the caller type was wrong we should warn the user
              if (!isSubtype) ctx.logger.warn("Found a wrap extension function in the package ${parentPackageOpt} but it's type ${caller.type.dumpKotlinLike()} is not a subtype of the extension reciever ${extension.type.dumpKotlinLike()}")
              isSubtype
            }
            useIt
          }
          // If there is no dispatch `wrap` function then try to find an extension `wrap` function
          possibleExtensionWrappers?.find { it.isValidWrapFunction(interpolateType) && it.isWrapForExprType(expr) }?.let { extensionFunction ->
            // I.e. in this case use the caller as the extension reciever
            extensionFunction to callGlobalMethod(extensionFunction, caller)(expr)
          }
        } ?: Messages.errorFailedToFindWrapper(ctx, caller, expr, interpolateType, annotationMessage)


      if (ctx.options.traceWrappers) ctx.logger.warn("==== Calling wrapper function `${invokeFunction.printInvokeFunctionSignature()}` on the expression `${invokeCall.dumpKotlinLike()}` typed as: `${expr.type.dumpKotlinLike()}`")
      return invokeCall
    }

}

fun IrSimpleFunctionSymbol.printInvokeFunctionSignature() =
  owner.dumpKotlinLike(KotlinLikeDumpOptions(bodyPrintingStrategy = BodyPrintingStrategy.NO_BODIES))
    .replace("/* fake */", "")
    .trim()