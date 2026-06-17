package com.revenuecat.purchases.paywallfixtures

import com.revenuecat.purchases.paywallfixtures.tasks.RecordPaywallFixturesTask
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin that records RevenueCat paywall fixtures for offline paywall snapshot testing with the
 * `purchases-ui-testing` artifact.
 *
 * Apply the plugin and configure the `paywallFixtures` extension:
 * ```kotlin
 * plugins {
 *     id("com.revenuecat.purchases.paywallfixtures")
 * }
 *
 * paywallFixtures {
 *     // Defaults to the REVENUECAT_API_KEY environment variable.
 *     apiKey.set(providers.gradleProperty("revenuecatApiKey"))
 * }
 * ```
 *
 * Then run `./gradlew recordPaywallFixtures` whenever the paywall changes on the RevenueCat dashboard.
 * The recorded fixtures (offerings JSON + images) are written to `src/test/resources/` so they can be
 * committed and loaded by `PaywallFixtures.load()` in tests.
 */
public class PaywallFixturesPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("paywallFixtures", PaywallFixturesExtension::class.java)
        extension.apiKey.convention(project.providers.environmentVariable("REVENUECAT_API_KEY"))
        extension.appUserId.convention("paywall-fixture-recorder")
        extension.baseUrl.convention("https://api.revenuecat.com")
        extension.outputDirectory.convention(
            project.layout.projectDirectory.dir("src/test/resources/revenuecat-paywall-fixtures"),
        )

        project.tasks.register("recordPaywallFixtures", RecordPaywallFixturesTask::class.java) { task ->
            task.apiKey.set(extension.apiKey)
            task.appUserId.set(extension.appUserId)
            task.offerings.set(extension.offerings)
            task.baseUrl.set(extension.baseUrl)
            task.outputDirectory.set(extension.outputDirectory)
        }
    }
}
