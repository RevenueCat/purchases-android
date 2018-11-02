package com.revenuecat.purchases

import android.app.Activity
import android.app.Application

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails

import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import java.util.ArrayList
import java.util.HashMap

import com.revenuecat.purchases.Purchases.AttributionNetwork.Companion.APPSFLYER
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertSame
import org.junit.Assert.assertNull
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PurchasesTest {

    private val mockApplication = mock(Application::class.java)
    private val mockBillingWrapper = mock(BillingWrapper::class.java)
    private val mockBillingWrapperFactory = mock(BillingWrapper.Factory::class.java)
    private val mockBackend = mock(Backend::class.java)
    private val mockCache = mock(DeviceCache::class.java)


    private var activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null
    private var purchasesUpdatedListener: BillingWrapper.PurchasesUpdatedListener? = null

    private val appUserId = "fakeUserID"

    private val listener = mock(Purchases.PurchasesListener::class.java)

    private var purchases: Purchases? = null

    private var receivedSkus: List<SkuDetails>? = null

    private var historyListener: BillingWrapper.PurchaseHistoryResponseListener? = null

    private var receivedEntitlementMap: Map<String, Entitlement>? = null

    private fun setup() {

        doAnswer(object : Answer {
            @Throws(Throwable::class)
            override fun answer(invocation: InvocationOnMock): Any? {
                activityLifecycleCallbacks = invocation.getArgument<ActivityLifecycleCallbacks>(0)
                return null
            }
        }).`when`(mockApplication)
            .registerActivityLifecycleCallbacks(any(Application.ActivityLifecycleCallbacks::class.java))

        doAnswer(object : Answer {
            override fun answer(invocation: InvocationOnMock): Any? {
                val handler = invocation.getArgument<Backend.BackendResponseHandler>(1)
                handler.onReceivePurchaserInfo(mock(PurchaserInfo::class.java))
                return null
            }
        }).`when`(mockBackend)
            .getSubscriberInfo(eq(appUserId), any(Backend.BackendResponseHandler::class.java))

        doAnswer(object : Answer {
            override fun answer(invocation: InvocationOnMock): Any? {
                val handler = invocation.getArgument<Backend.BackendResponseHandler>(4)
                handler.onReceivePurchaserInfo(mock(PurchaserInfo::class.java))
                return null
            }
        }).`when`(mockBackend).postReceiptData(
            any(String::class.java),
            any(String::class.java),
            any(String::class.java),
            any(Boolean::class.java),
            any(Backend.BackendResponseHandler::class.java)
        )

        doAnswer(object : Answer {
            override fun answer(invocation: InvocationOnMock): Any {
                purchasesUpdatedListener = invocation.getArgument<PurchasesUpdatedListener>(0)
                return mockBillingWrapper
            }
        }).`when`<Factory>(mockBillingWrapperFactory)
            .buildWrapper(any(BillingWrapper.PurchasesUpdatedListener::class.java))

        val mockInfo = mock(PurchaserInfo::class.java)
        `when`<PurchaserInfo>(mockCache.getCachedPurchaserInfo(any(String::class.java))).thenReturn(
            mockInfo
        )

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
        doAnswer(object : Answer {
            override fun answer(invocation: InvocationOnMock): Any? {
                val listener = invocation.getArgument<BillingWrapper.SkuDetailsResponseListener>(2)
                listener.onReceiveSkuDetails(details)
                return null
            }
        }).`when`(mockBillingWrapper).querySkuDetailsAsync(
            eq(skuType),
            eq(skus), any(BillingWrapper.SkuDetailsResponseListener::class.java)
        )
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

        val activity = mock(Activity::class.java)
        val sku = "onemonth_freetrial"
        val oldSkus = ArrayList<String>()

        purchases!!.makePurchase(activity, sku, BillingClient.SkuType.SUBS)

        verify(mockBillingWrapper).makePurchaseAsync(
            activity,
            appUserId,
            sku,
            oldSkus,
            BillingClient.SkuType.SUBS
        )
    }

    @Test
    fun postsSuccessfulPurchasesToBackend() {
        setup()

        val p = mock(Purchase::class.java)
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        `when`(p.sku).thenReturn(sku)
        `when`(p.purchaseToken).thenReturn(purchaseToken)

        val purchasesList = ArrayList<Purchase>()

        purchasesList.add(p)

        purchases!!.onPurchasesUpdated(purchasesList)

        verify(mockBackend).postReceiptData(
            eq(purchaseToken),
            eq(appUserId),
            eq(sku),
            eq(false),
            any(Backend.BackendResponseHandler::class.java)
        )

        verify(mockBillingWrapper, times(1)).consumePurchase(eq(purchaseToken))
    }

    @Test
    fun callsPostForEachUpdatedPurchase() {
        setup()

        val purchasesList = ArrayList<Purchase>()
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        for (i in 0..1) {
            val p = mock(Purchase::class.java)
            `when`(p.sku).thenReturn(sku)
            `when`(p.purchaseToken).thenReturn(purchaseToken + Integer.toString(i))
            purchasesList.add(p)
        }


        purchases!!.onPurchasesUpdated(purchasesList)

        verify(mockBackend, times(2)).postReceiptData(
            any(String::class.java),
            eq(appUserId),
            eq(sku),
            eq(false),
            any(Backend.BackendResponseHandler::class.java)
        )
    }

    @Test
    fun doesntPostIfNotOK() {
        setup()

        purchases!!.onPurchasesFailedToUpdate(0, "fail")

        verify(mockBackend, times(0)).postReceiptData(
            any(String::class.java),
            any(String::class.java),
            any(String::class.java),
            eq(false),
            any(Backend.BackendResponseHandler::class.java)
        )
    }

    @Test
    fun passesUpErrors() {
        setup()

        purchases!!.onPurchasesFailedToUpdate(0, "")

        verify<PurchasesListener>(listener).onFailedPurchase(
            eq(Purchases.ErrorDomains.PLAY_BILLING),
            eq(0),
            any(String::class.java)
        )
    }

    @Test
    fun addsAnApplicationLifecycleListener() {
        setup()

        verify(mockApplication).registerActivityLifecycleCallbacks(any(Application.ActivityLifecycleCallbacks::class.java))
    }

    @Test
    fun closingUnregistersLifecycleListener() {
        setup()

        purchases!!.close()

        verify(mockApplication).unregisterActivityLifecycleCallbacks(any(Application.ActivityLifecycleCallbacks::class.java))
    }

    @Test
    fun onResumeGetsSubscriberInfo() {
        setup()

        activityLifecycleCallbacks!!.onActivityResumed(mock(Activity::class.java))

        verify(mockBackend).getSubscriberInfo(
            eq(appUserId),
            any(Backend.BackendResponseHandler::class.java)
        )
        verify<PurchasesListener>(listener, times(3)).onReceiveUpdatedPurchaserInfo(
            any(
                PurchaserInfo::class.java
            )
        )
    }

    @Test
    fun getsSubscriberInfoOnCreated() {
        setup()

        verify(mockBackend).getSubscriberInfo(
            eq(appUserId),
            any(Backend.BackendResponseHandler::class.java)
        )
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
        verify(mockCache).cacheAppUserID(any(String::class.java))
    }

    @Test
    fun pullsUserIDFromCache() {
        setup()

        val appUserID = "random_id"
        `when`<String>(mockCache.getCachedAppUserID()).thenReturn(appUserID)
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

        val p = mock(Purchase::class.java)
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        `when`(p.sku).thenReturn(sku)
        `when`(p.purchaseToken).thenReturn(purchaseToken)

        val purchasesList = ArrayList<Purchase>()

        purchasesList.add(p)

        purchases.onPurchasesUpdated(purchasesList)

        verify(mockBackend).postReceiptData(
            eq(purchaseToken),
            eq(purchases.appUserID),
            eq(sku),
            eq(true),
            any(Backend.BackendResponseHandler::class.java)
        )
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

        val p = mock(Purchase::class.java)
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        `when`(p.sku).thenReturn(sku)
        `when`(p.purchaseToken).thenReturn(purchaseToken)

        val purchasesList = ArrayList<Purchase>()

        purchasesList.add(p)

        purchases.onPurchasesUpdated(purchasesList)

        verify(mockBackend).postReceiptData(
            eq(purchaseToken),
            eq(purchases.appUserID),
            eq(sku),
            eq(false),
            any(Backend.BackendResponseHandler::class.java)
        )
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

        val p = mock(Purchase::class.java)
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        `when`(p.sku).thenReturn(sku)
        `when`(p.purchaseToken).thenReturn(purchaseToken)

        val purchasesList = ArrayList<Purchase>()

        purchasesList.add(p)

        purchases.onPurchasesUpdated(purchasesList)

        verify(mockBackend).postReceiptData(
            eq(purchaseToken),
            eq(purchases.appUserID),
            eq(sku),
            eq(true),
            any(Backend.BackendResponseHandler::class.java)
        )
    }

    @Test
    fun restoringPurchasesGetsHistory() {
        setup()

        doAnswer(object : Answer {
            @Throws(Throwable::class)
            override fun answer(invocation: InvocationOnMock): Any? {
                historyListener = invocation.getArgument<PurchaseHistoryResponseListener>(1)
                historyListener!!.onReceivePurchaseHistory(ArrayList())
                return null
            }
        }).`when`(mockBillingWrapper).queryPurchaseHistoryAsync(
            any(String::class.java),
            any(BillingWrapper.PurchaseHistoryResponseListener::class.java)
        )

        purchases!!.restorePurchasesForPlayStoreAccount()

        verify(mockBillingWrapper, times(2)).queryPurchaseHistoryAsync(
            eq(BillingClient.SkuType.SUBS),
            any(BillingWrapper.PurchaseHistoryResponseListener::class.java)
        )

        verify(mockBillingWrapper).queryPurchaseHistoryAsync(
            eq(BillingClient.SkuType.INAPP),
            any(BillingWrapper.PurchaseHistoryResponseListener::class.java)
        )
    }

    @Test
    fun historicalPurchasesPassedToBackend() {
        setup()

        val p = mock(Purchase::class.java)
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        `when`(p.sku).thenReturn(sku)
        `when`(p.purchaseToken).thenReturn(purchaseToken)

        val purchasesList = ArrayList<Purchase>()

        purchasesList.add(p)

        doAnswer(object : Answer {
            @Throws(Throwable::class)
            override fun answer(invocation: InvocationOnMock): Any? {
                historyListener = invocation.getArgument<PurchaseHistoryResponseListener>(1)
                historyListener!!.onReceivePurchaseHistory(purchasesList)
                return null
            }
        }).`when`(mockBillingWrapper).queryPurchaseHistoryAsync(
            any(String::class.java),
            any(BillingWrapper.PurchaseHistoryResponseListener::class.java)
        )

        purchases!!.restorePurchasesForPlayStoreAccount()

        verify(mockBackend, times(1)).postReceiptData(
            eq(purchaseToken),
            eq(purchases!!.appUserID),
            eq(sku),
            eq(true),
            any(Backend.BackendResponseHandler::class.java)
        )

        verify<PurchasesListener>(listener, times(2)).onReceiveUpdatedPurchaserInfo(
            any(
                PurchaserInfo::class.java
            )
        )
        verify<PurchasesListener>(
            listener,
            times(1)
        ).onRestoreTransactions(any(PurchaserInfo::class.java))
        verify<PurchasesListener>(listener, times(0)).onCompletedPurchase(
            any(String::class.java),
            any(PurchaserInfo::class.java)
        )
    }

    @Test
    fun failedToRestorePurchases() {
        setup()

        doAnswer(object : Answer {
            @Throws(Throwable::class)
            override fun answer(invocation: InvocationOnMock): Any? {
                historyListener = invocation.getArgument<PurchaseHistoryResponseListener>(1)
                historyListener!!.onReceivePurchaseHistoryError(0, "Broken")
                return null
            }
        }).`when`(mockBillingWrapper).queryPurchaseHistoryAsync(
            any(String::class.java),
            any(BillingWrapper.PurchaseHistoryResponseListener::class.java)
        )

        purchases!!.restorePurchasesForPlayStoreAccount()

        verify(listener, times(2)).onReceiveUpdatedPurchaserInfo(any(PurchaserInfo::class.java))
        verify(listener, times(1)).onRestoreTransactionsFailed(
            Purchases.ErrorDomains.PLAY_BILLING,
            0,
            "Broken"
        )
        verify<PurchasesListener>(listener, times(0)).onCompletedPurchase(
            any(String::class.java),
            any(PurchaserInfo::class.java)
        )
    }

    @Test
    fun restoringCallsRestoreCallback() {
        setup()

        val p = mock(Purchase::class.java)
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        `when`(p.sku).thenReturn(sku)
        `when`(p.purchaseToken).thenReturn(purchaseToken)

        val purchasesList = ArrayList<Purchase>()

        purchasesList.add(p)

        doAnswer(object : Answer {
            @Throws(Throwable::class)
            override fun answer(invocation: InvocationOnMock): Any? {
                val listener =
                    invocation.getArgument<BillingWrapper.PurchaseHistoryResponseListener>(1)
                listener.onReceivePurchaseHistory(purchasesList)
                return null
            }
        }).`when`(mockBillingWrapper).queryPurchaseHistoryAsync(
            eq(BillingClient.SkuType.SUBS),
            any(BillingWrapper.PurchaseHistoryResponseListener::class.java)
        )

        doAnswer(object : Answer {
            @Throws(Throwable::class)
            override fun answer(invocation: InvocationOnMock): Any? {
                val listener =
                    invocation.getArgument<BillingWrapper.PurchaseHistoryResponseListener>(1)
                listener.onReceivePurchaseHistory(ArrayList())
                return null
            }
        }).`when`(mockBillingWrapper).queryPurchaseHistoryAsync(
            eq(BillingClient.SkuType.INAPP),
            any(BillingWrapper.PurchaseHistoryResponseListener::class.java)
        )

        doAnswer(object : Answer {
            @Throws(Throwable::class)
            override fun answer(invocation: InvocationOnMock): Any? {
                val handler = invocation.getArgument<Backend.BackendResponseHandler>(4)
                handler.onReceivePurchaserInfo(mock(PurchaserInfo::class.java))
                return null
            }
        }).`when`(mockBackend).postReceiptData(
            any(String::class.java),
            any(String::class.java),
            any(String::class.java),
            eq(true),
            any(Backend.BackendResponseHandler::class.java)
        )

        purchases!!.restorePurchasesForPlayStoreAccount()

        verify(mockBillingWrapper, times(3)).queryPurchaseHistoryAsync(
            any(String::class.java),
            any(BillingWrapper.PurchaseHistoryResponseListener::class.java)
        )
        verify<PurchasesListener>(
            listener,
            times(1)
        ).onRestoreTransactions(any(PurchaserInfo::class.java))
    }

    @Test
    fun doesntDoublePostReceipts() {
        setup()

        val p1 = mock(Purchase::class.java)
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        `when`(p1.sku).thenReturn(sku)
        `when`(p1.purchaseToken).thenReturn(purchaseToken)

        val p2 = mock(Purchase::class.java)

        `when`(p2.sku).thenReturn(sku)
        `when`(p2.purchaseToken).thenReturn(purchaseToken)

        val p3 = mock(Purchase::class.java)
        `when`(p3.sku).thenReturn(sku)
        `when`(p3.purchaseToken).thenReturn(purchaseToken + "diff")

        val purchasesList = ArrayList<Purchase>()
        purchasesList.add(p1)
        purchasesList.add(p2)
        purchasesList.add(p3)

        purchasesUpdatedListener!!.onPurchasesUpdated(purchasesList)

        verify(mockBackend, times(2)).postReceiptData(
            any(String::class.java),
            eq(purchases!!.appUserID),
            eq(sku),
            eq(false),
            any(Backend.BackendResponseHandler::class.java)
        )
    }

    @Test
    fun cachedUserInfoShouldGoToListener() {
        setup()

        verify<PurchasesListener>(listener, times(2)).onReceiveUpdatedPurchaserInfo(
            any(
                PurchaserInfo::class.java
            )
        )
    }

    @Test
    fun cachedUserInfoEmitOnResumeActive() {
        setup()

        verify<PurchasesListener>(listener, times(2)).onReceiveUpdatedPurchaserInfo(
            any(
                PurchaserInfo::class.java
            )
        )
        purchases!!.onActivityResumed(mock(Activity::class.java))
        verify<PurchasesListener>(listener, times(3)).onReceiveUpdatedPurchaserInfo(
            any(
                PurchaserInfo::class.java
            )
        )
    }

    @Test
    fun receivedPurchaserInfoShouldBeCached() {
        setup()

        val p = mock(Purchase::class.java)
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        `when`(p.sku).thenReturn(sku)
        `when`(p.purchaseToken).thenReturn(purchaseToken)

        val purchasesList = ArrayList<Purchase>()

        purchasesList.add(p)

        purchases!!.onPurchasesUpdated(purchasesList)

        verify(mockBackend).postReceiptData(
            eq(purchaseToken),
            eq(appUserId),
            eq(sku),
            eq(false),
            any(Backend.BackendResponseHandler::class.java)
        )

        verify(mockCache, times(2)).cachePurchaserInfo(
            any(String::class.java),
            any(PurchaserInfo::class.java)
        )
    }

    @Test
    fun getEntitlementsHitsBackend() {
        mockProducts(ArrayList())
        mockSkuDetails(ArrayList(), ArrayList(), "subs")

        setup()

        verify(mockBackend).getEntitlements(
            any(String::class.java),
            any(Backend.EntitlementsResponseHandler::class.java)
        )
    }

    private fun mockProducts(skus: List<String>) {
        doAnswer(object : Answer {
            @Throws(Throwable::class)
            override fun answer(invocation: InvocationOnMock): Any? {
                val handler = invocation.getArgument<Backend.EntitlementsResponseHandler>(1)
                val offeringMap = HashMap<String, Offering>()

                for (sku in skus) {
                    val o = Offering(sku)
                    offeringMap[sku + "_offering"] = o
                }

                val entitlementMap = HashMap<String, Entitlement>()
                val e = Entitlement(offeringMap)
                entitlementMap["pro"] = e

                handler.onReceiveEntitlements(entitlementMap)
                return null
            }
        }).`when`(mockBackend).getEntitlements(
            any(String::class.java),
            any(Backend.EntitlementsResponseHandler::class.java)
        )
    }

    private fun mockSkuDetails(
        skus: List<String>,
        returnSkus: List<String>,
        type: String
    ): List<SkuDetails> {
        val skuDetails = ArrayList<SkuDetails>()

        for (sku in returnSkus) {
            val details = mock(SkuDetails::class.java)
            `when`(details.sku).thenReturn(sku)
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

        verify(mockBillingWrapper, times(1)).querySkuDetailsAsync(
            eq(BillingClient.SkuType.SUBS),
            eq<List<String>>(skus),
            any(BillingWrapper.SkuDetailsResponseListener::class.java)
        )

        val e = receivedEntitlementMap!!["pro"]
        assertEquals(1, e.offerings.size)
        val o = e.offerings["monthly_offering"]
        assertSame(details[0], o.skuDetails)
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

        verify(mockBillingWrapper).querySkuDetailsAsync(
            eq(BillingClient.SkuType.SUBS),
            eq<List<String>>(skus),
            any(BillingWrapper.SkuDetailsResponseListener::class.java)
        )
        verify(mockBillingWrapper).querySkuDetailsAsync(
            eq(BillingClient.SkuType.INAPP),
            eq<List<String>>(inappSkus),
            any(BillingWrapper.SkuDetailsResponseListener::class.java)
        )
    }

    @Test
    fun getEntitlementsIsCached() {

        val skus = ArrayList<String>()
        skus.add("monthly")
        mockProducts(skus)

        mockSkuDetails(skus, skus, BillingClient.SkuType.SUBS)

        setup()

        verify(mockBackend, times(1)).getEntitlements(
            eq(appUserId),
            any(Backend.EntitlementsResponseHandler::class.java)
        )

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

        val errorMessage = arrayOf<String>(null)


        purchases!!.getEntitlements(object : Purchases.GetEntitlementsHandler {
            override fun onReceiveEntitlements(entitlementMap: Map<String, Entitlement>) {
                this@PurchasesTest.receivedEntitlementMap = entitlementMap
            }

            override fun onReceiveEntitlementsError(domain: Int, code: Int, message: String) {
                errorMessage[0] = message
            }
        })

        assertNull(errorMessage[0])
        assertNotNull(this.receivedEntitlementMap)
    }

    @Test
    fun getEntitlementsErrorIsCalledIfNoBackendResponse() {

        setup()
        doAnswer(object : Answer {
            @Throws(Throwable::class)
            override fun answer(invocation: InvocationOnMock): Any? {
                val handler = invocation.getArgument<Backend.EntitlementsResponseHandler>(1)
                handler.onError(0, "nope")
                return null
            }
        }).`when`(mockBackend).getEntitlements(
            any(String::class.java),
            any(Backend.EntitlementsResponseHandler::class.java)
        )

        val errorMessage = arrayOf<String>(null)

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

        val `object` = mock(JSONObject::class.java)
        @Purchases.AttributionNetwork val network = APPSFLYER
        purchases!!.addAttributionData(`object`, network)

        verify(mockBackend).postAttributionData(appUserId, network, `object`)
    }

    @Test
    fun addAttributionConvertsStringStringMapToJsonObject() {
        setup()

        val map = HashMap<String, String>()
        map["key"] = "value"

        @Purchases.AttributionNetwork val network = APPSFLYER
        purchases!!.addAttributionData(map, network)

        verify(mockBackend).postAttributionData(
            eq(appUserId),
            eq(network),
            any(JSONObject::class.java)
        )
    }

    @Test
    fun consumesNonSubscriptionPurchasesOn40x() {
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        val code = 402

        doAnswer(object : Answer {
            @Throws(Throwable::class)
            override fun answer(invocation: InvocationOnMock): Any? {
                val handler = invocation.getArgument<Backend.BackendResponseHandler>(4)
                handler.onError(code, "This is fake")
                return null
            }
        }).`when`(mockBackend).postReceiptData(
            eq(purchaseToken),
            eq(appUserId),
            eq(sku),
            eq(false),
            any(Backend.BackendResponseHandler::class.java)
        )

        setup()

        val p = mock(Purchase::class.java)

        `when`(p.sku).thenReturn(sku)
        `when`(p.purchaseToken).thenReturn(purchaseToken)

        val purchasesList = ArrayList<Purchase>()

        purchasesList.add(p)
        purchases!!.onPurchasesUpdated(purchasesList)

        verify(mockBillingWrapper).consumePurchase(eq(purchaseToken))
    }

    @Test
    fun triesToConsumeNonSubscriptionPurchasesOn50x() {
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        val code = 502

        doAnswer(object : Answer {
            @Throws(Throwable::class)
            override fun answer(invocation: InvocationOnMock): Any? {
                val handler = invocation.getArgument<Backend.BackendResponseHandler>(4)
                handler.onError(code, "This is fake")
                return null
            }
        }).`when`(mockBackend).postReceiptData(
            eq(purchaseToken),
            eq(appUserId),
            eq(sku),
            eq(false),
            any(Backend.BackendResponseHandler::class.java)
        )

        setup()

        val p = mock(Purchase::class.java)

        `when`(p.sku).thenReturn(sku)
        `when`(p.purchaseToken).thenReturn(purchaseToken)

        val purchasesList = ArrayList<Purchase>()

        purchasesList.add(p)
        purchases!!.onPurchasesUpdated(purchasesList)

        verify(mockBillingWrapper).consumePurchase(eq(purchaseToken))
    }

    @Test
    fun closeCloses() {
        setup()
        purchases!!.close()

        verify(mockBackend).close()
        verify(mockBillingWrapper).close()
    }

    @Test
    fun whenNoTokensRestoringPurchasesStillCallListener() {
        setup()

        doAnswer(object : Answer {
            override fun answer(invocation: InvocationOnMock): Any? {
                historyListener = invocation.getArgument<PurchaseHistoryResponseListener>(1)
                historyListener!!.onReceivePurchaseHistory(ArrayList())
                return null
            }
        }).`when`(mockBillingWrapper).queryPurchaseHistoryAsync(
            any(String::class.java),
            any(BillingWrapper.PurchaseHistoryResponseListener::class.java)
        )

        purchases!!.restorePurchasesForPlayStoreAccount()

        verify(listener, times(2)).onReceiveUpdatedPurchaserInfo(any(PurchaserInfo::class.java))
        verify(listener).onRestoreTransactions(any(PurchaserInfo::class.java))
    }
}
