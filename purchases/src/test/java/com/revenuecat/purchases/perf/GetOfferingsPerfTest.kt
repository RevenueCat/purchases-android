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

    private fun countByEndpoint(paths: List<String>) = mapOf(
        "config" to paths.count { it.contains("/config") },
        "offerings" to paths.count { it.contains("/offerings") },
        "products" to paths.count { it.contains("/products") },
    )

    // Observed request sequence on the default (remote-config-on) path, cold start:
    //   [/v1/subscribers/{id}/offerings, /rcbilling/v1/subscribers/{id}/products?id=..., /v1/config/app]
    // -> 3 requests total: config x1, offerings x1, products x1. This is the regression anchor: a
    // future change that adds a round trip on the default getOfferings path fails this test
    // regardless of how fast or slow the machine running it is.
    @Test
    fun defaultPathMakesExactlyTheExpectedRoundTrips() {
        // server.url(...) auto-starts the server; no explicit server.start().
        server.dispatcher = PerfFixtures.dispatcher(server.url("/").toString())

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
    fun warmCycleStillReturnsOfferings() {
        // server.url(...) auto-starts the server; no explicit server.start().
        server.dispatcher = PerfFixtures.dispatcher(server.url("/").toString())
        // Prime cache (cold), then run a warm cycle reusing state.
        val cold = harness().runCycle(cold = true)
        val warm = PerfHarness(context, server).runCycle(cold = false)
        // Elapsed time is printed for humans only — never asserted. This SDK's warm path is not a
        // pure cache read; it revalidates via conditional requests, so wall-clock comparisons
        // between cold and warm are noisy and machine-speed-dependent. The robust signal here is
        // resilience: the warm path still returns offerings.
        println("PERF_WARM cold=${cold.elapsedMs}ms warm=${warm.elapsedMs}ms")
        assertThat(warm.error).isNull()
        assertThat(warm.offeringsCount).isGreaterThan(0)
    }

    @Test
    fun failingNonCriticalConfigSyncStillReturnsOfferings() {
        // The remote-config /config sync is best-effort: if it fails, getOfferings must still return offerings.
        // server.url(...) auto-starts the server; no explicit server.start().
        server.dispatcher = PerfFixtures.dispatcher(server.url("/").toString()).withPathFailure("/config")
        val result = harness().runCycle(cold = true)
        assertThat(result.error).isNull()
        assertThat(result.offeringsCount).isGreaterThan(0)
    }
}
