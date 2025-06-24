package io.exoquery.terpal.plugin.transform

import io.exoquery.terpal.plugin.findMethodOrFail
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import kotlin.collections.plus

class CallGlobalMethod(private val invoke: IrSimpleFunctionSymbol, private val tpe: IrType?, private val extReciever: IrExpression?) {
  companion object {
    context(BuilderContext) operator fun invoke(callable: CallableId, tpe: IrType?, extReciever: IrExpression?): CallGlobalMethod =
      CallGlobalMethod(pluginCtx.referenceFunctions(callable).first(), tpe, extReciever)
    context(BuilderContext) operator fun invoke(funPath: String, funName: String, tpe: IrType?, extReciever: IrExpression?): CallGlobalMethod =
      CallGlobalMethod(CallableId(FqName(funPath), Name.identifier(funName)), tpe, extReciever)
  }

  context(BuilderContext) operator fun invoke(vararg args: IrExpression): IrExpression {
    return with (builder) {
      val invocation = if (tpe != null) irCall(invoke, tpe) else irCall(invoke)
      val allArgs = (extReciever?.let { listOf(it) } ?: emptyList()) + args.toList()
      invocation.apply {
        for ((index, expr) in allArgs.withIndex()) {
          arguments[index] = expr
        }
      }
    }
  }
}

context(BuilderContext) fun callGlobalMethod(path: String, name: String, extReciever: IrExpression? = null) = CallGlobalMethod(path, name, null, extReciever)
context(BuilderContext) fun callGlobalMethod(callable: CallableId, extReciever: IrExpression? = null) = CallGlobalMethod(callable, null, extReciever)
fun callGlobalMethod(invoke: IrSimpleFunctionSymbol, extReciever: IrExpression? = null) = CallGlobalMethod(invoke, null, extReciever)


class CallMethod(private val host: IrExpression, private val lambdaInvoke: IrSimpleFunctionSymbol, private val tpe: IrType?) {
  companion object {
    operator fun invoke(host: IrExpression, funName: String, tpe: IrType?): CallMethod =
      CallMethod(host, host.type.findMethodOrFail(funName), tpe)
  }

  context(BuilderContext) operator fun invoke(vararg args: IrExpression): IrExpression {
    return with (builder) {
      val invocation = if (tpe != null) irCall(lambdaInvoke, tpe) else irCall(lambdaInvoke)
      val allArgs = listOf(host) + args.toList()
      invocation.apply {
        for ((index, expr) in allArgs.withIndex()) {
          arguments[index] = expr
        }
      }
    }
  }
}

fun IrExpression.callMethod(name: String) = CallMethod(this, name, null)
fun IrExpression.callMethodWithType(name: String, tpe: IrType) = CallMethod(this, name, tpe)
fun IrExpression.callMethod(function: IrSimpleFunctionSymbol) = CallMethod(this, function, null)
fun IrExpression.callMethodWithType(function: IrSimpleFunctionSymbol, tpe: IrType) = CallMethod(this, function, tpe)

class CallMethodTypedArgs(private val host: IrExpression, private val function: IrSimpleFunctionSymbol, private val tpe: IrType?) {
  companion object {
    operator fun invoke(host: IrExpression, functionName: String, tpe: IrType?): CallMethodTypedArgs =
      CallMethodTypedArgs(host, host.type.findMethodOrFail(functionName), tpe)
  }

  operator fun invoke(vararg types: IrType): CallMethodTyped =
    CallMethodTyped(host, function, types.toList(), tpe)
}

class CallMethodTyped(private val dispatchArg: IrExpression, private val function: IrSimpleFunctionSymbol, private val types: List<IrType>, private val tpe: IrType?) {
  companion object {
    operator fun invoke(host: IrExpression, functionName: String, types: List<IrType>, tpe: IrType?): CallMethodTyped =
      CallMethodTyped(host, host.type.findMethodOrFail(functionName), types, tpe)
  }

  context(BuilderContext) operator fun invoke(vararg args: IrExpression): IrExpression {
    return with (builder) {
      val invocation = if (tpe != null) irCall(function, tpe) else irCall(function)
      invocation.apply {
        val allArgs = listOf(dispatchArg) + args.toList()
        for ((index, expr) in allArgs.withIndex()) {
          arguments[index] = expr
        }
      }
    }
  }
}


fun IrExpression.callMethodTyped(name: String): CallMethodTypedArgs = CallMethodTypedArgs(this, name, null)
fun IrExpression.callMethodTyped(function: IrSimpleFunctionSymbol): CallMethodTypedArgs = CallMethodTypedArgs(this, function, null)
fun IrExpression.callMethodTypedWithType(name: String, tpe: IrType): CallMethodTypedArgs = CallMethodTypedArgs(this, name, tpe)

context (BuilderContext) fun createLambda0(functionBody: IrExpression, functionParent: IrDeclarationParent): IrFunctionExpression =
  createLambdaN(functionBody, listOf(), functionParent)

context (BuilderContext) fun createLambdaN(functionBody: IrExpression, params: List<IrValueParameter>, functionParent: IrDeclarationParent): IrFunctionExpression =
  with(builder) {
    val functionClosure = createLambdaClosure(functionBody, params, functionParent)

    val typeWith = params.map { it.type } + functionClosure.returnType
    val functionType =
      pluginCtx.symbols.functionN(params.size)
        // Remember this is FunctionN<InputA, InputB, ... Output> so these input/output args need to be both specified here
        .typeWith(typeWith)

    IrFunctionExpressionImpl(startOffset, endOffset, functionType, functionClosure, IrStatementOrigin.LAMBDA)
  }

context (BuilderContext) fun createLambdaClosure(functionBody: IrExpression, params: List<IrValueParameter>, functionParent: IrDeclarationParent): IrSimpleFunction {
  return with(pluginCtx) {
    irFactory.buildFun {
      origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
      name = SpecialNames.NO_NAME_PROVIDED
      visibility = DescriptorVisibilities.LOCAL
      returnType = functionBody.type
      modality = Modality.FINAL
      isSuspend = false
    }.apply {
      parent = functionParent

      if (params.size > 0) {
        parameters = params
      }
      /*
      VERY important here to create a new irBuilder from the symbol i.e. createIrBuilder because
      the return-point needs to be the caller-function (which kotlin gets from the irBuilder).
      If the builder in the BuilderContext is used it will return back to whatever context the
      TransformInterpolatorInvoke IrCall expression is coming from (and this will be a non-local return)
      and since the return-type is wrong it will fail with a very large error that ultimately says:
      RETURN: Incompatible return type
       */
      body = pluginCtx.createIrBuilder(symbol).run {
        // don't use expr body, coroutine codegen can't generate for it.
        irBlockBody {
          +irReturn(functionBody)
        }
      }
    }
  }
}




fun IrPluginContext.createIrBuilder(
  symbol: IrSymbol,
  startOffset: Int = UNDEFINED_OFFSET,
  endOffset: Int = UNDEFINED_OFFSET,
) = DeclarationIrBuilder(this, symbol, startOffset, endOffset)
