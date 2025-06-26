pluginManagement {
  includeBuild("terpal-plugin-gradle")
  includeBuild("build-logic")

  pluginManagement {
    repositories {
      mavenCentral()
      google()
      gradlePluginPortal()
      mavenLocal()
    }
  }
}

includeBuild("terpal-runtime")
includeBuild("terpal-plugin-kotlin")

include("testing")

rootProject.name = "terpal"
