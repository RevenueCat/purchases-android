package com.revenuecat.purchases.android.buildlogic.convention

import com.revenuecat.purchases.android.buildlogic.ktx.libs
import com.revenuecat.purchases.android.buildlogic.ktx.plugins
import org.gradle.api.Project

/**
 * Configures conditional Maven publishing based on the ANDROID_VARIANT_TO_PUBLISH property.
 * - For the purchases module: always apply publishing (publishes both defaults and customEntitlementComputation)
 * - For other modules: only apply publishing for variants that don't contain "customEntitlementComputation"
 */
internal fun Project.configureConditionalPublishing() {
    val variantToPublish = project.properties["ANDROID_VARIANT_TO_PUBLISH"].toString()
    val isPurchasesModule = project.path == ":purchases"

    if (isPurchasesModule || !variantToPublish.contains("customEntitlementComputation")) {
        pluginManager.apply(libs.plugins.mavenPublish.get().pluginId)
    }
}
