import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    // Do NOT enable this otherwise all kinds of horror ensues. Not exactly sure why. Maybe something in the local repo interferes with the build. with builds.
    // Note that this is also used in the publish plugin althought it is not strictly necessary for it to be there.
    //mavenLocal()
    mavenCentral()
}

group = "io.exoquery"
// Everything inherits the version from here
version = "1.9.22-1.0.0-RC1"

object Versions {
    val decomatVersion = "0.3.0"
}

check("$version".isNotBlank() && version != "unspecified")
    { "invalid version $version" }
