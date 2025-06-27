plugins {
  `kotlin-dsl`
}

repositories {
    mavenCentral()
    mavenLocal()
    gradlePluginPortal()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.20")
  implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.9.20")
  implementation("com.gradleup.nmcp:nmcp:0.2.1")

  // Override the 1.6.1 dependency coming from kotlin-gradle-plugin
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")
}
