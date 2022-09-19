package com.revenuecat.purchases.google

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.PurchasesResponseListener
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.utils.mockQueryPurchasesAsync
import io.mockk.every
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.AssertionsForClassTypes.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class BillingWrapperBC4Test : BillingWrapperTestBase() {

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

        assertThat(numCallbacks == 1)
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
    fun `getPurchaseType returns UNKNOWN if sub not found and inapp responses not OK`() {
        val subPurchaseToken = "subToken"

        val querySubPurchasesListenerSlot = slot<PurchasesResponseListener>()
        every {
            mockClient.queryPurchasesAsync(
                BillingClient.SkuType.SUBS,
                capture(querySubPurchasesListenerSlot)
            )
        } answers {
            querySubPurchasesListenerSlot.captured.onQueryPurchasesResponse(
                billingClientOKResult,
                getMockedPurchaseList(subPurchaseToken)
            )
        }

        val errorResult = BillingClient.BillingResponseCode.ERROR.buildResult()
        val inAppPurchaseToken = "inAppToken"
        val queryInAppPurchasesListenerSlot = slot<PurchasesResponseListener>()
        every {
            mockClient.queryPurchasesAsync(
                BillingClient.SkuType.INAPP,
                capture(queryInAppPurchasesListenerSlot)
            )
        } answers {
            queryInAppPurchasesListenerSlot.captured.onQueryPurchasesResponse(
                errorResult,
                getMockedPurchaseList(inAppPurchaseToken)
            )
        }

        wrapper.getPurchaseType(inAppPurchaseToken) { productType ->
            assertThat(productType).isEqualTo(ProductType.UNKNOWN)
        }
    }

}
