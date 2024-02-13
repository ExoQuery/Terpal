package io.exoquery.terpal.plugin

import io.exoquery.terpal.plugin.transform.VisitTransformExpressions
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import java.nio.file.Path

class GenerationExtension(
    private val config: CompilerConfiguration,
    private val messages: MessageCollector,
    private val projectDir: Path,
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment
            .transform(
                VisitTransformExpressions(pluginContext, config, projectDir),
                null
            )
    }
}
