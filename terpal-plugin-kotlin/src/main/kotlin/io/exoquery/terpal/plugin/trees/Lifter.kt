package io.exoquery.terpal.plugin.trees

import io.exoquery.*
import io.exoquery.terpal.plugin.logging.CompileLogger
import io.exoquery.terpal.plugin.transform.BuilderContext
import io.exoquery.terpal.plugin.transform.callMethod
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder

import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class Lifter(val builderCtx: BuilderContext) {
  val irBuilder = builderCtx.builder
  val context = builderCtx.pluginCtx

  val listOfRef =
    context.referenceFunctions(CallableId(FqName("kotlin.collections"), Name.identifier("listOf") ))
      // Get the 1st variadic instance of listOf (note that some variations have zero args so need to do firstOrNull)
      .first { it.owner.valueParameters.firstOrNull()?.isVararg ?: false }

  fun List<IrExpression>.liftExprTyped(elementType: IrType): IrExpression {
    val variadics = irBuilder.irVararg(elementType, this)
    val listOfCall = irBuilder.irCall(listOfRef, context.symbols.list.typeWith(elementType)).apply {
      putTypeArgument(0, elementType)
      putValueArgument(0, variadics)
    }
    return listOfCall
  }
}
