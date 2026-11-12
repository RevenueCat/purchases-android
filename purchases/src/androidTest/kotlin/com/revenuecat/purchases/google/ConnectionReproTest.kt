package com.revenuecat.purchases.google

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesState
import com.revenuecat.purchases.PurchasesStateCache
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.caching.DeviceCache
import io.mockk.mockk
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Manual repro for https://github.com/RevenueCat/purchases-android/issues/3922.
 *
 * Install the defaults debug Android test APK on a dedicated Play Store emulator. In one terminal, run:
 * `while true; do adb -s "$SERIAL" shell am force-stop com.android.vending; sleep 0.15; done`
 * In another terminal, run:
 * `adb -s "$SERIAL" shell am instrument -w -e billingRepro true -e class \
 * com.revenuecat.purchases.google.ConnectionReproTest \
 * com.revenuecat.purchases.integrationTest/androidx.test.runner.AndroidJUnitRunner`
 * Stop the force-stop loop after the test. Check logcat for `Billing service died` and `RC_REPRO`.
 * A normal BILLING_UNAVAILABLE response doesn't reproduce the race; repeat until the service dies.
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
        try {
            wrapper.startConnection()
            repeat(2) { index ->
                wrapper.queryProductDetailsAsync(
                    ProductType.SUBS,
                    setOf("incident.repro.product"),
                    {
                        Log.i("RC_REPRO", "request=$index success")
                        latch.countDown()
                    },
                    {
                        Log.i("RC_REPRO", "request=$index error=${it.code} detail=${it.underlyingErrorMessage}")
                        latch.countDown()
                    },
                )
            }
            val completed = latch.await(25, TimeUnit.SECONDS)
            Log.i("RC_REPRO", "completed=$completed remaining=${latch.count}")
            check(completed) { "Billing requests did not complete within 25 seconds" }
        } finally {
            wrapper.close()
        }
    }
}
