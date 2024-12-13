package io.exoquery.terpal.plugin.trees

import io.decomat.*
import io.exoquery.terpal.ProtoInterpolator
import io.exoquery.terpal.InterpolatorBatching
import io.exoquery.terpal.InterpolatorFunction
import io.exoquery.terpal.plugin.logging.CompileLogger
import io.exoquery.terpal.plugin.qualifiedNameForce
import io.exoquery.terpal.plugin.safeName
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.superTypes

inline fun <reified T> IrExpression.isSubclassOf(): Boolean {
  val className = T::class.qualifiedNameForce
  return className == this.type.classFqName.toString() || type.superTypesRecursive().any { it.classFqName.toString() == className }
}

fun IrType.superTypesRecursive(): List<IrType> {
  val superTypes = superTypes()
  return superTypes + superTypes.flatMap { it.superTypesRecursive() }.distinct()
}

inline fun <reified T> IrType.isSubclassOf(): Boolean {
  val className = T::class.qualifiedNameForce
  return className == this.classFqName.toString() || this.superTypesRecursive().any { it.classFqName.toString() == className }
}

inline fun <reified T> IrType.isClassOf(): Boolean {
  val className = T::class.qualifiedNameForce
  return className == this.classFqName.toString()
}

inline fun <reified T> IrCall.reciverIs(methodName: String) =
  this.dispatchReceiver?.isSubclassOf<T>() ?: false && this.symbol.safeName == methodName

object ExtractorsDomain {
  object Call {
    object InlineInjectionFunction {
      fun matchesMethod(it: IrCall): Boolean =
        it.symbol.owner.kotlinFqName.asString() == "io.exoquery.terpal.inline"

      operator fun <AP: Pattern<IrExpression>> get(reciever: AP) =
        customPattern1(reciever) { call: IrCall ->
          if (matchesMethod(call)) {
            call.simpleValueArgs.first().let { firstArg -> Components1(firstArg) }
          } else {
            null
          }
        }
    }

    object InterpolateInvoke {
      context (CompileLogger) fun matchesMethod(it: IrCall): Boolean {
        // if (it.symbol.safeName == "invoke")
        //   warn("------------ `invoke` Reciver Super Types: ${it.dispatchReceiver?.type?.superTypesRecursive()?.toList()?.map { it.dumpKotlinLike() }}")
        return it.reciverIs<ProtoInterpolator<*, *>>("invoke") //&& it.simpleValueArgsCount == 2 && it.valueArguments.all{ it != null }
      }

      context (CompileLogger) operator fun <AP: Pattern<IrExpression>, BP: Pattern<List<IrExpression>>> get(reciver: AP, terpComps: BP) =
        customPattern2(reciver, terpComps) { call: IrCall ->
          if (matchesMethod(call)) {
            val caller = call.dispatchReceiver.also { if (it == null) error("Dispatch reciver of the Interpolator invocation `${call.dumpKotlinLike()}` was null. This should not be possible.") }
            val x = call.simpleValueArgs.first()
            on(x).match(
              case(InterpolationString[Is()]).then { components ->
                Components2(caller, components)
              }
            )
          } else {
            null
          }
        }
    }

    object OptionalTrimMargin {
      context (CompileLogger) operator fun <AP : Pattern<List<IrExpression>>> get(reciver: AP) =
        customPattern1(reciver) { expr: IrExpression ->
          expr.match(
            case(Ir.Call.FunctionUntethered1[Is()]).then { components ->
              Components1(components)
            }
          )
        }
    }

    object InterpolationString {
      context (CompileLogger) operator fun <AP : Pattern<List<IrExpression>>> get(reciver: AP) =
        customPattern1(reciver) { expr: IrExpression ->
          on(expr).match(
            case(Ir.StringConcatenation[Is()]).then { components ->
              Components1(components)
            },
            case(Ir.Call.NamedExtensionFunctionZeroArg[Is("kotlin.text.trimIndent"), Ir.StringConcatenation[Is()]]).then { str, (components) ->
              Components1(components)
            },
            // it's a single string-const in this case
            case(Ir.Const[Is()]).then { const ->
              Components1(listOf(const))
            }
          )
        }
    }

    object InterpolationStringPossibleTrim {
      context (CompileLogger) operator fun <AP : Pattern<List<IrExpression>>> get(reciver: AP) =
        customPattern1(reciver) { expr: IrExpression ->
          on(expr).match(
            case(Ir.StringConcatenation[Is()]).then { components ->
              Components1(components)
            },
            case( Ir.StringConcatenation[Is()]).then { components ->
              Components1(components)
            },
            // it's a single string-const in this case
            case(Ir.Const[Is()]).then { const ->
              Components1(listOf(const))
            }
          )
        }
    }

    object InterpolateBatchingInvoke {
      context (CompileLogger) fun matchesMethod(it: IrCall): Boolean {
        // if (it.symbol.safeName == "invoke")
        //   warn("------------ `invoke` Reciver Super Types: ${it.dispatchReceiver?.type?.superTypesRecursive()?.toList()?.map { it.dumpKotlinLike() }}")
        return it.reciverIs<InterpolatorBatching<*>>("invoke") //&& it.simpleValueArgsCount == 2 && it.valueArguments.all{ it != null }
      }

      data class Data(val caller: IrExpression, val components: List<IrExpression>, val funExpr: IrFunctionExpression)

      context (CompileLogger) operator fun <AP: Pattern<Data>> get(callData: AP) =
        customPattern1(callData) { call: IrCall ->
          if (matchesMethod(call)) {
            val caller = call.dispatchReceiver ?: throw IllegalStateException("Dispatch reciver of the Interpolator invocation `${call.dumpKotlinLike()}` was null. This should not be possible.")
            val x = call.simpleValueArgs.first()
            on(x).match(
              case(Ir.FunctionExpression.withReturnOnlyBlock[InterpolationString[Is()]]).then { (components) ->
                // TODO this was only included in the latest decomat versions need to rebase-master on it
                Components1(Data(caller, components, comp))
              }
            )
          } else {
            null
          }
        }
    }





    object InterpolatorFunctionInvoke {
      val interpolatorFunctionName =
        InterpolatorFunction::class.qualifiedName ?: throw IllegalStateException("Fatal Error: InterpolatorFunction qualified name was not found")

      sealed interface SpecialReciever {
        data class Exists(val reciever: IrExpression): SpecialReciever
        data object DoesNotExist: SpecialReciever
      }

      data class Match(val interpolatorType: IrType, val interpolatorClass: IrClassSymbol, val specialReciever: SpecialReciever)

      context (CompileLogger) fun matchesMethod(call: IrCall): Boolean =
        extractComponents(call) != null

      context (CompileLogger) fun extractComponents(call: IrCall): Match? {
        val matchingAnnotationConstructor =
          call.symbol.owner.annotations.find {
            it.type.classFqName?.asString() == interpolatorFunctionName
          } ?: return null

        val (arg1, arg2) = matchingAnnotationConstructor.valueArguments.let { args ->
          if (args.size != 2) {
            error("Fatal Error: Invalid interpolation expression by `${matchingAnnotationConstructor.dumpKotlinLike()}`. The expression shuold exactly 2 arguments but found: ${args.map { it?.dumpKotlinLike() }.toList()}")
            null
          } else {
            args.get(0) to args.get(1)
          }
        } ?: return null

        // interpolatorType: E.g. StaticTerp
        // hasSpecialReciever: This denotes a case where we're using the reciever as a specialized type to pass into the interpolate function e.g. (and we're using ProtoInterpolator<In, Out> as the type)
        // For example if we would want to use a builder context with something like:
        // @InterpolatorFunction(JsonTerp::class, hasSpecialReciever = true)
        // fun JsonBuilder.json(jsonString: String): T = Messages.throwPluginNotExecuted()
        // Then it's wired to:
        // object JsonTerp: ProtoInterpolator<JsonBuilder, T> {
        //   fun JsonBuilder.interpolate(parts: () -> List<String>, params: () -> List<JsonBuilder>): T = ...
        // }
        // See an example of this in InterpolateFunctionContextTest
        val (interpolatorType, hasSpecialReciever) =
          when {
            arg1 == null -> {
              error("Fatal Error: First constructor argument null in: ${matchingAnnotationConstructor.dumpKotlinLike()}"); null
            }
            // argument StaticTerp from the KClass<StaticTerp> in the annotation constructor
            else -> {
              val firstTypeArg = arg1.type.simpleTypeArgs.first()
              when {
                // Despite the fact that the default value of the 2nd argument is null, in the Kotlin AST it will still be null since it's not specified, so we need ot return its value as false
                arg2 == null ->
                  (firstTypeArg to false)

                // When the 2nd argument is specified return its value
                arg2 is IrConst<*> && arg2.value is Boolean ->
                  (firstTypeArg to arg2.value as Boolean)

                else -> {
                  error("Fatal Error: The second argument of the annotation constructor `${matchingAnnotationConstructor.dumpKotlinLike()}` was not a boolean."); null
                }
              }
            }
          } ?: return null

        val interpolatorClassSymbol =
          interpolatorType.classOrNull ?: run {
            error("The interpolator type `${interpolatorType.dumpKotlinLike()}` (from the annotation: `${matchingAnnotationConstructor.dumpKotlinLike()}` typed as: ${matchingAnnotationConstructor.type.dumpKotlinLike()}) is not a class in: ${call.dumpKotlinLike()}")
            return null
          }

        // TODO need to test this
        if (!interpolatorClassSymbol.owner.isObject) {
          error("The interpolator type `${interpolatorType.dumpKotlinLike()}` was not an object. It can object constructed as an object.")
          return null
        }

        // The parent-type of the interpolator e.g. for StaticTerp it will be Interpolator<In, Out>
        val interpolatorImplementation =
          interpolatorType.superTypes().find {
            it.isSubclassOf<ProtoInterpolator<*, *>>()
          } ?: return null

        // The return-type of the interpolation function e.g. Out for StaticTerp (also frequently Stmt, or String)
        val interpolatorReturnType =
          interpolatorImplementation.simpleTypeArgs.getOrNull(1)
            ?: throw IllegalStateException("Fatal Exception: The interpolator type ${interpolatorImplementation.dumpKotlinLike()} has no 2nd argument (i.e. the output-type)")

        // The return-type of the interpolation function e.g. `Out`
        val returnTypeClass = call.symbol.owner.returnType.classOrNull ?: throw IllegalStateException("Fatal Error: Type type `${call.symbol.owner.returnType.dumpKotlinLike()}` is not a class.")

        // The thing that the function returns must be the same (or a subtype) of what the interpolator "inside"
        // will actualy return, otherwise it will be a class cast error
        // TODO Need to test this
        if (!interpolatorReturnType.isSubtypeOfClass(returnTypeClass))
          error("The type that the interpolation function `${call.symbol.safeName}` returns `${interpolatorReturnType.dumpKotlinLike()}` is not a subtype of `${call.symbol.owner.returnType.dumpKotlinLike()}` which the ${interpolatorType.dumpKotlinLike()} interpolator returns. This will result in a class-cast error and is therefore not allowed.")

        val extensionReceiver = call.extensionReceiver
        // either it has the form of `fun String.unaryPlus():Result` or `fun staticTerp(str: String):Result`
        // it it has a extension reciver it must be a `fun String.unaryPlus():Result`
        if (
          // If there exists a reciever, and it's not a string, and we haven't marked it specifically as a special reciever (see note above about hasSpecialReciever)
          extensionReceiver != null &&
          !(extensionReceiver.isSubclassOf<kotlin.String>()) &&
          !hasSpecialReciever
          ) {
            error("A InterpolatorFunction must be an extension reciever on a String, ${if (call.extensionReceiver == null) "but no reciver was found" else "but it was a `${call.extensionReceiver?.type?.dumpKotlinLike()}` reciver."}")
        }

        val specialReciever =
          if (hasSpecialReciever)
            SpecialReciever.Exists(extensionReceiver ?: run { error("Fatal Error: The InterpolatorFunction marked as having a special reciever but no reciever was found."); return null })
          else
            SpecialReciever.DoesNotExist

        // Otherwise it has the form of `fun staticTerp(str: String):Result`
        return Match(interpolatorType, interpolatorClassSymbol, specialReciever)
      }

      context (CompileLogger) operator fun <AP: Pattern<InterpolatorFunctionInvoke.Match>, BP: Pattern<List<IrExpression>>> get(reciver: AP, terpComps: BP) =
        customPattern2(reciver, terpComps) { call: IrCall ->
          val match = extractComponents(call)
          if (match != null) {
            val concatExpr =
              // If it's an function or operator e.g. `+"foo ${bar} baz"` then the reciver is supposed to be the string-concatenation
              // (also make sure the reciever is not a special one (i.e. a builder context))
              if (call.extensionReceiver != null && match.specialReciever !is SpecialReciever.Exists) {
                call.extensionReceiver
              } else {
                // otherwise it's an argument to a function e.g. `staticTerp("foo ${bar} baz")`
                call.simpleValueArgs.first()
              }

            on(concatExpr).match(
              case(Ir.StringConcatenation[Is()]).then { components ->
                Components2(match, components)
              },
              // it's a single string-const in this case
              case(Ir.Const[Is()]).then { const ->
                Components2(match, listOf(const))
              }
            )
          } else {
            null
          }
        }

    }
  }
}