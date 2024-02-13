package io.exoquery.terpal

object Messages {

val PluginWasNotExecuted =
"""
The Terpal plugin did not transform the Interpolator.invoke method into the Interpolator.interpolate method.
This typically means that the Interpolator plugin was not correct instrumented in gradle. Be sure
to add the `io.exoquery.terpal-plugin` entry into your `plugins { ... }` section in your build.gradle.kts.
================ For example: ================
// build.gradle.kts

plugins {
  id("io.exoquery.terpal-plugin")
  kotlin("jvm")
  application
}

""".trimIndent()

}