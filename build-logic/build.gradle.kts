plugins {
  `kotlin-dsl`
}

repositories {
    mavenCentral()
    mavenLocal()
    gradlePluginPortal()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
  implementation("io.github.gradle-nexus:publish-plugin:1.1.0")
  implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.8.20")

  // Override the 1.6.1 dependency coming from kotlin-gradle-plugin
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}