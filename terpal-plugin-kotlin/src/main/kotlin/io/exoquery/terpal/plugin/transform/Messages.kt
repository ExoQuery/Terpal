package io.exoquery.terpal.plugin.transform

import io.exoquery.terpal.plugin.classOrFail
import io.exoquery.terpal.plugin.isValidWrapFunction
import io.exoquery.terpal.plugin.regularParams
import io.exoquery.terpal.plugin.safeName
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.functions

fun String.trimLeft() = this.dropWhile { it.isWhitespace() }

object Messages {
  fun errorFailedToFindWrapper(ctx: BuilderContext, caller: IrExpression, expr: IrExpression, interpolateClass: IrType, userSpecificError: String): Nothing {
    val validWrapFunctions =
      caller.type.classOrFail.functions.filter { it.isValidWrapFunction(interpolateClass) }.map { it.owner.regularParams.first().type.classOrNull?.safeName }.toList()
    val invalidWrapFunctions =
      caller.type.classOrFail.functions.filter { it.safeName == "wrap" && !it.isValidWrapFunction(interpolateClass) }.map { it.owner.dumpKotlinLike() }.toList()
    val dol = '$'
    val error =
"""
The interpolator ${caller.type.dumpKotlinLike()} does not have a wrap function for the type: ${expr.type.classFqName}.
A "${dol}dollar-sign-variable" a  must be wrapped unless it has the type ${interpolateClass.dumpKotlinLike()}. 
Wrapper functions exist for the following types: ${validWrapFunctions}
The faulty expression was: ${expr.dumpKotlinLike()}
""".trimLeft() +

(if (!userSpecificError.isEmpty())
"""
-----------------------------------
${userSpecificError}
""".trimLeft()
else ""
) +

(if (invalidWrapFunctions.size > 0)
"""
-----------------------------------
The following invalid wrap functions also exist but are invalid. A wrap-function needs to contain only one argument and return a type: ${interpolateClass.dumpKotlinLike()}
${invalidWrapFunctions}
""".trimLeft()
else ""
)

    ctx.logger.error(error)
    abortTransform()
  }

  fun errorFailedToInterpolator(ctx: BuilderContext, caller: IrExpression, expr: IrExpression): Nothing {
    val error =
"""
The interpolator ${caller.type.dumpKotlinLike()} does not have a `inlined(String)` function so the following expression is invalid:
${expr.dumpKotlinLike()}
""".trimLeft()

    ctx.logger.error(error)
    abortTransform()
  }

}
