pluginManagement {
  includeBuild("terpal-plugin-gradle")
  includeBuild("build-logic")
}

includeBuild("terpal-runtime")
includeBuild("terpal-plugin-kotlin")

include("testing")

rootProject.name = "terpal"
