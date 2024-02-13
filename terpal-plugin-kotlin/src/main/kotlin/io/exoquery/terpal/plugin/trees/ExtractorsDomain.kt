package io.exoquery.terpal.plugin.trees

import io.decomat.*
import io.exoquery.terpal.Interpolator
import io.exoquery.terpal.plugin.logging.CompileLogger
import io.exoquery.terpal.plugin.qualifiedNameForce
import io.exoquery.terpal.plugin.safeName
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
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
          val caller = call.dispatchReceiver.also { if (it == null) error("Dispatch reciver of the Interpolator invocation `${call.dumpKotlinLike()}` was null. This should not be possible.") }
          if (matchesMethod(call) && caller != null) {
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
  }
}