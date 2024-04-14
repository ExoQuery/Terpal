plugins {
  kotlin("jvm") version "1.9.20" apply false
  id("io.github.gradle-nexus.publish-plugin") version "1.1.0" apply false
}

subprojects {
    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/releases")
        mavenLocal()
    }
}
