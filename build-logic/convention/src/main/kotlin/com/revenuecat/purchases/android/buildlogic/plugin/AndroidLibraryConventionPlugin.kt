package com.revenuecat.purchases.android.buildlogic.plugin

import com.revenuecat.purchases.android.buildlogic.convention.configureAndroidLibrary
import com.revenuecat.purchases.android.buildlogic.convention.configureApisFlavors
import com.revenuecat.purchases.android.buildlogic.ktx.libs
import com.revenuecat.purchases.android.buildlogic.ktx.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * A build convention plugin to be applied to all Android library modules, to reduce duplication
 * in build scripts.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply(libs.plugins.android.library.get().pluginId)
            apply(libs.plugins.kotlin.android.get().pluginId)
            apply(libs.plugins.kover.get().pluginId)
        }
        configureAndroidLibrary()

        // Apply APIs flavors configuration for all libraries
        configureApisFlavors()
    }
}
