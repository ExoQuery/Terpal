plugins {
  `kotlin-dsl`
}

repositories {
    mavenCentral()
    mavenLocal()
    gradlePluginPortal()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
  implementation("io.github.gradle-nexus:publish-plugin:1.1.0")
  implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.8.20")
}