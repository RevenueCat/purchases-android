package com.revenuecat.purchases.google

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesState
import com.revenuecat.purchases.PurchasesStateCache
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.strings.BillingStrings
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Simulates a stuck client, then verifies that its replacement talks to real Play Billing. */
@RunWith(AndroidJUnit4::class)
class ConnectionRecreationReproTest {
    @Test
    @Suppress("LongMethod")
    fun requestAfterTimeoutUsesFreshBillingClient() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("billingRepro") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val realFactory = BillingWrapper.ClientFactory(instrumentation.targetContext, false)
        val factory = mockk<BillingWrapper.ClientFactory>()
        val oldClient = mockk<BillingClient>()
        val clientsBuilt = AtomicInteger()
        every { oldClient.isReady } returns false
        every { oldClient.connectionState } returns BillingClient.ConnectionState.DISCONNECTED andThen
            BillingClient.ConnectionState.CONNECTING
        every { oldClient.startConnection(any<BillingClientStateListener>()) } just runs
        every { oldClient.endConnection() } just runs
        every { factory.buildClient(any()) } answers {
            if (clientsBuilt.getAndIncrement() == 0) {
                oldClient
            } else {
                realFactory.buildClient(firstArg<PurchasesUpdatedListener>())
            }
        }
        val wrapper = BillingWrapper(
            clientFactory = factory,
            mainHandler = Handler(Looper.getMainLooper()),
            deviceCache = mockk<DeviceCache>(relaxed = true),
            diagnosticsTrackerIfEnabled = null,
            purchasesStateProvider = PurchasesStateCache(PurchasesState()),
        )
        wrapper.purchasesUpdatedListener = mockk<BillingAbstract.PurchasesUpdatedListener>(relaxed = true)
        val pending = CountDownLatch(2)
        val results = Collections.synchronizedList(mutableListOf<String>())
        try {
            wrapper.startConnection()
            val creationDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
            while (wrapper.billingClient == null && System.nanoTime() < creationDeadline) Thread.sleep(50)
            check(wrapper.billingClient === oldClient) { "Simulated stuck client was not created" }
            repeat(2) {
                wrapper.queryProductDetailsAsync(ProductType.SUBS, setOf("incident.repro.product"), {
                    results.add("success")
                    pending.countDown()
                }, { error ->
                    results.add(
                        if (error.code == PurchasesErrorCode.StoreProblemError &&
                            error.underlyingErrorMessage == BillingStrings.BILLING_PENDING_REQUEST_TIMED_OUT
                        ) {
                            "timeout"
                        } else {
                            "other error"
                        },
                    )
                    pending.countDown()
                })
            }
            check(pending.await(20, TimeUnit.SECONDS) && results == listOf("timeout", "timeout")) {
                "Expected two timeouts from the simulated stuck client: $results"
            }
            val retirementDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
            while (wrapper.billingClient != null && System.nanoTime() < retirementDeadline) Thread.sleep(50)
            check(wrapper.billingClient == null) { "The stuck client was not retired" }
            verify(exactly = 1) { oldClient.endConnection() }
            check(clientsBuilt.get() == 1) { "A new client was created before a new request" }
            val completed = CountDownLatch(1)
            var result = "pending"
            wrapper.queryProductDetailsAsync(ProductType.SUBS, setOf("incident.repro.product"), {
                result = "success"
                completed.countDown()
            }, { error ->
                result = if (error.code == PurchasesErrorCode.PurchaseNotAllowedError &&
                    error.underlyingErrorMessage?.contains("BILLING_UNAVAILABLE") == true
                ) {
                    "billing unavailable"
                } else {
                    "unexpected error: ${error.code}: ${error.underlyingErrorMessage}"
                }
                completed.countDown()
            })
            check(completed.await(10, TimeUnit.SECONDS)) { "Request on fresh client did not complete" }
            check(wrapper.billingClient != null && wrapper.billingClient !== oldClient) { "Client was not recreated" }
            Log.i("RC_RECREATE", "Request after simulated timeout completed on a real fresh client: $result")
            check(result == "success" || result == "billing unavailable") { result }
        } finally {
            wrapper.close()
        }
    }
}
