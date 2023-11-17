package com.revenuecat.purchases.google.usecase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.QueryPurchasesParams
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.mockQueryPurchasesAsync
import com.revenuecat.purchases.utils.stubGooglePurchase
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifySequence
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class QueryPurchasesUseCaseTest : BaseBillingUseCaseTest() {

    private val appUserId = "jerry"
    private var billingClientStateListener: BillingClientStateListener? = null
    private val billingClientBillingUnavailableResult =
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE.buildResult()
    
    @Before
    override fun setup() {
        super.setup()
        billingClientStateListener = null

        val billingClientStateListenerSlot = slot<BillingClientStateListener>()
        every {
            mockClient.startConnection(capture(billingClientStateListenerSlot))
        } answers {
            billingClientStateListener = billingClientStateListenerSlot.captured
        }

        val featureSlot = slot<String>()
        every {
            mockClient.isFeatureSupported(capture(featureSlot))
        } returns billingClientOKResult
    }

    @Test
    fun `when querying anything and billing client returns an empty list, returns an empty list`() {
        mockClient.mockQueryPurchasesAsync(
            billingClientOKResult,
            billingClientOKResult,
            emptyList(),
            emptyList()
        )

        var purchasesByHashedToken: Map<String, StoreTransaction>? = null
        wrapper.queryPurchases(
            appUserID = "appUserID",
            onSuccess = {
                purchasesByHashedToken = it
            },
            onError = {
                fail("should be a success)")
            }
        )

        assertThat(purchasesByHashedToken).isNotNull
        assertThat(purchasesByHashedToken).isEmpty()
    }

    @Test
    fun `defers query purchases if client not connected`() {
        every { mockClient.isReady } returns false

        var purchasesByHashedToken: Map<String, StoreTransaction>? = null
        wrapper.queryPurchases(
            appUserID = "appUserID",
            onSuccess = {
                purchasesByHashedToken = it
            },
            onError = {
                fail("should be a success)")
            }
        )

        assertThat(purchasesByHashedToken).isNull()

        verify(exactly = 0) {
            mockClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any())
        }

        mockClient.mockQueryPurchasesAsync(
            billingClientOKResult,
            billingClientOKResult,
            emptyList(),
            emptyList()
        )

        every { mockClient.isReady } returns true

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)

        verify(exactly = 2) {
            mockClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any())
        }

        assertThat(purchasesByHashedToken).isNotNull
    }

    @Test
    fun `queryPurchases returns error if error connecting`() {
        every { mockClient.isReady } returns false

        var receivedError: PurchasesError? = null
        wrapper.queryPurchases(
            appUserID = "appUserID",
            onSuccess = { fail("should be an error") },
            onError = { receivedError = it }
        )

        billingClientStateListener!!.onBillingSetupFinished(billingClientBillingUnavailableResult)

        verify(exactly = 0) {
            mockClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any())
        }

        assertThat(receivedError).isNotNull
    }

    @Test
    fun `when querying INAPPs result is created properly`() {
        val token = "token"
        val type = ProductType.INAPP
        val time = System.currentTimeMillis()
        val sku = "sku"

        val purchase = stubGooglePurchase(
            purchaseToken = token,
            purchaseTime = time,
            productIds = listOf(sku)
        )

        mockClient.mockQueryPurchasesAsync(
            billingClientOKResult,
            billingClientOKResult,
            emptyList(),
            listOf(purchase)
        )

        var purchasesByHashedToken: Map<String, StoreTransaction>? = null
        wrapper.queryPurchases(
            appUserID = "appUserID",
            onSuccess = {
                purchasesByHashedToken = it
            },
            onError = {
                fail("should be a success)")
            }
        )

        assertThat(purchasesByHashedToken).isNotNull
        assertThat(purchasesByHashedToken).isNotEmpty

        val purchaseWrapper = purchasesByHashedToken?.get(token.sha1())
        assertThat(purchaseWrapper).isNotNull
        assertThat(purchaseWrapper!!.type).isEqualTo(type)
        assertThat(purchaseWrapper.purchaseToken).isEqualTo(token)
        assertThat(purchaseWrapper.purchaseTime).isEqualTo(time)
        assertThat(purchaseWrapper.productIds[0]).isEqualTo(sku)
        assertThat(purchasesByHashedToken?.size == 1)
    }

    @Test
    fun `when querying SUBS result is created properly`() {
        val token = "token"
        val time = System.currentTimeMillis()
        val sku = "sku"

        val purchase = stubGooglePurchase(
            purchaseToken = token,
            purchaseTime = time,
            productIds = listOf(sku)
        )

        mockClient.mockQueryPurchasesAsync(
            billingClientOKResult,
            billingClientOKResult,
            listOf(purchase),
            emptyList()
        )

        var purchasesByHashedToken: Map<String, StoreTransaction>? = null
        wrapper.queryPurchases(
            appUserID = "appUserID",
            onSuccess = {
                purchasesByHashedToken = it
            },
            onError = {
                fail("should be a success)")
            }
        )

        assertThat(purchasesByHashedToken).isNotNull
        assertThat(purchasesByHashedToken).isNotEmpty

        val purchaseWrapper = purchasesByHashedToken?.get(token.sha1())
        assertThat(purchaseWrapper).isNotNull
        assertThat(purchaseWrapper!!.type).isEqualTo(ProductType.SUBS)
        assertThat(purchaseWrapper.purchaseToken).isEqualTo(token)
        assertThat(purchaseWrapper.purchaseTime).isEqualTo(time)
        assertThat(purchaseWrapper.productIds[0]).isEqualTo(sku)
    }

    // region diagnostics tracking

    @Test
    fun `queryPurchases tracks query purchases diagnostics calls for subs and inapp with correct parameters`() {
        every {
            mockDateProvider.now
        } returnsMany listOf(
            Date(timestamp0),
            Date(timestamp123),
            Date(timestamp500),
            Date(timestamp900)
        )

        val result = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .setDebugMessage("test-debug-message")
            .build()
        val slot = slot<PurchasesResponseListener>()
        every {
            mockClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                capture(slot)
            )
        } answers {
            slot.captured.onQueryPurchasesResponse(result, emptyList())
        }

        wrapper.queryPurchases(appUserId, {}, { fail("shouldn't be an error") })

        verifySequence {
            mockDiagnosticsTracker.trackGoogleQueryPurchasesRequest(
                BillingClient.ProductType.SUBS,
                BillingClient.BillingResponseCode.OK,
                billingDebugMessage = "test-debug-message",
                responseTime = 123.milliseconds
            )
            mockDiagnosticsTracker.trackGoogleQueryPurchasesRequest(
                BillingClient.ProductType.INAPP,
                BillingClient.BillingResponseCode.OK,
                billingDebugMessage = "test-debug-message",
                responseTime = 400.milliseconds
            )
        }
    }

    @Test
    fun `queryPurchases tracks query purchases diagnostics only for subs query if it fails`() {
        every { mockDateProvider.now } returnsMany listOf(Date(timestamp0), Date(timestamp123))

        val result = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED)
            .setDebugMessage("test-debug-message")
            .build()
        val slot = slot<PurchasesResponseListener>()
        every {
            mockClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                capture(slot)
            )
        } answers {
            slot.captured.onQueryPurchasesResponse(result, emptyList())
        }

        wrapper.queryPurchases(appUserId, { fail("should be an error") }, {})

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGoogleQueryPurchasesRequest(
                BillingClient.ProductType.SUBS,
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED,
                billingDebugMessage = "test-debug-message",
                responseTime = 123.milliseconds
            )
        }
    }

    // endregion diagnostics tracking

    // region multiple responses

    @Test
    fun `queryPurchasesAsync only calls one response when BillingClient responds twice`() {
        var numCallbacks = 0

        val slot = slot<PurchasesResponseListener>()
        every {
            mockClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                capture(slot)
            )
        } answers {
            slot.captured.onQueryPurchasesResponse(billingClientOKResult, emptyList())
            slot.captured.onQueryPurchasesResponse(billingClientOKResult, emptyList())
        }

        wrapper.queryPurchases(
            appUserID = "appUserID",
            onSuccess = {
                numCallbacks++
            },
            onError = {
                fail("shouldn't be an error")
            }
        )

        assertThat(numCallbacks).isEqualTo(1)
    }

    @Test
    fun `queryPurchasesAsync only calls one response when BillingClient responds twice from different threads`() {
        val numCallbacks = AtomicInteger(0)

        val slot = slot<PurchasesResponseListener>()
        // queryPurchasesAsync is called twice, so 4 would come from calls to onQueryPurchasesResponse
        // and 1 for the onSuccess
        val lock = CountDownLatch(5)
        every {
            mockClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                capture(slot)
            )
        } answers {
            Thread {
                slot.captured.onQueryPurchasesResponse(billingClientOKResult, emptyList())
                lock.countDown()
            }.start()

            Thread {
                slot.captured.onQueryPurchasesResponse(billingClientOKResult, emptyList())
                lock.countDown()
            }.start()
        }

        wrapper.queryPurchases(
            appUserID = "appUserID",
            onSuccess = {
                // ensuring we don't hit an edge case where numCallbacks doesn't increment before the final assert
                numCallbacks.incrementAndGet()
                lock.countDown()
            },
            onError = {
                fail("shouldn't be an error")
            }
        )

        lock.await(300, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isEqualTo(0)

        assertThat(numCallbacks.get()).isEqualTo(1)
    }

    // endregion multiple responses

    // region retries

    @Test
    fun `If service is disconnected, re-executeRequestOnUIThread`() {
        val slot = slot<PurchasesResponseListener>()
        val queryPurchasesStubbing = every {
            mockClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                capture(slot)
            )
        }
        var receivedList: Map<String, StoreTransaction>? = null
        var timesExecutedInMainThread = 0
        QueryPurchasesUseCase(
            QueryPurchasesUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
            ),
            { received ->
                receivedList = received
            },
            { _ ->
                fail("shouldn't be an error")
            },
            withConnectedClient = {
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = {
                timesExecutedInMainThread++

                queryPurchasesStubbing answers {
                    if (timesExecutedInMainThread == 1) {
                        slot.captured.onQueryPurchasesResponse(billingClientDisconnectedResult, emptyList())
                    } else {
                        slot.captured.onQueryPurchasesResponse(
                            billingClientOKResult,
                            listOf(stubGooglePurchase())
                        )
                    }
                }

                it.invoke(null)
            },
        ).run()

        assertThat(timesExecutedInMainThread).isEqualTo(2)
        assertThat(receivedList).isNotNull
        assertThat(receivedList!!.size).isOne
    }

    @Test
    fun `If service returns NETWORK_ERROR, re-execute a max of 3 times`() {
        val slot = slot<PurchasesResponseListener>()
        val queryPurchasesStubbing = every {
            mockClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                capture(slot)
            )
        }

        var receivedError: PurchasesError? = null
        var timesRetried = 0
        QueryPurchasesUseCase(
            QueryPurchasesUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
            ),
            { _ ->
                fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                timesRetried++
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = {
                queryPurchasesStubbing answers {
                    slot.captured.onQueryPurchasesResponse(
                        BillingClient.BillingResponseCode.NETWORK_ERROR.buildResult(),
                        emptyList()
                    )
                }

                it.invoke(null)
            },
        ).run()

        assertThat(timesRetried).isEqualTo(4) // First attempt plus 3 retries
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.NetworkError)
    }

    @Test
    fun `If service returns SERVICE_UNAVAILABLE, re-execute a max of 3 times`() {
        val slot = slot<PurchasesResponseListener>()
        val queryPurchasesStubbing = every {
            mockClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                capture(slot)
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        QueryPurchasesUseCase(
            QueryPurchasesUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
            ),
            { _ ->
                fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                timesRetried++
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = {
                queryPurchasesStubbing answers {
                    slot.captured.onQueryPurchasesResponse(
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult(),
                        emptyList()
                    )
                }

                it.invoke(null)
            },
        ).run()

        assertThat(timesRetried).isEqualTo(4) // First attempt plus 3 retries
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `If service returns ERROR, re-execute a max of 3 times`() {
        val slot = slot<PurchasesResponseListener>()
        val queryPurchasesStubbing = every {
            mockClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                capture(slot)
            )
        }

        var receivedError: PurchasesError? = null
        var timesRetried = 0
        QueryPurchasesUseCase(
            QueryPurchasesUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
            ),
            { _ ->
                fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                timesRetried++
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = {
                queryPurchasesStubbing answers {
                    slot.captured.onQueryPurchasesResponse(
                        BillingClient.BillingResponseCode.ERROR.buildResult(),
                        emptyList()
                    )
                }

                it.invoke(null)
            },
        ).run()

        assertThat(timesRetried).isEqualTo(4) // First attempt plus 3 retries
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `If service returns ITEM_UNAVAILABLE, doesn't retry`() {
        val slot = slot<PurchasesResponseListener>()
        val queryPurchasesStubbing = every {
            mockClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                capture(slot)
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        QueryPurchasesUseCase(
            QueryPurchasesUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
            ),
            { _ ->
                fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                timesRetried++
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = {
                queryPurchasesStubbing answers {
                    slot.captured.onQueryPurchasesResponse(
                        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE.buildResult(),
                        emptyList()
                    )
                }

                it.invoke(null)
            },
        ).run()

        assertThat(timesRetried).isEqualTo(1)
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.ProductNotAvailableForPurchaseError)
    }

    // endregion retries

}