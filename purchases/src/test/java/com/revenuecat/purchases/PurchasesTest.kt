//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams.ProrationMode
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchaseHistoryRecord
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.PostReceiptDataErrorCallback
import com.revenuecat.purchases.common.PostReceiptDataSuccessCallback
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.common.buildCustomerInfo
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.createOfferings
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.google.billingResponseToPurchasesError
import com.revenuecat.purchases.google.isBasePlan
import com.revenuecat.purchases.google.toGoogleProductType
import com.revenuecat.purchases.google.toStoreProduct
import com.revenuecat.purchases.google.toInAppStoreProduct
import com.revenuecat.purchases.google.toStoreTransaction
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.interfaces.GetStoreProductsCallback
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.BillingFeature
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.OfferingStrings
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.subscriberattributes.SubscriberAttribute
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.subscriberattributes.toBackendMap
import com.revenuecat.purchases.utils.Responses
import com.revenuecat.purchases.utils.SyncDispatcher
import com.revenuecat.purchases.utils.createMockOneTimeProductDetails
import com.revenuecat.purchases.utils.createMockProductDetailsFreeTrial
import com.revenuecat.purchases.utils.stubFreeTrialPricingPhase
import com.revenuecat.purchases.utils.stubGooglePurchase
import com.revenuecat.purchases.utils.stubPricingPhase
import com.revenuecat.purchases.utils.stubPurchaseHistoryRecord
import com.revenuecat.purchases.utils.stubSubscriptionOption
import com.revenuecat.purchases.utils.stubStoreProduct
import io.mockk.Call
import io.mockk.CapturingSlot
import io.mockk.MockKAnswerScope
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.AssertionsForClassTypes
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.net.URL
import java.util.Collections.emptyList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
@Suppress("DEPRECATION")
class PurchasesTest {
    private val mockBillingAbstract: BillingAbstract = mockk()
    private val mockBackend: Backend = mockk()
    private val mockCache: DeviceCache = mockk()
    private val updatedCustomerInfoListener: UpdatedCustomerInfoListener = mockk()
    private val mockApplication = mockk<Application>(relaxed = true)
    private val mockContext = mockk<Context>(relaxed = true).apply {
        every {
            applicationContext
        } returns mockApplication
    }
    private val mockActivity: Activity = mockk()
    private val mockIdentityManager = mockk<IdentityManager>()
    private val mockSubscriberAttributesManager = mockk<SubscriberAttributesManager>()
    private val mockCustomerInfoHelper = mockk<CustomerInfoHelper>()

    private var capturedPurchasesUpdatedListener = slot<BillingAbstract.PurchasesUpdatedListener>()
    private var capturedBillingWrapperStateListener = slot<BillingAbstract.StateListener>()
    private val capturedConsumePurchaseWrapper = slot<StoreTransaction>()
    private val capturedShouldTryToConsume = slot<Boolean>()

    private val randomAppUserId = "\$RCAnonymousID:ff68f26e432648369a713849a9f93b58"
    private val appUserId = "fakeUserID"
    private lateinit var purchases: Purchases
    private var receivedProducts: List<StoreProduct>? = null
    private var receivedOfferings: Offerings? = null
    private val mockInfo = mockk<CustomerInfo>()

    private val stubOfferingIdentifier = "offering_a"
    private val stubProductIdentifier = "monthly_freetrial"
    private val oneOfferingsResponse = "{'offerings': [" +
        "{'identifier': '$stubOfferingIdentifier', " +
        "'description': 'This is the base offering', " +
        "'packages': [" +
        "{'identifier': '\$rc_monthly','platform_product_identifier': '$stubProductIdentifier'," +
        "'platform_product_plan_identifier': 'p1m'}]}]," +
        "'current_offering_id': '$stubOfferingIdentifier'}"
    private val oneOfferingWithNoProductsResponse = "{'offerings': [" +
        "{'identifier': '$stubOfferingIdentifier', " +
        "'description': 'This is the base offering', " +
        "'packages': []}]," +
        "'current_offering_id': '$stubOfferingIdentifier'}"

    private val inAppProductId = "inapp"
    private val inAppPurchaseToken = "token_inapp"
    private val subProductId = "sub"
    private val subPurchaseToken = "token_sub"

    private val mockLifecycle = mockk<Lifecycle>()
    private val mockLifecycleOwner = mockk<LifecycleOwner>()

    @After
    fun tearDown() {
        Purchases.backingFieldSharedInstance = null
        clearMocks(mockCustomerInfoHelper)
    }

    @Before
    fun setup() {
        mockkObject(LockedFeature.SyncPurchases)
        every { LockedFeature.SyncPurchases.isLocked } returns false
        mockkObject(LockedFeature.ObserverMode)
        every { LockedFeature.ObserverMode.isLocked } returns false
        mockkObject(LockedFeature.AmazonStore)
        every { LockedFeature.AmazonStore.isLocked } returns false

        mockkStatic("com.revenuecat.purchases.common.CustomerInfoFactoriesKt")
        mockkStatic("com.revenuecat.purchases.common.OfferingFactoriesKt")
        mockkStatic(ProcessLifecycleOwner::class)

        val productIds = listOf(stubProductIdentifier)
        mockCache()
        mockBackend()
        mockBillingWrapper()
        mockStoreProduct(productIds, productIds, ProductType.SUBS)
        mockCustomerInfoHelper()

        every {
            updatedCustomerInfoListener.onReceived(any())
        } just Runs
        every {
            mockIdentityManager.configure(any())
        } just Runs

        anonymousSetup(false)
    }

    // region disabled features

    @Test
    fun `configure throws exception if using observer mode and observer mode disabled`() {
        every { LockedFeature.ObserverMode.isLocked } returns true

        val configuration = PurchasesConfiguration.Builder(mockContext, "api").apply {
            observerMode = true
        }

        assertThatThrownBy {
            Purchases.configure(configuration.build())
        }.isInstanceOf(FeatureNotSupportedException::class.java)
    }

    @Test
    fun `configure throws exception if using amazon store and amazon disabled`() {
        every { LockedFeature.AmazonStore.isLocked } returns true

        val configuration = PurchasesConfiguration.Builder(mockContext, "api").apply {
            store = Store.AMAZON
        }

        assertThatThrownBy {
            Purchases.configure(configuration.build())
        }.isInstanceOf(FeatureNotSupportedException::class.java)
    }

    @Test
    fun `syncObserverModeAmazonPurchase throws exception if amazon store is disabled`() {
        every { LockedFeature.AmazonStore.isLocked } returns true

        assertThatThrownBy {
            purchases.syncObserverModeAmazonPurchase(
                productID = "",
                receiptID = "",
                amazonUserID = "",
                price = null,
                isoCurrencyCode = null
            )
        }.isInstanceOf(FeatureNotSupportedException::class.java)
    }

    @Test
    fun `syncObserverModeAmazonPurchase throws exception if observer mode feature is disabled`() {
        every { LockedFeature.ObserverMode.isLocked } returns true

        assertThatThrownBy {
            purchases.syncObserverModeAmazonPurchase(
                productID = "",
                receiptID = "",
                amazonUserID = "",
                price = null,
                isoCurrencyCode = null
            )
        }.isInstanceOf(FeatureNotSupportedException::class.java)
    }

    @Test
    fun `syncPurchases throws exception if sync purchases feature is disabled`() {
        every { LockedFeature.SyncPurchases.isLocked } returns true

        assertThatThrownBy {
            purchases.syncPurchases()
        }.isInstanceOf(FeatureNotSupportedException::class.java)
    }

    // endregion

    // region setup

    @Test
    fun canBeCreated() {
        assertThat(purchases).isNotNull
    }

    @Test
    fun canBeSetupWithoutAppUserID() {
        anonymousSetup(true)
        assertThat(purchases).isNotNull

        assertThat(purchases.appUserID).isEqualTo(randomAppUserId)
    }

    @Test
    fun canOverrideAnonMode() {
        purchases.allowSharingPlayStoreAccount = true

        val productId = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        val productInfo = mockQueryingProductDetails(productId, ProductType.SUBS, null)

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(productId, purchaseToken, ProductType.SUBS)
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
    fun `when setting up, and passing a appUserID, user is identified`() {
        assertThat(purchases.allowSharingPlayStoreAccount).isEqualTo(false)
        assertThat(purchases.appUserID).isEqualTo(appUserId)
    }

    @Test
    fun `isConfigured is true if there's an instance set`() {
        assertThat(Purchases.isConfigured).isTrue
    }

    @Test
    fun `isConfigured is false if there's no instance set`() {
        Purchases.backingFieldSharedInstance = null
        assertThat(Purchases.isConfigured).isFalse()
    }

    @Test
    fun `when setting listener, we set customer info helper listener`() {
        purchases.updatedCustomerInfoListener = updatedCustomerInfoListener

        verify(exactly = 1) {
            mockCustomerInfoHelper.updatedCustomerInfoListener = updatedCustomerInfoListener
        }
    }

    @Test
    fun `when setting shared instance and there's already an instance, instance is closed`() {
        mockCloseActions()
        Purchases.sharedInstance = purchases
        verifyClose()
    }

    @Test
    fun `when setting listener for anonymous user, we set customer info helper listener`() {
        anonymousSetup(true)
        purchases.updatedCustomerInfoListener = updatedCustomerInfoListener

        verify(exactly = 1) {
            mockCustomerInfoHelper.updatedCustomerInfoListener = null
        }
        verify(exactly = 1) {
            mockCustomerInfoHelper.updatedCustomerInfoListener = updatedCustomerInfoListener
        }
    }

    @Test
    fun `Setting platform info sets it in the AppConfig when configuring the SDK`() {
        val expected = PlatformInfo("flavor", "version")
        Purchases.platformInfo = expected
        Purchases.configure(PurchasesConfiguration.Builder(mockContext, "api").build())
        assertThat(Purchases.sharedInstance.appConfig.platformInfo).isEqualTo(expected)
    }

    @Test
    fun `Setting proxy URL info sets it in the HttpClient when configuring the SDK`() {
        val expected = URL("https://a-proxy.com")
        Purchases.proxyURL = expected
        Purchases.configure(PurchasesConfiguration.Builder(mockContext, "api").build())
        assertThat(Purchases.sharedInstance.appConfig.baseURL).isEqualTo(expected)
    }

    @Test
    fun `Setting observer mode on sets finish transactions to false`() {
        val builder = PurchasesConfiguration.Builder(mockContext, "api").observerMode(true)
        Purchases.configure(builder.build())
        assertThat(Purchases.sharedInstance.appConfig.finishTransactions).isFalse()
    }

    @Test
    fun `Setting observer mode off sets finish transactions to true`() {
        val builder = PurchasesConfiguration.Builder(mockContext, "api").observerMode(false)
        Purchases.configure(builder.build())
        assertThat(Purchases.sharedInstance.appConfig.finishTransactions).isTrue()
    }

    // endregion

    // region get products

    @Test
    fun getsAllProducts() {
        val subProductIds = listOf("onemonth_freetrial")
        val inappProductIds = listOf("normal_purchase")
        val productIds = subProductIds + inappProductIds

        val subStoreProducts = mockStoreProduct(productIds, subProductIds, ProductType.SUBS)
        val inappStoreProducts = mockStoreProduct(productIds, inappProductIds, ProductType.INAPP)
        val storeProducts = subStoreProducts + inappStoreProducts

        purchases.getProducts(productIds,
            object : GetStoreProductsCallback {
                override fun onReceived(storeProducts: List<StoreProduct>) {
                    receivedProducts = storeProducts
                }

                override fun onError(error: PurchasesError) {
                    fail("shouldn't be error")
                }
            })

        assertThat(receivedProducts).isEqualTo(storeProducts)
        assertThat(receivedProducts?.size).isEqualTo(productIds.size)
    }

    @Test
    fun getsSubscriptionProducts() {
        val productIds = listOf("onemonth_freetrial")

        val subStoreProducts = mockStoreProduct(productIds, productIds, ProductType.SUBS)
        val inappStoreProducts = mockStoreProduct(productIds, listOf(), ProductType.INAPP)
        val storeProducts = subStoreProducts + inappStoreProducts

        purchases.getProducts(productIds,
            object : GetStoreProductsCallback {
                override fun onReceived(storeProducts: List<StoreProduct>) {
                    receivedProducts = storeProducts
                }

                override fun onError(error: PurchasesError) {
                    fail("shouldn't be error")
                }
            })

        assertThat(receivedProducts).isEqualTo(storeProducts)
        assertThat(receivedProducts?.size).isEqualTo(productIds.size)
    }

    @Test
    fun getsNonSubscriptionProducts() {
        val productIds = listOf("normal_purchase")

        val subStoreProducts = mockStoreProduct(productIds, listOf(), ProductType.SUBS)
        val inappStoreProducts = mockStoreProduct(productIds, productIds, ProductType.INAPP)
        val storeProducts = subStoreProducts + inappStoreProducts

        purchases.getProducts(productIds,
            object : GetStoreProductsCallback {
                override fun onReceived(storeProducts: List<StoreProduct>) {
                    receivedProducts = storeProducts
                }

                override fun onError(error: PurchasesError) {
                    fail("shouldn't be error")
                }
            })

        assertThat(receivedProducts).isEqualTo(storeProducts)
        assertThat(receivedProducts?.size).isEqualTo(productIds.size)
    }

    // endregion

    // region purchasing

    @Test
    fun `when making purchase with upgrade info, completion block is called`() {
        val productId = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        val receiptInfo = mockQueryingProductDetails(productId, ProductType.SUBS, null)

        val oldPurchase = mockPurchaseFound()

        var callCount = 0

        purchases.purchaseSubscriptionOptionWith(
            mockActivity,
            receiptInfo.storeProduct!!,
            receiptInfo.storeProduct!!.subscriptionOptions[0],
            UpgradeInfo(oldPurchase.productIds[0]),
            onError = { _, _ ->
                fail("should be successful")
            }, onSuccess = { _, _ ->
                callCount++
            })

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(productId, purchaseToken, ProductType.SUBS)
        )
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun `when making a deferred upgrade, completion is called with null purchase`() {
        val productId = "onemonth_freetrial"

        val receiptInfo = mockQueryingProductDetails(productId, ProductType.SUBS, null)

        val oldPurchase = mockPurchaseFound()

        var callCount = 0
        purchases.purchaseSubscriptionOptionWith(
            mockActivity,
            receiptInfo.storeProduct!!,
            receiptInfo.storeProduct!!.subscriptionOptions[0],
            UpgradeInfo(oldPurchase.productIds[0]),
            onError = { _, _ ->
                fail("should be success")
            }, onSuccess = { purchase, _ ->
                callCount++
                assertThat(purchase).isNull()
            })

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(emptyList())
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun `upgrade defaults to ProrationMode IMMEDIATE_WITHOUT_PRORATION`() {
        val productId = "gold"
        val oldSubId = "oldSubID"
        val receiptInfo = mockQueryingProductDetails(productId, ProductType.SUBS, null)

        val oldTransaction = getMockedStoreTransaction(oldSubId, "token", ProductType.SUBS)
        every {
            mockBillingAbstract.findPurchaseInPurchaseHistory(
                appUserId,
                ProductType.SUBS,
                oldSubId,
                captureLambda(),
                any()
            )
        } answers {
            lambda<(StoreTransaction) -> Unit>().captured.invoke(oldTransaction)
        }

        purchases.purchaseProductWith(
            mockActivity,
            receiptInfo.storeProduct!!,
            UpgradeInfo(oldSubId),
            onError = { _, _ ->
            }, onSuccess = { _, _ ->

            })

        val expectedReplaceProductInfo = ReplaceProductInfo(
            oldTransaction,
            ProrationMode.IMMEDIATE_WITHOUT_PRORATION
        )
        verify {
            mockBillingAbstract.makePurchaseAsync(
                any(),
                any(),
                receiptInfo.storeProduct!!,
                any(),
                expectedReplaceProductInfo,
                any()
            )
        }
    }

    @Test
    fun canMakePurchase() {
        val storeProduct = stubStoreProduct("abc")

        purchases.purchaseSubscriptionOptionWith(
            mockActivity,
            storeProduct,
            storeProduct.subscriptionOptions[0]
        ) { _, _ -> }

        verify {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                storeProduct,
                storeProduct.subscriptionOptions[0],
                null,
                null
            )
        }
    }

    @Test
    fun canMakePurchaseWithoutProvidingOption() {
        val storeProduct = stubStoreProduct("productId")

        purchases.purchaseProductWith(
            mockActivity,
            storeProduct
        ) { _, _ -> }

        verify {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                storeProduct,
                storeProduct.subscriptionOptions[0],
                null,
                null
            )
        }
    }

    @Test
    fun canMakePurchaseOfAPackage() {
        val (storeProduct, offerings) = stubOfferings("onemonth_freetrial")

        purchases.purchasePackageWith(
            mockActivity,
            offerings[stubOfferingIdentifier]!!.monthly!!
        ) { _, _ -> }

        verify {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                storeProduct,
                storeProduct.subscriptionOptions[0],
                null,
                stubOfferingIdentifier
            )
        }
    }

    @Test
    fun canMakePurchaseOfAPackageWithoutProvidingOption() {
        val (storeProduct, offerings) = stubOfferings("onemonth_freetrial")

        purchases.purchasePackageWith(
            mockActivity,
            offerings[stubOfferingIdentifier]!!.monthly!!
        ) { _, _ -> }

        verify {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                storeProduct,
                storeProduct.subscriptionOptions[0],
                null,
                stubOfferingIdentifier
            )
        }
    }

    @Test
    fun canMakePurchaseUpgradeOfAPackage() {
        val (storeProduct, offerings) = stubOfferings("onemonth_freetrial")

        val oldPurchase = mockPurchaseFound()

        purchases.purchasePackageWith(
            mockActivity,
            offerings[stubOfferingIdentifier]!!.monthly!!,
            UpgradeInfo(oldPurchase.productIds[0])
        ) { _, _ -> }

        verify {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                storeProduct,
                storeProduct.subscriptionOptions[0],
                ReplaceProductInfo(oldPurchase, ProrationMode.IMMEDIATE_WITHOUT_PRORATION),
                stubOfferingIdentifier
            )
        }
    }

    @Test
    fun postsSuccessfulPurchasesToBackend() {
        val productInfo = mockPostReceipt(
            inAppProductId,
            inAppPurchaseToken,
            observerMode = false,
            mockInfo = mockInfo,
            offeringIdentifier = null,
            type = ProductType.INAPP
        )
        val productInfo1 = mockPostReceipt(
            subProductId,
            subPurchaseToken,
            observerMode = false,
            mockInfo = mockInfo,
            offeringIdentifier = "offering_a",
            type = ProductType.SUBS
        )
        val mockedInApps = getMockedPurchaseList(inAppProductId, inAppPurchaseToken, ProductType.INAPP)
        val mockedSubs = getMockedPurchaseList(subProductId, subPurchaseToken, ProductType.SUBS, "offering_a")
        val allPurchases = mockedInApps + mockedSubs
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(allPurchases)

        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = inAppPurchaseToken,
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
                purchaseToken = subPurchaseToken,
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
        val receiptInfos = listOf(
            mockQueryingProductDetails(subProductId, ProductType.SUBS, null),
            mockQueryingProductDetails(inAppProductId, ProductType.INAPP, null)
        )

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(inAppProductId, inAppPurchaseToken, ProductType.INAPP) +
                getMockedPurchaseList(subProductId, subPurchaseToken, ProductType.SUBS)
        )

        receiptInfos.forEach {
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
        val storeProduct = stubStoreProduct("productId")
        purchases.purchaseSubscriptionOptionWith(
            mockk(),
            storeProduct,
            storeProduct.subscriptionOptions[0],
            onError = { error, _ ->
                errorCalled = true
                assertThat(error.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
            }, onSuccess = { _, _ -> })

        val error = PurchasesError(PurchasesErrorCode.StoreProblemError)
        capturedPurchasesUpdatedListener.captured.onPurchasesFailedToUpdate(error)
        assertThat(errorCalled).isTrue
    }

    @Test
    fun `when making another purchase for a product for a pending product, error is issued`() {
        purchases.updatedCustomerInfoListener = updatedCustomerInfoListener

        val storeProduct = stubStoreProduct("productId")
        purchases.purchaseSubscriptionOptionWith(
            mockk(),
            storeProduct,
            storeProduct.subscriptionOptions[0],
            onError = { _, _ -> fail("Should be success") }) { _, _ ->
            // First one works
        }

        var errorCalled: PurchasesError? = null
        purchases.purchaseSubscriptionOptionWith(
            mockk(),
            storeProduct,
            storeProduct.subscriptionOptions[0],
            onError = { error, _ ->
                errorCalled = error
            }) { _, _ ->
            fail("Should be error")
        }

        assertThat(errorCalled!!.code).isEqualTo(PurchasesErrorCode.OperationAlreadyInProgressError)
    }

    @Test
    fun `when making purchase, completion block is called once`() {
        val productId = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        mockQueryingProductDetails(productId, ProductType.SUBS, null)

        val storeProduct = stubStoreProduct(productId)

        var callCount = 0
        purchases.purchaseSubscriptionOptionWith(
            mockActivity,
            storeProduct,
            storeProduct.subscriptionOptions[0],
            onSuccess = { _, _ ->
                callCount++
            }, onError = { _, _ -> fail("should be successful") })

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(productId, purchaseToken, ProductType.SUBS)
        )
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(productId, purchaseToken, ProductType.SUBS)
        )

        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun `when making purchase, completion block not called for different products`() {
        val productId = "onemonth_freetrial"
        val productId1 = "onemonth_freetrial_1"
        val purchaseToken1 = "crazy_purchase_token_1"
        var callCount = 0
        mockQueryingProductDetails(productId1, ProductType.SUBS, null)
        val storeProduct = stubStoreProduct(productId)
        purchases.purchaseSubscriptionOptionWith(
            mockActivity,
            storeProduct,
            storeProduct.subscriptionOptions[0],
            onSuccess = { _, _ ->
                callCount++
            }, onError = { _, _ -> fail("should be successful") })

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(productId1, purchaseToken1, ProductType.SUBS)
        )

        assertThat(callCount).isEqualTo(0)
    }

    @Test
    fun `when multiple make purchase callbacks, a failure doesn't throw ConcurrentModificationException`() {
        val storeProduct = stubStoreProduct("productId")
        purchases.purchaseSubscriptionOptionWith(
            mockActivity,
            storeProduct,
            storeProduct.subscriptionOptions[0]
        ) { _, _ -> }

        purchases.purchaseSubscriptionOptionWith(
            mockActivity,
            storeProduct,
            storeProduct.subscriptionOptions[0]
        ) { _, _ -> }

        try {
            val error = PurchasesError(PurchasesErrorCode.StoreProblemError)
            capturedPurchasesUpdatedListener.captured.onPurchasesFailedToUpdate(error)
        } catch (e: ConcurrentModificationException) {
            fail("Test throws ConcurrentModificationException")
        }
    }

    @Test
    fun `when making purchase with upgrade info, error is forwarded`() {
        val productId = "onemonth_freetrial"

        val receiptInfo = mockQueryingProductDetails(productId, ProductType.SUBS, null)

        val stubBillingResult = mockk<BillingResult>()
        every { stubBillingResult.responseCode } returns BillingClient.BillingResponseCode.ERROR

        val underlyingErrorMessage = PurchaseStrings.ERROR_FINDING_PURCHASE.format("oldProductId")
        val error =
            stubBillingResult.responseCode.billingResponseToPurchasesError(underlyingErrorMessage)

        val oldPurchase = mockPurchaseFound(error)

        var receivedError: PurchasesError? = null
        var receivedUserCancelled: Boolean? = null

        purchases.purchaseSubscriptionOptionWith(
            mockActivity,
            receiptInfo.storeProduct!!,
            receiptInfo.storeProduct!!.subscriptionOptions[0],
            UpgradeInfo(oldPurchase.productIds[0]),
            onError = { purchaseError, userCancelled ->
                receivedError = purchaseError
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
        val productId = "onemonth_freetrial"

        val receiptInfo = mockQueryingProductDetails(productId, ProductType.SUBS, null)

        val oldPurchase = mockPurchaseFound()

        var receivedError: PurchasesError? = null
        var receivedUserCancelled: Boolean? = null

        purchases.purchaseSubscriptionOptionWith(
            mockActivity,
            receiptInfo.storeProduct!!,
            receiptInfo.storeProduct!!.subscriptionOptions[0],
            UpgradeInfo(oldPurchase.productIds[0]),
            onError = { error, userCancelled ->
                receivedError = error
                receivedUserCancelled = userCancelled
            }, onSuccess = { _, _ ->
                fail("should be error")
            })

        val error = PurchasesError(PurchasesErrorCode.StoreProblemError)
        capturedPurchasesUpdatedListener.captured.onPurchasesFailedToUpdate(error)


        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedUserCancelled).isFalse()
    }

    @Test
    fun `when purchasing a product with multiple subscriptionOptions, we choose the default`() {
        val productId = "onemonth_freetrial"

        val basePlanSubscriptionOption = stubSubscriptionOption("base-plan-purchase-option")
        val expectedDefaultSubscriptionOption = stubSubscriptionOption(
            "free-trial-purchase-option",
            pricingPhases = listOf(stubFreeTrialPricingPhase(), stubPricingPhase())
        )
        val storeProduct = stubStoreProduct(
            productId = productId,
            defaultOption = expectedDefaultSubscriptionOption,
            subscriptionOptions = listOf(expectedDefaultSubscriptionOption, basePlanSubscriptionOption)
        )

        mockQueryingProductDetails(storeProduct, null)

        purchases.purchaseProductWith(
            mockActivity,
            storeProduct,
            onError = { _, _ -> },
            onSuccess = { _, _ -> }
        )

        verify(exactly = 1) {
            mockBillingAbstract.makePurchaseAsync(
                mockActivity,
                appUserId,
                storeProduct,
                expectedDefaultSubscriptionOption,
                null,
                null
            )
        }
    }

    @Test
    fun `when purchasing a package with multiple subscriptionOptions, we choose the default`() {
        val productId = "onemonth_freetrial"

        val basePlanSubscriptionOption = stubSubscriptionOption("base-plan-purchase-option")
        val expectedDefaultSubscriptionOption = stubSubscriptionOption(
            "free-trial-purchase-option",
            pricingPhases = listOf(stubFreeTrialPricingPhase(), stubPricingPhase())
        )
        val storeProduct = stubStoreProduct(
            productId = productId,
            defaultOption = expectedDefaultSubscriptionOption,
            subscriptionOptions = listOf(expectedDefaultSubscriptionOption, basePlanSubscriptionOption)
        )
        val (_, offerings) = stubOfferings(storeProduct)

        mockQueryingProductDetails(storeProduct, null)

        purchases.purchasePackageWith(
            mockActivity,
            offerings[stubOfferingIdentifier]!!.monthly!!,
            onError = { _, _ -> },
            onSuccess = { _, _ -> }
        )

        verify(exactly = 1) {
            mockBillingAbstract.makePurchaseAsync(
                mockActivity,
                appUserId,
                storeProduct,
                expectedDefaultSubscriptionOption,
                null,
                stubOfferingIdentifier
            )
        }
    }

    @Test
    fun `when purchasing a package with upgrade info, completion block is called`() {
        val productId = "onemonth_freetrial"

        val (_, offerings) = stubOfferings(productId)
        mockQueryingProductDetails(productId, ProductType.SUBS, null)

        val purchaseToken = "crazy_purchase_token"

        val oldPurchase = mockPurchaseFound()

        var callCount = 0

        purchases.purchasePackageWith(
            mockActivity,
            offerings[stubOfferingIdentifier]!!.monthly!!,
            UpgradeInfo(oldPurchase.productIds[0]),
            onError = { _, _ ->
                fail("should be successful")
            }, onSuccess = { _, _ ->
                callCount++
            }
        )
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(
                offerings[stubOfferingIdentifier]!!.monthly!!.product.productId,
                purchaseToken,
                ProductType.SUBS
            )
        )
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun `when purchasing a package with upgrade info, completion is called with null purchase if product change is deferred`() {
        val productId = "onemonth_freetrial"
        val (_, offerings) = stubOfferings(productId)

        val oldPurchase = mockPurchaseFound()

        var callCount = 0

        purchases.purchasePackageWith(
            mockActivity,
            offerings[stubOfferingIdentifier]!!.monthly!!,
            UpgradeInfo(oldPurchase.productIds[0]),
            onError = { _, _ ->
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
        val productId = "onemonth_freetrial"
        val (_, offerings) = stubOfferings(productId)

        mockQueryingProductDetails(productId, ProductType.SUBS, null)

        val stubBillingResult = mockk<BillingResult>()
        every { stubBillingResult.responseCode } returns BillingClient.BillingResponseCode.ERROR

        val underlyingErrorMessage = PurchaseStrings.ERROR_FINDING_PURCHASE.format("oldProductId")
        val error =
            stubBillingResult.responseCode.billingResponseToPurchasesError(underlyingErrorMessage)

        val oldPurchase = mockPurchaseFound(error)

        var receivedError: PurchasesError? = null
        var receivedUserCancelled: Boolean? = null
        purchases.purchasePackageWith(
            mockActivity,
            offerings[stubOfferingIdentifier]!!.monthly!!,
            UpgradeInfo(oldPurchase.productIds[0]),
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
        val productId = "onemonth_freetrial"
        val (_, offerings) = stubOfferings(productId)

        val oldPurchase = mockPurchaseFound()

        var receivedError: PurchasesError? = null
        var receivedUserCancelled: Boolean? = null
        purchases.purchasePackageWith(
            mockActivity,
            offerings[stubOfferingIdentifier]!!.monthly!!,
            UpgradeInfo(oldPurchase.productIds[0]),
            onError = { error, userCancelled ->
                receivedError = error
                receivedUserCancelled = userCancelled
            }, onSuccess = { _, _ ->
                fail("should be error")
            }
        )

        val error = PurchasesError(PurchasesErrorCode.StoreProblemError)
        capturedPurchasesUpdatedListener.captured.onPurchasesFailedToUpdate(error)

        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedUserCancelled).isFalse()
    }

    @Test
    fun `Send error if cannot find the old purchase associated when upgrading a productId`() {
        val (storeProduct, offerings) = stubOfferings("onemonth_freetrial")

        val message = PurchaseStrings.NO_EXISTING_PURCHASE
        val error = PurchasesError(PurchasesErrorCode.PurchaseInvalidError, message)

        val oldPurchase = mockPurchaseFound(error)

        var receivedError: PurchasesError? = null
        var receivedUserCancelled: Boolean? = null
        purchases.purchasePackageWith(
            mockActivity,
            offerings[stubOfferingIdentifier]!!.monthly!!,
            UpgradeInfo(oldPurchase.productIds[0]),
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
                storeProduct,
                storeProduct.subscriptionOptions[0],
                ReplaceProductInfo(oldPurchase, ProrationMode.IMMEDIATE_WITHOUT_PRORATION),
                stubOfferingIdentifier
            )
        }
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
        assertThat(receivedUserCancelled).isFalse()
    }

    @Test
    fun `Send error if cannot find the old purchase associated when upgrading a product due to a billingclient error`() {
        val (storeProduct, offerings) = stubOfferings("onemonth_freetrial")
        val oldProductId = "oldProductId"

        val stubBillingResult = mockk<BillingResult>()
        every { stubBillingResult.responseCode } returns BillingClient.BillingResponseCode.ERROR

        val underlyingErrorMessage = PurchaseStrings.ERROR_FINDING_PURCHASE.format(oldProductId)
        val error =
            stubBillingResult.responseCode.billingResponseToPurchasesError(underlyingErrorMessage)

        val oldPurchase = mockPurchaseFound(error)

        every {
            mockBillingAbstract.findPurchaseInPurchaseHistory(
                appUserId,
                ProductType.SUBS,
                oldProductId,
                any(),
                captureLambda()
            )
        } answers {
            val underlyingErrorMessage = PurchaseStrings.ERROR_FINDING_PURCHASE.format(oldProductId)
            val error =
                stubBillingResult.responseCode.billingResponseToPurchasesError(underlyingErrorMessage)
            lambda<(PurchasesError) -> Unit>().captured.invoke(error)
        }

        var receivedError: PurchasesError? = null
        var receivedUserCancelled: Boolean? = null
        purchases.purchasePackageWith(
            mockActivity,
            offerings[stubOfferingIdentifier]!!.monthly!!,
            UpgradeInfo(oldPurchase.productIds[0]),
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
                storeProduct,
                storeProduct.subscriptionOptions[0],
                ReplaceProductInfo(oldPurchase, ProrationMode.IMMEDIATE_WITHOUT_PRORATION),
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
        val oldProductId = "oldProductId"

        val oldPurchase = mockk<StoreTransaction>()
        every { oldPurchase.productIds[0] } returns oldProductId
        every { oldPurchase.type } returns ProductType.SUBS

        every {
            mockBillingAbstract.findPurchaseInPurchaseHistory(
                appUserId,
                ProductType.SUBS,
                oldProductId,
                captureLambda(),
                any()
            )
        } answers {
            lambda<(StoreTransaction) -> Unit>().captured.invoke(oldPurchase)
        }

        purchases.purchasePackageWith(
            mockActivity,
            offerings[stubOfferingIdentifier]!!.monthly!!,
            UpgradeInfo(oldPurchase.productIds[0])
        ) { _, _ -> }

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(emptyList())
    }

    @Test
    fun `product not found when querying productId details while purchasing`() {
        val productId = "productId"
        val purchaseToken = "token"

        mockStoreProduct(listOf(productId), emptyList(), ProductType.INAPP)

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(
                productId,
                purchaseToken,
                ProductType.INAPP,
                "offering_a"
            )
        )
        val productInfo = ReceiptInfo(
            productIDs = listOf(productId),
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


    // endregion

    // region customer info

    @Test
    fun doesNotGetCustomerInfoOnCreated() {
        verify(exactly = 0) {
            mockCustomerInfoHelper.retrieveCustomerInfo(appUserId, any(), any(), any())
        }
    }

    @Test
    fun `fetch customer info on foregrounded if it's stale`() {
        mockCacheStale(customerInfoStale = true)
        mockSuccessfulQueryPurchases(
            queriedSUBS = emptyMap(),
            queriedINAPP = emptyMap(),
            notInCache = emptyList()
        )
        mockSynchronizeSubscriberAttributesForAllUsers()
        Purchases.sharedInstance.onAppForegrounded()
        verify(exactly = 1) {
            mockCustomerInfoHelper.retrieveCustomerInfo(
                appUserId,
                CacheFetchPolicy.FETCH_CURRENT,
                false,
                any()
            )
        }
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
            mockCustomerInfoHelper.retrieveCustomerInfo(
                appUserId,
                CacheFetchPolicy.FETCH_CURRENT,
                false,
                any()
            )
        }
        verify(exactly = 1) {
            mockCache.isCustomerInfoCacheStale(appInBackground = false, appUserID = appUserId)
        }
    }

    @Test
    fun receivedCustomerInfoShouldBeCached() {
        val productId = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        val productInfo = mockQueryingProductDetails(productId, ProductType.SUBS, null)

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(productId, purchaseToken, ProductType.SUBS)
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
            mockCustomerInfoHelper.cacheCustomerInfo(any())
        }
    }

    @Test
    fun `caches are not cleared if getting customer info fails`() {
        mockCustomerInfoHelper(PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken"))

        val lock = CountDownLatch(1)
        purchases.getCustomerInfoWith(onSuccess = {
            fail("supposed to be a failure")
        }, onError = {
            lock.countDown()
        })
        lock.await(200, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isZero()
        // This is not currently used, but we want to make sure we don't call it by mistake
        verify(exactly = 0) { mockCache.clearCachesForAppUserID(any()) }
    }

    @Test
    fun `invalidate customer info caches`() {
        Purchases.sharedInstance.invalidateCustomerInfoCache()
        verify(exactly = 1) {
            mockCache.clearCustomerInfoCache(appUserId)
        }
    }

    @Test
    fun `error when fetching customer info`() {
        mockCustomerInfoHelper(PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken"))
        val lock = CountDownLatch(1)
        purchases.getCustomerInfoWith(onSuccess = {
            fail("supposed to be error")
        }, onError = {
            lock.countDown()
        })
        lock.await(200, TimeUnit.MILLISECONDS)
        assertThat(lock.count).isZero()
    }

    @Test
    fun `given a random purchase update, we ask for listeners to update if there have been changes`() {
        val info = mockk<CustomerInfo>()

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
        purchases.updatedCustomerInfoListener = updatedCustomerInfoListener
        val productId = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        mockQueryingProductDetails(productId, ProductType.SUBS, null)
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(productId, purchaseToken, ProductType.SUBS)
        )
        verify(exactly = 1) {
            mockCustomerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(any())
        }
    }

    // endregion

    // region offerings

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
    fun `if no cached offerings, backend is hit when getting offerings`() {
        val productIds = listOf(stubProductIdentifier)
        mockProducts()
        mockStoreProduct(productIds, productIds, ProductType.SUBS)
        val (_, _) = stubOfferings("onemonth_freetrial")

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
        val productIds = listOf(stubProductIdentifier)
        mockProducts()
        mockStoreProduct(productIds, productIds, ProductType.SUBS)
        val (_, _) = stubOfferings("onemonth_freetrial")

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
        val productIds = listOf(stubProductIdentifier)
        mockProducts()
        mockStoreProduct(productIds, productIds, ProductType.SUBS)
        val (_, _) = stubOfferings("onemonth_freetrial")

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
    fun `configuration error if no products are found when fetching offerings`() {
        mockProducts(oneOfferingWithNoProductsResponse)

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
        assertThat(purchasesError!!.code).isEqualTo(PurchasesErrorCode.ConfigurationError)
        assertThat(purchasesError!!.underlyingErrorMessage).contains(
            OfferingStrings.CONFIGURATION_ERROR_NO_PRODUCTS_FOR_OFFERINGS
        )
        verify(exactly = 1) {
            mockCache.clearOfferingsCacheTimestamp()
        }
    }

    @Test
    fun `configuration error if products are not set up when fetching offerings`() {
        val productIds = listOf(stubProductIdentifier)

        mockProducts()
        clearMocks(mockBillingAbstract)
        mockStoreProduct(productIds, listOf(), ProductType.SUBS)
        mockStoreProduct(productIds, listOf(), ProductType.INAPP)

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
        assertThat(purchasesError!!.code).isEqualTo(PurchasesErrorCode.ConfigurationError)
        assertThat(purchasesError!!.underlyingErrorMessage).contains(
            OfferingStrings.CONFIGURATION_ERROR_PRODUCTS_NOT_FOUND
        )
        verify(exactly = 1) {
            mockCache.clearOfferingsCacheTimestamp()
        }
    }

    @Test
    fun `if cached offerings are not stale`() {
        mockProducts()
        mockStoreProduct(listOf(), listOf(), ProductType.SUBS)
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
        mockStoreProduct(listOf(), listOf(), ProductType.SUBS)
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
        mockStoreProduct(listOf(), listOf(), ProductType.SUBS)
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
        mockStoreProduct(listOf(), listOf(), ProductType.SUBS)
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

    // endregion

    // region restoring

    @Test
    fun isRestoreWhenUsingNullAppUserID() {
        anonymousSetup(true)

        val productId = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        val productInfo = mockQueryingProductDetails(productId, ProductType.SUBS, null)

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(productId, purchaseToken, ProductType.SUBS)
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
        val productId = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        val productInfo = mockQueryingProductDetails(productId, ProductType.SUBS, null)

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(productId, purchaseToken, ProductType.SUBS)
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
    fun restoringPurchasesGetsHistory() {
        var capturedLambda: ((List<StoreTransaction>) -> Unit)? = null
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
                captureLambda(),
                any()
            )
        } answers {
            capturedLambda = lambda<(List<StoreTransaction>) -> Unit>().captured.also {
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
    fun `restoring purchases updates customer info cache`() {
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
                captureLambda(),
                any()
            )
        } answers {
            lambda<(List<StoreTransaction>) -> Unit>().captured.also {
                it.invoke(listOf(mockk(relaxed = true)))
            }
        }

        purchases.restorePurchasesWith { }

        verify(exactly = 1) {
            mockCustomerInfoHelper.cacheCustomerInfo(any())
        }
        verify(exactly = 1) {
            mockCustomerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(any())
        }
    }

    @Test
    fun historicalPurchasesPassedToBackend() {
        var capturedLambda: ((List<StoreTransaction>) -> Unit)? = null
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
                captureLambda(),
                any()
            )
        } answers {
            capturedLambda = lambda<(List<StoreTransaction>) -> Unit>().captured
            capturedLambda?.invoke(
                getMockedPurchaseHistoryList(inAppProductId, inAppPurchaseToken, ProductType.INAPP) +
                    getMockedPurchaseHistoryList(subProductId, subPurchaseToken, ProductType.SUBS)
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

        val productInfo = ReceiptInfo(productIDs = listOf(inAppProductId))
        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = inAppPurchaseToken,
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

        val productInfo1 = ReceiptInfo(productIDs = listOf(subProductId))
        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = subPurchaseToken,
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
        val productId = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        val productIdSub = "onemonth_freetrial_sub"
        val purchaseTokenSub = "crazy_purchase_token_sub"

        var capturedLambda: ((List<StoreTransaction>) -> Unit)? = null
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
                captureLambda(),
                any()
            )
        } answers {
            capturedLambda = lambda<(List<StoreTransaction>) -> Unit>().captured.also {
                it.invoke(
                    getMockedPurchaseHistoryList(productId, purchaseToken, ProductType.INAPP) +
                        getMockedPurchaseHistoryList(productIdSub, purchaseTokenSub, ProductType.SUBS)
                )
            }
        }

        val mockInfo = JSONObject(Responses.validFullPurchaserResponse).buildCustomerInfo()
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
            lambda<PostReceiptDataSuccessCallback>().captured.invoke(mockInfo, mockInfo.rawData)
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

        val mockCompletion = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        purchases.restorePurchases(mockCompletion)

        verify {
            mockCompletion.onReceived(any())
        }
    }

    // endregion

    // region postReceipt

    @Test
    fun `tries to consume purchases on 4xx returned from postReceipt`() {
        var capturedLambda: (PostReceiptDataErrorCallback)? = null
        mockPostReceiptError(
            inAppProductId,
            inAppPurchaseToken,
            observerMode = false,
            offeringIdentifier = null,
            type = ProductType.INAPP,
            answer = {
                capturedLambda = lambda<PostReceiptDataErrorCallback>().captured
                capturedLambda?.invokeWithFinishableError()
            }
        )
        mockPostReceiptError(
            subProductId,
            subPurchaseToken,
            observerMode = false,
            offeringIdentifier = null,
            type = ProductType.SUBS,
            answer = {
                capturedLambda = lambda<PostReceiptDataErrorCallback>().captured
                capturedLambda?.invokeWithFinishableError()
            }
        )

        val purchases = getMockedPurchaseList(inAppProductId, inAppPurchaseToken, ProductType.INAPP) +
            getMockedPurchaseList(subProductId, subPurchaseToken, ProductType.SUBS)
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(purchases)

        purchases.forEach {
            verify(exactly = 1) {
                mockBillingAbstract.consumeAndSave(true, it)
            }
        }

        assertThat(capturedLambda).isNotNull
    }

    @Test
    fun `tries to consume restored purchases on 4xx returned from postReceipt`() {
        var capturedPostReceiptLambda: (PostReceiptDataErrorCallback)? = null
        mockPostReceiptError(
            inAppProductId,
            inAppPurchaseToken,
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
            subProductId,
            subPurchaseToken,
            observerMode = false,
            offeringIdentifier = null,
            type = ProductType.SUBS,
            answer = {
                capturedPostReceiptLambda = lambda<PostReceiptDataErrorCallback>().captured
                capturedPostReceiptLambda?.invokeWithFinishableError()
            },
            isRestore = true
        )
        val purchaseRecords = getMockedPurchaseHistoryList(inAppProductId, inAppPurchaseToken, ProductType.INAPP) +
            getMockedPurchaseHistoryList(subProductId, subPurchaseToken, ProductType.SUBS)

        var capturedRestoreLambda: ((List<StoreTransaction>) -> Unit)? = null
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
                captureLambda(),
                any()
            )
        } answers {
            capturedRestoreLambda = lambda<(List<StoreTransaction>) -> Unit>().captured
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
    fun `does not consume purchases on 5xx returned from postReceipt`() {
        mockPostReceiptError(
            inAppProductId,
            inAppPurchaseToken,
            observerMode = false,
            offeringIdentifier = null,
            type = ProductType.INAPP,
            answer = {
                lambda<PostReceiptDataErrorCallback>().captured.invokeWithNotFinishableError()
            }
        )
        mockPostReceiptError(
            subProductId,
            subPurchaseToken,
            observerMode = false,
            offeringIdentifier = null,
            type = ProductType.SUBS,
            answer = {
                lambda<PostReceiptDataErrorCallback>().captured.invokeWithNotFinishableError()
            }
        )

        val purchaseRecords = getMockedPurchaseList(inAppProductId, inAppPurchaseToken, ProductType.INAPP) +
            getMockedPurchaseList(subProductId, subPurchaseToken, ProductType.SUBS)

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(purchaseRecords)

        purchaseRecords.forEach {
            verify(exactly = 0) {
                mockBillingAbstract.consumeAndSave(any(), it)
            }
        }
    }

    @Test
    fun `does not consume restored purchases on 5xx returned from postReceipt`() {
        val purchaseRecords = getMockedPurchaseHistoryList(inAppProductId, inAppPurchaseToken, ProductType.INAPP) +
            getMockedPurchaseHistoryList(subProductId, subPurchaseToken, ProductType.SUBS)

        var capturedRestoreLambda: ((List<StoreTransaction>) -> Unit)? = null
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
                captureLambda(),
                any()
            )
        } answers {
            capturedRestoreLambda = lambda<(List<StoreTransaction>) -> Unit>().captured
            capturedRestoreLambda?.invoke(purchaseRecords)
        }

        mockPostReceiptError(
            inAppProductId,
            inAppPurchaseToken,
            observerMode = false,
            offeringIdentifier = null,
            type = ProductType.INAPP,
            answer = {
                lambda<PostReceiptDataErrorCallback>().captured.invokeWithNotFinishableError()
            },
            isRestore = true
        )
        mockPostReceiptError(
            subProductId,
            subPurchaseToken,
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
        assertThat(errorCalled).isTrue

        purchaseRecords.forEach {
            verify(exactly = 0) {
                mockBillingAbstract.consumeAndSave(any(), it)
            }
        }
    }

    @Test
    fun `when finishTransactions is set to false, do not consume transactions`() {
        purchases.finishTransactions = false

        val productInfo = mockPostReceipt(
            inAppProductId,
            inAppPurchaseToken,
            observerMode = true,
            mockInfo = mockInfo,
            offeringIdentifier = null,
            type = ProductType.INAPP
        )

        val productInfo1 = mockPostReceipt(
            subProductId,
            subPurchaseToken,
            observerMode = true,
            mockInfo = mockInfo,
            offeringIdentifier = null,
            type = ProductType.SUBS
        )

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(inAppProductId, inAppPurchaseToken, ProductType.INAPP) +
                getMockedPurchaseList(subProductId, subPurchaseToken, ProductType.SUBS)
        )

        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = inAppPurchaseToken,
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
                purchaseToken = subPurchaseToken,
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
        mockPostReceiptError(
            inAppProductId,
            inAppPurchaseToken,
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
            subProductId,
            subPurchaseToken,
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
            getMockedPurchaseList(inAppProductId, inAppPurchaseToken, ProductType.INAPP) +
                getMockedPurchaseList(subProductId, subPurchaseToken, ProductType.SUBS)
        )

        assertThat(capturedShouldTryToConsume.isCaptured).isTrue()
        assertThat(capturedShouldTryToConsume.captured).isFalse
    }

    @Test
    fun `when finishTransactions is set to false, don't consume transactions on 5xx`() {
        mockPostReceiptError(
            inAppProductId,
            inAppPurchaseToken,
            observerMode = true,
            offeringIdentifier = null,
            type = ProductType.INAPP,
            answer = {
                lambda<PostReceiptDataErrorCallback>().captured.invokeWithNotFinishableError()
            })
        mockPostReceiptError(
            subProductId,
            subPurchaseToken,
            observerMode = true,
            offeringIdentifier = null,
            type = ProductType.SUBS,
            answer = {
                lambda<PostReceiptDataErrorCallback>().captured.invokeWithNotFinishableError()
            })
        purchases.finishTransactions = false

        val allPurchases = getMockedPurchaseList(inAppProductId, inAppPurchaseToken, ProductType.INAPP) +
            getMockedPurchaseList(subProductId, subPurchaseToken, ProductType.SUBS)
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

        mockPostReceipt(
            inAppProductId,
            inAppPurchaseToken,
            observerMode = true,
            mockInfo = mockInfo,
            offeringIdentifier = null,
            type = ProductType.INAPP,
            restore = true
        )
        mockPostReceipt(
            subProductId,
            subPurchaseToken,
            observerMode = true,
            mockInfo = mockInfo,
            offeringIdentifier = null,
            type = ProductType.SUBS,
            restore = true
        )
        val purchaseRecords = getMockedPurchaseHistoryList(inAppProductId, inAppPurchaseToken, ProductType.INAPP) +
            getMockedPurchaseHistoryList(subProductId, subPurchaseToken, ProductType.SUBS)

        var capturedRestoreLambda: ((List<StoreTransaction>) -> Unit)? = null
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
                captureLambda(),
                any()
            )
        } answers {
            capturedRestoreLambda = lambda<(List<StoreTransaction>) -> Unit>().captured
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
    fun `when error posting receipts tokens are not saved in cache if error is not finishable`() {
        var capturedLambda: (PostReceiptDataErrorCallback)? = null
        mockPostReceiptError(
            inAppProductId,
            inAppPurchaseToken,
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
            subProductId,
            subPurchaseToken,
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
            getMockedPurchaseList(inAppProductId, inAppPurchaseToken, ProductType.INAPP) +
                getMockedPurchaseList(subProductId, subPurchaseToken, ProductType.SUBS)
        )

        assertThat(capturedLambda).isNotNull
        assertThat(capturedLambda1).isNotNull

        verify(exactly = 0) {
            mockCache.addSuccessfullyPostedToken(inAppPurchaseToken)
        }
        verify(exactly = 0) {
            mockCache.addSuccessfullyPostedToken(subPurchaseToken)
        }
    }

    @Test
    fun `reposted receipts are sent using allowSharingAccount`() {

        purchases.allowSharingPlayStoreAccount = true
        val purchase = stubGooglePurchase(
            purchaseToken = "token",
            productIds = listOf("product"),
            purchaseState = Purchase.PurchaseState.PURCHASED
        )
        val activePurchase = purchase.toStoreTransaction(ProductType.SUBS, null)
        mockSuccessfulQueryPurchases(
            queriedSUBS = mapOf(purchase.purchaseToken.sha1() to activePurchase),
            queriedINAPP = emptyMap(),
            notInCache = listOf(activePurchase)
        )
        val productInfo = mockQueryingProductDetails("product", ProductType.SUBS, null)

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
    fun `posted inapps also post currency and price`() {
        val productId = "productId"
        val purchaseToken = "token"

        val receiptInfo = mockQueryingProductDetails(productId, ProductType.INAPP, offeringIdentifier = "offering_a")

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(
                productId,
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
                receiptInfo = receiptInfo,
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `posted subs post currency and price`() {
        val productIdSub = "sub"
        val purchaseTokenSub = "token_sub"

        mockStoreProduct(listOf(productIdSub), emptyList(), ProductType.SUBS)

        val offeringIdentifier = "offering_a"
        val subscriptionOptionId = "subscription_option"
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(
                productId = productIdSub,
                purchaseToken = purchaseTokenSub,
                productType = ProductType.SUBS,
                offeringIdentifier = offeringIdentifier,
                subscriptionOptionId = subscriptionOptionId
            )
        )
        val productInfo = ReceiptInfo(
            productIDs = listOf(productIdSub),
            offeringIdentifier = offeringIdentifier,
            subscriptionOptionId = subscriptionOptionId
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
            mockBillingAbstract.queryProductDetailsAsync(
                ProductType.SUBS,
                any(),
                any(),
                any()
            )
        }
    }

    // endregion

    // region identity

    @Test
    fun `login with the same appUserID as the current, fetches customerInfo and calls onSuccess if successful`() {
        val appUserID = "myUser"
        every { mockCache.isCustomerInfoCacheStale(appUserID, any()) } returns true
        every { mockCache.setCustomerInfoCacheTimestampToNow(appUserID) } just Runs
        every { mockIdentityManager.currentAppUserID } returns appUserID

        val mockCompletion = mockk<LogInCallback>(relaxed = true)

        purchases.logIn(appUserID, mockCompletion)

        verify(exactly = 1) {
            mockCompletion.onReceived(any(), any())
            mockCustomerInfoHelper.retrieveCustomerInfo(appUserID, any(), false, any())
        }
    }

    @Test
    fun `login with the same appUserID as the current, calls onSuccess with created false if successful`() {
        val appUserID = "myUser"
        every { mockCache.isCustomerInfoCacheStale(appUserID, any()) } returns true
        every { mockCache.setCustomerInfoCacheTimestampToNow(appUserID) } just Runs
        every { mockIdentityManager.currentAppUserID } returns appUserID

        val mockCompletion = mockk<LogInCallback>(relaxed = true)

        purchases.logIn(appUserID, mockCompletion)

        verify(exactly = 1) {
            mockCompletion.onReceived(any(), false)
        }
    }

    @Test
    fun `login with the same appUserID as the current, fetches customerInfo and calls onError if failed`() {
        val appUserID = "myUser"
        every { mockCache.getCachedCustomerInfo(appUserID) } returns null
        every { mockCache.isCustomerInfoCacheStale(appUserID, any()) } returns true
        every { mockCache.setCustomerInfoCacheTimestampToNow(appUserID) } just Runs
        every { mockCache.clearCustomerInfoCacheTimestamp(appUserID) } just Runs
        every { mockIdentityManager.currentAppUserID } returns appUserID

        val purchasesError = PurchasesError(PurchasesErrorCode.InvalidCredentialsError)
        mockCustomerInfoHelper(errorGettingCustomerInfo = purchasesError)

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
            lambda<(CustomerInfo, Boolean) -> Unit>().captured.invoke(mockInfo, mockCreated)
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
            lambda<(CustomerInfo, Boolean) -> Unit>().captured.invoke(mockInfo, mockCreated)
        }

        val mockCompletion = mockk<LogInCallback>(relaxed = true)
        val newAppUserID = "newAppUserID"
        purchases.logIn(newAppUserID, mockCompletion)

        verify(exactly = 1) {
            mockCompletion.onReceived(mockInfo, mockCreated)
        }
    }

    @Test
    fun `login successful with new appUserID calls customer info helper to update delegate if changed`() {
        purchases.updatedCustomerInfoListener = updatedCustomerInfoListener

        val mockCreated = Random.nextBoolean()
        every { mockIdentityManager.currentAppUserID } returns "oldAppUserID"

        every {
            mockIdentityManager.logIn(any(), onSuccess = captureLambda(), any())
        } answers {
            lambda<(CustomerInfo, Boolean) -> Unit>().captured.invoke(mockInfo, mockCreated)
        }

        val mockCompletion = mockk<LogInCallback>(relaxed = true)
        val newAppUserID = "newAppUserID"
        purchases.logIn(newAppUserID, mockCompletion)

        verify(exactly = 1) {
            mockCustomerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(mockInfo)
        }
    }

    @Test
    fun `login successful with new appUserID refreshes offerings`() {
        val mockCreated = Random.nextBoolean()
        every { mockIdentityManager.currentAppUserID } returns "oldAppUserID"

        every {
            mockIdentityManager.logIn(any(), onSuccess = captureLambda(), any())
        } answers {
            lambda<(CustomerInfo, Boolean) -> Unit>().captured.invoke(mockInfo, mockCreated)
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
            mockCache.cleanupOldAttributionData()
        } just Runs
        mockIdentityManagerLogout()
        val mockCompletion = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        purchases.logOut(mockCompletion)

        verify(exactly = 1) {
            mockCache.setOfferingsCacheTimestampToNow()
        }
        verify(exactly = 1) {
            mockCustomerInfoHelper.retrieveCustomerInfo(
                appUserID,
                CacheFetchPolicy.FETCH_CURRENT,
                false,
                any()
            )
        }
        verify(exactly = 1) {
            mockBackend.getOfferings(appUserID, any(), any(), any())
        }
    }

    @Test
    fun `when logging out, identity manager is called`() {
        every {
            mockCache.cleanupOldAttributionData()
        } just Runs
        mockIdentityManagerLogout()

        purchases.logOut()
        verify(exactly = 1) {
            mockIdentityManager.logOut(any())
        }
    }

    @Test
    fun `when logging out, we fetch customer info`() {
        every {
            mockCache.cleanupOldAttributionData()
        } just Runs
        mockIdentityManagerLogout()

        purchases.logOut()
        verify(exactly = 1) {
            mockCustomerInfoHelper.retrieveCustomerInfo(
                appUserId,
                CacheFetchPolicy.FETCH_CURRENT,
                false,
                null)
        }
    }

    @Test
    fun `if there's an error on logOut, the error is passed`() {
        every {
            mockCache.cleanupOldAttributionData()
        } just Runs
        val mockError = mockk<PurchasesError>(relaxed = true)
        val mockCompletion = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        mockIdentityManagerLogout(mockError)

        purchases.logOut(mockCompletion)
        verify(exactly = 1) {
            mockCompletion.onError(mockError)
        }
    }

    @Test
    fun `logOut calls completion with new customerInfo when successful`() {
        every {
            mockCache.cleanupOldAttributionData()
        } just Runs

        val mockCompletion = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        mockIdentityManagerLogout()

        purchases.logOut(mockCompletion)
        verify(exactly = 1) {
            mockCompletion.onReceived(mockInfo)
        }
    }

    @Test
    fun `logOut clears backend caches when successful`() {
        setup()

        every {
            mockCache.cleanupOldAttributionData()
        } just Runs

        every {
            mockBackend.clearCaches()
        } just Runs

        val mockCompletion = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        mockIdentityManagerLogout()

        purchases.logOut(mockCompletion)
        verify(exactly = 1) {
            mockBackend.clearCaches()
        }
    }

    // endregion

    // region canMakePayments

    @Test
    fun `when calling canMakePayments and billing service disconnects, return false`() {
        var receivedCanMakePayments = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)

        val listener = setUpMockBillingClientBuilderAndListener(mockLocalBillingClient)

        Purchases.canMakePayments(mockContext, listOf()) {
            receivedCanMakePayments = it
        }
        listener.captured.onBillingServiceDisconnected()
        assertThat(receivedCanMakePayments).isFalse
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `canMakePayments with no features and OK BillingResponse returns true`() {
        var receivedCanMakePayments = false
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = setUpMockBillingClientBuilderAndListener(mockLocalBillingClient)

        Purchases.canMakePayments(mockContext, listOf()) {
            receivedCanMakePayments = it
        }

        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())

        assertThat(receivedCanMakePayments).isTrue
    }

    @Test
    fun `when no play services, canMakePayments returns false`() {
        var receivedCanMakePayments = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = setUpMockBillingClientBuilderAndListener(mockLocalBillingClient)

        every { mockLocalBillingClient.endConnection() } throws mockk<IllegalArgumentException>()
        Purchases.canMakePayments(mockContext, listOf(BillingFeature.SUBSCRIPTIONS)) {
            receivedCanMakePayments = it
        }
        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult())
        AssertionsForClassTypes.assertThat(receivedCanMakePayments).isFalse
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when calling canMakePayments, enablePendingPurchases is called`() {
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
        every { mockLocalBillingClient.startConnection(any()) } just Runs

        Purchases.canMakePayments(mockContext, listOf(BillingFeature.SUBSCRIPTIONS)) {}
        verify(exactly = 1) { mockBuilder.enablePendingPurchases() }
    }

    fun `canMakePayments returns true for Amazon configurations`() {
        purchases.appConfig = AppConfig(
            mockContext,
            false,
            PlatformInfo("", null),
            null,
            Store.AMAZON
        )
        Purchases.canMakePayments(mockContext, listOf()) {
            assertThat(it).isTrue()
        }
    }

    @Test
    fun `when billing is not supported, canMakePayments is false`() {
        var receivedCanMakePayments = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = setUpMockBillingClientBuilderAndListener(mockLocalBillingClient)

        every {
            mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        } returns BillingClient.BillingResponseCode.OK.buildResult()

        mockHandlerPost()

        Purchases.canMakePayments(mockContext, listOf(BillingFeature.SUBSCRIPTIONS)) {
            receivedCanMakePayments = it
        }

        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE.buildResult())

        AssertionsForClassTypes.assertThat(receivedCanMakePayments).isFalse
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when feature is not supported, canMakePayments is false`() {
        var receivedCanMakePayments = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = setUpMockBillingClientBuilderAndListener(mockLocalBillingClient)

        every {
            mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        } returns BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult()

        Purchases.canMakePayments(mockContext, listOf(BillingFeature.SUBSCRIPTIONS)) {
            receivedCanMakePayments = it
        }

        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())

        AssertionsForClassTypes.assertThat(receivedCanMakePayments).isFalse
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when one feature in list is not supported, canMakePayments is false`() {
        var receivedCanMakePayments = true
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = setUpMockBillingClientBuilderAndListener(mockLocalBillingClient)

        every {
            mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        } returns BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED.buildResult()

        every {
            mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS_UPDATE)
        } returns BillingClient.BillingResponseCode.OK.buildResult()

        Purchases.canMakePayments(
            mockContext,
            listOf(BillingFeature.SUBSCRIPTIONS,
                BillingFeature.SUBSCRIPTIONS_UPDATE)
        ) {
            receivedCanMakePayments = it
        }

        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())

        AssertionsForClassTypes.assertThat(receivedCanMakePayments).isFalse
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when single feature is supported and billing is supported, canMakePayments is true`() {
        var receivedCanMakePayments = false
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = setUpMockBillingClientBuilderAndListener(mockLocalBillingClient)

        every {
            mockLocalBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        } returns BillingClient.BillingResponseCode.OK.buildResult()

        mockHandlerPost()
        Purchases.canMakePayments(mockContext, listOf(BillingFeature.SUBSCRIPTIONS)) {
            receivedCanMakePayments = it
        }

        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())

        AssertionsForClassTypes.assertThat(receivedCanMakePayments).isTrue
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    @Test
    fun `when feature list is empty, canMakePayments does not check billing client for feature support`() {
        val mockLocalBillingClient = mockk<BillingClient>(relaxed = true)
        val listener = setUpMockBillingClientBuilderAndListener(mockLocalBillingClient)

        Purchases.canMakePayments(mockContext, listOf()) {}

        listener.captured.onBillingSetupFinished(BillingClient.BillingResponseCode.OK.buildResult())

        verify(exactly = 0) { mockLocalBillingClient.isFeatureSupported(any()) }
        verify(exactly = 1) { mockLocalBillingClient.endConnection() }
    }

    // endregion

    // region syncPurchases

    @Test
    fun `syncing transactions gets whole history and posts it to backend`() {
        purchases.finishTransactions = false

        var capturedLambda: ((List<StoreTransaction>) -> Unit)? = null
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
                captureLambda(),
                any()
            )
        } answers {
            capturedLambda = lambda<(List<StoreTransaction>) -> Unit>().captured.also {
                it.invoke(
                    getMockedPurchaseHistoryList(inAppProductId, inAppPurchaseToken, ProductType.INAPP) +
                        getMockedPurchaseHistoryList(subProductId, subPurchaseToken, ProductType.SUBS)
                )
            }
        }

        purchases.syncPurchases()

        val productInfo = ReceiptInfo(productIDs = listOf(inAppProductId))
        assertThat(capturedLambda).isNotNull
        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = inAppPurchaseToken,
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
        val productInfo1 = ReceiptInfo(productIDs = listOf(subProductId))
        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = subPurchaseToken,
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
        purchases.allowSharingPlayStoreAccount = true

        var capturedLambda: ((List<StoreTransaction>) -> Unit)? = null
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
                captureLambda(),
                any()
            )
        } answers {
            capturedLambda = lambda<(List<StoreTransaction>) -> Unit>().captured.also {
                it.invoke(
                    getMockedPurchaseHistoryList(inAppProductId, inAppPurchaseToken, ProductType.INAPP) +
                        getMockedPurchaseHistoryList(subProductId, subPurchaseToken, ProductType.SUBS)
                )
            }
        }

        purchases.syncPurchases()

        val productInfo = ReceiptInfo(productIDs = listOf(inAppProductId))
        assertThat(capturedLambda).isNotNull
        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = inAppPurchaseToken,
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

        val productInfo1 = ReceiptInfo(productIDs = listOf(subProductId))
        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = subPurchaseToken,
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
        val productId = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        purchases.allowSharingPlayStoreAccount = true

        var capturedLambda: ((List<StoreTransaction>) -> Unit)? = null
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
                captureLambda(),
                any()
            )
        } answers {
            capturedLambda = lambda<(List<StoreTransaction>) -> Unit>().captured.also {
                it.invoke(getMockedPurchaseHistoryList(productId, purchaseToken, ProductType.INAPP))
            }
        }

        purchases.syncPurchases()

        val productInfo = ReceiptInfo(productIDs = listOf(productId))
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
    fun `syncing transactions updates customer info cache`() {
        val productId = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
                captureLambda(),
                any()
            )
        } answers {
            lambda<(List<StoreTransaction>) -> Unit>().captured.also {
                it.invoke(getMockedPurchaseHistoryList(productId, purchaseToken, ProductType.INAPP))
            }
        }

        purchases.syncPurchases()

        verify(exactly = 1) {
            mockCustomerInfoHelper.cacheCustomerInfo(any())
        }
        verify(exactly = 1) {
            mockCustomerInfoHelper.sendUpdatedCustomerInfoToDelegateIfChanged(any())
        }
    }

    @Test
    fun `syncing an Amazon transaction posts normalized purchase data to backend`() {
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
            receiptID = purchaseToken,
            amazonUserID = amazonUserID,
            price = price,
            isoCurrencyCode = currencyCode
        )

        val productInfo = ReceiptInfo(
            productIDs = listOf(skuTerm),
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
    fun `syncing an Amazon transaction respects allow sharing account settings`() {
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
            receiptID = purchaseToken,
            amazonUserID = amazonUserID,
            price = price,
            isoCurrencyCode = currencyCode
        )

        val productInfo = ReceiptInfo(
            productIDs = listOf(skuTerm),
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
    fun `syncing an Amazon transaction never consumes it`() {
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
            receiptID = purchaseToken,
            amazonUserID = amazonUserID,
            price = price,
            isoCurrencyCode = currencyCode
        )

        val productInfo = ReceiptInfo(
            productIDs = listOf(skuTerm),
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
    fun `Amazon transaction is not synced again if it was already synced`() {
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
            receiptID = purchaseToken,
            amazonUserID = amazonUserID,
            price = price,
            isoCurrencyCode = currencyCode
        )

        val productInfo = ReceiptInfo(
            productIDs = listOf(skuTerm),
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
            receiptID = purchaseToken,
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
    fun `syncing an Amazon transaction sends subscriber attributes`() {
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
            mockSubscriberAttributesManager.getUnsyncedSubscriberAttributes(appUserId, captureLambda())
        } answers {
            lambda<(Map<String, SubscriberAttribute>) -> Unit>().captured.also {
                it.invoke(unsyncedSubscriberAttributes)
            }
        }

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
            receiptID = purchaseToken,
            amazonUserID = amazonUserID,
            price = price,
            isoCurrencyCode = currencyCode
        )

        val productInfo = ReceiptInfo(
            productIDs = listOf(skuTerm),
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
    fun `syncing an Amazon transaction without price nor currency code posts purchase data to backend`() {
        purchases.finishTransactions = false

        val skuParent = "sub"
        val skuTerm = "sub.monthly"
        val purchaseToken = "crazy_purchase_token"
        val amazonUserID = "amazon_user_id"

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
            receiptID = purchaseToken,
            amazonUserID = amazonUserID,
            price = null,
            isoCurrencyCode = null
        )

        val productInfo = ReceiptInfo(productIDs = listOf(skuTerm))
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
    fun `syncing an Amazon transaction with zero price posts correct purchase data to backend`() {
        purchases.finishTransactions = false

        val skuParent = "sub"
        val skuTerm = "sub.monthly"
        val purchaseToken = "crazy_purchase_token"
        val amazonUserID = "amazon_user_id"

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
            receiptID = purchaseToken,
            amazonUserID = amazonUserID,
            price = 0.0,
            isoCurrencyCode = null
        )

        val productInfo = ReceiptInfo(
            productIDs = listOf(skuTerm),
            currency = null,
            price = null
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

    // endregion

    // region teardown

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
    fun closeCloses() {
        mockCloseActions()

        purchases.close()
        verifyClose()
    }

    // endregion

    // region updatePendingPurchaseQueue

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
            mockBackend.postReceiptData(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `post pending purchases if autosync is on`() {
        buildPurchases(anonymous = true, autoSync = true)
        val purchase = stubGooglePurchase(
            purchaseToken = "token",
            productIds = listOf("product"),
            purchaseState = Purchase.PurchaseState.PURCHASED
        )
        val activePurchase = purchase.toStoreTransaction(ProductType.SUBS, null)
        mockSuccessfulQueryPurchases(
            queriedSUBS = mapOf(purchase.purchaseToken.sha1() to activePurchase),
            queriedINAPP = emptyMap(),
            notInCache = listOf(activePurchase)
        )
        val productInfo = mockQueryingProductDetails("product", ProductType.SUBS, null)

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
            productIds = listOf("product"),
            purchaseState = Purchase.PurchaseState.PURCHASED
        )

        val purchaseWrapper = purchase.toStoreTransaction(ProductType.SUBS, null)
        mockSuccessfulQueryPurchases(
            queriedSUBS = mapOf(purchase.purchaseToken.sha1() to purchaseWrapper),
            queriedINAPP = emptyMap(),
            notInCache = listOf(purchaseWrapper)
        )
        val receiptInfo = mockQueryingProductDetails("product", ProductType.SUBS, null)

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
            productIds = listOf("product")
        )
        mockSuccessfulQueryPurchases(
            queriedSUBS = mapOf(
                purchase.purchaseToken.sha1() to purchase.toStoreTransaction(
                    ProductType.SUBS,
                    null
                )
            ),
            queriedINAPP = emptyMap(),
            notInCache = emptyList()
        )
        purchases.updatePendingPurchaseQueue()

        val productInfo = ReceiptInfo(productIDs = listOf("product"))
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
    fun `all non-pending purchases returned from queryPurchases are posted to backend`() {
        val purchasedPurchase = stubGooglePurchase(
            purchaseToken = "purchasedToken",
            productIds = listOf("product"),
            purchaseState = Purchase.PurchaseState.PURCHASED
        )
        val activePurchasedPurchase = purchasedPurchase.toStoreTransaction(ProductType.SUBS, null)

        val pendingPurchase = stubGooglePurchase(
            purchaseToken = "pendingToken",
            productIds = listOf("product"),
            purchaseState = Purchase.PurchaseState.PENDING
        )
        val activePendingPurchase = pendingPurchase.toStoreTransaction(ProductType.SUBS, null)

        val unspecifiedPurchase = stubGooglePurchase(
            purchaseToken = "unspecifiedToken",
            productIds = listOf("product"),
            purchaseState = Purchase.PurchaseState.UNSPECIFIED_STATE
        )
        val activeUnspecifiedPurchase = unspecifiedPurchase.toStoreTransaction(ProductType.SUBS, null)

        mockSuccessfulQueryPurchases(
            queriedSUBS = mapOf(purchasedPurchase.purchaseToken.sha1() to activePurchasedPurchase,
                pendingPurchase.purchaseToken.sha1() to activePendingPurchase,
                unspecifiedPurchase.purchaseToken.sha1() to activeUnspecifiedPurchase),
            queriedINAPP = emptyMap(),
            notInCache = listOf(activePurchasedPurchase, activePendingPurchase, activeUnspecifiedPurchase)
        )
        val productInfo = mockQueryingProductDetails("product", ProductType.SUBS, null)

        purchases.updatePendingPurchaseQueue()

        verify(exactly = 1) {
            mockBackend.postReceiptData(
                purchaseToken = "purchasedToken",
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
                purchaseToken = "unspecifiedToken",
                appUserID = any(),
                isRestore = false,
                observerMode = false,
                subscriberAttributes = emptyMap(),
                receiptInfo = any(),
                storeAppUserID = null,
                onSuccess = any(),
                onError = any()
            )
        }

        verify(exactly = 0) {
            mockBackend.postReceiptData(
                purchaseToken = "pendingToken",
                appUserID = any(),
                isRestore = false,
                observerMode = false,
                subscriberAttributes = emptyMap(),
                receiptInfo = productInfo,
                storeAppUserID = any(),
                onSuccess = any(),
                onError = any()
            )
        }
    }

    // endregion

    // region queryPurchases

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
            mockBillingAbstract.queryPurchases(ProductType.SUBS.toGoogleProductType()!!, any(), any())
        }
        verify(exactly = 0) {
            mockBillingAbstract.queryPurchases(ProductType.INAPP.toGoogleProductType()!!, any(), any())
        }
    }

    // endregion

    // region app lifecycle

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
            mockCustomerInfoHelper.retrieveCustomerInfo(
                appUserId,
                CacheFetchPolicy.FETCH_CURRENT,
                false,
                any()
            )
        }
        verify(exactly = 0) {
            mockCache.isCustomerInfoCacheStale(appInBackground = false, appUserID = appUserId)
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
            mockCache.isCustomerInfoCacheStale(appInBackground = false, appUserID = appUserId)
        } returns false
        purchases.state = purchases.state.copy(appInBackground = false, firstTimeInForeground = false)
        Purchases.sharedInstance.onAppForegrounded()
        assertThat(purchases.state.firstTimeInForeground).isFalse()
        verify(exactly = 0) {
            mockCustomerInfoHelper.retrieveCustomerInfo(
                appUserId,
                CacheFetchPolicy.FETCH_CURRENT,
                false,
                any()
            )
        }
        verify(exactly = 1) {
            mockCache.isCustomerInfoCacheStale(appInBackground = false, appUserID = appUserId)
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
            mockCache.isCustomerInfoCacheStale(appInBackground = false, appUserID = appUserId)
        } returns true
        purchases.state = purchases.state.copy(appInBackground = false, firstTimeInForeground = false)
        Purchases.sharedInstance.onAppForegrounded()
        assertThat(purchases.state.firstTimeInForeground).isFalse()
        verify(exactly = 1) {
            mockCustomerInfoHelper.retrieveCustomerInfo(
                appUserId,
                CacheFetchPolicy.FETCH_CURRENT,
                false,
                any()
            )
        }
        verify(exactly = 1) {
            mockCache.isCustomerInfoCacheStale(appInBackground = false, appUserID = appUserId)
        }
    }

    // endregion

    // region Private Methods
    private fun mockSynchronizeSubscriberAttributesForAllUsers() {
        every {
            mockSubscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(appUserId)
        } just Runs
    }

    private fun mockBillingWrapper() {
        with(mockBillingAbstract) {
            every {
                makePurchaseAsync(any(), any(), any(), any(), any(), any())
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

            every {
                close()
            } answers {
                purchasesUpdatedListener = null
            }
        }
    }

    private fun mockCustomerInfoHelper(errorGettingCustomerInfo: PurchasesError? = null) {
        with(mockCustomerInfoHelper) {
            every {
                retrieveCustomerInfo(any(), any(), false, any())
            } answers {
                val callback  = arg<ReceiveCustomerInfoCallback?>(3)
                if (errorGettingCustomerInfo == null) {
                    callback?.onReceived(mockInfo)
                } else {
                    callback?.onError(errorGettingCustomerInfo)
                }
            }
            every {
                cacheCustomerInfo(any())
            } just runs
            every {
                sendUpdatedCustomerInfoToDelegateIfChanged(any())
            } just runs
            every {
                updatedCustomerInfoListener = any()
            } just runs
            every {
                updatedCustomerInfoListener
            } returns null
        }
    }

    private fun mockBackend() {
        with(mockBackend) {
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
                    marketplace = any(),
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
                getCachedCustomerInfo(any())
            } returns mockInfo
            every {
                cacheCustomerInfo(any(), any())
            } just Runs
            every {
                cacheAppUserID(any())
            } just Runs
            every {
                setCustomerInfoCacheTimestampToNow(appUserId)
            } just Runs
            every {
                setOfferingsCacheTimestampToNow()
            } just Runs
            every {
                clearCustomerInfoCacheTimestamp(appUserId)
            } just Runs
            every {
                clearCustomerInfoCache(appUserId)
            } just Runs
            every {
                clearOfferingsCacheTimestamp()
            } just Runs
            every {
                isCustomerInfoCacheStale(appUserId, any())
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

    private fun mockProducts(response: String = oneOfferingsResponse) {
        every {
            mockBackend.getOfferings(any(), any(), captureLambda(), any())
        } answers {
            lambda<(JSONObject) -> Unit>().captured.invoke(JSONObject(response))
        }
    }

    private fun mockStoreProduct(
        productIds: List<String>,
        productIdsSuccessfullyFetched: List<String>,
        type: ProductType
    ): List<StoreProduct> {
        val storeProducts = productIdsSuccessfullyFetched.map { productId ->
            if (type == ProductType.SUBS) stubStoreProduct(productId, stubSubscriptionOption("p1m", "P1M"))
            else createMockOneTimeProductDetails(productId).toInAppStoreProduct()
        }

        every {
            mockBillingAbstract.queryProductDetailsAsync(
                type,
                productIds.toSet(),
                captureLambda(),
                any()
            )
        } answers {
            lambda<(List<StoreProduct>) -> Unit>().captured.invoke(storeProducts)
        }
        return storeProducts
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

    private fun verifyClose() {
        verify {
            mockBackend.close()
            mockBillingAbstract.close()
        }
        assertThat(purchases.updatedCustomerInfoListener).isNull()
        verify(exactly = 1) {
            mockLifecycle.removeObserver(any())
        }
        verifyOrder {
            mockBillingAbstract.purchasesUpdatedListener = capturedPurchasesUpdatedListener.captured
            mockBillingAbstract.purchasesUpdatedListener = null
        }
    }

    private fun getMockedPurchaseHistoryList(
        productId: String,
        purchaseToken: String,
        productType: ProductType
    ): List<StoreTransaction> {
        val purchaseHistoryRecordWrapper =
            getMockedStoreTransaction(productId, purchaseToken, productType)
        return listOf(purchaseHistoryRecordWrapper)
    }

    private fun getMockedStoreTransaction(
        productId: String,
        purchaseToken: String,
        productType: ProductType
    ): StoreTransaction {
        val p: PurchaseHistoryRecord = stubPurchaseHistoryRecord(
            productIds = listOf(productId),
            purchaseToken = purchaseToken
        )

        return p.toStoreTransaction(productType)
    }

    private fun getMockedPurchaseList(
        productId: String,
        purchaseToken: String,
        productType: ProductType,
        offeringIdentifier: String? = null,
        purchaseState: Int = Purchase.PurchaseState.PURCHASED,
        acknowledged: Boolean = false,
        subscriptionOptionId: String? = null
    ): List<StoreTransaction> {
        val p = stubGooglePurchase(
            productIds = listOf(productId),
            purchaseToken = purchaseToken,
            purchaseState = purchaseState,
            acknowledged = acknowledged
        )

        return listOf(p.toStoreTransaction(productType, offeringIdentifier, subscriptionOptionId))
    }

    private fun mockSuccessfulQueryPurchases(
        queriedSUBS: Map<String, StoreTransaction>,
        queriedINAPP: Map<String, StoreTransaction>,
        notInCache: List<StoreTransaction>
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
            lambda<(Map<String, StoreTransaction>) -> Unit>().captured(purchasesByHashedToken)
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
            ),
            customerInfoHelper = mockCustomerInfoHelper
        )
        Purchases.sharedInstance = purchases
        purchases.state = purchases.state.copy(appInBackground = false)
    }

    private fun Int.buildResult(): BillingResult {
        return BillingResult.newBuilder().setResponseCode(this).build()
    }

    private fun mockPostReceiptError(
        productId: String,
        purchaseToken: String,
        observerMode: Boolean,
        offeringIdentifier: String?,
        type: ProductType,
        answer: MockKAnswerScope<Unit, Unit>.(Call) -> Unit,
        isRestore: Boolean = false
    ) {
        val receiptInfo = mockQueryingProductDetails(productId, type, offeringIdentifier)

        every {
            mockBackend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserId,
                isRestore = isRestore,
                observerMode = observerMode,
                subscriberAttributes = emptyMap(),
                receiptInfo = if (isRestore) ReceiptInfo(productIDs = listOf(productId)) else receiptInfo,
                storeAppUserID = null,
                onSuccess = any(),
                onError = captureLambda()
            )
        } answers answer
    }

    private fun mockPostReceipt(
        productId: String,
        purchaseToken: String,
        observerMode: Boolean,
        mockInfo: CustomerInfo,
        offeringIdentifier: String?,
        type: ProductType,
        restore: Boolean = false
    ): ReceiptInfo {
        val receiptInfo = mockQueryingProductDetails(productId, type, offeringIdentifier)

        every {
            mockBackend.postReceiptData(
                purchaseToken = purchaseToken,
                appUserID = appUserId,
                isRestore = restore,
                observerMode = observerMode,
                subscriberAttributes = emptyMap(),
                receiptInfo = if (restore) ReceiptInfo(productIDs = listOf(productId)) else receiptInfo,
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

    private fun mockQueryingProductDetails(
        productId: String,
        type: ProductType,
        offeringIdentifier: String?
    ): ReceiptInfo {
        if (type == ProductType.SUBS) {
            val productDetails = createMockProductDetailsFreeTrial(productId, 2.00)

            val defaultOfferDetails = productDetails.subscriptionOfferDetails?.first { it.isBasePlan }
            val storeProduct = productDetails.toStoreProduct(
                productDetails.subscriptionOfferDetails!!,
                defaultOfferDetails
            )

            return mockQueryingProductDetails(storeProduct, offeringIdentifier)
        } else {
            val productDetails = createMockOneTimeProductDetails(productId, 2.00)
            val storeProduct = productDetails.toInAppStoreProduct()

            return mockQueryingProductDetails(storeProduct, offeringIdentifier)
        }
    }

    private fun mockQueryingProductDetails(
        storeProduct: StoreProduct,
        offeringIdentifier: String?
    ): ReceiptInfo {
        val receiptInfo = ReceiptInfo(
            productIDs = listOf(storeProduct.productId),
            offeringIdentifier = offeringIdentifier,
            storeProduct = storeProduct
        )

        every {
            mockBillingAbstract.queryProductDetailsAsync(
                storeProduct.type,
                setOf(storeProduct.productId),
                captureLambda(),
                any()
            )
        } answers {
            lambda<(List<StoreProduct>) -> Unit>().captured.invoke(listOf(storeProduct))
        }

        return receiptInfo
    }

    private fun mockCacheStale(
        customerInfoStale: Boolean = false,
        offeringsStale: Boolean = false,
        appInBackground: Boolean = false
    ) {
        every {
            mockCache.isCustomerInfoCacheStale(appUserId, appInBackground)
        } returns customerInfoStale
        every {
            mockCache.isOfferingsCacheStale(appInBackground)
        } returns offeringsStale
    }

    private fun mockSubscriberAttributesManager(userIdToUse: String) {
        every {
            mockSubscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(appUserId)
        } just Runs
        every {
            mockSubscriberAttributesManager.getUnsyncedSubscriberAttributes(userIdToUse, captureLambda())
        } answers {
            lambda<(Map<String, SubscriberAttribute>) -> Unit>().captured.also {
                it.invoke(emptyMap())
            }
        }
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

    private fun mockPurchaseFound(error: PurchasesError? = null): StoreTransaction {
        val oldProductId = "oldProductId"
        val oldPurchase = getMockedStoreTransaction(
            productId = oldProductId,
            purchaseToken = "another_purchase_token",
            productType = ProductType.SUBS
        )

        every {
            mockBillingAbstract.findPurchaseInPurchaseHistory(
                appUserId,
                ProductType.SUBS,
                oldProductId,
                if (error == null) captureLambda() else any(),
                if (error != null) captureLambda() else any()
            )
        } answers {
            if (error != null) {
                lambda<(PurchasesError) -> Unit>().captured.invoke(error)
            } else {
                lambda<(StoreTransaction) -> Unit>().captured.invoke(oldPurchase)
            }
        }
        return oldPurchase
    }

    private fun setUpMockBillingClientBuilderAndListener(
        mockLocalBillingClient: BillingClient
    ): CapturingSlot<BillingClientStateListener> {
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

    private fun mockHandlerPost() {
        mockkConstructor(Handler::class)
        val lst = slot<Runnable>()
        every {
            anyConstructed<Handler>().post(capture(lst))
        } answers {
            lst.captured.run()
            true
        }
    }

    private fun mockIdentityManagerLogout(error: PurchasesError? = null) {
        every {
            mockIdentityManager.logOut(captureLambda())
        } answers {
            lambda<(PurchasesError?) -> Unit>().captured.invoke(error)
        }
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

    private fun stubOfferings(storeProduct: StoreProduct): Pair<StoreProduct, Offerings> {
        val jsonObject = JSONObject(oneOfferingsResponse)
        val packageObject = Package(
            "\$rc_monthly",
            PackageType.MONTHLY,
            storeProduct,
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
        return Pair(storeProduct, offerings)
    }

    private fun stubOfferings(productId: String): Pair<StoreProduct, Offerings> {
        val storeProduct = stubStoreProduct(productId)
        return stubOfferings(storeProduct)
    }

// endregion
}
