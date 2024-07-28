pluginManagement {
  includeBuild("terpal-plugin-gradle")
}

includeBuild("terpal-runtime")
includeBuild("terpal-plugin-kotlin")

include("testing")

rootProject.name = "terpal"
