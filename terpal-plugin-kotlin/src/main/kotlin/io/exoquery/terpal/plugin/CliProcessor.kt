package io.exoquery.terpal.plugin

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@AutoService(CommandLineProcessor::class)
@OptIn(ExperimentalCompilerApi::class)
class CliProcessor: CommandLineProcessor {
    override val pluginId: String = "io.exoquery.terpal-plugin"

    override val pluginOptions: Collection<CliOption> = listOf(
        CliOption(
            optionName = "projectDir",
            valueDescription = "path",
            description = "root project path",
            required = false,
        ),
        CliOption(
            optionName = "traceWrappers",
            valueDescription = "boolean",
            description = "trace auto-wrapping functions",
            required = false,
        )
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) {
        return when (option.optionName) {
            OptionKeys.TraceWrappers.name -> configuration.put(OptionKeys.TraceWrappers.compilerKey, value.toBoolean())
            "projectDir" -> configuration.put(PROJECT_DIR_KEY, value)
            else -> throw IllegalArgumentException("Unexpected config option ${option.optionName}")
        }
    }
}