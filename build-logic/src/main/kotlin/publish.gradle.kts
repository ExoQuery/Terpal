import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
  // Remove for now because unnecessary can add back in order to share settings.
  id("conventions")
  `maven-publish`
  signing
  id("io.github.gradle-nexus.publish-plugin")
  id("org.jetbrains.dokka")
}

apply(plugin = "io.github.gradle-nexus.publish-plugin")

repositories {
  mavenCentral()
  maven(url = "https://plugins.gradle.org/m2/")
}


// Disable publishing for testing project
tasks.withType<PublishToMavenRepository>().configureEach {
  gradle.startParameter.maxWorkerCount = 1

  onlyIf {
    publication.artifactId != "testing"
//      && !publication.name.lowercase().contains("iosX64".lowercase())
//      && !publication.name.lowercase().contains("iosArm64".lowercase())
//      && !publication.name.lowercase().contains("macosX64".lowercase())
//      && !publication.name.lowercase().contains("macosArm64".lowercase())
//      && publication.name != "publishKotlinMultiplatformPublicationToOssRepository"
//      && publication.name != "publishJvmPublicationToOssRepository"
  }
}

apply {
  plugin("org.jetbrains.dokka")
}

val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)
val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
  dependsOn(dokkaHtml)
  archiveClassifier.set("javadoc")
  from(dokkaHtml.outputDirectory)
}

publishing {
  val user = System.getenv("SONATYPE_USERNAME")
  val pass = System.getenv("SONATYPE_PASSWORD")

  repositories {
    maven {
      name = "Oss"
      setUrl {
        if (!HostManager.hostIsMac) {
          val repositoryId = System.getenv("STAGING_REPO_ID")
          if (repositoryId.trim().isEmpty() || repositoryId.trim() == "") error("STAGING_REPO_ID is empty")
          "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deployByRepositoryId/$repositoryId/"
        } else {
          // For macOS builds, create the default repo
          "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
        }
      }
      credentials {
        username = user
        password = pass
      }
    }
    maven {
      name = "Snapshot"
      setUrl { "https://central.sonatype.com/repository/maven-snapshots/" }
      credentials {
        username = user
        password = pass
      }
    }
  }

  publications.withType<MavenPublication> {
    artifact(javadocJar)

    pom {
      name.set("terpal")
      description.set("Terpal - Custom string-interpolation for Kotlin")
      url.set("https://github.com/exoquery/terpal")

      licenses {
        license {
          name.set("The Apache Software License, Version 2.0")
          url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
          distribution.set("repo")
        }
      }

      developers {
        developer {
          name.set("Alexander Ioffe")
          email.set("deusaquilus@gmail.com")
          organization.set("github")
          organizationUrl.set("http://www.github.com")
        }
      }

      scm {
        url.set("https://github.com/exoquery/terpal/tree/main")
        connection.set("scm:git:git://github.com/ExoQuery/Terpal.git")
        developerConnection.set("scm:git:ssh://github.com:ExoQuery/Terpal.git")
      }
    }
  }
}

val isCI = project.hasProperty("isCI")
val isLocal = !isCI
val noSign = project.hasProperty("nosign")
val doNotSign = isLocal || noSign

signing {
  // Sign if we're not doing a local build and we haven't specifically disabled it
  if (!doNotSign) {
    val signingKeyRaw = System.getenv("NEW_SIGNING_KEY_ID_BASE64")
    if (signingKeyRaw == null) error("ERROR: No Signing Key Found")
    // Seems like the right way was to have newlines after all the exported (ascii armored) lines
    // and you can put them into the github-var with newlines but if you
    // include the "-----BEGIN PGP PRIVATE KEY BLOCK-----" and "-----END PGP PRIVATE KEY BLOCK-----"
    // parts with that then errors happen. Have a look at https://github.com/gradle/gradle/issues/15718 for more detail
    // Ultimately however `iurysza` is only partially correct and they key-itself does not need to be escaped
    // and can be put into a github-var with newlines.
    val signingKey =
      "-----BEGIN PGP PRIVATE KEY BLOCK-----\n\n${signingKeyRaw}\n-----END PGP PRIVATE KEY BLOCK-----"
    useInMemoryPgpKeys(
      System.getenv("NEW_SIGNING_KEY_ID_BASE64_ID"),
      signingKey,
      System.getenv("NEW_SIGNING_KEY_ID_BASE64_PASS")
    )
    sign(publishing.publications)
  }
}

// Fix for Kotlin issue: https://youtrack.jetbrains.com/issue/KT-61313
tasks.withType<Sign>().configureEach {
  val pubName = name.removePrefix("sign").removeSuffix("Publication")

  // These tasks only exist for native targets, hence findByName() to avoid trying to find them for other targets

  // Task ':linkDebugTest<platform>' uses this output of task ':sign<platform>Publication' without declaring an explicit or implicit dependency
  tasks.findByName("linkDebugTest$pubName")?.let {
    mustRunAfter(it)
  }
  // Task ':compileTestKotlin<platform>' uses this output of task ':sign<platform>Publication' without declaring an explicit or implicit dependency
  tasks.findByName("compileTestKotlin$pubName")?.let {
    mustRunAfter(it)
  }
}

// Was having odd issues happening in CI releases like this:
// e.g. Task ':pprint-kotlin-core:publish<AndroidNativeArm32>PublicationToOssRepository' uses this output of task ':pprint-kotlin-core:sign<AndroidNativeArm64>Publication' without declaring an explicit or implicit dependency.
// I tried a few things that caused other issues. Ultimately the working solution I got from here:
// https://github.com/gradle/gradle/issues/26091#issuecomment-1722947958
tasks.withType<AbstractPublishToMaven>().configureEach {
  gradle.startParameter.maxWorkerCount = 1
  val signingTasks = tasks.withType<Sign>()
  mustRunAfter(signingTasks)

  onlyIf {
    publication.artifactId != "testing"
  }
}
