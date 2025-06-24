package io.exoquery.terpal.plugin.transform

import io.exoquery.terpal.InterpolatorBackend
import io.exoquery.terpal.StrictType
import io.exoquery.terpal.plugin.classOrFail
import io.exoquery.terpal.plugin.regularParams
import io.exoquery.terpal.plugin.trees.isSubclassOf
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.dumpKotlinLike

/**
 * Is a function annotated with [StrictType] and is it a wrap function for the given expression?
 * (i.e. does it take an expression of the same type as its first parameter)
 */
fun IrSimpleFunctionSymbol.isWrapForExprType(expr: IrExpression): Boolean {
  val func = this
  val isStrict = func.owner.annotations.any { it.isSubclassOf<StrictType>() }
  val firstParamType = func.owner.regularParams.first().type
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

tailrec fun IrSimpleFunctionSymbol.isInterpolatorBackend(maxRecursions: Int = 1000): Boolean =
  // Does the function directly have a @InterpolatorBackend annotation?
  owner.annotations.any { it.isSubclassOf<InterpolatorBackend>() } ||
    // Or is it an override of a function that has a @InterpolatorBackend annotation?
    // (NOTE Need to be careful about this because the overridden function might not be a backend
    // I am not sure if there are cases where this can infinitely recurse so adding a maxLookups)
    owner.overriddenSymbols.any {
      if (maxRecursions < 0) {
        error("Too many lookps for overridden function: ${this.owner.dumpKotlinLike()}")
        return false
      }
      it.isInterpolatorBackend(maxRecursions - 1)
    }
