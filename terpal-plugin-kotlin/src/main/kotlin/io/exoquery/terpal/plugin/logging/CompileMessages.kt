package io.exoquery.terpal.plugin.logging

import io.exoquery.terpal.plugin.dataClassProperties
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.*

object CompileMessages {

  fun ParserMessage(ir: IrExpression?, parsedCode: String?) =
"""
================ Parsed As: ================
$parsedCode
================ IR: ================
${ir?.dumpKotlinLike()}
================= Ast: ========================
${ir?.dump()}
""".trimIndent()


  fun PrintingMessage(ir: IrExpression?) =
"""
================ Kotlin-Like: ================
${ir?.dumpKotlinLike()}
================= IR: ========================
${ir?.dump()}
""".trimIndent()



  fun PrintingMessageMulti(ir: List<IrElement>?): String {
    fun writeOutput(ir: IrElement?): String =
      when(ir) {
        is IrReturn -> {
          val tpe = ir.value.type
          val additionalData: String =
            if (true) {
              (tpe.classOrNull?.dataClassProperties() ?: sequenceOf())
                .map { (name, value) -> "$name: ${value.dumpKotlinLike()}" }
                .joinToString(", ", "[", "]")
            } else {
              "$tpe is not a KClass"
            }
          "(Return Value): " + tpe.dumpKotlinLike() + " - " + additionalData
        }
        is IrExpression -> ir.type.dumpKotlinLike()
        else -> "No Type"
      }


return """
================ Kotlin-Like: ================
${ir?.map { it.dumpKotlinLike() }?.joinToString("\n")}
================= IR: ========================
${ir?.map { it.dump() }?.joinToString("\n")}
================= Output Type: ========================
${ir?.map { writeOutput(it) }?.joinToString("\n")}
""".trimIndent()
}


}
