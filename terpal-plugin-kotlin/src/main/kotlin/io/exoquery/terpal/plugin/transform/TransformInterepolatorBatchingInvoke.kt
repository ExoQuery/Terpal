package io.exoquery.terpal.plugin.transform

import io.decomat.Is
import io.decomat.case
import io.decomat.on
import io.exoquery.terpal.*
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
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.dumpKotlinLike

class TransformInterepolatorBatchingInvoke(val ctx: BuilderContext) {
  private val compileLogger = ctx.logger

  fun matches(expression: IrCall): Boolean =
    with (compileLogger) {
      Call.InterpolateBatchingInvoke.matchesMethod(expression)
    }

  fun transform(expression: IrCall, superTransformer: VisitTransformExpressions): IrExpression {
    val (caller, compsRaw, funExpr) =
      with(compileLogger) {
        on(expression).match(
          // interpolatorSubclass.invoke(.. { stuff } ...)
          case(Call.InterpolateBatchingInvoke[Is()]).then { data ->
            data
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

    val superTypes = caller.type.superTypesRecursive()

    val parentCaller =
      // Need to find the most specific interpolator implementation because the less-specific one will have non-generics for their
      // parameter specifications. E.g. if some interpolator e.g. Sql:Interpolator<Fragment, Statement> which implements
      // Interpolator is looked up as (parentCaller:Interpolator).simpleTypeArgs you will get back T,R instead of Fragment,Statement.
      superTypes.find { it.isClassOf<InterpolatorBatchingWithWrapper<*>>() }
        ?: superTypes.find { it.isClassOf<InterpolatorBatching<*>>() }
        ?: parseError("Could not isolate the parent type Interpolator<T, R>. This shuold be impossible.")

    // TODO need to catch parseError externally (i.e. in VisitTransformExpressions) & not transform the expressions

    // InterpolatorBatching type T
    val interpolateType = parentCaller.simpleTypeArgs.get(0)
    val interpolateTypeClass = interpolateType.classOrNull ?: parseError("The interpolator T parameter type `${interpolateType.dumpKotlinLike()}` was not a class.")
    // There is not type R for InterpolateBatch so we use the output-type of the interpolate/invoke function
    val interpolateReturn = expression.type

    val concatStringExprs =
      { a: IrExpression, b: IrExpression ->
        with (ctx) {
          ctx.builder.irString("").callMethod("plus")(a).callMethod("plus")(b)
        }
      }

    val (parts, paramsRaw) =
      UnzipPartsParams<IrExpression>({ it.isSubclassOf<String>() && it is IrConst<*> && it.kind == IrConstKind.String }, concatStringExprs, { ctx.builder.irString("") })
        .invoke(comps)

    return with(ctx) {

      // Put together an invocation call that would need to be used for the wrapper function (if it exists)
      val wrapperFunctionInvoke = run {
        val isInterpolatorWithWrapper =
          caller.type.superTypesRecursive()
            .find { it.isClassOf<InterpolatorWithWrapper<*, *>>() } != null

        if (isInterpolatorWithWrapper) {
          { expr: IrExpression -> caller.callMethod("wrap")(expr) }
        } else
          null
      }

      val params =
        paramsRaw.withIndex().map { (i, comp) ->
          if (comp.type.classOrFail.isSubtypeOfClass(interpolateTypeClass)) {
            comp
          } else if (wrapperFunctionInvoke != null) {
            wrapperFunctionInvoke(comp)
          } else {
            compileLogger.error(
              """|"The #${i} interpolated block had a type of `${comp.type.dumpKotlinLike()}` (${comp.type.classFqName}) but a type `${interpolateType.dumpKotlinLike()}` (${interpolateType.classFqName}) was expected by the ${caller.type.dumpKotlinLike()} interpolator.
                 |(Also no wrapper function has been defined because `${caller.type.classFqName}` is not a subtype of InterpolatorWithWrapper)
                 |========= The faulty expression was: =========
                 |${comp.dumpKotlinLike()}
              """.trimMargin()
            )
            // Return the param so logic can continue. In reality a class-cast-exception would happen (because the "... $component..." is not the required type and there's not wrapper function).
            comp
          }
        }

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
      val paramsLiftedFun = createLambdaN(paramsLifted, funExpr.function.valueParameters, currScope)



      val callOutput =
        caller.callMethodWithType("interpolate", interpolateReturn)(
          partsLiftedFun,
          paramsLiftedFun
        )

      //error("---------- Calling: \n ---------\n${callOutput.dumpKotlinLike()}")

      callOutput
    }
  }
}
