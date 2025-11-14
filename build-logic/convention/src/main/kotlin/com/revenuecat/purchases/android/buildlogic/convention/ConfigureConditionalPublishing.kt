package com.revenuecat.purchases.android.buildlogic.convention

import com.revenuecat.purchases.android.buildlogic.ktx.libs
import com.revenuecat.purchases.android.buildlogic.ktx.plugins
import org.gradle.api.Project

/**
 * Configures conditional Maven publishing based on the ANDROID_VARIANT_TO_PUBLISH property.
 * Only applies publishing for variants that don't contain "customEntitlementComputation"
 */
internal fun Project.configureConditionalPublishing() {
    if (!project.properties["ANDROID_VARIANT_TO_PUBLISH"].toString().contains("customEntitlementComputation")) {
        pluginManager.apply(libs.plugins.mavenPublish.get().pluginId)
    }
}
