import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.20"
    id("maven-publish")
    id("conventions")
    id("publish-jvm")
    kotlin("kapt") version "2.0.20"
}

kotlin {
    jvmToolchain(11)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
        // Otherwise will have: Could not resolve io.exoquery:pprint-kotlin:2.0.1.
        // Incompatible because this component declares a component, compatible with Java 11 and the consumer needed a component, compatible with Java 8
        java {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
        // If I remove this I get:
        //  'compileJava' task (current target is 11) and 'kaptGenerateStubsKotlin' task (current target is 1.8) jvm target compatibility should be set to the same Java version.
        // Not sure why
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

// Version from conventions.gradle.kts
val thisVersion = version


// TODO Reinstante when we bring back publishing functionality
//// Having issue with dokka failing: Reason: Task ':kaptmodule:dokkaHtml' uses this output of task ':kaptmodule:kaptDebugKotlin' without declaring an explicit or implicit dependency. This can lead to incorrect results being produced, depending on what order the tasks are executed.
//// See: https://github.com/Kotlin/dokka/issues/3117
//afterEvaluate {
//    tasks["dokkaHtml"].dependsOn(tasks.getByName("kaptKotlin"))
//}



dependencies {
    // Looks like it knows to do a project-dependency even if there is a version attached (i.e. I guess it ignores the version?)

    // NEED to have version here or downstream dependencies (i.e. projects using this plugin) will blow up
    api("io.exoquery:terpal-runtime:${thisVersion}")

    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")

    kapt("com.google.auto.service:auto-service:1.0.1")
    compileOnly("com.google.auto.service:auto-service-annotations:1.0.1")

    api("io.exoquery:decomat-core-jvm:${Conventions_gradle.Versions.decomatVersion}")
    api(kotlin("reflect"))
}
