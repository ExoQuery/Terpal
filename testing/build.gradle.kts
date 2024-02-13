plugins {
    id("io.exoquery.terpal-plugin")
    kotlin("jvm")
    application
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
    }
}

// Needed for Kotest
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

repositories {
    mavenCentral()
    mavenLocal()
}

application {
    mainClass.set("MainKt")
}

dependencies {
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("io.exoquery:pprint-kotlin:2.0.1")
}
