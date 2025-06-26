import org.jetbrains.kotlin.konan.target.HostManager

plugins {
  kotlin("multiplatform") version "2.2.0"

  id("maven-publish")
  id("conventions-multiplatform")
  id("publish")
  id("com.gradleup.nmcp.aggregation").version("0.2.1")

  signing
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

nmcpAggregation {
  centralPortal {
    username = System.getenv("SONATYPE_USERNAME")   ?: "default_username"
    password = System.getenv("SONATYPE_PASSWORD")   ?: "default_password"
    // publish manually from the portal
    publishingType = "USER_MANAGED"
  }

  // Publish all projects that apply the 'maven-publish' plugin
  publishAllProjectsProbablyBreakingProjectIsolation()
}
