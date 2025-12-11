package com.revenuecat.purchases.android.buildlogic.convention

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Configures APIs flavor dimension for application modules that need to test both API flavors.
 * Creates "defaults" and "customEntitlementComputation" flavors.
 */
internal fun Project.configureApisFlavorsForApplication() {
    extensions.configure<ApplicationExtension> {
        flavorDimensions += "apis"
        productFlavors {
            create("defaults") {
                dimension = "apis"
                isDefault = true
            }
            create("customEntitlementComputation") {
                dimension = "apis"
            }
        }
    }
}
