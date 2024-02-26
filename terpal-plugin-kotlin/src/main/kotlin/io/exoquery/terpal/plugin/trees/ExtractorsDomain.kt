package io.exoquery.terpal.plugin.trees

import io.decomat.*
import io.exoquery.terpal.Interpolator
import io.exoquery.terpal.InterpolatorFunction
import io.exoquery.terpal.plugin.logging.CompileLogger
import io.exoquery.terpal.plugin.qualifiedNameForce
import io.exoquery.terpal.plugin.safeName
import org.jetbrains.kotlin.ir.backend.js.utils.asString
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.isClass
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.superTypes

inline fun <reified T> IrExpression.isClass(): Boolean {
  val className = T::class.qualifiedNameForce
  return className == this.type.classFqName.toString() || type.superTypes().any { it.classFqName.toString() == className }
}

inline fun <reified T> IrType.isClass(): Boolean {
  val className = T::class.qualifiedNameForce
  return className == this.classFqName.toString() || this.superTypes().any { it.classFqName.toString() == className }
}

inline fun <reified T> IrCall.reciverIs(methodName: String) =
  this.dispatchReceiver?.isClass<T>() ?: false && this.symbol.safeName == methodName

object ExtractorsDomain {
  object Call {
    object InterpolateInvoke {
      context (CompileLogger) fun matchesMethod(it: IrCall): Boolean =
        it.reciverIs<Interpolator<*, *>>("invoke") //&& it.simpleValueArgsCount == 2 && it.valueArguments.all{ it != null }

      context (CompileLogger) operator fun <AP: Pattern<IrExpression>, BP: Pattern<List<IrExpression>>> get(reciver: AP, terpComps: BP) =
        customPattern2(reciver, terpComps) { call: IrCall ->
          if (matchesMethod(call)) {
            val caller = call.dispatchReceiver.also { if (it == null) error("Dispatch reciver of the Interpolator invocation `${call.dumpKotlinLike()}` was null. This should not be possible.") }
            val x = call.simpleValueArgs.first()
            on(x).match(
              case(Ir.StringConcatenation[Is()]).then { components ->
                Components2(caller, components)
              },
              // it's a single string-const in this case
              case(Ir.Const[Is()]).then { const ->
                Components2(caller, listOf(const))
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

      data class Match(val interpolatorType: IrType, val interpolatorClass: IrClassSymbol)

      context (CompileLogger) fun matchesMethod(call: IrCall): Boolean =
        extractComponents(call) != null

      context (CompileLogger) fun extractComponents(call: IrCall): Match? {
        val matchingAnnotationClass =
          call.symbol.owner.annotations.find {
            it.type.classFqName?.asString() == interpolatorFunctionName
          } ?: return null

        // E.g. StaticTerp
        val interpolatorType =
          matchingAnnotationClass.type.simpleTypeArgs.let { typeArgs ->
            if (typeArgs.size != 1) {
              error("Fatal Error: Invalid interpolation expression by `${matchingAnnotationClass.dumpKotlinLike()}`. The expression shuold have only one type arg but found: ${typeArgs.map { it.asString() }.toList()}")
              null
            } else
              typeArgs.first()
          } ?: return null

        val interpolatorClassSymbol =
          interpolatorType.classOrNull ?: run {
            error("The interpolator type `${interpolatorType.asString()}` (from the annotation: ${matchingAnnotationClass.type.asString()}) is not a class")
            return null
          }

        // TODO need to test this
        if (!interpolatorClassSymbol.owner.isObject) {
          error("The interpolator type `${interpolatorType.asString()}` was not an object. It can object constructed as an object.")
          return null
        }

        // The parent-type of the interpolator e.g. for StaticTerp it will be Interpolator<In, Out>
        val interpolatorImplementation =
          interpolatorType.superTypes().find {
            it.isClass<Interpolator<*, *>>()
          } ?: return null

        // The return-type of the interpolation function e.g. Out for StaticTerp (also frequently Stmt, or String)
        val interpolatorReturnType =
          interpolatorImplementation.simpleTypeArgs.getOrNull(1)
            ?: throw IllegalStateException("Fatal Exception: The interpolator type ${interpolatorImplementation.asString()} has no 2nd argument (i.e. the output-type)")

        // The return-type of the interpolation function e.g. `Out`
        val returnTypeClass = call.symbol.owner.returnType.classOrNull ?: throw IllegalStateException("Fatal Error: Type type `${call.symbol.owner.returnType.asString()}` is not a class.")

        // The thing that the function returns must be the same (or a subtype) of what the interpolator "inside"
        // will actualy return, otherwise it will be a class cast error
        // TODO Need to test this
        if (!interpolatorReturnType.isSubtypeOfClass(returnTypeClass))
          error("The type that the interpolation function `${call.symbol.safeName}` returns `${interpolatorReturnType.asString()}` is not a subtype of `${call.symbol.owner.returnType.asString()}` which the ${interpolatorType.asString()} interpolator returns. This will result in a class-cast error and is therefore not allowed.")

        if (!(call.extensionReceiver?.isClass<kotlin.String>() ?: false))
          error("A InterpolatorFunction must be an extension reciever on a String, ${if (call.extensionReceiver == null) "but no reciver was found" else "but it was a `${call.extensionReceiver?.type?.asString()}` reciver."}")

        return Match(interpolatorType, interpolatorClassSymbol)
      }

      context (CompileLogger) operator fun <AP: Pattern<InterpolatorFunctionInvoke.Match>, BP: Pattern<List<IrExpression>>> get(reciver: AP, terpComps: BP) =
        customPattern2(reciver, terpComps) { call: IrCall ->
          val match = extractComponents(call)
          if (match != null) {
            //error("------------ Is Operator: ${call.symbol.owner.isOperator}")

            // If it's an function or operator e.g. `+"foo ${bar} baz"` then the reciver is supposed to be the string-concatenation
            val concatExpr = call.extensionReceiver
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