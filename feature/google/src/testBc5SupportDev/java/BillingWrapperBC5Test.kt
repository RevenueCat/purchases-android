package com.revenuecat.purchases.google

import android.app.Activity
import android.content.Intent
import android.os.Handler
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ConsumeResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.PurchaseHistoryResponseListener
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchaseHistoryParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.SkuDetailsResponseListener
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.ReplaceSkuInfo
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.common.sha256
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.mockQueryPurchaseHistory
import com.revenuecat.purchases.utils.mockQueryPurchasesAsync
import com.revenuecat.purchases.utils.stubGooglePurchase
import com.revenuecat.purchases.utils.stubPurchaseHistoryRecord
import com.revenuecat.purchases.utils.stubSkuDetails
import io.mockk.Runs
import io.mockk.clearStaticMockk
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.AssertionsForClassTypes.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.lang.Thread.sleep
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class BillingWrapperBC5Test {
    private var onConnectedCalled: Boolean = false
    private var mockClientFactory: BillingWrapper.ClientFactory = mockk()
    private var mockClient: BillingClient = mockk()
    private var purchasesUpdatedListener: PurchasesUpdatedListener? = null
    private var billingClientStateListener: BillingClientStateListener? = null
    private var handler: Handler = mockk()
    private var mockDeviceCache: DeviceCache = mockk()

    private var mockPurchasesListener: BillingAbstract.PurchasesUpdatedListener = mockk()

    private var capturedAcknowledgeResponseListener = slot<AcknowledgePurchaseResponseListener>()
    private var capturedAcknowledgePurchaseParams = slot<AcknowledgePurchaseParams>()
    private var capturedConsumeResponseListener = slot<ConsumeResponseListener>()
    private var capturedConsumeParams = slot<ConsumeParams>()

    private lateinit var wrapper: BillingWrapper

    private lateinit var mockDetailsList: List<SkuDetails>

    private var storeProducts: List<StoreProduct>? = null

    private val billingClientOKResult = BillingClient.BillingResponseCode.OK.buildResult()
    private val appUserId = "jerry"
    private var mockActivity = mockk<Activity>()

    @Before
    fun setup() {
        setupRunnables()

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
        } just runs

        every {
            mockClient.acknowledgePurchase(
                capture(capturedAcknowledgePurchaseParams),
                capture(capturedAcknowledgeResponseListener)
            )
        } just Runs

        every {
            mockClient.consumeAsync(capture(capturedConsumeParams), capture(capturedConsumeResponseListener))
        } just Runs

        every {
            mockClient.isReady
        } returns false andThen true

        mockDetailsList = listOf(stubSkuDetails())

        wrapper = BillingWrapper(mockClientFactory, handler, mockDeviceCache)
        wrapper.purchasesUpdatedListener = mockPurchasesListener
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

    @Test
    fun `queryPurchaseHistoryAsync fails if sent invalid type`() {
        // TODO pull to common once refactoring queryPurchaseHistoryAsync to take RCProductType
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

        assertThat(numCallbacks == 1)
    }

    @Test
    fun `queryPurchaseHistoryAsync only calls one response when BillingClient responds twice from different threads`() {
        var numCallbacks = 0

        val slot = slot<PurchaseHistoryResponseListener>()
        val lock = CountDownLatch(2)
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

    private fun getMockedPurchaseList(purchaseToken: String): List<Purchase> {
        return listOf(mockk(
            relaxed = true
        ) {
            every { this@mockk.purchaseToken } returns purchaseToken
        })
    }

    private fun mockNullSkuDetailsResponse() {
        val slot = slot<SkuDetailsResponseListener>()
        every {
            mockClient.querySkuDetailsAsync(
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onSkuDetailsResponse(billingClientOKResult, null)
        }
    }

    private fun Int.buildResult(): BillingResult {
        return BillingResult.newBuilder().setResponseCode(this).build()
    }

    private fun mockPurchaseHistoryRecordWrapper(): StoreTransaction {
        val oldPurchase = stubPurchaseHistoryRecord(
            productIds = listOf("product_b"),
            purchaseToken = "atoken"
        )

        return oldPurchase.toStoreTransaction(type = ProductType.SUBS)
    }

    private fun mockReplaceSkuInfo(): ReplaceSkuInfo {
        val oldPurchase = mockPurchaseHistoryRecordWrapper()
        return ReplaceSkuInfo(oldPurchase, BillingFlowParams.ProrationMode.DEFERRED)
    }

    private fun getMockedPurchaseWrapper(
        sku: String,
        purchaseToken: String,
        productType: ProductType,
        offeringIdentifier: String? = null,
        purchaseState: Int = Purchase.PurchaseState.PURCHASED,
        acknowledged: Boolean = false
    ): StoreTransaction {
        val p = stubGooglePurchase(
            productIds = listOf(sku),
            purchaseToken = purchaseToken,
            purchaseState = purchaseState,
            acknowledged = acknowledged
        )

        return p.toStoreTransaction(productType, offeringIdentifier)
    }

    private fun getMockedPurchaseHistoryRecordWrapper(
        sku: String,
        purchaseToken: String,
        productType: ProductType
    ): StoreTransaction {
        val p: PurchaseHistoryRecord = stubPurchaseHistoryRecord(
            productIds = listOf(sku),
            purchaseToken = purchaseToken
        )

        return p.toStoreTransaction(
            type = productType
        )
    }

    private fun setUpForObfuscatedAccountIDTests(): BillingFlowParams.Builder {
        mockkStatic(BillingFlowParams::class)
        val mockBuilder = mockk<BillingFlowParams.Builder>(relaxed = true)
        every {
            BillingFlowParams.newBuilder()
        } returns mockBuilder

        every {
            mockBuilder.setSkuDetails(any())
        } returns mockBuilder

        val params = mockk<BillingFlowParams>(relaxed = true)
        every {
            mockBuilder.build()
        } returns params

        every {
            mockClient.launchBillingFlow(any(), params)
        } returns billingClientOKResult

        billingClientStateListener!!.onBillingSetupFinished(billingClientOKResult)

        return mockBuilder
    }

    private fun mockStandardSkuDetailsResponse() {
        val slot = slot<SkuDetailsResponseListener>()
        every {
            mockClient.querySkuDetailsAsync(
                any(),
                capture(slot)
            )
        } answers {
            slot.captured.onSkuDetailsResponse(billingClientOKResult, mockDetailsList)
        }
    }

    private fun setupRunnables() {
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
}
