package com.revenuecat.purchases.google

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesState
import com.revenuecat.purchases.PurchasesStateCache
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.strings.BillingStrings
import io.mockk.mockk
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Exercises a fresh billing client after a stuck connection has timed out. */
@RunWith(AndroidJUnit4::class)
class ConnectionRecreationReproTest {
    @Test
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun requestAfterTimeoutUsesFreshBillingClient() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("billingRepro") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        val wrapper = BillingWrapper(
            clientFactory = BillingWrapper.ClientFactory(instrumentation.targetContext, false),
            mainHandler = Handler(Looper.getMainLooper()),
            deviceCache = mockk<DeviceCache>(relaxed = true),
            diagnosticsTrackerIfEnabled = null,
            purchasesStateProvider = PurchasesStateCache(PurchasesState()),
        )
        wrapper.purchasesUpdatedListener = mockk<BillingAbstract.PurchasesUpdatedListener>(relaxed = true)
        val stopChurn = AtomicBoolean(false)
        val churn = Thread {
            while (!stopChurn.get()) {
                automation.executeShellCommand("am force-stop com.android.vending").use {
                    FileInputStream(it.fileDescriptor).bufferedReader().readText()
                }
                Thread.sleep(30)
            }
        }
        val pending = CountDownLatch(2)
        val results = Collections.synchronizedList(mutableListOf<String>())
        try {
            churn.start()
            Thread.sleep(100)
            wrapper.startConnection()
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
            val creationDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
            while (wrapper.billingClient == null && System.nanoTime() < creationDeadline) Thread.sleep(50)
            val oldClient = wrapper.billingClient
            check(oldClient != null) { "Recreation race may not have reproduced: no initial client" }
            check(pending.await(20, TimeUnit.SECONDS) && results == listOf("timeout", "timeout")) {
                "Recreation race may not have reproduced: $results"
            }
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
            while (wrapper.billingClient != null && System.nanoTime() < deadline) Thread.sleep(50)
            check(wrapper.billingClient == null) { "Recreation race may not have reproduced: client was not retired" }
            stopChurn.set(true)
            churn.join()
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
            Log.i("RC_RECREATE", "Request after timeout completed on a fresh client: $result")
            check(result == "success" || result == "billing unavailable") { result }
        } finally {
            stopChurn.set(true)
            if (churn.isAlive) churn.join()
            wrapper.close()
        }
    }
}
