//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.PostReceiptDataErrorCallback
import com.revenuecat.purchases.common.PostReceiptDataSuccessCallback
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.ReplaceSkuInfo
import com.revenuecat.purchases.common.billingResponseToPurchasesError
import com.revenuecat.purchases.common.buildPurchaserInfo
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.createOfferings
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.google.toProductDetails
import com.revenuecat.purchases.google.toRevenueCatPurchaseDetails
import com.revenuecat.purchases.google.toSKUType
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.Callback
import com.revenuecat.purchases.interfaces.GetSkusResponseListener
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.MakePurchaseListener
import com.revenuecat.purchases.interfaces.ReceivePurchaserInfoListener
import com.revenuecat.purchases.interfaces.UpdatedPurchaserInfoListener
import com.revenuecat.purchases.models.ProductDetails
import com.revenuecat.purchases.models.PurchaseDetails
import com.revenuecat.purchases.models.skuDetails
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.subscriberattributes.SubscriberAttribute
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.toBackendMap
import com.revenuecat.purchases.util.AdvertisingIdClient
import com.revenuecat.purchases.utils.Responses
import com.revenuecat.purchases.utils.SyncDispatcher
import com.revenuecat.purchases.utils.stubGooglePurchase
import com.revenuecat.purchases.utils.stubPurchaseHistoryRecord
import com.revenuecat.purchases.utils.stubSkuDetails
import io.mockk.Call
import io.mockk.CapturingSlot
import io.mockk.MockKAnswerScope
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.AssertionsForClassTypes
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.net.URL
import java.util.ArrayList
import java.util.Collections.emptyList
import java.util.ConcurrentModificationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import com.revenuecat.purchases.common.attribution.AttributionNetwork as CommonAttributionNetwork

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PurchasesTest {
    private val mockBillingAbstract: BillingAbstract = mockk()
    private val mockBackend: Backend = mockk()
    private val mockCache: DeviceCache = mockk()
    private val updatedPurchaserInfoListener: UpdatedPurchaserInfoListener = mockk()
    private val mockApplication = mockk<Application>(relaxed = true)
    private val mockContext = mockk<Context>(relaxed = true).apply {
        every {
            applicationContext
        } returns mockApplication
    }
    private val mockActivity: Activity = mockk()
    private val mockIdentityManager = mockk<IdentityManager>()
    private val mockSubscriberAttributesManager = mockk<SubscriberAttributesManager>()

    private var capturedPurchasesUpdatedListener = slot<BillingAbstract.PurchasesUpdatedListener>()
    private var capturedBillingWrapperStateListener = slot<BillingAbstract.StateListener>()
    private val capturedConsumePurchaseWrapper = slot<PurchaseDetails>()
    private val capturedShouldTryToConsume = slot<Boolean>()

    private val randomAppUserId = "\$RCAnonymousID:ff68f26e432648369a713849a9f93b58"
    private val appUserId = "fakeUserID"
    private val adID = "123"
    private lateinit var purchases: Purchases
    private var receivedSkus: List<SkuDetails>? = null
    private var receivedOfferings: Offerings? = null
    private val mockInfo = mockk<PurchaserInfo>()

    private val stubOfferingIdentifier = "offering_a"
    private val stubProductIdentifier = "monthly_freetrial"
    private val oneOfferingsResponse = "{'offerings': [" +
        "{'identifier': '$stubOfferingIdentifier', " +
        "'description': 'This is the base offering', " +
        "'packages': [" +
        "{'identifier': '\$rc_monthly','platform_product_identifier': '$stubProductIdentifier'}" +
        "]}]," +
        "'current_offering_id': '$stubOfferingIdentifier'}"

    private val mockLifecycle = mockk<Lifecycle>()
    private val mockLifecycleOwner = mockk<LifecycleOwner>()

    @After
    fun tearDown() {
        Purchases.backingFieldSharedInstance = null
        Purchases.postponedAttributionData = mutableListOf()
    }

    @Before
    fun setup() {
        mockkStatic("com.revenuecat.purchases.common.FactoriesKt")
        mockkStatic(ProcessLifecycleOwner::class)

        val skus = listOf(stubProductIdentifier)
        mockCache()
        mockBackend(null)
        mockBillingWrapper()
        mockProductDetails(skus, skus, ProductType.SUBS)

        every {
            updatedPurchaserInfoListener.onReceived(any())
        } just Runs
        every {
            mockIdentityManager.configure(any())
        } just Runs

        anonymousSetup(false)
    }

    private fun anonymousSetup(anonymous: Boolean) {
        val userIdToUse = if (anonymous) randomAppUserId else appUserId

        every {
            mockIdentityManager.currentAppUserID
        } returns userIdToUse

        every {
            mockIdentityManager.currentUserIsAnonymous()
        } returns anonymous

        buildPurchases(anonymous)
        mockSubscriberAttributesManager(userIdToUse)
    }

    private fun stubOfferings(sku: String): Pair<ProductDetails, Offerings> {
        val productDetails = mockk<ProductDetails>().also {
            every { it.sku } returns sku
            every { it.type } returns ProductType.SUBS
        }
        val jsonObject = JSONObject(oneOfferingsResponse)
        val packageObject = Package(
            "\$rc_monthly",
            PackageType.MONTHLY,
            productDetails,
            stubOfferingIdentifier
        )
        val offering = Offering(
            stubOfferingIdentifier,
            "This is the base offering",
            listOf(packageObject)
        )
        val offerings = Offerings(
            offering,
            mapOf(offering.identifier to offering)
        )
        every {
            jsonObject.createOfferings(any())
        } returns offerings
        return Pair(productDetails, offerings)
    }

    @Test
    fun canBeCreated() {
        assertThat(purchases).isNotNull
    }

    @Test
    fun getsSubscriptionSkus() {
        val skus = listOf("onemonth_freetrial")

        val skuDetails = mockProductDetails(skus, listOf(), ProductType.SUBS)

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
        val skus = listOf("normal_purchase")

        val skuDetails = mockProductDetails(skus, listOf(), ProductType.INAPP)

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

    @Test
    fun canMakePurchase() {
        val sku = "onemonth_freetrial"
        val productDetails = mockk<ProductDetails>().also {
            every { it.sku } returns sku
        }

        purchases.purchaseProductWith(
            mockActivity,
            productDetails
        ) { _, _ -> }

        verify {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                productDetails,
                null,
                null
            )
        }
    }

    @Test
    fun canMakePurchaseWithDeprecatedFunction() {
        val sku = "onemonth_freetrial"
        val skuDetails = stubSkuDetails(
            productId = sku
        )

        val slot = slot<ProductDetails>()
        every {
            mockBillingAbstract.makePurchaseAsync(any(), any(), capture(slot), any(), any())
        } just Runs

        purchases.purchaseProductWith(
            mockActivity,
            skuDetails
        ) { _, _ -> }

        assertThat(slot.isCaptured).isTrue()
        assertThat(slot.captured.sku).isEqualTo(sku)
    }

    @Test
    fun canMakePurchaseOfAPackage() {
        val (skuDetails, offerings) = stubOfferings("onemonth_freetrial")

        purchases.purchasePackageWith(
            mockActivity,
            offerings[stubOfferingIdentifier]!!.monthly!!
        ) { _, _ -> }

        verify {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                skuDetails,
                null,
                stubOfferingIdentifier
            )
        }
    }

    @Test
    fun canMakePurchaseUpgradeOfAPackage() {
        val (skuDetails, offerings) = stubOfferings("onemonth_freetrial")

        val oldPurchase = mockPurchaseFound()

        purchases.purchasePackageWith(
            mockActivity,
            offerings[stubOfferingIdentifier]!!.monthly!!,
            UpgradeInfo(oldPurchase.sku)
        ) { _, _ -> }

        verify {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                skuDetails,
                ReplaceSkuInfo(oldPurchase),
                stubOfferingIdentifier
            )
        }
    }

    @Test
    fun postsSuccessfulPurchasesToBackend() {
        val sku = "inapp"
        val purchaseToken = "token_inapp"
        val skuSub = "sub"
        val purchaseTokenSub = "token_sub"

        val productInfo = mockPostReceipt(
            sku,
            purchaseToken,
            observerMode = false,
            mockInfo = mockInfo,
            offeringIdentifier = null,
            type = ProductType.INAPP
        )
        val productInfo1 = mockPostReceipt(
            skuSub,
            purchaseTokenSub,
            observerMode = false,
            mockInfo = mockInfo,
            offeringIdentifier = "offering_a",
            type = ProductType.SUBS
        )
        val mockedInApps = getMockedPurchaseList(sku, purchaseToken, ProductType.INAPP)
        val mockedSubs = getMockedPurchaseList(skuSub, purchaseTokenSub, ProductType.SUBS, "offering_a")
        val allPurchases = mockedInApps + mockedSubs
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(allPurchases)

        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserId,
                isRestore = false,
                observerMode = false,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo,
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }

        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = purchaseTokenSub,
                appUserID = appUserId,
                isRestore = false,
                observerMode = false,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo1,
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }

        allPurchases.forEach {
            verify(exactly = 1) {
                mockBillingAbstract.consumeAndSave(any(), it)
            }
        }
    }

    @Test
    fun callsPostForEachUpdatedPurchase() {
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "sub"
        val purchaseTokenSub = "token_sub"

        val productInfos = listOf(
            mockQueryingSkuDetails(skuSub, ProductType.SUBS, null),
            mockQueryingSkuDetails(sku, ProductType.INAPP, null)
        )

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, ProductType.INAPP) +
                getMockedPurchaseList(skuSub, purchaseTokenSub, ProductType.SUBS)
        )

        productInfos.forEach {
            verify(exactly = 1) {
                mockBackend.postReceiptData(
                    purchaseToken = any(),
                    appUserID = appUserId,
                    isRestore = false,
                    observerMode = false,
                    subscriberAttributes = emptyMap(),
                    receiptInfo = it,
                    storeAppUserID = null,
                    onSuccess = any(),
                    onError = any()
                )
            }
        }
    }

    @Test
    fun doesntPostIfNotOK() {
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError)
        capturedPurchasesUpdatedListener.captured.onPurchasesFailedToUpdate(error)
        verify(exactly = 0) {
            mockBackend.postReceiptData(
                purchaseToken = any(),
                appUserID = any(),
                isRestore = false,
                observerMode = false,
                subscriberAttributes = emptyMap(),
                storeAppUserID = null,
                receiptInfo = any(),
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun passesUpErrors() {
        var errorCalled = false
        val skuDetails = stubSkuDetails(
            productId = "sku"
        )
        purchases.purchaseProductWith(
            mockk(),
            skuDetails.toProductDetails(),
            onError = { error, _ ->
                errorCalled = true
                assertThat(error.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
            }, onSuccess = { _, _ -> })

        val error = PurchasesError(PurchasesErrorCode.StoreProblemError)
        capturedPurchasesUpdatedListener.captured.onPurchasesFailedToUpdate(error)
        assertThat(errorCalled).isTrue()
    }

    @Test
    fun doesNotGetSubscriberInfoOnCreated() {
        verify(exactly = 0) {
            mockBackend.getPurchaserInfo(appUserId, appInBackground = false, onSuccess = any(), onError = any())
        }
    }

    @Test
    fun `fetch purchaser info on foregrounded if it's stale`() {
        mockCacheStale(purchaserInfoStale = true)
        mockSuccessfulQueryPurchases(
            queriedSUBS = emptyMap(),
            queriedINAPP = emptyMap(),
            notInCache = emptyList()
        )
        mockSynchronizeSubscriberAttributesForAllUsers()
        Purchases.sharedInstance.onAppForegrounded()
        verify(exactly = 1) {
            mockBackend.getPurchaserInfo(appUserId, appInBackground = false, onSuccess = any(), onError = any())
        }
    }

    @Test
    fun `fetch offerings on foregrounded if it's stale`() {
        mockCacheStale(offeringsStale = true)
        mockSuccessfulQueryPurchases(
            queriedSUBS = emptyMap(),
            queriedINAPP = emptyMap(),
            notInCache = emptyList()
        )
        mockSynchronizeSubscriberAttributesForAllUsers()
        Purchases.sharedInstance.onAppForegrounded()
        verify(exactly = 1) {
            mockBackend.getOfferings(appUserId, appInBackground = false, onSuccess = any(), onError = any())
        }
        verify(exactly = 1) {
            mockCache.isOfferingsCacheStale(appInBackground = false)
        }
    }

    private fun mockSynchronizeSubscriberAttributesForAllUsers() {
        every {
            mockSubscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(appUserId)
        } just Runs
    }

    @Test
    fun `does not fetch purchaser info on foregrounded if it's not stale`() {
        mockCacheStale()
        mockSuccessfulQueryPurchases(
            queriedSUBS = emptyMap(),
            queriedINAPP = emptyMap(),
            notInCache = emptyList()
        )
        mockSynchronizeSubscriberAttributesForAllUsers()
        purchases.state = purchases.state.copy(firstTimeInForeground = false)
        Purchases.sharedInstance.onAppForegrounded()
        verify(exactly = 0) {
            mockBackend.getPurchaserInfo(appUserId, appInBackground = false, onSuccess = any(), onError = any())
        }
        verify(exactly = 1) {
            mockCache.isPurchaserInfoCacheStale(appInBackground = false, appUserID = appUserId)
        }
    }

    @Test
    fun `does not fetch offerings on foregrounded if it's not stale`() {
        mockCacheStale()
        mockSuccessfulQueryPurchases(
            queriedSUBS = emptyMap(),
            queriedINAPP = emptyMap(),
            notInCache = emptyList()
        )
        mockSynchronizeSubscriberAttributesForAllUsers()
        Purchases.sharedInstance.onAppForegrounded()
        verify(exactly = 0) {
            mockBackend.getOfferings(appUserId, appInBackground = false, onSuccess = any(), onError = any())
        }
        verify(exactly = 1) {
            mockCache.isOfferingsCacheStale(appInBackground = false)
        }
    }

    @Test
    fun canBeSetupWithoutAppUserID() {
        anonymousSetup(true)
        assertThat(purchases).isNotNull

        assertThat(purchases.appUserID).isEqualTo(randomAppUserId)
    }

    @Test
    fun isRestoreWhenUsingNullAppUserID() {
        anonymousSetup(true)

        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        val productInfo = mockQueryingSkuDetails(sku, ProductType.SUBS, null)

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, ProductType.SUBS)
        )
        verify {
            mockBackend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = randomAppUserId,
                isRestore = true,
                observerMode = false,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo,
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun doesntRestoreNormally() {
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        val productInfo = mockQueryingSkuDetails(sku, ProductType.SUBS, null)

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, ProductType.SUBS)
        )

        verify {
            mockBackend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserId,
                isRestore = false,
                observerMode = false,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo,
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun canOverrideAnonMode() {
        purchases.allowSharingPlayStoreAccount = true

        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        val productInfo = mockQueryingSkuDetails(sku, ProductType.SUBS, null)

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, ProductType.SUBS)
        )

        verify {
            mockBackend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserId,
                isRestore = true,
                observerMode = false,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo,
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun restoringPurchasesGetsHistory() {
        var capturedLambda: ((List<PurchaseDetails>) -> Unit)? = null
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
                captureLambda(),
                any()
            )
        } answers {
            capturedLambda = lambda<(List<PurchaseDetails>) -> Unit>().captured.also {
                it.invoke(listOf(mockk(relaxed = true)))
            }
        }

        purchases.restorePurchasesWith { }

        assertThat(capturedLambda).isNotNull
        verify {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
                any(),
                any()
            )
        }
    }

    @Test
    fun historicalPurchasesPassedToBackend() {
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "sub"
        val purchaseTokenSub = "token_sub"

        var capturedLambda: ((List<PurchaseDetails>) -> Unit)? = null
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
                captureLambda(),
                any()
            )
        } answers {
            capturedLambda = lambda<(List<PurchaseDetails>) -> Unit>().captured
            capturedLambda?.invoke(
                getMockedPurchaseHistoryList(sku, purchaseToken, ProductType.INAPP) +
                    getMockedPurchaseHistoryList(skuSub, purchaseTokenSub, ProductType.SUBS)
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

        val productInfo = ReceiptInfo(
            productID = sku
        )
        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserId,
                isRestore = true,
                observerMode = false,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo,
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }

        val productInfo1 = ReceiptInfo(
            productID = skuSub
        )
        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = purchaseTokenSub,
                appUserID = appUserId,
                isRestore = true,
                observerMode = false,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo1,
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun failedToRestorePurchases() {
        val purchasesError = PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken")
        every {
            mockBillingAbstract.queryAllPurchases(appUserId, any(), captureLambda())
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
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "onemonth_freetrial_sub"
        val purchaseTokenSub = "crazy_purchase_token_sub"

        var capturedLambda: ((List<PurchaseDetails>) -> Unit)? = null
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
                captureLambda(),
                any()
            )
        } answers {
            capturedLambda = lambda<(List<PurchaseDetails>) -> Unit>().captured.also {
                it.invoke(
                    getMockedPurchaseHistoryList(sku, purchaseToken, ProductType.INAPP) +
                        getMockedPurchaseHistoryList(skuSub, purchaseTokenSub, ProductType.SUBS)
                )
            }
        }

        val mockInfo = JSONObject(Responses.validFullPurchaserResponse).buildPurchaserInfo()
        every {
            mockBackend.postReceiptData(
                purchaseToken = any(),
                appUserID = any(),
                isRestore = true,
                observerMode = false,
                subscriberAttributes = emptyMap(),
                receiptInfo = any(),
                storeAppUserID = null,
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<PostReceiptDataSuccessCallback>().captured.invoke(mockInfo, mockInfo.jsonObject)
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
            mockBillingAbstract.queryAllPurchases(appUserId, any(), any())
        }

        assertThat(callbackCalled).isTrue()
    }

    @Test
    fun receivedPurchaserInfoShouldBeCached() {
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        val productInfo = mockQueryingSkuDetails(sku, ProductType.SUBS, null)

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, ProductType.SUBS)
        )

        verify {
            mockBackend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserId,
                isRestore = false,
                observerMode = false,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo,
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }
        verify(exactly = 1) {
            mockCache.cachePurchaserInfo(
                any(),
                any()
            )
        }
    }

    @Test
    fun `if no cached offerings, backend is hit when getting offerings`() {
        val skus = listOf(stubProductIdentifier)
        mockProducts()
        mockProductDetails(skus, skus, ProductType.SUBS)
        val (_, offerings) = stubOfferings("onemonth_freetrial")

        every {
            mockCache.cachedOfferings
        } returns null
        every {
            mockCache.cacheOfferings(any())
        } just Runs

        purchases.getOfferingsWith({ fail("should be a success") }) {
            receivedOfferings = it
        }

        assertThat(receivedOfferings).isNotNull

        verify(exactly = 1) {
            mockBackend.getOfferings(appUserId, appInBackground = false, onSuccess = any(), onError = any())
        }
        verify(exactly = 1) {
            mockCache.cacheOfferings(any())
        }
    }

    @Test
    fun `if no cached offerings, backend is hit when getting offerings when on background`() {
        val skus = listOf(stubProductIdentifier)
        mockProducts()
        mockProductDetails(skus, skus, ProductType.SUBS)
        val (_, offerings) = stubOfferings("onemonth_freetrial")

        every {
            mockCache.cachedOfferings
        } returns null
        every {
            mockCache.cacheOfferings(any())
        } just Runs

        purchases.state = purchases.state.copy(appInBackground = true)

        purchases.getOfferingsWith({ fail("should be a success") }) {
            receivedOfferings = it
        }

        assertThat(receivedOfferings).isNotNull

        verify(exactly = 1) {
            mockBackend.getOfferings(appUserId, appInBackground = true, onSuccess = any(), onError = any())
        }
        verify(exactly = 1) {
            mockCache.cacheOfferings(any())
        }
    }

    @Test
    fun `products are populated when getting offerings`() {
        val skus = listOf(stubProductIdentifier)
        mockProducts()
        mockProductDetails(skus, skus, ProductType.SUBS)
        val (_, offerings) = stubOfferings("onemonth_freetrial")

        every {
            mockCache.cachedOfferings
        } returns null
        every {
            mockCache.cacheOfferings(any())
        } just Runs

        purchases.getOfferingsWith({ fail("should be a success") }) {
            receivedOfferings = it
        }

        assertThat(receivedOfferings).isNotNull
        assertThat(receivedOfferings!!.all.size).isEqualTo(1)
        assertThat(receivedOfferings!![stubOfferingIdentifier]!!.monthly!!.product).isNotNull
    }

    @Test
    fun `if cached offerings are not stale`() {
        mockProducts()
        mockProductDetails(listOf(), listOf(), ProductType.SUBS)
        val (_, offerings) = stubOfferings("onemonth_freetrial")

        every {
            mockCache.cachedOfferings
        } returns offerings
        mockCacheStale(offeringsStale = true)

        purchases.getOfferingsWith({ fail("should be a success") }) {
            receivedOfferings = it
        }

        assertThat(receivedOfferings).isEqualTo(offerings)
    }

    @Test
    fun `if cached offerings are not stale in background`() {
        mockProducts()
        mockProductDetails(listOf(), listOf(), ProductType.SUBS)
        val (_, offerings) = stubOfferings("onemonth_freetrial")

        every {
            mockCache.cachedOfferings
        } returns offerings
        mockCacheStale(offeringsStale = true, appInBackground = true)

        purchases.getOfferingsWith({ fail("should be a success") }) {
            receivedOfferings = it
        }

        assertThat(receivedOfferings).isEqualTo(offerings)
    }

    @Test
    fun `if cached offerings are stale, call backend`() {
        mockProducts()
        mockProductDetails(listOf(), listOf(), ProductType.SUBS)
        val (_, offerings) = stubOfferings("onemonth_freetrial")

        every {
            mockCache.cachedOfferings
        } returns offerings
        mockCacheStale(offeringsStale = true)

        purchases.getOfferingsWith({ fail("should be a success") }) {
            receivedOfferings = it
        }

        assertThat(receivedOfferings).isEqualTo(offerings)
        verify(exactly = 1) {
            mockBackend.getOfferings(appUserId, appInBackground = false, onSuccess = any(), onError = any())
        }
    }

    @Test
    fun `if cached offerings are stale when on background, call backend`() {
        mockProducts()
        mockProductDetails(listOf(), listOf(), ProductType.SUBS)
        val (_, offerings) = stubOfferings("onemonth_freetrial")

        every {
            mockCache.cachedOfferings
        } returns offerings
        mockCacheStale(offeringsStale = true, appInBackground = true)

        purchases.state = purchases.state.copy(appInBackground = true)
        purchases.getOfferingsWith({ fail("should be a success") }) {
            receivedOfferings = it
        }

        assertThat(receivedOfferings).isEqualTo(offerings)
        verify(exactly = 1) {
            mockBackend.getOfferings(appUserId, appInBackground = true, onSuccess = any(), onError = any())
        }
    }

    @Test
    fun getOfferingsIsCached() {
        every {
            mockCache.cachedOfferings
        } returns null
        every {
            mockCache.cacheOfferings(any())
        } just Runs

        purchases.getOfferingsWith({ fail("should be a success") }) {
            receivedOfferings = it
        }

        assertThat(receivedOfferings).isNotNull
        verify {
            mockCache.cacheOfferings(any())
        }
    }

    @Test
    fun getOfferingsErrorIsNotCalledIfSkuDetailsMissing() {
        every {
            mockCache.cachedOfferings
        } returns null

        val skus = listOf(stubProductIdentifier)
        mockProducts()
        mockProductDetails(skus, ArrayList(), ProductType.SUBS)
        mockProductDetails(skus, ArrayList(), ProductType.INAPP)

        val errorMessage = emptyArray<PurchasesError>()

        purchases.getOfferingsWith({ errorMessage[0] = it }) {
            receivedOfferings = it
        }

        assertThat(errorMessage.size).isZero()
        assertThat(this.receivedOfferings).isNotNull
    }

    @Test
    fun getOfferingsErrorIsCalledIfNoBackendResponse() {
        every {
            mockCache.cachedOfferings
        } returns null
        every {
            mockCache.cacheOfferings(any())
        } just Runs

        every {
            mockBackend.getOfferings(
                any(),
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

        purchases.getOfferingsWith({ error ->
            purchasesError = error
        }) {
            fail("should be an error")
        }

        assertThat(purchasesError).isNotNull
        verify(exactly = 1) {
            mockCache.clearOfferingsCacheTimestamp()
        }
    }

    @Test
    fun getOfferingsErrorIsCalledIfBadBackendResponse() {
        every {
            mockBackend.getOfferings(any(), any(), captureLambda(), any())
        } answers {
            lambda<(JSONObject) -> Unit>().captured.invoke(JSONObject("{}"))
        }
        every {
            mockCache.cachedOfferings
        } returns null

        var purchasesError: PurchasesError? = null

        purchases.getOfferingsWith({ error ->
            purchasesError = error
        }) {
            fail("should be an error")
        }

        assertThat(purchasesError).isNotNull
        assertThat(purchasesError!!.code).isEqualTo(PurchasesErrorCode.UnexpectedBackendResponseError)
        verify(exactly = 1) {
            mockCache.clearOfferingsCacheTimestamp()
        }
    }

    @Test
    fun `attribution data gets converted`() {
        val jsonObject = JSONObject()
        jsonObject.put("key", "value")
        val network = CommonAttributionNetwork.APPSFLYER

        val jsonSlot = slot<JSONObject>()
        every {
            mockSubscriberAttributesManager.convertAttributionDataAndSetAsSubscriberAttributes(
                capture(jsonSlot),
                network,
                appUserId
            )
        } just Runs

        val networkUserID = "networkid"
        mockAdInfo(false, networkUserID)

        Purchases.addAttributionData(jsonObject, Purchases.AttributionNetwork.APPSFLYER, networkUserID)

        verify {
            mockSubscriberAttributesManager.convertAttributionDataAndSetAsSubscriberAttributes(
                jsonObject = any(),
                network,
                appUserId
            )
        }
        assertThat(jsonSlot.captured["key"]).isEqualTo("value")
    }

    @Test
    fun addAttributionConvertsStringStringMapToJsonObject() {
        val network = CommonAttributionNetwork.APPSFLYER
        val capturedJSONObject = slot<JSONObject>()
        every {
            mockSubscriberAttributesManager.convertAttributionDataAndSetAsSubscriberAttributes(
                capture(capturedJSONObject),
                network,
                appUserId
            )
        } just Runs

        val networkUserID = "networkUserID"
        mockAdInfo(false, networkUserID)

        Purchases.addAttributionData(
            mapOf(
                "adgroup" to null,
                "campaign" to "awesome_campaign",
                "campaign_id" to 1234,
                "iscache" to true
            ),
            Purchases.AttributionNetwork.APPSFLYER,
            networkUserID
        )

        verify {
            mockSubscriberAttributesManager.convertAttributionDataAndSetAsSubscriberAttributes(
                jsonObject = any(),
                network,
                appUserId
            )
        }
        assertThat(capturedJSONObject.isCaptured).isTrue()
        assertThat(capturedJSONObject.captured.get("adgroup")).isEqualTo(null)
        assertThat(capturedJSONObject.captured.get("campaign")).isEqualTo("awesome_campaign")
        assertThat(capturedJSONObject.captured.get("campaign_id")).isEqualTo(1234)
        assertThat(capturedJSONObject.captured.get("iscache")).isEqualTo(true)
    }

    @Test
    fun `tries to consume purchases on 4xx`() {
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "sub"
        val purchaseTokenSub = "token_sub"

        var capturedLambda: (PostReceiptDataErrorCallback)? = null
        mockPostReceiptError(
            sku,
            purchaseToken,
            observerMode = false,
            offeringIdentifier = null,
            type = ProductType.INAPP,
            answer = {
                capturedLambda = lambda<PostReceiptDataErrorCallback>().captured
                capturedLambda?.invokeWithFinishableError()
            }
        )
        mockPostReceiptError(
            skuSub,
            purchaseTokenSub,
            observerMode = false,
            offeringIdentifier = null,
            type = ProductType.SUBS,
            answer = {
                capturedLambda = lambda<PostReceiptDataErrorCallback>().captured
                capturedLambda?.invokeWithFinishableError()
            }
        )

        val purchases = getMockedPurchaseList(sku, purchaseToken, ProductType.INAPP) +
            getMockedPurchaseList(skuSub, purchaseTokenSub, ProductType.SUBS)
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(purchases)

        purchases.forEach {
            verify(exactly = 1) {
                mockBillingAbstract.consumeAndSave(true, it)
            }
        }

        assertThat(capturedLambda).isNotNull
    }

    @Test
    fun `tries to consume restored purchases on 4xx`() {
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "sub"
        val purchaseTokenSub = "token_sub"

        var capturedPostReceiptLambda: (PostReceiptDataErrorCallback)? = null
        mockPostReceiptError(
            sku,
            purchaseToken,
            observerMode = false,
            offeringIdentifier = null,
            type = ProductType.INAPP,
            answer = {
                capturedPostReceiptLambda = lambda<PostReceiptDataErrorCallback>().captured
                capturedPostReceiptLambda?.invokeWithFinishableError()
            },
            isRestore = true
        )
        mockPostReceiptError(
            skuSub,
            purchaseTokenSub,
            observerMode = false,
            offeringIdentifier = null,
            type = ProductType.SUBS,
            answer = {
                capturedPostReceiptLambda = lambda<PostReceiptDataErrorCallback>().captured
                capturedPostReceiptLambda?.invokeWithFinishableError()
            },
            isRestore = true
        )
        val purchaseRecords = getMockedPurchaseHistoryList(sku, purchaseToken, ProductType.INAPP) +
            getMockedPurchaseHistoryList(skuSub, purchaseTokenSub, ProductType.SUBS)

        var capturedRestoreLambda: ((List<PurchaseDetails>) -> Unit)? = null
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
                captureLambda(),
                any()
            )
        } answers {
            capturedRestoreLambda = lambda<(List<PurchaseDetails>) -> Unit>().captured
            capturedRestoreLambda?.invoke(purchaseRecords)
        }

        var errorCalled = false
        purchases.restorePurchasesWith(onSuccess = {
            fail("Should not be success")
        }, onError = {
            errorCalled = true
        })
        assertThat(capturedRestoreLambda).isNotNull
        assertThat(errorCalled).isTrue()

        purchaseRecords.forEach {
            verify(exactly = 1) {
                mockBillingAbstract.consumeAndSave(true, it)
            }
        }
    }

    @Test
    fun `does not consume purchases on 5xx`() {
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "sub"
        val purchaseTokenSub = "token_sub"

        mockPostReceiptError(
            sku,
            purchaseToken,
            observerMode = false,
            offeringIdentifier = null,
            type = ProductType.INAPP,
            answer = {
                lambda<PostReceiptDataErrorCallback>().captured.invokeWithNotFinishableError()
            }
        )
        mockPostReceiptError(
            skuSub,
            purchaseTokenSub,
            observerMode = false,
            offeringIdentifier = null,
            type = ProductType.SUBS,
            answer = {
                lambda<PostReceiptDataErrorCallback>().captured.invokeWithNotFinishableError()
            }
        )

        val purchaseRecords = getMockedPurchaseList(sku, purchaseToken, ProductType.INAPP) +
            getMockedPurchaseList(skuSub, purchaseTokenSub, ProductType.SUBS)

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(purchaseRecords)

        purchaseRecords.forEach {
            verify(exactly = 0) {
                mockBillingAbstract.consumeAndSave(any(), it)
            }
        }
    }

    @Test
    fun `does not consume restored purchases on 5xx`() {
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "sub"
        val purchaseTokenSub = "token_sub"

        val purchaseRecords = getMockedPurchaseHistoryList(sku, purchaseToken, ProductType.INAPP) +
            getMockedPurchaseHistoryList(skuSub, purchaseTokenSub, ProductType.SUBS)

        var capturedRestoreLambda: ((List<PurchaseDetails>) -> Unit)? = null
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
                captureLambda(),
                any()
            )
        } answers {
            capturedRestoreLambda = lambda<(List<PurchaseDetails>) -> Unit>().captured
            capturedRestoreLambda?.invoke(purchaseRecords)
        }

        mockPostReceiptError(
            sku,
            purchaseToken,
            observerMode = false,
            offeringIdentifier = null,
            type = ProductType.INAPP,
            answer = {
                lambda<PostReceiptDataErrorCallback>().captured.invokeWithNotFinishableError()
            },
            isRestore = true
        )
        mockPostReceiptError(
            skuSub,
            purchaseTokenSub,
            observerMode = false,
            offeringIdentifier = null,
            type = ProductType.SUBS,
            answer = {
                lambda<PostReceiptDataErrorCallback>().captured.invokeWithNotFinishableError()
            },
            isRestore = true
        )

        var errorCalled = false
        purchases.restorePurchasesWith(onSuccess = {
            fail("Should not be success")
        }, onError = {
            errorCalled = true
        })
        assertThat(capturedRestoreLambda).isNotNull
        assertThat(errorCalled).isTrue()

        purchaseRecords.forEach {
            verify(exactly = 0) {
                mockBillingAbstract.consumeAndSave(any(), it)
            }
        }
    }

    @Test
    fun closeCloses() {
        mockCloseActions()

        purchases.close()
        verifyClose()
    }

    @Test
    fun whenNoTokensRestoringPurchasesStillCallListener() {
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
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
        every {
            mockIdentityManager.createAlias(
                eq("new_id"),
                captureLambda(),
                any()
            )
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }
        every {
            mockBackend.getPurchaserInfo(any(), any(), captureLambda(), any())
        } answers {
            lambda<(PurchaserInfo) -> Unit>().captured.invoke(mockInfo)
        }
        every {
            mockCache.setPurchaserInfoCacheTimestampToNow("new_id")
        } just Runs

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
        val purchasesError = PurchasesError(PurchasesErrorCode.InvalidCredentialsError)
        every {
            mockIdentityManager.createAlias(
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
    fun `when creating alias, caches are updated`() {
        every {
            mockIdentityManager.createAlias(
                eq("new_id"),
                captureLambda(),
                any()
            )
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }
        every {
            mockBackend.getPurchaserInfo(any(), any(), captureLambda(), any())
        } answers {
            lambda<(PurchaserInfo) -> Unit>().captured.invoke(mockInfo)
        }

        every {
            mockCache.setPurchaserInfoCacheTimestampToNow("new_id")
        } just Runs

        val mockCompletion = mockk<ReceivePurchaserInfoListener>(relaxed = true)
        purchases.createAlias(
            "new_id",
            mockCompletion
        )

        verify(exactly = 1) {
            mockCache.setPurchaserInfoCacheTimestampToNow("new_id")
        }
        verify(exactly = 1) {
            mockCache.setOfferingsCacheTimestampToNow()
        }
        verify(exactly = 1) {
            mockBackend.getPurchaserInfo("new_id", false, any(), any())
        }
        verify(exactly = 1) {
            mockBackend.getOfferings("new_id", false, any(), any())
        }
    }

    @Test
    fun `when identifying, user is identified and caches updated`() {
        every {
            mockIdentityManager.identify("new_id", captureLambda(), any())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }
        every {
            mockCache.setPurchaserInfoCacheTimestampToNow("new_id")
        } just Runs
        purchases.identify("new_id")

        verify(exactly = 1) {
            mockIdentityManager.identify("new_id", any(), any())
        }
        verify(exactly = 1) {
            mockCache.setPurchaserInfoCacheTimestampToNow("new_id")
        }
        verify(exactly = 1) {
            mockCache.setOfferingsCacheTimestampToNow()
        }
        verify(exactly = 1) {
            mockBackend.getPurchaserInfo("new_id", any(), any(), any())
        }
        verify(exactly = 1) {
            mockBackend.getOfferings("new_id", any(), any(), any())
        }
    }

    @Test
    fun `when resetting, identity manager is called`() {
        every {
            mockCache.clearLatestAttributionData(appUserId)
        } just Runs
        every {
            mockIdentityManager.reset()
        } just Runs
        purchases.reset()
        verify {
            mockIdentityManager.reset()
        }
    }

    @Test
    fun `when resetting, caches are updated`() {
        every {
            mockCache.clearLatestAttributionData(appUserId)
        } just Runs
        every {
            mockIdentityManager.reset()
        } just Runs
        purchases.reset()

        verify(exactly = 1) {
            mockCache.setPurchaserInfoCacheTimestampToNow("fakeUserID")
        }
        verify(exactly = 1) {
            mockCache.setOfferingsCacheTimestampToNow()
        }
        verify(exactly = 1) {
            mockBackend.getPurchaserInfo("fakeUserID", any(), any(), any())
        }
        verify(exactly = 1) {
            mockBackend.getOfferings("fakeUserID", any(), any(), any())
        }
    }

    @Test
    fun `when resetting, backend caches are cleared`() {
        setup()
        every {
            mockCache.clearLatestAttributionData(appUserId)
        } just Runs
        every {
            mockIdentityManager.reset()
        } just Runs
        every {
            mockBackend.clearCaches()
        } just Runs

        purchases.reset()

        verify(exactly = 1) {
            mockBackend.clearCaches()
        }
    }

    @Test
    fun `when setting up, and passing a appUserID, user is identified`() {
        assertThat(purchases.allowSharingPlayStoreAccount).isEqualTo(false)
        assertThat(purchases.appUserID).isEqualTo(appUserId)
    }

    @Test
    fun `when setting listener, caches are retrieved`() {
        purchases.updatedPurchaserInfoListener = updatedPurchaserInfoListener

        verify {
            updatedPurchaserInfoListener.onReceived(any())
        }
    }

    @Test
    fun `when setting shared instance and there's already an instance, instance is closed`() {
        mockCloseActions()
        Purchases.sharedInstance = purchases
        verifyClose()
    }

    @Test
    fun `when setting listener, listener is called`() {
        purchases.updatedPurchaserInfoListener = updatedPurchaserInfoListener

        verify(exactly = 1) {
            updatedPurchaserInfoListener.onReceived(any())
        }
    }

    @Test
    fun `when setting listener for anonymous user, listener is called`() {
        anonymousSetup(true)
        purchases.updatedPurchaserInfoListener = updatedPurchaserInfoListener

        verify(exactly = 1) {
            updatedPurchaserInfoListener.onReceived(any())
        }
    }

    @Test
    fun `given a random purchase update, listener is called if purchaser info has changed`() {
        val info = mockk<PurchaserInfo>()

        every {
            mockBackend.postReceiptData(
                purchaseToken = any(),
                appUserID = any(),
                isRestore = any(),
                observerMode = any(),
                subscriberAttributes = any(),
                receiptInfo = any(),
                storeAppUserID = null,
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<PostReceiptDataSuccessCallback>().captured.invoke(
                info,
                JSONObject(Responses.validFullPurchaserResponse)
            )
        }
        purchases.updatedPurchaserInfoListener = updatedPurchaserInfoListener
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        mockQueryingSkuDetails(sku, ProductType.SUBS, null)
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, ProductType.SUBS)
        )

        verify(exactly = 2) {
            updatedPurchaserInfoListener.onReceived(any())
        }
    }

    @Test
    fun `given a random purchase update, listener is not called if purchaser info has not changed`() {
        purchases.updatedPurchaserInfoListener = updatedPurchaserInfoListener
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        mockQueryingSkuDetails(sku, ProductType.SUBS, null)
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, ProductType.SUBS)
        )

        verify(exactly = 1) {
            updatedPurchaserInfoListener.onReceived(any())
        }
    }

    @Test
    fun `when making another purchase for a product for a pending product, error is issued`() {
        purchases.updatedPurchaserInfoListener = updatedPurchaserInfoListener

        val skuDetails = mockk<ProductDetails>().also {
            every { it.sku } returns "sku"
        }
        purchases.purchaseProductWith(
            mockk(),
            skuDetails,
            onError = { _, _ -> fail("Should be success") }) { _, _ ->
            // First one works
        }

        var errorCalled: PurchasesError? = null
        purchases.purchaseProductWith(
            mockk(),
            skuDetails,
            onError = { error, _ ->
                errorCalled = error
            }) { _, _ ->
            fail("Should be error")
        }

        assertThat(errorCalled!!.code).isEqualTo(PurchasesErrorCode.OperationAlreadyInProgressError)
    }

    @Test
    fun `when making purchase, completion block is called once`() {
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        mockQueryingSkuDetails(sku, ProductType.SUBS, null)

        val skuDetails = mockk<ProductDetails>().also {
            every { it.sku } returns sku
        }

        var callCount = 0
        purchases.purchaseProductWith(
            mockActivity,
            skuDetails,
            onSuccess = { _, _ ->
                callCount++
            }, onError = { _, _ -> fail("should be successful") })

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, ProductType.SUBS)
        )
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, ProductType.SUBS)
        )

        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun `when making purchase, completion block not called for different products`() {
        val sku = "onemonth_freetrial"
        val sku1 = "onemonth_freetrial_1"
        val purchaseToken1 = "crazy_purchase_token_1"
        var callCount = 0
        mockQueryingSkuDetails(sku1, ProductType.SUBS, null)
        purchases.purchaseProductWith(
            mockActivity,
            mockk<ProductDetails>().also {
                every { it.sku } returns sku
            },
            onSuccess = { _, _ ->
                callCount++
            }, onError = { _, _ -> fail("should be successful") })

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku1, purchaseToken1, ProductType.SUBS)
        )

        assertThat(callCount).isEqualTo(0)
    }

    @Test
    fun `sends cached purchaser info to getter`() {
        every {
            mockBackend.getPurchaserInfo(any(), any(), captureLambda(), any())
        } answers {
            // Timeout
        }

        var receivedInfo: PurchaserInfo? = null
        purchases.getPurchaserInfoWith(onSuccess = {
            receivedInfo = it
        }, onError = {
            fail("supposed to be successful")
        })

        assertThat(receivedInfo).isEqualTo(mockInfo)
    }

    @Test
    fun `given no cached purchaser info, backend is called again`() {
        every {
            mockCache.getCachedPurchaserInfo(any())
        } returns null
        every {
            mockBackend.getPurchaserInfo(any(), any(), captureLambda(), any())
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
        verify(exactly = 1) { mockBackend.getPurchaserInfo(any(), any(), any(), any()) }
    }

    @Test
    fun `don't create an alias if the new app user id is the same`() {
        val lock = CountDownLatch(1)
        purchases.createAliasWith(appUserId) {
            lock.countDown()
        }

        lock.await(200, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isZero()
        verify(exactly = 0) {
            mockBackend.createAlias(appUserId, appUserId, any(), any())
        }
    }

    @Test
    fun `don't identify if the new app user id is the same`() {
        var receivedInfo: PurchaserInfo? = null
        purchases.identifyWith(appUserId) {
            receivedInfo = it
        }

        verify(exactly = 1) {
            mockCache.getCachedPurchaserInfo(appUserId)
        }
        assertThat(receivedInfo).isEqualTo(mockInfo)
    }

    @Test
    fun `when multiple make purchase callbacks, a failure doesn't throw ConcurrentModificationException`() {
        purchases.purchaseProductWith(
            mockActivity,
            mockk<ProductDetails>().also {
                every { it.sku } returns "sku"
            }
        ) { _, _ -> }

        purchases.purchaseProductWith(
            mockActivity,
            mockk<ProductDetails>().also {
                every { it.sku } returns "sku"
            }
        ) { _, _ -> }

        try {
            val error = PurchasesError(PurchasesErrorCode.StoreProblemError)
            capturedPurchasesUpdatedListener.captured.onPurchasesFailedToUpdate(error)
        } catch (e: ConcurrentModificationException) {
            fail("Test throws ConcurrentModificationException")
        }
    }

    @Test
    fun `login with the same appUserID as the current, fetches purchaserInfo and calls onSuccess if successful`() {
        val appUserID = "myUser"
        every { mockCache.isPurchaserInfoCacheStale(appUserID, any()) } returns true
        every { mockCache.setPurchaserInfoCacheTimestampToNow(appUserID) } just Runs
        every { mockIdentityManager.currentAppUserID } returns appUserID

        val mockCompletion = mockk<LogInCallback>(relaxed = true)

        purchases.logIn(appUserID, mockCompletion)

        verify(exactly = 1) {
            mockCompletion.onReceived(any(), any())
            mockBackend.getPurchaserInfo(appUserID, any(), any(), any())
        }
    }

    @Test
    fun `login with the same appUserID as the current, calls onSuccess with created false if successful`() {
        val appUserID = "myUser"
        every { mockCache.isPurchaserInfoCacheStale(appUserID, any()) } returns true
        every { mockCache.setPurchaserInfoCacheTimestampToNow(appUserID) } just Runs
        every { mockIdentityManager.currentAppUserID } returns appUserID

        val mockCompletion = mockk<LogInCallback>(relaxed = true)

        purchases.logIn(appUserID, mockCompletion)

        verify(exactly = 1) {
            mockCompletion.onReceived(any(), false)
        }
    }

    @Test
    fun `login with the same appUserID as the current, fetches purchaserInfo and calls onError if failed`() {
        val appUserID = "myUser"
        every { mockCache.getCachedPurchaserInfo(appUserID) } returns null
        every { mockCache.isPurchaserInfoCacheStale(appUserID, any()) } returns true
        every { mockCache.setPurchaserInfoCacheTimestampToNow(appUserID) } just Runs
        every { mockCache.clearPurchaserInfoCacheTimestamp(appUserID) } just Runs
        every { mockIdentityManager.currentAppUserID } returns appUserID

        val purchasesError = PurchasesError(PurchasesErrorCode.InvalidCredentialsError)
        mockBackend(errorGettingPurchaserInfo = purchasesError)

        val mockCompletion = mockk<LogInCallback>(relaxed = true)

        purchases.logIn(appUserID, mockCompletion)

        verify(exactly = 1) {
            mockCompletion.onError(purchasesError)
        }
    }

    @Test
    fun `login called with different appUserID makes correct identityManager call`() {
        val mockCreated = Random.nextBoolean()
        every { mockIdentityManager.currentAppUserID } returns "oldAppUserID"
        every {
            mockIdentityManager.logIn(any(), onSuccess = captureLambda(), any())
        } answers {
            lambda<(PurchaserInfo, Boolean) -> Unit>().captured.invoke(mockInfo, mockCreated)
        }

        val mockCompletion = mockk<LogInCallback>(relaxed = true)
        val newAppUserID = "newAppUserID"
        purchases.logIn(newAppUserID, mockCompletion)

        verify(exactly = 1) {
            mockIdentityManager.logIn(newAppUserID, any(), any())
        }
    }

    @Test
    fun `login called with different appUserID passes errors to caller if call fails`() {
        every { mockIdentityManager.currentAppUserID } returns "oldAppUserID"
        val purchasesError = PurchasesError(PurchasesErrorCode.InvalidCredentialsError)

        every {
            mockIdentityManager.logIn(any(), any(), onError = captureLambda())
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(purchasesError)
        }

        val mockCompletion = mockk<LogInCallback>(relaxed = true)
        val newAppUserID = "newAppUserID"
        purchases.logIn(newAppUserID, mockCompletion)

        verify(exactly = 1) {
            mockCompletion.onError(purchasesError)
        }
    }

    @Test
    fun `login called with different appUserID calls listener with correct values`() {
        val mockCreated = Random.nextBoolean()
        every { mockIdentityManager.currentAppUserID } returns "oldAppUserID"

        every {
            mockIdentityManager.logIn(any(), onSuccess = captureLambda(), any())
        } answers {
            lambda<(PurchaserInfo, Boolean) -> Unit>().captured.invoke(mockInfo, mockCreated)
        }

        val mockCompletion = mockk<LogInCallback>(relaxed = true)
        val newAppUserID = "newAppUserID"
        purchases.logIn(newAppUserID, mockCompletion)

        verify(exactly = 1) {
            mockCompletion.onReceived(mockInfo, mockCreated)
        }
    }

    @Test
    fun `login successful with new appUserID calls delegate if purchaserInfo changed`() {
        purchases.updatedPurchaserInfoListener = updatedPurchaserInfoListener

        val mockCreated = Random.nextBoolean()
        every { mockIdentityManager.currentAppUserID } returns "oldAppUserID"

        every {
            mockIdentityManager.logIn(any(), onSuccess = captureLambda(), any())
        } answers {
            lambda<(PurchaserInfo, Boolean) -> Unit>().captured.invoke(mockInfo, mockCreated)
        }

        val mockCompletion = mockk<LogInCallback>(relaxed = true)
        val newAppUserID = "newAppUserID"
        purchases.logIn(newAppUserID, mockCompletion)

        verify(exactly = 1) {
            updatedPurchaserInfoListener.onReceived(mockInfo)
        }
    }

    @Test
    fun `login successful with new appUserID refreshes offerings`() {
        val mockCreated = Random.nextBoolean()
        every { mockIdentityManager.currentAppUserID } returns "oldAppUserID"

        every {
            mockIdentityManager.logIn(any(), onSuccess = captureLambda(), any())
        } answers {
            lambda<(PurchaserInfo, Boolean) -> Unit>().captured.invoke(mockInfo, mockCreated)
        }

        val mockCompletion = mockk<LogInCallback>(relaxed = true)
        val newAppUserID = "newAppUserID"
        purchases.logIn(newAppUserID, mockCompletion)

        verify(exactly = 1) {
            mockBackend.getOfferings(newAppUserID, any(), any(), any())
        }
    }

    @Test
    fun `logout called with identified user makes right calls`() {
        val appUserID = "fakeUserID"
        every {
            mockCache.clearLatestAttributionData(appUserID)
        } just Runs
        every {
            mockIdentityManager.logOut()
        } returns null
        val mockCompletion = mockk<ReceivePurchaserInfoListener>(relaxed = true)
        purchases.logOut(mockCompletion)

        verify(exactly = 1) {
            mockCache.setPurchaserInfoCacheTimestampToNow(appUserID)
        }
        verify(exactly = 1) {
            mockCache.setOfferingsCacheTimestampToNow()
        }
        verify(exactly = 1) {
            mockBackend.getPurchaserInfo(appUserID, any(), any(), any())
        }
        verify(exactly = 1) {
            mockBackend.getOfferings(appUserID, any(), any(), any())
        }
    }

    @Test
    fun `when logging out, identity manager is called`() {
        every {
            mockCache.clearLatestAttributionData(appUserId)
        } just Runs
        every {
            mockIdentityManager.logOut()
        } returns null

        purchases.logOut()
        verify(exactly = 1) {
            mockIdentityManager.logOut()
        }
    }

    @Test
    fun `if there's an error on logOut, the error is passed`() {
        every {
            mockCache.clearLatestAttributionData(appUserId)
        } just Runs
        val mockError = mockk<PurchasesError>(relaxed = true)
        val mockCompletion = mockk<ReceivePurchaserInfoListener>(relaxed = true)
        every {
            mockIdentityManager.logOut()
        } returns mockError

        purchases.logOut(mockCompletion)
        verify(exactly = 1) {
            mockCompletion.onError(mockError)
        }
    }

    @Test
    fun `logOut calls completion with new purchaserInfo when successful`() {
        every {
            mockCache.clearLatestAttributionData(appUserId)
        } just Runs

        every {
            mockBackend.getPurchaserInfo(any(), any(), onSuccess = captureLambda(), any())
        } answers {
            lambda<(PurchaserInfo) -> Unit>().captured.invoke(mockInfo)
        }

        val mockCompletion = mockk<ReceivePurchaserInfoListener>(relaxed = true)
        every {
            mockIdentityManager.logOut()
        } returns null

        purchases.logOut(mockCompletion)
        verify(exactly = 1) {
            mockCompletion.onReceived(mockInfo)
        }
    }

    @Test
    fun `logOut clears backend caches when successful`() {
        setup()
        val mockInfo = mockk<PurchaserInfo>()

        every {
            mockCache.clearLatestAttributionData(appUserId)
        } just Runs

        every {
            mockBackend.getPurchaserInfo(any(), any(), onSuccess = captureLambda(), any())
        } answers {
            lambda<(PurchaserInfo) -> Unit>().captured.invoke(mockInfo)
        }
        every {
            mockBackend.clearCaches()
        } just Runs

        val mockCompletion = mockk<ReceivePurchaserInfoListener>(relaxed = true)
        every {
            mockIdentityManager.logOut()
        } returns null

        purchases.logOut(mockCompletion)
        verify(exactly = 1) {
            mockBackend.clearCaches()
        }
    }

    @Test
    fun `when checking if Billing is supported, an OK response when starting connection means it's supported`() {
        var receivedIsBillingSupported = false

        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = isBillingSupportedSetup(mockLocalBillingClient)

        Purchases.isBillingSupported(mockContext, Callback {
            receivedIsBillingSupported = it
        })

        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())
        AssertionsForClassTypes.assertThat(receivedIsBillingSupported).isTrue()
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when checking if Billing is supported, disconnections mean billing is not supported`() {
        var receivedIsBillingSupported = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)

        val listener = isBillingSupportedSetup(mockLocalBillingClient)

        Purchases.isBillingSupported(mockContext, Callback {
            receivedIsBillingSupported = it
        })
        listener.captured.onBillingServiceDisconnected()
        AssertionsForClassTypes.assertThat(receivedIsBillingSupported).isFalse()
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when checking if Billing is supported, a non OK response when starting connection means it's not supported`() {
        var receivedIsBillingSupported = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = isBillingSupportedSetup(mockLocalBillingClient)

        Purchases.isBillingSupported(mockContext, Callback {
            receivedIsBillingSupported = it
        })
        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult())
        AssertionsForClassTypes.assertThat(receivedIsBillingSupported).isFalse()
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when checking if feature is supported, an OK response when starting connection means it's supported`() {
        var featureSupported = false
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        every {
            mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        } returns BillingClient.BillingResponseCode.OK.buildResult()
        val listener = isBillingSupportedSetup(mockLocalBillingClient)

        Purchases.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS, mockContext, Callback {
            featureSupported = it
        })
        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())
        AssertionsForClassTypes.assertThat(featureSupported).isTrue()
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when checking if feature is supported, disconnections mean billing is not supported`() {
        var featureSupported = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = isBillingSupportedSetup(mockLocalBillingClient)

        Purchases.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS, mockContext, Callback {
            featureSupported = it
        })
        listener.captured.onBillingServiceDisconnected()
        AssertionsForClassTypes.assertThat(featureSupported).isFalse()
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when checking if feature is supported, a non OK response when starting connection means it's not supported`() {
        var featureSupported = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        every {
            mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        } returns BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult()

        val listener = isBillingSupportedSetup(mockLocalBillingClient)

        Purchases.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS, mockContext, Callback {
            featureSupported = it
        })
        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult())
        AssertionsForClassTypes.assertThat(featureSupported).isFalse()
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when no play services, feature is not supported`() {
        var featureSupported = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = isBillingSupportedSetup(mockLocalBillingClient)
        every {
            mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        } returns BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult()

        every { mockLocalBillingClient.endConnection() } throws mockk<IllegalArgumentException>()
        Purchases.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS, mockContext, Callback {
            featureSupported = it
        })
        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult())
        AssertionsForClassTypes.assertThat(featureSupported).isFalse()
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when calling isFeatureSupported, enablePendingPurchases is called`() {
        var featureSupported = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        every {
            mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        } returns BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult()
        mockkStatic(BillingClient::class)
        val mockBuilder = mockk<BillingClient.Builder>(relaxed = true)
        every { BillingClient.newBuilder(any()) } returns mockBuilder
        every { mockBuilder.setListener(any()) } returns mockBuilder
        every { mockBuilder.enablePendingPurchases() } returns mockBuilder
        every { mockBuilder.build() } returns mockLocalBillingClient
        val listener = slot<BillingClientStateListener>()
        every { mockLocalBillingClient.startConnection(capture(listener)) } just Runs

        every { mockLocalBillingClient.endConnection() } throws mockk<IllegalArgumentException>()
        Purchases.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS, mockContext, Callback {
            featureSupported = it
        })
        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult())
        AssertionsForClassTypes.assertThat(featureSupported).isFalse()
        verify(exactly = 1) { mockBuilder.enablePendingPurchases() }
    }

    @Test
    fun `when no play services, billing is not supported`() {
        var receivedIsBillingSupported = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = isBillingSupportedSetup(mockLocalBillingClient)

        every { mockLocalBillingClient.endConnection() } throws mockk<IllegalArgumentException>()
        Purchases.isBillingSupported(mockContext, Callback {
            receivedIsBillingSupported = it
        })
        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult())
        AssertionsForClassTypes.assertThat(receivedIsBillingSupported).isFalse()
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when calling isBillingSupported, enablePendingPurchases is called`() {
        var receivedIsBillingSupported = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        mockkStatic(BillingClient::class)
        val mockBuilder = mockk<BillingClient.Builder>(relaxed = true)
        every { BillingClient.newBuilder(any()) } returns mockBuilder
        every { mockBuilder.setListener(any()) } returns mockBuilder
        every { mockBuilder.enablePendingPurchases() } returns mockBuilder
        every { mockBuilder.build() } returns mockLocalBillingClient
        every { mockLocalBillingClient.endConnection() } throws mockk<IllegalArgumentException>()
        val listener = slot<BillingClientStateListener>()
        every { mockLocalBillingClient.startConnection(capture(listener)) } just Runs
        Purchases.isBillingSupported(mockContext, Callback {
            receivedIsBillingSupported = it
        })
        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult())
        AssertionsForClassTypes.assertThat(receivedIsBillingSupported).isFalse()
        verify(exactly = 1) { mockBuilder.enablePendingPurchases() }
    }

    @Test
    fun `when billing is not supported, canMakePayments is false`() {
        var receivedCanMakePayments = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = isBillingSupportedSetup(mockLocalBillingClient)

        every {
            mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        } returns BillingClient.BillingResponseCode.OK.buildResult()

        Purchases.canMakePayments(mockContext, listOf(BillingFeature.SUBSCRIPTIONS), Callback {
            receivedCanMakePayments = it
        })

        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE.buildResult())

        AssertionsForClassTypes.assertThat(receivedCanMakePayments).isFalse()
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when feature is not supported, canMakePayments is false`() {
        var receivedCanMakePayments = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = isBillingSupportedSetup(mockLocalBillingClient)

        every {
            mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        } returns BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult()

        Purchases.canMakePayments(mockContext, listOf(BillingFeature.SUBSCRIPTIONS), Callback {
            receivedCanMakePayments = it
        })

        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())

        AssertionsForClassTypes.assertThat(receivedCanMakePayments).isFalse()
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when one feature in list is not supported, canMakePayments is false`() {
        var receivedCanMakePayments = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = isBillingSupportedSetup(mockLocalBillingClient)

        every {
            mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        } returns BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult()

        every {
            mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.IN_APP_ITEMS_ON_VR)
        } returns BillingClient.BillingResponseCode.OK.buildResult()

        Purchases.canMakePayments(mockContext,
            listOf(BillingFeature.SUBSCRIPTIONS, BillingFeature.IN_APP_ITEMS_ON_VR),
            Callback {
                receivedCanMakePayments = it
            })

        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())

        AssertionsForClassTypes.assertThat(receivedCanMakePayments).isFalse()
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when feature is supported and billing is supported, canMakePayments is true`() {
        var receivedCanMakePayments = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = isBillingSupportedSetup(mockLocalBillingClient)

        every {
            mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        } returns BillingClient.BillingResponseCode.OK.buildResult()

        Purchases.canMakePayments(mockContext, listOf(BillingFeature.SUBSCRIPTIONS), Callback {
            receivedCanMakePayments = it
        })

        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())

        AssertionsForClassTypes.assertThat(receivedCanMakePayments).isTrue()
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when feature list is empty, canMakePayments does not check billing client`() {
        var receivedCanMakePayments = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = isBillingSupportedSetup(mockLocalBillingClient)

        Purchases.canMakePayments(mockContext, listOf(), Callback {
            receivedCanMakePayments = it
        })

        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())

        verify(exactly = 0) { mockLocalBillingClient.isFeatureSupported(any()) }
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when finishTransactions is set to false, do not consume transactions`() {
        purchases.finishTransactions = false
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "onemonth_freetrial_sub"
        val purchaseTokenSub = "crazy_purchase_token_sub"

        val productInfo = mockPostReceipt(
            sku,
            purchaseToken,
            observerMode = true,
            mockInfo = mockInfo,
            offeringIdentifier = null,
            type = ProductType.INAPP
        )

        val productInfo1 = mockPostReceipt(
            skuSub,
            purchaseTokenSub,
            observerMode = true,
            mockInfo = mockInfo,
            offeringIdentifier = null,
            type = ProductType.SUBS
        )

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, ProductType.INAPP) +
                getMockedPurchaseList(skuSub, purchaseTokenSub, ProductType.SUBS)
        )

        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserId,
                isRestore = false,
                observerMode = true,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo,
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }

        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = purchaseTokenSub,
                appUserID = appUserId,
                isRestore = false,
                observerMode = true,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo1,
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }
        assertThat(capturedShouldTryToConsume.isCaptured).isTrue()
        assertThat(capturedShouldTryToConsume.captured).isFalse
    }

    @Test
    fun `when finishTransactions is set to false, don't consume transactions on 4xx`() {
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "onemonth_freetrial_sub"
        val purchaseTokenSub = "crazy_purchase_token_sub"

        mockPostReceiptError(
            sku,
            purchaseToken,
            observerMode = true,
            offeringIdentifier = null,
            type = ProductType.INAPP,
            answer = {
                lambda<PostReceiptDataErrorCallback>().captured.also {
                    it.invokeWithFinishableError()
                }
            }
        )

        mockPostReceiptError(
            skuSub,
            purchaseTokenSub,
            observerMode = true,
            offeringIdentifier = null,
            type = ProductType.SUBS,
            answer = {
                lambda<PostReceiptDataErrorCallback>().captured.also {
                    it.invokeWithFinishableError()
                }
            }
        )

        purchases.finishTransactions = false

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, ProductType.INAPP) +
                getMockedPurchaseList(skuSub, purchaseTokenSub, ProductType.SUBS)
        )

        assertThat(capturedShouldTryToConsume.isCaptured).isTrue()
        assertThat(capturedShouldTryToConsume.captured).isFalse
    }

    @Test
    fun `when finishTransactions is set to false, don't consume transactions on 5xx`() {

        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "onemonth_freetrial_sub"
        val purchaseTokenSub = "crazy_purchase_token_sub"

        mockPostReceiptError(
            sku,
            purchaseToken,
            observerMode = true,
            offeringIdentifier = null,
            type = ProductType.INAPP,
            answer = {
                lambda<PostReceiptDataErrorCallback>().captured.invokeWithNotFinishableError()
            })
        mockPostReceiptError(
            skuSub,
            purchaseTokenSub,
            observerMode = true,
            offeringIdentifier = null,
            type = ProductType.SUBS,
            answer = {
                lambda<PostReceiptDataErrorCallback>().captured.invokeWithNotFinishableError()
            })
        purchases.finishTransactions = false

        val allPurchases = getMockedPurchaseList(sku, purchaseToken, ProductType.INAPP) +
            getMockedPurchaseList(skuSub, purchaseTokenSub, ProductType.SUBS)
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(allPurchases)

        allPurchases.forEach {
            verify(exactly = 0) {
                mockBillingAbstract.consumeAndSave(any(), it)
            }
        }
    }

    @Test
    fun `when finishTransactions is set to false, it shouldn't consume when restoring`() {
        purchases.finishTransactions = false

        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "sub"
        val purchaseTokenSub = "token_sub"

        mockPostReceipt(
            sku,
            purchaseToken,
            observerMode = true,
            mockInfo = mockInfo,
            offeringIdentifier = null,
            type = ProductType.INAPP,
            restore = true
        )
        mockPostReceipt(
            skuSub,
            purchaseTokenSub,
            observerMode = true,
            mockInfo = mockInfo,
            offeringIdentifier = null,
            type = ProductType.SUBS,
            restore = true
        )
        val purchaseRecords = getMockedPurchaseHistoryList(sku, purchaseToken, ProductType.INAPP) +
            getMockedPurchaseHistoryList(skuSub, purchaseTokenSub, ProductType.SUBS)

        var capturedRestoreLambda: ((List<PurchaseDetails>) -> Unit)? = null
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
                captureLambda(),
                any()
            )
        } answers {
            capturedRestoreLambda = lambda<(List<PurchaseDetails>) -> Unit>().captured
            capturedRestoreLambda?.invoke(purchaseRecords)
        }

        var restoreCalled = false
        purchases.restorePurchasesWith(onSuccess = {
            restoreCalled = true
        }, onError = {
            fail("Should not be an error")
        })
        assertThat(capturedRestoreLambda).isNotNull
        assertThat(restoreCalled).isTrue()

        purchaseRecords.forEach {
            verify(exactly = 1) {
                mockBillingAbstract.consumeAndSave(false, it)
            }
        }
    }

    @Test
    fun `syncing transactions gets whole history and posts it to backend`() {

        purchases.finishTransactions = false

        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "onemonth_freetrial_sub"
        val purchaseTokenSub = "crazy_purchase_token_sub"

        var capturedLambda: ((List<PurchaseDetails>) -> Unit)? = null
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
                captureLambda(),
                any()
            )
        } answers {
            capturedLambda = lambda<(List<PurchaseDetails>) -> Unit>().captured.also {
                it.invoke(
                    getMockedPurchaseHistoryList(sku, purchaseToken, ProductType.INAPP) +
                        getMockedPurchaseHistoryList(skuSub, purchaseTokenSub, ProductType.SUBS)
                )
            }
        }

        purchases.syncPurchases()

        val productInfo = ReceiptInfo(
            productID = sku
        )
        assertThat(capturedLambda).isNotNull
        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserId,
                isRestore = false,
                observerMode = true,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo,
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }
        val productInfo1 = ReceiptInfo(
            productID = skuSub
        )
        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = purchaseTokenSub,
                appUserID = appUserId,
                isRestore = false,
                observerMode = true,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo1,
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `syncing transactions respects allow sharing account settings`() {

        purchases.finishTransactions = false

        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "onemonth_freetrial_sub"
        val purchaseTokenSub = "crazy_purchase_token_sub"
        purchases.allowSharingPlayStoreAccount = true

        var capturedLambda: ((List<PurchaseDetails>) -> Unit)? = null
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
                captureLambda(),
                any()
            )
        } answers {
            capturedLambda = lambda<(List<PurchaseDetails>) -> Unit>().captured.also {
                it.invoke(
                    getMockedPurchaseHistoryList(sku, purchaseToken, ProductType.INAPP) +
                        getMockedPurchaseHistoryList(skuSub, purchaseTokenSub, ProductType.SUBS)
                )
            }
        }

        purchases.syncPurchases()

        val productInfo = ReceiptInfo(
            productID = sku
        )
        assertThat(capturedLambda).isNotNull
        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserId,
                isRestore = true,
                observerMode = true,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo,
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }

        val productInfo1 = ReceiptInfo(
            productID = skuSub
        )
        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = purchaseTokenSub,
                appUserID = appUserId,
                isRestore = true,
                observerMode = true,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo1,
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `syncing transactions never consumes transactions`() {

        purchases.finishTransactions = false
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        purchases.allowSharingPlayStoreAccount = true

        var capturedLambda: ((List<PurchaseDetails>) -> Unit)? = null
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
                captureLambda(),
                any()
            )
        } answers {
            capturedLambda = lambda<(List<PurchaseDetails>) -> Unit>().captured.also {
                it.invoke(getMockedPurchaseHistoryList(sku, purchaseToken, ProductType.INAPP))
            }
        }

        purchases.syncPurchases()

        val productInfo = ReceiptInfo(
            productID = sku
        )
        verify {
            mockBackend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserId,
                isRestore = true,
                observerMode = true,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo,
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }
        verify(exactly = 0) {
            mockBillingAbstract.consumeAndSave(any(), any())
        }
        assertThat(capturedLambda).isNotNull
    }

    @Test
    fun `syncing a transaction posts normalized purchase data to backend`() {
        purchases.finishTransactions = false

        val skuParent = "sub"
        val skuTerm = "sub.monthly"
        val purchaseToken = "crazy_purchase_token"
        val amazonUserID = "amazon_user_id"
        val price = 10.40
        val currencyCode = "USD"

        every {
            mockBillingAbstract.normalizePurchaseData(
                productID = skuParent,
                purchaseToken = purchaseToken,
                storeUserID = amazonUserID,
                captureLambda(),
                any()
            )
        } answers {
            lambda<(String) -> Unit>().captured.also {
                it.invoke(skuTerm)
            }
        }

        every {
            mockCache.getPreviouslySentHashedTokens()
        } returns setOf()

        purchases.syncObserverModeAmazonPurchase(
            productID = skuParent,
            receiptId = purchaseToken,
            amazonUserID = amazonUserID,
            price = price,
            isoCurrencyCode = currencyCode
        )

        val productInfo = ReceiptInfo(
            productID = skuTerm,
            price = price,
            currency = currencyCode
        )
        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserId,
                isRestore = false,
                observerMode = true,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo,
                storeAppUserID = amazonUserID,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `syncing a transaction respects allow sharing account settings`() {
        purchases.finishTransactions = false

        val skuParent = "sub"
        val skuTerm = "sub.monthly"
        val purchaseToken = "crazy_purchase_token"
        val amazonUserID = "amazon_user_id"
        val price = 10.40
        val currencyCode = "USD"
        purchases.allowSharingPlayStoreAccount = true

        every {
            mockBillingAbstract.normalizePurchaseData(
                productID = skuParent,
                purchaseToken = purchaseToken,
                storeUserID = amazonUserID,
                captureLambda(),
                any()
            )
        } answers {
            lambda<(String) -> Unit>().captured.also {
                it.invoke(skuTerm)
            }
        }

        every {
            mockCache.getPreviouslySentHashedTokens()
        } returns setOf()

        purchases.syncObserverModeAmazonPurchase(
            productID = skuParent,
            receiptId = purchaseToken,
            amazonUserID = amazonUserID,
            price = price,
            isoCurrencyCode = currencyCode
        )

        val productInfo = ReceiptInfo(
            productID = skuTerm,
            price = price,
            currency = currencyCode
        )
        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserId,
                isRestore = true,
                observerMode = true,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo,
                storeAppUserID = amazonUserID,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `syncing a transaction never consumes it`() {
        purchases.finishTransactions = false

        val skuParent = "sub"
        val skuTerm = "sub.monthly"
        val purchaseToken = "crazy_purchase_token"
        val amazonUserID = "amazon_user_id"
        val price = 10.40
        val currencyCode = "USD"
        purchases.allowSharingPlayStoreAccount = true

        var capturedLambda: ((String) -> Unit)? = null
        every {
            mockBillingAbstract.normalizePurchaseData(
                productID = skuParent,
                purchaseToken = purchaseToken,
                storeUserID = amazonUserID,
                captureLambda(),
                any()
            )
        } answers {
            capturedLambda = lambda<(String) -> Unit>().captured.also {
                it.invoke(skuTerm)
            }
        }

        every {
            mockCache.getPreviouslySentHashedTokens()
        } returns setOf()

        purchases.syncObserverModeAmazonPurchase(
            productID = skuParent,
            receiptId = purchaseToken,
            amazonUserID = amazonUserID,
            price = price,
            isoCurrencyCode = currencyCode
        )

        val productInfo = ReceiptInfo(
            productID = skuTerm,
            price = price,
            currency = currencyCode
        )
        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserId,
                isRestore = true,
                observerMode = true,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo,
                storeAppUserID = amazonUserID,
                onSuccess = any(),
                onError = any()
            )
        }

        verify(exactly = 0) {
            mockBillingAbstract.consumeAndSave(any(), any())
        }
        assertThat(capturedLambda).isNotNull
    }

    @Test
    fun `transaction is not synced again if it was already synced`() {
        purchases.finishTransactions = false

        val skuParent = "sub"
        val skuTerm = "sub.monthly"
        val purchaseToken = "crazy_purchase_token"
        val amazonUserID = "amazon_user_id"
        val price = 10.40
        val currencyCode = "USD"

        every {
            mockCache.getPreviouslySentHashedTokens()
        } returns emptySet()

        every {
            mockBillingAbstract.normalizePurchaseData(
                productID = skuParent,
                purchaseToken = purchaseToken,
                storeUserID = amazonUserID,
                captureLambda(),
                any()
            )
        } answers {
            lambda<(String) -> Unit>().captured.also {
                it.invoke(skuTerm)
            }
        }

        purchases.syncObserverModeAmazonPurchase(
            productID = skuParent,
            receiptId = purchaseToken,
            amazonUserID = amazonUserID,
            price = price,
            isoCurrencyCode = currencyCode
        )

        val productInfo = ReceiptInfo(
            productID = skuTerm,
            price = price,
            currency = currencyCode
        )
        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserId,
                isRestore = false,
                observerMode = true,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo,
                storeAppUserID = amazonUserID,
                onSuccess = any(),
                onError = any()
            )
        }

        verify(exactly = 1) {
            mockCache.addSuccessfullyPostedToken(purchaseToken)
        }

        every {
            mockCache.getPreviouslySentHashedTokens()
        } returns setOf(purchaseToken.sha1())

        purchases.syncObserverModeAmazonPurchase(
            productID = skuParent,
            receiptId = purchaseToken,
            amazonUserID = amazonUserID,
            price = price,
            isoCurrencyCode = currencyCode
        )

        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = any(),
                appUserID = any(),
                isRestore = any(),
                observerMode = any(),
                subscriberAttributes = any(),
                receiptInfo = any(),
                storeAppUserID = any(),
                onSuccess = any(),
                onError = any()
            )
        }

        verify(exactly = 1) {
            mockCache.addSuccessfullyPostedToken(any())
        }

        verify(exactly = 0) {
            mockBillingAbstract.consumeAndSave(any(), any())
        }
    }

    @Test
    fun `syncing a transaction sends subscriber attributes`() {
        purchases.finishTransactions = false

        val skuParent = "sub"
        val skuTerm = "sub.monthly"
        val purchaseToken = "crazy_purchase_token"
        val amazonUserID = "amazon_user_id"
        val price = 10.40
        val currencyCode = "USD"
        val subscriberAttributeKey = "favorite_cat"
        val subscriberAttributeValue = "gardfield"
        val subscriberAttribute = SubscriberAttribute(subscriberAttributeKey, subscriberAttributeValue)
        val unsyncedSubscriberAttributes = mapOf(subscriberAttributeKey to subscriberAttribute)
        purchases.allowSharingPlayStoreAccount = true

        every {
            mockSubscriberAttributesManager.getUnsyncedSubscriberAttributes(appUserId)
        } returns unsyncedSubscriberAttributes

        var capturedLambda: ((String) -> Unit)? = null
        every {
            mockBillingAbstract.normalizePurchaseData(
                productID = skuParent,
                purchaseToken = purchaseToken,
                storeUserID = amazonUserID,
                captureLambda(),
                any()
            )
        } answers {
            capturedLambda = lambda<(String) -> Unit>().captured.also {
                it.invoke(skuTerm)
            }
        }

        every {
            mockCache.getPreviouslySentHashedTokens()
        } returns setOf()

        purchases.syncObserverModeAmazonPurchase(
            productID = skuParent,
            receiptId = purchaseToken,
            amazonUserID = amazonUserID,
            price = price,
            isoCurrencyCode = currencyCode
        )

        val productInfo = ReceiptInfo(
            productID = skuTerm,
            price = price,
            currency = currencyCode
        )
        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserId,
                isRestore = true,
                observerMode = true,
                subscriberAttributes = unsyncedSubscriberAttributes.toBackendMap(),
                receiptInfo = productInfo,
                storeAppUserID = amazonUserID,
                onSuccess = any(),
                onError = any()
            )
        }

        verify(exactly = 0) {
            mockBillingAbstract.consumeAndSave(any(), any())
        }
        assertThat(capturedLambda).isNotNull
    }

    @Test
    fun `Data is successfully postponed if no instance is set`() {
        val jsonObject = JSONObject()
        val network = CommonAttributionNetwork.APPSFLYER
        val networkUserID = "networkUserID"

        mockAdInfo(false, networkUserID)

        every {
            mockSubscriberAttributesManager.convertAttributionDataAndSetAsSubscriberAttributes(
                jsonObject,
                network,
                appUserId
            )
        } just Runs

        Purchases.addAttributionData(jsonObject, Purchases.AttributionNetwork.APPSFLYER, networkUserID)

        verify {
            mockSubscriberAttributesManager.convertAttributionDataAndSetAsSubscriberAttributes(
                jsonObject = any(),
                network,
                appUserId
            )
        }
    }

    @Test
    fun `Data is successfully postponed if no instance is set when sending map`() {
        val network = CommonAttributionNetwork.APPSFLYER
        val capturedJSONObject = slot<JSONObject>()

        every {
            mockSubscriberAttributesManager.convertAttributionDataAndSetAsSubscriberAttributes(
                capture(capturedJSONObject),
                network,
                appUserId
            )
        } just Runs

        val networkUserID = "networkUserID"
        mockAdInfo(false, networkUserID)
        Purchases.addAttributionData(mapOf("key" to "value"), Purchases.AttributionNetwork.APPSFLYER, networkUserID)

        verify {
            mockSubscriberAttributesManager.convertAttributionDataAndSetAsSubscriberAttributes(
                jsonObject = any(),
                network,
                appUserId
            )
        }
        assertThat(capturedJSONObject.captured.get("key")).isEqualTo("value")
    }

    @Test
    fun `GPS ID is automatically added`() {
        val network = CommonAttributionNetwork.APPSFLYER
        val capturedJSONObject = slot<JSONObject>()

        every {
            mockSubscriberAttributesManager.convertAttributionDataAndSetAsSubscriberAttributes(
                capture(capturedJSONObject),
                network,
                appUserId
            )
        } just Runs

        val networkUserID = "networkUserID"
        mockAdInfo(false, networkUserID)

        Purchases.addAttributionData(mapOf("key" to "value"), Purchases.AttributionNetwork.APPSFLYER, networkUserID)

        verify {
            mockSubscriberAttributesManager.convertAttributionDataAndSetAsSubscriberAttributes(
                jsonObject = any(),
                network,
                appUserId
            )
        }
        assertThat(capturedJSONObject.captured.get("key")).isEqualTo("value")
        assertThat(capturedJSONObject.captured.get("rc_gps_adid")).isEqualTo(adID)
    }

    @Test
    fun `GPS ID is not added if limited`() {
        val network = CommonAttributionNetwork.APPSFLYER
        val capturedJSONObject = slot<JSONObject>()

        every {
            mockSubscriberAttributesManager.convertAttributionDataAndSetAsSubscriberAttributes(
                capture(capturedJSONObject),
                network,
                appUserId
            )
        } just Runs

        val networkUserID = "networkUserID"
        mockAdInfo(true, networkUserID)

        Purchases.addAttributionData(mapOf("key" to "value"), Purchases.AttributionNetwork.APPSFLYER, networkUserID)

        verify {
            mockSubscriberAttributesManager.convertAttributionDataAndSetAsSubscriberAttributes(
                jsonObject = any(),
                network,
                appUserId
            )
        }
        assertThat(capturedJSONObject.captured.get("key")).isEqualTo("value")
        assertThat(capturedJSONObject.captured.has("rc_gps_adid")).isFalse()
    }

    @Test
    fun `GPS ID is not added if not present`() {
        val network = CommonAttributionNetwork.APPSFLYER
        val capturedJSONObject = slot<JSONObject>()

        every {
            mockSubscriberAttributesManager.convertAttributionDataAndSetAsSubscriberAttributes(
                capture(capturedJSONObject),
                network,
                appUserId
            )
        } just Runs

        val networkUserID = "networkUserID"
        mockAdInfo(true, networkUserID)

        Purchases.addAttributionData(mapOf("key" to "value"), Purchases.AttributionNetwork.APPSFLYER, networkUserID)

        verify {
            mockSubscriberAttributesManager.convertAttributionDataAndSetAsSubscriberAttributes(
                jsonObject = any(),
                network,
                appUserId
            )
        }
        assertThat(capturedJSONObject.captured.get("key")).isEqualTo("value")
        assertThat(capturedJSONObject.captured.has("rc_gps_adid")).isFalse()
    }

    @Test
    fun `do not resend last attribution data to backend`() {

        val network = CommonAttributionNetwork.APPSFLYER
        val capturedJSONObject = slot<JSONObject>()

        every {
            mockSubscriberAttributesManager.convertAttributionDataAndSetAsSubscriberAttributes(
                capture(capturedJSONObject),
                network,
                appUserId
            )
        } just Runs

        val networkUserID = "networkUserID"
        mockAdInfo(false, networkUserID)

        every {
            mockCache.getCachedAttributionData(network, appUserId)
        } returns "${adID}_networkUserID"

        Purchases.addAttributionData(mapOf("key" to "value"), Purchases.AttributionNetwork.APPSFLYER, networkUserID)

        verify(exactly = 0) {
            mockSubscriberAttributesManager.convertAttributionDataAndSetAsSubscriberAttributes(
                jsonObject = any(),
                network,
                appUserId
            )
        }
    }

    @Test
    fun `cache last sent attribution data`() {

        val network = CommonAttributionNetwork.APPSFLYER
        val capturedJSONObject = slot<JSONObject>()

        every {
            mockSubscriberAttributesManager.convertAttributionDataAndSetAsSubscriberAttributes(
                capture(capturedJSONObject),
                network,
                appUserId
            )
        } just Runs

        val networkUserID = "networkid"
        mockAdInfo(false, networkUserID)

        every {
            mockCache.getCachedAttributionData(CommonAttributionNetwork.APPSFLYER, appUserId)
        } returns null

        Purchases.addAttributionData(mapOf("key" to "value"), Purchases.AttributionNetwork.APPSFLYER, networkUserID)

        verify(exactly = 1) {
            mockSubscriberAttributesManager.convertAttributionDataAndSetAsSubscriberAttributes(
                jsonObject = any(),
                network,
                appUserId
            )
        }

        verify(exactly = 1) {
            mockCache.cacheAttributionData(network, appUserId, "${adID}_$networkUserID")
        }
    }

    @Test
    fun `network ID is set`() {
        val network = CommonAttributionNetwork.APPSFLYER
        val capturedJSONObject = slot<JSONObject>()

        every {
            mockSubscriberAttributesManager.convertAttributionDataAndSetAsSubscriberAttributes(
                capture(capturedJSONObject),
                network,
                appUserId
            )
        } just Runs

        val networkUserID = "networkUserID"
        mockAdInfo(false, networkUserID)

        Purchases.addAttributionData(mapOf("key" to "value"), Purchases.AttributionNetwork.APPSFLYER, networkUserID)



        verify {
            mockSubscriberAttributesManager.convertAttributionDataAndSetAsSubscriberAttributes(
                jsonObject = any(),
                network,
                appUserId
            )
        }
        assertThat(capturedJSONObject.captured.get("key")).isEqualTo("value")
        assertThat(capturedJSONObject.captured.get("rc_attribution_network_id")).isEqualTo(networkUserID)
        assertThat(capturedJSONObject.captured.has("rc_gps_adid")).isTrue()
    }

    @Test
    fun `caches are not cleared if update purchaser info fails`() {
        mockBackend(PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken"))

        val lock = CountDownLatch(1)
        purchases.getPurchaserInfoWith(onSuccess = {
            lock.countDown()
        }, onError = {
            fail("supposed to be successful")
        })
        lock.await(200, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isZero()
        verify(exactly = 0) { mockCache.clearCachesForAppUserID(any()) }
    }

    @Test
    fun `when error posting receipts tokens are not saved in cache if error is not finishable`() {
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val skuSub = "onemonth_freetrial_sub"
        val purchaseTokenSub = "crazy_purchase_token_sub"

        var capturedLambda: (PostReceiptDataErrorCallback)? = null
        mockPostReceiptError(
            sku,
            purchaseToken,
            observerMode = false,
            offeringIdentifier = null,
            type = ProductType.INAPP,
            answer = {
                capturedLambda = lambda<PostReceiptDataErrorCallback>().captured.also {
                    it.invokeWithNotFinishableError()
                }
            }
        )

        var capturedLambda1: (PostReceiptDataErrorCallback)? = null
        mockPostReceiptError(
            skuSub,
            purchaseTokenSub,
            observerMode = false,
            offeringIdentifier = null,
            type = ProductType.SUBS,
            answer = {
                capturedLambda1 = lambda<PostReceiptDataErrorCallback>().captured.also {
                    it.invokeWithNotFinishableError()
                }
            }
        )

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, ProductType.INAPP) +
                getMockedPurchaseList(skuSub, purchaseTokenSub, ProductType.SUBS)
        )

        assertThat(capturedLambda).isNotNull
        assertThat(capturedLambda1).isNotNull

        verify(exactly = 0) {
            mockCache.addSuccessfullyPostedToken(purchaseToken)
        }
        verify(exactly = 0) {
            mockCache.addSuccessfullyPostedToken(purchaseTokenSub)
        }
    }

    @Test
    fun `reposted receipts are sent using allowSharingAccount`() {

        purchases.allowSharingPlayStoreAccount = true
        val purchase = stubGooglePurchase(
            purchaseToken = "token",
            productId = "product",
            purchaseState = Purchase.PurchaseState.PURCHASED
        )
        val activePurchase = purchase.toRevenueCatPurchaseDetails(ProductType.SUBS, null)
        mockSuccessfulQueryPurchases(
            queriedSUBS = mapOf(purchase.purchaseToken.sha1() to activePurchase),
            queriedINAPP = emptyMap(),
            notInCache = listOf(activePurchase)
        )
        val productInfo = mockQueryingSkuDetails("product", ProductType.SUBS, null)

        purchases.updatePendingPurchaseQueue()

        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = "token",
                appUserID = appUserId,
                isRestore = true,
                observerMode = false,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo,
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `when closing instance, activity lifecycle callbacks are unregistered`() {
        every {
            ProcessLifecycleOwner.get()
        } returns mockLifecycleOwner
        every {
            mockLifecycleOwner.lifecycle
        } returns mockLifecycle
        every {
            mockLifecycle.removeObserver(any())
        } just Runs
        purchases.close()
        verify(exactly = 1) {
            mockLifecycle.removeObserver(any())
        }
    }

    @Test
    fun `when updating pending purchases, retrieve both INAPPS and SUBS`() {
        mockSuccessfulQueryPurchases(
            queriedSUBS = emptyMap(),
            queriedINAPP = emptyMap(),
            notInCache = emptyList()
        )
        purchases.updatePendingPurchaseQueue()
        verify(exactly = 1) {
            mockBillingAbstract.queryPurchases(appUserId, any(), any())
        }
    }

    @Test
    fun `skip updating pending purchases if autosync is off`() {
        buildPurchases(anonymous = true, autoSync = false)
        purchases.updatePendingPurchaseQueue()
        verify(exactly = 0) {
            mockBillingAbstract.queryPurchases(appUserId, any(), any())
        }
        verify(exactly = 0) {
            mockBackend.postReceiptData(any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `post pending purchases if autosync is on`() {
        buildPurchases(anonymous = true, autoSync = true)
        val purchase = stubGooglePurchase(
            purchaseToken = "token",
            productId = "product",
            purchaseState = Purchase.PurchaseState.PURCHASED
        )
        val activePurchase = purchase.toRevenueCatPurchaseDetails(ProductType.SUBS, null)
        mockSuccessfulQueryPurchases(
            queriedSUBS = mapOf(purchase.purchaseToken.sha1() to activePurchase),
            queriedINAPP = emptyMap(),
            notInCache = listOf(activePurchase)
        )
        val productInfo = mockQueryingSkuDetails("product", ProductType.SUBS, null)

        purchases.updatePendingPurchaseQueue()

        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = "token",
                appUserID = appUserId,
                isRestore = false,
                observerMode = false,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo,
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `when updating pending purchases, if token has not been sent, send it`() {
        val purchase = stubGooglePurchase(
            purchaseToken = "token",
            productId = "product",
            purchaseState = Purchase.PurchaseState.PURCHASED
        )

        val purchaseWrapper = purchase.toRevenueCatPurchaseDetails(ProductType.SUBS, null)
        mockSuccessfulQueryPurchases(
            queriedSUBS = mapOf(purchase.purchaseToken.sha1() to purchaseWrapper),
            queriedINAPP = emptyMap(),
            notInCache = listOf(purchaseWrapper)
        )
        val receiptInfo = mockQueryingSkuDetails("product", ProductType.SUBS, null)

        purchases.updatePendingPurchaseQueue()

        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = "token",
                appUserID = appUserId,
                isRestore = false,
                observerMode = false,
                subscriberAttributes = emptyMap(),
                receiptInfo = receiptInfo,
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `when updating pending purchases, if token has been sent, don't send it`() {
        val token = "1234token"
        val purchase = stubGooglePurchase(
            purchaseToken = "token",
            productId = "product"
        )
        mockSuccessfulQueryPurchases(
            queriedSUBS = mapOf(
                purchase.purchaseToken.sha1() to purchase.toRevenueCatPurchaseDetails(
                    ProductType.SUBS,
                    null
                )
            ),
            queriedINAPP = emptyMap(),
            notInCache = emptyList()
        )
        purchases.updatePendingPurchaseQueue()

        val productInfo = ReceiptInfo(
            productID = "product"
        )
        verify(exactly = 0) {
            mockBackend.postReceiptData(
                purchaseToken = token,
                appUserID = appUserId,
                isRestore = false,
                observerMode = false,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo,
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `when updating pending purchases, if result from querying purchases is not successful skip`() {
        every {
            mockBillingAbstract.queryPurchases(appUserId, any(), captureLambda())
        } answers {
            lambda<(PurchasesError) -> Unit>()
                .captured(PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken"))
        }

        purchases.updatePendingPurchaseQueue()
        verify(exactly = 0) {
            mockCache.getPreviouslySentHashedTokens()
        }
    }

    @Test
    fun `on billing wrapper connected, query purchases`() {
        mockSuccessfulQueryPurchases(
            queriedSUBS = emptyMap(),
            queriedINAPP = emptyMap(),
            notInCache = emptyList()
        )
        capturedBillingWrapperStateListener.captured.onConnected()
        verify(exactly = 1) {
            mockBillingAbstract.queryPurchases(appUserId, any(), any())
        }
    }

    @Test
    fun `on app foregrounded query purchases`() {
        mockSuccessfulQueryPurchases(
            queriedSUBS = emptyMap(),
            queriedINAPP = emptyMap(),
            notInCache = emptyList()
        )
        mockSynchronizeSubscriberAttributesForAllUsers()
        purchases.onAppForegrounded()
        verify(exactly = 1) {
            mockBillingAbstract.queryPurchases(appUserId, any(), any())
        }
    }

    @Test
    fun `if billing client not connected do not query purchases`() {
        every {
            mockBillingAbstract.isConnected()
        } returns false
        purchases.updatePendingPurchaseQueue()
        verify(exactly = 0) {
            mockBillingAbstract.queryPurchases(ProductType.SUBS.toSKUType()!!, any(), any())
        }
        verify(exactly = 0) {
            mockBillingAbstract.queryPurchases(ProductType.INAPP.toSKUType()!!, any(), any())
        }
    }

    @Test
    fun `posted inapps also post currency and price`() {
        val sku = "sku"
        val purchaseToken = "token"

        val productInfo = mockQueryingSkuDetails(sku, ProductType.INAPP, offeringIdentifier = "offering_a")

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(
                sku,
                purchaseToken,
                ProductType.INAPP,
                "offering_a"
            )
        )

        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserId,
                isRestore = false,
                observerMode = false,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo,
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `posted subs post currency and price`() {
        val skuSub = "sub"
        val purchaseTokenSub = "token_sub"

        mockProductDetails(listOf(skuSub), emptyList(), ProductType.SUBS)

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(
                skuSub,
                purchaseTokenSub,
                ProductType.SUBS,
                "offering_a"
            )
        )
        val productInfo = ReceiptInfo(
            productID = skuSub,
            offeringIdentifier = "offering_a"
        )
        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = purchaseTokenSub,
                appUserID = appUserId,
                isRestore = false,
                observerMode = false,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo,
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }

        verify(exactly = 1) {
            mockBillingAbstract.querySkuDetailsAsync(
                ProductType.SUBS,
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `invalidate purchaser info caches`() {
        Purchases.sharedInstance.invalidatePurchaserInfoCache()
        verify(exactly = 1) {
            mockCache.clearPurchaserInfoCache(appUserId)
        }
    }

    @Test
    fun `error when fetching purchaser info`() {
        mockBackend(PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken"))
        every {
            mockCache.getCachedPurchaserInfo(any())
        } returns null
        val lock = CountDownLatch(1)
        purchases.getPurchaserInfoWith(onSuccess = {
            fail("supposed to be error")
        }, onError = {
            lock.countDown()
        })
        lock.await(200, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isZero()
        verify(exactly = 1) { mockCache.clearPurchaserInfoCacheTimestamp(appUserId) }
    }

    @Test
    fun `product not found when querying sku details while purchasing`() {
        val sku = "sku"
        val purchaseToken = "token"

        mockProductDetails(listOf(sku), emptyList(), ProductType.INAPP)

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(
                sku,
                purchaseToken,
                ProductType.INAPP,
                "offering_a"
            )
        )
        val productInfo = ReceiptInfo(
            productID = sku,
            offeringIdentifier = "offering_a"
        )
        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserId,
                isRestore = false,
                observerMode = false,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo,
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `Setting platform info sets it in the AppConfig when configuring the SDK`() {
        val expected = PlatformInfo("flavor", "version")
        Purchases.platformInfo = expected
        Purchases.configure(mockContext, "api")
        assertThat(Purchases.sharedInstance.appConfig.platformInfo).isEqualTo(expected)
    }

    @Test
    fun `Setting proxy URL info sets it in the HttpClient when configuring the SDK`() {
        val expected = URL("https://a-proxy.com")
        Purchases.proxyURL = expected
        Purchases.configure(mockContext, "api")
        assertThat(Purchases.sharedInstance.appConfig.baseURL).isEqualTo(expected)
    }

    @Test
    fun `Setting observer mode on sets finish transactions to false`() {
        Purchases.configure(mockContext, "api", observerMode = true)
        assertThat(Purchases.sharedInstance.appConfig.finishTransactions).isFalse()
    }

    @Test
    fun `Setting observer mode off sets finish transactions to true`() {
        Purchases.configure(mockContext, "api", observerMode = false)
        assertThat(Purchases.sharedInstance.appConfig.finishTransactions).isTrue()
    }

    @Test
    fun `Send error if cannot find the old purchase associated when upgrading a SKU`() {
        val (skuDetails, offerings) = stubOfferings("onemonth_freetrial")

        val message = PurchaseStrings.NO_EXISTING_PURCHASE
        val error = PurchasesError(PurchasesErrorCode.PurchaseInvalidError, message)

        val oldPurchase = mockPurchaseFound(error)

        var receivedError: PurchasesError? = null
        var receivedUserCancelled: Boolean? = null
        purchases.purchasePackageWith(
            mockActivity,
            offerings[stubOfferingIdentifier]!!.monthly!!,
            UpgradeInfo(oldPurchase.sku),
            onError = { error, userCancelled ->
                receivedError = error
                receivedUserCancelled = userCancelled
            },
            onSuccess = { _, _ -> }
        )

        verify(exactly = 0) {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                skuDetails,
                ReplaceSkuInfo(oldPurchase),
                stubOfferingIdentifier
            )
        }
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
        assertThat(receivedUserCancelled).isFalse()
    }

    @Test
    fun `Send error if cannot find the old purchase associated when upgrading a SKU due to a billingclient error`() {
        val (skuDetails, offerings) = stubOfferings("onemonth_freetrial")

        val stubBillingResult = mockk<BillingResult>()
        every { stubBillingResult.responseCode } returns BillingClient.BillingResponseCode.ERROR

        val underlyingErrorMessage = PurchaseStrings.ERROR_FINDING_PURCHASE.format("oldSku")
        val error =
            stubBillingResult.responseCode.billingResponseToPurchasesError(underlyingErrorMessage)

        val oldPurchase = mockPurchaseFound(error)

        every {
            mockBillingAbstract.findPurchaseInPurchaseHistory(
                appUserId,
                ProductType.SUBS,
                "oldSku",
                any(),
                captureLambda()
            )
        } answers {
            val underlyingErrorMessage = PurchaseStrings.ERROR_FINDING_PURCHASE.format("oldSku")
            val error =
                stubBillingResult.responseCode.billingResponseToPurchasesError(underlyingErrorMessage)
            lambda<(PurchasesError) -> Unit>().captured.invoke(error)
        }

        var receivedError: PurchasesError? = null
        var receivedUserCancelled: Boolean? = null
        purchases.purchasePackageWith(
            mockActivity,
            offerings[stubOfferingIdentifier]!!.monthly!!,
            UpgradeInfo(oldPurchase.sku),
            onError = { error, userCancelled ->
                receivedError = error
                receivedUserCancelled = userCancelled
            },
            onSuccess = { _, _ -> }
        )

        verify(exactly = 0) {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                skuDetails,
                ReplaceSkuInfo(oldPurchase),
                stubOfferingIdentifier
            )
        }
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedUserCancelled).isFalse()
    }

    @Test
    fun `Deferred downgrade`() {
        val (_, offerings) = stubOfferings("onemonth_freetrial")

        val oldPurchase = mockk<PurchaseDetails>()
        every { oldPurchase.sku } returns "oldSku"
        every { oldPurchase.type } returns ProductType.SUBS

        every {
            mockBillingAbstract.findPurchaseInPurchaseHistory(
                appUserId,
                ProductType.SUBS,
                "oldSku",
                captureLambda(),
                any()
            )
        } answers {
            lambda<(PurchaseDetails) -> Unit>().captured.invoke(oldPurchase)
        }

        purchases.purchasePackageWith(
            mockActivity,
            offerings[stubOfferingIdentifier]!!.monthly!!,
            UpgradeInfo(oldPurchase.sku)
        ) { _, _ -> }

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(emptyList())
    }

    // region Private Methods
    private fun mockBillingWrapper() {
        with(mockBillingAbstract) {
            every {
                makePurchaseAsync(any(), any(), any(), any(), any())
            } just Runs
            every {
                purchasesUpdatedListener = capture(capturedPurchasesUpdatedListener)
            } just Runs
            every {
                consumeAndSave(capture(capturedShouldTryToConsume), capture(capturedConsumePurchaseWrapper))
            } just Runs
            every {
                purchasesUpdatedListener = null
            } just Runs
            every {
                stateListener = capture(capturedBillingWrapperStateListener)
            } just Runs
            every {
                isConnected()
            } returns true
        }
    }

    @Test
    fun `state appInBackground is updated when app foregrounded`() {

        mockSuccessfulQueryPurchases(
            queriedSUBS = emptyMap(),
            queriedINAPP = emptyMap(),
            notInCache = emptyList()
        )
        purchases.state = purchases.state.copy(appInBackground = true)
        Purchases.sharedInstance.onAppForegrounded()
        assertThat(purchases.state.appInBackground).isFalse()
    }

    @Test
    fun `state appInBackground is updated when app backgrounded`() {

        purchases.state = purchases.state.copy(appInBackground = false)
        Purchases.sharedInstance.onAppBackgrounded()
        assertThat(purchases.state.appInBackground).isTrue()
    }

    @Test
    fun `force update of caches when app foregrounded for the first time`() {

        mockSuccessfulQueryPurchases(
            queriedSUBS = emptyMap(),
            queriedINAPP = emptyMap(),
            notInCache = emptyList()
        )
        purchases.state = purchases.state.copy(appInBackground = false, firstTimeInForeground = true)
        Purchases.sharedInstance.onAppForegrounded()
        assertThat(purchases.state.firstTimeInForeground).isFalse()
        verify(exactly = 1) {
            mockBackend.getPurchaserInfo(appUserId, appInBackground = false, onSuccess = any(), onError = any())
        }
        verify(exactly = 0) {
            mockCache.isPurchaserInfoCacheStale(appInBackground = false, appUserID = appUserId)
        }
    }

    @Test
    fun `don't force update of caches when app foregrounded not for the first time`() {
        mockSuccessfulQueryPurchases(
            queriedSUBS = emptyMap(),
            queriedINAPP = emptyMap(),
            notInCache = emptyList()
        )
        every {
            mockCache.isPurchaserInfoCacheStale(appInBackground = false, appUserID = appUserId)
        } returns false
        purchases.state = purchases.state.copy(appInBackground = false, firstTimeInForeground = false)
        Purchases.sharedInstance.onAppForegrounded()
        assertThat(purchases.state.firstTimeInForeground).isFalse()
        verify(exactly = 0) {
            mockBackend.getPurchaserInfo(appUserId, appInBackground = false, onSuccess = any(), onError = any())
        }
        verify(exactly = 1) {
            mockCache.isPurchaserInfoCacheStale(appInBackground = false, appUserID = appUserId)
        }
    }

    @Test
    fun `update of caches when app foregrounded not for the first time and caches stale`() {
        mockSuccessfulQueryPurchases(
            queriedSUBS = emptyMap(),
            queriedINAPP = emptyMap(),
            notInCache = emptyList()
        )
        every {
            mockCache.isPurchaserInfoCacheStale(appInBackground = false, appUserID = appUserId)
        } returns true
        purchases.state = purchases.state.copy(appInBackground = false, firstTimeInForeground = false)
        Purchases.sharedInstance.onAppForegrounded()
        assertThat(purchases.state.firstTimeInForeground).isFalse()
        verify(exactly = 1) {
            mockBackend.getPurchaserInfo(appUserId, appInBackground = false, onSuccess = any(), onError = any())
        }
        verify(exactly = 1) {
            mockCache.isPurchaserInfoCacheStale(appInBackground = false, appUserID = appUserId)
        }
    }

    @Test
    fun `when making purchase with upgrade info and old method, completion block is called`() {
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        val receiptInfo = mockQueryingSkuDetails(sku, ProductType.SUBS, null)

        val oldPurchase = mockk<PurchaseDetails>()
        every { oldPurchase.sku } returns "oldSku"
        every { oldPurchase.type } returns ProductType.SUBS

        every {
            mockBillingAbstract.findPurchaseInPurchaseHistory(
                appUserId,
                ProductType.SUBS,
                "oldSku",
                captureLambda(),
                any()
            )
        } answers {
            lambda<(PurchaseDetails) -> Unit>().captured.invoke(oldPurchase)
        }

        var callCount = 0

        purchases.purchaseProduct(
            mockActivity,
            receiptInfo.productDetails!!.skuDetails,
            UpgradeInfo(oldPurchase.sku),
            object : MakePurchaseListener {
                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                    fail("should be successful")
                }

                override fun onCompleted(purchase: Purchase, purchaserInfo: PurchaserInfo) {
                    callCount++
                }
            }
        )
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, ProductType.SUBS)
        )
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun `when making purchase with upgrade info and old method, error is called if product change is deferred`() {
        val sku = "onemonth_freetrial"

        val receiptInfo = mockQueryingSkuDetails(sku, ProductType.SUBS, null)

        val oldPurchase = mockPurchaseFound()

        var callCount = 0

        purchases.purchaseProduct(
            mockActivity,
            receiptInfo.productDetails!!.skuDetails,
            UpgradeInfo(oldPurchase.sku),
            object : MakePurchaseListener {
                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                    callCount++
                    assertThat(error.code).isEqualTo(PurchasesErrorCode.PaymentPendingError)
                    assertThat(userCancelled).isFalse()
                }

                override fun onCompleted(purchase: Purchase, purchaserInfo: PurchaserInfo) {
                    fail("should be error")
                }
            }
        )
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(emptyList())
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun `when making purchase with upgrade info and old method, error is forwarded`() {
        val sku = "onemonth_freetrial"

        val receiptInfo = mockQueryingSkuDetails(sku, ProductType.SUBS, null)

        val stubBillingResult = mockk<BillingResult>()
        every { stubBillingResult.responseCode } returns BillingClient.BillingResponseCode.ERROR

        val underlyingErrorMessage = PurchaseStrings.ERROR_FINDING_PURCHASE.format(sku)
        val error =
            stubBillingResult.responseCode.billingResponseToPurchasesError(underlyingErrorMessage)

        val oldPurchase = mockPurchaseFound(error)

        var receivedError: PurchasesError? = null
        var receivedUserCancelled: Boolean? = null
        purchases.purchaseProduct(
            mockActivity,
            receiptInfo.productDetails!!.skuDetails,
            UpgradeInfo(oldPurchase.sku),
            object : MakePurchaseListener {
                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                    receivedError = error
                    receivedUserCancelled = userCancelled
                }

                override fun onCompleted(purchase: Purchase, purchaserInfo: PurchaserInfo) {
                    fail("should be error")
                }
            }
        )
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedUserCancelled).isFalse()
    }

    @Test
    fun `when purchasing a package with upgrade info and old method, completion block is called`() {
        val sku = "onemonth_freetrial"

        val (_, offerings) = stubOfferings(sku)
        mockQueryingSkuDetails(sku, ProductType.SUBS, null)

        val purchaseToken = "crazy_purchase_token"

        val oldPurchase = mockPurchaseFound()

        var callCount = 0

        purchases.purchasePackage(
            mockActivity,
            offerings[stubOfferingIdentifier]!!.monthly!!,
            UpgradeInfo(oldPurchase.sku),
            object : MakePurchaseListener {
                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                    fail("should be successful")
                }

                override fun onCompleted(purchase: Purchase, purchaserInfo: PurchaserInfo) {
                    callCount++
                }
            }
        )
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(
                offerings[stubOfferingIdentifier]!!.monthly!!.product.sku,
                purchaseToken,
                ProductType.SUBS
            )
        )
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun `when purchasing a package with upgrade info and old method, error is called if product change is deferred`() {
        val sku = "onemonth_freetrial"
        val (_, offerings) = stubOfferings(sku)

        val oldPurchase = mockPurchaseFound()

        var callCount = 0

        purchases.purchasePackage(
            mockActivity,
            offerings[stubOfferingIdentifier]!!.monthly!!,
            UpgradeInfo(oldPurchase.sku),
            object : MakePurchaseListener {
                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                    callCount++
                    assertThat(error.code).isEqualTo(PurchasesErrorCode.PaymentPendingError)
                    assertThat(userCancelled).isFalse()
                }

                override fun onCompleted(purchase: Purchase, purchaserInfo: PurchaserInfo) {
                    fail("should be error")
                }
            }
        )
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(emptyList())
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun `when purchasing a package with upgrade info and old method, error is forwarded`() {
        val sku = "onemonth_freetrial"
        val (_, offerings) = stubOfferings(sku)

        val stubBillingResult = mockk<BillingResult>()
        every { stubBillingResult.responseCode } returns BillingClient.BillingResponseCode.ERROR

        val underlyingErrorMessage = PurchaseStrings.ERROR_FINDING_PURCHASE.format("oldSku")
        val error =
            stubBillingResult.responseCode.billingResponseToPurchasesError(underlyingErrorMessage)

        val oldPurchase = mockPurchaseFound(error)

        var receivedError: PurchasesError? = null
        var receivedUserCancelled: Boolean? = null
        purchases.purchasePackage(
            mockActivity,
            offerings[stubOfferingIdentifier]!!.monthly!!,
            UpgradeInfo(oldPurchase.sku),
            object : MakePurchaseListener {
                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                    receivedError = error
                    receivedUserCancelled = userCancelled
                }

                override fun onCompleted(purchase: Purchase, purchaserInfo: PurchaserInfo) {
                    fail("should be error")
                }
            }
        )
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedUserCancelled).isFalse()
    }

    @Test
    fun `when making purchase with upgrade info, completion block is called`() {
        val sku = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        val receiptInfo = mockQueryingSkuDetails(sku, ProductType.SUBS, null)

        val oldPurchase = mockPurchaseFound()

        var callCount = 0

        purchases.purchaseProductWith(
            mockActivity,
            receiptInfo.productDetails!!,
            UpgradeInfo(oldPurchase.sku),
            onError = { _, _ ->
                fail("should be successful")
            }, onSuccess = { _, _ ->
                callCount++
            })

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(sku, purchaseToken, ProductType.SUBS)
        )
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun `when making a deferred upgrade, completion is called with null purchase`() {
        val sku = "onemonth_freetrial"

        val receiptInfo = mockQueryingSkuDetails(sku, ProductType.SUBS, null)

        val oldPurchase = mockPurchaseFound()

        var callCount = 0
        purchases.purchaseProductWith(
            mockActivity,
            receiptInfo.productDetails!!,
            UpgradeInfo(oldPurchase.sku),
            onError = { error, userCancelled ->
                fail("should be success")
            }, onSuccess = { purchase, _ ->
                callCount++
                assertThat(purchase).isNull()
            })

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(emptyList())
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun `when making purchase with upgrade info, error is forwarded`() {
        val sku = "onemonth_freetrial"

        val receiptInfo = mockQueryingSkuDetails(sku, ProductType.SUBS, null)

        val stubBillingResult = mockk<BillingResult>()
        every { stubBillingResult.responseCode } returns BillingClient.BillingResponseCode.ERROR

        val underlyingErrorMessage = PurchaseStrings.ERROR_FINDING_PURCHASE.format("oldSku")
        val error =
            stubBillingResult.responseCode.billingResponseToPurchasesError(underlyingErrorMessage)

        val oldPurchase = mockPurchaseFound(error)

        var receivedError: PurchasesError? = null
        var receivedUserCancelled: Boolean? = null

        purchases.purchaseProductWith(
            mockActivity,
            receiptInfo.productDetails!!,
            UpgradeInfo(oldPurchase.sku),
            onError = { error, userCancelled ->
                receivedError = error
                receivedUserCancelled = userCancelled
            }, onSuccess = { _, _ ->
                fail("should be error")
            })

        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedUserCancelled).isFalse()
    }

    @Test
    fun `when making purchase with upgrade info, failures purchasing are forwarded`() {
        val sku = "onemonth_freetrial"

        val receiptInfo = mockQueryingSkuDetails(sku, ProductType.SUBS, null)

        val oldPurchase = mockPurchaseFound()

        var receivedError: PurchasesError? = null
        var receivedUserCancelled: Boolean? = null

        purchases.purchaseProductWith(
            mockActivity,
            receiptInfo.productDetails!!,
            UpgradeInfo(oldPurchase.sku),
            onError = { error, userCancelled ->
                receivedError = error
                receivedUserCancelled = userCancelled
            }, onSuccess = { _, _ ->
                fail("should be error")
            })

        val purchase = mockk<Purchase>(relaxed = true)
        every { purchase.sku } returns sku

        val error = PurchasesError(PurchasesErrorCode.StoreProblemError)
        capturedPurchasesUpdatedListener.captured.onPurchasesFailedToUpdate(error)


        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedUserCancelled).isFalse()
    }

    @Test
    fun `when purchasing a package with upgrade info, completion block is called`() {
        val sku = "onemonth_freetrial"

        val (_, offerings) = stubOfferings(sku)
        mockQueryingSkuDetails(sku, ProductType.SUBS, null)

        val purchaseToken = "crazy_purchase_token"

        val oldPurchase = mockPurchaseFound()

        var callCount = 0

        purchases.purchasePackageWith(
            mockActivity,
            offerings[stubOfferingIdentifier]!!.monthly!!,
            UpgradeInfo(oldPurchase.sku),
            onError = { _, _ ->
                fail("should be successful")
            }, onSuccess = { _, _ ->
                callCount++
            }
        )
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(
                offerings[stubOfferingIdentifier]!!.monthly!!.product.sku,
                purchaseToken,
                ProductType.SUBS
            )
        )
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun `when purchasing a package with upgrade info, completion is called with null purchase if product change is deferred`() {
        val sku = "onemonth_freetrial"
        val (_, offerings) = stubOfferings(sku)

        val oldPurchase = mockPurchaseFound()

        var callCount = 0

        purchases.purchasePackageWith(
            mockActivity,
            offerings[stubOfferingIdentifier]!!.monthly!!,
            UpgradeInfo(oldPurchase.sku),
            onError = { error, userCancelled ->
                fail("should be success")
            }, onSuccess = { purchase, _ ->
                callCount++
                assertThat(purchase).isNull()
            }
        )
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(emptyList())
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun `when purchasing a package with upgrade info, error is forwarded`() {
        val sku = "onemonth_freetrial"
        val (_, offerings) = stubOfferings(sku)

        mockQueryingSkuDetails(sku, ProductType.SUBS, null)

        val stubBillingResult = mockk<BillingResult>()
        every { stubBillingResult.responseCode } returns BillingClient.BillingResponseCode.ERROR

        val underlyingErrorMessage = PurchaseStrings.ERROR_FINDING_PURCHASE.format("oldSku")
        val error =
            stubBillingResult.responseCode.billingResponseToPurchasesError(underlyingErrorMessage)

        val oldPurchase = mockPurchaseFound(error)

        var receivedError: PurchasesError? = null
        var receivedUserCancelled: Boolean? = null
        purchases.purchasePackageWith(
            mockActivity,
            offerings[stubOfferingIdentifier]!!.monthly!!,
            UpgradeInfo(oldPurchase.sku),
            onError = { error, userCancelled ->
                receivedError = error
                receivedUserCancelled = userCancelled
            }, onSuccess = { _, _ ->
                fail("should be error")
            }
        )
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedUserCancelled).isFalse()
    }

    @Test
    fun `when purchasing a package with upgrade info, failures purchasing are forwarded`() {
        val sku = "onemonth_freetrial"
        val (_, offerings) = stubOfferings(sku)

        val oldPurchase = mockPurchaseFound()

        var receivedError: PurchasesError? = null
        var receivedUserCancelled: Boolean? = null
        purchases.purchasePackageWith(
            mockActivity,
            offerings[stubOfferingIdentifier]!!.monthly!!,
            UpgradeInfo(oldPurchase.sku),
            onError = { error, userCancelled ->
                receivedError = error
                receivedUserCancelled = userCancelled
            }, onSuccess = { _, _ ->
                fail("should be error")
            }
        )

        val purchase = mockk<Purchase>(relaxed = true)
        every { purchase.sku } returns sku

        val error = PurchasesError(PurchasesErrorCode.StoreProblemError)
        capturedPurchasesUpdatedListener.captured.onPurchasesFailedToUpdate(error)

        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedUserCancelled).isFalse()
    }

    private fun mockBackend(errorGettingPurchaserInfo: PurchasesError? = null) {
        with(mockBackend) {
            if (errorGettingPurchaserInfo != null) {
                every {
                    getPurchaserInfo(any(), any(), any(), captureLambda())
                } answers {
                    lambda<(PurchasesError) -> Unit>().captured.invoke(errorGettingPurchaserInfo)
                }
            } else {
                every {
                    getPurchaserInfo(any(), any(), captureLambda(), any())
                } answers {
                    lambda<(PurchaserInfo) -> Unit>().captured.invoke(mockInfo)
                }
            }
            mockProducts()
            every {
                postReceiptData(
                    purchaseToken = any(),
                    appUserID = any(),
                    isRestore = any(),
                    observerMode = any(),
                    subscriberAttributes = any(),
                    receiptInfo = any(),
                    storeAppUserID = any(),
                    onSuccess = captureLambda(),
                    onError = any()
                )
            } answers {
                lambda<PostReceiptDataSuccessCallback>().captured.invoke(
                    mockInfo,
                    JSONObject(Responses.validFullPurchaserResponse)
                )
            }
            every {
                close()
            } just Runs
            every {
                clearCaches()
            } just Runs
        }
    }

    private fun mockCache() {
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
                getCachedAttributionData(CommonAttributionNetwork.APPSFLYER, appUserId)
            } returns null
            every {
                setPurchaserInfoCacheTimestampToNow(appUserId)
            } just Runs
            every {
                setOfferingsCacheTimestampToNow()
            } just Runs
            every {
                clearPurchaserInfoCacheTimestamp(appUserId)
            } just Runs
            every {
                clearPurchaserInfoCache(appUserId)
            } just Runs
            every {
                clearOfferingsCacheTimestamp()
            } just Runs
            every {
                isPurchaserInfoCacheStale(appUserId, any())
            } returns false
            every {
                isOfferingsCacheStale(any())
            } returns false
            every {
                addSuccessfullyPostedToken(any())
            } just Runs
            every {
                mockCache.cacheOfferings(any())
            } just Runs
        }
    }

    private fun mockProducts() {
        every {
            mockBackend.getOfferings(any(), any(), captureLambda(), any())
        } answers {
            lambda<(JSONObject) -> Unit>().captured.invoke(JSONObject(oneOfferingsResponse))
        }
    }

    private fun mockProductDetails(
        skus: List<String>,
        skusSuccessfullyFetched: List<String>,
        type: ProductType
    ): List<ProductDetails> {
        val productDetails = skusSuccessfullyFetched.map { sku ->
            stubSkuDetails(sku, type.toSKUType()!!).toProductDetails()
        }

        every {
            mockBillingAbstract.querySkuDetailsAsync(
                type,
                skus.toSet(),
                captureLambda(),
                any()
            )
        } answers {
            lambda<(List<ProductDetails>) -> Unit>().captured.invoke(productDetails)
        }
        return productDetails
    }

    private fun mockCloseActions() {
        every {
            ProcessLifecycleOwner.get()
        } returns mockLifecycleOwner
        every {
            mockLifecycleOwner.lifecycle
        } returns mockLifecycle
        every {
            mockLifecycle.removeObserver(any())
        } just Runs
        every {
            mockBackend.close()
        } just Runs
        every {
            mockBillingAbstract.purchasesUpdatedListener = null
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
                CommonAttributionNetwork.APPSFLYER,
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
        verify(exactly = 1) {
            mockLifecycle.removeObserver(any())
        }
        verifyOrder {
            mockBillingAbstract.purchasesUpdatedListener = capturedPurchasesUpdatedListener.captured
            mockBillingAbstract.purchasesUpdatedListener = null
        }
    }

    private fun getMockedPurchaseHistoryList(
        sku: String,
        purchaseToken: String,
        productType: ProductType
    ): List<PurchaseDetails> {
        val purchaseHistoryRecordWrapper =
            getMockedPurchaseDetails(sku, purchaseToken, productType)
        return listOf(purchaseHistoryRecordWrapper)
    }

    private fun getMockedPurchaseDetails(
        sku: String,
        purchaseToken: String,
        productType: ProductType
    ): PurchaseDetails {
        val p: PurchaseHistoryRecord = stubPurchaseHistoryRecord(
            productId = sku,
            purchaseToken = purchaseToken
        )

        return p.toRevenueCatPurchaseDetails(productType)
    }

    private fun getMockedPurchaseList(
        sku: String,
        purchaseToken: String,
        productType: ProductType,
        offeringIdentifier: String? = null,
        purchaseState: Int = Purchase.PurchaseState.PURCHASED,
        acknowledged: Boolean = false
    ): List<PurchaseDetails> {
        val p = stubGooglePurchase(
            productId = sku,
            purchaseToken = purchaseToken,
            purchaseState = purchaseState,
            acknowledged = acknowledged
        )

        return listOf(p.toRevenueCatPurchaseDetails(productType, offeringIdentifier))
    }

    private fun mockSuccessfulQueryPurchases(
        queriedSUBS: Map<String, PurchaseDetails>,
        queriedINAPP: Map<String, PurchaseDetails>,
        notInCache: List<PurchaseDetails>
    ) {
        val purchasesByHashedToken = queriedSUBS + queriedINAPP
        every {
            mockCache.cleanPreviouslySentTokens(purchasesByHashedToken.keys)
        } just Runs
        every {
            mockCache.getActivePurchasesNotInCache(purchasesByHashedToken)
        } returns notInCache

        every {
            mockBillingAbstract.queryPurchases(appUserId, captureLambda(), any())
        } answers {
            lambda<(Map<String, PurchaseDetails>) -> Unit>().captured(purchasesByHashedToken)
        }
    }

    private fun buildPurchases(anonymous: Boolean, autoSync: Boolean = true) {
        purchases = Purchases(
            mockApplication,
            if (anonymous) null else appUserId,
            mockBackend,
            mockBillingAbstract,
            mockCache,
            dispatcher = SyncDispatcher(),
            identityManager = mockIdentityManager,
            subscriberAttributesManager = mockSubscriberAttributesManager,
            appConfig = AppConfig(
                context = mockContext,
                observerMode = false,
                platformInfo = PlatformInfo("native", "3.2.0"),
                proxyURL = null,
                store = Store.PLAY_STORE,
                dangerousSettings = DangerousSettings(autoSyncPurchases = autoSync)
            )
        )
        Purchases.sharedInstance = purchases
        purchases.state = purchases.state.copy(appInBackground = false)
    }

    private fun Int.buildResult(): BillingResult {
        return BillingResult.newBuilder().setResponseCode(this).build()
    }

    private fun mockPostReceiptError(
        sku: String,
        purchaseToken: String,
        observerMode: Boolean,
        offeringIdentifier: String?,
        type: ProductType,
        answer: MockKAnswerScope<Unit, Unit>.(Call) -> Unit,
        isRestore: Boolean = false
    ) {
        val receiptInfo = mockQueryingSkuDetails(sku, type, offeringIdentifier)

        every {
            mockBackend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserId,
                isRestore = isRestore,
                observerMode = observerMode,
                subscriberAttributes = emptyMap(),
                receiptInfo = if (isRestore) ReceiptInfo(productID = sku) else receiptInfo,
                storeAppUserID = null,
                onSuccess = any(),
                onError = captureLambda()
            )
        } answers answer
    }

    private fun mockPostReceipt(
        sku: String,
        purchaseToken: String,
        observerMode: Boolean,
        mockInfo: PurchaserInfo,
        offeringIdentifier: String?,
        type: ProductType,
        restore: Boolean = false
    ): ReceiptInfo {
        val receiptInfo = mockQueryingSkuDetails(sku, type, offeringIdentifier)

        every {
            mockBackend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserId,
                isRestore = restore,
                observerMode = observerMode,
                subscriberAttributes = emptyMap(),
                receiptInfo = if (restore) ReceiptInfo(productID = sku) else receiptInfo,
                storeAppUserID = null,
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<PostReceiptDataSuccessCallback>().captured.invoke(
                mockInfo,
                JSONObject(Responses.validFullPurchaserResponse)
            )
        }

        return receiptInfo
    }

    private fun mockQueryingSkuDetails(
        sku: String,
        type: ProductType,
        offeringIdentifier: String?
    ): ReceiptInfo {
        val skuDetails = stubSkuDetails(
            productId = sku,
            price = 2.00,
            subscriptionPeriod = if (type == ProductType.SUBS) "P1M" else "",
            introductoryPricePeriod = if (type == ProductType.SUBS) "P7D" else null,
            freeTrialPeriod = if (type == ProductType.SUBS) "P7D" else null
        )

        val productInfo = ReceiptInfo(
            productID = sku,
            offeringIdentifier = offeringIdentifier,
            productDetails = skuDetails.toProductDetails()
        )

        every {
            mockBillingAbstract.querySkuDetailsAsync(
                type,
                setOf(sku),
                captureLambda(),
                any()
            )
        } answers {
            lambda<(List<ProductDetails>) -> Unit>().captured.invoke(listOf(skuDetails.toProductDetails()))
        }

        return productInfo
    }

    private fun mockCacheStale(
        purchaserInfoStale: Boolean = false,
        offeringsStale: Boolean = false,
        appInBackground: Boolean = false
    ) {
        every {
            mockCache.isPurchaserInfoCacheStale(appUserId, appInBackground)
        } returns purchaserInfoStale
        every {
            mockCache.isOfferingsCacheStale(appInBackground)
        } returns offeringsStale
    }

    private fun mockSubscriberAttributesManager(userIdToUse: String) {
        every {
            mockSubscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(appUserId)
        } just Runs
        every {
            mockSubscriberAttributesManager.getUnsyncedSubscriberAttributes(userIdToUse)
        } returns emptyMap()
        every {
            mockSubscriberAttributesManager.markAsSynced(userIdToUse, any(), any())
        } just runs
    }

    private fun PostReceiptDataErrorCallback.invokeWithFinishableError() {
        invoke(
            PurchasesError(PurchasesErrorCode.InvalidCredentialsError),
            true,
            JSONObject(Responses.invalidCredentialsErrorResponse)
        )
    }

    private fun PostReceiptDataErrorCallback.invokeWithNotFinishableError() {
        invoke(
            PurchasesError(PurchasesErrorCode.UnexpectedBackendResponseError),
            false,
            JSONObject(Responses.internalServerErrorResponse)
        )
    }

    private fun mockPurchaseFound(error: PurchasesError? = null): PurchaseDetails {
        val oldPurchase = getMockedPurchaseDetails(
            sku = "oldSku",
            purchaseToken = "another_purchase_token",
            productType = ProductType.SUBS
        )

        every {
            mockBillingAbstract.findPurchaseInPurchaseHistory(
                appUserId,
                ProductType.SUBS,
                "oldSku",
                if (error == null) captureLambda() else any(),
                if (error != null) captureLambda() else any()
            )
        } answers {
            if (error != null) {
                lambda<(PurchasesError) -> Unit>().captured.invoke(error)
            } else {
                lambda<(PurchaseDetails) -> Unit>().captured.invoke(oldPurchase)
            }
        }
        return oldPurchase
    }

    private fun isBillingSupportedSetup(mockLocalBillingClient: BillingClient): CapturingSlot<BillingClientStateListener> {
        mockkStatic(BillingClient::class)
        val mockBuilder = mockk<BillingClient.Builder>(relaxed = true)
        every { BillingClient.newBuilder(any()) } returns mockBuilder
        every { mockBuilder.setListener(any()) } returns mockBuilder
        every { mockBuilder.enablePendingPurchases() } returns mockBuilder
        every { mockBuilder.build() } returns mockLocalBillingClient
        val listener = slot<BillingClientStateListener>()
        every { mockLocalBillingClient.startConnection(capture(listener)) } just Runs
        return listener
    }

// endregion
}
