//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingFlowParams.ProrationMode
import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.google.toInAppStoreProduct
import com.revenuecat.purchases.google.toStoreProduct
import com.revenuecat.purchases.google.toStoreTransaction
import com.revenuecat.purchases.interfaces.GetStoreProductsCallback
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.STUB_OFFERING_IDENTIFIER
import com.revenuecat.purchases.utils.createMockOneTimeProductDetails
import com.revenuecat.purchases.utils.createMockProductDetailsFreeTrial
import com.revenuecat.purchases.utils.stubGooglePurchase
import com.revenuecat.purchases.utils.stubOfferings
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
@Suppress("DEPRECATION")
internal class PurchasesTest : BasePurchasesTest() {
    private val mockActivity: Activity = mockk()
    private var receivedProducts: List<StoreProduct>? = null

    private val subscriptionOptionId = "mock-base-plan-id:mock-offer-id"

    @Test
    fun getsSubscriptionSkus() {
        val skus = listOf("onemonth_freetrial")

        val skuDetails = mockStoreProduct(skus, listOf(), ProductType.SUBS)

        purchases.getSubscriptionSkus(
            skus,
            object : GetStoreProductsCallback {
                override fun onReceived(storeProducts: List<StoreProduct>) {
                    receivedProducts = storeProducts
                }

                override fun onError(error: PurchasesError) {
                    fail("shouldn't be error")
                }
            },
        )

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
                onError = any(),
            )
        }
    }

    @Test
    fun `when setting up, and passing a appUserID, user is identified`() {
        assertThat(purchases.allowSharingPlayStoreAccount).isEqualTo(false)
        assertThat(purchases.appUserID).isEqualTo(appUserId)
    }

    // region purchasing

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
            },
            onSuccess = { purchase, _ ->
                callCount++
                assertThat(purchase).isEqualTo(oldPurchase)
            },
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
                onError = any(),
            )
        }
    }

    @Test
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
                any(),
            )
        } answers {
            lambda<(StoreTransaction) -> Unit>().captured.invoke(oldTransaction)
        }

        purchases.purchaseProductWith(
            mockActivity,
            receiptInfo.storeProduct!!,
            UpgradeInfo(oldSubId),
            onError = { _, _ ->
            },
            onSuccess = { _, _ ->
            },
        )

        val expectedReplaceProductInfo = ReplaceProductInfo(
            oldTransaction,
            null,
        )
        verify {
            mockBillingAbstract.makePurchaseAsync(
                any(),
                any(),
                receiptInfo.storeProduct!!.defaultOption!!.purchasingData,
                expectedReplaceProductInfo,
                any(),
            )
        }
    }

    @Test
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
                any(),
            )
        } answers {
            lambda<(StoreTransaction) -> Unit>().captured.invoke(oldTransaction)
        }

        purchases.purchaseProductWith(
            mockActivity,
            receiptInfo.storeProduct!!,
            UpgradeInfo(oldSubId),
            onError = { _, _ ->
            },
            onSuccess = { _, _ ->
            },
        )

        verify {
            mockBillingAbstract.makePurchaseAsync(
                any(),
                any(),
                receiptInfo.storeProduct!!.defaultOption!!.purchasingData,
                any(),
                null,
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
            },
        )

        verify {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                packageToPurchase.product.purchasingData,
                null,
                STUB_OFFERING_IDENTIFIER,
                null,
            )
        }
    }

    // region customer info

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
                any(),
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
                null,
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

    // region syncPurchases

    @Test
    fun `syncing transactions calls helper with correct parameters`() {
        val allowSharingAccount = true
        val appInBackground = true
        purchases.allowSharingPlayStoreAccount = allowSharingAccount
        purchases.purchasesOrchestrator.state = purchases.purchasesOrchestrator.state.copy(
            appInBackground = appInBackground,
        )

        every { mockSyncPurchasesHelper.syncPurchases(any(), any(), any(), any()) } just Runs

        purchases.syncPurchases()

        verify(exactly = 1) {
            mockSyncPurchasesHelper.syncPurchases(
                allowSharingAccount,
                appInBackground,
                any(),
                any(),
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
                any(),
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
            isoCurrencyCode = currencyCode,
        )

        val productInfo = ReceiptInfo(
            productIDs = listOf(skuTerm),
            price = price,
            currency = currencyCode,
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
                onError = any(),
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
                any(),
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
            isoCurrencyCode = currencyCode,
        )

        val productInfo = ReceiptInfo(
            productIDs = listOf(skuTerm),
            price = price,
            currency = currencyCode,
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
                onError = any(),
            )
        }

        assertThat(capturedLambda).isNotNull
    }

    // endregion

    // region Private Methods
    private fun mockSynchronizeSubscriberAttributesForAllUsers() {
        every {
            mockSubscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(appUserId)
        } just Runs
    }

    private fun getMockedPurchaseList(
        productId: String,
        purchaseToken: String,
        productType: ProductType,
        offeringIdentifier: String? = null,
        purchaseState: Int = Purchase.PurchaseState.PURCHASED,
        acknowledged: Boolean = false,
        subscriptionOptionId: String? = this.subscriptionOptionId,
    ): List<StoreTransaction> {
        val p = stubGooglePurchase(
            productIds = listOf(productId),
            purchaseToken = purchaseToken,
            purchaseState = purchaseState,
            acknowledged = acknowledged,
        )

        return listOf(
            p.toStoreTransaction(
                productType,
                offeringIdentifier,
                if (productType == ProductType.SUBS) subscriptionOptionId else null,
            ),
        )
    }

    private fun mockQueryingProductDetails(
        productId: String,
        type: ProductType,
        offeringIdentifier: String?,
        subscriptionOptionId: String? = this.subscriptionOptionId,
    ): ReceiptInfo {
        return if (type == ProductType.SUBS) {
            val productDetails = createMockProductDetailsFreeTrial(productId, 2.00)

            val storeProduct = productDetails.toStoreProduct(
                productDetails.subscriptionOfferDetails!!,
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
        subscriptionOptionId: String? = this.subscriptionOptionId,
    ): ReceiptInfo {
        val productId = storeProduct.purchasingData.productId

        val receiptInfo = ReceiptInfo(
            productIDs = listOf(productId),
            offeringIdentifier = offeringIdentifier,
            storeProduct = storeProduct,
            subscriptionOptionId = if (storeProduct.type == ProductType.SUBS) subscriptionOptionId else null,
        )

        every {
            mockBillingAbstract.queryProductDetailsAsync(
                storeProduct.type,
                setOf(productId),
                captureLambda(),
                any(),
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
            productType = ProductType.SUBS,
        )

        every {
            mockBillingAbstract.findPurchaseInPurchaseHistory(
                appUserId,
                ProductType.SUBS,
                oldProductId,
                if (error == null) captureLambda() else any(),
                if (error != null) captureLambda() else any(),
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

    private fun mockIdentityManagerLogout(error: PurchasesError? = null) {
        every {
            mockIdentityManager.logOut(captureLambda())
        } answers {
            lambda<(PurchasesError?) -> Unit>().captured.invoke(error)
        }
    }

    // endregion
}
