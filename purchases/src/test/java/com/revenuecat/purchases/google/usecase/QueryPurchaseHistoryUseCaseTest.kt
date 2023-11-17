package com.revenuecat.purchases.google.usecase

import android.app.Activity
import android.content.Intent
import android.os.Handler
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.firstSku
import com.revenuecat.purchases.google.BillingWrapper
import com.revenuecat.purchases.google.toGoogleProductType
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.utils.mockProductDetails
import com.revenuecat.purchases.utils.mockQueryPurchaseHistory
import com.revenuecat.purchases.utils.verifyQueryPurchaseHistoryCalledWithType
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class QueryPurchaseHistoryUseCaseTest {

    private companion object {
        const val timestamp0 = 1676379370000 // Tuesday, February 14, 2023 12:56:10.000 PM GMT
        const val timestamp123 = 1676379370123 // Tuesday, February 14, 2023 12:56:10.123 PM GMT
    }

    private var onConnectedCalled: Boolean = false
    private var mockClientFactory: BillingWrapper.ClientFactory = mockk()
    private var mockClient: BillingClient = mockk()
    private var purchasesUpdatedListener: PurchasesUpdatedListener? = null
    private var billingClientStateListener: BillingClientStateListener? = null
    private var handler: Handler = mockk()
    private var mockDeviceCache: DeviceCache = mockk()
    private var mockDiagnosticsTracker: DiagnosticsTracker = mockk()
    private var mockDateProvider: DateProvider = mockk()

    private var mockPurchasesListener: BillingAbstract.PurchasesUpdatedListener = mockk()

    private var capturedAcknowledgeResponseListener = slot<AcknowledgePurchaseResponseListener>()
    private var capturedAcknowledgePurchaseParams = slot<AcknowledgePurchaseParams>()
    private var capturedConsumeResponseListener = slot<ConsumeResponseListener>()
    private var capturedConsumeParams = slot<ConsumeParams>()

    private lateinit var wrapper: BillingWrapper

    private lateinit var mockDetailsList: List<ProductDetails>

    private var storeProducts: List<StoreProduct>? = null

    private val billingClientOKResult = BillingClient.BillingResponseCode.OK.buildResult()
    private var mockActivity = mockk<Activity>()

    private val subsGoogleProductType = ProductType.SUBS.toGoogleProductType()!!
    private val inAppGoogleProductType = ProductType.INAPP.toGoogleProductType()!!

    @Before
    fun setup() {
        storeProducts = null
        purchasesUpdatedListener = null
        billingClientStateListener = null

        mockRunnables()
        mockDiagnosticsTracker()
        every { mockDateProvider.now } returns Date(1676379370000) // Tuesday, February 14, 2023 12:56:10 PM GMT

        val listenerSlot = slot<PurchasesUpdatedListener>()
        every {
            mockClientFactory.buildClient(capture(listenerSlot))
        } answers {
            purchasesUpdatedListener = listenerSlot.captured
            mockClient
        }

        val billingClientStateListenerSlot = slot<BillingClientStateListener>()
        every {
            mockClient.startConnection(capture(billingClientStateListenerSlot))
        } answers {
            billingClientStateListener = billingClientStateListenerSlot.captured
        }

        every {
            mockClient.endConnection()
        } just Runs

        every {
            mockClient.acknowledgePurchase(
                capture(capturedAcknowledgePurchaseParams),
                capture(capturedAcknowledgeResponseListener)
            )
        } just Runs

        mockConsumeAsync(billingClientOKResult)

        every {
            mockClient.isReady
        } returns false andThen true

        val featureSlot = slot<String>()
        every {
            mockClient.isFeatureSupported(capture(featureSlot))
        } returns billingClientOKResult

        mockDetailsList = listOf(mockProductDetails())

        wrapper = BillingWrapper(mockClientFactory, handler, mockDeviceCache, mockDiagnosticsTracker, mockDateProvider)
        wrapper.purchasesUpdatedListener = mockPurchasesListener
        wrapper.startConnectionOnMainThread()
        onConnectedCalled = false
        wrapper.stateListener = object : BillingAbstract.StateListener {
            override fun onConnected() {
                onConnectedCalled = true
            }
        }

        every {
            mockActivity.intent
        } returns Intent()
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `if no listener is set, we fail`() {
        wrapper.purchasesUpdatedListener = null

        var error: PurchasesError? = null
        wrapper.queryPurchaseHistoryAsync(
            ProductType.SUBS.toGoogleProductType()!!,
            {
                fail("call should not succeed")
            },
            {
                error = it
            }
        )
        assertThat(error).isNotNull
        assertThat(error?.code).isEqualTo(PurchasesErrorCode.UnknownError)
        assertThat(error?.underlyingErrorMessage).isEqualTo("BillingWrapper is not attached to a listener")
    }

    @Test
    fun `queryPurchaseHistoryAsync fails if sent invalid type`() {
        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)

        mockClient.mockQueryPurchaseHistory(
            billingClientOKResult,
            emptyList()
        )
        var errorCalled = false
        wrapper.queryPurchaseHistoryAsync(
            "notValid",
            {
                fail("call should not succeed")
            },
            {
                errorCalled = true
            }
        )
        assertThat(errorCalled).isTrue
    }

    @Test
    fun `queryPurchaseHistoryAsync only calls one response when BillingClient responds twice`() {
        var numCallbacks = 0

        val slot = slot<PurchaseHistoryResponseListener>()
        every {
            mockClient.queryPurchaseHistoryAsync(
                any<QueryPurchaseHistoryParams>(),
                capture(slot)
            )
        } answers {
            slot.captured.onPurchaseHistoryResponse(billingClientOKResult, null)
            slot.captured.onPurchaseHistoryResponse(billingClientOKResult, null)
        }

        wrapper.queryPurchaseHistoryAsync(
            BillingClient.ProductType.SUBS,
            {
                numCallbacks++
            }, {
                fail("shouldn't be an error")
            })

        assertThat(numCallbacks).isEqualTo(1)
    }

    @Test
    fun `queryPurchaseHistoryAsync only calls one response when BillingClient responds twice from different threads`() {
        val numCallbacks = AtomicInteger(0)

        val slot = slot<PurchaseHistoryResponseListener>()
        val lock = CountDownLatch(3)
        every {
            mockClient.queryPurchaseHistoryAsync(
                any<QueryPurchaseHistoryParams>(),
                capture(slot)
            )
        } answers {
            Thread {
                slot.captured.onPurchaseHistoryResponse(billingClientOKResult, null)
                lock.countDown()
            }.start()

            Thread {
                slot.captured.onPurchaseHistoryResponse(billingClientOKResult, null)
                lock.countDown()
            }.start()
        }

        wrapper.queryPurchaseHistoryAsync(
            BillingClient.ProductType.SUBS,
            {
                // ensuring we don't hit an edge case where numCallbacks doesn't increment before the final assert
                numCallbacks.incrementAndGet()
                lock.countDown()
            }, {
                fail("shouldn't be an error")
            })

        lock.await()
        assertThat(lock.count).isEqualTo(0)

        assertThat(numCallbacks.get()).isEqualTo(1)
    }

    @Test
    fun queryHistoryCallsListenerIfOk() {
        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)

        mockClient.mockQueryPurchaseHistory(
            billingClientOKResult,
            emptyList()
        )

        var successCalled = false
        wrapper.queryPurchaseHistoryAsync(
            subsGoogleProductType,
            {
                successCalled = true
            },
            {
                fail("shouldn't go to on error")
            }
        )
        assertThat(successCalled).isTrue
    }

    @Test
    fun queryHistoryErrorCalledIfNotOK() {
        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)

        mockClient.mockQueryPurchaseHistory(
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult(),
            emptyList()
        )

        var errorCalled = false
        wrapper.queryPurchaseHistoryAsync(
            subsGoogleProductType,
            {
                fail("should go to on error")
            },
            {
                assertThat(it.code).isEqualTo(PurchasesErrorCode.PurchaseNotAllowedError)
                errorCalled = true
            }
        )

        assertThat(errorCalled).isTrue
    }

    @Test
    fun `queryPurchaseHistoryAsync sets correct type`() {
        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)

        val subsBuilder = mockClient.mockQueryPurchaseHistory(
            billingClientOKResult,
            emptyList()
        )

        wrapper.queryPurchaseHistoryAsync(
            subsGoogleProductType,
            {},
            {}
        )

        mockClient.verifyQueryPurchaseHistoryCalledWithType(subsGoogleProductType, subsBuilder)

        val inAppBuilder = mockClient.mockQueryPurchaseHistory(
            billingClientOKResult,
            emptyList()
        )

        wrapper.queryPurchaseHistoryAsync(
            inAppGoogleProductType,
            {},
            {}
        )

        mockClient.verifyQueryPurchaseHistoryCalledWithType(inAppGoogleProductType, inAppBuilder)
    }

    // region diagnostics tracking

    @Test
    fun `queryPurchaseHistoryAsync tracks diagnostics call with correct parameters`() {
        every { mockDateProvider.now } returnsMany listOf(Date(timestamp0), Date(timestamp123))

        val result = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .setDebugMessage("test-debug-message")
            .build()
        val slot = slot<PurchaseHistoryResponseListener>()
        every {
            mockClient.queryPurchaseHistoryAsync(
                any<QueryPurchaseHistoryParams>(),
                capture(slot)
            )
        } answers {
            slot.captured.onPurchaseHistoryResponse(result, null)
        }

        wrapper.queryPurchaseHistoryAsync(BillingClient.ProductType.SUBS, {}, { fail("shouldn't be an error") })

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGoogleQueryPurchaseHistoryRequest(
                BillingClient.ProductType.SUBS,
                BillingClient.BillingResponseCode.OK,
                billingDebugMessage = "test-debug-message",
                responseTime = 123.milliseconds
            )
        }
    }

    @Test
    fun `queryPurchaseHistoryAsync tracks diagnostics call with correct parameters on error`() {
        every { mockDateProvider.now } returnsMany listOf(Date(timestamp0), Date(timestamp123))

        val result = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE)
            .setDebugMessage("test-debug-message")
            .build()
        val slot = slot<PurchaseHistoryResponseListener>()
        every {
            mockClient.queryPurchaseHistoryAsync(
                any<QueryPurchaseHistoryParams>(),
                capture(slot)
            )
        } answers {
            slot.captured.onPurchaseHistoryResponse(result, null)
        }

        wrapper.queryPurchaseHistoryAsync(BillingClient.ProductType.SUBS, { fail("should be an error") }, {})

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGoogleQueryPurchaseHistoryRequest(
                BillingClient.ProductType.SUBS,
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                billingDebugMessage = "test-debug-message",
                responseTime = 123.milliseconds
            )
        }
    }

    // endregion diagnostics tracking

    // region retries

    @Test
    fun `If service is disconnected, re-executeRequestOnUIThread`() {
        val slot = slot<PurchaseHistoryResponseListener>()
        val queryPurchaseHistoryStubbing = every {
            mockClient.queryPurchaseHistoryAsync(
                any<QueryPurchaseHistoryParams>(),
                capture(slot),
            )
        }
        var receivedList: List<PurchaseHistoryRecord>? = null
        var timesExecutedInMainThread = 0
        val useCase = QueryPurchaseHistoryUseCase(
            QueryPurchaseHistoryUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                BillingClient.ProductType.SUBS,
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

                queryPurchaseHistoryStubbing answers {
                    if (timesExecutedInMainThread == 0) {
                        slot.captured.onPurchaseHistoryResponse(
                            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED.buildResult(),
                            emptyList()
                        )
                    } else {
                        val sku = "aPurchase"
                        val purchaseHistoryRecord = mockk<PurchaseHistoryRecord>(relaxed = true).also {
                            every { it.firstSku } returns sku + "somethingrandom"
                        }
                        slot.captured.onPurchaseHistoryResponse(
                            BillingClient.BillingResponseCode.OK.buildResult(),
                            listOf(purchaseHistoryRecord)
                        )
                    }
                }

                it.invoke(null)
            },
        )

        useCase.run()

        assertThat(receivedList).isNotNull
        assertThat(receivedList!!.size).isOne
    }

    @Test
    fun `If service returns NETWORK_ERROR, re-execute a max of 3 times`() {
        val slot = slot<PurchaseHistoryResponseListener>()
        val queryPurchaseHistoryStubbing = every {
            mockClient.queryPurchaseHistoryAsync(
                any<QueryPurchaseHistoryParams>(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = QueryPurchaseHistoryUseCase(
            QueryPurchaseHistoryUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                BillingClient.ProductType.SUBS,
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
                queryPurchaseHistoryStubbing answers {
                    slot.captured.onPurchaseHistoryResponse(
                        BillingClient.BillingResponseCode.NETWORK_ERROR.buildResult(),
                        emptyList()
                    )
                }

                it.invoke(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(4) // First attempt plus 3 retries
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.NetworkError)
    }

    @Test
    fun `If service returns SERVICE_UNAVAILABLE, re-execute a max of 3 times`() {
        val slot = slot<PurchaseHistoryResponseListener>()
        val queryPurchaseHistoryStubbing = every {
            mockClient.queryPurchaseHistoryAsync(
                any<QueryPurchaseHistoryParams>(),
                capture(slot)
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = QueryPurchaseHistoryUseCase(
            QueryPurchaseHistoryUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                BillingClient.ProductType.SUBS,
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
                queryPurchaseHistoryStubbing answers {
                    slot.captured.onPurchaseHistoryResponse(
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult(),
                        emptyList()
                    )
                }

                it.invoke(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(4) // First attempt plus 3 retries
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `If service returns ERROR, re-execute a max of 3 times`() {
        val slot = slot<PurchaseHistoryResponseListener>()
        val queryPurchaseHistoryStubbing = every {
            mockClient.queryPurchaseHistoryAsync(
                any<QueryPurchaseHistoryParams>(),
                capture(slot)
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = QueryPurchaseHistoryUseCase(
            QueryPurchaseHistoryUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                BillingClient.ProductType.SUBS,
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
                queryPurchaseHistoryStubbing answers {
                    slot.captured.onPurchaseHistoryResponse(
                        BillingClient.BillingResponseCode.ERROR.buildResult(),
                        emptyList()
                    )
                }

                it.invoke(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(4) // First attempt plus 3 retries
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `If service returns ITEM_UNAVAILABLE, doesn't retry`() {
        val slot = slot<PurchaseHistoryResponseListener>()
        val queryPurchaseHistoryStubbing = every {
            mockClient.queryPurchaseHistoryAsync(
                any<QueryPurchaseHistoryParams>(),
                capture(slot),
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val useCase = QueryPurchaseHistoryUseCase(
            QueryPurchaseHistoryUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                BillingClient.ProductType.SUBS,
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
                queryPurchaseHistoryStubbing answers {
                    slot.captured.onPurchaseHistoryResponse(
                        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE.buildResult(),
                        emptyList()
                    )
                }

                it.invoke(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(1)
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.ProductNotAvailableForPurchaseError)
    }

    // endregion retries

    private fun Int.buildResult(): BillingResult {
        return BillingResult.newBuilder().setResponseCode(this).build()
    }

    private fun mockConsumeAsync(billingResult: BillingResult) {
        every {
            mockClient.consumeAsync(capture(capturedConsumeParams), capture(capturedConsumeResponseListener))
        } answers {
            capturedConsumeResponseListener.captured.onConsumeResponse(
                billingResult,
                capturedConsumeParams.captured.purchaseToken
            )
        }
    }

    private fun mockRunnables() {
        val slot = slot<Runnable>()
        every {
            handler.post(capture(slot))
        } answers {
            slot.captured.run()
            true
        }

        val delayedSlot = slot<Runnable>()
        every {
            handler.postDelayed(capture(delayedSlot), any())
        } answers {
            delayedSlot.captured.run()
            true
        }
    }

    private fun mockDiagnosticsTracker() {
        every {
            mockDiagnosticsTracker.trackGoogleQueryProductDetailsRequest(any(), any(), any(), any())
        } just Runs
        every {
            mockDiagnosticsTracker.trackGoogleQueryPurchasesRequest(any(), any(), any(), any())
        } just Runs
        every {
            mockDiagnosticsTracker.trackGoogleQueryPurchaseHistoryRequest(any(), any(), any(), any())
        } just Runs
        every {
            mockDiagnosticsTracker.trackProductDetailsNotSupported(any(), any())
        } just Runs
    }
}