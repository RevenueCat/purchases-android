package com.revenuecat.purchases.ui.revenuecatui.testing

import android.content.Context
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.utils.FixtureOfferingsFactory
import org.json.JSONObject
import java.io.InputStream
import java.net.URI

/**
 * Loads RevenueCat paywall fixtures from the test classpath, so that server-driven paywalls can be
 * rendered deterministically in JVM screenshot tests (e.g. Paparazzi or Roborazzi) without configuring
 * the [Purchases] SDK, network access, or Google Play Billing.
 *
 * Fixtures are typically recorded with the `recordPaywallFixtures` Gradle task and committed to your
 * repo under `src/test/resources/revenuecat-paywall-fixtures`. A fixture directory contains:
 * - `offerings.json` — the offerings backend response, including the paywall configuration.
 * - Image files, mirrored from their CDN URLs.
 *
 * Typical usage with Paparazzi:
 * ```kotlin
 * private val fixtures = PaywallFixtures.load()
 *
 * @Test
 * fun defaultPaywall() {
 *     paparazzi.snapshot {
 *         PaywallFixtureView(fixtures.offering())
 *     }
 * }
 * ```
 *
 * Note: loading fixtures replaces [Purchases.logHandler] with a println-based handler, because the
 * default handler uses `android.util.Log`, which is unavailable in non-Robolectric JVM tests.
 */
public class PaywallFixtures private constructor(
    private val offeringsJson: JSONObject,
    private val resourcesRoot: String,
) {

    public companion object {
        /** The default classpath directory that fixtures are loaded from. */
        public const val DEFAULT_RESOURCES_ROOT: String = "revenuecat-paywall-fixtures"

        private const val OFFERINGS_FILE_NAME = "offerings.json"
        private const val PACKAGES_FILE_NAME = "packages.json"

        /**
         * Loads fixtures from the given classpath directory (relative to the classpath root), typically
         * `src/test/resources/[resourcesRoot]` in your project.
         *
         * @throws IllegalStateException if no `offerings.json` is found at that location.
         */
        @JvmStatic
        @JvmOverloads
        public fun load(resourcesRoot: String = DEFAULT_RESOURCES_ROOT): PaywallFixtures {
            Purchases.logHandler = PrintLnLogHandler
            val offeringsJsonString = readResourceAsString(resourcesRoot, OFFERINGS_FILE_NAME)
                ?: error(
                    "No paywall fixture found at classpath location '/$resourcesRoot/$OFFERINGS_FILE_NAME'. " +
                        "Record fixtures with the recordPaywallFixtures Gradle task, or pass the directory " +
                        "they were recorded to as 'resourcesRoot'.",
                )
            val offeringsJson = JSONObject(offeringsJsonString)
            mergeSharedPackagesIfNeeded(offeringsJson, resourcesRoot)
            return PaywallFixtures(offeringsJson, resourcesRoot)
        }

        private fun readResourceAsString(resourcesRoot: String, fileName: String): String? =
            PaywallFixtures::class.java.getResourceAsStream("/$resourcesRoot/$fileName")
                ?.use { it.readBytes().decodeToString() }

        /**
         * Fixture directories may contain a shared `packages.json` instead of inlining packages in each
         * offering (the format used by RevenueCat's internal paywall-preview-resources). Merge it into
         * any offering that has no packages of its own.
         */
        private fun mergeSharedPackagesIfNeeded(offeringsJson: JSONObject, resourcesRoot: String) {
            val packagesJsonString = readResourceAsString(resourcesRoot, PACKAGES_FILE_NAME) ?: return
            val packagesArray = JSONObject(packagesJsonString).getJSONArray("packages")
            val offerings = offeringsJson.getJSONArray("offerings")
            for (i in 0 until offerings.length()) {
                val offering = offerings.getJSONObject(i)
                if (!offering.has("packages")) {
                    offering.put("packages", packagesArray)
                }
            }
        }
    }

    /**
     * The identifiers of all offerings in this fixture that have a components paywall, sorted
     * alphabetically. Useful for parameterized tests that snapshot every paywall.
     */
    public val offeringIds: List<String> = run {
        val offerings = offeringsJson.getJSONArray("offerings")
        (0 until offerings.length())
            .map { offerings.getJSONObject(it) }
            .filter { it.has("paywall_components") }
            .map { it.getString("identifier") }
            .sorted()
    }

    /**
     * Builds the [Offering] with the given identifier (or the fixture's current offering when null),
     * ready to be rendered with [PaywallFixtureView].
     *
     * Because product prices are not part of the offerings backend response (they come from Google Play
     * Billing at runtime), products are fabricated with fixed default prices. Pass [products] to control
     * the prices shown in snapshots: each [TestStoreProduct] replaces the fabricated product whose
     * package has a matching `platform_product_identifier` (the [TestStoreProduct.id]).
     *
     * @throws IllegalArgumentException if [offeringId] does not exist in the fixture.
     * @throws IllegalStateException if the resolved offering has no components paywall.
     */
    @JvmOverloads
    public fun offering(
        offeringId: String? = null,
        products: List<TestStoreProduct> = emptyList(),
    ): PaywallFixtureOffering {
        val offerings = FixtureOfferingsFactory.createOfferings(
            offeringsJson = offeringsJson,
            productOverridesByStoreId = products.associateBy { it.id },
        )
        val offering = if (offeringId != null) {
            requireNotNull(offerings.all[offeringId]) {
                "No offering with identifier '$offeringId' found in the fixture. " +
                    "Available offerings: ${offerings.all.keys.sorted()}."
            }
        } else {
            checkNotNull(offerings.current) {
                "The fixture has no current offering. Pass an offeringId explicitly. " +
                    "Available offerings: ${offerings.all.keys.sorted()}."
            }
        }
        check(offering.paywallComponents != null) {
            "Offering '${offering.identifier}' has no components paywall. Only Paywalls V2 (components) " +
                "are supported by this kit. If it should have one, the fixture may have been recorded " +
                "with paywall features this SDK version cannot parse — try re-recording the fixture or " +
                "upgrading the SDK. Offerings with a components paywall in this fixture: $offeringIds."
        }
        return PaywallFixtureOffering(
            offering = offering,
            imageResolver = classpathImageResolver(resourcesRoot),
        )
    }

    private object PrintLnLogHandler : LogHandler {
        override fun v(tag: String, msg: String): Unit = println("V [$tag]: $msg")
        override fun d(tag: String, msg: String): Unit = println("D [$tag]: $msg")
        override fun i(tag: String, msg: String): Unit = println("I [$tag]: $msg")
        override fun w(tag: String, msg: String): Unit = println("W [$tag]: $msg")
        override fun e(tag: String, msg: String, throwable: Throwable?) {
            println("E [$tag]: $msg")
            throwable?.printStackTrace()
        }
    }
}

/**
 * An [Offering] built from a fixture, ready to be rendered with [PaywallFixtureView].
 */
public class PaywallFixtureOffering internal constructor(
    /**
     * The underlying [Offering]. An escape hatch for advanced use cases; most tests only need to pass
     * this object to [PaywallFixtureView].
     */
    public val offering: Offering,
    internal val imageResolver: (url: String) -> InputStream?,
) {

    /**
     * Validates the components paywall without rendering it, returning the list of validation error
     * messages. An empty list means the paywall is valid. Useful as a fast, non-visual assertion:
     * ```kotlin
     * assertThat(fixtures.offering().validate(context)).isEmpty()
     * ```
     */
    public fun validate(context: Context): List<String> =
        validateComponentsPaywallForTesting(offering, context)
}

/**
 * Resolves an image URL to a classpath resource recorded by the recordPaywallFixtures Gradle task:
 * the host (minus its TLD) is reversed and prepended to the URL path, mirroring the layout used by
 * RevenueCat's paywall preview resources. For example, with [resourcesRoot] `revenuecat-paywall-fixtures`,
 * `https://assets.pawwalls.com/header.webp` resolves to
 * `/revenuecat-paywall-fixtures/pawwalls/assets/header.webp`.
 */
private fun classpathImageResolver(resourcesRoot: String): (url: String) -> InputStream? = { url ->
    val uri = URI(url)
    val reversedHost = uri.host.split('.').dropLast(1).reversed().joinToString("/")
    PaywallFixtures::class.java.getResourceAsStream("/$resourcesRoot/$reversedHost${uri.path}")
}
