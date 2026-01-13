package com.revenuecat.purchases.android.buildlogic.plugin

import com.revenuecat.purchases.android.buildlogic.convention.configureAndroidApplication
import com.revenuecat.purchases.android.buildlogic.convention.configureApisFlavorsForApplication
import com.revenuecat.purchases.android.buildlogic.ktx.libs
import com.revenuecat.purchases.android.buildlogic.ktx.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin for API tester applications that need to test both API flavors.
 * Applies application configuration + both "defaults" and "customEntitlementComputation" flavors.
 */
class ApiTesterApplicationConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply(libs.plugins.android.application.get().pluginId)
            apply(libs.plugins.kotlin.android.get().pluginId)
        }

        configureAndroidApplication()
        configureApisFlavorsForApplication()
    }
}
