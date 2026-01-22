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
import com.revenuecat.purchases.common.SharedConstants
import com.revenuecat.purchases.common.events.FeatureEvent
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.google.toInAppStoreProduct
import com.revenuecat.purchases.google.toStoreProduct
import com.revenuecat.purchases.interfaces.GetCustomerCenterConfigCallback
import com.revenuecat.purchases.interfaces.GetStoreProductsCallback
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.RedeemWebPurchaseListener
import com.revenuecat.purchases.interfaces.SyncPurchasesCallback
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.GoogleSubscriptionOption
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.paywalls.DownloadedFontFamily
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.paywalls.events.PaywallEventType
import com.revenuecat.purchases.utils.Responses
import com.revenuecat.purchases.utils.Result
import com.revenuecat.purchases.utils.STUB_OFFERING_IDENTIFIER
import com.revenuecat.purchases.utils.createMockOneTimeProductDetails
import com.revenuecat.purchases.utils.createMockProductDetailsFreeTrial
import com.revenuecat.purchases.utils.stubOfferings
import com.revenuecat.purchases.utils.stubPricingPhase
import com.revenuecat.purchases.utils.stubStoreProduct
import com.revenuecat.purchases.utils.stubStoreProductWithGoogleSubscriptionPurchaseData
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyAll
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.net.URL
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

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
                subscriptionOptionForProductIDs = any(),
                isRestore = true,
                appUserID = appUserId,
                initiationSource = initiationSource,
                sdkOriginated = false,
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

    // region storefrontCountryCode

    @Test
    fun `getting storefront country code calls billing store with correct parameters`() {
        assertThat(purchases.storefrontCountryCode).isNull()

        every { mockBillingAbstract.getStorefront(captureLambda(), any()) }.answers {
            lambda<(String) -> Unit>().captured.invoke("test-storefront")
        }

        var storefrontCountryCode: String? = null
        purchases.getStorefrontCountryCodeWith { storefrontCountryCode = it }

        assertThat(storefrontCountryCode).isEqualTo("test-storefront")
        assertThat(purchases.storefrontCountryCode).isEqualTo("test-storefront")
        verify(exactly = 1) { mockBillingAbstract.getStorefront(any(), any()) }
    }

    @Test
    fun `if already there, getting storefront country code does not calls billing store`() {
        assertThat(purchases.storefrontCountryCode).isNull()

        every { mockBillingAbstract.getStorefront(captureLambda(), any()) }.answers {
            lambda<(String) -> Unit>().captured.invoke("test-storefront")
        }

        purchases.getStorefrontCountryCodeWith {  }

        assertThat(purchases.storefrontCountryCode).isEqualTo("test-storefront")
        verify(exactly = 1) { mockBillingAbstract.getStorefront(any(), any()) }

        every { mockBillingAbstract.getStorefront(captureLambda(), any()) }.answers {
            lambda<(String) -> Unit>().captured.invoke("test-storefront-should-not-be-called")
        }

        purchases.getStorefrontCountryCodeWith {  }

        assertThat(purchases.storefrontCountryCode).isEqualTo("test-storefront")
        verify(exactly = 1) { mockBillingAbstract.getStorefront(any(), any()) }
    }

    @Test
    fun `if getting storefront fails, it propagates failure`() {
        every { mockBillingAbstract.getStorefront(any(), captureLambda()) }.answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(PurchasesError(PurchasesErrorCode.StoreProblemError))
        }

        var error: PurchasesError? = null
        purchases.getStorefrontCountryCodeWith(
            onError = { error = it },
            onSuccess = { fail("Should error") }
        )

        assertThat(error?.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `storefront locale is correctly constructed from storefront country`() {
        // Arrange
        val regionCode = "NL"
        val expectedIso3Country = "NLD"
        val expectedLocale = Locale.Builder().setRegion(regionCode).build()
        every { mockBillingAbstract.getStorefront(onSuccess = captureLambda(), onError = any()) }.answers {
            lambda<(String) -> Unit>().captured.invoke(regionCode)
        }

        // Act
        var actualLocaleFromCallback: Locale? = null
        purchases.getStorefrontLocaleWith { actualLocaleFromCallback = it }
        val actualLocaleFromProperty = purchases.storefrontLocale

        // Assert
        assertThat(actualLocaleFromCallback).isEqualTo(expectedLocale)
        assertThat(actualLocaleFromProperty).isEqualTo(expectedLocale)
        // The below assertion is added so we're notified if our assumptions are no longer true. If it starts failing,
        // we should look into providing another way of getting the 3-letter storefront country code.
        assertThat(actualLocaleFromProperty?.isO3Country).isEqualTo(expectedIso3Country)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `storefront locale is null when the billing library returns an error`() {
        // Arrange
        val expectedLocale: Locale? = null
        val expectedError = PurchasesError(
            code = PurchasesErrorCode.StoreProblemError,
            underlyingErrorMessage = "Error getting storefront"
        )
        every { mockBillingAbstract.getStorefront(onSuccess = any(), onError = captureLambda()) }.answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(expectedError)
        }

        // Act
        var actualLocaleFromCallback: Locale? = null
        var actualErrorFromCallback: PurchasesError? = null
        purchases.getStorefrontLocaleWith(
            onSuccess = { actualLocaleFromCallback = it },
            onError = { actualErrorFromCallback = it }
        )
        val actualLocaleFromProperty = purchases.storefrontLocale

        // Assert
        assertThat(actualLocaleFromCallback).isEqualTo(expectedLocale)
        assertThat(actualLocaleFromProperty).isEqualTo(expectedLocale)
        assertThat(actualErrorFromCallback).isEqualTo(expectedError)
    }

    // endregion storefrontCountryCode

    // region purchasing

    @Test
    fun `upgrade defaults ReplacementMode to WITHOUT_PRORATION if not passed`() {
        val productId = "gold"
        val oldSubId = "oldSubID"
        val storeProduct = stubStoreProduct(productId)
        mockQueryingProductDetails(productId, ProductType.SUBS, null)

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

        purchases.purchaseWith(
            PurchaseParams.Builder(
                mockActivity,
                storeProduct,
            ).oldProductId(oldSubId)
                .build(),
            onError = { _, _ ->
            },
            onSuccess = { _, _ ->
            },
        )

        val expectedReplaceProductInfo = ReplaceProductInfo(
            oldTransaction,
            GoogleReplacementMode.WITHOUT_PRORATION,
        )
        verify {
            mockBillingAbstract.makePurchaseAsync(
                any(),
                any(),
                storeProduct.defaultOption!!.purchasingData,
                expectedReplaceProductInfo,
                any(),
            )
        }
    }

    @Test
    fun `purchase does not set isPersonalizedPrice`() {
        val productId = "gold"
        val oldSubId = "oldSubID"
        val storeProduct = stubStoreProduct(productId)
        mockQueryingProductDetails(productId, ProductType.SUBS, null)

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

        purchases.purchaseWith(
            PurchaseParams.Builder(
                mockActivity,
                storeProduct,
            ).oldProductId(oldSubId)
                .build(),
            onError = { _, _ ->
            },
            onSuccess = { _, _ ->
            },
        )

        verify {
            mockBillingAbstract.makePurchaseAsync(
                any(),
                any(),
                storeProduct.defaultOption!!.purchasingData,
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
                PresentedOfferingContext(STUB_OFFERING_IDENTIFIER),
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
                PresentedOfferingContext(STUB_OFFERING_IDENTIFIER),
                null,
            )
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
                PresentedOfferingContext(STUB_OFFERING_IDENTIFIER),
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
            mockCustomerInfoHelper.retrieveCustomerInfo(appUserID, any(), any(), any(), false, any())
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
    fun `login called with different appUserID notifies backup manager`() {
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
            mockBackupManager.dataChanged()
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
                false,
                any(),
            )
        }
        verify(exactly = 1) {
            mockOfferingsManager.fetchAndCacheOfferings(appUserID, false, any(), any())
        }
        verify(exactly = 1) {
            mockBackupManager.dataChanged()
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
                false,
                callback = null,
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

        purchases.syncAmazonPurchase(
            productID = skuParent,
            receiptID = purchaseToken,
            amazonUserID = amazonUserID,
            price = price,
            isoCurrencyCode = currencyCode
        )

        val productInfo = ReceiptInfo(
            productIDs = listOf(skuTerm),
            price = price,
            currency = currencyCode,
            storeUserID = amazonUserID,
            marketplace = null,
        )
        verify(exactly = 1) {
            mockPostReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = purchaseToken,
                receiptInfo = productInfo,
                isRestore = false,
                appUserID = appUserId,
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

        purchases.syncAmazonPurchase(
            productID = skuParent,
            receiptID = purchaseToken,
            amazonUserID = amazonUserID,
            price = price,
            isoCurrencyCode = currencyCode
        )

        val productInfo = ReceiptInfo(
            productIDs = listOf(skuTerm),
            price = price,
            currency = currencyCode,
            storeUserID = amazonUserID,
            marketplace = null,
        )
        verify(exactly = 1) {
            mockPostReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = purchaseToken,
                receiptInfo = productInfo,
                isRestore = false,
                appUserID = appUserId,
                initiationSource = restoreInitiationSource,
                onSuccess = any(),
                onError = any()
            )
        }

        every {
            mockCache.getPreviouslySentHashedTokens()
        } returns setOf(purchaseToken.sha1())

        purchases.syncAmazonPurchase(
            productID = skuParent,
            receiptID = purchaseToken,
            amazonUserID = amazonUserID,
            price = price,
            isoCurrencyCode = currencyCode
        )

        verify(exactly = 1) {
            mockPostReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = purchaseToken,
                receiptInfo = productInfo,
                isRestore = false,
                appUserID = appUserId,
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

        purchases.syncAmazonPurchase(
            productID = skuParent,
            receiptID = purchaseToken,
            amazonUserID = amazonUserID,
            price = null,
            isoCurrencyCode = null
        )

        val productInfo = ReceiptInfo(
            productIDs = listOf(skuTerm),
            storeUserID = amazonUserID,
            marketplace = null,
        )
        verify(exactly = 1) {
            mockPostReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = purchaseToken,
                receiptInfo = productInfo,
                isRestore = false,
                appUserID = appUserId,
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

        purchases.syncAmazonPurchase(
            productID = skuParent,
            receiptID = purchaseToken,
            amazonUserID = amazonUserID,
            price = 0.0,
            isoCurrencyCode = null
        )

        val productInfo = ReceiptInfo(
            productIDs = listOf(skuTerm),
            currency = null,
            price = null,
            storeUserID = amazonUserID,
            marketplace = null,
        )
        verify(exactly = 1) {
            mockPostReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = purchaseToken,
                receiptInfo = productInfo,
                isRestore = false,
                appUserID = appUserId,
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

        purchases.syncAmazonPurchase(
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
            storeUserID = amazonUserID,
            marketplace = null,
        )
        verify(exactly = 1) {
            mockPostReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = purchaseToken,
                receiptInfo = productInfo,
                isRestore = true,
                appUserID = appUserId,
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

        purchases.syncAmazonPurchase(
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
            storeUserID = amazonUserID,
            marketplace = null,
        )
        verify(exactly = 1) {
            mockPostReceiptHelper.postTokenWithoutConsuming(
                purchaseToken = purchaseToken,
                receiptInfo = productInfo,
                isRestore = true,
                appUserID = appUserId,
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
        val inAppTransactions = getMockedPurchaseList(inAppProductId, inAppPurchaseToken, ProductType.INAPP)
        val subTransactions = getMockedPurchaseList(subProductId, subPurchaseToken, ProductType.SUBS)

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
                subscriptionOptionForProductIDs = null,
                isRestore = true,
                appUserID = appUserId,
                initiationSource = restoreInitiationSource,
                onSuccess = any(),
                onError = any(),
            )
            mockPostReceiptHelper.postTransactionAndConsumeIfNeeded(
                purchase = subTransactions[0],
                storeProduct = null,
                subscriptionOptionForProductIDs = null,
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
                    getMockedPurchaseList(productId, purchaseToken, ProductType.INAPP) +
                        getMockedPurchaseList(productIdSub, purchaseTokenSub, ProductType.SUBS)
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
                subscriptionOptionForProductIDs = null,
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
    fun `track purchase initiated event caches it`() {
        val event = mockk<PaywallEvent>().apply {
            every { type } returns PaywallEventType.PURCHASE_INITIATED
        }
        every { mockEventsManager.track(event) } just Runs
        assertThat(paywallPresentedCache.hasCachedPurchaseInitiatedData()).isFalse
        purchases.track(event)
        assertThat(paywallPresentedCache.hasCachedPurchaseInitiatedData()).isTrue
    }

    @Test
    fun `track purchase error event clears cache`() {
        every { mockEventsManager.track(any<FeatureEvent>()) } just Runs
        val purchaseInitiatedEvent = mockk<PaywallEvent>().apply {
            every { type } returns PaywallEventType.PURCHASE_INITIATED
        }
        val purchaseErrorEvent = mockk<PaywallEvent>().apply {
            every { type } returns PaywallEventType.PURCHASE_ERROR
        }
        assertThat(paywallPresentedCache.hasCachedPurchaseInitiatedData()).isFalse
        purchases.track(purchaseInitiatedEvent)
        assertThat(paywallPresentedCache.hasCachedPurchaseInitiatedData()).isTrue
        purchases.track(purchaseErrorEvent)
        assertThat(paywallPresentedCache.hasCachedPurchaseInitiatedData()).isFalse
    }

    @Test
    fun `track cancel event clears cache`() {
        every { mockEventsManager.track(any<FeatureEvent>()) } just Runs
        val purchaseInitiatedEvent = mockk<PaywallEvent>().apply {
            every { type } returns PaywallEventType.PURCHASE_INITIATED
        }
        val cancelEvent = mockk<PaywallEvent>().apply {
            every { type } returns PaywallEventType.CANCEL
        }
        assertThat(paywallPresentedCache.hasCachedPurchaseInitiatedData()).isFalse
        purchases.track(purchaseInitiatedEvent)
        assertThat(paywallPresentedCache.hasCachedPurchaseInitiatedData()).isTrue
        purchases.track(cancelEvent)
        assertThat(paywallPresentedCache.hasCachedPurchaseInitiatedData()).isFalse
    }

    @Test
    fun `track event tracks event`() {
        val event = mockk<PaywallEvent>().apply {
            every { type } returns PaywallEventType.IMPRESSION
        }
        every { mockEventsManager.track(event) } just Runs

        purchases.track(event)
        verify(exactly = 1) { mockEventsManager.track(event) }
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

    // region getAmazonLWAConsentStatus

    @Test
    fun `getAmazonLWAConsentStatus returns success`() {
        every {
            mockBillingAbstract.getAmazonLWAConsentStatus(
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<(AmazonLWAConsentStatus) -> Unit>().captured.also {
                it.invoke(AmazonLWAConsentStatus.CONSENTED)
            }
        }

        var exception: PurchasesError? = null
        var onErrorCalled = false
        purchases.getAmazonLWAConsentStatusWith(
            onSuccess = {
                assertThat(it).isEqualTo(AmazonLWAConsentStatus.CONSENTED)
            },
            onError = {
                fail("should be success")
            }
        )
        assertThat(onErrorCalled).isFalse()
        assertThat(exception).isNull()
    }

    @Test
    fun `getAmazonLWAConsentStatus returns error`() {
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "Store Problem Error")
        every {
            mockBillingAbstract.getAmazonLWAConsentStatus(
                onSuccess = any(),
                onError = captureLambda()
            )
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.also {
                it.invoke(error)
            }
        }

        var exception: PurchasesError? = null
        var onErrorCalled = false
        purchases.getAmazonLWAConsentStatusWith(
            onSuccess = {
                fail("should be an error")
            },
            onError = {
                onErrorCalled = true
                exception = it
            }
        )
        assertThat(onErrorCalled).isTrue()
        assertThat(exception).isEqualTo(error)
    }

    @Test
    fun `getCustomerCenterData returns data from backend on success`() {
        val expectedData = CustomerCenterConfigData(
            screens = emptyMap(),
            appearance = CustomerCenterConfigData.Appearance(),
            localization = CustomerCenterConfigData.Localization(
                locale = "en",
                localizedStrings = emptyMap(),
            ),
            support = CustomerCenterConfigData.Support(
                email = "",
                supportTickets = CustomerCenterConfigData.Support.SupportTickets(),
            ),
        )

        every {
            mockBackend.getCustomerCenterConfig(
                appUserID = appUserId,
                onSuccessHandler = captureLambda(),
                onErrorHandler = any(),
            )
        } answers {
            lambda<(CustomerCenterConfigData) -> Unit>().captured.invoke(expectedData)
        }

        var receivedData: CustomerCenterConfigData? = null
        purchases.getCustomerCenterConfigData(object : GetCustomerCenterConfigCallback {
            override fun onError(error: PurchasesError) {
                fail("should be success")
            }

            override fun onSuccess(customerCenterConfig: CustomerCenterConfigData) {
                receivedData = customerCenterConfig
            }
        })

        assertThat(receivedData).isEqualTo(expectedData)
    }

    @Test
    fun `getCustomerCenterData returns error from backend on error`() {
        val expectedError = PurchasesError(PurchasesErrorCode.UnknownBackendError, "Unknown backend error")

        every {
            mockBackend.getCustomerCenterConfig(
                appUserID = appUserId,
                onSuccessHandler = any(),
                onErrorHandler = captureLambda(),
            )
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.also {
                it.invoke(expectedError)
            }
        }

        var receivedError: PurchasesError? = null
        purchases.getCustomerCenterConfigData(object : GetCustomerCenterConfigCallback {
            override fun onError(error: PurchasesError) {
                receivedError = error
            }

            override fun onSuccess(customerCenterConfig: CustomerCenterConfigData) {
                fail("should be error")
            }
        })

        assertThat(receivedError).isEqualTo(expectedError)
    }

    // region parseAsWebPurchaseRedemption

    @Test
    fun `parseAsWebPurchaseRedemption returns value if a valid web purchase redemption link`() {
        val redemptionLink = Purchases.parseAsWebPurchaseRedemption("rc-1111://redeem_web_purchase?redemption_token=1234")
        assertThat(redemptionLink).isNotNull
    }

    @Test
    fun `parseAsWebPurchaseRedemption does not return value if not a web purchase redemption link`() {
        val redemptionLink = Purchases.parseAsWebPurchaseRedemption("rc-1111://another_link?redemption_token=1234")
        assertThat(redemptionLink).isNull()
    }

    @Test
    fun `parseAsWebPurchaseRedemption does not return value if not a link`() {
        val redemptionLink = Purchases.parseAsWebPurchaseRedemption("invalid_link")
        assertThat(redemptionLink).isNull()
    }

    // endregion parseAsWebPurchaseRedemption

    // region redeemWebPurchase

    @Test
    fun `redeemWebPurchase is successful if helper returns success`() {
        val redemptionLink = WebPurchaseRedemption("redemption_token")
        val slot = slot<RedeemWebPurchaseListener>()
        every { mockWebPurchasesRedemptionHelper.handleRedeemWebPurchase(redemptionLink, capture(slot)) } answers {
            slot.captured.handleResult(RedeemWebPurchaseListener.Result.Success(mockInfo))
        }
        var result: RedeemWebPurchaseListener.Result? = null
        purchases.redeemWebPurchase(redemptionLink) {
            result = it
        }
        assertThat(result).isEqualTo(RedeemWebPurchaseListener.Result.Success(mockInfo))
    }

    @Test
    fun `redeemWebPurchase errors if helper returns error`() {
        val redemptionLink = WebPurchaseRedemption("redemption_token")
        val slot = slot<RedeemWebPurchaseListener>()
        val expectedError = PurchasesError(PurchasesErrorCode.UnknownBackendError)
        every { mockWebPurchasesRedemptionHelper.handleRedeemWebPurchase(redemptionLink, capture(slot)) } answers {
            slot.captured.handleResult(RedeemWebPurchaseListener.Result.Error(expectedError))
        }
        var result: RedeemWebPurchaseListener.Result? = null
        purchases.redeemWebPurchase(redemptionLink) {
            result = it
        }
        assertThat(result).isEqualTo(RedeemWebPurchaseListener.Result.Error(expectedError))
    }

    // endregion redeemWebPurchase

    // region Paywall fonts

    @Test
    fun `getCachedFontFileOrStartDownload returns correct file if found`() {
        val expectedFontFamily = DownloadedFontFamily(
            family = "test-family",
            fonts = emptyList(),
        )
        val fontInfo = UiConfig.AppConfig.FontsConfig.FontInfo.Name(value = "test-value")
        every { mockFontLoader.getCachedFontFamilyOrStartDownload(fontInfo) } returns expectedFontFamily

        val result = purchases.getCachedFontFamilyOrStartDownload(fontInfo)

        assertThat(result).isEqualTo(expectedFontFamily)
    }

    // endregion Paywall fonts

    // region Simulated store

    @Test
    fun `syncing transactions on simulated store does not sync purchases`() {
        buildPurchases(
            anonymous = false,
            apiKeyValidationResult = APIKeyValidator.ValidationResult.SIMULATED_STORE,
            enableSimulatedStore = true,
        )

        var receivedCustomerInfo: CustomerInfo? = null
        purchases.syncPurchases(object: SyncPurchasesCallback {
            override fun onSuccess(customerInfo: CustomerInfo) {
                receivedCustomerInfo = customerInfo
            }

            override fun onError(error: PurchasesError) {
                fail("Expected succeess. Got $error")
            }
        })

        verify(exactly = 0) { mockSyncPurchasesHelper.syncPurchases(any(), any(), any(), any()) }
        assertThat(receivedCustomerInfo).isNotNull
    }

    @Test
    fun `restore transactions on simulated store does not restore purchases`() {
        buildPurchases(
            anonymous = false,
            apiKeyValidationResult = APIKeyValidator.ValidationResult.SIMULATED_STORE,
            enableSimulatedStore = true,
        )

        var receivedCustomerInfo: CustomerInfo? = null
        purchases.restorePurchases(object: ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                receivedCustomerInfo = customerInfo
            }

            override fun onError(error: PurchasesError) {
                fail("Expected succeess. Got $error")
            }
        })

        verify(exactly = 0) { mockBillingAbstract.queryAllPurchases(any(), any(), any()) }
        assertThat(receivedCustomerInfo).isNotNull
    }

    // endregion Simulated store

    // region Add-On Purchases
    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `purchase with empty add-ons list starts purchase with expected parameters`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val purchaseParams = PurchaseParams.Builder(mockActivity, baseProduct)
            .addOnStoreProducts(addOnStoreProducts = emptyList())
            .build()
        buildPurchases(
            anonymous = false,
            apiKeyValidationResult = APIKeyValidator.ValidationResult.SIMULATED_STORE,
            enableSimulatedStore = true,
            store = Store.PLAY_STORE
        )

        var capturedError: PurchasesError? = null
        purchases.purchase(
            purchaseParams = purchaseParams,
            callback = object: PurchaseCallback {
                override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) { }
                override fun onError(error: PurchasesError, userCancelled: Boolean) { capturedError = error }
            },
        )

        val purchasingDataSlot = slot<PurchasingData>()
        verify(exactly = 1) {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                capture(purchasingDataSlot),
                null,
                null,
                null,
            )
        }

        val capturedPurchasingData = purchasingDataSlot.captured
        assertThat(capturedPurchasingData).isInstanceOf(GooglePurchasingData.Subscription::class.java)
        val subscription = capturedPurchasingData as GooglePurchasingData.Subscription
        assertThat(subscription.productId).isEqualTo(baseProduct.purchasingData.productId)
        assertThat(subscription.productType).isEqualTo(baseProduct.purchasingData.productType)
        assertThat(subscription.addOnProducts).isEmpty()
        assertThat(capturedError).isNull()
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `product change with empty add-ons list starts purchase with expected parameters`() {
        val oldProductId = "oldProductId"
        mockPurchaseFound()
        val expectedOldPurchase = getMockedStoreTransaction(
            productId = oldProductId,
            purchaseToken = "another_purchase_token",
            productType = ProductType.SUBS,
        )

        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val purchaseParams = PurchaseParams.Builder(mockActivity, baseProduct)
            .addOnStoreProducts(addOnStoreProducts = emptyList())
            .oldProductId(oldProductId)
            .build()
        buildPurchases(
            anonymous = false,
            apiKeyValidationResult = APIKeyValidator.ValidationResult.SIMULATED_STORE,
            enableSimulatedStore = true,
            store = Store.PLAY_STORE
        )

        var capturedError: PurchasesError? = null
        purchases.purchase(
            purchaseParams = purchaseParams,
            callback = object: PurchaseCallback {
                override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) { }
                override fun onError(error: PurchasesError, userCancelled: Boolean) { capturedError = error }
            },
        )

        val purchasingDataSlot = slot<PurchasingData>()
        val replaceProductInfoSlot = slot<ReplaceProductInfo>()
        verify(exactly = 1) {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                capture(purchasingDataSlot),
                capture(replaceProductInfoSlot),
                null,
                null,
            )
        }

        val capturedPurchasingData = purchasingDataSlot.captured
        assertThat(capturedPurchasingData).isInstanceOf(GooglePurchasingData.Subscription::class.java)

        val subscription = capturedPurchasingData as GooglePurchasingData.Subscription
        assertThat(subscription.productId).isEqualTo(baseProduct.purchasingData.productId)
        assertThat(subscription.productType).isEqualTo(baseProduct.purchasingData.productType)
        assertThat(subscription.addOnProducts).isEmpty()
        assertThat(capturedError).isNull()

        val capturedReplaceProductInfo = replaceProductInfoSlot.captured
        assertThat(capturedReplaceProductInfo.oldPurchase.productIds).isEqualTo(expectedOldPurchase.productIds)
        assertThat(capturedReplaceProductInfo.oldPurchase.purchaseToken).isEqualTo(expectedOldPurchase.purchaseToken)
        assertThat(capturedReplaceProductInfo.replacementMode).isEqualTo(GoogleReplacementMode.WITHOUT_PRORATION)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `product change with add-ons starts purchase with expected parameters`() {
        val oldProductId = "oldProductId"
        mockPurchaseFound()
        val expectedOldPurchase = getMockedStoreTransaction(
            productId = oldProductId,
            purchaseToken = "another_purchase_token",
            productType = ProductType.SUBS,
        )
        val addOnStoreProducts = listOf(
            stubStoreProductWithGoogleSubscriptionPurchaseData(
                productId = "abc123",
                optionId = "option1",
                token = "abc"
            )
        )
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val purchaseParams = PurchaseParams.Builder(mockActivity, baseProduct)
            .addOnStoreProducts(addOnStoreProducts = addOnStoreProducts)
            .oldProductId(oldProductId)
            .build()
        buildPurchases(
            anonymous = false,
            apiKeyValidationResult = APIKeyValidator.ValidationResult.SIMULATED_STORE,
            enableSimulatedStore = true,
            store = Store.PLAY_STORE
        )

        var capturedError: PurchasesError? = null
        purchases.purchase(
            purchaseParams = purchaseParams,
            callback = object: PurchaseCallback {
                override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) { }
                override fun onError(error: PurchasesError, userCancelled: Boolean) { capturedError = error }
            },
        )

        val purchasingDataSlot = slot<PurchasingData>()
        val replaceProductInfoSlot = slot<ReplaceProductInfo>()
        verify(exactly = 1) {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                capture(purchasingDataSlot),
                capture(replaceProductInfoSlot),
                null,
                null,
            )
        }

        val capturedPurchasingData = purchasingDataSlot.captured
        assertThat(capturedPurchasingData).isInstanceOf(GooglePurchasingData.Subscription::class.java)

        val subscription = capturedPurchasingData as GooglePurchasingData.Subscription
        assertThat(subscription.productId).isEqualTo(baseProduct.purchasingData.productId)
        assertThat(subscription.productType).isEqualTo(baseProduct.purchasingData.productType)
        assertThat(subscription.addOnProducts?.size).isEqualTo(1)
        assertThat(subscription.addOnProducts?.first()?.productId).isEqualTo("abc123")
        assertThat(capturedError).isNull()

        val capturedReplaceProductInfo = replaceProductInfoSlot.captured
        assertThat(capturedReplaceProductInfo.oldPurchase.productIds).isEqualTo(expectedOldPurchase.productIds)
        assertThat(capturedReplaceProductInfo.oldPurchase.purchaseToken).isEqualTo(expectedOldPurchase.purchaseToken)
        assertThat(capturedReplaceProductInfo.replacementMode).isEqualTo(GoogleReplacementMode.WITHOUT_PRORATION)
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `purchase with add-ons starts purchase with expected parameters`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()

        val addOns = listOf(
            stubStoreProductWithGoogleSubscriptionPurchaseData(
                productId = "abc123",
                optionId = "option1",
                token = "abc"
            ),
            stubStoreProductWithGoogleSubscriptionPurchaseData(
                productId = "xyz789",
                optionId = "option2",
                token = "xyz"
            ),
        )
        val purchaseParams = PurchaseParams.Builder(mockActivity, baseProduct)
            .addOnStoreProducts(addOnStoreProducts = addOns)
            .build()
        buildPurchases(
            anonymous = false,
            apiKeyValidationResult = APIKeyValidator.ValidationResult.SIMULATED_STORE,
            enableSimulatedStore = true,
            store = Store.PLAY_STORE
        )

        var capturedError: PurchasesError? = null
        purchases.purchase(
            purchaseParams = purchaseParams,
            callback = object: PurchaseCallback {
                override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) { }
                override fun onError(error: PurchasesError, userCancelled: Boolean) { capturedError = error }
            },
        )

        val purchasingDataSlot = slot<PurchasingData>()
        verify(exactly = 1) {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                capture(purchasingDataSlot),
                null,
                null,
                null,
            )
        }

        val capturedPurchasingData = purchasingDataSlot.captured
        assertThat(capturedPurchasingData).isInstanceOf(GooglePurchasingData.Subscription::class.java)
        val subscription = capturedPurchasingData as GooglePurchasingData.Subscription
        assertThat(subscription.productId).isEqualTo(baseProduct.purchasingData.productId)
        assertThat(subscription.productType).isEqualTo(baseProduct.purchasingData.productType)
        val capturedAddOns = subscription.addOnProducts
        assertThat(capturedAddOns?.size).isEqualTo(2)
        assertThat(capturedAddOns?.first()?.productId).isEqualTo("abc123")
        assertThat(capturedAddOns?.first()?.productType).isEqualTo(ProductType.SUBS)
        assertThat(capturedAddOns?.last()?.productId).isEqualTo("xyz789")
        assertThat(capturedAddOns?.last()?.productType).isEqualTo(ProductType.SUBS)
        assertThat(capturedError).isNull()
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `purchase with add-on SubscriptionOption starts purchase with expected parameters`() {
        val baseProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        val subOption1 = GoogleSubscriptionOption(
            productId = "productID1",
            basePlanId = "basePlan1",
            offerId = null,
            pricingPhases = listOf(stubPricingPhase()),
            tags = emptyList(),
            productDetails = mockk(),
            offerToken = "token"
        )

        val subOption2 = GoogleSubscriptionOption(
            productId = "productID2",
            basePlanId = "basePlan2",
            offerId = null,
            pricingPhases = listOf(stubPricingPhase()),
            tags = emptyList(),
            productDetails = mockk(),
            offerToken = "token"
        )

        val purchaseParams = PurchaseParams.Builder(mockActivity, baseProduct)
            .addOnSubscriptionOptions(addOnSubscriptionOptions = listOf(subOption1, subOption2))
            .build()
        buildPurchases(
            anonymous = false,
            apiKeyValidationResult = APIKeyValidator.ValidationResult.SIMULATED_STORE,
            enableSimulatedStore = true,
            store = Store.PLAY_STORE
        )

        var capturedError: PurchasesError? = null
        purchases.purchase(
            purchaseParams = purchaseParams,
            callback = object: PurchaseCallback {
                override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) { }
                override fun onError(error: PurchasesError, userCancelled: Boolean) { capturedError = error }
            },
        )

        val purchasingDataSlot = slot<PurchasingData>()
        verify(exactly = 1) {
            mockBillingAbstract.makePurchaseAsync(
                eq(mockActivity),
                eq(appUserId),
                capture(purchasingDataSlot),
                null,
                null,
                null,
            )
        }

        val capturedPurchasingData = purchasingDataSlot.captured
        assertThat(capturedPurchasingData).isInstanceOf(GooglePurchasingData.Subscription::class.java)
        val subscription = capturedPurchasingData as GooglePurchasingData.Subscription
        assertThat(subscription.productId).isEqualTo(baseProduct.purchasingData.productId)
        assertThat(subscription.productType).isEqualTo(baseProduct.purchasingData.productType)
        val capturedAddOns = subscription.addOnProducts
        assertThat(capturedAddOns?.size).isEqualTo(2)

        fun validateAddOnMatchesSubscriptionOption(
            addOn: GooglePurchasingData,
            expectedSubscriptionOption: GoogleSubscriptionOption
        ) {
            assertThat(addOn.productId).isEqualTo(expectedSubscriptionOption.productId)
            assertThat((addOn as? GooglePurchasingData.Subscription)!!.optionId).isEqualTo(expectedSubscriptionOption.basePlanId)
            assertThat(addOn.productType).isEqualTo(ProductType.SUBS)
        }
        validateAddOnMatchesSubscriptionOption(addOn = capturedAddOns!!.first(), expectedSubscriptionOption = subOption1)
        validateAddOnMatchesSubscriptionOption(addOn = capturedAddOns.last(), expectedSubscriptionOption = subOption2)

        assertThat(capturedError).isNull()
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `initial purchase with add-ons that fails purchaseParams validation throws an error`() {

        val errorCode = PurchasesErrorCode.PurchaseInvalidError
        val underlyingErrorMessage = "This is an error."
        every { mockPurchaseParamsValidator.validate(any()) } returns Result.Error(
            PurchasesError(code = errorCode, underlyingErrorMessage = underlyingErrorMessage)
        )
        val purchaseParams = PurchaseParams.Builder(
            activity = mockActivity,
            storeProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        )
            .addOnStoreProducts(
                addOnStoreProducts = listOf(stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "abc"))
            )
            .build()

        buildPurchases(
            anonymous = false,
            apiKeyValidationResult = APIKeyValidator.ValidationResult.SIMULATED_STORE,
            enableSimulatedStore = true,
        )

        val latch = CountDownLatch(1)
        var capturedError: PurchasesError? = null
        var capturedUserCancelled: Boolean? = null

        purchases.purchase(
            purchaseParams = purchaseParams,
            callback = object: PurchaseCallback {
                override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                    fail("purchase() should fail due to a purchaseParams validation error")
                    latch.countDown()
                }

                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                    capturedError = error
                    capturedUserCancelled = userCancelled
                    latch.countDown()
                }
            },
        )

        assertThat(latch.await(3, TimeUnit.SECONDS)).withFailMessage(
            "Callback was not called within timeout"
        ).isTrue()

        assertThat(capturedError).withFailMessage(
            "Expected error to be captured"
        ).isNotNull()

        assertThat(capturedError?.code).isEqualTo(errorCode)
        assertThat(capturedError?.message).isEqualTo(PurchasesErrorCode.PurchaseInvalidError.description)
        assertThat(capturedError?.underlyingErrorMessage).isEqualTo(underlyingErrorMessage)

        assertThat(capturedUserCancelled).withFailMessage(
            "Expected userCancelled to be false"
        ).isNotNull().isFalse()
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `initial purchase with add-ons throws when Store is not PLAY_STORE`() {
        val purchaseParams = PurchaseParams.Builder(
            activity = mockActivity,
            storeProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        )
            .addOnStoreProducts(
                addOnStoreProducts = listOf(stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "abc"))
            )
            .build()

        for (store in Store.values()) {
            if (store == Store.PLAY_STORE) { continue }
            buildPurchases(
                anonymous = false,
                apiKeyValidationResult = APIKeyValidator.ValidationResult.SIMULATED_STORE,
                enableSimulatedStore = true,
                store = store
            )

            val latch = CountDownLatch(1)
            var capturedError: PurchasesError? = null
            var capturedUserCancelled: Boolean? = null

            purchases.purchase(
                purchaseParams = purchaseParams,
                callback = object: PurchaseCallback {
                    override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                        fail("purchase() should fail with a purchase with add-ons when the store is not the Play Store")
                        latch.countDown()
                    }

                    override fun onError(error: PurchasesError, userCancelled: Boolean) {
                        capturedError = error
                        capturedUserCancelled = userCancelled
                        latch.countDown()
                    }
                },
            )

            assertThat(latch.await(3, TimeUnit.SECONDS)).withFailMessage(
                "Callback was not called within timeout for store: $store"
            ).isTrue()

            assertThat(capturedError).withFailMessage(
                "Expected error to be captured for store: $store"
            ).isNotNull()

            assertThat(capturedError?.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
            assertThat(capturedError?.message).isEqualTo(PurchasesErrorCode.PurchaseInvalidError.description)
            assertThat(capturedError?.underlyingErrorMessage).isEqualTo("Making a purchase with add-ons is only supported on" +
                " the Play Store.")

            assertThat(capturedUserCancelled).withFailMessage(
                "Expected userCancelled to be false for store: $store"
            ).isNotNull().isFalse()
        }
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Test
    fun `product change with add-ons throws when Store is not PLAY_STORE`() {
        val purchaseParams = PurchaseParams.Builder(
            activity = mockActivity,
            storeProduct = stubStoreProductWithGoogleSubscriptionPurchaseData()
        )
            .addOnStoreProducts(
                addOnStoreProducts = listOf(stubStoreProductWithGoogleSubscriptionPurchaseData(productId = "abc"))
            )
            .oldProductId("123")
            .build()

        for (store in Store.values()) {
            if (store == Store.PLAY_STORE) { continue }
            buildPurchases(
                anonymous = false,
                apiKeyValidationResult = APIKeyValidator.ValidationResult.SIMULATED_STORE,
                enableSimulatedStore = true,
                store = store
            )

            val latch = CountDownLatch(1)
            var capturedError: PurchasesError? = null
            var capturedUserCancelled: Boolean? = null

            purchases.purchase(
                purchaseParams = purchaseParams,
                callback = object: PurchaseCallback {
                    override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                        fail("purchase() should fail with a purchase with add-ons when the store is not the Play Store")
                        latch.countDown()
                    }

                    override fun onError(error: PurchasesError, userCancelled: Boolean) {
                        capturedError = error
                        capturedUserCancelled = userCancelled
                        latch.countDown()
                    }
                },
            )

            assertThat(latch.await(3, TimeUnit.SECONDS)).withFailMessage(
                "Callback was not called within timeout for store: $store"
            ).isTrue()

            assertThat(capturedError).withFailMessage(
                "Expected error to be captured for store: $store"
            ).isNotNull()

            assertThat(capturedError?.code).isEqualTo(PurchasesErrorCode.PurchaseInvalidError)
            assertThat(capturedError?.message).isEqualTo(PurchasesErrorCode.PurchaseInvalidError.description)
            assertThat(capturedError?.underlyingErrorMessage).isEqualTo("Making a purchase with add-ons is only supported on" +
                " the Play Store.")

            assertThat(capturedUserCancelled).withFailMessage(
                "Expected userCancelled to be false for store: $store"
            ).isNotNull().isFalse()
        }
    }
    // endregion Add-On Purchases

    // region Private Methods

    private fun getMockedPurchaseList(
        productId: String,
        purchaseToken: String,
        productType: ProductType
    ): List<StoreTransaction> {
        val purchaseRecordWrapper =
            getMockedStoreTransaction(productId, purchaseToken, productType)
        return listOf(purchaseRecordWrapper)
    }

    private fun mockQueryingProductDetails(
        productId: String,
        type: ProductType,
        presentedOfferingContext: PresentedOfferingContext?,
        subscriptionOptionId: String? = this.subscriptionOptionId,
    ): ReceiptInfo {
        return if (type == ProductType.SUBS) {
            val productDetails = createMockProductDetailsFreeTrial(productId, 2.00)

            val storeProduct = productDetails.toStoreProduct(
                productDetails.subscriptionOfferDetails!!,
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
        subscriptionOptionId: String? = this.subscriptionOptionId,
    ): ReceiptInfo {
        val productId = storeProduct.purchasingData.productId

        val platformProductIds = listOf(mutableMapOf(
            "product_id" to productId,
        ))
        if (storeProduct.type == ProductType.SUBS && subscriptionOptionId != null) {
            platformProductIds[0]["base_plan_id"] = subscriptionOptionId
        }

        val receiptInfo = ReceiptInfo(
            productIDs = listOf(productId),
            presentedOfferingContext = presentedOfferingContext,
            price = storeProduct.price.amountMicros.div(SharedConstants.MICRO_MULTIPLIER),
            formattedPrice = storeProduct.price.formatted,
            currency = storeProduct.price.currencyCode,
            period = storeProduct.period,
            pricingPhases = storeProduct.subscriptionOptions?.firstOrNull { it.id == subscriptionOptionId }?.pricingPhases,
            platformProductIds = platformProductIds,
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
