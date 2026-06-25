package com.revenuecat.purchases.android.buildlogic.plugin

import com.revenuecat.purchases.android.buildlogic.convention.configureSnapshotPublishing
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Applies SNAPSHOT publishing config to modules that apply the Maven publish plugin directly
 * instead of through [PublicLibraryConventionPlugin], such as `:bom` and `:codegen`.
 */
class SnapshotPublishingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.configureSnapshotPublishing()
    }
}
