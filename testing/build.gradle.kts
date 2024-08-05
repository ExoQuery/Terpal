plugins {
    id("io.exoquery.terpal-plugin")
    id("conventions-multiplatform")
    kotlin("multiplatform") version "1.9.22"
}

kotlin {
    sourceSets {
        val commonMain by getting {
        }

        val commonTest by getting {
            kotlin.srcDir("$buildDir/generated/ksp/metadata/commonMain/kotlin")
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions{
        freeCompilerArgs = listOf(
            "-Xcontext-receivers",
            "-P",
            "plugin:io.exoquery.terpal-plugin:traceWrappers=true"
        )
        // Otherwise will have: Could not resolve io.exoquery:pprint-kotlin:2.0.1.
        // Incompatible because this component declares a component, compatible with Java 11 and the consumer needed a component, compatible with Java 8
        java {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
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
