plugins {
    id("io.exoquery.terpal-plugin")
    id("conventions-multiplatform")
    kotlin("multiplatform") version "2.2.0"
}

kotlin {
    sourceSets {
        val commonMain by getting {
        }

        val commonTest by getting {
            dependencies {
                // Used to ad-hoc some examples but not needed.
                //api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.2")
                //implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                implementation(kotlin("test"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-parameters",
            "-P",
            "plugin:io.exoquery.terpal-plugin:traceWrappers=true"
        )
        // Otherwise will have: Could not resolve io.exoquery:pprint-kotlin:2.0.1.
        // Incompatible because this component declares a component, compatible with Java 11 and the consumer needed a component, compatible with Java 8
        java {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
  // These settings are set in GradlePlugin.kt. Otherwise would need to set them here:
  //kotlinNativeCompilerPluginClasspath("io.exoquery:terpal-runtime:${...}")
  //kotlinNativeCompilerPluginClasspath("io.exoquery:decomat-core-jvm:${...}")
}
