package com.revenuecat.purchases.android.buildlogic.convention

import com.revenuecat.purchases.android.buildlogic.ktx.libs
import com.revenuecat.purchases.android.buildlogic.ktx.plugins
import org.gradle.api.Project

/**
 * Configures Metalava with common settings for RevenueCat libraries
 */
internal fun Project.configureMetalava() {
    pluginManager.apply(libs.plugins.metalava.get().pluginId)

    // Configure metalava after evaluation when the extension is available
    afterEvaluate {
        extensions.findByName("metalava")?.let { metalavaExt ->
            // Using reflection to configure since metalava types aren't in classpath at compile time
            val metalavaClass = metalavaExt::class.java

            // hiddenAnnotations.add("com.revenuecat.purchases.InternalRevenueCatAPI")
            metalavaClass.getMethod("getHiddenAnnotations")
                .invoke(metalavaExt)
                ?.let { it as? MutableList<String> }
                ?.add("com.revenuecat.purchases.InternalRevenueCatAPI")

            // arguments.addAll(listOf("--hide", "ReferencesHidden"))
            metalavaClass.getMethod("getArguments")
                .invoke(metalavaExt)
                ?.let { it as? MutableList<String> }
                ?.addAll(listOf("--hide", "ReferencesHidden"))
        }
    }
}
