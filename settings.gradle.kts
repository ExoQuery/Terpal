pluginManagement {
  includeBuild("terpal-plugin-gradle")
}

includeBuild("terpal-runtime")
includeBuild("terpal-plugin-kotlin")

include("testing")
include("terpal-sql")

rootProject.name = "terpal"
