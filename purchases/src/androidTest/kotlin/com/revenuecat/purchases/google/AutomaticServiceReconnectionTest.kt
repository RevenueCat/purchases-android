package com.revenuecat.purchases.google

import android.os.Handler
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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

/** Opt-in smoke test: run only on a disposable emulator with a signed-in Google Play account. */
@RunWith(AndroidJUnit4::class)
class AutomaticServiceReconnectionTest {
    @Test
    fun operationsReconnectAfterPlayStoreStops() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString("automaticBillingRecovery") == "true")
        val connected = CountDownLatch(1)
        val wrapper = BillingWrapper(
            clientFactory = BillingWrapper.ClientFactory(instrumentation.targetContext, false),
            mainHandler = Handler(Looper.getMainLooper()),
            deviceCache = mockk<DeviceCache>(relaxed = true),
            diagnosticsTrackerIfEnabled = null,
            purchasesStateProvider = PurchasesStateCache(PurchasesState()),
        )
        wrapper.purchasesUpdatedListener = mockk<BillingAbstract.PurchasesUpdatedListener>(relaxed = true)
        wrapper.stateListener = object : BillingAbstract.StateListener {
            override fun onConnected() {
                connected.countDown()
            }
        }
        try {
            wrapper.startConnection()
            check(connected.await(15, TimeUnit.SECONDS)) { "Initial Play Billing setup did not succeed" }
            val originalClient = wrapper.billingClient
            instrumentation.uiAutomation.executeShellCommand("am force-stop com.android.vending").use {
                FileInputStream(it.fileDescriptor).bufferedReader().readText()
            }
            val disconnectDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (wrapper.isConnected() && System.nanoTime() < disconnectDeadline) Thread.sleep(50)
            check(!wrapper.isConnected()) { "Play Billing did not disconnect" }

            val completed = CountDownLatch(2)
            val results = Collections.synchronizedList(mutableListOf<String>())
            repeat(2) { index ->
                wrapper.queryPurchases(
                    appUserID = "automatic-reconnection-test",
                    onSuccess = {
                        results.add("$index success")
                        completed.countDown()
                    },
                    onError = {
                        results.add("$index error: $it")
                        completed.countDown()
                    },
                )
            }
            check(completed.await(12, TimeUnit.SECONDS)) { "Operations did not finish: $results" }
            check(results.toSet() == setOf("0 success", "1 success")) { "Recovery failed: $results" }
            check(wrapper.billingClient === originalClient) { "Recovery replaced the client" }
            check(wrapper.isConnected()) { "Recovered client is not ready" }
        } finally {
            wrapper.close()
        }
    }
}
