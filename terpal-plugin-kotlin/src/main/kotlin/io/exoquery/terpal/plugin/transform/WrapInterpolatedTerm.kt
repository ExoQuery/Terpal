package io.exoquery.terpal.plugin.transform

import io.exoquery.terpal.WrapFailureMessage
import io.exoquery.terpal.plugin.Globals
import io.exoquery.terpal.plugin.classOrFail
import io.exoquery.terpal.plugin.isValidWrapFunction
import io.exoquery.terpal.plugin.location
import io.exoquery.terpal.plugin.printing.render
import io.exoquery.terpal.plugin.trees.isSubclassOf
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTry
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.functions

fun plainInterpolatedTerm(ctx: BuilderContext, expr: IrExpression, parent: IrDeclarationParent, spliceTermNumber: Int, totalTerms: Int): IrExpression =
  with (ctx) {
    // then we take that and put it into a zero-arg lambda in order to be able to make it lazy
    // () -> wrapString(person.name)
    val invokeCallLambda = createLambda0(expr, parent)

    // then we take that and pass it ot he SpliceWrapper.wrapSplice function getting:
    // wrapSplice(code = "foo ${person.name} bar", spliceTermNumber = 1, () -> wrapString(person.name))
    val code = ctx.builder.irString(expr.dumpKotlinLike())
    val termNumber = ctx.builder.irInt(spliceTermNumber)
    val loc = expr.location(ctx.currentFile.fileEntry)
    val locationPath = ctx.builder.irString("file://${loc.path}:${loc.line}:${loc.column}")
    val totalTermsExpr = ctx.builder.irInt(totalTerms)
    val wrapSpliceCall = callGlobalMethod("io.exoquery.terpal", "wrapSplice")(locationPath, code, termNumber, totalTermsExpr, invokeCallLambda)

    if (Globals.logWrappers) ctx.logger.warn("==== Calling Wrap ${expr.dumpKotlinLike()} with type: ${expr.type.dumpKotlinLike()} - ${expr.dumpKotlinLike()}")
    return wrapSpliceCall
  }


fun wrapInterpolatedTerm(ctx: BuilderContext, caller: IrExpression, expr: IrExpression, interpolateType: IrType, parent: IrDeclarationParent, spliceTermNumber: Int, totalTerms: Int): IrExpression =
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

    // Say you've got Sql("foo ${person.name} bar"), expr will be `a.b`
    // Then say you've got a wrapper function for it Sql.wrapString
    // the valid-wrap-function will be wrapString
    // then invokeCall will be wrapStirng(person.name)
    val invokeCall = caller.callMethodTyped(invokeFunction)().invoke(expr)

    // then we take that and put it into a zero-arg lambda in order to be able to make it lazy
    // () -> wrapString(person.name)
    val invokeCallLambda = createLambda0(invokeCall, parent)

    // then we take that and pass it ot he SpliceWrapper.wrapSplice function getting:
    // wrapSplice(code = "foo ${person.name} bar", spliceTermNumber = 1, () -> wrapString(person.name))
    val code = ctx.builder.irString(expr.dumpKotlinLike())
    val termNumber = ctx.builder.irInt(spliceTermNumber)
    val loc = expr.location(ctx.currentFile.fileEntry)
    val locationPath = ctx.builder.irString("file://${loc.path}:${loc.line}:${loc.column}")
    val totalTermsExpr = ctx.builder.irInt(totalTerms)
    val wrapSpliceCall = callGlobalMethod("io.exoquery.terpal", "wrapSplice")(locationPath, code, termNumber, totalTermsExpr, invokeCallLambda)

    if (Globals.logWrappers) ctx.logger.warn("==== Calling Wrap ${expr.dumpKotlinLike()} with type: ${expr.type.dumpKotlinLike()} - ${invokeCall.dumpKotlinLike()}")
    return wrapSpliceCall
  }