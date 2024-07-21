plugins {
    id("io.exoquery.terpal-plugin")
    kotlin("multiplatform") version "1.9.22"
}

//buildscript {
//    dependencies {
//        classpath("io.exoquery:terpal-runtime:1.9.22-0.3.0")
//    }
//}

kotlin {
    jvm {
        jvmToolchain(11)
    }

    linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                //api(kotlin("reflect"))

                //api("io.exoquery:terpal-runtime:1.9.22-0.3.0")

                //implementation("io.exoquery:decomat-core:3.0.0")
                //implementation("io.exoquery:pprint-kotlin:2.0.2")
            }
        }

        //val linuxX64Main by getting {
        //    dependencies {
        //        api("io.exoquery:terpal-runtime:1.9.22-0.3.0")
        //    }
        //}

        val commonTest by getting {
            kotlin.srcDir("$buildDir/generated/ksp/metadata/commonMain/kotlin")
            dependencies {
                // Used to ad-hoc some examples but not needed.
                //api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.2")
                //implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                implementation(kotlin("test"))
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
    }
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

//configurations["kotlinCompilerPluginClasspath"].attributes.attribute(
//  org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE,
//  objects.named(org.gradle.api.attributes.Usage::class.java, "java-runtime")
//)

dependencies {
  //kotlinCompilerClasspath("io.exoquery:terpal-runtime:1.9.22-0.3.0")
  kotlinCompilerPluginClasspath("io.exoquery:terpal-runtime:1.9.22-0.3.0")
  kotlinNativeCompilerPluginClasspath("io.exoquery:terpal-runtime:1.9.22-0.3.0")

  //kotlinCompilerClasspath("io.exoquery:decomat-core-jvm:4.0.1")
  kotlinCompilerPluginClasspath("io.exoquery:decomat-core-jvm:4.0.1")
  kotlinNativeCompilerPluginClasspath("io.exoquery:decomat-core-jvm:4.0.1")
}
