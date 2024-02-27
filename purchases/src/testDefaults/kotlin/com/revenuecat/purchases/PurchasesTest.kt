//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.common.CustomerInfoFactory
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.google.toInAppStoreProduct
import com.revenuecat.purchases.google.toStoreProduct
import com.revenuecat.purchases.interfaces.GetStoreProductsCallback
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.models.GoogleProrationMode
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.paywalls.events.PaywallEventType
import com.revenuecat.purchases.utils.Responses
import com.revenuecat.purchases.utils.STUB_OFFERING_IDENTIFIER
import com.revenuecat.purchases.utils.createMockOneTimeProductDetails
import com.revenuecat.purchases.utils.createMockProductDetailsFreeTrial
import com.revenuecat.purchases.utils.stubOfferings
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyAll
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
@Suppress("DEPRECATION")
internal class PurchasesTest : BasePurchasesTest() {
    private val inAppProductId = "inapp"
    private val inAppPurchaseToken = "token_inapp"
    private val subProductId = "sub"
    private val subPurchaseToken = "token_sub"
    private val initiationSource = PostReceiptInitiationSource.PURCHASE
    private val restoreInitiationSource = PostReceiptInitiationSource.RESTORE
    private var receivedProducts: List<StoreProduct>? = null

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
                initiationSource = initiationSource,
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
    fun `deprecated upgrade defaults ProrationMode to null if not passed`() {
        val productId = "gold"
        val oldSubId = "oldSubID"
        val receiptInfo = mockQueryingProductDetails(productId, ProductType.SUBS, null)

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
                appUserID = appUserId,
                productType = ProductType.SUBS,
                productId = oldSubId,
                onCompletion = captureLambda(),
                onError = any(),
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

    // region purchasing with upgrade/downgrade

    @Test
    fun `purchasing an upgrade with actual productId starts purchase with expected parameters`() {
        val (_, offerings) = stubOfferings("onemonth_freetrial")
        val packageToPurchase = offerings[STUB_OFFERING_IDENTIFIER]!!.monthly!!
        val purchaseParams = PurchaseParams.Builder(mockActivity, packageToPurchase)
            .oldProductId("oldProductId")
            .googleReplacementMode(GoogleReplacementMode.CHARGE_PRORATED_PRICE)
            .build()

        mockPurchaseFound()

        purchases.purchase(
            purchaseParams,
            object : PurchaseCallback {
                override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {}
                override fun onError(error: PurchasesError, userCancelled: Boolean) {}
            },
        )

        verify(exactly = 1) {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                packageToPurchase.product.purchasingData,
                match { replaceProductInfo ->
                    replaceProductInfo.oldPurchase.productIds.size == 1 &&
                        replaceProductInfo.oldPurchase.productIds.first() == "oldProductId" &&
                        replaceProductInfo.replacementMode == GoogleReplacementMode.CHARGE_PRORATED_PRICE
                },
                STUB_OFFERING_IDENTIFIER,
                null,
            )
        }
    }

    @Test
    fun `Converting between GoogleProrationMode and GoogleReplacementMode works`() {
        GoogleProrationMode.values().forEach {
            assertEquals(it.asGoogleReplacementMode.asGoogleProrationMode, it)
        }
        GoogleReplacementMode.values().forEach {
            assertEquals(it.asGoogleProrationMode.asGoogleReplacementMode, it)
        }
    }

    @Test
    fun `purchasing an upgrade with productId and basePlanId finds purchase with productId`() {
        val (_, offerings) = stubOfferings("onemonth_freetrial")
        val packageToPurchase = offerings[STUB_OFFERING_IDENTIFIER]!!.monthly!!
        val purchaseParams = PurchaseParams.Builder(mockActivity, packageToPurchase)
            .oldProductId("oldProductId:oldBasePlanId")
            .googleReplacementMode(GoogleReplacementMode.CHARGE_PRORATED_PRICE)
            .build()

        mockPurchaseFound()

        purchases.purchase(
            purchaseParams,
            object : PurchaseCallback {
                override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {}
                override fun onError(error: PurchasesError, userCancelled: Boolean) {}
            },
        )

        verify(exactly = 1) {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                packageToPurchase.product.purchasingData,
                match { replaceProductInfo ->
                    replaceProductInfo.oldPurchase.productIds.size == 1 &&
                        replaceProductInfo.oldPurchase.productIds.first() == "oldProductId" &&
                        replaceProductInfo.replacementMode == GoogleReplacementMode.CHARGE_PRORATED_PRICE
                },
                STUB_OFFERING_IDENTIFIER,
                null,
            )
        }
    }

    // endregion

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

    @Test
    fun `invalidate customer info clears cache`() {
        Purchases.sharedInstance.invalidateCustomerInfoCache()
        verify(exactly = 1) {
            mockCache.clearCustomerInfoCache(appUserId)
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

    // endregion

    // region syncAttributesAndOfferingsIfNeeded

    @Test
    fun `syncing attributes and offerings calls success callback when process completes successfully`() {
        every {
            mockSubscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(any(), captureLambda())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        every {
            mockOfferingsManager.getOfferings(any(), any(), any(), captureLambda(), any())
        } answers {
            lambda<(Offerings?) -> Unit>().captured.invoke(mockOfferings)
        }

        var successCallCount = 0
        var receivedOfferings: Offerings? = null
        purchases.syncAttributesAndOfferingsIfNeededWith(
            { fail("Expected to succeed") },
            {
                successCallCount++
                receivedOfferings = it
            }
        )

        assertThat(successCallCount).isEqualTo(1)
        assertThat(receivedOfferings).isEqualTo(mockOfferings)
    }

    @Test
    fun `syncing attributes and offerings calls error callback when called twice within 60 seconds`() {
        every {
            mockSubscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(any(), captureLambda())
        } answers {
            lambda<() -> Unit>().captured.invoke()
        }

        every {
            mockOfferingsManager.getOfferings(any(), any(), any(), captureLambda(), any())
        } answers {
            lambda<(Offerings?) -> Unit>().captured.invoke(mockOfferings)
        }

        var successCallCount = 0
        repeat(6) {
            purchases.syncAttributesAndOfferingsIfNeededWith(
                onError = { print("") },
                onSuccess = { successCallCount++ }
            )
        }

        verify(exactly = 5) {
            mockSubscriberAttributesManager.synchronizeSubscriberAttributesForAllUsers(
                currentAppUserID = any(),
                completion = any(),
            )
        }

        verify(exactly = 5) {
            mockOfferingsManager.getOfferings(
                appUserID = any(),
                appInBackground = any(),
                onError = any(),
                onSuccess = any(),
                fetchCurrent = true
            )
        }

        verify(exactly = 1) {
            mockOfferingsManager.getOfferings(
                appUserID = any(),
                appInBackground = any(),
                onError = any(),
                onSuccess = any(),
                fetchCurrent = false
            )
        }

        assertThat(successCallCount).isEqualTo(6)
    }

    // endregion

    // region syncPurchases

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
                initiationSource = restoreInitiationSource,
                onSuccess = any(),
                onError = any()
            )
        }
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
                initiationSource = restoreInitiationSource,
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
                initiationSource = restoreInitiationSource,
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
                initiationSource = restoreInitiationSource,
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
                initiationSource = restoreInitiationSource,
                onSuccess = any(),
                onError = any()
            )
        }
    }

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
                initiationSource = restoreInitiationSource,
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
                initiationSource = restoreInitiationSource,
                onSuccess = any(),
                onError = any(),
            )
        }

        assertThat(capturedLambda).isNotNull
    }

    // endregion

    // region restore purchases

    @Test
    fun restoringPurchasesGetsHistory() {
        var capturedLambda: ((List<StoreTransaction>) -> Unit)? = null
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserID = appUserId,
                onReceivePurchaseHistory = captureLambda(),
                onReceivePurchaseHistoryError = any()
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
                appUserID = appUserId,
                onReceivePurchaseHistory = any(),
                onReceivePurchaseHistoryError = any()
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
                appUserID = appUserId,
                onReceivePurchaseHistory = captureLambda(),
                onReceivePurchaseHistoryError = any()
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
                initiationSource = restoreInitiationSource,
                onSuccess = any(),
                onError = any()
            )
            mockPostReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = subTransactions[0],
                storeProduct = null,
                isRestore = true,
                appUserID = appUserId,
                initiationSource = restoreInitiationSource,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    @Test
    fun failedToRestorePurchases() {
        val purchasesError = PurchasesError(PurchasesErrorCode.StoreProblemError, "Broken")
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserID = appUserId,
                onReceivePurchaseHistory = any(),
                onReceivePurchaseHistoryError = captureLambda()
            )
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
                appUserID = appUserId,
                onReceivePurchaseHistory = captureLambda(),
                onReceivePurchaseHistoryError = any()
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
            mockPostReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = any(),
                storeProduct = any(),
                isRestore = any(),
                appUserID = any(),
                initiationSource = any(),
                onSuccess = captureLambda(),
                onError = any(),
            )
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
            mockBillingAbstract.queryAllPurchases(
                appUserID = appUserId,
                onReceivePurchaseHistory = any(),
                onReceivePurchaseHistoryError = any()
            )
        }

        assertThat(callbackCalled).isTrue()
    }

    @Test
    fun whenNoTokensRestoringPurchasesStillCallListener() {
        every {
            mockBillingAbstract.queryAllPurchases(
                appUserID = appUserId,
                onReceivePurchaseHistory = captureLambda(),
                onReceivePurchaseHistoryError = any(),
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

    // region track events

    @Test
    fun `track impression event caches it`() {
        val event = mockk<PaywallEvent>().apply {
            every { type } returns PaywallEventType.IMPRESSION
        }
        every { mockPaywallEventsManager.track(event) } just Runs
        assertThat(paywallPresentedCache.getAndRemovePresentedEvent()).isNull()
        purchases.track(event)
        assertThat(paywallPresentedCache.getAndRemovePresentedEvent()).isEqualTo(event)
    }

    @Test
    fun `track close event clears cache`() {
        every { mockPaywallEventsManager.track(any()) } just Runs
        val impressionEvent = mockk<PaywallEvent>().apply {
            every { type } returns PaywallEventType.IMPRESSION
        }
        val closeEvent = mockk<PaywallEvent>().apply {
            every { type } returns PaywallEventType.CLOSE
        }
        assertThat(paywallPresentedCache.getAndRemovePresentedEvent()).isNull()
        purchases.track(impressionEvent)
        purchases.track(closeEvent)
        assertThat(paywallPresentedCache.getAndRemovePresentedEvent()).isNull()
    }

    @Test
    fun `track event tracks event`() {
        val event = mockk<PaywallEvent>().apply {
            every { type } returns PaywallEventType.IMPRESSION
        }
        every { mockPaywallEventsManager.track(event) } just Runs

        purchases.track(event)
        verify(exactly = 1) { mockPaywallEventsManager.track(event) }
    }

    // endregion track events

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

    @Test
    fun `Setting store in the configuration sets it on the Purchases instance`() {
        val builder = PurchasesConfiguration.Builder(mockContext, "api").store(Store.PLAY_STORE)
        Purchases.configure(builder.build())
        assertThat(Purchases.sharedInstance.store).isEqualTo(Store.PLAY_STORE)
    }

    // region Private Methods

    private fun getMockedPurchaseHistoryList(
        productId: String,
        purchaseToken: String,
        productType: ProductType
    ): List<StoreTransaction> {
        val purchaseHistoryRecordWrapper =
            getMockedStoreTransaction(productId, purchaseToken, productType)
        return listOf(purchaseHistoryRecordWrapper)
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
            productType = ProductType.SUBS,
        )

        every {
            mockBillingAbstract.findPurchaseInPurchaseHistory(
                appUserID = appUserId,
                productType = ProductType.SUBS,
                productId = oldProductId,
                onCompletion = if (error == null) captureLambda() else any(),
                onError = if (error != null) captureLambda() else any(),
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
