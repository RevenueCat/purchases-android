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

@RunWith(RobolectricTestRunner::class)
@OptIn(InternalRevenueCatAPI::class)
class PerfHarnessTest {
    private val server = MockWebServer()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After fun tearDown() {
        Purchases.backingFieldSharedInstance?.close()
        Purchases.proxyURL = null
        server.shutdown()
    }

    @Test
    fun coldFeatureCycleSucceedsAndHitsConfig() {
        // server.url("/") already starts the server as a side effect (MockWebServer.before()), so no
        // separate server.start() call is needed here — calling it explicitly would throw
        // "start() already called".
        server.dispatcher = PerfFixtures.dispatcher(server.url("/").toString())
        val result = PerfHarness(context, server).runCycle(useWorkflows = true, cold = true)
        assertThat(result.error).isNull()
        assertThat(result.offeringsCount).isGreaterThan(0)
        assertThat(result.requestPaths.any { it.contains("/config") }).isTrue
    }
}
