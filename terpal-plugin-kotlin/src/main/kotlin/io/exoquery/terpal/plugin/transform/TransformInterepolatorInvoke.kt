package io.exoquery.terpal.plugin.transform

import io.decomat.Is
import io.decomat.case
import io.decomat.match
import io.decomat.on
import io.exoquery.terpal.*
import io.exoquery.terpal.plugin.classOrFail
import io.exoquery.terpal.plugin.logging.CompileLogger
import io.exoquery.terpal.plugin.trees.ExtractorsDomain.Call
import io.exoquery.terpal.plugin.trees.isClassOf
import io.exoquery.terpal.plugin.trees.isSubclassOf
import io.exoquery.terpal.plugin.trees.simpleTypeArgs
import io.exoquery.terpal.plugin.trees.superTypesRecursive
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.functions

class TransformInterepolatorInvoke(val ctx: BuilderContext) {
  private val compileLogger = ctx.logger

  fun matches(expression: IrCall): Boolean =
    with (compileLogger) {
      Call.InterpolateInvoke.matchesMethod(expression) || Call.InterpolatorFunctionInvoke.matchesMethod(expression)
    }

  fun transform(expression: IrCall, superTransformer: VisitTransformExpressions): IrExpression {
    val (caller, compsRaw, specialReciever) =
      with(compileLogger) {
        on(expression).match(
          // interpolatorSubclass.invoke(.. { stuff } ...)
          case(Call.InterpolateInvoke[Is(), Is()]).then { caller, comps ->
            Triple(caller, comps, Call.InterpolatorFunctionInvoke.SpecialReciever.DoesNotExist)
          },
          case(Call.InterpolatorFunctionInvoke[Is(), Is()]).then { callerData, comps ->
            Triple(ctx.builder.irGetObject(callerData.interpolatorClass), comps, callerData.specialReciever)
          }
        )
      } ?: run {
        val dol = '$'
        parseError(
          """|======= Parsing Error =======
           |The contents of Interpolator.invoke(...) must be a single String concatenation statement e.g:
           |myInterpolator.invoke("foo $dol{bar} baz")
           |
           |==== However, the following was found: ====
           |${expression.dumpKotlinLike()}
           |======= IR: =======
           |${expression.dump()}"
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
      superTypes.find { it.isClassOf<InterpolatorWithWrapper<*, *>>() }
        ?: superTypes.find { it.isClassOf<Interpolator<*, *>>() }
        ?: superTypes.find { it.isClassOf<ProtoInterpolator<*, *>>() }
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

    val (parts, paramsRaw) =
      UnzipPartsParams<IrExpression>({ it.isSubclassOf<String>() && it is IrConst && it.kind == IrConstKind.String }, concatStringExprs, { ctx.builder.irString("") })
        .invoke(comps)

    return with(ctx) {

      val currScope = superTransformer.peekCurrentScope() ?: run {
        compileLogger.error(
          """|Could not find parent scope of the following expression:
             |${expression.dumpKotlinLike()}
          """.trimMargin()
        )
        return expression
      }

      val parametersWrapper = ParametersWrapper(
        interpolateTypeClass,
        interpolateType,
        ctx,
        caller,
        compileLogger,
        currScope,
        { type -> type.isSubclassOf<InterpolatorWithWrapper<*, *>>() }
      )

      // Wrap the parameters either in the correct wrapper-function from the Interpolator or an infix-wrapper or skip wrapping if it is not a subtype of InterpolatorWithWrapper
      val params = parametersWrapper.wrapParams(paramsRaw)

      val lifter = makeLifter()
      val partsLifted =
        with (lifter) { parts.liftExprTyped(context.symbols.string.defaultType) }

      val paramsLifted =
        with (lifter) { params.liftExprTyped(interpolateType) }


      val partsLiftedFun = createLambda0(partsLifted, currScope)
      val paramsLiftedFun = createLambda0(paramsLifted, currScope)

      val interpolatorBackend =
        caller.type.classOrFail.functions.find { it.isInterpolatorBackend() }
          ?: parseError("Could not find the interpolator backend function (i.e. a function annotated with @InterpolatorBackend) in the interpolator class: ${caller.type.dumpKotlinLike()}")

      val callOutput =
        when (specialReciever) {
          // In a normal case just put in parts and params
          is Call.InterpolatorFunctionInvoke.SpecialReciever.DoesNotExist ->
            caller.callMethodWithType(interpolatorBackend, interpolateReturn)(
              partsLiftedFun,
              paramsLiftedFun
            )
          // If there is a special reciever then treat that as a third parameter
          is Call.InterpolatorFunctionInvoke.SpecialReciever.Exists ->
            caller.callMethodWithType(interpolatorBackend, interpolateReturn)(
              partsLiftedFun,
              paramsLiftedFun,
              specialReciever.reciever
            )
        }

      callOutput
    }
  }
}

class ParametersWrapper(
  private val interpolateTypeClass: IrClassSymbol,
  private val interpolateType: IrType,
  private val ctx: BuilderContext,
  private val caller: IrExpression,
  private val compileLogger: CompileLogger,
  private val currScope: IrDeclarationParent,
  private val isInterpolatorCorrect: (IrType) -> Boolean
) {
  fun wrapParams(params: List<IrExpression>) =
    params.withIndex().map { (i, comp) ->
      wrapParameter(comp, i, params.size)
    }

  // Create the factory that will wrap interpolated terms (if needed). There are some initialization steps in here but they are lazy.
  // this class should not do anything if there is nothing to wrap.
  private val wrapper = WrapperMaker(ctx, caller, interpolateType)

  // Put together an invocation call that would need to be used for the wrapper function (if it exists)
  private val wrapperFunctionInvoke: ((IrExpression, Int) -> IrExpression)? = run {
    if (isInterpolatorCorrect(caller.type))
      { expr: IrExpression, termIndex: Int -> wrapper.wrapInterpolatedTerm(expr) }
    else
      null
  }

  private fun wrapParameter(comp: IrExpression, i: Int, totalParams: Int): IrExpression {
    val possiblyWrappedParam =
      // If the component is already the correct type then just return it
      if (comp.type.classOrFail.isSubtypeOfClass(interpolateTypeClass)) {
        comp
      } else {
        // If it is a string directly passed via the `inject` function then just return it
        comp.match(
          case(Call.InlineInjectionFunction[Is()]).then { inlineArg ->
            if (inlineArg is IrConst && inlineArg.kind == IrConstKind.String) {
              val dol = '$'
              compileLogger.error(
                """Found a constant-string passed to an invocation of `inline(...)`. Do not do that.
                        |If you want to actually use a static string here, use it directly e.g. "foo$dol{inline("bar")}baz" -> "foobarbaz"
                        |If you want to use a static string here then write it into a variable first e.g. val bar = "bar"; "foo$dol{bar}baz"
                      """.trimMargin()
              )
            }
            wrapper.wrapInlineTerm(inlineArg)
          }
        ) ?: run {
          // Otherwise we need to try to get a wrapper function for the component
          if (wrapperFunctionInvoke != null) {
            wrapperFunctionInvoke.invoke(comp, i)
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
      }

    return wrapWithExceptionHandler(ctx, possiblyWrappedParam, currScope, i, totalParams)
  }
}
