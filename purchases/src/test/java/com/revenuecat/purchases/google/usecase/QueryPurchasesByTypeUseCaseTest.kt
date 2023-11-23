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
    fun `If service returns SERVICE_UNAVAILABLE, don't retry and error if user in session`() {
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
                        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE.buildResult(),
                        emptyList()
                    )
                }

                request(null)
            },
        ).run()

        assertThat(timesRetried).isEqualTo(1)
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

}