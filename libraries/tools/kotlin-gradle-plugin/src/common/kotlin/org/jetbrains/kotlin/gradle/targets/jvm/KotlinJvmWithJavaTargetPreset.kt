/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.jetbrains.kotlin.gradle.DeprecatedTargetPresetApi
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.targets.android.internal.InternalKotlinTargetPreset
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.utils.configureExperimentalTryNext
import org.jetbrains.kotlin.gradle.utils.maybeCreateResolvable

@DeprecatedTargetPresetApi
class KotlinJvmWithJavaTargetPreset(
    private val project: Project
) : InternalKotlinTargetPreset<KotlinWithJavaTarget<KotlinJvmOptions, KotlinJvmCompilerOptions>> {

    override fun getName(): String = PRESET_NAME

    override fun createTargetInternal(name: String): KotlinWithJavaTarget<KotlinJvmOptions, KotlinJvmCompilerOptions> {
        project.reportDiagnostic(KotlinToolingDiagnostics.DeprecatedJvmWithJavaPresetDiagnostic())

        project.plugins.apply(JavaPlugin::class.java)

        @Suppress("UNCHECKED_CAST", "DEPRECATION", "TYPEALIAS_EXPANSION_DEPRECATION")
        val target = (project.objects.newInstance(
            KotlinWithJavaTarget::class.java,
            project,
            KotlinPlatformType.jvm,
            name,
            {
                object : DeprecatedHasCompilerOptions<KotlinJvmCompilerOptions> {
                    override val options: KotlinJvmCompilerOptions = project.objects
                        .newInstance(KotlinJvmCompilerOptionsDefault::class.java)
                        .configureExperimentalTryNext(project)
                }
            },
            { compilerOptions: KotlinJvmCompilerOptions ->
                object : KotlinJvmOptions {
                    override val options: KotlinJvmCompilerOptions
                        get() = compilerOptions
                }
            }
        ) as KotlinWithJavaTarget<KotlinJvmOptions, KotlinJvmCompilerOptions>)
            .apply {
                disambiguationClassifier = name
                preset = this@KotlinJvmWithJavaTargetPreset
            }

        AbstractKotlinPlugin.configureTarget(target) { compilation ->
            Kotlin2JvmSourceSetProcessor(KotlinTasksProvider(), KotlinCompilationInfo(compilation))
        }

        target.compilations.configureEach {
            @Suppress("DEPRECATION")
            it.compilerOptions.options.moduleName.convention(
                it.moduleNameForCompilation()
            )
        }

        target.compilations.getByName("test").run {
            val main = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

            compileDependencyFiles = project.files(
                main.output.allOutputs,
                project.configurations.maybeCreateResolvable(compileDependencyConfigurationName)
            )
            runtimeDependencyFiles = project.files(
                output.allOutputs,
                main.output.allOutputs,
                project.configurations.maybeCreateResolvable(runtimeDependencyConfigurationName)
            )
        }

        return target
    }

    companion object {
        const val PRESET_NAME = "jvmWithJava"
    }
}
