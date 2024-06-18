package com.revenuecat.purchases.amazon

import android.content.Context
import android.os.Handler
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.ProductType
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.RequestId
import com.amazon.device.iap.model.UserData
import com.amazon.device.iap.model.UserDataRequest
import com.revenuecat.purchases.AmazonLWAConsentStatus
import com.revenuecat.purchases.PostReceiptInitiationSource
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesState
import com.revenuecat.purchases.PurchasesStateCache
import com.revenuecat.purchases.amazon.handler.ProductDataHandler
import com.revenuecat.purchases.amazon.handler.PurchaseHandler
import com.revenuecat.purchases.amazon.handler.PurchaseUpdatesHandler
import com.revenuecat.purchases.amazon.handler.UserDataHandler
import com.revenuecat.purchases.amazon.helpers.dummyAmazonProduct
import com.revenuecat.purchases.amazon.helpers.dummyReceipt
import com.revenuecat.purchases.amazon.helpers.dummyUserData
import com.revenuecat.purchases.amazon.helpers.successfulRVSResponse
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.sha1
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.add
import com.revenuecat.purchases.utils.subtract
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class AmazonBillingTest {
    private companion object {
        const val timestamp0 = 1676379370000 // Tuesday, February 14, 2023 12:56:10.000 PM GMT
        const val timestamp123 = 1676379370123 // Tuesday, February 14, 2023 12:56:10.123 PM GMT
    }

    private val appUserID: String = "appUserID"
    private val countryCode = "JP"
    private lateinit var underTest: AmazonBilling

    private val mockPurchasingServiceProvider = mockk<PurchasingServiceProvider>()
    private val mockPurchaseUpdatesHandler = mockk<PurchaseUpdatesHandler>()
    private val mockProductDataHandler = mockk<ProductDataHandler>()
    private val mockUserDataHandler = mockk<UserDataHandler>()
    private val mockPurchaseHandler = mockk<PurchaseHandler>()
    private var handler = mockk<Handler>()

    private val mockAmazonBackend = mockk<AmazonBackend>()
    private val mockCache = mockk<AmazonCache>()
    private val mockContext = mockk<Context>()
    private var mockDiagnosticsTracker: DiagnosticsTracker = mockk()
    private var mockDateProvider: DateProvider = mockk()

    private val capturedCachedReceiptSkus = slot<Map<String, String>>()

    fun setup() {
        underTest = AmazonBilling(
            applicationContext = mockContext,
            amazonBackend = mockAmazonBackend,
            cache = mockCache,
            finishTransactions = true,
            purchasingServiceProvider = mockPurchasingServiceProvider,
            productDataHandler = mockProductDataHandler,
            purchaseHandler = mockPurchaseHandler,
            purchaseUpdatesHandler = mockPurchaseUpdatesHandler,
            userDataHandler = mockUserDataHandler,
            mainHandler = handler,
            stateProvider = PurchasesStateCache(PurchasesState()),
            diagnosticsTrackerIfEnabled = mockDiagnosticsTracker,
            dateProvider = mockDateProvider,
        )

        mockSetupFunctions()
        setupRunnables()
        mockDiagnosticsTracker()
        every { mockDateProvider.now } returns Date(1676379370000) // Tuesday, February 14, 2023 12:56:10 PM GMT

        underTest.purchasesUpdatedListener = mockk()
    }

    fun setupNotFinishingTransactions() {
        underTest = AmazonBilling(
            applicationContext = mockContext,
            amazonBackend = mockAmazonBackend,
            cache = mockCache,
            finishTransactions = false,
            mainHandler = handler,
            purchasingServiceProvider = mockPurchasingServiceProvider,
            productDataHandler = mockProductDataHandler,
            purchaseHandler = mockPurchaseHandler,
            purchaseUpdatesHandler = mockPurchaseUpdatesHandler,
            userDataHandler = mockUserDataHandler,
            stateProvider = PurchasesStateCache(PurchasesState()),
            diagnosticsTrackerIfEnabled = mockDiagnosticsTracker,
        )

        mockSetupFunctions()
        setupRunnables()
        mockDiagnosticsTracker()
        every { mockDateProvider.now } returns Date(1676379370000) // Tuesday, February 14, 2023 12:56:10 PM GMT

        underTest.purchasesUpdatedListener = mockk()
    }

    @Test
    fun `If there are no receipts, querying purchases returns an empty list`() {
        setup()
        every {
            mockPurchaseUpdatesHandler.queryPurchases(captureLambda(), any())
        } answers {
            lambda<(List<Receipt>, UserData) -> Unit>().captured.invoke(emptyList(), dummyUserData())
        }

        var receivedPurchases: Map<String, StoreTransaction>? = null
        underTest.queryPurchases(
            appUserID,
            onSuccess = {
                receivedPurchases = it
            },
            onError = {
                fail("Should be a success")
            }
        )

        assertThat(receivedPurchases).isNotNull
        assertThat(receivedPurchases).isEmpty()
    }

    @Test
    fun `If there are subscription receipts, received purchases have term skus, not skus`() {
        setup()
        val expectedTermSku = "sub_sku.monthly"
        val dummyReceipt = dummyReceipt(sku = "sub_sku")
        val dummyUserData = dummyUserData()

        mockQueryPurchases(listOf(dummyReceipt), dummyUserData)

        mockEmptyCache()

        mockGetAmazonReceiptData(dummyReceipt, dummyUserData, expectedTermSku)

        var receivedPurchases: Map<String, StoreTransaction>? = null
        underTest.queryPurchases(
            appUserID,
            onSuccess = {
                receivedPurchases = it
            },
            onError = {
                fail("Should be a success")
            }
        )

        assertThat(receivedPurchases).isNotNull
        assertThat(receivedPurchases!!.size).isOne
        checkPurchaseIsCorrect(
            receivedPurchases!!.values.toList()[0],
            expectedTermSku,
            dummyUserData,
            PurchaseState.UNSPECIFIED_STATE
        )
    }

    @Test
    fun `When querying purchases, receipt data is fetched for all subscriptions`() {
        setup()
        val dummyReceiptA = dummyReceipt(sku = "sub_sku_a", receiptId = "receipt_a")
        val dummyReceiptB = dummyReceipt(sku = "sub_sku_b", receiptId = "receipt_b")

        val expectedTermSkuA = "sub_sku_a.monthly"
        val expectedTermSkuB = "sub_sku_b.monthly"

        val dummyUserData = dummyUserData()

        mockQueryPurchases(listOf(dummyReceiptA, dummyReceiptB), dummyUserData)

        mockEmptyCache()

        mockGetAmazonReceiptData(dummyReceiptA, dummyUserData, expectedTermSkuA)

        mockGetAmazonReceiptData(dummyReceiptB, dummyUserData, expectedTermSkuB)

        var receivedPurchases: Map<String, StoreTransaction>? = null
        underTest.queryPurchases(
            appUserID,
            onSuccess = {
                receivedPurchases = it
            },
            onError = {
                fail("Should be a success")
            }
        )

        assertThat(receivedPurchases).isNotNull
        assertThat(receivedPurchases!!.size).isEqualTo(2)

        val purchaseA = receivedPurchases!!.values.first { it.productIds[0] == expectedTermSkuA }
        checkPurchaseIsCorrect(
            purchaseA,
            expectedTermSkuA,
            dummyUserData,
            PurchaseState.UNSPECIFIED_STATE
        )

        val purchaseB = receivedPurchases!!.values.first { it.productIds[0] == expectedTermSkuB }
        checkPurchaseIsCorrect(
            purchaseB,
            expectedTermSkuB,
            dummyUserData,
            PurchaseState.UNSPECIFIED_STATE
        )
    }

    @Test
    fun `When querying purchases, canceled but not expired subscriptions are returned`() {
        setup()
        val expectedNotExpiredTermSku = "expired_sku.monthly"
        val dummyReceiptNotExpired = dummyReceipt(
            receiptId = "not_expired_id",
            sku = "not_expired_sku",
            cancelDate = Date().add(1.hours)
        )

        val dummyUserData = dummyUserData()

        mockQueryPurchases(listOf(dummyReceiptNotExpired), dummyUserData)

        mockEmptyCache()

        mockGetAmazonReceiptData(dummyReceiptNotExpired, dummyUserData, expectedNotExpiredTermSku)

        var receivedPurchases: Map<String, StoreTransaction>? = null
        underTest.queryPurchases(
            appUserID,
            onSuccess = {
                receivedPurchases = it
            },
            onError = {
                fail("Should be a success")
            }
        )

        assertThat(receivedPurchases).isNotNull
        assertThat(receivedPurchases!!.keys.size).isEqualTo(1)
        checkPurchaseIsCorrect(
            receivedPurchases!![dummyReceiptNotExpired.receiptId.sha1()]!!,
            expectedNotExpiredTermSku,
            dummyUserData,
            PurchaseState.UNSPECIFIED_STATE
        )
    }

    @Test
    fun `When querying purchases, expired subscriptions are not returned`() {
        setup()
        val expectedTermSku = "sub_sku.monthly"
        val expectedNotExpiredTermSku = "expired_sku.monthly"
        val dummyReceipt = dummyReceipt(receiptId = "dummy_id", sku = "sub_sku")
        val dummyReceiptExpired = dummyReceipt(
            receiptId = "expired_id",
            sku = "expired_sku",
            cancelDate = Date(timestamp0).subtract(1.hours)
        )
        val dummyReceiptNotExpired = dummyReceipt(
            receiptId = "not_expired_id",
            sku = "not_expired_sku",
            cancelDate = Date(timestamp0).add(1.hours)
        )

        val dummyUserData = dummyUserData()

        mockQueryPurchases(listOf(dummyReceipt, dummyReceiptExpired, dummyReceiptNotExpired), dummyUserData)

        mockEmptyCache()

        mockGetAmazonReceiptData(dummyReceipt, dummyUserData, expectedTermSku)
        mockGetAmazonReceiptData(dummyReceiptNotExpired, dummyUserData, expectedNotExpiredTermSku)

        var receivedPurchases: Map<String, StoreTransaction>? = null
        underTest.queryPurchases(
            appUserID,
            onSuccess = {
                receivedPurchases = it
            },
            onError = {
                fail("Should be a success")
            }
        )

        assertThat(receivedPurchases).isNotNull
        assertThat(receivedPurchases!!.keys.size).isEqualTo(2)
        checkPurchaseIsCorrect(
            receivedPurchases!![dummyReceipt.receiptId.sha1()]!!,
            expectedTermSku,
            dummyUserData,
            PurchaseState.UNSPECIFIED_STATE
        )
        checkPurchaseIsCorrect(
            receivedPurchases!![dummyReceiptNotExpired.receiptId.sha1()]!!,
            expectedNotExpiredTermSku,
            dummyUserData,
            PurchaseState.UNSPECIFIED_STATE
        )
    }

    @Test
    fun `When querying purchases, consumables don't need to fetch receipt data`() {
        setup()
        val expectedSku = "sub_sku"
        val dummyReceipt = dummyReceipt(sku = expectedSku, productType = ProductType.CONSUMABLE)
        val dummyUserData = dummyUserData()

        mockQueryPurchases(listOf(dummyReceipt), dummyUserData)

        mockEmptyCache()

        var receivedPurchases: Map<String, StoreTransaction>? = null
        underTest.queryPurchases(
            appUserID,
            onSuccess = {
                receivedPurchases = it
            },
            onError = {
                fail("Should be a success")
            }
        )

        verify(exactly = 0) { mockAmazonBackend.getAmazonReceiptData(any(), any(), any(), any()) }

        assertThat(receivedPurchases).isNotNull
        assertThat(receivedPurchases!!.size).isOne
        checkPurchaseIsCorrect(
            receivedPurchases!!.values.toList()[0],
            expectedSku,
            dummyUserData,
            PurchaseState.UNSPECIFIED_STATE
        )
    }

    @Test
    fun `When querying purchases, cached tokens don't need to fetch receipt data`() {
        setup()
        val dummyReceiptA = dummyReceipt(sku = "sub_sku_a", receiptId = "receipt_a")
        val dummyReceiptB = dummyReceipt(sku = "sub_sku_b", receiptId = "receipt_b")

        val expectedTermSkuA = "sub_sku_a.monthly"

        val dummyUserData = dummyUserData()

        mockQueryPurchases(listOf(dummyReceiptA, dummyReceiptB), dummyUserData)

        every {
            mockCache.getReceiptSkus()
        } returns mapOf(dummyReceiptA.receiptId to expectedTermSkuA)

        mockGetAmazonReceiptData(dummyReceiptB, dummyUserData, "sub_sku_b.monthly")

        var receivedPurchases: Map<String, StoreTransaction>? = null
        underTest.queryPurchases(
            appUserID,
            onSuccess = {
                receivedPurchases = it
            },
            onError = {
                fail("Should be a success")
            }
        )

        verifyBackendCalled(dummyReceiptA, dummyUserData, times = 0)
        verifyBackendCalled(dummyReceiptB, dummyUserData)

        assertThat(receivedPurchases).isNotNull
        assertThat(receivedPurchases!!.size).isEqualTo(2)

        val purchaseA = receivedPurchases!!.values.first { it.productIds[0] == expectedTermSkuA }
        checkPurchaseIsCorrect(
            purchaseA,
            expectedTermSkuA,
            dummyUserData,
            PurchaseState.UNSPECIFIED_STATE
        )

        val purchaseB = receivedPurchases!!.values.first { it.productIds[0] == "sub_sku_b.monthly" }
        checkPurchaseIsCorrect(
            purchaseB,
            "sub_sku_b.monthly",
            dummyUserData,
            PurchaseState.UNSPECIFIED_STATE
        )
    }

    @Test
    fun `If there are errors getting receipt data for all receipts, an InvalidReceiptError is passed`() {
        setup()
        mockEmptyCache()
        val expectedSkuA = "sub_sku_a"
        val expectedSkuB = "sub_sku_b"

        val dummyReceiptA = dummyReceipt(sku = expectedSkuA, receiptId = "receipt_a")
        val dummyReceiptB = dummyReceipt(sku = expectedSkuB, receiptId = "receipt_b")

        val dummyUserData = dummyUserData()

        mockQueryPurchases(listOf(dummyReceiptA, dummyReceiptB), dummyUserData)

        val expectedError = PurchasesError(PurchasesErrorCode.NetworkError)

        every {
            mockAmazonBackend.getAmazonReceiptData(
                receiptId = or(dummyReceiptA.receiptId, dummyReceiptB.receiptId),
                storeUserID = dummyUserData.userId,
                onSuccess = any(),
                onError = captureLambda()
            )
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(expectedError)
        }

        var receivedError: PurchasesError? = null
        underTest.queryPurchases(
            appUserID,
            onSuccess = {
                fail("Should be an error")
            },
            onError = {
                receivedError = it
            }
        )

        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.InvalidReceiptError)
    }

    @Test
    fun `Success is called when there are errors getting receipt data for just one of the receipts`() {
        setup()
        val dummyReceiptInCache = dummyReceipt(sku = "sub_sku_a", receiptId = "receipt_a")
        val dummyReceiptThatErrors = dummyReceipt(sku = "sub_sku_b", receiptId = "receipt_b")
        val dummySuccessfulReceipt = dummyReceipt(sku = "sub_sku_c", receiptId = "receipt_c")

        val dummyUserData = dummyUserData()

        mockQueryPurchases(listOf(dummyReceiptInCache, dummyReceiptThatErrors, dummySuccessfulReceipt), dummyUserData)

        every {
            mockCache.getReceiptSkus()
        } returns mapOf(dummyReceiptInCache.receiptId to "sub_sku_a.monthly")

        val expectedError = PurchasesError(PurchasesErrorCode.NetworkError)
        every {
            mockAmazonBackend.getAmazonReceiptData(
                receiptId = dummyReceiptThatErrors.receiptId,
                storeUserID = dummyUserData.userId,
                onSuccess = any(),
                onError = captureLambda()
            )
        } answers {
            lambda<(PurchasesError) -> Unit>().captured.invoke(expectedError)
        }

        mockGetAmazonReceiptData(dummySuccessfulReceipt, dummyUserData, "sub_sku_c.monthly")

        lateinit var receivedPurchases: Map<String, StoreTransaction>
        var successCalled: Boolean = false
        underTest.queryPurchases(
            appUserID,
            onSuccess = {
                successCalled = true
                receivedPurchases = it
            },
            onError = {
                fail("Should be a success")
            }
        )

        assertThat(successCalled).isTrue()
        assertThat(receivedPurchases).isNotNull
        assertThat(receivedPurchases.size).isEqualTo(2)

        val purchaseB = receivedPurchases.values.firstOrNull { it.purchaseToken == dummyReceiptThatErrors.receiptId }
        assertThat(purchaseB).isNull()
    }

    @Test
    fun `Correct marketplace is used when getting product data`() {
        setup()
        underTest.startConnection()

        val productIds = setOf("sku", "sku_2")

        mockGetUserData()

        val marketplaceSlot = slot<String>()
        val storeProducts = listOfNotNull(dummyAmazonProduct().toStoreProduct(countryCode))

        every {
            mockProductDataHandler.getProductData(productIds, capture(marketplaceSlot), captureLambda(), any())
        } answers {
            lambda<(List<StoreProduct>) -> Unit>().captured.invoke(storeProducts)
        }

        var onReceiveCalled = false
        underTest.queryProductDetailsAsync(
            productType = com.revenuecat.purchases.ProductType.SUBS,
            productIds = productIds,
            onError = {
                fail("should be a success")
            },
            onReceive = {
                onReceiveCalled = true
            },
        )

        assertThat(onReceiveCalled).isTrue()
        assertThat(marketplaceSlot.captured).isEqualTo(countryCode)
    }

    @Test
    fun `If purchase state is pending, purchase is not fulfilled`() {
        setup()
        underTest.consumeAndSave(
            finishTransactions = true,
            purchase = dummyReceipt().toStoreTransaction(
                productId = "sku.monthly",
                presentedOfferingContext = null,
                purchaseState = PurchaseState.PENDING,
                userData = dummyUserData(
                    storeUserId = "store_user_id",
                    marketplace = "US"
                ),
            ),
            shouldConsume = true,
            initiationSource = PostReceiptInitiationSource.PURCHASE,
        )

        verify(exactly = 0) {
            mockPurchasingServiceProvider.notifyFulfillment(any(), any())
        }
    }

    @Test
    fun `If purchase state is "purchased", purchase is fulfilled and cached`() {
        setup()
        val dummyReceipt = dummyReceipt()

        every {
            mockPurchasingServiceProvider.notifyFulfillment(dummyReceipt.receiptId, FulfillmentResult.FULFILLED)
        } just Runs

        every {
            mockCache.addSuccessfullyPostedToken(dummyReceipt.receiptId)
        } just Runs

        underTest.consumeAndSave(
            finishTransactions = true,
            purchase = dummyReceipt.toStoreTransaction(
                productId = "sku.monthly",
                presentedOfferingContext = null,
                purchaseState = PurchaseState.PURCHASED,
                userData = dummyUserData(
                    storeUserId = "store_user_id",
                    marketplace = "US"
                )
            ),
            shouldConsume = true,
            initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
        )

        verify(exactly = 1) {
            mockPurchasingServiceProvider.notifyFulfillment(dummyReceipt.receiptId, FulfillmentResult.FULFILLED)
        }

        verify(exactly = 1) {
            mockCache.addSuccessfullyPostedToken(dummyReceipt.receiptId)
        }
    }

    @Test
    fun `Purchase is fulfilled and cached even if shouldConsume is false`() {
        setup()
        val dummyReceipt = dummyReceipt()

        every {
            mockPurchasingServiceProvider.notifyFulfillment(dummyReceipt.receiptId, FulfillmentResult.FULFILLED)
        } just Runs

        every {
            mockCache.addSuccessfullyPostedToken(dummyReceipt.receiptId)
        } just Runs

        underTest.consumeAndSave(
            finishTransactions = true,
            purchase = dummyReceipt.toStoreTransaction(
                productId = "sku.monthly",
                presentedOfferingContext = null,
                purchaseState = PurchaseState.PURCHASED,
                userData = dummyUserData(
                    storeUserId = "store_user_id",
                    marketplace = "US"
                )
            ),
            shouldConsume = false,
            initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
        )

        verify(exactly = 1) {
            mockPurchasingServiceProvider.notifyFulfillment(dummyReceipt.receiptId, FulfillmentResult.FULFILLED)
        }

        verify(exactly = 1) {
            mockCache.addSuccessfullyPostedToken(dummyReceipt.receiptId)
        }
    }

    @Test
    fun `If purchase shouldn't be finished, purchase is not fulfilled, but it is cached`() {
        setup()
        val dummyReceipt = dummyReceipt()

        every {
            mockPurchasingServiceProvider.notifyFulfillment(dummyReceipt.receiptId, FulfillmentResult.FULFILLED)
        } just Runs

        every {
            mockCache.addSuccessfullyPostedToken(dummyReceipt.receiptId)
        } just Runs

        underTest.consumeAndSave(
            finishTransactions = false,
            purchase = dummyReceipt.toStoreTransaction(
                productId = "sku.monthly",
                presentedOfferingContext = null,
                purchaseState = PurchaseState.PURCHASED,
                userData = dummyUserData(
                    storeUserId = "store_user_id",
                    marketplace = "US"
                )
            ),
            shouldConsume = true,
            initiationSource = PostReceiptInitiationSource.UNSYNCED_ACTIVE_PURCHASES,
        )

        verify(exactly = 0) {
            mockPurchasingServiceProvider.notifyFulfillment(dummyReceipt.receiptId, FulfillmentResult.FULFILLED)
        }

        verify(exactly = 1) {
            mockCache.addSuccessfullyPostedToken(dummyReceipt.receiptId)
        }
    }

    @Test
    fun `If there are receipts, querying all purchases returns a list of purchases`() {
        setup()
        val expectedTermSku = "sub_sku.monthly"
        val dummyReceipt = dummyReceipt(sku = "sub_sku")
        val dummyUserData = dummyUserData()

        mockQueryPurchases(listOf(dummyReceipt), dummyUserData)

        mockEmptyCache()

        mockGetAmazonReceiptData(dummyReceipt, dummyUserData, expectedTermSku)

        var receivedPurchases: List<StoreTransaction>? = null
        underTest.queryAllPurchases(
            appUserID,
            onReceivePurchaseHistory = {
                receivedPurchases = it
            },
            onReceivePurchaseHistoryError = {
                fail("Should be a success")
            }
        )

        assertThat(receivedPurchases).isNotNull
        assertThat(receivedPurchases!!.size).isOne
        checkPurchaseIsCorrect(
            receivedPurchases!![0],
            expectedTermSku,
            dummyUserData,
            PurchaseState.UNSPECIFIED_STATE
        )
    }
//
//    @Test
//    fun `Term sku is used when purchasing subscriptions`() {
//        setup()
//        val appUserID = "appUserID"
//        val storeProduct = dummyAmazonProduct().toStoreProduct("US")
//        val dummyReceipt = dummyReceipt()
//        val dummyUserData = dummyUserData()
//        val expectedTermSku = "sku.monthly"
//
//        every {
//            mockPurchasingServiceProvider.registerListener(mockContext, any())
//        } just Runs
//
//        var receivedPurchases: List<StoreTransaction>? = null
//        underTest.purchasesUpdatedListener = object : BillingAbstract.PurchasesUpdatedListener {
//            override fun onPurchasesUpdated(purchases: List<StoreTransaction>) {
//                receivedPurchases = purchases
//            }
//
//            override fun onPurchasesFailedToUpdate(purchasesError: PurchasesError) {
//                fail<String>("should be success")
//            }
//        }
//
//        assertThat(storeProduct).isNotNull
//
//        val activity = mockk<Activity>()
//        every {
//            mockPurchaseHandler.purchase(
//                handler,
//                activity,
//                appUserID,
//                storeProduct!!,
//                presentedOfferingIdentifier = null,
//                onSuccess = captureLambda(),
//                onError = any()
//            )
//        } answers {
//            lambda<(Receipt, UserData) -> Unit>().captured.invoke(dummyReceipt, dummyUserData)
//        }
//
//        mockGetAmazonReceiptData(dummyReceipt, dummyUserData, expectedTermSku)
//
//        underTest.makePurchaseAsync(
//            activity,
//            appUserID,
//            storeProduct = storeProduct!!,
//            replaceSkuInfo = null,
//            presentedOfferingIdentifier = null
//        )
//
//        assertThat(receivedPurchases).isNotNull
//        assertThat(receivedPurchases!!.size).isOne
//        checkPurchaseIsCorrect(
//            receivedPurchases!![0],
//            expectedTermSku,
//            dummyUserData,
//            PurchaseState.PURCHASED
//        )
//    }

//    @Test
//    fun `Sku is used when purchasing consumables`() {
//        setup()
//        val appUserID = "appUserID"
//        val sku = "sku"
//        val storeProduct = dummyAmazonProduct(
//            sku = sku,
//            productType = ProductType.CONSUMABLE
//        ).toStoreProduct("US")
//        val dummyReceipt = dummyReceipt(
//            sku = sku,
//            productType = ProductType.CONSUMABLE
//        )
//        val dummyUserData = dummyUserData()
//
//        every {
//            mockPurchasingServiceProvider.registerListener(mockContext, any())
//        } just Runs
//
//        var receivedPurchases: List<StoreTransaction>? = null
//        underTest.purchasesUpdatedListener = object : BillingAbstract.PurchasesUpdatedListener {
//            override fun onPurchasesUpdated(purchases: List<StoreTransaction>) {
//                receivedPurchases = purchases
//            }
//
//            override fun onPurchasesFailedToUpdate(purchasesError: PurchasesError) {
//                fail<String>("should be success")
//            }
//        }
//
//        assertThat(storeProduct).isNotNull
//
//        val activity = mockk<Activity>()
//        every {
//            mockPurchaseHandler.purchase(
//                handler,
//                activity,
//                appUserID,
//                storeProduct!!,
//                presentedOfferingIdentifier = null,
//                onSuccess = captureLambda(),
//                onError = any()
//            )
//        } answers {
//            lambda<(Receipt, UserData) -> Unit>().captured.invoke(dummyReceipt, dummyUserData)
//        }
//
//        underTest.makePurchaseAsync(
//            activity,
//            appUserID,
//            storeProduct = storeProduct!!,
//            replaceSkuInfo = null,
//            presentedOfferingIdentifier = null
//        )
//
//        assertThat(receivedPurchases).isNotNull
//        assertThat(receivedPurchases!!.size).isOne
//        checkPurchaseIsCorrect(
//            receivedPurchases!![0],
//            sku,
//            dummyUserData,
//            PurchaseState.PURCHASED
//        )
//    }

    @Test
    fun `querying all purchases returns all purchases, even if they were canceled previously`() {
        setup()
        val expectedTermSku = "sub_sku.monthly"
        val expectedExpiredTermSku = "expired_sku.monthly"
        val expectedNotExpiredTermSku = "expired_sku.monthly"
        val dummyReceipt = dummyReceipt(receiptId = "dummy_id", sku = "sub_sku")
        val dummyReceiptExpired = dummyReceipt(
            receiptId = "expired_id",
            sku = "expired_sku",
            cancelDate = Date().subtract(1.hours)
        )
        val dummyReceiptNotExpired = dummyReceipt(
            receiptId = "not_expired_id",
            sku = "not_expired_sku",
            cancelDate = Date().add(1.hours)
        )

        val dummyUserData = dummyUserData()

        mockQueryPurchases(listOf(dummyReceipt, dummyReceiptExpired, dummyReceiptNotExpired), dummyUserData)

        mockEmptyCache()

        mockGetAmazonReceiptData(dummyReceipt, dummyUserData, expectedTermSku)
        mockGetAmazonReceiptData(dummyReceiptExpired, dummyUserData, expectedExpiredTermSku)
        mockGetAmazonReceiptData(dummyReceiptNotExpired, dummyUserData, expectedNotExpiredTermSku)

        var receivedPurchases: List<StoreTransaction>? = null
        underTest.queryAllPurchases(
            appUserID,
            onReceivePurchaseHistory = {
                receivedPurchases = it
            },
            onReceivePurchaseHistoryError = {
                fail("Should be a success")
            }
        )

        assertThat(receivedPurchases).isNotNull
        assertThat(receivedPurchases!!.size).isEqualTo(3)
        checkPurchaseIsCorrect(
            receivedPurchases!![0],
            expectedTermSku,
            dummyUserData,
            PurchaseState.UNSPECIFIED_STATE
        )
        checkPurchaseIsCorrect(
            receivedPurchases!![1],
            expectedExpiredTermSku,
            dummyUserData,
            PurchaseState.UNSPECIFIED_STATE
        )
        checkPurchaseIsCorrect(
            receivedPurchases!![2],
            expectedNotExpiredTermSku,
            dummyUserData,
            PurchaseState.UNSPECIFIED_STATE
        )
    }

    @Test
    fun `if not finishing transactions, registerListener not called`() {
        setupNotFinishingTransactions()
        every {
            mockPurchasingServiceProvider.registerListener(mockContext, any())
        } just Runs

        underTest.startConnection()
        verify(exactly = 0) {
            mockPurchasingServiceProvider.registerListener(any(), any())
        }
    }

    @Test
    fun `if not not finishing transactions, registerListener called`() {
        setup()
        every {
            mockPurchasingServiceProvider.registerListener(mockContext, any())
        } just Runs

        underTest.startConnection()

        verify(exactly = 1) {
            mockPurchasingServiceProvider.registerListener(any(), any())
        }
    }

    @Test
    fun `on connection, stateListener called`() {
        setup()
        val stateListenerMock = mockk<BillingAbstract.StateListener>()
        underTest.stateListener = stateListenerMock
        every { stateListenerMock.onConnected() } just Runs

        every {
            mockPurchasingServiceProvider.registerListener(mockContext, any())
        } just Runs

        underTest.startConnection()

        verify(exactly = 1) {
            stateListenerMock.onConnected()
        }
    }

    @Test
    fun `When normalizing purchase data, term sku is returned when passing parent sku as product ID`() {
        setup()
        val parentSku = "sub_sku"
        val expectedTermSku = "sub_sku.monthly"
        val expectedToken = "token"
        val expectedUserId = "store_user_id"
        val dummyReceipt = dummyReceipt(sku = parentSku, receiptId = expectedToken)
        val dummyUserData = dummyUserData(storeUserId = expectedUserId)

        mockEmptyCache()

        mockGetAmazonReceiptData(dummyReceipt, dummyUserData, expectedTermSku)

        var receivedCorrectProductID: String? = null
        underTest.normalizePurchaseData(
            productID = parentSku,
            purchaseToken = expectedToken,
            storeUserID = expectedUserId,
            onSuccess = { correctProductID ->
                receivedCorrectProductID = correctProductID
            },
            onError = {
                fail("Should be a success")
            }
        )

        assertThat(receivedCorrectProductID).isEqualTo(expectedTermSku)
    }

    @Test
    fun `When normalizing purchase data, cached term sku is returned as product ID`() {
        setup()
        val parentSku = "sub_sku"
        val expectedTermSku = "sub_sku.monthly"
        val expectedToken = "token"
        val expectedUserId = "store_user_id"
        val dummyReceiptInCache = dummyReceipt(sku = parentSku, receiptId = expectedToken)

        every {
            mockCache.getReceiptSkus()
        } returns mapOf(dummyReceiptInCache.receiptId to expectedTermSku)

        var receivedCorrectProductID: String? = null
        underTest.normalizePurchaseData(
            productID = parentSku,
            purchaseToken = expectedToken,
            storeUserID = expectedUserId,
            onSuccess = { correctProductID ->
                receivedCorrectProductID = correctProductID
            },
            onError = {
                fail("Should be a success")
            }
        )

        assertThat(receivedCorrectProductID).isEqualTo(expectedTermSku)
    }

    @Test
    fun `When normalizing purchase data, an error is returned if response from backend is not complete`() {
        setup()
        val parentSku = "sub_sku"
        val expectedTermSku = "sub_sku.monthly"
        val expectedToken = "token"
        val expectedUserId = "store_user_id"
        val dummyReceipt = dummyReceipt(sku = parentSku, receiptId = expectedToken)
        val dummyUserData = dummyUserData(storeUserId = expectedUserId)

        mockEmptyCache()

        val jsonObject = JSONObject(successfulRVSResponse(expectedTermSku))
        jsonObject.remove("termSku")
        every {
            mockAmazonBackend.getAmazonReceiptData(
                receiptId = dummyReceipt.receiptId,
                storeUserID = dummyUserData.userId,
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<(JSONObject) -> Unit>().captured.invoke(jsonObject)
        }

        var receivedError: PurchasesError? = null
        underTest.normalizePurchaseData(
            productID = parentSku,
            purchaseToken = expectedToken,
            storeUserID = expectedUserId,
            onSuccess = {
                fail("Should be a failure")
            },
            onError = { error ->
                receivedError = error
            }
        )

        assertThat(receivedError).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.UnexpectedBackendResponseError)
    }

    @Test
    fun `When normalizing purchase data, term sku is returned when passing term sku as product ID`() {
        setup()
        val parentSku = "sub_sku"
        val expectedTermSku = "sub_sku.monthly"
        val expectedToken = "token"
        val expectedUserId = "store_user_id"
        val dummyReceipt = dummyReceipt(sku = parentSku, receiptId = expectedToken)
        val dummyUserData = dummyUserData(storeUserId = expectedUserId)

        mockEmptyCache()

        mockGetAmazonReceiptData(dummyReceipt, dummyUserData, expectedTermSku)

        var receivedCorrectProductID: String? = null
        underTest.normalizePurchaseData(
            productID = expectedTermSku,
            purchaseToken = expectedToken,
            storeUserID = expectedUserId,
            onSuccess = { correctProductID ->
                receivedCorrectProductID = correctProductID
            },
            onError = {
                fail("Should be a success")
            }
        )

        assertThat(receivedCorrectProductID).isEqualTo(expectedTermSku)
    }

    @Test
    fun `querying storefront success`() {
        setup()
        mockGetUserData()
        var countryCode: String? = null
        underTest.getStorefront(
            onSuccess = { countryCode = it },
            onError = { fail("Should be a success") },
        )
        assertThat(countryCode).isEqualTo(countryCode)
    }

    @Test
    fun `querying storefront error`() {
        setup()
        mockGetUserDataError()
        var error: PurchasesError? = null
        underTest.getStorefront(
            onSuccess = { fail("Should be a error") },
            onError = { error = it },
        )
        assertThat(error?.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `querying getAmazonLWAConsentStatus returns error`() {
        setup()
        mockGetUserDataError()
        var error: PurchasesError? = null
        underTest.getAmazonLWAConsentStatus(
            onSuccess = { fail("Should be a error") },
            onError = { error = it },
        )
        assertThat(error?.code).isEqualTo(PurchasesErrorCode.StoreProblemError)
    }

    @Test
    fun `querying getAmazonLWAConsentStatus returns success`() {
        setup()
        mockGetUserData()
        var consent: AmazonLWAConsentStatus? = null
        underTest.getAmazonLWAConsentStatus(
            onSuccess = { consent = it },
            onError = { fail("Should be success") }
        )
        assertThat(consent).isEqualTo(AmazonLWAConsentStatus.CONSENTED)
    }

    @Test
    fun `calling getUserData() in DefaultPurchasingServiceProvider sets LWA flag in builder`() {
        setup()

        mockkStatic(UserDataRequest::class)
        mockkStatic(PurchasingService::class)

        val mockBuilder = mockk<UserDataRequest.Builder>(relaxed = true)
        every {
            UserDataRequest.newBuilder()
        } returns mockBuilder

        val fetchLWAConsentStatusSlot = slot<Boolean>()
        every {
            mockBuilder.setFetchLWAConsentStatus(capture(fetchLWAConsentStatusSlot))
        } returns mockBuilder

        every {
            PurchasingService.getUserData(any())
        } returns mockk<RequestId>()

        DefaultPurchasingServiceProvider().getUserData()
        assertThat(fetchLWAConsentStatusSlot.captured).isTrue()
    }

    // region diagnostics tracking

    @Test
    fun `queryPurchases tracks diagnostics call with correct parameters`() {
        setup()
        every { mockDateProvider.now } returnsMany listOf(Date(timestamp0), Date(timestamp123))

        val expectedTermSku = "sub_sku.monthly"
        val dummyReceipt = dummyReceipt(sku = "sub_sku")
        val dummyUserData = dummyUserData()

        mockQueryPurchases(listOf(dummyReceipt), dummyUserData)

        mockEmptyCache()
        mockGetAmazonReceiptData(dummyReceipt, dummyUserData, expectedTermSku)

        var receivedPurchases: Map<String, StoreTransaction>? = null
        underTest.queryPurchases(
            appUserID,
            onSuccess = {
                receivedPurchases = it
            },
            onError = {
                fail("Should be a success")
            }
        )
        assertThat(receivedPurchases).isNotNull

        verify(exactly = 1) {
            mockDiagnosticsTracker.trackAmazonQueryPurchasesRequest(
                responseTime = 123.milliseconds,
                wasSuccessful = true,
            )
        }
    }

    @Test
    fun `queryProductDetailsAsync tracks diagnostics call with correct parameters`() {
        setup()
        every { mockDateProvider.now } returnsMany listOf(Date(timestamp0), Date(timestamp123))

        underTest.startConnection()

        val productIds = setOf("sku", "sku_2")
        mockGetUserData()

        val marketplaceSlot = slot<String>()
        val storeProducts = listOfNotNull(dummyAmazonProduct().toStoreProduct(countryCode))

        every {
            mockProductDataHandler.getProductData(productIds, capture(marketplaceSlot), captureLambda(), any())
        } answers {
            lambda<(List<StoreProduct>) -> Unit>().captured.invoke(storeProducts)
        }

        var onReceiveCalled = false
        underTest.queryProductDetailsAsync(
            productType = com.revenuecat.purchases.ProductType.SUBS,
            productIds = productIds,
            onError = {
                fail("should be a success")
            },
            onReceive = {
                onReceiveCalled = true
            },
        )

        assertThat(onReceiveCalled).isTrue()
        verify(exactly = 1) {
            mockDiagnosticsTracker.trackAmazonQueryProductDetailsRequest(
                responseTime = 123.milliseconds,
                wasSuccessful = true,
            )
        }
    }

    // endregion

    private fun verifyBackendCalled(
        receipt: Receipt,
        dummyUserData: UserData,
        times: Int = 1
    ) {
        verify(exactly = times) {
            mockAmazonBackend.getAmazonReceiptData(
                receiptId = receipt.receiptId,
                storeUserID = dummyUserData.userId,
                onSuccess = any(),
                onError = any()
            )
        }
    }

    private fun checkPurchaseIsCorrect(
        purchase: StoreTransaction,
        expectedSku: String,
        dummyUserData: UserData,
        purchaseState: PurchaseState
    ) {
        with(purchase) {
            assertThat(skus[0]).isEqualTo(expectedSku)
            assertThat(presentedOfferingIdentifier).isNull()
            assertThat(purchaseState).isEqualTo(purchaseState)
            assertThat(storeUserID).isEqualTo(dummyUserData.userId)
        }
    }

    private fun mockEmptyCache() {
        every {
            mockCache.getReceiptSkus()
        } returns emptyMap()
    }

    private fun mockQueryPurchases(
        receipts: List<Receipt>,
        userData: UserData
    ) {
        every {
            mockPurchaseUpdatesHandler.queryPurchases(captureLambda(), any())
        } answers {
            lambda<(List<Receipt>, UserData) -> Unit>().captured.invoke(receipts, userData)
        }
    }

    private fun mockGetAmazonReceiptData(
        receipt: Receipt,
        userData: UserData,
        expectedTermSku: String
    ) {
        every {
            mockAmazonBackend.getAmazonReceiptData(
                receiptId = receipt.receiptId,
                storeUserID = userData.userId,
                onSuccess = captureLambda(),
                onError = any()
            )
        } answers {
            lambda<(JSONObject) -> Unit>().captured.invoke(JSONObject(successfulRVSResponse(expectedTermSku)))
        }
    }

    private fun mockGetUserData(userData: UserData = dummyUserData(marketplace = countryCode)) {
        every {
            mockUserDataHandler.getUserData(captureLambda(), any())
        } answers {
            lambda<(UserData) -> Unit>().captured.invoke(userData)
        }
    }

    private fun mockGetUserDataError(error: PurchasesError = PurchasesError(PurchasesErrorCode.StoreProblemError)) {
        every {
            mockUserDataHandler.getUserData(any(), captureLambda())
        } answers {
            lambda<PurchasesErrorCallback>().captured.invoke(error)
        }
    }

    private fun mockSetupFunctions() {
        every {
            mockCache.cacheSkusByToken(capture(capturedCachedReceiptSkus))
        } just Runs

        every {
            mockPurchasingServiceProvider.registerListener(mockContext, any())
        } just Runs
    }

    private fun setupRunnables() {
        val slot = slot<Runnable>()
        every {
            handler.post(capture(slot))
        } answers {
            slot.captured.run()
            true
        }

        val delayedSlot = slot<Runnable>()
        every {
            handler.postDelayed(capture(delayedSlot), any())
        } answers {
            delayedSlot.captured.run()
            true
        }
    }

    private fun mockDiagnosticsTracker() {
        every {
            mockDiagnosticsTracker.trackAmazonQueryPurchasesRequest(any(), any())
        } just Runs
        every {
            mockDiagnosticsTracker.trackAmazonQueryProductDetailsRequest(any(), any())
        } just Runs
    }
}
