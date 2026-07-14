package com.revenuecat.purchases.perf

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// NOTE (from Task 5): `Purchases.backingFieldSharedInstance` is an internal companion member —
// access it as `Purchases.backingFieldSharedInstance`, do NOT add an import for it (won't compile).
// NOTE (from Task 5): `server.url(...)` auto-starts MockWebServer, so do NOT also call `server.start()`
// after building the dispatcher from `server.url(...)` — the redundant start() throws.
@RunWith(RobolectricTestRunner::class)
@OptIn(InternalRevenueCatAPI::class)
class GetOfferingsPerfTest {
    private val server = MockWebServer()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun harness() = PerfHarness(context, server)

    @After fun tearDown() {
        Purchases.backingFieldSharedInstance?.close()
        Purchases.proxyURL = null
        server.shutdown()
    }

    // Anchors confirmed by the Task 1 spike:
    //   baseline (no workflows): [/offerings, /products]                -> size 2, config 0
    //   feature  (forWorkflows): [/offerings, /products, /config/app]   -> size 3, config 1
    private fun countByEndpoint(paths: List<String>) = mapOf(
        "config" to paths.count { it.contains("/config") },
        "offerings" to paths.count { it.contains("/offerings") },
        "products" to paths.count { it.contains("/products") },
    )

    @Test
    fun featurePathHitsConfigOnceAndOfferingsOnce_noAddedSerialRoundTrips() {
        // server.url(...) auto-starts the server; no explicit server.start().
        server.dispatcher = NetworkProfile.GOOD.decorate(PerfFixtures.dispatcher(server.url("/").toString()))

        val baseline = harness().runCycle(useWorkflows = false, cold = true)
        Purchases.backingFieldSharedInstance?.close()
        val feature = harness().runCycle(useWorkflows = true, cold = true)

        assertThat(baseline.error).isNull()
        assertThat(feature.error).isNull()

        // Legacy path must NOT hit /config; feature path hits it exactly once.
        assertThat(countByEndpoint(baseline.paths())["config"]).isEqualTo(0)
        assertThat(countByEndpoint(feature.paths())["config"]).isEqualTo(1)
        // Offerings and product-resolution are each a single round-trip on both paths.
        assertThat(countByEndpoint(baseline.paths())["offerings"]).isEqualTo(1)
        assertThat(countByEndpoint(feature.paths())["offerings"]).isEqualTo(1)
        assertThat(countByEndpoint(baseline.paths())["products"]).isEqualTo(1)
        assertThat(countByEndpoint(feature.paths())["products"]).isEqualTo(1)
        // Feature adds exactly the config sync over baseline — no other extra requests.
        assertThat(feature.paths().size).isEqualTo(baseline.paths().size + 1)
    }
}

private fun CycleResult.paths() = requestPaths
