package com.revenuecat.purchases.google.usecase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.firstSku
import com.revenuecat.purchases.google.toGoogleProductType
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.mockQueryPurchaseHistory
import com.revenuecat.purchases.utils.stubPurchaseHistoryRecord
import com.revenuecat.purchases.utils.verifyQueryPurchaseHistoryCalledWithType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.assertj.core.data.Offset
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

    private val subsGoogleProductType = ProductType.SUBS.toGoogleProductType()!!
    private val inAppGoogleProductType = ProductType.INAPP.toGoogleProductType()!!
    private val appUserId = "jerry"

    @Test
    fun `if no listener is set, we fail`() {
        wrapper.purchasesUpdatedListener = null

        var error: PurchasesError? = null
        wrapper.queryPurchaseHistoryAsync(
            productType = ProductType.SUBS.toGoogleProductType()!!,
            onReceivePurchaseHistory = {
                fail("call should not succeed")
            },
            onReceivePurchaseHistoryError = {
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
            productType = "notValid",
            onReceivePurchaseHistory = {
                fail("call should not succeed")
            },
            onReceivePurchaseHistoryError = {
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
            productType = BillingClient.ProductType.SUBS,
            onReceivePurchaseHistory = {
                numCallbacks++
            }, onReceivePurchaseHistoryError = {
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
            productType = BillingClient.ProductType.SUBS,
            onReceivePurchaseHistory = {
                // ensuring we don't hit an edge case where numCallbacks doesn't increment before the final assert
                numCallbacks.incrementAndGet()
                lock.countDown()
            }, onReceivePurchaseHistoryError = {
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
            productType = subsGoogleProductType,
            onReceivePurchaseHistory = {
                successCalled = true
            },
            onReceivePurchaseHistoryError = {
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
            productType = subsGoogleProductType,
            onReceivePurchaseHistory = {
                fail("should go to on error")
            },
            onReceivePurchaseHistoryError = {
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
            productType = subsGoogleProductType,
            onReceivePurchaseHistory = {},
            onReceivePurchaseHistoryError = {}
        )

        mockClient.verifyQueryPurchaseHistoryCalledWithType(subsGoogleProductType, subsBuilder)

        val inAppBuilder = mockClient.mockQueryPurchaseHistory(
            billingClientOKResult,
            emptyList()
        )

        wrapper.queryPurchaseHistoryAsync(
            productType = inAppGoogleProductType,
            onReceivePurchaseHistory = {},
            onReceivePurchaseHistoryError = {}
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

        wrapper.queryPurchaseHistoryAsync(
            productType = BillingClient.ProductType.SUBS,
            onReceivePurchaseHistory = {},
            onReceivePurchaseHistoryError = { fail("shouldn't be an error") }
        )

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

        wrapper.queryPurchaseHistoryAsync(
            productType = BillingClient.ProductType.SUBS,
            onReceivePurchaseHistory = { fail("should be an error") }
        ) {}

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

    // region findPurchaseInPurchaseHistory

    @Test
    fun `findPurchaseInPurchaseHistory works`() {
        val sku = "aPurchase"
        val purchaseHistoryRecord = stubPurchaseHistoryRecord(productIds = listOf(sku))

        mockClient.mockQueryPurchaseHistory(
            billingClientOKResult,
            listOf(purchaseHistoryRecord)
        )

        var recordFound: StoreTransaction? = null
        wrapper.findPurchaseInPurchaseHistory(
            appUserID = appUserId,
            productType = ProductType.SUBS,
            productId = sku,
            onCompletion = {
                recordFound = it
            },
            onError = {
                fail("should be success")
            }
        )

        assertThat(recordFound).isNotNull
        assertThat(recordFound!!.productIds[0]).isEqualTo(purchaseHistoryRecord.firstSku)
        assertThat(recordFound!!.purchaseTime).isEqualTo(purchaseHistoryRecord.purchaseTime)
        assertThat(recordFound!!.purchaseToken).isEqualTo(purchaseHistoryRecord.purchaseToken)
    }

    @Test
    fun `findPurchaseInPurchaseHistory returns error if not found`() {
        val sku = "aPurchase"
        val purchaseHistoryRecord = mockk<PurchaseHistoryRecord>(relaxed = true).also {
            every { it.firstSku } returns sku + "somethingrandom"
        }

        mockClient.mockQueryPurchaseHistory(
            billingClientOKResult,
            listOf(purchaseHistoryRecord)
        )
        var errorReturned: PurchasesError? = null
        wrapper.findPurchaseInPurchaseHistory(
            appUserID = appUserId,
            productType = ProductType.SUBS,
            productId = sku,
            onCompletion = {
                fail("should be error")
            },
            onError = {
                errorReturned = it
            }
        )

        assertThat(errorReturned).isNotNull
        assertThat(errorReturned!!.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
    }

    // endregion findPurchaseInPurchaseHistory

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
                appInBackground = false,
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
            executeRequestOnUIThread = { _, request ->
                timesExecutedInMainThread++

                queryPurchaseHistoryStubbing answers {
                    if (timesExecutedInMainThread == 1) {
                        slot.captured.onPurchaseHistoryResponse(
                            billingClientDisconnectedResult,
                            emptyList()
                        )
                    } else {
                        val sku = "aPurchase"
                        val purchaseHistoryRecord = mockk<PurchaseHistoryRecord>(relaxed = true).also {
                            every { it.firstSku } returns sku + "somethingrandom"
                        }
                        slot.captured.onPurchaseHistoryResponse(
                            billingClientOKResult,
                            listOf(purchaseHistoryRecord)
                        )
                    }
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesExecutedInMainThread).isEqualTo(2)
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
                appInBackground = false,
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
            executeRequestOnUIThread = { _, request ->
                queryPurchaseHistoryStubbing answers {
                    slot.captured.onPurchaseHistoryResponse(
                        BillingClient.BillingResponseCode.NETWORK_ERROR.buildResult(),
                        emptyList()
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(4) // First attempt plus 3 retries
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.NetworkError)
    }

    @Test
    fun `If service returns SERVICE_UNAVAILABLE, retry with backoff a few times then error if user in session`() {
        val slot = slot<PurchaseHistoryResponseListener>()
        val queryPurchaseHistoryStubbing = every {
            mockClient.queryPurchaseHistoryAsync(
                any<QueryPurchaseHistoryParams>(),
                capture(slot)
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val capturedDelays = mutableListOf<Long>()
        val useCase = QueryPurchaseHistoryUseCase(
            QueryPurchaseHistoryUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                BillingClient.ProductType.SUBS,
                appInBackground = false,
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
            executeRequestOnUIThread = { delay, request ->
                capturedDelays.add(delay)
                queryPurchaseHistoryStubbing answers {
                    slot.captured.onPurchaseHistoryResponse(
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult(),
                        emptyList()
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(4)
        assertThat(capturedDelays.last()).isCloseTo(RETRY_TIMER_MAX_TIME_MILLISECONDS_FOREGROUND, Offset.offset(1000L))
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `If service returns SERVICE_UNAVAILABLE, re-execute with backoff`() {
        val slot = slot<PurchaseHistoryResponseListener>()
        val queryPurchaseHistoryStubbing = every {
            mockClient.queryPurchaseHistoryAsync(
                any<QueryPurchaseHistoryParams>(),
                capture(slot)
            )
        }
        var receivedError: PurchasesError? = null
        val capturedDelays = mutableListOf<Long>()
        val useCase = QueryPurchaseHistoryUseCase(
            QueryPurchaseHistoryUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                BillingClient.ProductType.SUBS,
                appInBackground = true,
            ),
            { _ ->
                fail("shouldn't be success")
            },
            { error ->
                receivedError = error
            },
            withConnectedClient = {
                it.invoke(mockClient)
            },
            executeRequestOnUIThread = { delay, request ->
                capturedDelays.add(delay)
                queryPurchaseHistoryStubbing answers {
                    slot.captured.onPurchaseHistoryResponse(
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult(),
                        emptyList()
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(capturedDelays.size).isEqualTo(12)
        assertThat(capturedDelays.last()).isCloseTo(RETRY_TIMER_MAX_TIME_MILLISECONDS, Offset.offset(1000L))
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
                appInBackground = false,
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
            executeRequestOnUIThread = { _, request ->
                queryPurchaseHistoryStubbing answers {
                    slot.captured.onPurchaseHistoryResponse(
                        BillingClient.BillingResponseCode.ERROR.buildResult(),
                        emptyList()
                    )
                }

                request(null)
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
                appInBackground = false,
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
            executeRequestOnUIThread = { _, request ->
                queryPurchaseHistoryStubbing answers {
                    slot.captured.onPurchaseHistoryResponse(
                        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE.buildResult(),
                        emptyList()
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(1)
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.ProductNotAvailableForPurchaseError)
    }

    // endregion retries

}
