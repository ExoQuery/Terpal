plugins {
    // No inclusion of `publish` here because this project is not published to maven directly
    id("conventions")
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "1.1.0"

    id("com.github.gmazzo.buildconfig") version "3.1.0"
}

kotlin {
    jvmToolchain(8)
}

buildConfig {
    buildConfigField("String", "VERSION", "\"${project.version}\"")
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
