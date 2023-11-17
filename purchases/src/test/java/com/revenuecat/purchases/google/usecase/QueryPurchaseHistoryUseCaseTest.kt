package com.revenuecat.purchases.google.usecase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.firstSku
import com.revenuecat.purchases.google.toGoogleProductType
import com.revenuecat.purchases.utils.mockQueryPurchaseHistory
import com.revenuecat.purchases.utils.verifyQueryPurchaseHistoryCalledWithType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class QueryPurchaseHistoryUseCaseTest: BaseBillingUseCaseTest() {

    private companion object {
        const val timestamp0 = 1676379370000 // Tuesday, February 14, 2023 12:56:10.000 PM GMT
        const val timestamp123 = 1676379370123 // Tuesday, February 14, 2023 12:56:10.123 PM GMT
    }

    private var capturedConsumeResponseListener = slot<ConsumeResponseListener>()
    private var capturedConsumeParams = slot<ConsumeParams>()

    private val subsGoogleProductType = ProductType.SUBS.toGoogleProductType()!!
    private val inAppGoogleProductType = ProductType.INAPP.toGoogleProductType()!!

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

}