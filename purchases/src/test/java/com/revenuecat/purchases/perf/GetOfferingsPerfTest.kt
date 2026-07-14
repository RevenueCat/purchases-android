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
    companion object {
        private const val RATIO_BOUND = 3.0
    }

    private val server = MockWebServer()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun harness() = PerfHarness(context, server)

    private fun medianElapsedMs(useWorkflows: Boolean, iterations: Int = 5): Long {
        val samples = (1..iterations).map {
            Purchases.backingFieldSharedInstance?.close()
            harness().runCycle(useWorkflows = useWorkflows, cold = true).also {
                assertThat(it.error).isNull()
            }.elapsedMs
        }.sorted()
        return samples[samples.size / 2]
    }

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

    @Test
    fun featureVsBaselineRatioUnderBadNetworkStaysBounded() {
        // server.url(...) auto-starts the server; no explicit server.start().
        server.dispatcher = NetworkProfile.BAD.decorate(PerfFixtures.dispatcher(server.url("/").toString()))

        val baseline = medianElapsedMs(useWorkflows = false)
        val feature = medianElapsedMs(useWorkflows = true)
        val ratio = feature.toDouble() / baseline.toDouble()

        println("PERF_RATIO baseline=${baseline}ms feature=${feature}ms ratio=$ratio")
        assertThat(ratio)
            .withFailMessage("feature/baseline getOfferings ratio %.2f exceeded bound %.2f (regression?)", ratio, RATIO_BOUND)
            .isLessThan(RATIO_BOUND)
    }

    @Test
    fun warmGetOfferingsUnderBadNetworkServesFromCacheFast() {
        // server.url(...) auto-starts the server; no explicit server.start().
        server.dispatcher = NetworkProfile.BAD.decorate(PerfFixtures.dispatcher(server.url("/").toString()))
        // Prime cache (cold), then measure a warm cycle reusing state.
        val cold = harness().runCycle(useWorkflows = true, cold = true)
        val warm = PerfHarness(context, server).runCycle(useWorkflows = true, cold = false)
        println("PERF_WARM cold=${cold.elapsedMs}ms warm=${warm.elapsedMs}ms")
        assertThat(warm.error).isNull()
        assertThat(warm.offeringsCount).isGreaterThan(0)
        // The SDK's "warm" path is not a pure cache read — it revalidates via conditional
        // requests, so it does not stay under a single request delay (NetworkProfile.BAD.
        // perRequestDelayMs). Instead, assert it beats a full cold cycle, which proves the
        // cache still helps even though it does not eliminate the round trip entirely.
        assertThat(warm.elapsedMs).isLessThan(cold.elapsedMs)
    }

    @Test
    fun failingNonCriticalConfigSyncStillReturnsOfferings() {
        // The workflow /config sync is best-effort: if it fails, getOfferings must still return offerings.
        // server.url(...) auto-starts the server; no explicit server.start().
        server.dispatcher = NetworkProfile.FLAKY.decorate(
            PerfFixtures.dispatcher(server.url("/").toString()),
            failMatch = "/config",
        )
        val result = harness().runCycle(useWorkflows = true, cold = true)
        assertThat(result.error).isNull()
        assertThat(result.offeringsCount).isGreaterThan(0)
    }
}

private fun CycleResult.paths() = requestPaths
