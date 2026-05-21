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
    // `:rules-engine` is a skeleton with no functionality and no consumers yet,
    // so we deliberately don't publish `purchases-rules-engine` to Maven Central.
    // Maven Central versions are immutable: once `10.7.0` ships, every following
    // release has to keep publishing the (empty) artifact. Skip applying the
    // publish plugin entirely here so the module still compiles, gets tested,
    // and runs detekt on every PR, but produces no artifact. This short-circuit
    // should be removed once the JSON Logic engine lands and `:rules-engine`
    // has a real consumer.
    if (project.path == ":rules-engine") return

    val variantToPublish = project.properties["ANDROID_VARIANT_TO_PUBLISH"].toString()
    val isPurchasesModule = project.path == ":purchases"

    if (isPurchasesModule || !variantToPublish.contains("customEntitlementComputation")) {
        pluginManager.apply(libs.plugins.mavenPublish.get().pluginId)
    }
}
