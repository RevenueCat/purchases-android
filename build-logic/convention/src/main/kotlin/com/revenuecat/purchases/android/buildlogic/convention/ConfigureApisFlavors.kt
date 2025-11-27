package com.revenuecat.purchases.android.buildlogic.convention

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Configures the standard "apis" flavor dimension used across all library modules
 */
internal fun Project.configureApisFlavors() {
    extensions.configure<LibraryExtension> {
        flavorDimensions += "apis"

        productFlavors {
            create("defaults") {
                dimension = "apis"
                isDefault = true
            }
        }
    }
}
