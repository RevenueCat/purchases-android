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

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
@Suppress("DEPRECATION")
internal class PurchasesCommonTest: BasePurchasesTest() {
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
    fun `when setting up, and passing a appUserID, user is identified`() {
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
        assertThat(Purchases.sharedInstance.purchasesOrchestrator.appConfig.platformInfo).isEqualTo(expected)
    }

    @Test
    fun `Setting proxy URL info sets it in the HttpClient when configuring the SDK`() {
        val expected = URL("https://a-proxy.com")
        Purchases.proxyURL = expected
        Purchases.configure(PurchasesConfiguration.Builder(mockContext, "api").build())
        assertThat(Purchases.sharedInstance.purchasesOrchestrator.appConfig.baseURL).isEqualTo(expected)
    }

    @Test
    fun `Setting observer mode on sets finish transactions to false`() {
        val builder = PurchasesConfiguration.Builder(mockContext, "api").observerMode(true)
        Purchases.configure(builder.build())
        assertThat(Purchases.sharedInstance.purchasesOrchestrator.appConfig.finishTransactions).isFalse()
    }

    @Test
    fun `Setting observer mode off sets finish transactions to true`() {
        val builder = PurchasesConfiguration.Builder(mockContext, "api").observerMode(false)
        Purchases.configure(builder.build())
        assertThat(Purchases.sharedInstance.purchasesOrchestrator.appConfig.finishTransactions).isTrue()
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
        Purchases.sharedInstance.purchasesOrchestrator.onAppForegrounded()
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
    fun `does not fetch customer info on foregrounded if custom entitlement computation mode`() {
        buildPurchases(anonymous = false, customEntitlementComputation = true)
        mockCacheStale(customerInfoStale = true)
        mockSynchronizeSubscriberAttributesForAllUsers()
        mockOfferingsManagerAppForeground()
        Purchases.sharedInstance.purchasesOrchestrator.onAppForegrounded()
        verify(exactly = 0) {
            mockCustomerInfoHelper.retrieveCustomerInfo(any(), any(), any(), any())
        }
    }

    @Test
    fun `fetch product entitlement mapping on foreground if it's stale`() {
        mockOfferingsManagerAppForeground()
        Purchases.sharedInstance.purchasesOrchestrator.onAppForegrounded()
        verify(exactly = 1) {
            mockOfflineEntitlementsManager.updateProductEntitlementMappingCacheIfStale()
        }
    }

    @Test
    fun `does not fetch purchaser info on foregrounded if it's not stale`() {
        mockCacheStale()
        mockSynchronizeSubscriberAttributesForAllUsers()
        purchases.purchasesOrchestrator.state = purchases.purchasesOrchestrator.state.copy(firstTimeInForeground = false)
        mockOfferingsManagerAppForeground()
        Purchases.sharedInstance.purchasesOrchestrator.onAppForegrounded()
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

    // endregion

    // region offerings

    @Test
    fun `on foreground delegates logic`() {
        mockSynchronizeSubscriberAttributesForAllUsers()
        every { mockOfferingsManager.onAppForeground(any()) } just Runs
        Purchases.sharedInstance.purchasesOrchestrator.onAppForegrounded()
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
        purchases.purchasesOrchestrator.appConfig = AppConfig(
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
        purchases.purchasesOrchestrator.onAppForegrounded()
        verify(exactly = 1) {
            mockPostPendingTransactionsHelper.syncPendingPurchaseQueue(any())
        }
    }

    // endregion

    // region app lifecycle

    @Test
    fun `state appInBackground is updated when app foregrounded`() {
        mockOfferingsManagerAppForeground()
        purchases.purchasesOrchestrator.state = purchases.purchasesOrchestrator.state.copy(appInBackground = true)
        Purchases.sharedInstance.purchasesOrchestrator.onAppForegrounded()
        assertThat(purchases.purchasesOrchestrator.state.appInBackground).isFalse()
    }

    @Test
    fun `state appInBackground is updated when app backgrounded`() {
        purchases.purchasesOrchestrator.state = purchases.purchasesOrchestrator.state.copy(appInBackground = false)
        Purchases.sharedInstance.purchasesOrchestrator.onAppBackgrounded()
        assertThat(purchases.purchasesOrchestrator.state.appInBackground).isTrue()
    }

    @Test
    fun `force update of caches when app foregrounded for the first time`() {
        mockOfferingsManagerAppForeground()
        purchases.purchasesOrchestrator.state = purchases.purchasesOrchestrator.state.copy(appInBackground = false, firstTimeInForeground = true)
        Purchases.sharedInstance.purchasesOrchestrator.onAppForegrounded()
        assertThat(purchases.purchasesOrchestrator.state.firstTimeInForeground).isFalse()
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
        purchases.purchasesOrchestrator.state = purchases.purchasesOrchestrator.state.copy(appInBackground = false, firstTimeInForeground = false)
        Purchases.sharedInstance.purchasesOrchestrator.onAppForegrounded()
        assertThat(purchases.purchasesOrchestrator.state.firstTimeInForeground).isFalse()
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
        purchases.purchasesOrchestrator.state = purchases.purchasesOrchestrator.state.copy(appInBackground = false, firstTimeInForeground = false)
        Purchases.sharedInstance.purchasesOrchestrator.onAppForegrounded()
        assertThat(purchases.purchasesOrchestrator.state.firstTimeInForeground).isFalse()
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

    // endregion

}
