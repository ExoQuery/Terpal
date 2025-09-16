plugins {
    kotlin("jvm") version "2.2.20"

    // No inclusion of `publish` here because this project is not published to maven directly
    id("maven-publish")
    id("conventions")
    id("publish-jvm")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "1.1.0"
    id("com.github.gmazzo.buildconfig") version "3.1.0"
}

kotlin {
    jvmToolchain(11)
}

val conventionsDecomatVersion = Conventions_gradle.Versions.decomatVersion

buildConfig {
    buildConfigField("String", "VERSION", "\"${project.version}\"")
    buildConfigField("String", "DECOMAT_VERSION", "\"${conventionsDecomatVersion}\"")
}

dependencies {
    implementation(kotlin("gradle-plugin-api"))
}

gradlePlugin {
    website.set("https://github.com/exoquery/terpal")
    vcsUrl.set("https://github.com/exoquery/terpal.git")

    plugins {
        create("terpalPlugin") {
            id = "io.exoquery.terpal-plugin"
            displayName = "Terpal Plugin"
            description = "Kotlin Terpal Compiler Plugin"
            implementationClass = "io.exoquery.terpal.GradlePlugin"

            tags.set(listOf("kotlin", "terpal", "jvm"))
        }
    }
}
