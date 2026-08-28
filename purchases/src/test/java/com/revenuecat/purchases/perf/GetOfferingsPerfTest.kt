package com.revenuecat.purchases.perf

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import okhttp3.mockwebserver.Dispatcher
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
        private const val CONFIG_DELAY_MS = 1500L

        // The /config fetch is triggered lazily from getOfferings' success path and awaited by the
        // paywall-config readiness gate, so it costs roughly one serial round trip. Budget 1.5x the
        // injected delay: a second serial config hop (or a retry storm) would cost ~2x and trip this.
        private const val CONFIG_ROUND_TRIP_BUDGET_MS = 2250L
        private const val SAMPLES = 3
    }

    private val server = MockWebServer()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun harness() = PerfHarness(context, server)

    private fun medianElapsedMs(dispatcher: Dispatcher): Long {
        val samples = (1..SAMPLES).map {
            Purchases.backingFieldSharedInstance?.close()
            server.dispatcher = dispatcher
            harness().runCycle(cold = true).also {
                assertThat(it.error).isNull()
                assertThat(it.offeringsCount).isGreaterThan(0)
            }.elapsedMs
        }.sorted()
        return samples[samples.size / 2]
    }

    @After fun tearDown() {
        Purchases.backingFieldSharedInstance?.close()
        Purchases.proxyURL = null
        server.shutdown()
    }

    private fun countByEndpoint(paths: List<String>) = mapOf(
        "config" to paths.count { it.contains("/config") },
        "offerings" to paths.count { it.contains("/offerings") },
        "products" to paths.count { it.contains("/products") },
    )

    // Observed request sequence on the default (remote-config-on) path, GOOD network, cold start:
    //   [/v1/subscribers/{id}/offerings, /rcbilling/v1/subscribers/{id}/products?id=..., /v1/config/app]
    // -> 3 requests total: config x1, offerings x1, products x1. This is the regression anchor: a
    // future change that adds a serial round trip on the default getOfferings path fails this test
    // regardless of how fast or slow the machine running it is.
    @Test
    fun defaultPathMakesExactlyTheExpectedRoundTrips() {
        // server.url(...) auto-starts the server; no explicit server.start().
        server.dispatcher = NetworkProfile.GOOD.decorate(PerfFixtures.dispatcher(server.url("/").toString()))

        val result = harness().runCycle(cold = true)
        println(result.requestPaths)

        assertThat(result.error).isNull()
        assertThat(result.offeringsCount).isGreaterThan(0)

        val counts = countByEndpoint(result.requestPaths)
        assertThat(counts["config"]).isEqualTo(1)
        assertThat(counts["offerings"]).isEqualTo(1)
        assertThat(counts["products"]).isEqualTo(1)
        assertThat(result.requestPaths.size).isEqualTo(3)
    }

    @Test
    fun configFetchContributesAtMostOneSerialRoundTrip() {
        val base = server.url("/").toString() // auto-starts the server
        val fast = PerfFixtures.dispatcher(base)
        val configDelayed = PerfFixtures.dispatcher(base).withPathDelay("/config", CONFIG_DELAY_MS)

        val fastMs = medianElapsedMs(fast)
        val delayedMs = medianElapsedMs(configDelayed)
        val delta = delayedMs - fastMs

        println(
            "PERF_CONFIG_COST fast=${fastMs}ms configDelayed=${delayedMs}ms delta=${delta}ms " +
                "(injected /config delay=${CONFIG_DELAY_MS}ms, budget=${CONFIG_ROUND_TRIP_BUDGET_MS}ms)",
        )
        // Same configuration, same run, same machine — only the /config response delay differs, so the
        // delta isolates how much of that one endpoint's latency reaches getOfferings. Machine speed
        // cancels; the injected delay dominates.
        assertThat(delta)
            .withFailMessage(
                "getOfferings grew %d ms when /config was delayed %d ms — the remote-config fetch now " +
                    "costs more than ~one serial round trip on the getOfferings critical path.",
                delta, CONFIG_DELAY_MS,
            )
            .isLessThan(CONFIG_ROUND_TRIP_BUDGET_MS)
    }

    @Test
    fun warmGetOfferingsUnderBadNetworkStillReturnsOfferings() {
        // server.url(...) auto-starts the server; no explicit server.start().
        server.dispatcher = NetworkProfile.BAD.decorate(PerfFixtures.dispatcher(server.url("/").toString()))
        // Prime cache (cold), then run a warm cycle reusing state.
        val cold = harness().runCycle(cold = true)
        val warm = PerfHarness(context, server).runCycle(cold = false)
        // Robust signal = resilience: the warm path still returns offerings under bad network.
        // We deliberately do NOT assert warm is faster than cold: this SDK's warm path is not a
        // pure cache read — it revalidates via conditional requests and issues the same round-trip
        // shape as cold, so warm ≈ cold and a `warm < cold` comparison of two near-equal,
        // high-variance timings would flake — the exact failure mode this design avoids. Timing is
        // logged for humans, not asserted.
        println("PERF_WARM cold=${cold.elapsedMs}ms warm=${warm.elapsedMs}ms")
        assertThat(warm.error).isNull()
        assertThat(warm.offeringsCount).isGreaterThan(0)
    }

    @Test
    fun failingConfigSyncStillReturnsOfferings() {
        // The remote-config /config sync is best-effort: if it fails, getOfferings must still return offerings.
        // server.url(...) auto-starts the server; no explicit server.start().
        server.dispatcher = NetworkProfile.FLAKY.decorate(
            PerfFixtures.dispatcher(server.url("/").toString()),
            failMatch = "/config",
        )
        val result = harness().runCycle(cold = true)
        assertThat(result.error).isNull()
        assertThat(result.offeringsCount).isGreaterThan(0)
    }
}
