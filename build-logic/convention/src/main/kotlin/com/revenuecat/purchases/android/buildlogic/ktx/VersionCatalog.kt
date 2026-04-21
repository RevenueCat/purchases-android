package com.revenuecat.purchases.android.buildlogic.ktx

import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionConstraint
import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependency

internal fun VersionCatalog.getVersion(alias: String): String =
    findVersion(alias).get().version

/**
 * Uses the same logic as VersionFactory.doGetVersion().
 */
internal val VersionConstraint.version: String
    get() = requiredVersion.ifEmpty {
        strictVersion.ifEmpty {
            preferredVersion
        }
    }

/**
 * Provides access to plugin aliases in the version catalog
 */
internal val VersionCatalog.plugins: PluginAccessor
    get() = PluginAccessor(this)

internal class PluginAccessor(private val catalog: VersionCatalog) {
    val android: PluginGroup get() = PluginGroup(catalog, "android")
    val kotlin: PluginGroup get() = PluginGroup(catalog, "kotlin")
    val kover: Provider<PluginDependency> get() = catalog.findPlugin("kover").get()
    val dokka: Provider<PluginDependency> get() = catalog.findPlugin("dokka").get()
    val mavenPublish: Provider<PluginDependency> get() = catalog.findPlugin("mavenPublish").get()
    val baselineprofile: Provider<PluginDependency> get() = catalog.findPlugin("baselineprofile").get()
    val metalava: Provider<PluginDependency> get() = catalog.findPlugin("metalava").get()
}

internal class PluginGroup(private val catalog: VersionCatalog, private val prefix: String) {
    val library: Provider<PluginDependency> get() = catalog.findPlugin("$prefix.library").get()
    val application: Provider<PluginDependency> get() = catalog.findPlugin("$prefix.application").get()
    val android: Provider<PluginDependency> get() = catalog.findPlugin("$prefix.android").get()
}
