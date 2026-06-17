package com.revenuecat.purchases.paywallfixtures

import com.revenuecat.purchases.paywallfixtures.tasks.RecordPaywallFixturesTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.Properties

/**
 * Gradle plugin for offline paywall snapshot testing with the `purchases-ui-testing` artifact.
 *
 * It always registers the `recordPaywallFixtures` task. By default it is "lean": it does NOT bring its
 * own Paparazzi, so applying it alone never forces a Paparazzi version onto your buildscript classpath.
 *
 * To opt in to the snapshot-testing setup, apply Paparazzi yourself (one line, your chosen version) —
 * the plugin reacts to it and wires up the rest:
 *
 * ```kotlin
 * plugins {
 *     id("com.android.application")
 *     id("org.jetbrains.kotlin.plugin.compose")     // paywall snapshots render @Composable content
 *     alias(libs.plugins.paparazzi)                  // opt in to snapshot testing (your version)
 *     id("com.revenuecat.purchases.paywallfixtures")
 * }
 * // REVENUECAT_API_KEY=<public sdk key> ./gradlew recordPaywallFixtures
 * ```
 *
 * When Paparazzi is applied, the plugin adds the `purchases-ui-testing` test dependency (version matched
 * to this plugin) and enables `testOptions.unitTests.isIncludeAndroidResources`. Without Paparazzi (e.g.
 * if you use Roborazzi instead, or only want to record fixtures), none of that happens — you stay lean
 * and fully control your screenshot tooling.
 *
 * The auto-wiring can be disabled even when Paparazzi is present with
 * `revenuecat.paywallFixtures.snapshotTesting=false` in `gradle.properties`.
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

        // React to the consumer's own Paparazzi instead of applying our own, so we never couple them to a
        // Paparazzi version. Applying Paparazzi is the consumer's one-line opt-in. The wiring is deferred
        // to afterEvaluate so the `paywallFixtures { }` DSL flag (evaluated after the plugins block) can
        // be read — which is only possible because we add a dependency / set options rather than apply a
        // plugin (plugin application would be too late in afterEvaluate).
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

    /**
     * The Gradle property overrides the DSL flag when present (handy for CI), otherwise the
     * `paywallFixtures { setupSnapshotTesting = ... }` value is used (default true).
     */
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

    /**
     * Enables `android.testOptions.unitTests.isIncludeAndroidResources`, which Paparazzi/Robolectric need
     * to read resources in unit tests. Done reflectively to avoid a hard dependency on a specific AGP
     * version's DSL types.
     */
    private fun enableAndroidResourcesInUnitTests(project: Project) {
        val android = project.extensions.findByName("android") ?: return
        runCatching {
            val testOptions = android.javaClass.getMethod("getTestOptions").invoke(android)
            val unitTests = testOptions.javaClass.getMethod("getUnitTests").invoke(testOptions)
            unitTests.javaClass
                .getMethod("setIncludeAndroidResources", Boolean::class.javaPrimitiveType)
                .invoke(unitTests, true)
        }.onFailure {
            project.logger.warn(
                "paywallFixtures: couldn't enable testOptions.unitTests.isIncludeAndroidResources " +
                    "automatically. Set it manually for paywall snapshot tests.",
                it,
            )
        }
    }

    // Called from within afterEvaluate, so check directly rather than registering another afterEvaluate.
    private fun warnIfComposeMissing(project: Project) {
        if (!project.pluginManager.hasPlugin(COMPOSE_COMPILER_PLUGIN_ID)) {
            project.logger.warn(
                "paywallFixtures: the Compose compiler plugin ($COMPOSE_COMPILER_PLUGIN_ID) is not " +
                    "applied. Paywall snapshot tests render @Composable content, so the test module " +
                    "needs it (and buildFeatures.compose = true).",
            )
        }
    }
}
