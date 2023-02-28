package com.revenuecat.purchases.google

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.SkuDetailsResponseListener
import com.revenuecat.purchases.ProductType
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifySequence
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.AssertionsForClassTypes.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import java.util.concurrent.CountDownLatch
import kotlin.time.Duration.Companion.milliseconds

@Suppress("TooManyFunctions")
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class BillingWrapperBC4Test : BillingWrapperTestBase() {

    private companion object {
        const val timestamp0 = 1676379370000 // Tuesday, February 14, 2023 12:56:10.000 PM GMT
        const val timestamp123 = 1676379370123 // Tuesday, February 14, 2023 12:56:10.123 PM GMT
        const val timestamp500 = 1676379370500 // Tuesday, February 14, 2023 12:56:10.500 PM GMT
        const val timestamp900 = 1676379370900 // Tuesday, February 14, 2023 12:56:10.900 PM GMT
    }

    @Test
    fun `queryPurchaseHistoryAsync only calls one response when BillingClient responds twice`() {
        var numCallbacks = 0

        val slot = slot<PurchaseHistoryResponseListener>()
        every {
            mockClient.queryPurchaseHistoryAsync(
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onPurchaseHistoryResponse(billingClientOKResult, null)
            slot.captured.onPurchaseHistoryResponse(billingClientOKResult, null)
        }

        wrapper.queryPurchaseHistoryAsync(
            BillingClient.SkuType.SUBS,
            {
                numCallbacks++
            }, {
                fail("shouldn't be an error")
            })

        assertThat(numCallbacks).isEqualTo(1)
    }

    @Test
    fun `queryPurchaseHistoryAsync only calls one response when BillingClient responds twice from different threads`() {
        var numCallbacks = 0

        val slot = slot<PurchaseHistoryResponseListener>()
        val lock = CountDownLatch(2)
        every {
            mockClient.queryPurchaseHistoryAsync(
                any(),
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
            BillingClient.SkuType.SUBS,
            {
                // ensuring we don't hit an edge case where numCallbacks doesn't increment before the final assert
                handler.post {
                    numCallbacks++
                }
            }, {
                fail("shouldn't be an error")
            })

        lock.await()
        assertThat(lock.count).isEqualTo(0)

        assertThat(numCallbacks).isEqualTo(1)
    }

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
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onPurchaseHistoryResponse(result, null)
        }

        wrapper.queryPurchaseHistoryAsync(BillingClient.SkuType.SUBS, {}, { fail("shouldn't be an error") })

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGoogleQueryPurchaseHistoryRequest(
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
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onPurchaseHistoryResponse(result, null)
        }

        wrapper.queryPurchaseHistoryAsync(BillingClient.SkuType.SUBS, { fail("should be an error") }, {})

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGoogleQueryPurchaseHistoryRequest(
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                billingDebugMessage = "test-debug-message",
                responseTime = 123.milliseconds
            )
        }
    }

    @Test
    fun `querySkuDetailsAsync tracks diagnostics call with correct parameters`() {
        every { mockDateProvider.now } returnsMany listOf(Date(timestamp0), Date(timestamp123))

        val result = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.OK)
            .setDebugMessage("test-debug-message")
            .build()
        val slot = slot<SkuDetailsResponseListener>()
        every {
            mockClient.querySkuDetailsAsync(
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onSkuDetailsResponse(result, emptyList())
        }

        wrapper.querySkuDetailsAsync(ProductType.SUBS, setOf("test-sku"), {}, { fail("shouldn't be an error") })

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGoogleQuerySkuDetailsRequest(
                BillingClient.SkuType.SUBS,
                BillingClient.BillingResponseCode.OK,
                billingDebugMessage = "test-debug-message",
                responseTime = 123.milliseconds
            )
        }
    }

    @Test
    fun `querySkuDetailsAsync tracks diagnostics call with correct parameters on error`() {
        every { mockDateProvider.now } returnsMany listOf(Date(timestamp0), Date(timestamp123))

        val result = BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.DEVELOPER_ERROR)
            .setDebugMessage("test-debug-message")
            .build()
        val slot = slot<SkuDetailsResponseListener>()
        every {
            mockClient.querySkuDetailsAsync(
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onSkuDetailsResponse(result, emptyList())
        }

        wrapper.querySkuDetailsAsync(ProductType.SUBS, setOf("test-sku"), { fail("should be an error") }, {})

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGoogleQuerySkuDetailsRequest(
                BillingClient.SkuType.SUBS,
                BillingClient.BillingResponseCode.DEVELOPER_ERROR,
                billingDebugMessage = "test-debug-message",
                responseTime = 123.milliseconds
            )
        }
    }

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
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onQueryPurchasesResponse(result, emptyList())
        }

        wrapper.queryPurchases(appUserId, {}, { fail("shouldn't be an error") })

        verifySequence {
            mockDiagnosticsTracker.trackGoogleQueryPurchasesRequest(
                BillingClient.SkuType.SUBS,
                BillingClient.BillingResponseCode.OK,
                billingDebugMessage = "test-debug-message",
                responseTime = 123.milliseconds
            )
            mockDiagnosticsTracker.trackGoogleQueryPurchasesRequest(
                BillingClient.SkuType.INAPP,
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
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onQueryPurchasesResponse(result, emptyList())
        }

        wrapper.queryPurchases(appUserId, { fail("should be an error") }, {})

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGoogleQueryPurchasesRequest(
                BillingClient.SkuType.SUBS,
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED,
                billingDebugMessage = "test-debug-message",
                responseTime = 123.milliseconds
            )
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
            BillingClient.SkuType.SUBS,
            getMockedPurchaseList("subToken")
        )
        mockQueryPurchasesAsyncResponse(
            BillingClient.SkuType.INAPP,
            getMockedPurchaseList("inappToken")
        )

        var returnedType: ProductType? = null
        wrapper.getPurchaseType("inappToken") { returnedType = it }
        assertThat(returnedType).isEqualTo(ProductType.INAPP)

        verifySequence {
            mockDiagnosticsTracker.trackGoogleQueryPurchasesRequest(
                BillingClient.SkuType.SUBS,
                BillingClient.BillingResponseCode.OK,
                billingDebugMessage = "",
                responseTime = 123.milliseconds
            )
            mockDiagnosticsTracker.trackGoogleQueryPurchasesRequest(
                BillingClient.SkuType.INAPP,
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
            BillingClient.SkuType.SUBS,
            getMockedPurchaseList("subToken")
        )

        var returnedType: ProductType? = null
        wrapper.getPurchaseType("subToken") { returnedType = it }
        assertThat(returnedType).isEqualTo(ProductType.SUBS)

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackGoogleQueryPurchasesRequest(
                BillingClient.SkuType.SUBS,
                BillingClient.BillingResponseCode.OK,
                billingDebugMessage = "",
                responseTime = 123.milliseconds
            )
        }
    }

    @Test
    fun `getPurchaseType returns UNKNOWN if sub not found and inapp responses not OK`() {
        val subPurchaseToken = "subToken"

        mockQueryPurchasesAsyncResponse(
            BillingClient.SkuType.SUBS,
            getMockedPurchaseList(subPurchaseToken)
        )

        val errorResult = BillingClient.BillingResponseCode.ERROR.buildResult()
        val inAppPurchaseToken = "inAppToken"
        mockQueryPurchasesAsyncResponse(
            BillingClient.SkuType.INAPP,
            getMockedPurchaseList(inAppPurchaseToken),
            errorResult
        )

        wrapper.getPurchaseType(inAppPurchaseToken) { productType ->
            assertThat(productType).isEqualTo(ProductType.UNKNOWN)
        }
    }

    private fun mockQueryPurchasesAsyncResponse(
        @BillingClient.SkuType queryType: String,
        purchasesToReturn: List<Purchase>,
        billingResult: BillingResult = billingClientOKResult
    ) {
        val queryPurchasesListenerSlot = slot<PurchasesResponseListener>()
        every {
            mockClient.queryPurchasesAsync(
                queryType,
                capture(queryPurchasesListenerSlot)
            )
        } answers {
            queryPurchasesListenerSlot.captured.onQueryPurchasesResponse(
                billingResult,
                purchasesToReturn
            )
        }
    }
}
