plugins {
    id("conventions")
    id("publish")
    kotlin("kapt") version "2.0.0"
}

kotlin {
    jvmToolchain(11)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions{
        freeCompilerArgs = listOf("-Xcontext-receivers")
        // Otherwise will have: Could not resolve io.exoquery:pprint-kotlin:2.0.1.
        // Incompatible because this component declares a component, compatible with Java 11 and the consumer needed a component, compatible with Java 8
        java {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
        // If I remove this I get:
        //  'compileJava' task (current target is 11) and 'kaptGenerateStubsKotlin' task (current target is 1.8) jvm target compatibility should be set to the same Java version.
        // Not sure why
        jvmTarget = "11"
    }
}

// Version from conventions.gradle.kts
val thisVersion = version
//println("------------------ This Version: ${thisVersion} --------------------------")

dependencies {
    // Looks like it knows to do a project-dependency even if there is a version attached (i.e. I guess it ignores the version?)
    api("io.exoquery:terpal-runtime:${thisVersion}")

    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")

    kapt("com.google.auto.service:auto-service:1.0.1")
    compileOnly("com.google.auto.service:auto-service-annotations:1.0.1")

    api("io.exoquery:decomat-core:0.2.0")
    api(kotlin("reflect"))
}
