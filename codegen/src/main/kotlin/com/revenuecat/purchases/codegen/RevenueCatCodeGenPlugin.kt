package com.revenuecat.purchases.codegen

import com.revenuecat.purchases.codegen.tasks.FetchSchemaTask
import com.revenuecat.purchases.codegen.tasks.GenerateCodeTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

public class RevenueCatCodeGenPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("revenuecat", RevenueCatExtension::class.java)

        val cacheDir = project.layout.buildDirectory.dir("revenuecat/cache")
        val outputDir = project.layout.buildDirectory.dir("generated/revenuecat/kotlin")

        val fetchTask: TaskProvider<FetchSchemaTask> = project.tasks.register(
            "rcFetchSchema",
            FetchSchemaTask::class.java,
        ) { task ->
            task.apiKey.set(extension.apiKey)
            task.projectId.set(extension.projectId)
            task.cacheTtlMinutes.set(extension.cacheTtlMinutes)
            task.offlineMode.set(extension.offlineMode)
            task.cacheDir.set(cacheDir)
        }

        val generateTask: TaskProvider<GenerateCodeTask> = project.tasks.register(
            "rcGenerateCode",
            GenerateCodeTask::class.java,
        ) { task ->
            task.packageName.set(extension.packageName)
            task.namingStyle.set(extension.namingStyle)
            task.generateEntitlements.set(extension.generateEntitlements)
            task.generateOfferings.set(extension.generateOfferings)
            task.generatePackages.set(extension.generatePackages)
            task.generateCustomerInfoExtensions.set(extension.generateCustomerInfoExtensions)
            task.cacheDir.set(cacheDir)
            task.outputDir.set(outputDir)
            task.dependsOn(fetchTask)
        }

        // Use plugins.withId() instead of afterEvaluate so wiring is plugin-lifecycle
        // aware and does not block configuration cache support.
        val outputFile = outputDir.get().asFile
        project.plugins.withId("com.android.application") {
            wireToAndroid(project, generateTask, outputFile)
        }
        project.plugins.withId("com.android.library") {
            wireToAndroid(project, generateTask, outputFile)
        }
        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            wireToKotlinJvm(project, generateTask, outputFile)
        }
    }

    private fun wireToAndroid(
        project: Project,
        generateTask: TaskProvider<GenerateCodeTask>,
        outputDir: java.io.File,
    ) {
        // Try the modern AndroidComponents API first. Only fall back to the legacy
        // source-set API when AndroidComponents is unavailable, to avoid registering
        // the output directory twice (which would cause duplicate compilation inputs).
        var wiredViaComponents = false
        try {
            val androidComponents = project.extensions.findByName("androidComponents")
            if (androidComponents != null) {
                val method = androidComponents.javaClass.getMethod(
                    "onVariants",
                    kotlin.jvm.functions.Function1::class.java,
                )
                @Suppress("UNCHECKED_CAST")
                method.invoke(
                    androidComponents,
                    { variant: Any ->
                        try {
                            val sourcesMethod = variant.javaClass.getMethod("getSources")
                            val sources = sourcesMethod.invoke(variant)
                            val kotlinMethod = sources.javaClass.getMethod("getKotlin")
                            val kotlin = kotlinMethod.invoke(sources)
                            val addStaticSourceDirectory = kotlin.javaClass.getMethod(
                                "addStaticSourceDirectory",
                                String::class.java,
                            )
                            addStaticSourceDirectory.invoke(kotlin, outputDir.absolutePath)
                        } catch (e: Exception) {
                            project.logger.debug("Could not wire via AndroidComponents API: ${e.message}")
                        }
                    } as kotlin.jvm.functions.Function1<Any, Unit>,
                )
                wiredViaComponents = true
            }
        } catch (e: Exception) {
            project.logger.debug("AndroidComponents wiring failed, falling back to source set: ${e.message}")
        }

        // Fallback: add to source sets directly — only when AndroidComponents wiring did not run.
        if (!wiredViaComponents) {
            val android = project.extensions.findByName("android")
            if (android != null) {
                try {
                    val sourceSetsMethod = android.javaClass.getMethod("getSourceSets")
                    val sourceSets = sourceSetsMethod.invoke(android)
                    val getByNameMethod = sourceSets.javaClass.getMethod("getByName", String::class.java)
                    val mainSourceSet = getByNameMethod.invoke(sourceSets, "main")
                    val kotlinSrcDirs = mainSourceSet.javaClass.getMethod("getKotlin")
                    val kotlin = kotlinSrcDirs.invoke(mainSourceSet)
                    val srcDirMethod = kotlin.javaClass.getMethod("srcDir", Any::class.java)
                    srcDirMethod.invoke(kotlin, outputDir)
                } catch (e: Exception) {
                    project.logger.warn(
                        "Could not add generated source directory to Android source sets: ${e.message}",
                    )
                }
            }
        }

        // Ensure all Kotlin-processing tasks (compile, KSP, KAPT) depend on code generation.
        // KSP tasks are named ksp*Kotlin and KAPT tasks kapt*Kotlin — they both consume
        // the generated source directory but would otherwise have no implicit dependency on
        // rcGenerateCode, causing Gradle's implicit dependency validation to fail.
        project.tasks.configureEach { task ->
            if (task.name.endsWith("Kotlin") && (
                    task.name.startsWith("compile") ||
                        task.name.startsWith("ksp") ||
                        task.name.startsWith("kapt")
                    )
            ) {
                task.dependsOn(generateTask)
            }
        }
    }

    private fun wireToKotlinJvm(
        project: Project,
        generateTask: TaskProvider<GenerateCodeTask>,
        outputDir: java.io.File,
    ) {
        // For Kotlin JVM / pure JVM projects
        try {
            val kotlinExtension = project.extensions.findByName("kotlin")
            if (kotlinExtension != null) {
                val sourceSetsMethod = kotlinExtension.javaClass.getMethod("getSourceSets")
                val sourceSets = sourceSetsMethod.invoke(kotlinExtension)
                val getByNameMethod = sourceSets.javaClass.getMethod("getByName", String::class.java)
                val mainSourceSet = getByNameMethod.invoke(sourceSets, "main")
                val kotlinDir = mainSourceSet.javaClass.getMethod("getKotlin")
                val kotlin = kotlinDir.invoke(mainSourceSet)
                val srcDirMethod = kotlin.javaClass.getMethod("srcDir", Any::class.java)
                srcDirMethod.invoke(kotlin, outputDir)
            }
        } catch (e: Exception) {
            project.logger.warn("Could not add generated source directory to Kotlin source sets: ${e.message}")
        }

        project.tasks.configureEach { task ->
            if (task.name.startsWith("compile") && task.name.contains("Kotlin")) {
                task.dependsOn(generateTask)
            }
        }
    }
}
