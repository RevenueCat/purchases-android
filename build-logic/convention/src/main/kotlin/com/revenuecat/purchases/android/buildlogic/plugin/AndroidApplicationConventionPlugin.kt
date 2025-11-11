package com.revenuecat.purchases.android.buildlogic.plugin

import com.revenuecat.purchases.android.buildlogic.convention.configureAndroidApplication
import com.revenuecat.purchases.android.buildlogic.ktx.libs
import com.revenuecat.purchases.android.buildlogic.ktx.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * A build convention plugin to be applied to all Android application modules, to reduce duplication
 * in build scripts.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply(libs.plugins.android.application.get().pluginId)
            apply(libs.plugins.kotlin.android.get().pluginId)
        }
        configureAndroidApplication()
    }
}
