//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.interfaces.GetSkusResponseListener
import com.revenuecat.purchases.interfaces.PurchaseCompletedListener
import com.revenuecat.purchases.interfaces.ReceiveEntitlementsListener
import com.revenuecat.purchases.interfaces.ReceivePurchaserInfoListener
import com.revenuecat.purchases.interfaces.UpdatedPurchaserInfoListener
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertSame
import junit.framework.Assert.fail
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.ArrayList

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PurchasesTest {

    private val mockBillingWrapper: BillingWrapper = mockk()
    private val mockBackend: Backend = mockk()
    private val mockCache: DeviceCache = mockk()
    private val listener: UpdatedPurchaserInfoListener = mockk()

    private var capturedPurchasesUpdatedListener = slot<BillingWrapper.PurchasesUpdatedListener>()

    private val appUserId = "fakeUserID"
    private lateinit var purchases: Purchases
    private var receivedSkus: List<SkuDetails>? = null
    private var receivedEntitlementMap: Map<String, Entitlement>? = null

    private fun setup() {
        val mockInfo = mockk<PurchaserInfo>()

        mockCache(mockInfo)
        mockBackend(mockInfo)
        mockBillingWrapper()
        every {
            listener.onReceived(any())
        } just Runs

        purchases = Purchases(
            appUserId,
            mockBackend,
            mockBillingWrapper,
            mockCache
        )
    }

    @Test
    fun canBeCreated() {
        setup()
        assertNotNull(purchases)
    }

    @Test
    fun getsSubscriptionSkus() {
        setup()

        val skus = ArrayList<String>()
        skus.add("onemonth_freetrial")

        val skuDetails = ArrayList<SkuDetails>()

        mockSkuDetailFetch(skuDetails, skus, BillingClient.SkuType.SUBS)

        purchases.getSubscriptionSkus(skus,
            GetSkusResponseListener { skuDetails ->
                this@PurchasesTest.receivedSkus = skuDetails
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

        purchases.getNonSubscriptionSkus(skus,
            GetSkusResponseListener { skuDetails ->
                this@PurchasesTest.receivedSkus = skuDetails
            })

        assertSame(receivedSkus, skuDetails)
    }

    @Test
    fun canMakePurchase() {
        setup()

        val activity: Activity = mockk()
        val sku = "onemonth_freetrial"
        val oldSkus = ArrayList<String>()

        purchases.makePurchase(
            activity,
            sku,
            BillingClient.SkuType.SUBS,
            completion = PurchaseCompletedListener { _, _, _ ->
            })

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

        purchases.onPurchasesUpdated(listOf(p))

        verify {
            mockBackend.postReceiptData(
                eq(purchaseToken),
                eq(appUserId),
                eq(sku),
                eq(false),
                any(),
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

        purchases.onPurchasesUpdated(purchasesList)

        verify(exactly = 2) {
            mockBackend.postReceiptData(
                any(),
                eq(appUserId),
                eq(sku),
                eq(false),
                any(),
                any()
            )
        }
    }

    @Test
    fun doesntPostIfNotOK() {
        setup()

        purchases.onPurchasesFailedToUpdate(emptyList(), 0, "fail")

        verify(exactly = 0) {
            mockBackend.postReceiptData(
                any(),
                any(),
                any(),
                eq(false),
                any(),
                any()
            )
        }
    }

    @Test
    fun passesUpErrors() {
        setup()
        var errorCalled = false
        purchases.makePurchase(
            mockk(),
            "sku",
            "SKUS",
            ArrayList(),
            PurchaseCompletedListener { _, _, error ->
                errorCalled = true
                assertThat(error).isEqualTo(
                    PurchasesError(
                        Purchases.ErrorDomains.PLAY_BILLING,
                        0,
                        ""
                    )
                )
            }
        )
        val purchase = mockk<Purchase>(relaxed = true)
        every { purchase.sku } returns "sku"
        purchases.onPurchasesFailedToUpdate(listOf(purchase), 0, "")
        assertThat(errorCalled).isTrue()
    }

    @Test
    fun getsSubscriberInfoOnCreated() {
        setup()

        verify {
            mockBackend.getPurchaserInfo(eq(appUserId), any(), any())
        }
    }

    @Test
    fun canBeSetupWithoutAppUserID() {
        val mockInfo = mockk<PurchaserInfo>()

        mockCache(mockInfo)
        mockBackend(mockInfo)
        mockBillingWrapper()

        val purchases = Purchases(
            null,
            mockBackend,
            mockBillingWrapper,
            mockCache
        )

        assertNotNull(purchases)

        val appUserID = purchases.appUserID
        assertNotNull(appUserID)
        assertEquals(36, appUserID.length)
    }

    @Test
    fun storesGeneratedAppUserID() {
        val mockInfo = mockk<PurchaserInfo>()

        mockCache(mockInfo)
        mockBackend(mockInfo)
        mockBillingWrapper()

        Purchases(
            null,
            mockBackend,
            mockBillingWrapper,
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
            null,
            mockBackend,
            mockBillingWrapper,
            mockCache
        )

        assertEquals(appUserID, p.appUserID)
    }

    @Test
    fun isRestoreWhenUsingNullAppUserID() {
        setup()

        val purchases = Purchases(
            null,
            mockBackend,
            mockBillingWrapper,
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
                any(),
                any()
            )
        }
    }

    @Test
    fun doesntRestoreNormally() {
        setup()

        val purchases = Purchases(
            "a_fixed_id",
            mockBackend,
            mockBillingWrapper,
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
                any(),
                any()
            )
        }
    }

    @Test
    fun canOverrideAnonMode() {
        setup()

        val purchases = Purchases(
            "a_fixed_id",
            mockBackend,
            mockBillingWrapper,
            mockCache
        )
        purchases.allowSharingPlayStoreAccount = true

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
                any(),
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
                captureLambda(),
                any()
            )
        } answers {
            lambda<(List<Purchase>) -> Unit>().captured.invoke(listOf(mockk(relaxed = true)))
        }

        purchases.restorePurchases(ReceivePurchaserInfoListener { _, _ -> })

        verify {
            mockBillingWrapper.queryPurchaseHistoryAsync(
                eq(BillingClient.SkuType.SUBS),
                any(),
                any()
            )
        }

        verify {
            mockBillingWrapper.queryPurchaseHistoryAsync(
                eq(BillingClient.SkuType.INAPP),
                any(),
                any()
            )
        }
    }

    @Test
    fun historicalPurchasesPassedToBackend() {
        setup()

        val p: Purchase = mockk(relaxed = true)
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        every {
            p.sku
        } returns sku
        every {
            p.purchaseToken
        } returns purchaseToken

        every {
            mockBillingWrapper.queryPurchaseHistoryAsync(
                any(),
                captureLambda(),
                any()
            )
        } answers {
            lambda<(List<Purchase>) -> Unit>().captured.invoke(listOf(p))
        }

        var restoreCalled = false
        purchases.restorePurchases(ReceivePurchaserInfoListener { purchaserInfo, purchasesError ->
            if (purchasesError != null) {
                fail("Should not be an error")
            }
            restoreCalled = true
        })
        assertThat(restoreCalled).isTrue()

        verify {
            mockBackend.postReceiptData(
                eq(purchaseToken),
                eq(purchases.appUserID),
                eq(sku),
                eq(true),
                any(),
                any()
            )
        }
    }

    @Test
    fun failedToRestorePurchases() {
        setup()
        val purchasesError = PurchasesError(
            Purchases.ErrorDomains.PLAY_BILLING,
            0,
            "Broken"
        )
        every {
            mockBillingWrapper.queryPurchaseHistoryAsync(any(), any(), captureLambda())
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(purchasesError)
        }

        var onErrorCalled = false
        purchases.restorePurchases(ReceivePurchaserInfoListener { _, error ->
            if (error != null) {
                onErrorCalled = true
                assertThat(error).isEqualTo(purchasesError)
            } else {
                fail("should be an error")
            }
        })

        assertThat(onErrorCalled).isTrue()
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
                captureLambda(),
                any()
            )
        } answers {
            lambda<(List<Purchase>) -> Unit>().captured.invoke(purchasesList)
        }

        every {
            mockBillingWrapper.queryPurchaseHistoryAsync(
                eq(BillingClient.SkuType.INAPP),
                captureLambda(),
                any()
            )
        } answers {
            lambda<(List<Purchase>) -> Unit>().captured.invoke(ArrayList())
        }
        val mockInfo = mockk<PurchaserInfo>()
        every {
            mockBackend.postReceiptData(
                any(),
                any(),
                any(),
                eq(true),
                captureLambda(),
                any()
            )
        } answers {
            lambda<(PurchaserInfo)->Unit>().captured.invoke(mockInfo)
        }

        var callbackCalled = false
        purchases.restorePurchases(ReceivePurchaserInfoListener { info, error ->
            assertThat(mockInfo).isEqualTo(info)
            assertThat(error).isNull()
            callbackCalled = true
        })

        verify(exactly = 2) {
            mockBillingWrapper.queryPurchaseHistoryAsync(any(), any(), any())
        }

        assertThat(callbackCalled).isTrue()
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

        purchases.onPurchasesUpdated(purchasesList)

        verify {
            mockBackend.postReceiptData(
                eq(purchaseToken),
                eq(appUserId),
                eq(sku),
                eq(false),
                any(),
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
        mockProducts(listOf())
        mockSkuDetails(listOf(), listOf(), "subs")

        setup()

        verify {
            mockBackend.getEntitlements(any(), any(), any())
        }
    }

    @Test
    fun getEntitlementsPopulatesMissingSkuDetails() {
        setup()

        val skus = listOf("monthly")

        mockProducts(skus)
        val details = mockSkuDetails(skus, skus, BillingClient.SkuType.SUBS)

        purchases.getEntitlements(ReceiveEntitlementsListener { entitlementMap, _ ->
            this@PurchasesTest.receivedEntitlementMap = entitlementMap
        })

        assertThat(receivedEntitlementMap).isNotNull

        verify {
            mockBillingWrapper.querySkuDetailsAsync(
                eq(BillingClient.SkuType.SUBS),
                eq(skus),
                any()
            )
        }

        val e = receivedEntitlementMap!!["pro"]
        assertThat(e!!.offerings.size).isEqualTo(1)
        val o = e.offerings["monthly_offering"]
        assertThat(o!!.skuDetails).isEqualTo(details[0])
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
        setup()
        val skus = ArrayList<String>()
        skus.add("monthly")
        mockProducts(skus)
        mockSkuDetails(skus, skus, BillingClient.SkuType.SUBS)

        purchases.getEntitlements(ReceiveEntitlementsListener { entitlementMap, _ ->
            receivedEntitlementMap = entitlementMap
        })

        assertThat(receivedEntitlementMap).isNotNull
        assertThat(purchases.cachedEntitlements).isEqualTo(receivedEntitlementMap)
    }

    @Test
    fun getEntitlementsErrorIsNotCalledIfSkuDetailsMissing() {
        setup()

        val skus = listOf("monthly")
        mockProducts(skus)
        mockSkuDetails(skus, ArrayList(), BillingClient.SkuType.SUBS)
        mockSkuDetails(skus, ArrayList(), BillingClient.SkuType.INAPP)

        val errorMessage = emptyArray<PurchasesError>()

        purchases.getEntitlements(ReceiveEntitlementsListener { entitlementMap, error ->
            if (error != null) {
                errorMessage[0] = error
            } else {
                receivedEntitlementMap = entitlementMap
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
                any(),
                captureLambda()
            )
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(PurchasesError(
                Purchases.ErrorDomains.REVENUECAT_BACKEND,
                0,
                "nope"
            ))
        }

        var purchasesError: PurchasesError? = null

        purchases.getEntitlements(ReceiveEntitlementsListener { _, error ->
            if (error == null) {
                fail("should be an error")
            } else {
                purchasesError = error
            }
        })

        assertThat(purchasesError).isNotNull
    }

    @Test
    fun addAttributionPassesDataToBackend() {
        setup()

        val jsonObject = mockk<JSONObject>()
        val network = Purchases.AttributionNetwork.APPSFLYER

        every {
            mockBackend.postAttributionData(appUserId, network, jsonObject)
        } just Runs

        purchases.addAttributionData(jsonObject, network)

        verify { mockBackend.postAttributionData(eq(appUserId), eq(network), eq(jsonObject)) }
    }

    @Test
    fun addAttributionConvertsStringStringMapToJsonObject() {
        setup()

        val network = Purchases.AttributionNetwork.APPSFLYER

        every {
            mockBackend.postAttributionData(appUserId, network, any())
        } just Runs

        purchases.addAttributionData(mapOf("key" to "value"), network)

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

        every {
            mockBackend.postReceiptData(
                eq(purchaseToken),
                eq(appUserId),
                eq(sku),
                eq(false),
                any(),
                captureLambda()
            )
        } answers {
            lambda<(PurchasesError)->Unit>().captured.invoke(PurchasesError(
                Purchases.ErrorDomains.REVENUECAT_BACKEND,
                402,
                "This is fake"
            ))
        }

        setup()

        purchases.onPurchasesUpdated(listOf(mockk<Purchase>().also {
            every {
                it.sku
            } returns sku
            every {
                it.purchaseToken
            } returns purchaseToken
        }))

        verify {
            mockBillingWrapper.consumePurchase(eq(purchaseToken))
        }
    }

    @Test
    fun triesToConsumeNonSubscriptionPurchasesOn50x() {
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        every {
            mockBackend.postReceiptData(
                eq(purchaseToken),
                eq(appUserId),
                eq(sku),
                eq(false),
                any(),
                captureLambda()
            )
        } answers {
            lambda<(PurchasesError)->Unit>().captured.invoke(PurchasesError(
                Purchases.ErrorDomains.REVENUECAT_BACKEND,
                502,
                "This is fake"
            ))
        }

        setup()

        purchases.onPurchasesUpdated(listOf(mockk<Purchase>().also {
            every {
                it.sku
            } returns sku
            every {
                it.purchaseToken
            } returns purchaseToken
        }))

        verify {
            mockBillingWrapper.consumePurchase(eq(purchaseToken))
        }
    }

    @Test
    fun closeCloses() {
        setup()
        mockCloseActions()

        purchases.close()

        verify { mockBackend.close() }
        verifyOrder {
            mockBillingWrapper.purchasesUpdatedListener = purchases
            mockBillingWrapper.purchasesUpdatedListener = null
        }
    }

    @Test
    fun whenNoTokensRestoringPurchasesStillCallListener() {
        setup()

        every {
            mockBillingWrapper.queryPurchaseHistoryAsync(
                any(),
                captureLambda(),
                any()
            )
        } answers {
            lambda<(List<Purchase>) -> Unit>().captured.invoke(emptyList())
        }

        val mockCompletion = mockk<ReceivePurchaserInfoListener>(relaxed = true)
        purchases.restorePurchases(mockCompletion)

        verify {
            mockCompletion.onReceived(any(), null)
        }
    }

    @Test
    fun `given a successful aliasing, success handler is called`() {
        setup()
        every {
            mockBackend.createAlias(
                eq(appUserId),
                eq("new_id"),
                captureLambda(),
                any()
            )
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }
        val mockInfo = mockk<PurchaserInfo>()
        every {
            mockBackend.getPurchaserInfo(any(), captureLambda(), any())
        } answers {
            lambda<(PurchaserInfo) -> Unit>().captured.invoke(mockInfo)
        }

        val mockCompletion = mockk<ReceivePurchaserInfoListener>(relaxed = true)
        purchases.createAlias(
            "new_id",
            mockCompletion
        )

        verify {
            mockCompletion.onReceived(mockInfo, null)
        }
    }

    @Test
    fun `given an unsuccessful aliasing, onError handler is called`() {
        setup()
        val purchasesError = PurchasesError(
            Purchases.ErrorDomains.REVENUECAT_BACKEND,
            0,
            "error"
        )
        every {
            mockBackend.createAlias(
                eq(appUserId),
                eq("new_id"),
                any(),
                captureLambda()
            )
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(purchasesError)
        }
        val mockReceivePurchaserInfoListener = mockk<ReceivePurchaserInfoListener>(relaxed = true)
        purchases.createAlias(
            "new_id",
            mockReceivePurchaserInfoListener
        )
        verify {
            mockReceivePurchaserInfoListener.onReceived(
                null,
                eq(purchasesError)
            )
        }
    }

    @Test
    fun `given a successful aliasing, appUserID is identified`() {
        setup()
        every {
            mockBackend.createAlias(
                eq(appUserId),
                eq("new_id"),
                captureLambda(),
                any()
            )
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        every {
            mockBackend.getPurchaserInfo(eq("new_id"), captureLambda(), any())
        } answers {
            lambda<(PurchaserInfo) -> Unit>().captured.invoke(mockk())
        }

        purchases.createAlias(
            "new_id",
            null
        )

        verify(exactly = 2) {
            mockCache.clearCachedAppUserID()
        }
        verify(exactly = 2) {
            mockCache.cachePurchaserInfo(any(), any())
        }
        assertThat(purchases.allowSharingPlayStoreAccount).isEqualTo(false)
        assertThat(purchases.appUserID).isEqualTo("new_id")
    }

    @Test
    fun `when identifying, appUserID is identified`() {
        setup()

        every {
            mockBackend.getPurchaserInfo("new_id", captureLambda(), any())
        } answers {
            lambda<(PurchaserInfo) -> Unit>().captured.invoke(mockk(relaxed = true))
        }

        purchases.identify("new_id")

        verify(exactly = 2) {
            mockCache.clearCachedAppUserID()
        }
        verify(exactly = 2) {
            mockCache.cachePurchaserInfo(any(), any())
        }
        assertThat(purchases.allowSharingPlayStoreAccount).isEqualTo(false)
        assertThat(purchases.appUserID).isEqualTo("new_id")
    }

    @Test
    fun `when resetting, random app user id is generated and saved`() {
        setup()
        purchases.reset()
        val randomID = slot<String>()
        verify {
            mockCache.cacheAppUserID(capture(randomID))
        }
        assertThat(purchases.appUserID).isEqualTo(randomID.captured)
        assertThat(randomID.captured).isNotNull()
    }

    @Test
    fun `when setting up, and passing a appUserID, user is identified`() {
        setup()
        verify(exactly = 1) {
            mockCache.clearCachedAppUserID()
        }
        verify(exactly = 1) {
            mockCache.cachePurchaserInfo(any(), any())
        }
        assertThat(purchases.allowSharingPlayStoreAccount).isEqualTo(false)
        assertThat(purchases.appUserID).isEqualTo(appUserId)
    }

    @Test
    fun `when setting listener, caches are retrieved`() {
        setup()

        purchases.updatedPurchaserInfoListener = listener

        verify {
            listener.onReceived(any())
        }
    }

    @Test
    fun `when setting listener, purchases are restored`() {
        setup()
        verify(exactly = 0) {
            mockBillingWrapper.queryPurchaseHistoryAsync(any(), any(), any())
        }
    }

    @Test
    fun `when setting shared instance and there's already an instance, instance is closed`() {
        setup()
        mockCloseActions()
        Purchases.sharedInstance = purchases
        Purchases.sharedInstance = purchases
        verify {
            purchases.close()
        }
    }

    private fun mockSkuDetailFetch(details: List<SkuDetails>, skus: List<String>, skuType: String) {
        every {
            mockBillingWrapper.querySkuDetailsAsync(
                eq(skuType),
                eq(skus),
                captureLambda()
            )
        } answers {
            lambda<(List<SkuDetails>) -> Unit>().captured.invoke(details)
        }
    }

    private fun mockBillingWrapper() {
        with(mockBillingWrapper) {
            every {
                querySkuDetailsAsync(any(), any(), captureLambda())
            } just Runs
            every {
                makePurchaseAsync(any(), any(), any(), any(), any())
            } just Runs
            every {
                purchasesUpdatedListener = capture(capturedPurchasesUpdatedListener)
            } just Runs
            every {
                consumePurchase(any())
            } just Runs
        }
    }

    private fun mockBackend(mockInfo: PurchaserInfo) {
        with(mockBackend) {
            every {
                getPurchaserInfo(any(), captureLambda(), any())
            } answers {
                lambda<(PurchaserInfo) -> Unit>().captured.invoke(mockInfo)
            }
            every {
                getEntitlements(any(), captureLambda(), any())
            } answers {
                lambda<(Map<String, Entitlement>) -> Unit>().captured.invoke(
                    mapOf(
                        "entitlement" to Entitlement(mapOf("sku" to Offering("sku")))
                    )
                )
            }
            every {
                postReceiptData(any(), any(), any(), any(), captureLambda(), any())
            } answers {
                lambda<(PurchaserInfo) -> Unit>().captured.invoke(mockInfo)
            }
        }
    }

    private fun mockCache(mockInfo: PurchaserInfo) {
        with(mockCache) {
            every {
                getCachedAppUserID()
            } returns null
            every {
                getCachedPurchaserInfo(any())
            } returns mockInfo
            every {
                clearCachedAppUserID()
            } just Runs
            every {
                cachePurchaserInfo(any(), any())
            } just Runs
            every {
                cacheAppUserID(any())
            } just Runs
            every {
                clearCachedPurchaserInfo(any())
            } just Runs
        }
    }

    private fun mockProducts(skus: List<String>) {
        every {
            mockBackend.getEntitlements(any(), captureLambda(), any())
        } answers {
            val offeringMap = skus.map { sku -> sku + "_offering" to Offering(sku) }.toMap()
            lambda<(Map<String, Entitlement>) -> Unit>().captured.invoke(mapOf("pro" to Entitlement(offeringMap)))
        }
    }

    private fun mockSkuDetails(
        skus: List<String>,
        returnSkus: List<String>,
        type: String
    ): List<SkuDetails> {
        val skuDetailsList = returnSkus.map { sku ->
            mockk<SkuDetails>().also {
                every { it.sku } returns sku
            }
        }

        mockSkuDetailFetch(skuDetailsList, skus, type)
        return skuDetailsList
    }

    private fun mockCloseActions() {
        every {
            mockBackend.close()
        } just Runs
        every {
            mockBillingWrapper.purchasesUpdatedListener = null
        } just Runs
    }
}
