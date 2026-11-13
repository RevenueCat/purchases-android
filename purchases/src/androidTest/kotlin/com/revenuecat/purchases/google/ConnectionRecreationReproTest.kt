package com.revenuecat.purchases.google

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesState
import com.revenuecat.purchases.PurchasesStateCache
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.StoreTransaction
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Android delivers a null binding to a real BillingClient; its replacement binds to the actual Play Store. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 28)
class ConnectionRecreationReproTest {
    @Test
    @Suppress("LongMethod")
    fun requestAfterTimeoutUsesFreshBillingClient() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("billingRepro") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = NullBillingContext(instrumentation.targetContext)
        val wrapper = BillingWrapper(
            clientFactory = BillingWrapper.ClientFactory(context, false),
            mainHandler = Handler(Looper.getMainLooper()),
            deviceCache = DeviceCache(
                context.getSharedPreferences("billing-connection-repro", Context.MODE_PRIVATE),
                "local-cache-namespace",
            ),
            diagnosticsTrackerIfEnabled = null,
            purchasesStateProvider = PurchasesStateCache(PurchasesState(appInBackground = false)),
        )
        val pending = CountDownLatch(2)
        val results = ConcurrentLinkedQueue<Pair<String, PurchasesError?>>()
        fun record(name: String, error: PurchasesError?) {
            results.add(name to error)
            Log.i("RC_RECREATE", "$name completed: $error")
            pending.countDown()
        }
        wrapper.purchasesUpdatedListener = object : BillingAbstract.PurchasesUpdatedListener {
            override fun onPurchasesUpdated(purchases: List<StoreTransaction>) = record("purchase", null)
            override fun onPurchasesFailedToUpdate(purchasesError: PurchasesError) = record("purchase", purchasesError)
        }
        try {
            wrapper.startConnection()
            check(context.nullBinding.await(5, TimeUnit.SECONDS)) { "Android did not deliver onNullBinding" }
            val oldClient = checkNotNull(wrapper.billingClient)
            check(oldClient.connectionState == BillingClient.ConnectionState.CONNECTING) {
                "Expected the real client to remain CONNECTING after onNullBinding"
            }
            instrumentation.runOnMainSync {
                wrapper.makePurchaseAsync(
                    Activity().apply { intent = Intent() },
                    "repro-user",
                    subscription(),
                    replaceProductInfo = null,
                    presentedOfferingContext = null,
                    isPersonalizedPrice = null,
                )
                wrapper.queryProductDetailsAsync(ProductType.SUBS, setOf("incident.repro.product"), {
                    record("products", null)
                }, { record("products", it) })
            }
            check(pending.await(20, TimeUnit.SECONDS)) { "Purchase or product request did not time out: $results" }
            check(
                results.map { it.first }.sorted() == listOf("products", "purchase"),
            ) { "Unexpected callbacks: $results" }
            check(
                results.all { (_, error) ->
                    error?.code == PurchasesErrorCode.StoreProblemError &&
                        error.underlyingErrorMessage == "Timed out waiting for the billing service to connect."
                },
            ) { "Expected connection timeouts: $results" }
            awaitRetirement(wrapper, oldClient)
            check(context.nullBindingUnbinds.get() == 1) { "The rejected binding was not unbound exactly once" }
            check(context.bindCalls.get() == 1) { "A new connection was started without a new request" }
            // Callback counts alone can miss retained work that finishes after the replacement query.
            val queuedRequests = BillingWrapper::class.java.getDeclaredField("serviceRequests").apply {
                isAccessible = true
            }.get(wrapper) as Collection<*>
            check(queuedRequests.isEmpty()) { "Timed-out requests are still queued for execution" }

            val outcome = queryOnReplacement(wrapper)
            check(wrapper.billingClient != null && wrapper.billingClient !== oldClient) { "Client was not recreated" }
            check(context.bindCalls.get() == 2) { "Expected one new binding to the real Play Store" }
            check(results.size == 2) { "Timed-out requests were called again: $results" }
            Log.i("RC_RECREATE", "Real client retired after null binding; request on replacement completed: $outcome")
        } finally {
            wrapper.close()
        }
    }

    private fun awaitRetirement(wrapper: BillingWrapper, oldClient: BillingClient) {
        val deadline = SystemClock.elapsedRealtime() + TimeUnit.SECONDS.toMillis(3)
        while ((wrapper.billingClient != null || oldClient.connectionState != BillingClient.ConnectionState.CLOSED) &&
            SystemClock.elapsedRealtime() < deadline
        ) {
            Thread.sleep(50)
        }
        check(wrapper.billingClient == null) { "The stuck client was not retired" }
        check(oldClient.connectionState == BillingClient.ConnectionState.CLOSED) { "The old client was not closed" }
    }

    private fun queryOnReplacement(wrapper: BillingWrapper): String {
        val completed = CountDownLatch(1)
        val outcome = AtomicReference("pending")
        wrapper.queryProductDetailsAsync(ProductType.SUBS, setOf("incident.repro.product"), {
            outcome.set("success")
            completed.countDown()
        }, { error ->
            // CI has no signed-in Play account. This proves a terminal response, not purchase readiness.
            outcome.set(
                if (error.code == PurchasesErrorCode.PurchaseNotAllowedError &&
                    error.underlyingErrorMessage?.contains("BILLING_UNAVAILABLE") == true
                ) {
                    "billing unavailable"
                } else {
                    "unexpected error: $error"
                },
            )
            completed.countDown()
        })
        check(completed.await(15, TimeUnit.SECONDS)) { "Request on fresh client did not complete" }
        return outcome.get().also {
            check(it == "success" || it == "billing unavailable") { it }
        }
    }

    private fun subscription(): GooglePurchasingData.Subscription {
        // This purchase must time out before launch. No real product, account, or purchase sheet is needed.
        val details = ProductDetails::class.java.getDeclaredConstructor(String::class.java).apply {
            isAccessible = true
        }.newInstance(
            """{"productId":"incident.repro.product","type":"subs","title":"Repro","name":"Repro",
                |"description":"Repro","subscriptionOfferDetails":[]}
            """.trimMargin(),
        )
        return GooglePurchasingData.Subscription("incident.repro.product", "monthly", details, "repro-offer-token")
    }
}
