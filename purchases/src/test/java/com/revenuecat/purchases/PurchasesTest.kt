//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.app.Activity
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
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.CustomerInfoFactory
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.google.billingResponseToPurchasesError
import com.revenuecat.purchases.google.toInAppStoreProduct
import com.revenuecat.purchases.google.toStoreProduct
import com.revenuecat.purchases.google.toStoreTransaction
import com.revenuecat.purchases.interfaces.GetStoreProductsCallback
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.models.BillingFeature
import com.revenuecat.purchases.models.GoogleProrationMode
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.utils.Responses
import com.revenuecat.purchases.utils.STUB_OFFERING_IDENTIFIER
import com.revenuecat.purchases.utils.createMockOneTimeProductDetails
import com.revenuecat.purchases.utils.createMockProductDetailsFreeTrial
import com.revenuecat.purchases.utils.stubFreeTrialPricingPhase
import com.revenuecat.purchases.utils.stubGooglePurchase
import com.revenuecat.purchases.utils.stubINAPPStoreProduct
import com.revenuecat.purchases.utils.stubOTPOffering
import com.revenuecat.purchases.utils.stubOfferings
import com.revenuecat.purchases.utils.stubPricingPhase
import com.revenuecat.purchases.utils.stubStoreProduct
import com.revenuecat.purchases.utils.stubSubscriptionOption
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyAll
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.AssertionsForClassTypes
import org.json.JSONObject
import org.junit.Assert.fail
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
internal class PurchasesTest: BasePurchasesTest() {
    private val mockActivity: Activity = mockk()
    private var receivedProducts: List<StoreProduct>? = null

    private val inAppProductId = "inapp"
    private val inAppPurchaseToken = "token_inapp"
    private val subProductId = "sub"
    private val subPurchaseToken = "token_sub"
    private val subscriptionOptionId = "mock-base-plan-id:mock-offer-id"

    private val mockLifecycle = mockk<Lifecycle>()
    private val mockLifecycleOwner = mockk<LifecycleOwner>()

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
    fun `diagnostics is synced if needed on constructor`() {
        verify(exactly = 1) { mockDiagnosticsSynchronizer.syncDiagnosticsFileIfNeeded() }
    }

    @Test
    fun getsSubscriptionSkus() {
        val skus = listOf("onemonth_freetrial")

        val skuDetails = mockStoreProduct(skus, listOf(), ProductType.SUBS)

        purchases.getSubscriptionSkus(skus,
            object : GetStoreProductsCallback {
                override fun onReceived(storeProducts: List<StoreProduct>) {
                    receivedProducts = storeProducts
                }

                override fun onError(error: PurchasesError) {
                    fail("shouldn't be error")
                }
            })

        assertThat(receivedProducts).isEqualTo(skuDetails)
    }

    @Test
    fun canOverrideAnonMode() {
        purchases.allowSharingPlayStoreAccount = true

        val productId = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        mockQueryingProductDetails(productId, ProductType.SUBS, null)
        val transactions = getMockedPurchaseList(productId, purchaseToken, ProductType.SUBS)

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(transactions)

        verify {
            mockPostReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = transactions[0],
                storeProduct = any(),
                isRestore = true,
                appUserID = appUserId,
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
    fun `when setting listener, we set customer info updater listener`() {
        purchases.updatedCustomerInfoListener = updatedCustomerInfoListener

        verify(exactly = 1) {
            mockCustomerInfoUpdateHandler.updatedCustomerInfoListener = updatedCustomerInfoListener
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
            mockCustomerInfoUpdateHandler.updatedCustomerInfoListener = null
        }
        verify(exactly = 1) {
            mockCustomerInfoUpdateHandler.updatedCustomerInfoListener = updatedCustomerInfoListener
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

    @Test
    fun `Setting store in the configuration sets it on the Purchases instance`() {
        val builder = PurchasesConfiguration.Builder(mockContext, "api").store(Store.PLAY_STORE)
        Purchases.configure(builder.build())
        assertThat(Purchases.sharedInstance.store).isEqualTo(Store.PLAY_STORE)
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
        val subProductIds = listOf("onemonth_freetrial")
        val inappProductIds = listOf("normal_purchase")
        val productIds = subProductIds + inappProductIds

        val subStoreProducts = mockStoreProduct(productIds, subProductIds, ProductType.SUBS)
        val inappStoreProductsFilteredOut = mockStoreProduct(productIds, inappProductIds, ProductType.INAPP)

        purchases.getProducts(
            productIds,
            ProductType.SUBS,
            object : GetStoreProductsCallback {
                override fun onReceived(storeProducts: List<StoreProduct>) {
                    receivedProducts = storeProducts
                }

                override fun onError(error: PurchasesError) {
                    fail("shouldn't be error")
                }
            }
        )

        assertThat(receivedProducts).isEqualTo(subStoreProducts)
        assertThat(receivedProducts?.size).isEqualTo(subStoreProducts.size)
    }

    @Test
    fun getsNonSubscriptionProducts() {
        val subProductIds = listOf("onemonth_freetrial")
        val inappProductIds = listOf("normal_purchase")
        val productIds = subProductIds + inappProductIds

        val subStoreProductsFilteredOut = mockStoreProduct(productIds, subProductIds, ProductType.SUBS)
        val inappStoreProducts = mockStoreProduct(productIds, inappProductIds, ProductType.INAPP)

        purchases.getProducts(
            productIds,
            ProductType.INAPP,
            object : GetStoreProductsCallback {
                override fun onReceived(storeProducts: List<StoreProduct>) {
                    receivedProducts = storeProducts
                }

                override fun onError(error: PurchasesError) {
                    fail("shouldn't be error")
                }
            }
        )

        assertThat(receivedProducts).isEqualTo(inappStoreProducts)
        assertThat(receivedProducts?.size).isEqualTo(inappStoreProducts.size)
    }

    // endregion

    // region purchasing

    @Test
    fun `when making product change, completion block is called`() {
        val productId = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        val receiptInfo = mockQueryingProductDetails(productId, ProductType.SUBS, null)

        val oldPurchase = mockPurchaseFound()

        var callCount = 0

        val productChangeParams = getPurchaseParams(
            receiptInfo.storeProduct!!.subscriptionOptions!!.first(),
            oldPurchase.productIds.first()
        )
        purchases.purchaseWith(
            productChangeParams,
            onError = { _, _ ->
                fail("should be successful")
            }
        ) { _, _ ->
            callCount++
        }

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(productId, purchaseToken, ProductType.SUBS)
        )
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun `when making a deferred product change using the deprecated method, completion is called with the old product`() {
        val newProductId = listOf("newproduct")
        val receiptInfo = mockQueryingProductDetails(newProductId.first(), ProductType.SUBS, null)
        val oldPurchase = mockPurchaseFound()
        mockQueryingProductDetails(oldPurchase.productIds.first(), ProductType.SUBS, null)
        var callCount = 0
        purchases.purchaseProductWith(
            mockActivity,
            receiptInfo.storeProduct!!,
            UpgradeInfo(oldPurchase.productIds[0], ProrationMode.DEFERRED),
            onError = { _, _ ->
                fail("should be successful")
            }, onSuccess = { purchase, _ ->
                callCount++
                assertThat(purchase).isEqualTo(oldPurchase)
            })
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(listOf(oldPurchase))
        assertThat(callCount).isEqualTo(1)
        verify(exactly = 1) {
            mockPostReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = oldPurchase,
                storeProduct = any(),
                isRestore = false,
                appUserID = appUserId,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `when making a deferred product change, completion is called with the transaction for the old product`() {
        val newProductId = listOf("newproduct")
        val storeProduct = mockStoreProduct(newProductId, newProductId, ProductType.SUBS)
        val oldPurchase = mockPurchaseFound()
        mockQueryingProductDetails(oldPurchase.productIds.first(), ProductType.SUBS, null)
        every {
            mockPostReceiptHelper.postTransactionAndConsumeIfNeeded(oldPurchase, any(), false, appUserId, captureLambda(), any())
        } answers {
            lambda<SuccessfulPurchaseCallback>().captured.invoke(oldPurchase, mockk())
        }
        val productChangeParams = getPurchaseParams(
            storeProduct.first().subscriptionOptions!!.first(),
            oldPurchase.productIds.first(),
            googleProrationMode = GoogleProrationMode.DEFERRED
        )
        var callCount = 0
        purchases.purchaseWith(
            productChangeParams,
            onError = { _, _ ->
                fail("should be successful")
            },
            onSuccess = { purchase, _ ->
                callCount++
                assertThat(purchase).isEqualTo(oldPurchase)
            }
        )
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(listOf(oldPurchase))
        assertThat(callCount).isEqualTo(1)
        verify(exactly = 1) {
            mockPostReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = oldPurchase,
                storeProduct = any(),
                isRestore = false,
                appUserID = appUserId,
                onSuccess = any(),
                onError = any()
            )
        }
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

        val upgradePurchaseParams =
            getPurchaseParams(receiptInfo.storeProduct!!.defaultOption!!, oldSubId)
        purchases.purchaseWith(
            upgradePurchaseParams,
            onError = { _, _ ->
            }) { _, _ ->

        }

        val expectedReplaceProductInfo = ReplaceProductInfo(
            oldTransaction,
            GoogleProrationMode.IMMEDIATE_WITHOUT_PRORATION
        )
        verify {
            mockBillingAbstract.makePurchaseAsync(
                any(),
                any(),
                receiptInfo.storeProduct!!.defaultOption!!.purchasingData,
                expectedReplaceProductInfo,
                any(),
                any()
            )
        }
    }

    fun `deprecated upgrade defaults ProrationMode to null if not passed`() {
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
            null
        )
        verify {
            mockBillingAbstract.makePurchaseAsync(
                any(),
                any(),
                receiptInfo.storeProduct!!.defaultOption!!.purchasingData,
                expectedReplaceProductInfo,
                any()
            )
        }
    }

    fun `deprecated purchase does not set isPersonalizedPrice`() {
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

        verify {
            mockBillingAbstract.makePurchaseAsync(
                any(),
                any(),
                receiptInfo.storeProduct!!.defaultOption!!.purchasingData,
                any(),
                null
            )
        }
    }

    @Test
    fun canMakePurchase() {
        val storeProduct = stubStoreProduct("abc")
        val purchaseOptionParams = getPurchaseParams(storeProduct.subscriptionOptions!!.first())
        purchases.purchaseWith(
            purchaseOptionParams
        ) { _, _ -> }

        verify {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                storeProduct.subscriptionOptions!!.first().purchasingData,
                null,
                null,
                any()
            )
        }
    }

    @Test
    fun canMakePurchaseWithoutProvidingOption() {
        val storeProduct = stubStoreProduct("productId")
        val purchaseProductParams = getPurchaseParams(storeProduct)
        purchases.purchaseWith(
            purchaseProductParams
        ) { _, _ -> }

        verify {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                storeProduct.purchasingData,
                null,
                null,
                any()
            )
        }
    }

    @Test
    fun canMakePurchaseOfAPackage() {
        val (_, offerings) = stubOfferings("onemonth_freetrial")
        val packageToPurchase = offerings[STUB_OFFERING_IDENTIFIER]!!.monthly!!
        val purchasePackageParams = getPurchaseParams(packageToPurchase)
        purchases.purchaseWith(
            purchasePackageParams
        ) { _, _ -> }

        verify {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                packageToPurchase.product.defaultOption!!.purchasingData,
                null,
                STUB_OFFERING_IDENTIFIER,
                any()
            )
        }
    }

    @Test
    fun `purchase of sub Package passes presentedOfferingIdentifier through to purchase`() {
        val (storeProduct, offerings) = stubOfferings("onemonth_freetrial")
        val expectedOfferingIdentifier = STUB_OFFERING_IDENTIFIER

        val packageToPurchase = offerings[STUB_OFFERING_IDENTIFIER]!!.monthly!!
        val purchaseParams = PurchaseParams.Builder(
            mockActivity,
            packageToPurchase
        )
        purchases.purchaseWith(purchaseParams.build()) { _, _ -> }

        verify {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                storeProduct.subscriptionOptions!!.first().purchasingData,
                null,
                expectedOfferingIdentifier,
                any()
            )
        }
    }

    @Test
    fun `purchase of sub StoreProduct passes presentedOfferingIdentifier through to purchase`() {
        val (_, offerings) = stubOfferings("onemonth_freetrial")
        val expectedOfferingIdentifier = STUB_OFFERING_IDENTIFIER

        val storeProduct = offerings[STUB_OFFERING_IDENTIFIER]!!.monthly!!.product.copyWithOfferingId(
            expectedOfferingIdentifier
        )
        val purchaseParams = PurchaseParams.Builder(
            mockActivity,
            storeProduct
        )
        purchases.purchaseWith(
            purchaseParams.build()
        ) { _, _ -> }

        verify {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                storeProduct.subscriptionOptions!!.first().purchasingData,
                null,
                expectedOfferingIdentifier,
                any()
            )
        }
    }

    @Test
    fun `purchase of SubscriptionOption passes presentedOfferingIdentifier through to purchase`() {
        val (storeProduct, offerings) = stubOfferings("onemonth_freetrial")
        val expectedOfferingIdentifier = STUB_OFFERING_IDENTIFIER

        val option =
            offerings[STUB_OFFERING_IDENTIFIER]!!.monthly!!.product.copyWithOfferingId(expectedOfferingIdentifier).defaultOption!!
        val purchaseParams = PurchaseParams.Builder(
            mockActivity,
            option
        )
        purchases.purchaseWith(
            purchaseParams.build()
        ) { _, _ -> }

        verify {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                storeProduct.subscriptionOptions!!.first().purchasingData,
                null,
                expectedOfferingIdentifier,
                any()
            )
        }
    }

    fun `purchase of OTP Package passes presentedOfferingIdentifier through to purchase`() {
        val expectedOfferingIdentifier = STUB_OFFERING_IDENTIFIER
        val stubOtpProduct = stubINAPPStoreProduct(
            "tokens",
            expectedOfferingIdentifier
        )

        val (_, stubOTPOffering) = stubOTPOffering(stubOtpProduct)

        val packageToPurchase = stubOTPOffering[expectedOfferingIdentifier]!!.availablePackages.get(0)
        val purchaseParams = PurchaseParams.Builder(
            mockActivity,
            packageToPurchase
        )
        purchases.purchaseWith(purchaseParams.build()) { _, _ -> }

        verify {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                packageToPurchase.product.purchasingData,
                null,
                expectedOfferingIdentifier,
                any()
            )
        }
    }

    @Test
    fun `purchase of OTP StoreProduct passes presentedOfferingIdentifier through to purchase`() {
        val expectedOfferingIdentifier = STUB_OFFERING_IDENTIFIER
        val stubOtpProduct = stubINAPPStoreProduct(
            "tokens",
            expectedOfferingIdentifier
        )

        val (_, stubOTPOffering) = stubOTPOffering(stubOtpProduct)

        val purchaseParams = PurchaseParams.Builder(
            mockActivity,
            stubOtpProduct
        )
        purchases.purchaseWith(purchaseParams.build()) { _, _ -> }

        verify {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                stubOtpProduct.purchasingData,
                null,
                expectedOfferingIdentifier,
                any()
            )
        }
    }

    @Test
    fun canMakePurchaseUpgradeOfAPackage() {
        val (storeProduct, offerings) = stubOfferings("onemonth_freetrial")
        val oldPurchase = mockPurchaseFound()
        val purchasePackageUpgradeParams = getPurchaseParams(
            offerings[STUB_OFFERING_IDENTIFIER]!!.monthly!!,
            oldPurchase.productIds[0]
        )

        purchases.purchaseWith(
            purchasePackageUpgradeParams
        ) { _, _ -> }

        verify {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                storeProduct.defaultOption!!.purchasingData,
                ReplaceProductInfo(oldPurchase, GoogleProrationMode.IMMEDIATE_WITHOUT_PRORATION),
                STUB_OFFERING_IDENTIFIER,
                any()
            )
        }
    }

    @Test
    fun `receiving new transactions in listener, posts them to the backend`() {
        mockQueryingProductDetails(inAppProductId, ProductType.INAPP, null, null)
        mockQueryingProductDetails(subProductId, ProductType.SUBS, null, subscriptionOptionId)

        val mockedInApps = getMockedPurchaseList(inAppProductId, inAppPurchaseToken, ProductType.INAPP)
        val mockedSubs = getMockedPurchaseList(subProductId, subPurchaseToken, ProductType.SUBS, "offering_a")
        val allPurchases = mockedInApps + mockedSubs

        allPurchases.forEach { transaction ->
            every {
                mockPostReceiptHelper.postTransactionAndConsumeIfNeeded(transaction, any(), false, appUserId, captureLambda(), any())
            } answers {
                lambda<SuccessfulPurchaseCallback>().captured.invoke(transaction, mockk())
            }
        }

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(allPurchases)

        verifyAll {
            mockPostReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = mockedInApps[0],
                storeProduct = match { it.purchasingData.productId == inAppProductId },
                isRestore = false,
                appUserID = appUserId,
                onSuccess = any(),
                onError = any()
            )
            mockPostReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = mockedSubs[0],
                storeProduct = match { it.purchasingData.productId == subProductId },
                isRestore = false,
                appUserID = appUserId,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun doesntPostIfNotOK() {
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError)
        capturedPurchasesUpdatedListener.captured.onPurchasesFailedToUpdate(error)
        verify(exactly = 0) {
            mockPostReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = any(),
                storeProduct = any(),
                isRestore = any(),
                appUserID = any(),
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun passesUpErrors() {
        var errorCalled = false
        val storeProduct = stubStoreProduct("productId")
        val purchaseParams = getPurchaseParams(storeProduct.subscriptionOptions!!.first())
        purchases.purchaseWith(
            purchaseParams,
            onError = { error, _ ->
                errorCalled = true
                assertThat(error.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
            }) { _, _ -> }

        val error = PurchasesError(PurchasesErrorCode.StoreProblemError)
        capturedPurchasesUpdatedListener.captured.onPurchasesFailedToUpdate(error)
        assertThat(errorCalled).isTrue
    }

    @Test
    fun `when making another purchase for a product for a pending product, error is issued`() {
        purchases.updatedCustomerInfoListener = updatedCustomerInfoListener

        val storeProduct = stubStoreProduct("productId")
        val purchaseParams = getPurchaseParams(storeProduct.subscriptionOptions!!.first())
        purchases.purchaseWith(
            purchaseParams,
            onError = { _, _ -> fail("Should be success") }) { _, _ ->
            // First one works
        }

        var errorCalled: PurchasesError? = null
        purchases.purchaseWith(
            purchaseParams,
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
        val purchaseParams = getPurchaseParams(storeProduct.subscriptionOptions!!.first())
        var callCount = 0
        purchases.purchaseWith(
            purchaseParams,
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
        val purchaseParams = getPurchaseParams(storeProduct.subscriptionOptions!!.first())
        purchases.purchaseWith(
            purchaseParams,
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
        val purchaseParams = getPurchaseParams(storeProduct.subscriptionOptions!!.first())
        purchases.purchaseWith(
            purchaseParams
        ) { _, _ -> }

        purchases.purchaseWith(
            purchaseParams
        ) { _, _ -> }

        try {
            val error = PurchasesError(PurchasesErrorCode.StoreProblemError)
            capturedPurchasesUpdatedListener.captured.onPurchasesFailedToUpdate(error)
        } catch (e: ConcurrentModificationException) {
            fail("Test throws ConcurrentModificationException")
        }
    }

    @Test
    fun `when making product change purchase, error is forwarded`() {
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

        val purchaseUpgradeParams = getPurchaseParams(
            receiptInfo.storeProduct!!.subscriptionOptions!!.first(),
            oldPurchase.productIds[0]
        )

        purchases.purchaseWith(
            purchaseUpgradeParams,
            onError = { purchaseError, userCancelled ->
                receivedError = purchaseError
                receivedUserCancelled = userCancelled
            }) { _, _ ->
            fail("should be error")
        }

        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedUserCancelled).isFalse
    }

    @Test
    fun `no OperationAlreadyInProgress error if first purchase already returned error`() {
        val productId = "onemonth_freetrial"
        val receiptInfo = mockQueryingProductDetails(productId, ProductType.SUBS, null)

        val error = PurchasesError(PurchasesErrorCode.PurchaseInvalidError, PurchaseStrings.NO_EXISTING_PURCHASE)

        // force failure before starting purchase with store by ensuring old purchase not found
        val oldPurchase = mockPurchaseFound(error)

        var receivedError: PurchasesError? = null
        val purchaseParams = getPurchaseParams(
            receiptInfo.storeProduct!!.subscriptionOptions!!.first(), oldPurchase.productIds[0]
        )
        purchases.purchaseWith(
            purchaseParams,
            onError = { _, _ -> }
        ) { _, _ ->
            fail("should be error")
        }

        purchases.purchaseWith(
            purchaseParams,
            onError = { error, _ ->
                receivedError = error
            }
        ) { _, _ -> }
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isNotEqualTo(PurchasesErrorCode.OperationAlreadyInProgressError)
    }

    @Test
    fun `when making product change purchase, failures purchasing are forwarded`() {
        val productId = "onemonth_freetrial"

        val receiptInfo = mockQueryingProductDetails(productId, ProductType.SUBS, null)

        val oldPurchase = mockPurchaseFound()

        var receivedError: PurchasesError? = null
        var receivedUserCancelled: Boolean? = null

        val purchaseParams = getPurchaseParams(
            receiptInfo.storeProduct!!.subscriptionOptions!!.first(), oldPurchase.productIds[0]
        )
        purchases.purchaseWith(
            purchaseParams,
            onError = { error, userCancelled ->
                receivedError = error
                receivedUserCancelled = userCancelled
            }) { _, _ ->
            fail("should be error")
        }

        val error = PurchasesError(PurchasesErrorCode.StoreProblemError)
        capturedPurchasesUpdatedListener.captured.onPurchasesFailedToUpdate(error)


        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedUserCancelled).isFalse
    }

    @Test
    fun `when purchasing a product with multiple subscription options, we choose the default`() {
        val productId = "onemonth_freetrial"

        val basePlanSubscriptionOption = stubSubscriptionOption("base-plan-purchase-option", productId)
        val expectedDefaultSubscriptionOption = stubSubscriptionOption(
            "free-trial-purchase-option",
            pricingPhases = listOf(stubFreeTrialPricingPhase(), stubPricingPhase()),
            productId = productId
        )
        val storeProduct = stubStoreProduct(
            productId = productId,
            defaultOption = expectedDefaultSubscriptionOption,
            subscriptionOptions = listOf(expectedDefaultSubscriptionOption, basePlanSubscriptionOption)
        )

        mockQueryingProductDetails(storeProduct, null)
        val purchaseProductParams = getPurchaseParams(storeProduct)

        purchases.purchaseWith(
            purchaseProductParams,
            onError = { _, _ -> }
        ) { _, _ -> }

        verify(exactly = 1) {
            mockBillingAbstract.makePurchaseAsync(
                mockActivity,
                appUserId,
                expectedDefaultSubscriptionOption.purchasingData,
                null,
                null,
                any()
            )
        }
    }

    @Test
    fun `when purchasing a package with multiple subscription options, we choose the default`() {
        val productId = "onemonth_freetrial"

        val basePlanSubscriptionOption = stubSubscriptionOption("base-plan-purchase-option", productId)
        val expectedDefaultSubscriptionOption = stubSubscriptionOption(
            "free-trial-purchase-option",
            pricingPhases = listOf(stubFreeTrialPricingPhase(), stubPricingPhase()),
            productId = productId
        )
        val storeProduct = stubStoreProduct(
            productId = productId,
            defaultOption = expectedDefaultSubscriptionOption,
            subscriptionOptions = listOf(expectedDefaultSubscriptionOption, basePlanSubscriptionOption)
        )
        val (_, offerings) = stubOfferings(storeProduct)

        mockQueryingProductDetails(storeProduct, null)
        val packageToPurchase = offerings[STUB_OFFERING_IDENTIFIER]!!.monthly!!
        val purchasePackageParams = getPurchaseParams(packageToPurchase)
        purchases.purchaseWith(
            purchasePackageParams,
            onError = { _, _ -> }
        ) { _, _ -> }

        verify(exactly = 1) {
            mockBillingAbstract.makePurchaseAsync(
                mockActivity,
                appUserId,
                expectedDefaultSubscriptionOption.purchasingData,
                null,
                STUB_OFFERING_IDENTIFIER,
                any()
            )
        }
    }

    @Test
    fun `when purchasing a package as product change, completion block is called`() {
        val productId = "onemonth_freetrial"

        val (_, offerings) = stubOfferings(productId)
        mockQueryingProductDetails(productId, ProductType.SUBS, null)

        val purchaseToken = "crazy_purchase_token"

        val oldPurchase = mockPurchaseFound()

        var callCount = 0

        val purchasePackageUpgradeParams = getPurchaseParams(
            offerings[STUB_OFFERING_IDENTIFIER]!!.monthly!!,
            oldPurchase.productIds[0]
        )
        purchases.purchaseWith(
            purchasePackageUpgradeParams,
            onError = { _, _ ->
                fail("should be successful")
            }
        ) { _, _ ->
            callCount++
        }
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(
            getMockedPurchaseList(
                offerings[STUB_OFFERING_IDENTIFIER]!!.monthly!!.product.id,
                purchaseToken,
                ProductType.SUBS
            )
        )
        assertThat(callCount).isEqualTo(1)
    }



    @Test
    fun `when purchasing a package as product change, error is forwarded`() {
        val productId = "onemonth_freetrial"
        val (_, offerings) = stubOfferings(productId)

        mockQueryingProductDetails(productId, ProductType.SUBS, null)

        val stubBillingResult = mockk<BillingResult>()
        every { stubBillingResult.responseCode } returns BillingClient.BillingResponseCode.ERROR

        val underlyingErrorMessage = PurchaseStrings.ERROR_FINDING_PURCHASE.format("oldProductId")
        val error = stubBillingResult.responseCode.billingResponseToPurchasesError(underlyingErrorMessage)

        val oldPurchase = mockPurchaseFound(error)

        var receivedError: PurchasesError? = null
        var receivedUserCancelled: Boolean? = null
        val purchasePackageUpgradeParams = getPurchaseParams(
            offerings[STUB_OFFERING_IDENTIFIER]!!.monthly!!,
            oldPurchase.productIds[0]
        )
        purchases.purchaseWith(
            purchasePackageUpgradeParams,
            onError = { error, userCancelled ->
                receivedError = error
                receivedUserCancelled = userCancelled
            }
        ) { _, _ ->
            fail("should be error")
        }
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedUserCancelled).isFalse
    }

    @Test
    fun `when purchasing a package as product change, failures purchasing are forwarded`() {
        val productId = "onemonth_freetrial"
        val (_, offerings) = stubOfferings(productId)

        val oldPurchase = mockPurchaseFound()

        var receivedError: PurchasesError? = null
        var receivedUserCancelled: Boolean? = null
        val purchasePackageUpgradeParams =
            getPurchaseParams(
                offerings[STUB_OFFERING_IDENTIFIER]!!.monthly!!,
                oldPurchase.productIds[0]
            )
        purchases.purchaseWith(
            purchasePackageUpgradeParams,
            onError = { error, userCancelled ->
                receivedError = error
                receivedUserCancelled = userCancelled
            }
        ) { _, _ ->
            fail("should be error")
        }

        val error = PurchasesError(PurchasesErrorCode.StoreProblemError)
        capturedPurchasesUpdatedListener.captured.onPurchasesFailedToUpdate(error)

        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedUserCancelled).isFalse
    }

    @Test
    fun `Send error if cannot find the old purchase associated when upgrading a productId`() {
        val (storeProduct, offerings) = stubOfferings("onemonth_freetrial")

        val message = PurchaseStrings.NO_EXISTING_PURCHASE
        val error = PurchasesError(PurchasesErrorCode.PurchaseInvalidError, message)

        val oldPurchase = mockPurchaseFound(error)

        var receivedError: PurchasesError? = null
        var receivedUserCancelled: Boolean? = null

        val purchasePackageUpgradeParams = getPurchaseParams(
            offerings[STUB_OFFERING_IDENTIFIER]!!.monthly!!,
            oldPurchase.productIds[0]
        )
        purchases.purchaseWith(
            purchasePackageUpgradeParams,
            onError = { error, userCancelled ->
                receivedError = error
                receivedUserCancelled = userCancelled
            }
        ) { _, _ -> }

        verify(exactly = 0) {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                storeProduct.subscriptionOptions!!.first().purchasingData,
                ReplaceProductInfo(oldPurchase, GoogleProrationMode.IMMEDIATE_WITHOUT_PRORATION),
                STUB_OFFERING_IDENTIFIER,
                any()
            )
        }
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
        assertThat(receivedUserCancelled).isFalse
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

        val purchasePackageUpgradeParams = getPurchaseParams(
            offerings[STUB_OFFERING_IDENTIFIER]!!.monthly!!,
            oldPurchase.productIds[0]
        )
        purchases.purchaseWith(
            purchasePackageUpgradeParams,
            onError = { error, userCancelled ->
                receivedError = error
                receivedUserCancelled = userCancelled
            }
        ) { _, _ -> }

        verify(exactly = 0) {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                storeProduct.subscriptionOptions!!.first().purchasingData,
                ReplaceProductInfo(oldPurchase, GoogleProrationMode.IMMEDIATE_WITHOUT_PRORATION),
                STUB_OFFERING_IDENTIFIER,
                any()
            )
        }
        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
        assertThat(receivedUserCancelled).isFalse()
    }

    @Test
    fun `product not found when querying productId details while purchasing still posts product`() {
        val productId = "productId"
        val purchaseToken = "token"

        mockStoreProduct(listOf(productId), emptyList(), ProductType.INAPP)

        val transactions = getMockedPurchaseList(
            productId,
            purchaseToken,
            ProductType.INAPP,
            "offering_a"
        )

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(transactions)
        val productInfo = ReceiptInfo(
            productIDs = listOf(productId),
            offeringIdentifier = "offering_a"
        )
        verify(exactly = 1) {
            mockPostReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = transactions[0],
                storeProduct = null,
                isRestore = false,
                appUserID = appUserId,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun `isPersonalizedPrice value is passed through to purchase`() {
        val storeProduct = stubStoreProduct("abc")
        val expectedPersonalizedPrice = true

        val purchaseParams = getPurchaseParams(storeProduct, null, expectedPersonalizedPrice)

        purchases.purchaseWith(
            purchaseParams
        ) { _, _ -> }

        verify {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                storeProduct.subscriptionOptions!![0].purchasingData,
                null,
                any(),
                expectedPersonalizedPrice
            )
        }
    }

    @Test
    fun `isPersonalizedPrice value is passed through to product change purchase`() {
        val storeProduct = stubStoreProduct("abc")
        val expectedPersonalizedPrice = true
        val oldSubId = "123"

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

        val upgradePurchaseParams = getPurchaseParams(storeProduct, oldSubId, expectedPersonalizedPrice)

        purchases.purchaseWith(
            upgradePurchaseParams
        ) { _, _ -> }

        val expectedReplaceProductInfo = ReplaceProductInfo(
            oldTransaction,
            GoogleProrationMode.IMMEDIATE_WITHOUT_PRORATION
        )

        verify {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                storeProduct.subscriptionOptions!![0].purchasingData,
                expectedReplaceProductInfo,
                any(),
                expectedPersonalizedPrice
            )
        }
    }

    @Test
    fun `isPersonalizedPrice defaults to null for deprecated purchase`() {
        val (_, offerings) = stubOfferings("onemonth_freetrial")
        val packageToPurchase = offerings[STUB_OFFERING_IDENTIFIER]!!.monthly!!

        purchases.purchasePackage(
            mockActivity,
            packageToPurchase,
            object : PurchaseCallback {
                override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {}
                override fun onError(error: PurchasesError, userCancelled: Boolean) {}
            }
        )

        verify {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                packageToPurchase.product.purchasingData,
                null,
                STUB_OFFERING_IDENTIFIER,
                null
            )
        }
    }

    @Test
    fun `isPersonalizedPrice defaults to null for purchase with purchaseparams`() {
        val (_, offerings) = stubOfferings("onemonth_freetrial")
        val packageToPurchase = offerings[STUB_OFFERING_IDENTIFIER]!!.monthly!!

        val purchaseParams = PurchaseParams.Builder(mockActivity, packageToPurchase).build()

        purchases.purchaseWith(
            purchaseParams
        ) { _, _ -> }

        verify {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                packageToPurchase.product.purchasingData,
                null,
                STUB_OFFERING_IDENTIFIER,
                null
            )
        }
    }

    @Test
    fun `isPersonalizedPrice value defaults to null for product change purchase`() {
        val storeProduct = stubStoreProduct("abc")
        val oldSubId = "123"

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

        val upgradePurchaseParams = getPurchaseParams(storeProduct, oldSubId)

        purchases.purchaseWith(
            upgradePurchaseParams
        ) { _, _ -> }

        val expectedReplaceProductInfo = ReplaceProductInfo(
            oldTransaction,
            GoogleProrationMode.IMMEDIATE_WITHOUT_PRORATION
        )

        verify {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                storeProduct.subscriptionOptions!![0].purchasingData,
                expectedReplaceProductInfo,
                any(),
                null
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
        mockSynchronizeSubscriberAttributesForAllUsers()
        mockOfferingsManagerAppForeground()
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
    fun `fetch product entitlement mapping on foreground if it's stale`() {
        mockOfferingsManagerAppForeground()
        Purchases.sharedInstance.onAppForegrounded()
        verify(exactly = 1) {
            mockOfflineEntitlementsManager.updateProductEntitlementMappingCacheIfStale()
        }
    }

    @Test
    fun `does not fetch purchaser info on foregrounded if it's not stale`() {
        mockCacheStale()
        mockSynchronizeSubscriberAttributesForAllUsers()
        purchases.state = purchases.state.copy(firstTimeInForeground = false)
        mockOfferingsManagerAppForeground()
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

    // endregion

    // region offerings

    @Test
    fun `on foreground delegates logic`() {
        mockSynchronizeSubscriberAttributesForAllUsers()
        every { mockOfferingsManager.onAppForeground(any()) } just Runs
        Purchases.sharedInstance.onAppForegrounded()
        verify(exactly = 1) { mockOfferingsManager.onAppForeground(appUserId) }
    }

    @Test
    fun `getOfferings success calls success callback`() {
        val offerings = mockOfferingsManagerGetOfferings()
        var receivedOfferings: Offerings? = null
        Purchases.sharedInstance.getOfferingsWith(
            onError = { fail("Expected success") },
            onSuccess = { receivedOfferings = it }
        )
        assertThat(receivedOfferings).isEqualTo(offerings)
    }

    @Test
    fun `getOfferings error calls error callback`() {
        val error = PurchasesError(PurchasesErrorCode.UnknownError)
        mockOfferingsManagerGetOfferings(error)
        var receivedError: PurchasesError? = null
        Purchases.sharedInstance.getOfferingsWith(
            onError = { receivedError = it },
            onSuccess = { fail("Expected error") }
        )
        assertThat(receivedError).isEqualTo(error)
    }

    // endregion

    // region restoring

    @Test
    fun isRestoreWhenUsingNullAppUserID() {
        anonymousSetup(true)

        val productId = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        mockQueryingProductDetails(productId, ProductType.SUBS, null)
        val transactions = getMockedPurchaseList(productId, purchaseToken, ProductType.SUBS)
        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(transactions)
        verify {
            mockPostReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = transactions[0],
                storeProduct = any(),
                isRestore = true,
                appUserID = randomAppUserId,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun doesntRestoreNormally() {
        val productId = "onemonth_freetrial"
        val purchaseToken = "crazy_purchase_token"

        mockQueryingProductDetails(productId, ProductType.SUBS, null)
        val transactions = getMockedPurchaseList(productId, purchaseToken, ProductType.SUBS)

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(transactions)

        verify {
            mockPostReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = transactions[0],
                storeProduct = any(),
                isRestore = false,
                appUserID = appUserId,
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
    fun historicalPurchasesPassedToBackend() {
        var capturedLambda: ((List<StoreTransaction>) -> Unit)? = null
        val inAppTransactions = getMockedPurchaseHistoryList(inAppProductId, inAppPurchaseToken, ProductType.INAPP)
        val subTransactions = getMockedPurchaseHistoryList(subProductId, subPurchaseToken, ProductType.SUBS)

        every {
            mockBillingAbstract.queryAllPurchases(
                appUserId,
                captureLambda(),
                any()
            )
        } answers {
            capturedLambda = lambda<(List<StoreTransaction>) -> Unit>().captured
            capturedLambda?.invoke(inAppTransactions + subTransactions)
        }

        var restoreCalled = false
        purchases.restorePurchasesWith(onSuccess = {
            restoreCalled = true
        }, onError = {
            fail("Should not be an error")
        })
        assertThat(capturedLambda).isNotNull
        assertThat(restoreCalled).isTrue()

        verifyAll {
            mockPostReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = inAppTransactions[0],
                storeProduct = null,
                isRestore = true,
                appUserID = appUserId,
                onSuccess = any(),
                onError = any()
            )
            mockPostReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = subTransactions[0],
                storeProduct = null,
                isRestore = true,
                appUserID = appUserId,
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

        val mockInfo = CustomerInfoFactory.buildCustomerInfo(
            JSONObject(Responses.validFullPurchaserResponse),
            null,
            VerificationResult.NOT_REQUESTED
        )
        every {
            mockPostReceiptHelper.postTransactionAndConsumeIfNeeded(any(), any(), any(), any(), captureLambda(), any())
        } answers {
            lambda<SuccessfulPurchaseCallback>().captured.invoke(firstArg(), mockInfo)
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
            mockCustomerInfoHelper.retrieveCustomerInfo(appUserID, any(), any(), any(), any())
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
        mockOfferingsManagerFetchOfferings(newAppUserID)

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
        mockOfferingsManagerFetchOfferings(newAppUserID)

        purchases.logIn(newAppUserID, mockCompletion)

        verify(exactly = 1) {
            mockCompletion.onReceived(mockInfo, mockCreated)
        }
    }

    @Test
    fun `login successful with new appUserID calls customer info updater to update delegate if changed`() {
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
        mockOfferingsManagerFetchOfferings(newAppUserID)

        purchases.logIn(newAppUserID, mockCompletion)

        verify(exactly = 1) {
            mockCustomerInfoUpdateHandler.notifyListeners(mockInfo)
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
        mockOfferingsManagerFetchOfferings(newAppUserID)

        purchases.logIn(newAppUserID, mockCompletion)

        verify(exactly = 1) {
            mockOfferingsManager.fetchAndCacheOfferings(newAppUserID, any(), any(), any())
        }
    }

    @Test
    fun `logout called with identified user makes right calls`() {
        val appUserID = "fakeUserID"
        every {
            mockCache.cleanupOldAttributionData()
        } just Runs
        mockIdentityManagerLogout()
        mockOfferingsManagerFetchOfferings()
        val mockCompletion = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        purchases.logOut(mockCompletion)

        verify(exactly = 1) {
            mockCustomerInfoHelper.retrieveCustomerInfo(
                appUserID,
                CacheFetchPolicy.FETCH_CURRENT,
                appInBackground = false,
                allowSharingPlayStoreAccount = false,
                any()
            )
        }
        verify(exactly = 1) {
            mockOfferingsManager.fetchAndCacheOfferings(appUserID, false, any(), any())
        }
    }

    @Test
    fun `when logging out, identity manager is called`() {
        every {
            mockCache.cleanupOldAttributionData()
        } just Runs
        mockIdentityManagerLogout()
        mockOfferingsManagerFetchOfferings()

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
        mockOfferingsManagerFetchOfferings()

        purchases.logOut()
        verify(exactly = 1) {
            mockCustomerInfoHelper.retrieveCustomerInfo(
                appUserId,
                CacheFetchPolicy.FETCH_CURRENT,
                false,
                allowSharingPlayStoreAccount = false,
                null
            )
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
        mockOfferingsManagerFetchOfferings()

        purchases.logOut(mockCompletion)
        verify(exactly = 1) {
            mockCompletion.onReceived(mockInfo)
        }
    }

    @Test
    fun `logOut clears backend caches when successful`() {
        setUp()

        every {
            mockCache.cleanupOldAttributionData()
        } just Runs

        every {
            mockBackend.clearCaches()
        } just Runs

        val mockCompletion = mockk<ReceiveCustomerInfoCallback>(relaxed = true)
        mockIdentityManagerLogout()
        mockOfferingsManagerFetchOfferings()

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
            listOf(
                BillingFeature.SUBSCRIPTIONS,
                BillingFeature.SUBSCRIPTIONS_UPDATE
            )
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
    fun `syncing transactions calls helper with correct parameters`() {
        val allowSharingAccount = true
        val appInBackground = true
        purchases.allowSharingPlayStoreAccount = allowSharingAccount
        purchases.state = purchases.state.copy(appInBackground = appInBackground)

        every { mockSyncPurchasesHelper.syncPurchases(any(), any(), any(), any()) } just Runs

        purchases.syncPurchases()

        verify(exactly = 1) {
            mockSyncPurchasesHelper.syncPurchases(
                allowSharingAccount,
                appInBackground,
                any(),
                any()
            )
        }
    }

    @Test
    fun `syncing transactions calls success callback when process completes successfully`() {
        every {
            mockSyncPurchasesHelper.syncPurchases(any(), any(), captureLambda(), any())
        } answers {
            lambda<(CustomerInfo) -> Unit>().captured.invoke(mockInfo)
        }

        var successCallCount = 0
        var receivedCustomerInfo: CustomerInfo? = null
        purchases.syncPurchasesWith(
            { fail("Expected to succeed") },
            {
                successCallCount++
                receivedCustomerInfo = it
            }
        )

        assertThat(successCallCount).isEqualTo(1)
        assertThat(receivedCustomerInfo).isEqualTo(mockInfo)
    }

    @Test
    fun `syncing transactions calls error callback when process completes with error`() {
        every {
            mockSyncPurchasesHelper.syncPurchases(any(), any(), any(), captureLambda())
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(PurchasesError(PurchasesErrorCode.UnknownError))
        }

        var errorCallCount = 0
        purchases.syncPurchasesWith(
            {
                assertThat(it.code).isEqualTo(PurchasesErrorCode.UnknownError)
                errorCallCount++
            },
            { fail("Expected to error") }
        )

        assertThat(errorCallCount).isEqualTo(1)
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
            mockPostReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = purchaseToken,
                storeUserID = amazonUserID,
                receiptInfo = productInfo,
                isRestore = false,
                appUserID = appUserId,
                marketplace = null,
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
            mockPostReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = purchaseToken,
                storeUserID = amazonUserID,
                receiptInfo = productInfo,
                isRestore = true,
                appUserID = appUserId,
                marketplace = null,
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
            mockPostReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = purchaseToken,
                storeUserID = amazonUserID,
                receiptInfo = productInfo,
                isRestore = true,
                appUserID = appUserId,
                marketplace = null,
                onSuccess = any(),
                onError = any()
            )
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
            mockPostReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = purchaseToken,
                storeUserID = amazonUserID,
                receiptInfo = productInfo,
                isRestore = false,
                appUserID = appUserId,
                marketplace = null,
                onSuccess = any(),
                onError = any()
            )
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
            mockPostReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = purchaseToken,
                storeUserID = amazonUserID,
                receiptInfo = productInfo,
                isRestore = false,
                appUserID = appUserId,
                marketplace = null,
                onSuccess = any(),
                onError = any()
            )
        }
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
            mockPostReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = purchaseToken,
                storeUserID = amazonUserID,
                receiptInfo = productInfo,
                isRestore = false,
                appUserID = appUserId,
                marketplace = null,
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
            mockPostReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = purchaseToken,
                storeUserID = amazonUserID,
                receiptInfo = productInfo,
                isRestore = false,
                appUserID = appUserId,
                marketplace = null,
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

    // region queryPurchases

    @Test
    fun `on billing wrapper connected, sync pending purchases`() {
        capturedBillingWrapperStateListener.captured.onConnected()
        verify(exactly = 1) {
            mockPostPendingTransactionsHelper.syncPendingPurchaseQueue(any())
        }
    }

    @Test
    fun `on app foregrounded sync pending purchases`() {
        mockSynchronizeSubscriberAttributesForAllUsers()
        mockOfferingsManagerAppForeground()
        purchases.onAppForegrounded()
        verify(exactly = 1) {
            mockPostPendingTransactionsHelper.syncPendingPurchaseQueue(any())
        }
    }

    // endregion

    // region app lifecycle

    @Test
    fun `state appInBackground is updated when app foregrounded`() {
        mockOfferingsManagerAppForeground()
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
        mockOfferingsManagerAppForeground()
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
        every {
            mockCache.isCustomerInfoCacheStale(appInBackground = false, appUserID = appUserId)
        } returns false
        mockOfferingsManagerAppForeground()
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
        every {
            mockCache.isCustomerInfoCacheStale(appInBackground = false, appUserID = appUserId)
        } returns true
        mockOfferingsManagerAppForeground()
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

    private fun getPurchaseParams(
        purchaseable: Any,
        oldProductId: String? = null,
        isPersonalizedPrice: Boolean? = null,
        googleProrationMode: GoogleProrationMode? = null
    ): PurchaseParams {
        val builder = when (purchaseable) {
            is SubscriptionOption -> PurchaseParams.Builder(mockActivity, purchaseable)
            is Package -> PurchaseParams.Builder(mockActivity, purchaseable)
            is StoreProduct -> PurchaseParams.Builder(mockActivity, purchaseable)
            else -> null
        }

        oldProductId?.let {
            builder!!.oldProductId(it)
        }

        isPersonalizedPrice?.let {
            builder!!.isPersonalizedPrice(it)
        }

        googleProrationMode?.let {
            builder!!.googleProrationMode(googleProrationMode)
        }
        return builder!!.build()
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

    private fun getMockedPurchaseList(
        productId: String,
        purchaseToken: String,
        productType: ProductType,
        offeringIdentifier: String? = null,
        purchaseState: Int = Purchase.PurchaseState.PURCHASED,
        acknowledged: Boolean = false,
        subscriptionOptionId: String? = this.subscriptionOptionId
    ): List<StoreTransaction> {
        val p = stubGooglePurchase(
            productIds = listOf(productId),
            purchaseToken = purchaseToken,
            purchaseState = purchaseState,
            acknowledged = acknowledged
        )

        return listOf(
            p.toStoreTransaction(
                productType,
                offeringIdentifier,
                if (productType == ProductType.SUBS) subscriptionOptionId else null
            )
        )
    }

    private fun Int.buildResult(): BillingResult {
        return BillingResult.newBuilder().setResponseCode(this).build()
    }

    private fun mockQueryingProductDetails(
        productId: String,
        type: ProductType,
        offeringIdentifier: String?,
        subscriptionOptionId: String? = this.subscriptionOptionId
    ): ReceiptInfo {
        return if (type == ProductType.SUBS) {
            val productDetails = createMockProductDetailsFreeTrial(productId, 2.00)

            val storeProduct = productDetails.toStoreProduct(
                productDetails.subscriptionOfferDetails!!
            )!!

            mockQueryingProductDetails(storeProduct, offeringIdentifier, subscriptionOptionId)
        } else {
            val productDetails = createMockOneTimeProductDetails(productId, 2.00)
            val storeProduct = productDetails.toInAppStoreProduct()!!

            mockQueryingProductDetails(storeProduct, offeringIdentifier, null)
        }
    }

    private fun mockQueryingProductDetails(
        storeProduct: StoreProduct,
        offeringIdentifier: String?,
        subscriptionOptionId: String? = this.subscriptionOptionId
    ): ReceiptInfo {
        val productId = storeProduct.purchasingData.productId

        val receiptInfo = ReceiptInfo(
            productIDs = listOf(productId),
            offeringIdentifier = offeringIdentifier,
            storeProduct = storeProduct,
            subscriptionOptionId = if (storeProduct.type == ProductType.SUBS) subscriptionOptionId else null
        )

        every {
            mockBillingAbstract.queryProductDetailsAsync(
                storeProduct.type,
                setOf(productId),
                captureLambda(),
                any()
            )
        } answers {
            lambda<(List<StoreProduct>) -> Unit>().captured.invoke(listOf(storeProduct))
        }

        return receiptInfo
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

    // endregion

}
