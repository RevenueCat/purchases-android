/*
 * PERF_SPIKE — Task 1 decision record (see .superpowers/sdd/task-1-report.md for full detail)
 *
 * (a) Did it pass? YES (after two fixes described below — see git history of this file for the
 *     intermediate failing attempts).
 *
 * (b) Observed request sequence (exact stdout line from the passing run):
 *     PERF_SPIKE requests=[/v1/subscribers/perf-spike-user-id/offerings,
 *     /rcbilling/v1/subscribers/perf-spike-user-id/products?id=monthly_freetrial, /v1/config/app]
 *     error=null offerings=1
 *
 *   Two deviations from the brief's assumed 3-request sequence:
 *   - No bare `/v1/subscribers/{id}` (customer info) request happens on this path at all — only the
 *     `/offerings`-suffixed one. Tasks 6-8 should not assume a separate customer-info fetch fires before
 *     getOfferings when an explicit appUserID is passed to configure().
 *   - The simulated ("test_") store resolves products via a *fourth* endpoint —
 *     `Backend.getWebBillingProducts` (`/rcbilling/v1/subscribers/{id}/products?id=...`), which the
 *     brief's three-branch dispatcher does not anticipate. This spike's dispatcher adds a fourth branch
 *     for it (`WEB_BILLING_PRODUCTS_JSON`). Later tasks must account for this request too, or getOfferings
 *     will return zero packages (the offering's package fails to resolve to a product).
 *   - `/v1/config` is actually requested as POST `/v1/config/app` (domain suffix), not a bare `/v1/config`.
 *     The assertion below matches on the `/config` substring so this still holds.
 *
 * (c) test_ key acceptance: ACCEPTED without the release-guard fallback. Under Robolectric,
 *     ApplicationProvider.getApplicationContext().applicationInfo has FLAG_DEBUGGABLE set (the merged
 *     test manifest is debuggable), so `PurchasesFactory.isDebugBuild()` (the default
 *     `DefaultIsDebugBuildProvider`) returned true and `Purchases.configure(...)` worked directly — the
 *     `PurchasesFactory(isDebugBuild = { true })` fallback described in the brief was not needed.
 *
 * Two fixes were required beyond the brief's literal test code:
 *   1. Robolectric's shadow application does not auto-grant manifest permissions, so
 *      `PurchasesFactory.validateConfiguration()`'s explicit `INTERNET` permission check fails unless the
 *      test grants it via `shadowOf(application).grantPermissions(Manifest.permission.INTERNET)`.
 *   2. CRITICAL for tasks 6-8: `getOfferings`'s final callback is delivered via
 *      `OfferingsManager.dispatch`, which posts to a `Handler(Looper.getMainLooper())` whenever the
 *      calling thread isn't already the main thread (true here — offerings/config/products all complete
 *      on background executor threads). Robolectric's default `LooperMode.PAUSED` never auto-drains that
 *      queue, and the JUnit test thread *is* Robolectric's main thread, so a plain blocking
 *      `latch.await()` deadlocks forever: the thread that needs to pump the main Looper is the same one
 *      parked in `await()`. The fix is to poll — `shadowOf(Looper.getMainLooper()).idle()` in a loop
 *      interleaved with short `latch.await(50ms)` calls — so background network I/O keeps progressing
 *      between idle() calls and the posted callback drains once it lands. Every later Robolectric test in
 *      this harness that calls a real async SDK method and blocks on a latch needs this same pattern, not
 *      a single `latch.await(timeout)`.
 *
 * Decision: Robolectric drive works end-to-end (configure + getOfferings through MockWebServer via
 * proxyURL, including /v1/config) once the main-Looper-pump pattern above is used. Proceed with
 * Robolectric for the full harness — no need to fall back to an instrumented :integration-tests harness.
 */
package com.revenuecat.purchases.perf

import android.Manifest
import androidx.test.core.app.ApplicationProvider
import com.revenuecat.purchases.DangerousSettings
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getOfferingsWith
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLog
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@OptIn(InternalRevenueCatAPI::class)
class PerfSpikeTest {

    private val server = MockWebServer()
    private val context = ApplicationProvider.getApplicationContext<android.app.Application>().apply {
        // Robolectric's shadow application does not auto-grant manifest permissions; PurchasesFactory's
        // validateConfiguration() requires INTERNET explicitly (see purchases/src/test/java/com/revenuecat/
        // purchases/BasePurchasesTest.kt for the same pattern).
        shadowOf(this).grantPermissions(Manifest.permission.INTERNET)
    }
    private val apiKey = "test_perfSpikeFakeKey"

    @After
    fun tearDown() {
        Purchases.backingFieldSharedInstance?.close()
        Purchases.proxyURL = null
        server.shutdown()
    }

    @Test
    fun drivesConfigureAndGetOfferingsThroughMock() {
        ShadowLog.stream = System.out
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                return when {
                    // The simulated store resolves products via a separate web-billing-products call;
                    // its path also contains "v1/subscribers" so this branch must be checked first.
                    path.contains("/products") -> MockResponse().setResponseCode(200)
                        .setBody(WEB_BILLING_PRODUCTS_JSON)
                    path.contains("/offerings") -> MockResponse().setResponseCode(200).setBody(OFFERINGS_JSON)
                    path.contains("/config") -> MockResponse().setResponseCode(200).setBody(CONFIG_JSON)
                    path.startsWith("/v1/subscribers") -> MockResponse().setResponseCode(200).setBody(SUBSCRIBER_JSON)
                    else -> MockResponse().setResponseCode(404).setBody("{}")
                }
            }
        }
        server.start()
        Purchases.proxyURL = server.url("/").toUrl()
        Purchases.logLevel = com.revenuecat.purchases.LogLevel.VERBOSE

        Purchases.configure(
            PurchasesConfiguration.Builder(context, apiKey)
                .appUserID("perf-spike-user-id")
                .dangerousSettings(DangerousSettings.forWorkflows())
                .diagnosticsEnabled(false)
                .build(),
        )

        val latch = CountDownLatch(1)
        var received: Offerings? = null
        var error: PurchasesError? = null
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error = it; latch.countDown() },
            onSuccess = { received = it; latch.countDown() },
        )
        // The SDK delivers the final callback via a Handler posted to the main Looper (OfferingsManager.dispatch).
        // Robolectric's default LooperMode.PAUSED never auto-drains that queue, and this test thread *is* the main
        // thread, so a plain latch.await() would block that same thread forever without ever pumping the queued
        // runnable. Poll: idle the main looper (drains anything posted so far) between short latch waits so
        // background-thread network work (which does progress independently) has room to post its callback.
        val deadlineMs = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(15)
        while (latch.count > 0 && System.currentTimeMillis() < deadlineMs) {
            shadowOf(android.os.Looper.getMainLooper()).idle()
            latch.await(50, TimeUnit.MILLISECONDS)
        }
        check(latch.count == 0L) { "getOfferings timed out" }

        // Log the observed request sequence for later tasks.
        val paths = generateSequence { server.takeRequest(0, TimeUnit.MILLISECONDS) }
            .map { it.path }.toList()
        println("PERF_SPIKE requests=$paths error=$error offerings=${received?.all?.size}")

        assertThat(error).isNull()
        assertThat(received!!.all).isNotEmpty
        assertThat(paths.any { it?.contains("/config") == true }).isTrue // proves proxyURL covers /v1/config
    }

    private companion object {
        // language=JSON
        // Smallest valid offerings body: one offering, one package, one product ("offering_a" /
        // "monthly_freetrial"), modeled on ONE_OFFERINGS_RESPONSE in
        // purchases/src/test/java/com/revenuecat/purchases/utils/productStubs.kt.
        const val OFFERINGS_JSON = """
        {
          "offerings": [
            {
              "identifier": "offering_a",
              "description": "This is the base offering",
              "packages": [
                {
                  "identifier": "${'$'}rc_monthly",
                  "platform_product_identifier": "monthly_freetrial",
                  "platform_product_plan_identifier": "p1m"
                }
              ]
            }
          ],
          "current_offering_id": "offering_a"
        }
        """

        // CONFIG_JSON stays as "{}": Backend.getRemoteConfig() treats any 2xx body that isn't RC Container
        // Format as a retryable error, falls back (no fallback base URL configured -> immediate error), and
        // WorkflowManager.onPaywallConfigReady() swallows that failure and calls onComplete() anyway. No
        // valid /v1/config body is needed to unblock getOfferings.
        const val CONFIG_JSON = "{}"

        // language=JSON
        // Minimal valid subscriber (customer info) body — see
        // CustomerInfoFactory.buildCustomerInfo in purchases/src/main/kotlin/com/revenuecat/purchases/common.
        const val SUBSCRIBER_JSON = """
        {
          "request_date": "2019-08-16T10:30:42Z",
          "request_date_ms": 1565951442879,
          "subscriber": {
            "original_app_user_id": "perf-spike-user-id",
            "first_seen": "2019-06-17T16:05:33Z",
            "non_subscriptions": {},
            "subscriptions": {},
            "entitlements": {}
          }
        }
        """

        // language=JSON
        // WebBillingProductsResponse body for the simulated store's product resolution call
        // (Backend.getWebBillingProducts -> SimulatedStoreBillingWrapper.queryProductDetailsAsync). Shape from
        // purchases/src/main/kotlin/com/revenuecat/purchases/common/networking/WebBillingProductsResponse.kt.
        const val WEB_BILLING_PRODUCTS_JSON = """
        {
          "product_details": [
            {
              "identifier": "monthly_freetrial",
              "product_type": "subscription",
              "title": "Monthly",
              "description": "Monthly subscription",
              "default_purchase_option_id": "option1",
              "purchase_options": {
                "option1": {
                  "base": {
                    "price": {"amount_micros": 4990000, "currency": "USD"},
                    "period_duration": "P1M"
                  }
                }
              }
            }
          ]
        }
        """
    }
}
