package com.revenuecat.purchases.codegen.tasks

import com.revenuecat.purchases.codegen.NamingStyle
import com.revenuecat.purchases.codegen.cache.SchemaCache
import com.revenuecat.purchases.codegen.generator.CustomerInfoExtGenerator
import com.revenuecat.purchases.codegen.generator.EntitlementsGenerator
import com.revenuecat.purchases.codegen.generator.OfferingsGenerator
import com.revenuecat.purchases.codegen.generator.PackagesGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

// Note: @CacheableTask is intentionally omitted. The cache JSON includes a timestamp that
// changes on every fresh fetch, so the Gradle build cache would never produce a hit.
public abstract class GenerateCodeTask : DefaultTask() {

    @get:Input
    public abstract val packageName: Property<String>

    @get:Input
    public abstract val namingStyle: Property<NamingStyle>

    @get:Input
    public abstract val generateEntitlements: Property<Boolean>

    @get:Input
    public abstract val generateOfferings: Property<Boolean>

    @get:Input
    public abstract val generatePackages: Property<Boolean>

    @get:Input
    public abstract val generateCustomerInfoExtensions: Property<Boolean>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val cacheDir: DirectoryProperty

    @get:OutputDirectory
    public abstract val outputDir: DirectoryProperty

    init {
        group = "revenuecat"
        description = "Generates type-safe Kotlin accessors from RevenueCat schema"
    }

    @TaskAction
    public fun generate() {
        val cache = SchemaCache(cacheDir.get().asFile)
        val schema = cache.read()

        if (schema == null) {
            logger.warn("No RevenueCat schema cache found. Skipping code generation.")
            return
        }

        cache.cacheAgeMinutes()?.let { age ->
            if (age > 0) logger.lifecycle("Using RevenueCat schema cached ${age}m ago.")
        }

        val outputDirectory = outputDir.get().asFile
        outputDirectory.deleteRecursively()
        outputDirectory.mkdirs()

        val pkg = packageName.get()
        val style = namingStyle.get()

        if (generateEntitlements.get() && schema.entitlements.isNotEmpty()) {
            EntitlementsGenerator(pkg, style).generate(schema.entitlements, outputDirectory)
            logger.lifecycle("Generated entitlement accessors for ${schema.entitlements.size} entitlements.")
        }

        if (generateOfferings.get() && schema.offerings.isNotEmpty()) {
            OfferingsGenerator(pkg, style).generate(schema.offerings, outputDirectory)
            logger.lifecycle("Generated offering accessors for ${schema.offerings.size} offerings.")
        }

        if (generatePackages.get() && schema.offerings.isNotEmpty()) {
            PackagesGenerator(pkg, style).generate(schema.offerings, outputDirectory)
            logger.lifecycle("Generated package accessors.")
        }

        if (generateCustomerInfoExtensions.get() && schema.entitlements.isNotEmpty()) {
            CustomerInfoExtGenerator(pkg, style).generate(schema.entitlements, outputDirectory)
            logger.lifecycle("Generated CustomerInfo extension properties.")
        }
    }
}
