//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.os.Handler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.google.billingResponseToPurchasesError
import com.revenuecat.purchases.google.toInAppStoreProduct
import com.revenuecat.purchases.google.toStoreProduct
import com.revenuecat.purchases.interfaces.GetStoreProductsCallback
import com.revenuecat.purchases.models.BillingFeature
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.strings.PurchaseStrings
import com.revenuecat.purchases.utils.STUB_OFFERING_IDENTIFIER
import com.revenuecat.purchases.utils.createMockOneTimeProductDetails
import com.revenuecat.purchases.utils.createMockProductDetailsFreeTrial
import com.revenuecat.purchases.utils.stubFreeTrialPricingPhase
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
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyAll
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Collections.emptyList

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
@Suppress("DEPRECATION")
internal class PurchasesCommonTest: BasePurchasesTest() {
    private var receivedProducts: List<StoreProduct>? = null

    private val inAppProductId = "inapp"
    private val inAppPurchaseToken = "token_inapp"
    private val subProductId = "sub"
    private val subPurchaseToken = "token_sub"

    private val initiationSource = PostReceiptInitiationSource.PURCHASE

    private val mockLifecycle = mockk<Lifecycle>()
    private val mockLifecycleOwner = mockk<LifecycleOwner>()

    @After
    fun removeMocks() {
        unmockkStatic(BillingClient::class)
    }
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
            mockPostReceiptHelper.postTransactionAndConsumeIfNeeded(
                oldPurchase, any(), isRestore = false, appUserId, initiationSource, captureLambda(), any(),
            )
        } answers {
            lambda<SuccessfulPurchaseCallback>().captured.invoke(oldPurchase, mockk())
        }
        val productChangeParams = getPurchaseParams(
            storeProduct.first().subscriptionOptions!!.first(),
            oldPurchase.productIds.first(),
            googleReplacementMode = GoogleReplacementMode.DEFERRED,
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
                initiationSource = initiationSource,
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
                onCompletion = captureLambda(),
                onError = any()
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
            GoogleReplacementMode.WITHOUT_PRORATION
        )
        verify {
            mockBillingAbstract.makePurchaseAsync(
                any(),
                any(),
                receiptInfo.storeProduct.defaultOption!!.purchasingData,
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
                PresentedOfferingContext(STUB_OFFERING_IDENTIFIER),
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
                PresentedOfferingContext(expectedOfferingIdentifier),
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
                PresentedOfferingContext(expectedOfferingIdentifier),
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
                PresentedOfferingContext(expectedOfferingIdentifier),
                any()
            )
        }
    }

    fun `purchase of OTP Package passes presentedOfferingIdentifier through to purchase`() {
        val expectedOfferingIdentifier = STUB_OFFERING_IDENTIFIER
        val stubOtpProduct = stubINAPPStoreProduct(
            "tokens",
            PresentedOfferingContext(expectedOfferingIdentifier)
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
                PresentedOfferingContext(expectedOfferingIdentifier),
                any()
            )
        }
    }

    @Test
    fun `purchase of OTP StoreProduct passes presentedOfferingIdentifier through to purchase`() {
        val expectedOfferingIdentifier = STUB_OFFERING_IDENTIFIER
        val stubOtpProduct = stubINAPPStoreProduct(
            "tokens",
            PresentedOfferingContext(expectedOfferingIdentifier),
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
                PresentedOfferingContext(expectedOfferingIdentifier),
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
                ReplaceProductInfo(oldPurchase, GoogleReplacementMode.WITHOUT_PRORATION),
                PresentedOfferingContext(STUB_OFFERING_IDENTIFIER),
                any()
            )
        }
    }

    @Test
    fun `receiving new transactions in listener, posts them to the backend`() {
        mockQueryingProductDetails(inAppProductId, ProductType.INAPP, null, null)
        mockQueryingProductDetails(subProductId, ProductType.SUBS, null, subscriptionOptionId)

        val mockedInApps = getMockedPurchaseList(inAppProductId, inAppPurchaseToken, ProductType.INAPP)
        val mockedSubs = getMockedPurchaseList(subProductId, subPurchaseToken, ProductType.SUBS, PresentedOfferingContext("offering_a"))
        val allPurchases = mockedInApps + mockedSubs

        allPurchases.forEach { transaction ->
            every {
                mockPostReceiptHelper.postTransactionAndConsumeIfNeeded(
                    purchase = transaction,
                    storeProduct = any(),
                    isRestore = false,
                    appUserID = appUserId,
                    initiationSource = initiationSource,
                    onSuccess = captureLambda(),
                    onError = any(),
                )
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
                initiationSource = initiationSource,
                onSuccess = any(),
                onError = any()
            )
            mockPostReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = mockedSubs[0],
                storeProduct = match { it.purchasingData.productId == subProductId },
                isRestore = false,
                appUserID = appUserId,
                initiationSource = initiationSource,
                onSuccess = any(),
                onError = any()
            )
            mockEventsManager.flushEvents()
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
                initiationSource = any(),
                onSuccess = any(),
                onError = any(),
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
                PresentedOfferingContext(STUB_OFFERING_IDENTIFIER),
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
                ReplaceProductInfo(oldPurchase, GoogleReplacementMode.WITHOUT_PRORATION),
                PresentedOfferingContext(STUB_OFFERING_IDENTIFIER),
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
                appUserID = appUserId,
                productType = ProductType.SUBS,
                productId = oldProductId,
                onCompletion = any(),
                onError = captureLambda()
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
                ReplaceProductInfo(oldPurchase, GoogleReplacementMode.WITHOUT_PRORATION),
                PresentedOfferingContext(STUB_OFFERING_IDENTIFIER),
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
            PresentedOfferingContext("offering_a"),
        )

        capturedPurchasesUpdatedListener.captured.onPurchasesUpdated(transactions)
        verify(exactly = 1) {
            mockPostReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = transactions[0],
                storeProduct = null,
                isRestore = false,
                appUserID = appUserId,
                initiationSource = initiationSource,
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
                appUserID = appUserId,
                productType = ProductType.SUBS,
                productId = oldSubId,
                onCompletion = captureLambda(),
                onError = any(),
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
            GoogleReplacementMode.WITHOUT_PRORATION
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
                PresentedOfferingContext(STUB_OFFERING_IDENTIFIER),
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
                appUserID = appUserId,
                productType = ProductType.SUBS,
                productId = oldSubId,
                onCompletion = captureLambda(),
                onError = any(),
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
            GoogleReplacementMode.WITHOUT_PRORATION
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

    @Test
    fun `diagnostics is synced on app foregrounded`() {
        verify(exactly = 0) { mockDiagnosticsSynchronizer.syncDiagnosticsFileIfNeeded() }
        mockOfferingsManagerAppForeground()
        Purchases.sharedInstance.purchasesOrchestrator.onAppForegrounded()
        verify(exactly = 1) { mockDiagnosticsSynchronizer.syncDiagnosticsFileIfNeeded() }
    }

    @Test
    fun `diagnostics is synced only on first app foregrounded`() {
        verify(exactly = 0) { mockDiagnosticsSynchronizer.syncDiagnosticsFileIfNeeded() }
        mockOfferingsManagerAppForeground()
        Purchases.sharedInstance.purchasesOrchestrator.onAppForegrounded()
        verify(exactly = 1) { mockDiagnosticsSynchronizer.syncDiagnosticsFileIfNeeded() }
        Purchases.sharedInstance.purchasesOrchestrator.onAppForegrounded()
        Purchases.sharedInstance.purchasesOrchestrator.onAppForegrounded()
        verify(exactly = 1) { mockDiagnosticsSynchronizer.syncDiagnosticsFileIfNeeded() }
    }

    @Test
    fun `paywall events synced on app foregrounded`() {
        verify(exactly = 0) { mockEventsManager.flushEvents() }
        mockOfferingsManagerAppForeground()
        Purchases.sharedInstance.purchasesOrchestrator.onAppForegrounded()
        verify(exactly = 1) { mockEventsManager.flushEvents() }
    }

    @Test
    fun `paywall events synced on app backgrounded`() {
        verify(exactly = 0) { mockEventsManager.flushEvents() }
        Purchases.sharedInstance.purchasesOrchestrator.onAppBackgrounded()
        verify(exactly = 1) { mockEventsManager.flushEvents() }
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
                initiationSource = initiationSource,
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
                initiationSource = initiationSource,
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
        mockGetStorefront()
        capturedBillingWrapperStateListener.captured.onConnected()
        verify(exactly = 1) {
            mockPostPendingTransactionsHelper.syncPendingPurchaseQueue(any(), any())
        }
    }

    @Test
    fun `on app foregrounded sync pending purchases`() {
        mockSynchronizeSubscriberAttributesForAllUsers()
        mockOfferingsManagerAppForeground()
        purchases.purchasesOrchestrator.onAppForegrounded()
        verify(exactly = 1) {
            mockPostPendingTransactionsHelper.syncPendingPurchaseQueue(any(), any())
        }
    }

    // endregion

    @Test
    fun `on billing wrapper connected, gets storefront`() {
        mockGetStorefront()
        capturedBillingWrapperStateListener.captured.onConnected()
        verify(exactly = 1) {
            mockBillingAbstract.getStorefront(any(), any())
        }
    }

    // region Private Methods
    private fun mockSynchronizeSubscriberAttributesForAllUsers() {
        every {
            mockSubscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(appUserId)
        } just Runs
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

    private fun Int.buildResult(): BillingResult {
        return BillingResult.newBuilder().setResponseCode(this).build()
    }

    private fun mockQueryingProductDetails(
        productId: String,
        type: ProductType,
        presentedOfferingContext: PresentedOfferingContext?,
        subscriptionOptionId: String? = this.subscriptionOptionId
    ): ReceiptInfo {
        return if (type == ProductType.SUBS) {
            val productDetails = createMockProductDetailsFreeTrial(productId, 2.00)

            val storeProduct = productDetails.toStoreProduct(
                productDetails.subscriptionOfferDetails!!
            )!!

            mockQueryingProductDetails(storeProduct, presentedOfferingContext, subscriptionOptionId)
        } else {
            val productDetails = createMockOneTimeProductDetails(productId, 2.00)
            val storeProduct = productDetails.toInAppStoreProduct()!!

            mockQueryingProductDetails(storeProduct, presentedOfferingContext, null)
        }
    }

    private fun mockQueryingProductDetails(
        storeProduct: StoreProduct,
        presentedOfferingContext: PresentedOfferingContext?,
        subscriptionOptionId: String? = this.subscriptionOptionId
    ): ReceiptInfo {
        val productId = storeProduct.purchasingData.productId

        val receiptInfo = ReceiptInfo(
            productIDs = listOf(productId),
            presentedOfferingContext = presentedOfferingContext,
            storeProduct = storeProduct,
            subscriptionOptionId = if (storeProduct.type == ProductType.SUBS) subscriptionOptionId else null
        )

        every {
            mockBillingAbstract.queryProductDetailsAsync(
                productType = storeProduct.type,
                productIds = setOf(productId),
                onReceive = captureLambda(),
                onError = any(),
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
                appUserID = appUserId,
                productType = ProductType.SUBS,
                productId = oldProductId,
                onCompletion = if (error == null) captureLambda() else any(),
                onError = if (error != null) captureLambda() else any()
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

    private fun mockGetStorefront() {
        every {
            mockBillingAbstract.getStorefront(
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<(String) -> Unit>().captured.invoke("US")
        }
    }

    // endregion

}
