package com.revenuecat.purchases.google

import android.os.Handler
import android.os.Looper
import android.os.Process
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

/** Explicitly retries the billing connection after stopping Play Store churn, before the SDK timeout. */
@RunWith(AndroidJUnit4::class)
class ConnectionRecoveryReproTest {
    @Test
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun pendingRequestsCompleteAfterReconnection() {
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
                wrapper.queryProductDetailsAsync(
                    ProductType.SUBS,
                    setOf("incident.repro.product"),
                    {
                        results.add("success")
                        pending.countDown()
                    },
                    { error ->
                        val timeout = error.code == PurchasesErrorCode.StoreProblemError &&
                            error.underlyingErrorMessage == BillingStrings.BILLING_PENDING_REQUEST_TIMED_OUT
                        val unavailable = error.code == PurchasesErrorCode.PurchaseNotAllowedError &&
                            error.underlyingErrorMessage?.contains("BILLING_UNAVAILABLE") == true
                        results.add(
                            when {
                                timeout -> "timeout"
                                unavailable -> "billing unavailable"
                                else -> "unexpected error: ${error.code}: ${error.underlyingErrorMessage}"
                            },
                        )
                        pending.countDown()
                    },
                )
            }
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4)
            var serviceDied = false
            while (System.nanoTime() < deadline && pending.count == 2L && !serviceDied) {
                val output = automation.executeShellCommand("logcat -d --pid=${Process.myPid()} -s BillingClient:W *:S")
                serviceDied = output.use { FileInputStream(it.fileDescriptor).bufferedReader().readText() }
                    .contains("Billing service died.")
                if (!serviceDied) Thread.sleep(100)
            }
            stopChurn.set(true)
            churn.join()
            check(serviceDied && pending.count == 2L) { "Reconnection race may not have reproduced: $results" }
            Log.i("RC_RECOVERY", "Service died with two requests pending; starting a new connection")
            wrapper.startConnection()
            check(pending.await(8, TimeUnit.SECONDS)) { "Requests did not complete after reconnecting" }
            Log.i("RC_RECOVERY", "Results after reconnecting: $results")
            check(results.all { it == "success" || it == "billing unavailable" }) {
                "Requests did not receive a successful or BILLING_UNAVAILABLE response after reconnecting: $results"
            }
        } finally {
            stopChurn.set(true)
            if (churn.isAlive) churn.join()
            wrapper.close()
        }
    }
}
