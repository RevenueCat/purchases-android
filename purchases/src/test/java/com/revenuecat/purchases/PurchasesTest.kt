package com.revenuecat.purchases

import android.app.Activity
import android.app.Application
import android.support.test.runner.AndroidJUnit4

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.Purchases.AttributionNetwork.Companion.APPSFLYER

import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

import java.util.ArrayList
import java.util.HashMap

import io.mockk.*
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertSame

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PurchasesTest {

    private val mockApplication: Application = mockk(relaxed = true)
    private val mockBillingWrapper: BillingWrapper = mockk(relaxed = true)
    private val mockBillingWrapperFactory: BillingWrapper.Factory = mockk(relaxed = true)
    private val mockBackend: Backend = mockk(relaxed = true)
    private val mockCache: DeviceCache = mockk(relaxed = true)
    private val listener: Purchases.PurchasesListener = mockk(relaxed = true)

    private val capturedPurchaseHistoryResponseListener =
        slot<BillingWrapper.PurchaseHistoryResponseListener>()
    private var capturedActivityLifecycleCallbacks = slot<Application.ActivityLifecycleCallbacks>()
    private var purchasesUpdatedListener = slot<BillingWrapper.PurchasesUpdatedListener>()
    private val capturedSubscriberInfoHandler = slot<Backend.BackendResponseHandler>()
    private val capturedListener = slot<BillingWrapper.SkuDetailsResponseListener>()
    private val capturedEntitlementResponseHandler = slot<Backend.EntitlementsResponseHandler>()

    private val appUserId = "fakeUserID"

    private var purchases: Purchases? = null

    private var receivedSkus: List<SkuDetails>? = null

    private var receivedEntitlementMap: Map<String, Entitlement>? = null

    private fun setup() {
        every {
            mockCache.getCachedAppUserID()
        } returns null

        every {
            mockApplication.registerActivityLifecycleCallbacks(
                capture(
                    capturedActivityLifecycleCallbacks
                )
            )
        } just Runs

        every {
            mockBackend.getSubscriberInfo(eq(appUserId), capture(capturedSubscriberInfoHandler))
        } answers {
            capturedSubscriberInfoHandler.captured.onReceivePurchaserInfo(mockk())
        }

        every {
            mockBackend.postReceiptData(
                any(),
                any(),
                any(),
                any(),
                capture(capturedSubscriberInfoHandler)
            )
        } answers {
            capturedSubscriberInfoHandler.captured.onReceivePurchaserInfo(mockk())
        }

        every {
            mockBillingWrapperFactory.buildWrapper(capture(purchasesUpdatedListener))
        } returns mockBillingWrapper

        val mockInfo = mockk<PurchaserInfo>()
        every {
            mockCache.getCachedPurchaserInfo(any())
        } returns mockInfo

        purchases = Purchases(
            mockApplication,
            appUserId,
            listener,
            mockBackend,
            mockBillingWrapperFactory,
            mockCache
        )
    }

    @Test
    fun canBeCreated() {
        setup()
        assertNotNull(purchases)
    }

    private fun mockSkuDetailFetch(details: List<SkuDetails>, skus: List<String>, skuType: String) {
        every {
            mockBillingWrapper.querySkuDetailsAsync(
                eq(skuType),
                eq(skus),
                capture(capturedListener)
            )
        } answers {
            capturedListener.captured.onReceiveSkuDetails(details)
        }
    }

    @Test
    fun getsSubscriptionSkus() {
        setup()

        val skus = ArrayList<String>()
        skus.add("onemonth_freetrial")

        val skuDetails = ArrayList<SkuDetails>()

        mockSkuDetailFetch(skuDetails, skus, BillingClient.SkuType.SUBS)

        purchases!!.getSubscriptionSkus(skus, object : Purchases.GetSkusResponseHandler {
            override fun onReceiveSkus(skus: List<SkuDetails>) {
                this@PurchasesTest.receivedSkus = skus
            }
        })

        assertSame(receivedSkus, skuDetails)
    }

    @Test
    fun getsNonSubscriptionSkus() {
        setup()

        val skus = ArrayList<String>()
        skus.add("normal_purchase")

        val skuDetails = ArrayList<SkuDetails>()

        mockSkuDetailFetch(skuDetails, skus, BillingClient.SkuType.INAPP)

        purchases!!.getNonSubscriptionSkus(skus, object : Purchases.GetSkusResponseHandler {
            override fun onReceiveSkus(skus: List<SkuDetails>) {
                this@PurchasesTest.receivedSkus = skus
            }
        })

        assertSame(receivedSkus, skuDetails)
    }

    @Test
    fun canMakePurchase() {
        setup()

        val activity: Activity = mockk()
        val sku = "onemonth_freetrial"
        val oldSkus = ArrayList<String>()

        purchases!!.makePurchase(activity, sku, BillingClient.SkuType.SUBS)

        verify {
            mockBillingWrapper.makePurchaseAsync(
                eq(activity),
                eq(appUserId),
                eq(sku),
                eq(oldSkus),
                eq(BillingClient.SkuType.SUBS)
            )
        }

    }

    @Test
    fun postsSuccessfulPurchasesToBackend() {
        setup()

        val p: Purchase = mockk()
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        every {
            p.sku
        } returns sku
        every {
            p.purchaseToken
        } returns purchaseToken

        val purchasesList = ArrayList<Purchase>()

        purchasesList.add(p)

        purchases!!.onPurchasesUpdated(purchasesList)

        verify {
            mockBackend.postReceiptData(
                eq(purchaseToken),
                eq(appUserId),
                eq(sku),
                eq(false),
                any()
            )
        }
        verify {
            mockBillingWrapper.consumePurchase(eq(purchaseToken))
        }
    }

    @Test
    fun callsPostForEachUpdatedPurchase() {
        setup()

        val purchasesList = ArrayList<Purchase>()
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        for (i in 0..1) {
            val p: Purchase = mockk()
            every {
                p.sku
            } returns sku
            every {
                p.purchaseToken
            } returns purchaseToken + Integer.toString(i)
            purchasesList.add(p)
        }


        purchases!!.onPurchasesUpdated(purchasesList)

        verify(exactly = 2) {
            mockBackend.postReceiptData(
                any(),
                eq(appUserId),
                eq(sku),
                eq(false),
                any()
            )
        }
    }

    @Test
    fun doesntPostIfNotOK() {
        setup()

        purchases!!.onPurchasesFailedToUpdate(0, "fail")

        verify (exactly = 0){
            mockBackend.postReceiptData(
                any(),
                any(),
                any(),
                eq(false),
                any()
            )
        }
    }

    @Test
    fun passesUpErrors() {
        setup()

        purchases!!.onPurchasesFailedToUpdate(0, "")

        verify {
            listener.onFailedPurchase(
                eq(Purchases.ErrorDomains.PLAY_BILLING),
                eq(0),
                any()
            )
        }
    }

    @Test
    fun addsAnApplicationLifecycleListener() {
        setup()

        verify {
            mockApplication.registerActivityLifecycleCallbacks(any())
        }
    }

    @Test
    fun closingUnregistersLifecycleListener() {
        setup()

        purchases!!.close()

        verify {
            mockApplication.unregisterActivityLifecycleCallbacks(any())
        }
    }

    @Test
    fun onResumeGetsSubscriberInfo() {
        setup()

        capturedActivityLifecycleCallbacks.captured.onActivityResumed(mockk())

        verify {
            mockBackend.getSubscriberInfo(eq(appUserId), any())
        }
        verify(exactly = 3) {
            listener.onReceiveUpdatedPurchaserInfo(any())
        }
    }

    @Test
    fun getsSubscriberInfoOnCreated() {
        setup()

        verify {
            mockBackend.getSubscriberInfo(eq(appUserId), any())
        }
    }

    @Test
    fun canBeSetupWithoutAppUserID() {
        setup()

        val purchases = Purchases(
            mockApplication,
            null,
            listener,
            mockBackend,
            mockBillingWrapperFactory,
            mockCache
        )
        assertNotNull(purchases)

        val appUserID = purchases.appUserID
        assertNotNull(appUserID)
        assertEquals(36, appUserID.length)
    }

    @Test
    fun storesGeneratedAppUserID() {
        setup()

        Purchases(
            mockApplication,
            null,
            listener,
            mockBackend,
            mockBillingWrapperFactory,
            mockCache
        )
        verify {
            mockCache.cacheAppUserID(any())
        }
    }

    @Test
    fun pullsUserIDFromCache() {
        setup()

        val appUserID = "random_id"
        every {
            mockCache.getCachedAppUserID()
        } returns appUserID
        val p = Purchases(
            mockApplication,
            null,
            listener,
            mockBackend,
            mockBillingWrapperFactory,
            mockCache
        )
        assertEquals(appUserID, p.appUserID)
    }

    @Test
    fun isRestoreWhenUsingNullAppUserID() {
        setup()

        val purchases = Purchases(
            mockApplication,
            null,
            listener,
            mockBackend,
            mockBillingWrapperFactory,
            mockCache
        )

        val p: Purchase = mockk()
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        every {
            p.sku
        } returns sku
        every {
            p.purchaseToken
        } returns purchaseToken

        val purchasesList = ArrayList<Purchase>()

        purchasesList.add(p)

        purchases.onPurchasesUpdated(purchasesList)

        verify {
            mockBackend.postReceiptData(
                eq(purchaseToken),
                eq(purchases.appUserID),
                eq(sku),
                eq(true),
                any()
            )
        }
    }

    @Test
    fun doesntRestoreNormally() {
        setup()

        val purchases = Purchases(
            mockApplication,
            "a_fixed_id",
            listener,
            mockBackend,
            mockBillingWrapperFactory,
            mockCache
        )

        val p: Purchase = mockk()
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        every {
            p.sku
        } returns sku
        every {
            p.purchaseToken
        } returns purchaseToken

        val purchasesList = ArrayList<Purchase>()

        purchasesList.add(p)

        purchases.onPurchasesUpdated(purchasesList)

        verify {
            mockBackend.postReceiptData(
                eq(purchaseToken),
                eq(purchases.appUserID),
                eq(sku),
                eq(false),
                any()
            )
        }
    }

    @Test
    fun canOverideAnonMode() {
        setup()

        val purchases = Purchases(
            mockApplication,
            "a_fixed_id",
            listener,
            mockBackend,
            mockBillingWrapperFactory,
            mockCache
        )
        purchases.setIsUsingAnonymousID(true)

        val p: Purchase = mockk()
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        every {
            p.sku
        } returns sku
        every {
            p.purchaseToken
        } returns purchaseToken

        val purchasesList = ArrayList<Purchase>()

        purchasesList.add(p)

        purchases.onPurchasesUpdated(purchasesList)

        verify {
            mockBackend.postReceiptData(
                eq(purchaseToken),
                eq(purchases.appUserID),
                eq(sku),
                eq(true),
                any()
            )
        }
    }

    @Test
    fun restoringPurchasesGetsHistory() {
        setup()

        every {
            mockBillingWrapper.queryPurchaseHistoryAsync(
                any(),
                capture(capturedPurchaseHistoryResponseListener)
            )
        } answers {
            capturedPurchaseHistoryResponseListener.captured.onReceivePurchaseHistory(ArrayList())
        }

        purchases!!.restorePurchasesForPlayStoreAccount()

        verify(exactly = 2) {
            mockBillingWrapper.queryPurchaseHistoryAsync(
                eq(BillingClient.SkuType.SUBS),
                any()
            )
        }

        verify {
            mockBillingWrapper.queryPurchaseHistoryAsync(
                eq(BillingClient.SkuType.INAPP),
                any()
            )
        }
    }

    @Test
    fun historicalPurchasesPassedToBackend() {
        setup()

        val p: Purchase = mockk()
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        every {
            p.sku
        } returns sku
        every {
            p.purchaseToken
        } returns purchaseToken

        val purchasesList = ArrayList<Purchase>()

        purchasesList.add(p)

        every {
            mockBillingWrapper.queryPurchaseHistoryAsync(
                any(),
                capture(capturedPurchaseHistoryResponseListener)
            )
        } answers {
            capturedPurchaseHistoryResponseListener.captured.onReceivePurchaseHistory(purchasesList)
        }

        purchases!!.restorePurchasesForPlayStoreAccount()

        verify {
            mockBackend.postReceiptData(
                eq(purchaseToken),
                eq(purchases!!.appUserID),
                eq(sku),
                eq(true),
                any()
            )
        }
        verify(exactly = 2) {
            listener.onReceiveUpdatedPurchaserInfo(any())
        }
        verify {
            listener.onRestoreTransactions(any())
        }
        verify(exactly = 0) {
            listener.onCompletedPurchase(
                any(),
                any()
            )
        }
    }

    @Test
    fun failedToRestorePurchases() {
        setup()

        every {
            mockBillingWrapper.queryPurchaseHistoryAsync(any(), capture(capturedPurchaseHistoryResponseListener))
        } answers {
            capturedPurchaseHistoryResponseListener.captured.onReceivePurchaseHistoryError(0, "Broken")

        }

        purchases!!.restorePurchasesForPlayStoreAccount()

        verify(exactly = 2) {
            listener.onReceiveUpdatedPurchaserInfo(any())
        }
        verify {
            listener.onRestoreTransactionsFailed(
                eq(Purchases.ErrorDomains.PLAY_BILLING),
                eq(0),
                eq("Broken")
            )
        }
        verify (exactly = 0) {
            listener.onCompletedPurchase(
                any(),
                any()
            )
        }
    }

    @Test
    fun restoringCallsRestoreCallback() {
        setup()

        val p: Purchase = mockk()
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        every {
            p.sku
        } returns sku
        every {
            p.purchaseToken
        } returns purchaseToken

        val purchasesList = ArrayList<Purchase>()

        purchasesList.add(p)

        every {
            mockBillingWrapper.queryPurchaseHistoryAsync(
                eq(BillingClient.SkuType.SUBS),
                capture(capturedPurchaseHistoryResponseListener)
            )
        } answers {
            capturedPurchaseHistoryResponseListener.captured.onReceivePurchaseHistory(purchasesList)
        }

        every {
            mockBillingWrapper.queryPurchaseHistoryAsync(
                eq(BillingClient.SkuType.INAPP),
                capture(capturedPurchaseHistoryResponseListener)
            )
        } answers {
            capturedPurchaseHistoryResponseListener.captured.onReceivePurchaseHistory(ArrayList())
        }

        every {
            mockBackend.postReceiptData(
                any(),
                any(),
                any(),
                eq(true),
                capture(capturedSubscriberInfoHandler)
            )
        } answers {
            capturedSubscriberInfoHandler.captured.onReceivePurchaserInfo(mockk())
        }

        purchases!!.restorePurchasesForPlayStoreAccount()

        verify(exactly = 3) {
            mockBillingWrapper.queryPurchaseHistoryAsync(any(), any())
        }

        verify {
            listener.onRestoreTransactions(any())
        }

    }

    @Test
    fun doesntDoublePostReceipts() {
        setup()

        val p1: Purchase = mockk()
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        every {
            p1.sku
        } returns sku
        every {
            p1.purchaseToken
        } returns purchaseToken

        val p2: Purchase = mockk()
        every {
            p2.sku
        } returns sku
        every {
            p2.purchaseToken
        } returns purchaseToken

        val p3: Purchase = mockk()
        every {
            p3.sku
        } returns sku
        every {
            p3.purchaseToken
        } returns purchaseToken + "diff"

        val purchasesList = ArrayList<Purchase>()
        purchasesList.add(p1)
        purchasesList.add(p2)
        purchasesList.add(p3)

        purchasesUpdatedListener.captured.onPurchasesUpdated(purchasesList)

        verify(exactly = 2) {
            mockBackend.postReceiptData(
                any(),
                eq(purchases!!.appUserID),
                eq(sku),
                eq(false),
                any()
            )
        }
    }

    @Test
    fun cachedUserInfoShouldGoToListener() {
        setup()

        verify {
            listener.onReceiveUpdatedPurchaserInfo(any())
        }
    }

    @Test
    fun cachedUserInfoEmitOnResumeActive() {
        setup()
        verify(exactly = 2) {
            listener.onReceiveUpdatedPurchaserInfo(any())
        }
        purchases!!.onActivityResumed(mockk())
        verify(exactly = 3) {
            listener.onReceiveUpdatedPurchaserInfo(any())
        }
    }

    @Test
    fun receivedPurchaserInfoShouldBeCached() {
        setup()

        val p: Purchase = mockk()
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        every {
            p.sku
        } returns sku
        every {
            p.purchaseToken
        } returns purchaseToken

        val purchasesList = ArrayList<Purchase>()

        purchasesList.add(p)

        purchases!!.onPurchasesUpdated(purchasesList)

        verify {
            mockBackend.postReceiptData(
                eq(purchaseToken),
                eq(appUserId),
                eq(sku),
                eq(false),
                any()
            )
        }
        verify(exactly = 2) {
            mockCache.cachePurchaserInfo(
                any(),
                any()
            )
        }

    }

    @Test
    fun getEntitlementsHitsBackend() {
        mockProducts(ArrayList())
        mockSkuDetails(ArrayList(), ArrayList(), "subs")

        setup()

        verify {
            mockBackend.getEntitlements(any(), any())
        }
    }

    private fun mockProducts(skus: List<String>) {
        every {
            mockBackend.getEntitlements(any(), capture(capturedEntitlementResponseHandler))
        } answers {
            val offeringMap = HashMap<String, Offering>()

            for (sku in skus) {
                val o = Offering(sku)
                offeringMap[sku + "_offering"] = o
            }

            val entitlementMap = HashMap<String, Entitlement>()
            val e = Entitlement(offeringMap)
            entitlementMap["pro"] = e

            capturedEntitlementResponseHandler.captured.onReceiveEntitlements(entitlementMap)
        }
    }

    private fun mockSkuDetails(
        skus: List<String>,
        returnSkus: List<String>,
        type: String
    ): List<SkuDetails> {
        val skuDetails = ArrayList<SkuDetails>()

        for (sku in returnSkus) {
            val details = mockk<SkuDetails>()
            every { details.sku } returns sku
            skuDetails.add(details)
        }

        mockSkuDetailFetch(skuDetails, skus, type)
        return skuDetails
    }

    @Test
    fun getEntitlementsPopulatesMissingSkuDetails() {
        val skus = ArrayList<String>()
        skus.add("monthly")

        mockProducts(skus)
        val details = mockSkuDetails(skus, skus, BillingClient.SkuType.SUBS)

        setup()

        purchases!!.getEntitlements(object : Purchases.GetEntitlementsHandler {
            override fun onReceiveEntitlements(entitlementMap: Map<String, Entitlement>) {
                this@PurchasesTest.receivedEntitlementMap = entitlementMap
            }

            override fun onReceiveEntitlementsError(domain: Int, code: Int, message: String) {

            }
        })

        assertNotNull(receivedEntitlementMap)

        verify {
            mockBillingWrapper.querySkuDetailsAsync(
                eq(BillingClient.SkuType.SUBS),
                eq<List<String>>(skus),
                any()
            )
        }

        val e = receivedEntitlementMap!!["pro"]
        assertEquals(1, e!!.offerings.size)
        val o = e.offerings["monthly_offering"]
        assertSame(details[0], o!!.skuDetails)
    }

    @Test
    fun getEntitlementsDoesntCheckInappsUnlessThereAreMissingSubs() {

        val skus = ArrayList<String>()
        val subsSkus = ArrayList<String>()
        skus.add("monthly")
        subsSkus.add("monthly")

        val inappSkus = ArrayList<String>()
        skus.add("monthly_inapp")
        inappSkus.add("monthly_inapp")

        mockProducts(skus)
        mockSkuDetails(skus, subsSkus, BillingClient.SkuType.SUBS)
        mockSkuDetails(inappSkus, inappSkus, BillingClient.SkuType.INAPP)

        setup()

        every {
            mockBillingWrapper.querySkuDetailsAsync(
                eq(BillingClient.SkuType.SUBS),
                eq<List<String>>(skus),
                any()
            )
        }
        every {
            mockBillingWrapper.querySkuDetailsAsync(
                eq(BillingClient.SkuType.INAPP),
                eq<List<String>>(inappSkus),
                any()
            )
        }
    }

    @Test
    fun getEntitlementsIsCached() {

        val skus = ArrayList<String>()
        skus.add("monthly")
        mockProducts(skus)

        mockSkuDetails(skus, skus, BillingClient.SkuType.SUBS)

        setup()

        verify {
            mockBackend.getEntitlements(
                eq(appUserId),
                any()
            )
        }

        purchases!!.getEntitlements(object : Purchases.GetEntitlementsHandler {
            override fun onReceiveEntitlements(entitlementMap: Map<String, Entitlement>) {
                this@PurchasesTest.receivedEntitlementMap = entitlementMap
            }

            override fun onReceiveEntitlementsError(domain: Int, code: Int, message: String) {

            }
        })

        assertNotNull(receivedEntitlementMap)
    }

    @Test
    fun getEntitlementsErrorIsCalledIfSkuDetailsMissing() {

        setup()

        val skus = ArrayList<String>()
        skus.add("monthly")
        mockProducts(skus)
        mockSkuDetails(skus, ArrayList(), BillingClient.SkuType.SUBS)
        mockSkuDetails(skus, ArrayList(), BillingClient.SkuType.INAPP)

        val errorMessage = emptyArray<String>()

        purchases!!.getEntitlements(object : Purchases.GetEntitlementsHandler {
            override fun onReceiveEntitlements(entitlementMap: Map<String, Entitlement>) {
                this@PurchasesTest.receivedEntitlementMap = entitlementMap
            }

            override fun onReceiveEntitlementsError(domain: Int, code: Int, message: String) {
                errorMessage[0] = message
            }
        })

        assertEquals(errorMessage.size, 0)
        assertNotNull(this.receivedEntitlementMap)
    }

    @Test
    fun getEntitlementsErrorIsCalledIfNoBackendResponse() {
        setup()

        every {
            mockBackend.getEntitlements(
                any(),
                capture(capturedEntitlementResponseHandler)
            )
        } answers {
            capturedEntitlementResponseHandler.captured.onError(0, "nope")
        }

        val errorMessage = arrayOf("")

        purchases!!.getEntitlements(object : Purchases.GetEntitlementsHandler {
            override fun onReceiveEntitlements(entitlementMap: Map<String, Entitlement>) {}

            override fun onReceiveEntitlementsError(domain: Int, code: Int, message: String) {
                errorMessage[0] = message
            }
        })

        assertNotNull(errorMessage[0])
    }

    @Test
    fun addAttributionPassesDataToBackend() {
        setup()

        val `object` = mockk<JSONObject>()
        @Purchases.AttributionNetwork val network = APPSFLYER
        purchases!!.addAttributionData(`object`, network)

        verify { mockBackend.postAttributionData(eq(appUserId), eq(network), eq(`object`)) }
    }

    @Test
    fun addAttributionConvertsStringStringMapToJsonObject() {
        setup()

        val map = HashMap<String, String>()
        map["key"] = "value"

        @Purchases.AttributionNetwork val network = APPSFLYER
        purchases!!.addAttributionData(map, network)

        verify {
            mockBackend.postAttributionData(
                eq(appUserId),
                eq(network),
                any()
            )
        }
    }

    @Test
    fun consumesNonSubscriptionPurchasesOn40x() {
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        val code = 402

        every {
            mockBackend.postReceiptData(
                eq(purchaseToken),
                eq(appUserId),
                eq(sku),
                eq(false),
                capture(capturedSubscriberInfoHandler)
            )
        } answers {
            capturedSubscriberInfoHandler.captured.onError(code, "This is fake")
        }

        setup()

        val p: Purchase = mockk()

        every {
            p.sku
        } returns sku
        every {
            p.purchaseToken
        } returns purchaseToken

        val purchasesList = ArrayList<Purchase>()

        purchasesList.add(p)
        purchases!!.onPurchasesUpdated(purchasesList)

        verify {
            mockBillingWrapper.consumePurchase(eq(purchaseToken))
        }
    }

    @Test
    fun triesToConsumeNonSubscriptionPurchasesOn50x() {
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        val code = 502

        every {
            mockBackend.postReceiptData(
                eq(purchaseToken),
                eq(appUserId),
                eq(sku),
                eq(false),
                capture(capturedSubscriberInfoHandler)
            )
        } answers {
            capturedSubscriberInfoHandler.captured.onError(code, "This is fake")
        }

        setup()

        val p: Purchase = mockk()

        every {
            p.sku
        } returns sku
        every {
            p.purchaseToken
        } returns purchaseToken

        val purchasesList = ArrayList<Purchase>()

        purchasesList.add(p)
        purchases!!.onPurchasesUpdated(purchasesList)

        verify {
            mockBillingWrapper.consumePurchase(eq(purchaseToken))
        }
    }

    @Test
    fun closeCloses() {
        setup()
        purchases!!.close()

        verify { mockBackend.close() }
        verify { mockBillingWrapper.close() }
    }

    @Test
    fun whenNoTokensRestoringPurchasesStillCallListener() {
        setup()

        every {
            mockBillingWrapper.queryPurchaseHistoryAsync(
                any(),
                capture(capturedPurchaseHistoryResponseListener)
            )
        } answers {
            capturedPurchaseHistoryResponseListener.captured.onReceivePurchaseHistory(ArrayList())
        }

        purchases!!.restorePurchasesForPlayStoreAccount()

        verify(exactly = 2) {
            listener.onReceiveUpdatedPurchaserInfo(any())
        }
        verify { listener.onRestoreTransactions(any()) }
    }
}
