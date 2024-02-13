package io.exoquery.terpal.plugin.printing

import com.facebook.ktfmt.format.Formatter

fun format(code: String): String {
  val format =
    Formatter.GOOGLE_FORMAT.copy(
      maxWidth = 100, blockIndent = 2
    )
  val codeString = code.toString()
  val output =
    try {
      Formatter.format(format, "fun stuff() { ${codeString} }")
        .replaceFirst("fun stuff() {", "").dropLastWhile { it == '}' || it.isWhitespace() }
    } catch (e: Exception) {
      codeString
    }

  return output
}
