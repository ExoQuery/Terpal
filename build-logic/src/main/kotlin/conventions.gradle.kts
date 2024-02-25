import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    mavenLocal()
    mavenCentral()
}

plugins {
  kotlin("jvm")
}

group = "io.exoquery"
version = "1.0.2"

check("$version".isNotBlank() && version != "unspecified")
    { "invalid version $version" }

java {
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
    }
}
