package io.exoquery.terpal

class InterpolationException(val msg: String, cause: Throwable): Exception(msg, cause)

fun <T> wrapSplice(locationPath: String, code: String, codeIsApproximate: Boolean, spliceTermNumber: Int, totalTerms: Int, splice: () -> T): T =
  try {
    splice()
  } catch (e: Exception) {

    fun longMsg() = run {
      fun codeTypeMsg() =
        if (codeIsApproximate) "looks approximately like" else "is"

      """Error in spliced expression #${spliceTermNumber} (of ${totalTerms}) at ${locationPath}
        |======= The code at this locaiton ${codeTypeMsg()}: =======
        |${code}
      """.trimMargin()
    }

    fun shortMsg() = run {
      fun codeTypeMsg() =
        if (codeIsApproximate) "(approximately looking) code" else "code"

      "Error in spliced ${codeTypeMsg()} `${code}` expression #${spliceTermNumber} (of ${totalTerms}) at ${locationPath}"
    }

    val isLong = code.contains("\n")
    val msg = if (isLong) longMsg() else shortMsg()

    throw InterpolationException(msg, e)
  }


