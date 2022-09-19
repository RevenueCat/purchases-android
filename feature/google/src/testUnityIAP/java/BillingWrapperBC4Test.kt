package com.revenuecat.purchases.google

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchaseHistoryResponseListener
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
        // TODO pull to common once refactoring queryPurchaseHistoryAsync to take RCProductType
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
        // TODO pull to common once refactoring queryPurchaseHistoryAsync to take RCProductType
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

}
