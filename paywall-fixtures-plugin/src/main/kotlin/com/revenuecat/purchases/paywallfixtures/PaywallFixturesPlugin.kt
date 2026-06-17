package com.revenuecat.purchases.paywallfixtures

import com.revenuecat.purchases.paywallfixtures.tasks.RecordPaywallFixturesTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.Properties

/**
 * Gradle plugin for offline paywall snapshot testing with the `purchases-ui-testing` artifact.
 *
 * Always registers `recordPaywallFixtures`. It does not bring Paparazzi itself; when the consumer
 * applies Paparazzi it adds the `purchases-ui-testing` test dependency and enables
 * `testOptions.unitTests.isIncludeAndroidResources`. Disable that auto-wiring with
 * `paywallFixtures { setupSnapshotTesting = false }` or `-Prevenuecat.paywallFixtures.snapshotTesting=false`.
 */
public class PaywallFixturesPlugin : Plugin<Project> {

    private companion object {
        const val PAPARAZZI_PLUGIN_ID = "app.cash.paparazzi"
        const val COMPOSE_COMPILER_PLUGIN_ID = "org.jetbrains.kotlin.plugin.compose"
        const val SNAPSHOT_TESTING_PROPERTY = "revenuecat.paywallFixtures.snapshotTesting"
    }

    override fun apply(project: Project) {
        val extension = project.extensions.create("paywallFixtures", PaywallFixturesExtension::class.java)
        extension.apiKey.convention(project.providers.environmentVariable("REVENUECAT_API_KEY"))
        extension.appUserId.convention("paywall-fixture-recorder")
        extension.baseUrl.convention("https://api.revenuecat.com")
        extension.outputDirectory.convention(
            project.layout.projectDirectory.dir("src/test/resources/revenuecat-paywall-fixtures"),
        )
        extension.setupSnapshotTesting.convention(true)

        project.tasks.register("recordPaywallFixtures", RecordPaywallFixturesTask::class.java) { task ->
            task.apiKey.set(extension.apiKey)
            task.appUserId.set(extension.appUserId)
            task.offerings.set(extension.offerings)
            task.baseUrl.set(extension.baseUrl)
            task.outputDirectory.set(extension.outputDirectory)
        }

        // Wire the kit + testOptions only when the consumer applies Paparazzi; deferred to afterEvaluate
        // so the setupSnapshotTesting DSL flag (set after the plugins block) is readable.
        project.pluginManager.withPlugin(PAPARAZZI_PLUGIN_ID) {
            project.afterEvaluate {
                if (snapshotTestingEnabled(it, extension)) {
                    it.dependencies.add("testImplementation", uiTestingDependency())
                    enableAndroidResourcesInUnitTests(it)
                    warnIfComposeMissing(it)
                }
            }
        }
    }

    private fun snapshotTestingEnabled(project: Project, extension: PaywallFixturesExtension): Boolean {
        val propertyOverride = (project.findProperty(SNAPSHOT_TESTING_PROPERTY) as? String)?.toBooleanStrictOrNull()
        return propertyOverride ?: extension.setupSnapshotTesting.get()
    }

    private fun uiTestingDependency(): String {
        val version = javaClass.getResourceAsStream("/com/revenuecat/purchases/paywallfixtures/version.properties")
            ?.use { Properties().apply { load(it) }.getProperty("version") }
            ?: error("paywall-fixtures-plugin version resource is missing; this is a packaging bug.")
        return "com.revenuecat.purchases:purchases-ui-testing:$version"
    }

    // Set reflectively to avoid a hard dependency on a specific AGP DSL version.
    private fun enableAndroidResourcesInUnitTests(project: Project) {
        val android = project.extensions.findByName("android") ?: return
        runCatching {
            val testOptions = android.javaClass.getMethod("getTestOptions").invoke(android)
            val unitTests = testOptions.javaClass.getMethod("getUnitTests").invoke(testOptions)
            unitTests.javaClass
                .getMethod("setIncludeAndroidResources", Boolean::class.javaPrimitiveType)
                .invoke(unitTests, true)
        }.onFailure {
            project.logger.warn("paywallFixtures: couldn't enable testOptions.unitTests.isIncludeAndroidResources.", it)
        }
    }

    private fun warnIfComposeMissing(project: Project) {
        if (!project.pluginManager.hasPlugin(COMPOSE_COMPILER_PLUGIN_ID)) {
            project.logger.warn(
                "paywallFixtures: the Compose compiler plugin is not applied; paywall snapshot tests " +
                    "render @Composable content and need it (plus buildFeatures.compose = true).",
            )
        }
    }
}
