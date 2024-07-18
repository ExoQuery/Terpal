plugins {
  id("io.github.gradle-nexus.publish-plugin") version "1.1.0" apply false
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/releases")
        mavenLocal()
    }
}

//repositories {
//    mavenCentral()
//}