//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.SkuType.INAPP
import com.android.billingclient.api.BillingClient.SkuType.SUBS
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.interfaces.Callback
import com.revenuecat.purchases.interfaces.GetSkusResponseListener
import com.revenuecat.purchases.interfaces.ReceivePurchaserInfoListener
import com.revenuecat.purchases.interfaces.UpdatedPurchaserInfoListener
import com.revenuecat.purchases.util.AdvertisingIdClient
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.AssertionsForClassTypes
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.ArrayList
import java.util.Collections.emptyList
import java.util.ConcurrentModificationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PurchasesTest {

    private val mockBillingWrapper: BillingWrapper = mockk()
    private val mockBackend: Backend = mockk()
    private val mockCache: DeviceCache = mockk()
    private val listener: UpdatedPurchaserInfoListener = mockk()
    private val capturedActivityLifecycleListener = slot<Application.ActivityLifecycleCallbacks>()
    private val mockApplication = mockk<Application>().apply {
        every {
            registerActivityLifecycleCallbacks(capture(capturedActivityLifecycleListener))
        } just Runs
        every {
            unregisterActivityLifecycleCallbacks(any())
        } just Runs
    }
    private val mockContext = mockk<Context>(relaxed = true).apply {
        every {
            applicationContext
        } returns mockApplication
    }

    private var capturedPurchasesUpdatedListener = slot<BillingWrapper.PurchasesUpdatedListener>()
    private var capturedBillingWrapperStateListener = slot<BillingWrapper.StateListener>()
    private var capturedConsumeResponseListener = slot<(Int, String) -> Unit>()

    private val appUserId = "fakeUserID"
    private val adID = "123"
    private lateinit var purchases: Purchases
    private var receivedSkus: List<SkuDetails>? = null
    private var receivedEntitlementMap: Map<String, Entitlement>? = null
    private var mockExecutorService = mockk<ExecutorService>().apply {
        val capturedRunnable = slot<Runnable>()
        every { execute(capture(capturedRunnable)) } answers { capturedRunnable.captured.run() }
    }

    @After
    fun tearDown() {
        Purchases.backingFieldSharedInstance = null
        Purchases.postponedAttributionData = mutableListOf()
    }

    private fun setup(): PurchaserInfo {
        val mockInfo = mockk<PurchaserInfo>()

        mockCache(mockInfo)
        mockBackend(mockInfo)
        mockBillingWrapper()
        every {
            listener.onReceived(any())
        } just Runs

        purchases = Purchases(
            mockContext,
            appUserId,
            mockBackend,
            mockBillingWrapper,
            mockCache,
            executorService = mockExecutorService
        )
        Purchases.sharedInstance = purchases
        return mockInfo
    }

    private fun setupAnonymous() {
        val mockInfo = mockk<PurchaserInfo>()

        mockCache(mockInfo)
        mockBackend(mockInfo)
        mockBillingWrapper()
        every {
            listener.onReceived(any())
        } just Runs

        purchases = Purchases(
            mockContext,
            null,
            mockBackend,
            mockBillingWrapper,
            mockCache,
            executorService = mockExecutorService
        )
        Purchases.sharedInstance = purchases
    }

    @Test
    fun canBeCreated() {
        setup()
        assertThat(purchases).isNotNull
    }

    @Test
    fun getsSubscriptionSkus() {
        setup()

        val skus = ArrayList<String>()
        skus.add("onemonth_freetrial")

        val skuDetails = ArrayList<SkuDetails>()

        mockSkuDetailFetch(skuDetails, skus, SUBS)

        purchases.getSubscriptionSkus(skus,
            object : GetSkusResponseListener {
                override fun onReceived(skus: MutableList<SkuDetails>) {
                    receivedSkus = skus
                }

                override fun onError(error: PurchasesError) {
                    fail("shouldn't be error")
                }
            })

        assertThat(receivedSkus).isEqualTo(skuDetails)
    }

    @Test
    fun getsNonSubscriptionSkus() {
        setup()

        val skus = ArrayList<String>()
        skus.add("normal_purchase")

        val skuDetails = ArrayList<SkuDetails>()

        mockSkuDetailFetch(skuDetails, skus, INAPP)

        purchases.getNonSubscriptionSkus(skus,
            object : GetSkusResponseListener {
                override fun onReceived(skus: MutableList<SkuDetails>) {
                    receivedSkus = skus
                }

                override fun onError(error: PurchasesError) {
                    fail("shouldn't be error")
                }
            })

        assertThat(receivedSkus).isEqualTo(skuDetails)
    }

    @Suppress("DEPRECATION")
    @Test
    fun canMakePurchaseDeprecated() {
        setup()

        val activity: Activity = mockk()
        val sku = "onemonth_freetrial"
        val oldSkus = ArrayList<String>()

        every {
            mockBillingWrapper.querySkuDetailsAsync(any(), any(), captureLambda(), any())
        } answers {
            lambda<(List<Purchase>) -> Unit>().captured.invoke(listOf(mockk(relaxed = true)))
        }

        purchases.makePurchaseWith(
            activity,
            sku,
            SUBS
        ) { _, _ -> }

        verify (exactly = 1) {
            mockBillingWrapper.makePurchaseAsync(
                eq(activity),
                eq(appUserId),
                eq(sku),
                eq(oldSkus),
                eq(SUBS)
            )
        }
    }

    @Test
    fun canMakePurchase() {
        setup()

        val activity: Activity = mockk()
        val sku = "onemonth_freetrial"
        val skuDetails = mockk<SkuDetails>().also {
            every { it.sku } returns sku
        }

        purchases.makePurchaseWith(
            activity,
            skuDetails
        ) { _, _ -> }

        verify {
            mockBillingWrapper.makePurchaseAsync(
                eq(activity),
                eq(appUserId),
                skuDetails,
                null
            )
        }
    }

    @Test
    fun postsSuccessfulPurchasesToBackend() {
        setup()
        val sku = "inapp"
        val purchaseToken = "token_inapp"
        val skuSub = "sub"
        val purchaseTokenSub = "token_sub"

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, INAPP) +
                getMockedPurchaseList(skuSub, purchaseTokenSub, SUBS)
        )

        verify (exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken,
                appUserId,
                sku,
                false,
                any(),
                any()
            )
        }

        verify (exactly = 1) {
            mockBackend.postReceiptData(
                purchaseTokenSub,
                appUserId,
                skuSub,
                false,
                any(),
                any()
            )
        }

        verify (exactly = 1){
            mockBillingWrapper.consumePurchase(purchaseToken, any())
        }

        verify (exactly = 0){
            mockBillingWrapper.consumePurchase(purchaseTokenSub, any())
        }
    }

    @Test
    fun callsPostForEachUpdatedPurchase() {
        setup()

        val purchasesList = ArrayList<PurchaseWrapper>()
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        for (i in 0..1) {
            val p: Purchase = mockk()
            every {
                p.sku
            } returns sku
            every {
                p.purchaseToken
            } returns purchaseToken + i.toString()
            every {
                p.purchaseTime
            } returns System.currentTimeMillis()
            val wrapper = PurchaseWrapper(p, SUBS)
            purchasesList.add(wrapper)
        }

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(purchasesList)

        verify(exactly = 2) {
            mockBackend.postReceiptData(
                any(),
                appUserId,
                sku,
                false,
                any(),
                any()
            )
        }
    }

    @Test
    fun doesntPostIfNotOK() {
        setup()

        capturedPurchasesUpdatedListener.captured.onPurchasesFailedToUpdate(emptyList(), 0, "fail")

        verify(exactly = 0) {
            mockBackend.postReceiptData(
                any(),
                any(),
                any(),
                false,
                any(),
                any()
            )
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun passesUpErrorsDeprecated() {
        setup()
        var errorCalled = false
        purchases.makePurchaseWith(
            mockk(),
            "sku",
            "SKUS",
            onError = { error, _ ->
                errorCalled = true
                assertThat(error.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
            }) { _, _ -> }

        val purchase = mockk<Purchase>(relaxed = true)
        every { purchase.sku } returns "sku"
        capturedPurchasesUpdatedListener.captured.onPurchasesFailedToUpdate(listOf(purchase), 2, "")
        assertThat(errorCalled).isTrue()
    }

    @Test
    fun passesUpErrors() {
        setup()
        var errorCalled = false
        purchases.makePurchaseWith(
            mockk(),
            mockk<SkuDetails>().also {
                every { it.sku } returns "sku"
            },
            onError = { error, _ ->
                errorCalled = true
                assertThat(error.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
            }, onSuccess = { _, _ -> })

        val purchase = mockk<Purchase>(relaxed = true)
        every { purchase.sku } returns "sku"
        capturedPurchasesUpdatedListener.captured.onPurchasesFailedToUpdate(listOf(purchase), 2, "")
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
            mockContext,
            null,
            mockBackend,
            mockBillingWrapper,
            mockCache,
            executorService = mockExecutorService
        )

        assertThat(purchases).isNotNull

        val appUserID = purchases.appUserID
        assertThat(appUserID).isNotNull()
        assertThat(appUserID.length).isEqualTo(36)
    }

    @Test
    fun storesGeneratedAppUserID() {
        val mockInfo = mockk<PurchaserInfo>()

        mockCache(mockInfo)
        mockBackend(mockInfo)
        mockBillingWrapper()

        Purchases(
            mockContext,
            null,
            mockBackend,
            mockBillingWrapper,
            mockCache,
            executorService = mockExecutorService
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
            mockContext,
            null,
            mockBackend,
            mockBillingWrapper,
            mockCache,
            executorService = mockExecutorService
        )

        assertThat(appUserID).isEqualTo(p.appUserID)
    }

    @Test
    fun isRestoreWhenUsingNullAppUserID() {
        setup()

        val purchases = Purchases(
            mockContext,
            null,
            mockBackend,
            mockBillingWrapper,
            mockCache,
            executorService = mockExecutorService
        )

        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, SUBS)
        )

        verify {
            mockBackend.postReceiptData(purchaseToken, purchases.appUserID, sku, true, any(), any())
        }
    }

    @Test
    fun doesntRestoreNormally() {
        setup()

        val purchases = Purchases(
            mockContext,
            "a_fixed_id",
            mockBackend,
            mockBillingWrapper,
            mockCache,
            executorService = mockExecutorService
        )

        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, SUBS)
        )

        verify {
            mockBackend.postReceiptData(
                purchaseToken,
                purchases.appUserID,
                sku,
                false,
                any(),
                any()
            )
        }
    }

    @Test
    fun canOverrideAnonMode() {
        setup()

        val purchases = Purchases(
            mockContext,
            "a_fixed_id",
            mockBackend,
            mockBillingWrapper,
            mockCache,
            executorService = mockExecutorService
        )
        purchases.allowSharingPlayStoreAccount = true

        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, SUBS)
        )

        verify {
            mockBackend.postReceiptData(purchaseToken, purchases.appUserID, sku, true, any(), any())
        }
    }

    @Test
    fun restoringPurchasesGetsHistory() {
        setup()
        var capturedLambda: ((List<PurchaseWrapper>) -> Unit)? = null
        every {
            mockBillingWrapper.queryAllPurchases(
                captureLambda(),
                any()
            )
        } answers {
            capturedLambda = lambda<(List<PurchaseWrapper>) -> Unit>().captured.also {
                it.invoke(listOf(mockk(relaxed = true)))
            }
        }

        purchases.restorePurchasesWith { }

        assertThat(capturedLambda).isNotNull
        verify {
            mockBillingWrapper.queryAllPurchases(
                any(),
                any()
            )
        }
    }

    @Test
    fun historicalPurchasesPassedToBackend() {
        setup()
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "sub"
        val purchaseTokenSub = "token_sub"

        var capturedLambda: ((List<PurchaseWrapper>) -> Unit)? = null
        every {
            mockBillingWrapper.queryAllPurchases(
                captureLambda(),
                any()
            )
        } answers {
            capturedLambda = lambda<(List<PurchaseWrapper>) -> Unit>().captured
            capturedLambda?.invoke(
                getMockedPurchaseList(sku, purchaseToken, INAPP) +
                    getMockedPurchaseList(skuSub, purchaseTokenSub, SUBS)
            )
        }

        var restoreCalled = false
        purchases.restorePurchasesWith(onSuccess = {
            restoreCalled = true
        }, onError = {
            fail("Should not be an error")
        })
        assertThat(capturedLambda).isNotNull
        assertThat(restoreCalled).isTrue()

        verify (exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken,
                purchases.appUserID,
                sku,
                true,
                any(),
                any()
            )
        }

        verify (exactly = 1) {
            mockBackend.postReceiptData(
                purchaseTokenSub,
                purchases.appUserID,
                skuSub,
                true,
                any(),
                any()
            )
        }
    }

    @Test
    fun failedToRestorePurchases() {
        setup()
        val purchasesError = PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken")
        every {
            mockBillingWrapper.queryAllPurchases(any(), captureLambda())
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(purchasesError)
        }

        var onErrorCalled = false
        purchases.restorePurchasesWith(onSuccess = {
            fail("should be an error")
        }, onError = { error ->
            onErrorCalled = true
            assertThat(error).isEqualTo(purchasesError)
        })

        assertThat(onErrorCalled).isTrue()
    }

    @Test
    fun restoringCallsRestoreCallback() {
        setup()

        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "onemonth_freetrial_sub"
        val purchaseTokenSub = "crazy_purchase_token_sub"

        var capturedLambda: ((List<PurchaseWrapper>) -> Unit)? = null
        every {
            mockBillingWrapper.queryAllPurchases(
                captureLambda(),
                any()
            )
        } answers {
            capturedLambda = lambda<(List<PurchaseWrapper>) -> Unit>().captured.also {
                it.invoke(
                    getMockedPurchaseList(sku, purchaseToken, INAPP) +
                        getMockedPurchaseList(skuSub, purchaseTokenSub, SUBS)
                )
            }
        }

        val mockInfo = mockk<PurchaserInfo>()
        every {
            mockBackend.postReceiptData(
                any(),
                any(),
                any(),
                true,
                captureLambda(),
                any()
            )
        } answers {
            lambda<(PurchaserInfo) -> Unit>().captured.invoke(mockInfo)
        }

        var callbackCalled = false
        purchases.restorePurchasesWith(onSuccess = { info ->
            assertThat(mockInfo).isEqualTo(info)
            callbackCalled = true
        }, onError = {
            fail("should be success")
        })

        assertThat(capturedLambda).isNotNull
        verify(exactly = 1) {
            mockBillingWrapper.queryAllPurchases(any(), any())
        }

        assertThat(callbackCalled).isTrue()
    }

    @Test
    fun receivedPurchaserInfoShouldBeCached() {
        setup()

        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, SUBS)
        )

        verify {
            mockBackend.postReceiptData(purchaseToken, appUserId, sku, false, any(), any())
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
        val details = mockSkuDetails(skus, skus, SUBS)

        purchases.getEntitlementsWith(onSuccess = { entitlementMap ->
            this@PurchasesTest.receivedEntitlementMap = entitlementMap
        }, onError = {
            fail("should be a success")
        })

        assertThat(receivedEntitlementMap).isNotNull

        verify {
            mockBillingWrapper.querySkuDetailsAsync(
                eq(SUBS),
                eq(skus),
                any(),
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
        mockSkuDetails(skus, subsSkus, SUBS)
        mockSkuDetails(inappSkus, inappSkus, INAPP)

        setup()

        every {
            mockBillingWrapper.querySkuDetailsAsync(
                eq(SUBS),
                eq<List<String>>(skus),
                any(),
                any()
            )
        }
        every {
            mockBillingWrapper.querySkuDetailsAsync(
                eq(INAPP),
                eq<List<String>>(inappSkus),
                any(),
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
        mockSkuDetails(skus, skus, SUBS)

        purchases.getEntitlementsWith(onSuccess = { entitlementMap ->
            receivedEntitlementMap = entitlementMap
        }, onError = {
            fail("should be a success")
        })

        assertThat(receivedEntitlementMap).isNotNull
        assertThat(purchases.state.cachedEntitlements).isEqualTo(receivedEntitlementMap)
    }

    @Test
    fun getEntitlementsErrorIsNotCalledIfSkuDetailsMissing() {
        setup()

        val skus = listOf("monthly")
        mockProducts(skus)
        mockSkuDetails(skus, ArrayList(), SUBS)
        mockSkuDetails(skus, ArrayList(), INAPP)

        val errorMessage = emptyArray<PurchasesError>()

        purchases.getEntitlementsWith(onSuccess = { entitlementMap ->
            receivedEntitlementMap = entitlementMap
        }, onError = { error ->
            errorMessage[0] = error
        })

        assertThat(errorMessage.size).isZero()
        assertThat(this.receivedEntitlementMap).isNotNull
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
            lambda<(PurchasesError) -> Unit>().captured.invoke(
                PurchasesError(PurchasesErrorCode.StoreProblemError)
            )
        }

        var purchasesError: PurchasesError? = null

        purchases.getEntitlementsWith(onSuccess = {
            fail("should be an error")
        }, onError = { error ->
            purchasesError = error
        })

        assertThat(purchasesError).isNotNull
    }

    @Test
    fun addAttributionPassesDataToBackend() {
        setup()

        val jsonObject = JSONObject()
        jsonObject.put("key", "value")
        val network = Purchases.AttributionNetwork.APPSFLYER

        val jsonSlot = slot<JSONObject>()
        every {
            mockBackend.postAttributionData(appUserId, network, capture(jsonSlot), captureLambda())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        val networkUserID = "networkid"
        mockAdInfo(false, networkUserID)

        Purchases.addAttributionData(jsonObject, network, networkUserID)

        verify { mockBackend.postAttributionData(appUserId, network, any(), any()) }
        assertThat(jsonSlot.captured["key"]).isEqualTo("value")
    }

    @Test
    fun addAttributionConvertsStringStringMapToJsonObject() {
        setup()

        val network = Purchases.AttributionNetwork.APPSFLYER

        every {
            mockBackend.postAttributionData(appUserId, network, any(), captureLambda())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        val networkUserID = "networkUserID"
        mockAdInfo(false, networkUserID)

        Purchases.addAttributionData(mapOf("key" to "value"), network, networkUserID)

        verify {
            mockBackend.postAttributionData(
                eq(appUserId),
                eq(network),
                any(),
                any()
            )
        }
    }

    @Test
    fun consumesNonSubscriptionPurchasesOn40x() {
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "sub"
        val purchaseTokenSub = "token_sub"

        setup()

        var capturedLambda: ((PurchasesError, Boolean) -> Unit)? = null
        every {
            mockBackend.postReceiptData(
                purchaseToken,
                appUserId,
                sku,
                false,
                any(),
                captureLambda()
            )
        } answers {
            capturedLambda = lambda<(PurchasesError, Boolean) -> Unit>().captured
            capturedLambda?.invoke(
                PurchasesError(PurchasesErrorCode.InvalidCredentialsError),
                true
            )
        }

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, INAPP) +
                getMockedPurchaseList(skuSub, purchaseTokenSub, SUBS)
        )

        verify (exactly = 1) {
            mockBillingWrapper.consumePurchase(purchaseToken,  any())
        }

        verify (exactly = 0) {
            mockBillingWrapper.consumePurchase(purchaseTokenSub,  any())
        }
        assertThat(capturedLambda).isNotNull
    }

    @Test
    fun triesToConsumeNonSubscriptionPurchasesOn50x() {
        setup()

        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "sub"
        val purchaseTokenSub = "token_sub"

        var capturedLambda: ((PurchasesError, Boolean) -> Unit)? = null
        var capturedLambda1: ((PurchasesError, Boolean) -> Unit)? = null
        every {
            mockBackend.postReceiptData(
                purchaseToken,
                appUserId,
                sku,
                false,
                any(),
                captureLambda()
            )
        } answers {
            capturedLambda = lambda<(PurchasesError, Boolean) -> Unit>().captured.also {
                it.invoke(
                    PurchasesError(PurchasesErrorCode.InvalidCredentialsError),
                    true
                )
            }
        }
        every {
            mockBackend.postReceiptData(
                purchaseTokenSub,
                appUserId,
                skuSub,
                false,
                any(),
                captureLambda()
            )
        } answers {
            capturedLambda1 = lambda<(PurchasesError, Boolean) -> Unit>().captured.also {
                it.invoke(
                    PurchasesError(PurchasesErrorCode.InvalidCredentialsError),
                    true
                )
            }
        }

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, INAPP) +
                getMockedPurchaseList(skuSub, purchaseTokenSub, SUBS)
        )

        assertThat(capturedLambda).isNotNull
        assertThat(capturedLambda1).isNotNull
        verify (exactly = 1) {
            mockBillingWrapper.consumePurchase(purchaseToken,  any())
        }

        verify (exactly = 0) {
            mockBillingWrapper.consumePurchase(purchaseTokenSub,  any())
        }
    }

    @Test
    fun closeCloses() {
        setup()
        mockCloseActions()

        purchases.close()
        verifyClose()
    }

    @Test
    fun whenNoTokensRestoringPurchasesStillCallListener() {
        setup()

        every {
            mockBillingWrapper.queryAllPurchases(
                captureLambda(),
                any()
            )
        } answers {
            lambda<(List<Purchase>) -> Unit>().captured.invoke(emptyList())
        }

        val mockCompletion = mockk<ReceivePurchaserInfoListener>(relaxed = true)
        purchases.restorePurchases(mockCompletion)

        verify {
            mockCompletion.onReceived(any())
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
            mockCompletion.onReceived(mockInfo)
        }
    }

    @Test
    fun `given an unsuccessful aliasing, onError handler is called`() {
        setup()
        val purchasesError = PurchasesError(PurchasesErrorCode.InvalidCredentialsError)
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
            mockReceivePurchaserInfoListener.onError(eq(purchasesError))
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

        purchases.createAlias("new_id")

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
            mockCache.cachePurchaserInfo(any(), any())
        }
        assertThat(purchases.allowSharingPlayStoreAccount).isEqualTo(false)
        assertThat(purchases.appUserID).isEqualTo("new_id")
    }

    @Test
    fun `when resetting, random app user id is generated and saved`() {
        setup()
        every {
            mockCache.clearLatestAttributionData(appUserId)
        } just Runs
        purchases.reset()
        val randomID = slot<String>()
        verify {
            mockCache.cacheAppUserID(capture(randomID))
        }
        verify {
            mockCache.clearLatestAttributionData(appUserId)
        }
        assertThat(purchases.appUserID).isEqualTo(randomID.captured)
        assertThat(randomID.captured).isNotNull()
    }

    @Test
    fun `when setting up, and passing a appUserID, user is identified`() {
        setup()
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
        verifyClose()
    }

    @Test
    fun `when setting listener, listener is called`() {
        setup()
        purchases.updatedPurchaserInfoListener = listener

        verify(exactly = 1) {
            listener.onReceived(any())
        }
    }

    @Test
    fun `when setting listener for anonymous user, listener is called`() {
        setupAnonymous()
        purchases.updatedPurchaserInfoListener = listener

        verify(exactly = 1) {
            listener.onReceived(any())
        }
    }

    @Test
    fun `given a random purchase update, listener is called if purchaser info has changed`() {
        setup()
        val info = mockk<PurchaserInfo>()
        every {
            mockBackend.postReceiptData(any(), any(), any(), any(), captureLambda(), any())
        } answers {
            lambda<(PurchaserInfo) -> Unit>().captured.invoke(info)
        }
        purchases.updatedPurchaserInfoListener = listener
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, SUBS)
        )

        verify(exactly = 2) {
            listener.onReceived(any())
        }
    }

    @Test
    fun `given a random purchase update, listener is not called if purchaser info has not changed`() {
        setup()
        purchases.updatedPurchaserInfoListener = listener
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, SUBS)
        )

        verify(exactly = 1) {
            listener.onReceived(any())
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `DEPRECATED when making another purchase for a product for a pending product, error is issued`() {
        setup()
        purchases.updatedPurchaserInfoListener = listener
        val sku = "onemonth_freetrial"

        purchases.makePurchaseWith(
            mockk(),
            sku,
            SUBS,
            onError = { _, _ -> fail("Should be success") }) { _, _ ->
                // First one works
            }

        var errorCalled: PurchasesError? = null
        purchases.makePurchaseWith(
            mockk(),
            sku,
            SUBS,
            onError = { error, _  ->
                errorCalled = error
            }) { _, _ ->
                fail("Should be error")
            }

        assertThat(errorCalled!!.code).isEqualTo(PurchasesErrorCode.OperationAlreadyInProgressError)
    }

    @Test
    fun `when making another purchase for a product for a pending product, error is issued`() {
        setup()
        purchases.updatedPurchaserInfoListener = listener

        val skuDetails = mockk<SkuDetails>().also {
            every { it.sku } returns "sku"
        }
        purchases.makePurchaseWith(
            mockk(),
            skuDetails,
            onError = { _, _ -> fail("Should be success") }) { _, _ ->
            // First one works
        }

        var errorCalled: PurchasesError? = null
        purchases.makePurchaseWith(
            mockk(),
            skuDetails,
            onError = { error, _  ->
                errorCalled = error
            }) { _, _ ->
            fail("Should be error")
        }

        assertThat(errorCalled!!.code).isEqualTo(PurchasesErrorCode.OperationAlreadyInProgressError)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `DEPRECATED when making purchase, completion block is called once`() {
        setup()

        val activity: Activity = mockk()
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        var callCount = 0
        purchases.makePurchaseWith(
            activity,
            sku,
            SUBS,
            onSuccess = { _, _ ->
                callCount++
            }, onError = { _, _ -> fail("should be successful") })

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, SUBS)
        )
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, SUBS)
        )

        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun `when making purchase, completion block is called once`() {
        setup()

        val activity: Activity = mockk()
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"


        val skuDetails = mockk<SkuDetails>().also {
            every { it.sku } returns sku
        }

        var callCount = 0
        purchases.makePurchaseWith(
            activity,
            skuDetails,
            onSuccess = { _, _ ->
                callCount++
            }, onError = { _, _ -> fail("should be successful") })

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, SUBS)
        )
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, SUBS)
        )

        assertThat(callCount).isEqualTo(1)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `DEPRECATED when making purchase, completion block not called for different products`() {
        setup()

        val activity: Activity = mockk()
        val sku = "onemonth_freetrial"
        val sku1 = "onemonth_freetrial_1"
        val purchaseToken1 = "crazy_purchase_token_1"
        var callCount = 0
        purchases.makePurchaseWith(
            activity,
            sku,
            SUBS,
            onSuccess = { _, _ ->
                callCount++
            }, onError = { _, _ -> fail("should be successful") })

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku1, purchaseToken1, SUBS)
        )

        assertThat(callCount).isEqualTo(0)
    }

    @Test
    fun `when making purchase, completion block not called for different products`() {
        setup()

        val activity: Activity = mockk()
        val sku = "onemonth_freetrial"
        val sku1 = "onemonth_freetrial_1"
        val purchaseToken1 = "crazy_purchase_token_1"
        var callCount = 0
        purchases.makePurchaseWith(
            activity,
            mockk<SkuDetails>().also {
                every { it.sku } returns sku
            },
            onSuccess = { _, _ ->
                callCount++
            }, onError = { _, _ -> fail("should be successful") })

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku1, purchaseToken1, SUBS)
        )

        assertThat(callCount).isEqualTo(0)
    }

    @Test
    fun `sends cached purchaser info to getter`() {
        val info = setup()

        every {
            mockBackend.getPurchaserInfo(any(), captureLambda(), any())
        } answers {
            // Timeout
        }

        var receivedInfo: PurchaserInfo? = null
        purchases.getPurchaserInfoWith(onSuccess = {
            receivedInfo = it
        }, onError = {
            fail("supposed to be successful")
        })

        assertThat(receivedInfo).isEqualTo(info)
    }

    @Test
    fun `given no cached purchaser info, backend is called again`() {
        setup()
        every {
            mockCache.getCachedPurchaserInfo(any())
        } returns null
        every {
            mockBackend.getPurchaserInfo(any(), captureLambda(), any())
        } answers {
            // Timeout
        }

        var receivedInfo: PurchaserInfo? = null
        purchases.getPurchaserInfoWith(onSuccess = {
            receivedInfo = it
        }, onError = {
            fail("supposed to be successful")
        })

        assertThat(receivedInfo).isEqualTo(null)
        verify(exactly = 2) { mockBackend.getPurchaserInfo(any(), any(), any()) }
    }


    @Test
    fun `don't create an alias if the new app user id is the same`() {
        setup()

        val lock = CountDownLatch(1)
        purchases.createAliasWith(appUserId) {
            lock.countDown()
        }

        lock.await(200, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isZero()
        verify (exactly = 0) {
            mockBackend.createAlias(appUserId, appUserId, any(), any())
        }
    }

    @Test
    fun `don't identify if the new app user id is the same`() {
        val info = setup()

        var receivedInfo: PurchaserInfo? = null
        purchases.identifyWith(appUserId) {
           receivedInfo = it
        }

        verify (exactly = 1) {
            mockCache.getCachedPurchaserInfo(appUserId)
        }
        assertThat(receivedInfo).isEqualTo(info)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `DEPRECATED when multiple make purchase callbacks, a failure doesn't throw ConcurrentModificationException`() {
        setup()

        val activity: Activity = mockk()

        purchases.makePurchaseWith(
            activity,
            "onemonth_freetrial",
            SUBS
        ) { _, _ -> }

        purchases.makePurchaseWith(
            activity,
            "annual_freetrial",
            SUBS
        ) { _, _ -> }

        try {
            capturedPurchasesUpdatedListener.captured.onPurchasesFailedToUpdate(emptyList(), 0, "fail")
        } catch (e: ConcurrentModificationException) {
            fail("Test throws ConcurrentModificationException")
        }
    }

    @Test
    fun `when multiple make purchase callbacks, a failure doesn't throw ConcurrentModificationException`() {
        setup()

        val activity: Activity = mockk()

        purchases.makePurchaseWith(
            activity,
            mockk<SkuDetails>().also {
                every { it.sku } returns "sku"
            }
        ) { _, _ -> }

        purchases.makePurchaseWith(
            activity,
            mockk<SkuDetails>().also {
                every { it.sku } returns "sku"
            }
        ) { _, _ -> }

        try {
            capturedPurchasesUpdatedListener.captured.onPurchasesFailedToUpdate(emptyList(), 0, "fail")
        } catch (e: ConcurrentModificationException) {
            fail("Test throws ConcurrentModificationException")
        }
    }

    @Test
    fun `when checking if Billing is supported, an OK response when starting connection means it's supported`() {
        setup()
        var receivedIsBillingSupported = false
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        mockkStatic(BillingClient::class)
        val mockBuilder = mockk<BillingClient.Builder>(relaxed = true)
        every { BillingClient.newBuilder(any()) } returns mockBuilder
        every { mockBuilder.setListener(any()) } returns mockBuilder
        every { mockBuilder.build() } returns mockLocalBillingClient
        val listener = slot<BillingClientStateListener>()
        every { mockLocalBillingClient.startConnection(capture(listener)) } just Runs
        Purchases.isBillingSupported(mockContext, Callback {
            receivedIsBillingSupported = it
        })
        listener.captured.onBillingSetupFinished(BillingClient.BillingResponse.OK)
        AssertionsForClassTypes.assertThat(receivedIsBillingSupported).isTrue()
        verify (exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when checking if Billing is supported, disconnections mean billing is not supported`() {
        setup()
        var receivedIsBillingSupported = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        mockkStatic(BillingClient::class)
        val mockBuilder = mockk<BillingClient.Builder>(relaxed = true)
        every { BillingClient.newBuilder(any()) } returns mockBuilder
        every { mockBuilder.setListener(any()) } returns mockBuilder
        every { mockBuilder.build() } returns mockLocalBillingClient
        val listener = slot<BillingClientStateListener>()
        every { mockLocalBillingClient.startConnection(capture(listener)) } just Runs
        Purchases.isBillingSupported(mockContext, Callback {
            receivedIsBillingSupported = it
        })
        listener.captured.onBillingServiceDisconnected()
        AssertionsForClassTypes.assertThat(receivedIsBillingSupported).isFalse()
        verify (exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when checking if Billing is supported, a non OK response when starting connection means it's not supported`() {
        setup()
        var receivedIsBillingSupported = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        mockkStatic(BillingClient::class)
        val mockBuilder = mockk<BillingClient.Builder>(relaxed = true)
        every { BillingClient.newBuilder(any()) } returns mockBuilder
        every { mockBuilder.setListener(any()) } returns mockBuilder
        every { mockBuilder.build() } returns mockLocalBillingClient
        val listener = slot<BillingClientStateListener>()
        every { mockLocalBillingClient.startConnection(capture(listener)) } just Runs
        Purchases.isBillingSupported(mockContext, Callback {
            receivedIsBillingSupported = it
        })
        listener.captured.onBillingSetupFinished(BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED)
        AssertionsForClassTypes.assertThat(receivedIsBillingSupported).isFalse()
        verify (exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when checking if feature is supported, an OK response when starting connection means it's supported`() {
        setup()
        var featureSupported = false
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        every { mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS) } returns BillingClient.BillingResponse.OK
        mockkStatic(BillingClient::class)
        val mockBuilder = mockk<BillingClient.Builder>(relaxed = true)
        every { BillingClient.newBuilder(any()) } returns mockBuilder
        every { mockBuilder.setListener(any()) } returns mockBuilder
        every { mockBuilder.build() } returns mockLocalBillingClient
        val listener = slot<BillingClientStateListener>()
        every { mockLocalBillingClient.startConnection(capture(listener)) } just Runs
        Purchases.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS, mockContext, Callback {
            featureSupported = it
        })
        listener.captured.onBillingSetupFinished(BillingClient.BillingResponse.OK)
        AssertionsForClassTypes.assertThat(featureSupported).isTrue()
        verify (exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when checking if feature is supported, disconnections mean billing is not supported`() {
        setup()
        var featureSupported = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        mockkStatic(BillingClient::class)
        val mockBuilder = mockk<BillingClient.Builder>(relaxed = true)
        every { BillingClient.newBuilder(any()) } returns mockBuilder
        every { mockBuilder.setListener(any()) } returns mockBuilder
        every { mockBuilder.build() } returns mockLocalBillingClient
        val listener = slot<BillingClientStateListener>()
        every { mockLocalBillingClient.startConnection(capture(listener)) } just Runs
        every { mockLocalBillingClient.startConnection(capture(listener)) } just Runs
        Purchases.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS, mockContext, Callback {
            featureSupported = it
        })
        listener.captured.onBillingServiceDisconnected()
        AssertionsForClassTypes.assertThat(featureSupported).isFalse()
        verify (exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when checking if feature is supported, a non OK response when starting connection means it's not supported`() {
        setup()
        var featureSupported = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        every {
            mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        } returns BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED
        mockkStatic(BillingClient::class)
        val mockBuilder = mockk<BillingClient.Builder>(relaxed = true)
        every { BillingClient.newBuilder(any()) } returns mockBuilder
        every { mockBuilder.setListener(any()) } returns mockBuilder
        every { mockBuilder.build() } returns mockLocalBillingClient
        val listener = slot<BillingClientStateListener>()
        every { mockLocalBillingClient.startConnection(capture(listener)) } just Runs
        Purchases.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS, mockContext, Callback {
            featureSupported = it
        })
        listener.captured.onBillingSetupFinished(BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED)
        AssertionsForClassTypes.assertThat(featureSupported).isFalse()
        verify (exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when no play services, feature is not supported`() {
        setup()
        var featureSupported = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        every {
            mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        } returns BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED
        mockkStatic(BillingClient::class)
        val mockBuilder = mockk<BillingClient.Builder>(relaxed = true)
        every { BillingClient.newBuilder(any()) } returns mockBuilder
        every { mockBuilder.setListener(any()) } returns mockBuilder
        every { mockBuilder.build() } returns mockLocalBillingClient
        every { mockLocalBillingClient.endConnection() } throws mockk<IllegalArgumentException>()
        val listener = slot<BillingClientStateListener>()
        every { mockLocalBillingClient.startConnection(capture(listener)) } just Runs
        Purchases.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS, mockContext, Callback {
            featureSupported = it
        })
        listener.captured.onBillingSetupFinished(BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED)
        AssertionsForClassTypes.assertThat(featureSupported).isFalse()
        verify (exactly = 1) { mockLocalBillingClient.endConnection() }
    }


    @Test
    fun `when no play services, billing is not supported`() {
        setup()
        var receivedIsBillingSupported = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        mockkStatic(BillingClient::class)
        val mockBuilder = mockk<BillingClient.Builder>(relaxed = true)
        every { BillingClient.newBuilder(any()) } returns mockBuilder
        every { mockBuilder.setListener(any()) } returns mockBuilder
        every { mockBuilder.build() } returns mockLocalBillingClient
        every { mockLocalBillingClient.endConnection() } throws mockk<IllegalArgumentException>()
        val listener = slot<BillingClientStateListener>()
        every { mockLocalBillingClient.startConnection(capture(listener)) } just Runs
        Purchases.isBillingSupported(mockContext, Callback {
            receivedIsBillingSupported = it
        })
        listener.captured.onBillingSetupFinished(BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED)
        AssertionsForClassTypes.assertThat(receivedIsBillingSupported).isFalse()
        verify (exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when finishTransactions is set to false, do not consume transactions but save token in cache`() {
        setup()
        purchases.finishTransactions = false
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "onemonth_freetrial_sub"
        val purchaseTokenSub = "crazy_purchase_token_sub"

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, INAPP) +
            getMockedPurchaseList(skuSub, purchaseTokenSub, SUBS)
        )
        verify(exactly = 1){
            mockBackend.postReceiptData(
                purchaseToken,
                appUserId,
                sku,
                false,
                any(),
                any()
            )
        }
        verify(exactly = 0) {
            mockBillingWrapper.consumePurchase(purchaseToken, any())
        }
        verify(exactly = 0) {
            mockBillingWrapper.consumePurchase(purchaseTokenSub, any())
        }
        verify(exactly = 1) {
            mockCache.addSuccessfullyPostedToken(purchaseToken)
        }
        verify(exactly = 1) {
            mockCache.addSuccessfullyPostedToken(purchaseTokenSub)
        }
    }

    @Test
    fun `when finishTransactions is set to false, don't consume transactions on 40x but save token in cache`() {
        setup()

        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "onemonth_freetrial_sub"
        val purchaseTokenSub = "crazy_purchase_token_sub"

        var capturedLambda: ((PurchasesError, Boolean) -> Unit)? = null
        var capturedLambda1: ((PurchasesError, Boolean) -> Unit)? = null
        every {
            mockBackend.postReceiptData(
                purchaseToken,
                appUserId,
                sku,
                false,
                any(),
                captureLambda()
            )
        } answers {
            capturedLambda = lambda<(PurchasesError, Boolean) -> Unit>().captured.also {
                it.invoke(
                    PurchasesError(PurchasesErrorCode.InvalidCredentialsError),
                    true
                )
            }
        }
        every {
            mockBackend.postReceiptData(
                purchaseTokenSub,
                appUserId,
                skuSub,
                false,
                any(),
                captureLambda()
            )
        } answers {
            capturedLambda1 = lambda<(PurchasesError, Boolean) -> Unit>().captured.also {
                it.invoke(
                    PurchasesError(PurchasesErrorCode.InvalidCredentialsError),
                    true
                )
            }
        }

        purchases.finishTransactions = false

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, INAPP) +
                getMockedPurchaseList(skuSub, purchaseTokenSub, SUBS)
        )

        assertThat(capturedLambda).isNotNull
        assertThat(capturedLambda1).isNotNull
        verify (exactly = 0) {
            mockBillingWrapper.consumePurchase(purchaseToken, any())
        }
        verify (exactly = 0) {
            mockBillingWrapper.consumePurchase(purchaseTokenSub, any())
        }
        verify(exactly = 1) {
            mockCache.addSuccessfullyPostedToken(purchaseToken)
        }
        verify(exactly = 1) {
            mockCache.addSuccessfullyPostedToken(purchaseTokenSub)
        }
    }

    @Test
    fun `when finishTransactions is set to false, don't consume transactions on 50x but save token in cache`() {
        setup()

        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "onemonth_freetrial_sub"
        val purchaseTokenSub = "crazy_purchase_token_sub"

        var captured: ((PurchasesError, Boolean) -> Unit)? = null
        every {
            mockBackend.postReceiptData(
                purchaseToken,
                appUserId,
                sku,
                false,
                any(),
                captureLambda()
            )
        } answers {
            captured = lambda<(PurchasesError, Boolean) -> Unit>().captured.also {
                it.invoke(PurchasesError(PurchasesErrorCode.InvalidCredentialsError), true)
            }
        }

        purchases.finishTransactions = false

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, INAPP) +
            getMockedPurchaseList(skuSub, purchaseTokenSub, SUBS)
        )

        assertThat(captured).isNotNull
        verify (exactly = 0) {
            mockBillingWrapper.consumePurchase(purchaseToken, any())
        }
        verify (exactly = 0) {
            mockBillingWrapper.consumePurchase(purchaseTokenSub, any())
        }
        verify(exactly = 1) {
            mockCache.addSuccessfullyPostedToken(purchaseToken)
        }
        verify(exactly = 1) {
            mockCache.addSuccessfullyPostedToken(purchaseTokenSub)
        }
    }

    @Test
    fun `syncing transactions gets whole history and posts it to backend`() {
        setup()

        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "onemonth_freetrial_sub"
        val purchaseTokenSub = "crazy_purchase_token_sub"

        var capturedLambda: ((List<PurchaseWrapper>) -> Unit)? = null
        every {
            mockBillingWrapper.queryAllPurchases(
                captureLambda(),
                any()
            )
        } answers {
            capturedLambda = lambda<(List<PurchaseWrapper>) -> Unit>().captured.also {
                it.invoke(
                    getMockedPurchaseList(sku, purchaseToken, INAPP) +
                    getMockedPurchaseList(skuSub, purchaseTokenSub, SUBS)
                )
            }
        }

        purchases.syncPurchases()

        assertThat(capturedLambda).isNotNull
        verify (exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken,
                purchases.appUserID,
                sku,
                false,
                any(),
                any()
            )
        }
        verify (exactly = 1) {
            mockBackend.postReceiptData(
                purchaseTokenSub,
                purchases.appUserID,
                skuSub,
                false,
                any(),
                any()
            )
        }
    }

    @Test
    fun `syncing transactions respects allow sharing account settings`() {
        setup()

        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "onemonth_freetrial_sub"
        val purchaseTokenSub = "crazy_purchase_token_sub"
        purchases.allowSharingPlayStoreAccount = true

        var capturedLambda: ((List<PurchaseWrapper>) -> Unit)? = null
        every {
            mockBillingWrapper.queryAllPurchases(
                captureLambda(),
                any()
            )
        } answers {
            capturedLambda = lambda<(List<PurchaseWrapper>) -> Unit>().captured.also {
                it.invoke(
                    getMockedPurchaseList(sku, purchaseToken, INAPP) +
                        getMockedPurchaseList(skuSub, purchaseTokenSub, SUBS)
                )
            }
        }

        purchases.syncPurchases()

        assertThat(capturedLambda).isNotNull
        verify (exactly = 1) {
            mockBackend.postReceiptData(purchaseToken, purchases.appUserID, sku, true, any(), any())
        }
        verify (exactly = 1) {
            mockBackend.postReceiptData(
                purchaseTokenSub,
                purchases.appUserID,
                skuSub,
                true,
                any(),
                any()
            )
        }
    }

    @Test
    fun `syncing transactions never consumes transactions`() {
        setup()

        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        purchases.allowSharingPlayStoreAccount = true

        var capturedLambda: ((List<PurchaseWrapper>) -> Unit)? = null
        every {
            mockBillingWrapper.queryAllPurchases(
                captureLambda(),
                any()
            )
        } answers {
            capturedLambda = lambda<(List<PurchaseWrapper>) -> Unit>().captured.also {
                it.invoke(getMockedPurchaseList(sku, purchaseToken, INAPP))
            }
        }

        purchases.syncPurchases()

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
        verify (exactly = 0){
            mockBillingWrapper.consumePurchase(eq(purchaseToken), any())
        }
        assertThat(capturedLambda).isNotNull
    }

    // TODO: syncing subscriptions never consumes them

    @Test
    fun `Data is successfully postponed if no instance is set`() {
        val jsonObject = JSONObject()
        val network = Purchases.AttributionNetwork.APPSFLYER

        every {
            mockBackend.postAttributionData(appUserId, network, jsonObject, captureLambda())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        val networkUserID = "networkUserID"
        Purchases.addAttributionData(jsonObject, network, networkUserID)

        mockAdInfo(false, networkUserID)

        setup()

        verify { mockBackend.postAttributionData(eq(appUserId), eq(network), eq(jsonObject), any()) }
    }

    @Test
    fun `Data is successfully postponed if no instance is set when sending map`() {
        val network = Purchases.AttributionNetwork.APPSFLYER
        val capturedJSONObject = slot<JSONObject>()

        every {
            mockBackend.postAttributionData(appUserId, network, capture(capturedJSONObject), captureLambda())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        val networkUserID = "networkUserID"
        mockAdInfo(false, networkUserID)
        Purchases.addAttributionData(mapOf("key" to "value"), network, networkUserID)

        setup()

        verify {
            mockBackend.postAttributionData(eq(appUserId), eq(network), any(), any())
        }
        assertThat(capturedJSONObject.captured.get("key")).isEqualTo("value")
    }

    @Test
    fun `GPS ID is automatically added`() {
        val network = Purchases.AttributionNetwork.APPSFLYER
        val capturedJSONObject = slot<JSONObject>()

        every {
            mockBackend.postAttributionData(appUserId, network, capture(capturedJSONObject), captureLambda())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        val networkUserID = "networkUserID"
        mockAdInfo(false, networkUserID)

        Purchases.addAttributionData(mapOf("key" to "value"), network, networkUserID)

        setup()

        verify {
            mockBackend.postAttributionData(
                eq(appUserId),
                eq(network),
                any(),
                any()
            )
        }
        assertThat(capturedJSONObject.captured.get("key")).isEqualTo("value")
        assertThat(capturedJSONObject.captured.get("rc_gps_adid")).isEqualTo(adID)
    }

    @Test
    fun `GPS ID is not added if limited`() {
        val network = Purchases.AttributionNetwork.APPSFLYER
        val capturedJSONObject = slot<JSONObject>()

        every {
            mockBackend.postAttributionData(appUserId, network, capture(capturedJSONObject), captureLambda())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        val networkUserID = "networkUserID"
        mockAdInfo(true, networkUserID)

        Purchases.addAttributionData(mapOf("key" to "value"), network, networkUserID)

        setup()

        verify {
            mockBackend.postAttributionData(eq(appUserId), eq(network), any(), any())
        }
        assertThat(capturedJSONObject.captured.get("key")).isEqualTo("value")
        assertThat(capturedJSONObject.captured.has("rc_gps_adid")).isFalse()
    }

    @Test
    fun `GPS ID is not added if not present`() {
        val network = Purchases.AttributionNetwork.APPSFLYER
        val capturedJSONObject = slot<JSONObject>()

        every {
            mockBackend.postAttributionData(appUserId, network, capture(capturedJSONObject), captureLambda())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        val networkUserID = "networkUserID"
        mockAdInfo(true, networkUserID)

        Purchases.addAttributionData(mapOf("key" to "value"), network, networkUserID)

        setup()

        verify {
            mockBackend.postAttributionData(
                eq(appUserId),
                eq(network),
                any(),
                any()
            )
        }
        assertThat(capturedJSONObject.captured.get("key")).isEqualTo("value")
        assertThat(capturedJSONObject.captured.has("rc_gps_adid")).isFalse()
    }

    @Test
    fun `do not resend last attribution data to backend`() {
        setup()

        val network = Purchases.AttributionNetwork.APPSFLYER
        val capturedJSONObject = slot<JSONObject>()

        every {
            mockBackend.postAttributionData(appUserId, network, capture(capturedJSONObject), captureLambda())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        val networkUserID = "networkUserID"
        mockAdInfo(false, networkUserID)

        every {
            mockCache.getCachedAttributionData(Purchases.AttributionNetwork.APPSFLYER, appUserId)
        } returns "${adID}_networkUserID"

        Purchases.addAttributionData(mapOf("key" to "value"), network, networkUserID)

        verify (exactly = 0){
            mockBackend.postAttributionData(appUserId, network, any(), any())
        }
    }

    @Test
    fun `cache last sent attribution data`() {
        setup()

        val network = Purchases.AttributionNetwork.APPSFLYER
        val capturedJSONObject = slot<JSONObject>()

        every {
            mockBackend.postAttributionData(appUserId, network, capture(capturedJSONObject), captureLambda())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        val networkUserID = "networkid"
        mockAdInfo(false, networkUserID)

        every {
            mockCache.getCachedAttributionData(Purchases.AttributionNetwork.APPSFLYER, appUserId)
        } returns null

        Purchases.addAttributionData(mapOf("key" to "value"), network, networkUserID)

        verify (exactly = 1){
            mockBackend.postAttributionData(appUserId, network, any(), any())
        }

        verify (exactly = 1){
            mockCache.cacheAttributionData(network, appUserId, "${adID}_$networkUserID")
        }
    }

    @Test
    fun `network ID is set`() {
        val network = Purchases.AttributionNetwork.APPSFLYER
        val capturedJSONObject = slot<JSONObject>()

        every {
            mockBackend.postAttributionData(appUserId, network, capture(capturedJSONObject), captureLambda())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        val networkUserID = "networkUserID"
        mockAdInfo(false, networkUserID)

        Purchases.addAttributionData(mapOf("key" to "value"), network, networkUserID)

        setup()

        verify {
            mockBackend.postAttributionData(eq(appUserId), eq(network), any(), any())
        }
        assertThat(capturedJSONObject.captured.get("key")).isEqualTo("value")
        assertThat(capturedJSONObject.captured.get("rc_attribution_network_id")).isEqualTo(networkUserID)
        assertThat(capturedJSONObject.captured.has("rc_gps_adid")).isTrue()
    }

    @Test
    fun `caches are not cleared if update purchaser info fails`() {
        val mockInfo = mockk<PurchaserInfo>()

        mockCache(mockInfo)
        with(mockBackend) {
            val purchasesError = PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken")
            every {
                getPurchaserInfo(any(), any(), captureLambda())
            } answers {
                lambda<(PurchasesError) -> Unit>().captured.invoke(purchasesError)
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
            every {
                close()
            } just Runs
        }
        mockBillingWrapper()
        every {
            listener.onReceived(any())
        } just Runs

        purchases = Purchases(
            mockContext,
            appUserId,
            mockBackend,
            mockBillingWrapper,
            mockCache,
            executorService = mockExecutorService
        )
        Purchases.sharedInstance = purchases

        verify (exactly = 0) { mockCache.clearCachedPurchaserInfo(any()) }
    }

    @Test
    fun `successfully posted receipts are not saved in cache if consumption fails`() {
        setup()
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(getMockedPurchaseList(sku, purchaseToken, INAPP))
        capturedConsumeResponseListener.captured.invoke(2, purchaseToken)
        verify (exactly = 0) {
            mockCache.addSuccessfullyPostedToken(purchaseToken)
        }
    }

    @Test
    fun `when error posting receipts tokens are not saved in cache if error is finishable and consumption fails`() {
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        setup()

        var capturedLambda: ((PurchasesError, Boolean) -> Unit)? = null
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
            capturedLambda = lambda<(PurchasesError, Boolean) -> Unit>().captured
        }

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(getMockedPurchaseList(sku, purchaseToken, INAPP))
        capturedLambda!!.invoke(
            PurchasesError(PurchasesErrorCode.InvalidCredentialsError),
            true
        )
        capturedConsumeResponseListener.captured.invoke(2, purchaseToken)
        verify (exactly = 0 ) {
            mockCache.addSuccessfullyPostedToken(purchaseToken)
        }
    }


    @Test
    fun `successfully posted receipts are saved in cache after consumption`() {
        setup()
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(getMockedPurchaseList(sku, purchaseToken, INAPP))
        capturedConsumeResponseListener.captured.invoke(0, purchaseToken)
        verify (exactly = 1) {
            mockCache.addSuccessfullyPostedToken(purchaseToken)
        }
    }

    @Test
    fun `when error posting receipts tokens are saved in cache if error is finishable`() {
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        setup()

        var capturedLambda: ((PurchasesError, Boolean) -> Unit)? = null
        every {
            mockBackend.postReceiptData(
                purchaseToken,
                appUserId,
                sku,
                false,
                any(),
                captureLambda()
            )
        } answers {
            capturedLambda = lambda<(PurchasesError, Boolean) -> Unit>().captured
        }

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(getMockedPurchaseList(sku, purchaseToken, INAPP))
        capturedLambda!!.invoke(
            PurchasesError(PurchasesErrorCode.InvalidCredentialsError),
            true
        )
        capturedConsumeResponseListener.captured.invoke(0, "crazy_purchase_token")
        verify (exactly = 1) {
            mockCache.addSuccessfullyPostedToken("crazy_purchase_token")
        }
    }

    @Test
    fun `when error posting receipts tokens are not saved in cache if error is not finishable`() {
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "onemonth_freetrial_sub"
        val purchaseTokenSub = "crazy_purchase_token_sub"

        setup()

        var capturedLambda: ((PurchasesError, Boolean) -> Unit)? = null
        var capturedLambda1: ((PurchasesError, Boolean) -> Unit)? = null
        every {
            mockBackend.postReceiptData(
                purchaseToken,
                appUserId,
                sku,
                false,
                any(),
                captureLambda()
            )
        } answers {
            capturedLambda = lambda<(PurchasesError, Boolean) -> Unit>().captured.also {
                it.invoke(PurchasesError(PurchasesErrorCode.InvalidCredentialsError), false)
            }
        }
        every {
            mockBackend.postReceiptData(
                purchaseTokenSub,
                appUserId,
                skuSub,
                false,
                any(),
                captureLambda()
            )
        } answers {
            capturedLambda1 = lambda<(PurchasesError, Boolean) -> Unit>().captured.also {
                it.invoke(PurchasesError(PurchasesErrorCode.InvalidCredentialsError), false)
            }
        }

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, INAPP) +
                getMockedPurchaseList(skuSub, purchaseTokenSub, SUBS)
        )

        assertThat(capturedLambda).isNotNull
        assertThat(capturedLambda1).isNotNull

        verify (exactly = 0) {
            mockCache.addSuccessfullyPostedToken(purchaseToken)
        }
        verify (exactly = 0) {
            mockCache.addSuccessfullyPostedToken(purchaseTokenSub)
        }
    }

    @Test
    fun `reposted receipts are sent using allowSharingAccount`() {
        setup()
        purchases.allowSharingPlayStoreAccount = true
        val purchase = mockk<Purchase>(relaxed = true).apply {
            every { purchaseToken } returns "token"
            every { sku } returns "product"
        }
        every {
            mockBillingWrapper.queryPurchases(SUBS)
        } returns Purchase.PurchasesResult(0, listOf(purchase))
        every {
            mockBillingWrapper.queryPurchases(INAPP)
        } returns Purchase.PurchasesResult(0, emptyList())
        every {
            mockCache.getSentTokens()
        } returns setOf("1234token".sha1())
        every {
            mockCache.setSavedTokens(any())
        } just Runs
        purchases.updatePendingPurchaseQueue()
        verify (exactly = 1) {
            mockBackend.postReceiptData(
                "token",
                appUserId,
                "product",
                true,
                any(),
                any()
            )
        }
    }

    @Test
    fun `when closing instance, activity lifecycle callbacks are unregistered`() {
        setup()
        purchases.close()
        verify (exactly = 1) {
            mockApplication.unregisterActivityLifecycleCallbacks(any())
        }
    }

    @Test
    fun `when updating pending purchases, retrieve both INAPPS and SUBS`() {
        setup()
        every {
            mockBillingWrapper.queryPurchases(SUBS)
        } returns Purchase.PurchasesResult(0, emptyList())
        every {
            mockBillingWrapper.queryPurchases(INAPP)
        } returns Purchase.PurchasesResult(0, emptyList())
        every {
            mockCache.getSentTokens()
        } returns setOf("1234token".sha1())
        val capturedSetTokens = slot<Set<String>>()
        every {
            mockCache.setSavedTokens(capture(capturedSetTokens))
        } just Runs
        purchases.updatePendingPurchaseQueue()
        assertThat(capturedSetTokens.captured).isEmpty()
        verify (exactly = 1) {
            mockBillingWrapper.queryPurchases(SUBS)
        }
        verify (exactly = 1) {
            mockBillingWrapper.queryPurchases(INAPP)
        }
    }

    @Test
    fun `when updating pending purchases, if token is not active anymore, remove it from database`() {
        setup()
        every {
            mockBillingWrapper.queryPurchases(SUBS)
        } returns Purchase.PurchasesResult(0, emptyList())
        every {
            mockBillingWrapper.queryPurchases(INAPP)
        } returns Purchase.PurchasesResult(0, emptyList())
        every {
            mockCache.getSentTokens()
        } returns setOf("1234token".sha1())
        val capturedSetTokens = slot<Set<String>>()
        every {
            mockCache.setSavedTokens(capture(capturedSetTokens))
        } just Runs
        purchases.updatePendingPurchaseQueue()
        assertThat(capturedSetTokens.captured).isEmpty()
    }

    @Test
    fun `when updating pending purchases, if token has not been sent, send it`() {
        setup()
        val purchase = mockk<Purchase>(relaxed = true).apply {
            every { purchaseToken } returns "token"
            every { sku } returns "product"
        }
        every {
            mockBillingWrapper.queryPurchases(SUBS)
        } returns Purchase.PurchasesResult(0, listOf(purchase))
        every {
            mockBillingWrapper.queryPurchases(INAPP)
        } returns Purchase.PurchasesResult(0, emptyList())
        every {
            mockCache.getSentTokens()
        } returns setOf("1234token".sha1())
        val capturedSetTokens = slot<Set<String>>()
        every {
            mockCache.setSavedTokens(capture(capturedSetTokens))
        } just Runs
        purchases.updatePendingPurchaseQueue()
        verify (exactly = 1) {
            mockBackend.postReceiptData(
                "token",
                appUserId,
                "product",
                false,
                any(),
                any()
            )
        }
    }

    @Test
    fun `when updating pending purchases, if token has been sent, don't send it`() {
        setup()
        val token = "1234token"
        val purchase = mockk<Purchase>(relaxed = true).apply {
            every { purchaseToken } returns token
            every { sku } returns "product"
        }
        every {
            mockBillingWrapper.queryPurchases(SUBS)
        } returns Purchase.PurchasesResult(0, listOf(purchase))
        every {
            mockBillingWrapper.queryPurchases(INAPP)
        } returns Purchase.PurchasesResult(0, emptyList())
        every {
            mockCache.getSentTokens()
        } returns setOf(token.sha1())
        val capturedSetTokens = slot<Set<String>>()
        every {
            mockCache.setSavedTokens(capture(capturedSetTokens))
        } just Runs
        purchases.updatePendingPurchaseQueue()
        verify (exactly = 0) {
            mockBackend.postReceiptData(
                token,
                appUserId,
                "product",
                false,
                any(),
                any()
            )
        }
    }

    @Test
    fun `when updating pending purchases, if result from query SUBS is not positive skip`() {
        setup()
        every {
            mockBillingWrapper.queryPurchases(SUBS)
        } returns Purchase.PurchasesResult(-1, emptyList())
        every {
            mockBillingWrapper.queryPurchases(INAPP)
        } returns Purchase.PurchasesResult(0, emptyList())
        purchases.updatePendingPurchaseQueue()
        verify (exactly = 0) {
            mockCache.getSentTokens()
        }
    }

    @Test
    fun `when updating pending purchases, if result from query INAPP is not positive skip`() {
        setup()
        every {
            mockBillingWrapper.queryPurchases(SUBS)
        } returns Purchase.PurchasesResult(0, emptyList())
        every {
            mockBillingWrapper.queryPurchases(INAPP)
        } returns Purchase.PurchasesResult(-1, emptyList())
        purchases.updatePendingPurchaseQueue()
        verify (exactly = 0) {
            mockCache.getSentTokens()
        }
    }

    @Test
    fun `on billing wrapper connected, query purchases and register lifecycle callbacks`() {
        setup()
        every {
            mockCache.getSentTokens()
        } returns setOf("1234token".sha1())
        every {
            mockCache.setSavedTokens(any())
        } just Runs
        every {
            mockBillingWrapper.queryPurchases(SUBS)
        } returns Purchase.PurchasesResult(0, emptyList())
        every {
            mockBillingWrapper.queryPurchases(INAPP)
        } returns Purchase.PurchasesResult(0, emptyList())
        capturedBillingWrapperStateListener.captured.onConnected()
        verify (exactly = 1) {
            mockBillingWrapper.queryPurchases(SUBS)
        }
        verify (exactly = 1) {
            mockBillingWrapper.queryPurchases(INAPP)
        }
        verify (exactly = 1) {
            mockApplication.registerActivityLifecycleCallbacks(any())
        }
        assertThat(capturedActivityLifecycleListener.captured).isNotNull
    }

    // region Private Methods
    private fun mockSkuDetailFetch(details: List<SkuDetails>, skus: List<String>, skuType: String) {
        every {
            mockBillingWrapper.querySkuDetailsAsync(
                eq(skuType),
                eq(skus),
                captureLambda(),
                any()
            )
        } answers {
            lambda<(List<SkuDetails>) -> Unit>().captured.invoke(details)
        }
    }

    private fun mockBillingWrapper() {
        with(mockBillingWrapper) {
            every {
                querySkuDetailsAsync(any(), any(), any(), any())
            } just Runs
            every {
                @Suppress("DEPRECATION")
                makePurchaseAsync(any(), any(), any(), any(), any())
            } just Runs
            every {
                makePurchaseAsync(any(), any(), any(), any())
            } just Runs
            every {
                purchasesUpdatedListener = capture(capturedPurchasesUpdatedListener)
            } just Runs
            every {
                consumePurchase(any(), capture(capturedConsumeResponseListener))
            } just Runs
            every {
                purchasesUpdatedListener = null
            } just Runs
            every {
                stateListener = capture(capturedBillingWrapperStateListener)
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
            every {
                mockCache.addSuccessfullyPostedToken(any())
            } just Runs
            every {
                close()
            } just Runs
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
                cachePurchaserInfo(any(), any())
            } just Runs
            every {
                cacheAppUserID(any())
            } just Runs
            every {
                clearCachedPurchaserInfo(any())
            } just Runs
            every {
                mockCache.getCachedAttributionData(Purchases.AttributionNetwork.APPSFLYER, appUserId)
            } returns null
        }
    }

    private fun mockProducts(skus: List<String>) {
        every {
            mockBackend.getEntitlements(any(), captureLambda(), any())
        } answers {
            val offeringMap = skus.map { sku -> sku + "_offering" to Offering(sku) }.toMap()
            lambda<(Map<String, Entitlement>) -> Unit>().captured.invoke(
                mapOf(
                    "pro" to Entitlement(
                        offeringMap
                    )
                )
            )
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

    private fun mockAdInfo(limitAdTrackingEnabled: Boolean, networkUserID: String) {
        val adInfo = mockk<AdvertisingIdClient.AdInfo>()
        every { adInfo.isLimitAdTrackingEnabled } returns limitAdTrackingEnabled
        every { adInfo.id } returns if (!limitAdTrackingEnabled) adID else ""

        mockkObject(AdvertisingIdClient)

        val lst = slot<(AdvertisingIdClient.AdInfo?) -> Unit>()
        every {
            AdvertisingIdClient.getAdvertisingIdInfo(any(), capture(lst))
        } answers {
            lst.captured.invoke(adInfo)
        }

        every {
            mockCache.cacheAttributionData(
                Purchases.AttributionNetwork.APPSFLYER,
                appUserId,
                listOfNotNull(adID.takeUnless { limitAdTrackingEnabled }, networkUserID).joinToString("_")
            )
        } just Runs
    }
    private fun verifyClose() {
        verify {
            mockBackend.close()
        }
        assertThat(purchases.updatedPurchaserInfoListener).isNull()
        verify {
            mockApplication.unregisterActivityLifecycleCallbacks(any())
        }
        verifyOrder {
            mockBillingWrapper.purchasesUpdatedListener = capturedPurchasesUpdatedListener.captured
            mockBillingWrapper.purchasesUpdatedListener = null
        }
    }

    private fun getMockedPurchaseList(
        sku: String,
        purchaseToken: String,
        skuType: String
    ): ArrayList<PurchaseWrapper> {
        val p: Purchase = mockk()
        every {
            p.sku
        } returns sku
        every {
            p.purchaseToken
        } returns purchaseToken
        every {
            p.purchaseTime
        } returns System.currentTimeMillis()
        val purchasesList = ArrayList<PurchaseWrapper>()
        purchasesList.add(PurchaseWrapper(p, skuType))
        return purchasesList
    }

    // endregion
}
