plugins {
  kotlin("multiplatform") version "2.1.20"

  id("maven-publish")
  id("conventions-multiplatform")
  id("publish")

  signing
  id("com.google.devtools.ksp") version "2.1.20-1.0.32"
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
