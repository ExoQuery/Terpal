@file:Suppress("NAME_SHADOWING", "NAME_SHADOWING")

package io.exoquery.terpal.plugin.trees

import io.decomat.*
import io.exoquery.terpal.plugin.logging.CompileLogger
import io.exoquery.terpal.parseError
import io.exoquery.terpal.plugin.dispatchArg
import io.exoquery.terpal.plugin.extensionArg
import io.exoquery.terpal.plugin.regularArgs
import io.exoquery.terpal.plugin.regularParams
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.kotlinFqName

val IrType.simpleTypeArgs: List<IrType> get() =
  when (this) {
    is IrSimpleType ->
      this.arguments.mapNotNull { it.typeOrNull }
    else ->
      listOf()
  }

object Ir {
  object StringConcatenation {
    // Can't do just get(components: Pattern<List<IrExpression>) need to do:
    // <AP: Pattern<List<IrExpression>>> get(components: AP) or it doesn't work because
    // it needs to have a concrete pattern instance
    context(_: CompileLogger) operator fun <AP: Pattern<List<IrExpression>>> get(components: AP) =
      customPattern1(components) { it: IrExpression ->
        if (it is IrStringConcatenation) {
          Components1(it.arguments)
        } else {
          null
        }
      }
  }

  object Const {
    operator fun get(value: Pattern0<IrConst>) =
      customPattern1(value) { it: IrConst ->
        Components1(it)
      }
  }

  object Call {
    // not a function on an object or class i.e. top-level
    object FunctionUntethered1 {
      context (_: CompileLogger) operator fun <AP : Pattern<E>, E: IrExpression> get(x: AP): Pattern1<AP, E, IrCall> =
        customPattern1(x) { it: IrCall ->
          val reciever = it.extensionArg?: it.dispatchArg
          if (reciever == null && it.regularArgs.size == 1 && it.regularArgs.all { it != null }) {
            Components1(it.regularArgs.first())
          } else {
            null
          }
        }
    }

    object NamedExtensionFunctionZeroArg {
      context (_: CompileLogger) operator fun <AP : Pattern<String>, BP : Pattern<E>, E: IrExpression> get(name: AP, reciever: BP): Pattern2<AP, BP, String, E, IrCall> =
        customPattern2(name, reciever) { it: IrCall ->
          val reciever = it.extensionArg
          if (reciever != null && it.regularArgs.size == 0) {
            Components2(it.symbol.owner.kotlinFqName.asString(), reciever)
          } else {
            null
          }
        }
    }
  }




  object BlockBody {
    // Need to use type-parameter like this or matching (e.g. in SimpleBlockBody) won't type correctly
    operator fun <AP: Pattern<A>, A: List<S>, S: IrStatement> get(statements: AP) =
      customPattern1(statements) { it: IrBlockBody -> Components1(it.statements.toList()) }

    object ReturnOnly {
      operator fun <AP: Pattern<A>, A: IrExpression> get(statements: AP) =
        customPattern1(statements) { it: IrBlockBody ->
          on(it).match(
            case(BlockBody[Is()]).then { (irReturn) -> Components1(irReturn) }
          )
        }
    }

    object Statements {
      operator fun <AP: Pattern<A>, A: List<IrStatement>> get(statements: AP) =
        customPattern1(statements) { it: IrBlockBody ->
          on(it).match(
            case(BlockBody[Is()]).then { statements -> Components1(statements) }
          )
        }
    }
  }

  object Return {
    operator fun <AP: Pattern<A>, A: IrExpression> get(statements: AP) =
      customPattern1(statements) { it: IrReturn -> Components1(it.value) }
  }

  /** I.e. a Lambda! */
  object FunctionExpression {
    operator fun <AP: Pattern<A>, A: IrSimpleFunction> get(body: AP)  /*: Pattern1<AP, A, IrFunctionExpression>*/ =
      // Note, each one of these in here needs to be it:IrExression, not it:IrFunctionExpression or they will never match arbitrary IrExpression instances
      customPattern1(body) { it: IrFunctionExpression ->
        Components1(it.function)
      }

    object withBlockStatements {
      operator fun <AP: Pattern<List<IrValueParameter>>, BP: Pattern<List<IrStatement>>> get(params: AP, body: BP) =
        customPattern2(params, body) { it: IrFunctionExpression ->
          on(it).match(
            case(FunctionExpression[SimpleFunction.withBlockStatements[Is(), Is()]])
              .then { (params, statements) -> Components2(params, statements) }
          )
        }
    }

    object withReturnOnlyBlock {
      operator fun <AP: Pattern<A>, A: IrExpression> get(body: AP) =
        customPattern1(body) { it: IrFunctionExpression ->
          on(it).match(
            case(FunctionExpression[SimpleFunction.withReturnOnlyExpression[Is()]]).then { (expr) ->
              on(expr).match(
                // output the return-body
                case(Return[Is()]).then { returnExpr -> Components1(returnExpr) }
              )
            }
          )
        }
    }
  }

  object SimpleFunction {
    operator fun <AP: Pattern<A>, BP: Pattern<B>, A: List<IrValueParameter>, B: IrBlockBody> get(args: AP, body: BP): Pattern2<AP, BP, A, B, IrSimpleFunction> =
      customPattern2(args, body) { it: IrSimpleFunction ->
        it.body?.let { bodyVal ->
          when (val body = it.body) {
            // Ignore context-parameters here
            is IrBlockBody -> Components2(it.regularParams, body)
            else -> parseError("The function ${it.name} body was not a blockBody")
          }

        }
      }

    object withBlockStatements {
      operator fun <AP: Pattern<List<IrValueParameter>>, BP: Pattern<List<IrStatement>>> get(params: AP, body: BP) =
        customPattern2(params, body) { it: IrSimpleFunction ->
          on(it).match(
            case(SimpleFunction[Is(), BlockBody.Statements[Is()]]).then { params, (b) ->
              Components2(params, b)
            }
          )
        }
    }

    object withReturnOnlyExpression    {
      operator fun <AP: Pattern<A>, A: IrExpression> get(body: AP) =
        customPattern1(body) { it: IrSimpleFunction ->
          on(it).match(
            case(SimpleFunction[Is(), BlockBody.ReturnOnly[Is()]]).then { _, (b) ->
              Components1(b)
            }
          )
        }
    }
  }

}
