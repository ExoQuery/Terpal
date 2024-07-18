import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    mavenLocal()
    mavenCentral()
}

group = "io.exoquery"
// Everything inherits the version from here
version = "1.9.22-0.3.0"

check("$version".isNotBlank() && version != "unspecified")
    { "invalid version $version" }
