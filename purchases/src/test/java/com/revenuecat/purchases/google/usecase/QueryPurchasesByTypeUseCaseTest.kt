package com.revenuecat.purchases.google.usecase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.QueryPurchasesParams
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.google.BillingWrapperTest
import com.revenuecat.purchases.google.buildQueryPurchasesParams
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.mockQueryPurchasesAsync
import com.revenuecat.purchases.utils.stubGooglePurchase
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifySequence
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.assertj.core.data.Offset
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
internal class QueryPurchasesByTypeUseCaseTest : BaseBillingUseCaseTest() {

    private var billingClientStateListener: BillingClientStateListener? = null

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
    fun `Getting INAPPs type`() {
        val inAppToken = "inAppToken"
        val subToken = "subToken"

        mockClient.mockQueryPurchasesAsync(
            billingClientOKResult,
            billingClientOKResult,
            getMockedPurchaseList(subToken),
            getMockedPurchaseList(inAppToken)
        )

        wrapper.getPurchaseType(inAppToken) { productType ->
            assertThat(productType).isEqualTo(ProductType.INAPP)
        }

    }

    @Test
    fun `Getting SUBS type`() {
        val inAppToken = "inAppToken"
        val subsToken = "subsToken"

        mockClient.mockQueryPurchasesAsync(
            billingClientOKResult,
            billingClientOKResult,
            getMockedPurchaseList(subsToken),
            getMockedPurchaseList(inAppToken)
        )

        wrapper.getPurchaseType(subsToken) { productType ->
            assertThat(productType).isEqualTo(ProductType.SUBS)
        }
    }

    @Test
    fun `getPurchaseType returns UNKNOWN if sub and inapps response not OK`() {
        val errorResult = BillingClient.BillingResponseCode.ERROR.buildResult()
        val subToken = "subToken"
        val inAppToken = "abcd"

        mockClient.mockQueryPurchasesAsync(
            errorResult,
            errorResult,
            getMockedPurchaseList(subToken),
            getMockedPurchaseList(inAppToken)
        )

        wrapper.getPurchaseType(inAppToken) { productType ->
            assertThat(productType).isEqualTo(ProductType.UNKNOWN)
        }
    }

    @Test
    fun `getPurchaseType returns UNKNOWN if sub not found and inapp responses not OK`() {
        val subPurchaseToken = "subToken"
        val inAppPurchaseToken = "inAppToken"

        mockClient.mockQueryPurchasesAsync(
            subsResult = billingClientOKResult,
            inAppResult = billingClientErrorResult,
            subPurchases = getMockedPurchaseList(subPurchaseToken),
            inAppPurchases = getMockedPurchaseList(inAppPurchaseToken)
        )

        wrapper.getPurchaseType(inAppPurchaseToken) { productType ->
            assertThat(productType).isEqualTo(ProductType.UNKNOWN)
        }
    }

    @Test
    fun `getPurchaseType tracks query purchases diagnostics calls for subs and inapp`() {
        every {
            mockDateProvider.now
        } returnsMany listOf(
            Date(timestamp0),
            Date(timestamp123),
            Date(timestamp500),
            Date(timestamp900)
        )

        mockQueryPurchasesAsyncResponse(
            BillingClient.ProductType.SUBS,
            "subToken",
        )
        mockQueryPurchasesAsyncResponse(
            BillingClient.ProductType.INAPP,
            "inappToken"
        )

        var returnedType: ProductType? = null
        wrapper.getPurchaseType("inappToken") { returnedType = it }
        assertThat(returnedType).isEqualTo(ProductType.INAPP)

        verifySequence {
            mockDiagnosticsTracker.trackGoogleQueryPurchasesRequest(
                BillingClient.ProductType.SUBS,
                BillingClient.BillingResponseCode.OK,
                billingDebugMessage = "",
                responseTime = 123.milliseconds
            )
            mockDiagnosticsTracker.trackGoogleQueryPurchasesRequest(
                BillingClient.ProductType.INAPP,
                BillingClient.BillingResponseCode.OK,
                billingDebugMessage = "",
                responseTime = 400.milliseconds
            )
        }
    }

    @Test
    fun `getPurchaseType tracks query purchases diagnostics calls for subs if found as subs`() {
        every { mockDateProvider.now } returnsMany listOf(Date(timestamp0), Date(timestamp123))

        mockQueryPurchasesAsyncResponse(
            BillingClient.ProductType.SUBS,
            "subToken",
        )

        var returnedType: ProductType? = null
        wrapper.getPurchaseType("subToken") { returnedType = it }
        assertThat(returnedType).isEqualTo(ProductType.SUBS)

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGoogleQueryPurchasesRequest(
                BillingClient.ProductType.SUBS,
                BillingClient.BillingResponseCode.OK,
                billingDebugMessage = "",
                responseTime = 123.milliseconds
            )
        }
    }


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
        QueryPurchasesByTypeUseCase(
            QueryPurchasesByTypeUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                appInBackground = false,
                BillingClient.ProductType.INAPP,
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

                request(null)
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
        QueryPurchasesByTypeUseCase(
            QueryPurchasesByTypeUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                appInBackground = false,
                BillingClient.ProductType.INAPP,
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
                queryPurchasesStubbing answers {
                    slot.captured.onQueryPurchasesResponse(
                        BillingClient.BillingResponseCode.NETWORK_ERROR.buildResult(),
                        emptyList()
                    )
                }

                request(null)
            },
        ).run()

        assertThat(timesRetried).isEqualTo(4) // First attempt plus 3 retries
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.NetworkError)
    }

    @Test
    fun `If service returns SERVICE_UNAVAILABLE, retry with backoff a few times then error if user in session`() {
        val slot = slot<PurchasesResponseListener>()
        val queryPurchasesStubbing = every {
            mockClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                capture(slot)
            )
        }
        var receivedError: PurchasesError? = null
        var timesRetried = 0
        val capturedDelays = mutableListOf<Long>()
        QueryPurchasesByTypeUseCase(
            QueryPurchasesByTypeUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                appInBackground = false,
                BillingClient.ProductType.INAPP,
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
                queryPurchasesStubbing answers {
                    slot.captured.onQueryPurchasesResponse(
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult(),
                        emptyList()
                    )
                }

                request(null)
            },
        ).run()

        assertThat(timesRetried).isEqualTo(4)
        assertThat(capturedDelays.last()).isCloseTo(RETRY_TIMER_MAX_TIME_MILLISECONDS_FOREGROUND, Offset.offset(1000L))
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `If service returns SERVICE_UNAVAILABLE, re-execute with backoff`() {
        val slot = slot<PurchasesResponseListener>()
        val queryPurchasesStubbing = every {
            mockClient.queryPurchasesAsync(
                any<QueryPurchasesParams>(),
                capture(slot)
            )
        }
        var receivedError: PurchasesError? = null
        val capturedDelays = mutableListOf<Long>()
        QueryPurchasesByTypeUseCase(
            QueryPurchasesByTypeUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                appInBackground = true,
                BillingClient.ProductType.INAPP,
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
                queryPurchasesStubbing answers {
                    slot.captured.onQueryPurchasesResponse(
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult(),
                        emptyList()
                    )
                }

                request(null)
            },
        ).run()

        assertThat(capturedDelays.size).isEqualTo(12)
        assertThat(capturedDelays.last()).isCloseTo(RETRY_TIMER_MAX_TIME_MILLISECONDS, Offset.offset(1000L))
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
        QueryPurchasesByTypeUseCase(
            QueryPurchasesByTypeUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                appInBackground = false,
                BillingClient.ProductType.INAPP,
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
                queryPurchasesStubbing answers {
                    slot.captured.onQueryPurchasesResponse(
                        BillingClient.BillingResponseCode.ERROR.buildResult(),
                        emptyList()
                    )
                }

                request(null)
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
        QueryPurchasesByTypeUseCase(
            QueryPurchasesByTypeUseCaseParams(
                mockDateProvider,
                mockDiagnosticsTracker,
                appInBackground = false,
                BillingClient.ProductType.INAPP,
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
                queryPurchasesStubbing answers {
                    slot.captured.onQueryPurchasesResponse(
                        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE.buildResult(),
                        emptyList()
                    )
                }

                request(null)
            },
        ).run()

        assertThat(timesRetried).isEqualTo(1)
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.ProductNotAvailableForPurchaseError)
    }

    // endregion retries

    private fun getMockedPurchaseList(purchaseToken: String): List<Purchase> {
        return listOf(stubGooglePurchase(purchaseToken = purchaseToken))
    }

    private fun mockQueryPurchasesAsyncResponse(
        @BillingClient.ProductType productType: String,
        token: String,
        billingResult: BillingResult = billingClientOKResult
    ) {
        val queryPurchasesListenerSlot = slot<PurchasesResponseListener>()
        every {
            mockClient.queryPurchasesAsync(
                productType.buildQueryPurchasesParams()!!,
                capture(queryPurchasesListenerSlot)
            )
        } answers {
            queryPurchasesListenerSlot.captured.onQueryPurchasesResponse(
                billingResult,
                getMockedPurchaseList(token)
            )
        }
    }
}
