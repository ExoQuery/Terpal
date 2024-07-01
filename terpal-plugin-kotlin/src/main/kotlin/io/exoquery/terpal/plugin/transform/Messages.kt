package io.exoquery.terpal.plugin.transform

import io.exoquery.terpal.plugin.isValidWrapFunction
import io.exoquery.terpal.plugin.safeName
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.asSimpleType

object Messages {

  fun errorFailedToFindWrapper(ctx: BuilderContext, caller: IrExpression, expr: IrExpression, interpolateClass: IrType): Nothing {
    val validWrapFunctions =
      caller.type.classOrFail.functions.filter { it.isValidWrapFunction(interpolateClass) }.map { it.owner.valueParameters.first().type.classOrNull?.safeName }.toList()
    val invalidWrapFunctions =
      caller.type.classOrFail.functions.filter { it.safeName == "wrap" && !it.isValidWrapFunction(interpolateClass) }.map { it.owner.dumpKotlinLike() }.toList()
    val dol = '$'
    ctx.logger.error(
                  """|The interpolator ${caller.type.dumpKotlinLike()} does not have a wrap function for the type: ${expr.type.classFqName}.
                     |A "${dol}dollar-sign-variable" a  must be wrapped unless it has the type ${interpolateClass.dumpKotlinLike()}. 
                     |Wrapper functions exist for the following types: ${validWrapFunctions}
                     |The faulty expression was: ${expr.dumpKotlinLike()}
                     |-----------------------------------
                     |For a datatype that does not have a wrap-function, use the Param(...) constructor to lift it into the proper type. You may
                     |need specify a serializer for the type or (if it is contextual) ensure that it has a encoder in the `additionalEncoders` of the context.
                  """.trimMargin() +
                    // TODO The last part is context specific, need to find a way to propagate it from the compile-time
        (if (invalidWrapFunctions.size > 0)
                      "\n" +
                      """|-----------------------------------
                         |The following invalid wrap functions also exist but are invalid. A wrap-function needs to contain only one argument and return a type: ${interpolateClass.dumpKotlinLike()}
                         |${invalidWrapFunctions}
                      """.trimMargin() else "")
    )
    abortTransform()
  }

}