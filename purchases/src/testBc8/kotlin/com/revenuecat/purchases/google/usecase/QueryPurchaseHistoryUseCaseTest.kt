package com.revenuecat.purchases.google.usecase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.QueryPurchasesParams
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.api.BuildConfig
import com.revenuecat.purchases.common.firstProductId
import com.revenuecat.purchases.google.toGoogleProductType
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.utils.mockQueryPurchasesAsync
import com.revenuecat.purchases.utils.stubGooglePurchase
import io.mockk.clearStaticMockk
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.assertj.core.data.Offset
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

@Suppress("MagicNumber", "FunctionNaming", "TooManyFunctions", "LargeClass")
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class QueryPurchaseHistoryUseCaseTest : BaseBillingUseCaseTest() {

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
            },
        )
        assertThat(error).isNotNull
        assertThat(error?.code).isEqualTo(PurchasesErrorCode.UnknownError)
        assertThat(error?.underlyingErrorMessage).isEqualTo("BillingWrapper is not attached to a listener")
    }

    @Test
    fun `queryPurchaseHistoryAsync fails if sent invalid type`() {
        mockClient.mockQueryPurchasesAsync(
            subsResult = billingClientOKResult,
            inAppResult = billingClientOKResult,
            subPurchases = emptyList(),
            inAppPurchases = emptyList(),
        )
        var errorCalled = false
        wrapper.queryPurchaseHistoryAsync(
            productType = "notValid",
            onReceivePurchaseHistory = {
                fail("call should not succeed")
            },
            onReceivePurchaseHistoryError = {
                errorCalled = true
            },
        )
        assertThat(errorCalled).isTrue
    }

    @Test
    fun `queryPurchaseHistoryAsync only calls one response when BillingClient responds twice`() {
        var numCallbacks = 0

        val slot = slot<PurchasesResponseListener>()
        every {
            mockClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                capture(slot),
            )
        } answers {
            slot.captured.onQueryPurchasesResponse(billingClientOKResult, emptyList())
            slot.captured.onQueryPurchasesResponse(billingClientOKResult, emptyList())
        }

        wrapper.queryPurchaseHistoryAsync(
            productType = BillingClient.ProductType.SUBS,
            onReceivePurchaseHistory = {
                numCallbacks++
            },
            onReceivePurchaseHistoryError = {
                fail("shouldn't be an error")
            },
        )

        assertThat(numCallbacks).isEqualTo(1)
    }

    @Test
    fun `queryPurchaseHistoryAsync only calls one response when BillingClient responds twice from different threads`() {
        val numCallbacks = AtomicInteger(0)

        val slot = slot<PurchasesResponseListener>()
        val lock = CountDownLatch(3)
        every {
            mockClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                capture(slot),
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

        wrapper.queryPurchaseHistoryAsync(
            productType = BillingClient.ProductType.SUBS,
            onReceivePurchaseHistory = {
                // ensuring we don't hit an edge case where numCallbacks doesn't increment before the final assert
                numCallbacks.incrementAndGet()
                lock.countDown()
            },
            onReceivePurchaseHistoryError = {
                fail("shouldn't be an error")
            },
        )

        lock.await()
        assertThat(lock.count).isEqualTo(0)

        assertThat(numCallbacks.get()).isEqualTo(1)
    }

    @Test
    public fun queryHistoryCallsListenerIfOk() {
        mockClient.mockQueryPurchasesAsync(
            subsResult = billingClientOKResult,
            inAppResult = billingClientOKResult,
            subPurchases = emptyList(),
            inAppPurchases = emptyList(),
        )

        var successCalled = false
        wrapper.queryPurchaseHistoryAsync(
            productType = subsGoogleProductType,
            onReceivePurchaseHistory = {
                successCalled = true
            },
            onReceivePurchaseHistoryError = {
                fail("shouldn't go to on error")
            },
        )
        assertThat(successCalled).isTrue
    }

    @Test
    public fun queryHistoryErrorCalledIfNotOK() {
        mockClient.mockQueryPurchasesAsync(
            subsResult = BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult(),
            inAppResult = BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult(),
            subPurchases = emptyList(),
            inAppPurchases = emptyList(),
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
            },
        )

        assertThat(errorCalled).isTrue
    }

    @Test
    fun `queryPurchaseHistoryAsync sets correct type`() {
        assumeFalse(BuildConfig.ENABLE_QUERY_PURCHASE_HISTORY_AIDL)

        val subsBuilder = mockClient.mockQueryPurchasesAsync(
            subsResult = billingClientOKResult,
            inAppResult = billingClientOKResult,
            subPurchases = emptyList(),
            inAppPurchases = emptyList(),
        )

        wrapper.queryPurchaseHistoryAsync(
            productType = subsGoogleProductType,
            onReceivePurchaseHistory = {},
            onReceivePurchaseHistoryError = {},
        )

        verify(exactly = 1) { (subsBuilder as QueryPurchasesParams.Builder).setProductType(subsGoogleProductType) }
        verify(exactly = 1) {
            mockClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any())
        }
        clearStaticMockk(QueryPurchasesParams::class)

        val inAppBuilder = mockClient.mockQueryPurchasesAsync(
            subsResult = billingClientOKResult,
            inAppResult = billingClientOKResult,
            subPurchases = emptyList(),
            inAppPurchases = emptyList(),
        )

        wrapper.queryPurchaseHistoryAsync(
            productType = inAppGoogleProductType,
            onReceivePurchaseHistory = {},
            onReceivePurchaseHistoryError = {},
        )

        verify(exactly = 1) { (inAppBuilder as QueryPurchasesParams.Builder).setProductType(inAppGoogleProductType) }
        verify(exactly = 2) {
            mockClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any())
        }
        clearStaticMockk(QueryPurchasesParams::class)
    }

    // region diagnostics tracking

    @Test
    fun `queryPurchaseHistoryAsync tracks diagnostics call with correct parameters`() {
        every { mockDateProvider.now } returnsMany listOf(Date(timestamp0), Date(timestamp123))

        val result = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .setDebugMessage("test-debug-message")
            .build()
        val slot = slot<PurchasesResponseListener>()
        every {
            mockClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                capture(slot),
            )
        } answers {
            slot.captured.onQueryPurchasesResponse(result, emptyList())
        }

        wrapper.queryPurchaseHistoryAsync(
            productType = BillingClient.ProductType.SUBS,
            onReceivePurchaseHistory = {},
            onReceivePurchaseHistoryError = { fail("shouldn't be an error") },
        )

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGoogleQueryPurchaseHistoryRequest(
                BillingClient.ProductType.SUBS,
                BillingClient.BillingResponseCode.OK,
                billingDebugMessage = "test-debug-message",
                responseTime = 123.milliseconds,
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
        val slot = slot<PurchasesResponseListener>()
        every {
            mockClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                capture(slot),
            )
        } answers {
            slot.captured.onQueryPurchasesResponse(result, emptyList())
        }

        wrapper.queryPurchaseHistoryAsync(
            productType = BillingClient.ProductType.SUBS,
            onReceivePurchaseHistory = { fail("should be an error") },
            onReceivePurchaseHistoryError = {},
        )

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGoogleQueryPurchaseHistoryRequest(
                BillingClient.ProductType.SUBS,
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                billingDebugMessage = "test-debug-message",
                responseTime = 123.milliseconds,
            )
        }
    }

    // endregion diagnostics tracking

    // region findPurchaseInPurchaseHistory

    @Test
    fun `findPurchaseInPurchaseHistory works`() {
        val sku = "aPurchase"
        val subPurchase = stubGooglePurchase(productIds = listOf(sku))

        mockClient.mockQueryPurchasesAsync(
            subsResult = billingClientOKResult,
            inAppResult = billingClientOKResult,
            subPurchases = listOf(subPurchase),
            inAppPurchases = emptyList(),
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
            },
        )

        val finalRecord = recordFound
        assertThat(finalRecord).isNotNull
        assertThat(finalRecord!!.productIds[0]).isEqualTo(subPurchase.firstProductId)
        assertThat(finalRecord.purchaseTime).isEqualTo(subPurchase.purchaseTime)
        assertThat(finalRecord.purchaseToken).isEqualTo(subPurchase.purchaseToken)
    }

    @Test
    fun `findPurchaseInPurchaseHistory returns error if not found`() {
        val sku = "aPurchase"
        val subPurchase = mockk<Purchase>(relaxed = true).also {
            every { it.firstProductId } returns sku + "somethingrandom"
            every { it.originalJson } returns "{}"
        }

        mockClient.mockQueryPurchasesAsync(
            subsResult = billingClientOKResult,
            inAppResult = billingClientOKResult,
            subPurchases = listOf(subPurchase),
            inAppPurchases = emptyList(),
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
            },
        )

        assertThat(errorReturned).isNotNull
        assertThat(errorReturned!!.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
    }

    // endregion findPurchaseInPurchaseHistory

    // region retries

    @Test
    fun `If service is disconnected, re-executeRequestOnUIThread`() {
        val slot = slot<PurchasesResponseListener>()
        val queryPurchaseHistoryStubbing = every {
            mockClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                capture(slot),
            )
        }
        var receivedList: List<StoreTransaction>? = null
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
                        slot.captured.onQueryPurchasesResponse(
                            billingClientDisconnectedResult,
                            emptyList(),
                        )
                    } else {
                        val sku = "aPurchase"
                        val purchaseHistoryRecord = mockk<Purchase>(relaxed = true).also {
                            every { it.firstProductId } returns sku + "somethingrandom"
                            every { it.originalJson } returns "{}"
                        }
                        slot.captured.onQueryPurchasesResponse(
                            billingClientOKResult,
                            listOf(purchaseHistoryRecord),
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
        val slot = slot<PurchasesResponseListener>()
        val queryPurchaseHistoryStubbing = every {
            mockClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
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
                    slot.captured.onQueryPurchasesResponse(
                        BillingClient.BillingResponseCode.NETWORK_ERROR.buildResult(),
                        emptyList(),
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
        val slot = slot<PurchasesResponseListener>()
        val queryPurchaseHistoryStubbing = every {
            mockClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                capture(slot),
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
                    slot.captured.onQueryPurchasesResponse(
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult(),
                        emptyList(),
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(timesRetried).isEqualTo(4)
        assertThat(capturedDelays.last())
            .isCloseTo(RETRY_TIMER_SERVICE_UNAVAILABLE_MAX_TIME_FOREGROUND.inWholeMilliseconds, Offset.offset(1000L))
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `If service returns SERVICE_UNAVAILABLE, re-execute with backoff`() {
        val slot = slot<PurchasesResponseListener>()
        val queryPurchaseHistoryStubbing = every {
            mockClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                capture(slot),
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
                    slot.captured.onQueryPurchasesResponse(
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult(),
                        emptyList(),
                    )
                }

                request(null)
            },
        )

        useCase.run()

        assertThat(capturedDelays.size).isEqualTo(12)
        assertThat(capturedDelays.last()).isCloseTo(RETRY_TIMER_MAX_TIME.inWholeMilliseconds, Offset.offset(1000L))
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `If service returns ERROR, re-execute a max of 3 times`() {
        val slot = slot<PurchasesResponseListener>()
        val queryPurchaseHistoryStubbing = every {
            mockClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
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
                    slot.captured.onQueryPurchasesResponse(
                        BillingClient.BillingResponseCode.ERROR.buildResult(),
                        emptyList(),
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
        val slot = slot<PurchasesResponseListener>()
        val queryPurchaseHistoryStubbing = every {
            mockClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
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
                    slot.captured.onQueryPurchasesResponse(
                        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE.buildResult(),
                        emptyList(),
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

    // region findPurchaseInPurchaseHistory

    @Test
    fun `findPurchaseInPurchaseHistory finds purchase in history purchases`() {
        val oldPurchase = stubGooglePurchase()
        val purchases = listOf(oldPurchase)

        mockClient.mockQueryPurchasesAsync(
            subsResult = billingClientOKResult,
            inAppResult = billingClientOKResult,
            subPurchases = purchases,
            inAppPurchases = emptyList(),
        )

        var foundPurchase: StoreTransaction? = null
        wrapper.findPurchaseInPurchaseHistory(
            appUserID = "test-app-user-id",
            productType = ProductType.SUBS,
            productId = oldPurchase.products.first(),
            onCompletion = { foundPurchase = it },
            onError = { fail("Shouldn't be an error: $it") },
        )

        assertThat(foundPurchase).isNotNull
        assertThat(foundPurchase!!.purchaseToken).isEqualTo(oldPurchase.purchaseToken)
    }

    @Test
    fun `findPurchaseInPurchaseHistory does not find purchase if not in active purchases`() {
        val oldPurchase = stubGooglePurchase()
        val purchases = listOf(oldPurchase)

        mockClient.mockQueryPurchasesAsync(
            subsResult = billingClientOKResult,
            inAppResult = billingClientOKResult,
            subPurchases = purchases,
            inAppPurchases = emptyList(),
        )

        var error: PurchasesError? = null
        wrapper.findPurchaseInPurchaseHistory(
            appUserID = "test-app-user-id",
            productType = ProductType.SUBS,
            productId = "unpurchased-product-id",
            onCompletion = { fail("Should be an error") },
            onError = { error = it },
        )

        assertThat(error).isNotNull
        assertThat(error!!.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
        assertThat(error!!.underlyingErrorMessage).isEqualTo(
            PurchaseStrings.NO_EXISTING_PURCHASE.format("unpurchased-product-id"),
        )
    }

    // endregion findPurchaseInActivePurchases

    // region BillingClient queryAllPurchases

    @Test
    fun `getting all purchases gets both subs and inapps`() {
        assumeFalse(BuildConfig.ENABLE_QUERY_PURCHASE_HISTORY_AIDL)

        val builder = mockClient.mockQueryPurchasesAsync(
            subsResult = billingClientOKResult,
            inAppResult = billingClientOKResult,
            subPurchases = listOf(stubGooglePurchase()),
            inAppPurchases = emptyList(),
        )

        var receivedPurchases = listOf<StoreTransaction>()
        wrapper.queryAllPurchases(
            appUserID = "appUserID",
            onReceivePurchaseHistory = {
                receivedPurchases = it
            },
            onReceivePurchaseHistoryError = { fail("Shouldn't be error") },
        )

        assertThat(receivedPurchases.size).isNotZero
        verify(exactly = 1) { (builder as QueryPurchasesParams.Builder).setProductType(subsGoogleProductType) }
        verify(exactly = 1) { (builder as QueryPurchasesParams.Builder).setProductType(inAppGoogleProductType) }

        verify(exactly = 2) { mockClient.queryPurchasesAsync(any<QueryPurchasesParams>(), any()) }

        clearStaticMockk(QueryPurchasesParams::class)
    }

    // endregion BillingClient queryAllPurchases
}
