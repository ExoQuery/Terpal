package io.exoquery.terpal.plugin

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.io.path.Path

@AutoService(CompilerPluginRegistrar::class)
@OptIn(ExperimentalCompilerApi::class)
class Registrar: CompilerPluginRegistrar() {
    override val supportsK2: Boolean get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        IrGenerationExtension.registerExtension(
            GenerationExtension(
              // Need to initialize the config here because later the options info inside the config will be
              // erased. I am not sure why kotlin does this.
              Options(configuration),
              configuration,
              configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY),
              Path(configuration.getNotNull(PROJECT_DIR_KEY))
            )
        )
    }
}
