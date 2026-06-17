package com.revenuecat.purchases.paywallfixtures

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

/**
 * Configuration for the `recordPaywallFixtures` task.
 */
public abstract class PaywallFixturesExtension {

    /**
     * The public SDK API key of your RevenueCat app (the same key you pass to `Purchases.configure`).
     * Defaults to the `REVENUECAT_API_KEY` environment variable. Do not commit API keys to your repo.
     */
    public abstract val apiKey: Property<String>

    /**
     * The app user ID used to fetch offerings. Only relevant if your project serves different paywalls
     * to different users (e.g. targeting rules). Defaults to `paywall-fixture-recorder`.
     */
    public abstract val appUserId: Property<String>

    /**
     * Identifiers of the offerings to record. When empty (the default), all offerings that have a
     * components paywall are recorded.
     */
    public abstract val offerings: SetProperty<String>

    /**
     * The RevenueCat API base URL. Only override this for testing.
     */
    public abstract val baseUrl: Property<String>

    /**
     * Where to write the recorded fixtures. Defaults to `src/test/resources/revenuecat-paywall-fixtures`,
     * which is where `PaywallFixtures.load()` looks by default.
     */
    public abstract val outputDirectory: DirectoryProperty

    /**
     * When true (the default) and Paparazzi is also applied, the plugin auto-adds the
     * `purchases-ui-testing` test dependency and enables `testOptions.unitTests.isIncludeAndroidResources`.
     * Set to false to keep only the `recordPaywallFixtures` task and wire those up yourself (e.g. to use
     * Roborazzi, or to control the dependency).
     *
     * The Gradle property `revenuecat.paywallFixtures.snapshotTesting`, if set, overrides this — handy
     * for toggling on CI without editing the build script.
     */
    public abstract val setupSnapshotTesting: Property<Boolean>
}
