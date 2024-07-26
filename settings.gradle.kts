pluginManagement {
  includeBuild("terpal-plugin-gradle")

  //resolutionStrategy {
  //  eachPlugin {
  //    if (requested.id.id == "io.exoquery.terpal-plugin") {
  //      useModule("io.exoquery:terpal-runtime:1.9.22-0.3.7")
  //    }
  //  }
  //}
}

includeBuild("terpal-runtime")
includeBuild("terpal-plugin-kotlin")

include("testing")

rootProject.name = "terpal"
