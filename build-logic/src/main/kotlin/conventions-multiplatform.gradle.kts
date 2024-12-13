import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("conventions")
  kotlin("multiplatform")
}


kotlin {
  val isCI = project.hasProperty("isCI")
  val platform =
      if (project.hasProperty("platform"))
          project.property("platform")
      else
          "any"
  val isLinux = platform == "linux"
  val isMac = platform == "mac"
  val isWindows = platform == "windows"

  jvmToolchain(11)
  val isLocalMultiplatform = project.hasProperty("isLocalMultiplatform")

  jvm {
  }

  if(isLocalMultiplatform && !isCI) {
    js {
      browser()
      nodejs()
    }
    linuxX64()
    macosX64()
    mingwX64()
  }

  // If we are a CI, build all the targets for the specified platform
  if (isLinux && isCI) {
    js {
      browser()
      nodejs()
    }

    linuxX64()
    linuxArm64()

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmWasi()
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs()

    androidNativeX64()
    androidNativeX86()
    androidNativeArm32()
    androidNativeArm64()

    // Need to know about this since we publish the -tooling metadata from
    // the linux containers. Although it doesn't build these it needs to know about them.
    macosX64()
    macosArm64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    tvosX64()
    tvosArm64()
    watchosX64()
    watchosArm32()
    watchosArm64()

    watchosDeviceArm64()
    tvosSimulatorArm64()
    watchosSimulatorArm64()

    mingwX64()
  }

  if (isMac && isCI) {
    macosX64()
    macosArm64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    tvosX64()
    tvosArm64()
    watchosX64()
    watchosArm32()
    watchosArm64()

    watchosDeviceArm64()
    tvosSimulatorArm64()
    watchosSimulatorArm64()
  }
  if (isWindows && isCI) {
    mingwX64()
  }


    sourceSets {
        commonMain {
            kotlin.srcDir("$buildDir/templates/")
            dependencies {
            }
        }

        commonTest {
            kotlin.srcDir("$buildDir/templates/")
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
    }
}

tasks.withType<AbstractTestTask>().configureEach {
    testLogging {
        showStandardStreams = true
        showExceptions = true
        exceptionFormat = TestExceptionFormat.SHORT
        events(TestLogEvent.STARTED, TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}
