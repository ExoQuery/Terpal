package io.exoquery.terpal.plugin.transform

import io.decomat.Is
import io.decomat.case
import io.decomat.on
import io.exoquery.terpal.UnzipPartsParams
import io.exoquery.terpal.Interpolator
import io.exoquery.terpal.parseError
import io.exoquery.terpal.plugin.printing.dumpSimple
import io.exoquery.terpal.plugin.trees.ExtractorsDomain.Call
import io.exoquery.terpal.plugin.trees.isClassOf
import io.exoquery.terpal.plugin.trees.isSubclassOf
import io.exoquery.terpal.plugin.trees.simpleTypeArgs
import io.exoquery.terpal.plugin.trees.superTypesRecursive
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.dumpKotlinLike

class TransformInterepolatorInvoke(val ctx: BuilderContext) {
  private val compileLogger = ctx.logger

  fun matches(expression: IrCall): Boolean =
    with (compileLogger) {
      Call.InterpolateInvoke.matchesMethod(expression) || Call.InterpolatorFunctionInvoke.matchesMethod(expression)
    }

  fun transform(expression: IrCall, superTransformer: VisitTransformExpressions): IrExpression {
    val (caller, compsRaw) =
      with(compileLogger) {
        on(expression).match(
          // interpolatorSubclass.invoke(.. { stuff } ...)
          case(Call.InterpolateInvoke[Is(), Is()]).then { caller, comps ->
            caller to comps
          },
          case(Call.InterpolatorFunctionInvoke[Is(), Is()]).then { callerData, comps ->
            ctx.builder.irGetObject(callerData.interpolatorClass) to comps
          }
        )
      } ?: run {
        val bar = "\${bar}"
        parseError(
          """|======= Parsing Error =======
           |The contents of Interpolator.invoke(...) must be a single String concatenation statement e.g:
           |myInterpolator.invoke("foo $bar baz")
           |
           |==== However, the following was found: ====
           |${expression.dumpKotlinLike()}
           |======= IR: =======
           |${expression.dumpSimple()}"
        """.trimMargin()
        )
      }



    // before doing anything else need to run recursive transformations on the components because they could be
    // there could be nested interpolations e.g. stmt("foo_#{stmt("bar")}_baz")
    val comps = compsRaw.map { it.transform(superTransformer, null) }

    val parentCaller =
      caller.type.superTypesRecursive()
        .find { it.isClassOf<Interpolator<*, *>>() }
        ?: parseError("Could not isolate the parent type Interpolator<T, R>. This shuold be impossible.")

    // TODO need to catch parseError externally (i.e. in VisitTransformExpressions) & not transform the expressions

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
      UnzipPartsParams<IrExpression>({ it.isSubclassOf<String>() && it is IrConst<*> && it.kind == IrConstKind.String }, concatStringExprs, { ctx.builder.irString("") })
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


      // TODO what if it's an interpolator in an interpolator (i.e. Sql("...${Sql(...)}...") ) so need to call recursive transform
      // TODO if it's a InterpolatorWithPreProcess need to invoke the preProcess function on the Params (this is what will inoke the param & do the lift)
      //      (it should also check if the user manually created a Param with it, if so just ignore it. Should unify Param and Statement also because it should just be Sql
      //      interface which should make these things easier)

      val paramsLifted =
        with (lifter) { params.liftExprTyped(interpolateType) }

      val currScope = superTransformer.peekCurrentScope() ?: run {
        compileLogger.error(
          """|Could not find parent scope of the following expression:
             |${expression.dumpKotlinLike()}
          """.trimMargin()
        )
        return expression
      }
      val partsLiftedFun = createLambda0(partsLifted, currScope)
      val paramsLiftedFun = createLambda0(paramsLifted, currScope)

      val callOutput =
        caller.callMethodWithType("interpolate", interpolateReturn)(
          partsLiftedFun,
          paramsLiftedFun
        )

      callOutput
    }
  }
}
