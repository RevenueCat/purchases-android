package com.revenuecat.purchases.android.buildlogic.plugin

import com.revenuecat.purchases.android.buildlogic.convention.configureAndroidLibrary
import com.revenuecat.purchases.android.buildlogic.convention.configureApisFlavors
import com.revenuecat.purchases.android.buildlogic.convention.configureConditionalPublishing
import com.revenuecat.purchases.android.buildlogic.convention.configureMetalava
import com.revenuecat.purchases.android.buildlogic.ktx.libs
import com.revenuecat.purchases.android.buildlogic.ktx.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin for public library modules.
 * Applies library configuration + conditional publishing + metalava + baseline profile.
 */
class PublicLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply(libs.plugins.android.library.get().pluginId)
            apply(libs.plugins.kotlin.android.get().pluginId)
            apply(libs.plugins.kover.get().pluginId)
            apply(libs.plugins.dokka.get().pluginId)
            apply(libs.plugins.baselineprofile.get().pluginId)
        }

        configureAndroidLibrary()
        configureApisFlavors()
        configureConditionalPublishing()
        configureMetalava()
    }
}
