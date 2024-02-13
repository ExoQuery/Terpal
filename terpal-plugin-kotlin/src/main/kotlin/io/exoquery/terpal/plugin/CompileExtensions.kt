package io.exoquery.terpal.plugin

import io.decomat.fail.fail
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocationWithRange
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.isPropertyAccessor
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.*
import kotlin.reflect.KClass

val KClass<*>.qualifiedNameForce get(): String =
  if (this.qualifiedName == null) fail("Qualified name of the class ${this} was null")
  else this.qualifiedName!!

fun IrType.findMethodOrFail(methodName: String) = run {
  (this
    .classOrNull ?: error("Cannot locate the method ${methodName} from the type: ${this.dumpKotlinLike()} type is not a class."))
    .functions
    .find { it.safeName == methodName } ?: error("Cannot locate the method ${methodName} from the type: ${this.dumpKotlinLike()} because the method does not exist.")
}

fun IrClassSymbol.isDataClass() = this.owner.isData

fun IrClassSymbol.dataClassProperties() =
  if (this.isDataClass()) {
    val constructorParams = this.constructors.firstOrNull()?.owner?.valueParameters?.map { it.name }?.toSet() ?: setOf()
    this.owner.properties
      .filter { constructorParams.contains(it.name) && it.getter != null }
      .map { it.name.toString() to it.getter!!.returnType }
  }
  else sequenceOf()

val IrSymbol.safeName   get() =
  (if (owner is IrFunction && (owner as IrFunction).isPropertyAccessor) {
    (owner as IrFunction).name.asStringStripSpecialMarkers().removePrefix("get-")
  } else if (isBound) {
    (owner as? IrDeclarationWithName)?.name?.asString() ?: "<???>"
  } else {
    "<???>"
  }).replace("$", "")

fun IrElement.location(fileEntry: IrFileEntry): CompilerMessageSourceLocation {
  val irElement = this
  val sourceRangeInfo = fileEntry.getSourceRangeInfo(
    beginOffset = irElement.startOffset ?: UNDEFINED_OFFSET,
    endOffset = irElement.endOffset ?: UNDEFINED_OFFSET
  )
  val messageWithRange = CompilerMessageLocationWithRange.create(
    path = sourceRangeInfo.filePath,
    lineStart = sourceRangeInfo.startLineNumber + 1,
    columnStart = sourceRangeInfo.startColumnNumber + 1,
    lineEnd = sourceRangeInfo.endLineNumber + 1,
    columnEnd = sourceRangeInfo.endColumnNumber + 1,
    lineContent = null
  )!!
  return messageWithRange
}
