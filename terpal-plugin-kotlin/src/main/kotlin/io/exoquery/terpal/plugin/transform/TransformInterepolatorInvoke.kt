package io.exoquery.terpal.plugin.transform

import io.decomat.Is
import io.decomat.case
import io.decomat.on
import io.exoquery.terpal.UnzipPartsParams
import io.exoquery.terpal.Interpolator
import io.exoquery.terpal.parseError
import io.exoquery.terpal.plugin.findMethodOrFail
import io.exoquery.terpal.plugin.qualifiedNameForce
import io.exoquery.terpal.plugin.safeName
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.superTypes

val IrCall.simpleValueArgsCount get() = this.valueArgumentsCount - this.contextReceiversCount
val IrCall.simpleValueArgs get() =
  if (this.contextReceiversCount > 0)
    this.valueArguments.drop(this.contextReceiversCount)
  else
    this.valueArguments

val IrType.simpleTypeArgs: List<IrType> get() =
  when (this) {
    is IrSimpleType ->
      this.arguments.mapNotNull { it.typeOrNull }
    else ->
      listOf()
  }

class TransformInterepolatorInvoke(val ctx: BuilderContext) {
  val className = Interpolator::class.qualifiedNameForce

  private val compileLogger = ctx.logger

  fun matches(expression: IrCall): Boolean {
    val dispatchtype =
      (expression.dispatchReceiver ?: return false).type

    return expression.symbol.safeName == "invoke" &&
      (dispatchtype.classFqName?.asString() == className ||
        dispatchtype.superTypes().any { it.classFqName.toString() == className })
  }

  fun transform(expr: IrCall, superTransformer: VisitTransformExpressions): IrExpression {
    val (caller, compsRaw) =
      expr.simpleValueArgs.first()?.let { firstArg ->
        when {
          firstArg is IrStringConcatenation ->
            expr.dispatchReceiver!! to firstArg.arguments
          else -> throw IllegalStateException("Invalid inner context: ${firstArg.dumpKotlinLike()}")
        }
      } ?: throw IllegalStateException("invalid outer context")


    // before doing anything else need to run recursive transformations on the components because they could be
    // there could be nested interpolations e.g. stmt("foo_#{stmt("bar")}_baz")
    val comps = compsRaw.map { it.transform(superTransformer, null) }

    val parentCaller =
      caller.type.superTypes()
        .find { it.classFqName.toString() == className }
        ?: parseError("Could not isolate the parent type Interpolator<T, R>. This shuold be impossible.")

    // Interpolator type T
    val interpolateType = parentCaller.simpleTypeArgs.get(0)
    val interpolateTypeClass = interpolateType.classOrNull ?: parseError("The interpolator T parameter type `${interpolateType.dumpKotlinLike()}` was not a class.")
    // Interpolator type R
    val interpolateReturn = parentCaller.simpleTypeArgs.get(1)

    val concatStringExprs =
      { a: IrExpression, b: IrExpression ->
        with (ctx) {
          ctx.builder.irString("").callMethod("plus")(a).callMethod("plus")(b)
        }
      }

    val (parts, params) =
      UnzipPartsParams<IrExpression>({ it is IrConst<*> && it.kind == IrConstKind.String }, concatStringExprs, { ctx.builder.irString("") })
        .invoke(comps)

    for ((i, comp) in params.withIndex()) {
      if (!comp.type.isSubtypeOfClass(interpolateTypeClass))
        compileLogger.error(
          """|"The #${i} interpolated block had a type of `${comp.type.dumpKotlinLike()}` (${comp.type.classFqName}) but a type `${interpolateType.dumpKotlinLike()}` (${interpolateType.classFqName}) was expected by the ${caller.type.dumpKotlinLike()} interpolator.
             |========= The faulty expression was: =========
             |${comp.dumpKotlinLike()}
          """.trimMargin()
        )
    }

    return with(ctx) {
      val lifter = makeLifter()
      val partsLifted =
        with (lifter) { parts.liftExprTyped(context.symbols.string.defaultType) }
      val paramsLifted =
        with (lifter) { params.liftExprTyped(interpolateType) }

      val runCall = caller.type.findMethodOrFail("interpolate")
      val partsLiftedFun = createLambda0(partsLifted, runCall.owner)
      val paramsLiftedFun = createLambda0(paramsLifted, runCall.owner)

      val callOutput =
        caller.callMethodWithType("interpolate", interpolateReturn)(
          partsLiftedFun,
          paramsLiftedFun
        )

      callOutput
    }
  }
}
