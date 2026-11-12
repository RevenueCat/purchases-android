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
import io.mockk.mockk
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Manual repro for https://github.com/RevenueCat/purchases-android/issues/3922.
 * Run the CircleCI workflow with pipeline parameter `action=billing_connection_repro`.
 *
 * Install the defaults debug Android test APK on a dedicated Play Store emulator. In one terminal, run:
 * `while true; do adb -s "$SERIAL" shell am force-stop com.android.vending; sleep 0.15; done`
 * In another terminal, run:
 * `adb -s "$SERIAL" shell am instrument -w -e billingRepro true -e class \
 * com.revenuecat.purchases.google.ConnectionReproTest \
 * com.revenuecat.purchases.integrationTest/androidx.test.runner.AndroidJUnitRunner`
 * Stop the force-stop loop after the test. Check logcat for `Billing service died` and `RC_REPRO`.
 * The test fails if both requests do not time out or logcat does not show the service death.
 * Retry if the race was not triggered.
 */
@RunWith(AndroidJUnit4::class)
class ConnectionReproTest {
    @Test
    fun pendingRequestsCompleteWhenBillingServiceDies() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("billingRepro") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val wrapper = BillingWrapper(
            clientFactory = BillingWrapper.ClientFactory(context, false),
            mainHandler = Handler(Looper.getMainLooper()),
            deviceCache = mockk<DeviceCache>(relaxed = true),
            diagnosticsTrackerIfEnabled = null,
            purchasesStateProvider = PurchasesStateCache(PurchasesState()),
        )
        wrapper.purchasesUpdatedListener = mockk<BillingAbstract.PurchasesUpdatedListener>(relaxed = true)
        val latch = CountDownLatch(2)
        val results = Collections.synchronizedList(mutableListOf<String>())
        try {
            wrapper.startConnection()
            repeat(2) { index ->
                wrapper.queryProductDetailsAsync(
                    ProductType.SUBS,
                    setOf("incident.repro.product"),
                    {
                        Log.i("RC_REPRO", "request=$index success")
                        results.add("success")
                        latch.countDown()
                    },
                    {
                        Log.i("RC_REPRO", "request=$index error=${it.code} detail=${it.underlyingErrorMessage}")
                        results.add(
                            if (it.code == PurchasesErrorCode.StoreProblemError &&
                                it.underlyingErrorMessage == "Timed out waiting for the billing service to connect."
                            ) {
                                "timeout"
                            } else {
                                "${it.code}: ${it.underlyingErrorMessage}"
                            },
                        )
                        latch.countDown()
                    },
                )
            }
            val completed = latch.await(25, TimeUnit.SECONDS)
            Log.i("RC_REPRO", "completed=$completed remaining=${latch.count}")
            check(completed) { "Billing requests did not complete within 25 seconds" }
            check(results == listOf("timeout", "timeout")) {
                "Expected two connection timeouts; race may not have reproduced: $results"
            }
            val logcat = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "logcat -d --pid=${Process.myPid()} -s BillingClient:W *:S",
            )
            val logs = logcat.use { FileInputStream(it.fileDescriptor).bufferedReader().readText() }
            check("Billing service died." in logs) { "Billing service death was not observed in logcat" }
        } finally {
            wrapper.close()
        }
    }
}
