package io.exoquery.terpal.plugin.trees

import io.exoquery.terpal.plugin.regularParams
import io.exoquery.terpal.plugin.transform.BuilderContext

import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class Lifter(val builderCtx: BuilderContext) {
  val irBuilder = builderCtx.builder
  val context = builderCtx.pluginCtx

  val listOfRef =
    context.referenceFunctions(CallableId(FqName("kotlin.collections"), Name.identifier("listOf") ))
      // Get the 1st variadic instance of listOf (note that some variations have zero args so need to do firstOrNull)
      .first { it.owner.regularParams.firstOrNull()?.isVararg ?: false }

  fun List<IrExpression>.liftExprTyped(elementType: IrType): IrExpression {
    val variadics = irBuilder.irVararg(elementType, this)
    val listOfCall = irBuilder.irCall(listOfRef, context.symbols.list.typeWith(elementType)).apply {
      typeArguments[0] = elementType
      arguments[0] = variadics
    }
    return listOfCall
  }
}
