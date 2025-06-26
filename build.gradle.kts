plugins {
  kotlin("multiplatform") version "2.2.0" apply false
  id("io.github.gradle-nexus.publish-plugin") version "1.1.0" apply false
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/releases")
        mavenLocal()
    }
}

tasks.register("publishLinux") {
  dependsOn(
    gradle.includedBuild(Release.Project.`terpal-runtime`).task(":publishAllPublicationsToOssRepository"),
    gradle.includedBuild(Release.Project.`terpal-plugin-gradle`).task(":publish"),
    gradle.includedBuild(Release.Project.`terpal-plugin-kotlin`).task(":publish"),
  )
}

tasks.register("publishLinuxLocal") {
  ":${Release.Project.`terpal-runtime`}:publishToMavenLocal" // should this first clause even be here?
  dependsOn(
    gradle.includedBuild(Release.Project.`terpal-runtime`).task(":publishToMavenLocal"),
    gradle.includedBuild(Release.Project.`terpal-plugin-gradle`).task(":publishToMavenLocal"),
    gradle.includedBuild(Release.Project.`terpal-plugin-kotlin`).task(":publishToMavenLocal"),
  )
}

tasks.register("publishMac") {
  //Release.macBuildCommands.forEach {
  //  dependsOn(gradle.includedBuild(Release.Project.`terpal-runtime`).task(":$it"))
  //}
  dependsOn(
    gradle.includedBuild(Release.Project.`terpal-runtime`).task(":publishAllPublicationsToOssRepository")
  )
}

tasks.register("publishWindows") {
  Release.windowsBuildCommands.forEach {
    dependsOn(gradle.includedBuild(Release.Project.`terpal-runtime`).task(":$it"))
  }
}


tasks.register("publishLinuxNoRunner") {
  dependsOn(
    gradle.includedBuild(Release.Project.`terpal-runtime`).task(":publishAllPublicationsToOssRepository"),
    gradle.includedBuild(Release.Project.`terpal-plugin-gradle`).task(":publish"),
    gradle.includedBuild(Release.Project.`terpal-plugin-kotlin`).task(":publish"),
  )
}
tasks.register("publishMacNoRunner") {
  Release.macBuildCommands.forEach {
    dependsOn(gradle.includedBuild(Release.Project.`terpal-runtime`).task(":$it"))
  }
}
tasks.register("publishWindowsNoRunner") {
  Release.windowsBuildCommands.forEach {
    dependsOn(gradle.includedBuild(Release.Project.`terpal-runtime`).task(":$it"))
  }
}


object Release {

  object Project {
    val `terpal-runtime` = "terpal-runtime"
    val `terpal-plugin-gradle` = "terpal-plugin-gradle"
    val `terpal-plugin-kotlin` = "terpal-plugin-kotlin"
  }

  // :terpal-runtime:publishMacosX64PublicationToOss
  // :terpal-runtime:publishMacosArm64PublicationToOss
  // :terpal-runtime:publishIosX64PublicationToOss
  // :terpal-runtime:publishIosArm64PublicationToOss

  val macBuildCommands =
    listOf(
      //"iosX64",
      //"iosArm64",
      "tvosX64",
      "tvosArm64",
      "watchosX64",
      "watchosArm32",
      "watchosArm64",
      //"macosX64",
      //"macosArm64",
      "iosSimulatorArm64"
    ).map { "publish${it.capitalize()}PublicationToOssRepository" }

  val windowsBuildCommands =
    listOf(
      "mingwX64"
    ).map { "publish${it.capitalize()}PublicationToOssRepository" }

  fun String.capitalize() = this.replaceFirstChar { it.uppercase() }
}
