plugins {
  id("publish")
}

kotlin {
    jvmToolchain(11)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions{
        freeCompilerArgs = listOf("-Xcontext-receivers")
        // Otherwise will have: Could not resolve io.exoquery:pprint-kotlin:2.0.1.
        // Incompatible because this component declares a component, compatible with Java 11 and the consumer needed a component, compatible with Java 8
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}


dependencies {
    api(kotlin("reflect"))
    implementation("io.exoquery:pprint-kotlin:2.0.2")
    implementation("io.exoquery:decomat-core:0.0.7")
}

// Needed for testing
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}