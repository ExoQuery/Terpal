package io.exoquery.terpal.plugin.transform

class AbortTransform() : Exception("Transformation aborted")
fun abortTransform(): Nothing = throw AbortTransform()
