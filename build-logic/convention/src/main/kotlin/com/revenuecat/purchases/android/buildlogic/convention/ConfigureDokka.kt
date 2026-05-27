package com.revenuecat.purchases.android.buildlogic.convention

import com.revenuecat.purchases.android.buildlogic.ktx.libs
import com.revenuecat.purchases.android.buildlogic.ktx.plugins
import org.gradle.api.Project

/**
 * Configures Dokka for RevenueCat libraries that expose a public API surface.
 */
internal fun Project.configureDokka() {
    // Skip Dokka for `:rules-engine-internal` since it's SDK-internal by design.
    if (project.path == ":rules-engine-internal") return

    pluginManager.apply(libs.plugins.dokka.get().pluginId)
}
