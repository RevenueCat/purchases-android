package com.revenuecat.purchases.android.buildlogic.convention

import com.revenuecat.purchases.android.buildlogic.ktx.libs
import com.revenuecat.purchases.android.buildlogic.ktx.plugins
import org.gradle.api.Project

/**
 * Configures Dokka for RevenueCat libraries that expose a public API surface.
 */
internal fun Project.configureDokka() {
    pluginManager.apply(libs.plugins.dokka.get().pluginId)
}
