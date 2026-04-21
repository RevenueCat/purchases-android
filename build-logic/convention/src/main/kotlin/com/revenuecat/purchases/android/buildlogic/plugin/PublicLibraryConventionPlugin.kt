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

        // Workaround for https://issuetracker.google.com/issues/328687152:
        // Modules without a baselineProfile(...) dependency do not get the task wiring between
        // copyBaselineProfileIntoSrc and prepareXxxArtProfile, causing an implicit dependency
        // error in Gradle 8+. mustRunAfter enforces ordering without adding a dependency edge
        // (which would create a circular dependency in modules that DO have explicit wiring).
        afterEvaluate {
            val hasBaselineProfileDeps = configurations.findByName("baselineProfile")
                ?.dependencies?.isNotEmpty() == true
            if (!hasBaselineProfileDeps) {
                tasks.matching { it.name.startsWith("prepare") && it.name.endsWith("ArtProfile") }
                    .configureEach { mustRunAfter("copyBaselineProfileIntoSrc") }
            }
        }
    }
}
