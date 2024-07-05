import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import org.testcontainers.containers.PostgreSQLContainer
import java.util.function.Consumer

buildscript {
    repositories {
        mavenCentral()
        mavenLocal()
    }
    dependencies {
        classpath("io.kotest.extensions:kotest-extensions-testcontainers:2.0.2")
        classpath("org.testcontainers:testcontainers:1.19.8")
        classpath("org.testcontainers:postgresql:1.19.8")
    }
}

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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {

    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.2")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    // Optional by the user. This library provides certain utilities that enhance Hikari.
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("com.typesafe:config:1.4.1")

    testImplementation("io.exoquery:pprint-kotlin:2.0.2")
    testImplementation("io.zonky.test:embedded-postgres:2.0.7")
    testImplementation("mysql:mysql-connector-java:8.0.29")
    testImplementation("com.microsoft.sqlserver:mssql-jdbc:7.4.1.jre11")
    testImplementation("com.h2database:h2:2.2.224")
    testImplementation("org.xerial:sqlite-jdbc:3.42.0.1")
    testImplementation("com.oracle.ojdbc:ojdbc8:19.3.0.0")

    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest.extensions:kotest-extensions-testcontainers:2.0.2")

    testImplementation("org.testcontainers:mysql:1.19.8")
    testImplementation("org.testcontainers:postgresql:1.19.8")
    testImplementation("org.testcontainers:mssqlserver:1.19.8")
    testImplementation("org.testcontainers:oracle-xe:1.19.8")

    testApi(platform("io.zonky.test.postgres:embedded-postgres-binaries-bom:16.2.0"))
    testImplementation("org.flywaydb:flyway-core:7.15.0") // corresponding to embedded-postgres
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
