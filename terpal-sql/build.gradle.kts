plugins {
    id("io.exoquery.terpal-plugin")
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.0.0-RC3"
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

repositories {
    mavenCentral()
    mavenLocal()
}

application {
    mainClass.set("MainKt")
}

dependencies {
    implementation("io.exoquery:pprint-kotlin:2.0.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.2")
    testImplementation("io.zonky.test:embedded-postgres:2.0.7")
    testImplementation(kotlin("test"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}