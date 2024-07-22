package io.exoquery.terpal

import io.exoquery.terpal_plugin_gradle.BuildConfig
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class GradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = "io.exoquery.terpal-plugin"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "io.exoquery",
        artifactId = "terpal-plugin-kotlin",
        version = BuildConfig.VERSION
    )

    override fun apply(target: Project) {
        /* make sure we don't try to add dependency until it has been configured by kotlin plugin */
        target.plugins.withId("org.jetbrains.kotlin.jvm") {
            target.dependencies.add("implementation", "io.exoquery:terpal-runtime:${BuildConfig.VERSION}")
        }

        // Needed for the plugin classpath
        target.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            target.dependencies.add("kotlinNativeCompilerPluginClasspath", "io.exoquery:terpal-runtime:${BuildConfig.VERSION}")
            target.dependencies.add("kotlinNativeCompilerPluginClasspath", "io.exoquery:decomat-core-jvm:${BuildConfig.DECOMAT_VERSION}")
        }
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project

        // ALSO needed for the plugin classpath
        kotlinCompilation.dependencies {
            api("io.exoquery:terpal-runtime:${BuildConfig.VERSION}")
        }

        return project.provider {
            listOf(
                SubpluginOption("projectDir", project.projectDir.path)
            )
        }
    }
}
