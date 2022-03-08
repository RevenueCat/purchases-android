package com.revenuecat.purchases.amazon

import android.content.Context
import android.os.Handler
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.ProductType
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.UserData
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.amazon.handler.ProductDataHandler
import com.revenuecat.purchases.amazon.handler.PurchaseHandler
import com.revenuecat.purchases.amazon.handler.PurchaseUpdatesHandler
import com.revenuecat.purchases.amazon.handler.UserDataHandler
import com.revenuecat.purchases.amazon.helpers.dummyAmazonProduct
import com.revenuecat.purchases.amazon.helpers.dummyReceipt
import com.revenuecat.purchases.amazon.helpers.dummyUserData
import com.revenuecat.purchases.amazon.helpers.successfulRVSResponse
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AmazonBillingTest {

    private val appUserID: String = "appUserID"
    private lateinit var underTest: AmazonBilling

    private val mockPurchasingServiceProvider = mockk<PurchasingServiceProvider>()
    private val mockPurchaseUpdatesHandler = mockk<PurchaseUpdatesHandler>()
    private val mockProductDataHandler = mockk<ProductDataHandler>()
    private val mockUserDataHandler = mockk<UserDataHandler>()
    private val mockPurchaseHandler = mockk<PurchaseHandler>()
    private var handler: Handler = mockk()

    private val mockAmazonBackend = mockk<AmazonBackend>()
    private val mockCache = mockk<AmazonCache>()
    private val mockContext = mockk<Context>()

    private val capturedCachedReceiptSkus = slot<Map<String, String>>()

    @Before
    fun setup() {
        underTest = AmazonBilling(
            applicationContext = mockContext,
            amazonBackend = mockAmazonBackend,
            cache = mockCache,
            observerMode = false,
            purchasingServiceProvider = mockPurchasingServiceProvider,
            productDataHandler = mockProductDataHandler,
            purchaseHandler = mockPurchaseHandler,
            purchaseUpdatesHandler = mockPurchaseUpdatesHandler,
            userDataHandler = mockUserDataHandler,
            mainHandler = handler
        )

        every {
            mockCache.setReceiptSkus(capture(capturedCachedReceiptSkus))
        } just Runs

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

    @Test
    fun `If there are no receipts, querying purchases returns an empty list`() {
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

        val purchaseA = receivedPurchases!!.values.first { it.skus[0] == expectedTermSkuA }
        checkPurchaseIsCorrect(
            purchaseA,
            expectedTermSkuA,
            dummyUserData,
            PurchaseState.UNSPECIFIED_STATE
        )

        val purchaseB = receivedPurchases!!.values.first { it.skus[0] == expectedTermSkuB }
        checkPurchaseIsCorrect(
            purchaseB,
            expectedTermSkuB,
            dummyUserData,
            PurchaseState.UNSPECIFIED_STATE
        )
    }

    @Test
    fun `When querying purchases, consumables don't need to fetch receipt data`() {
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

        val purchaseA = receivedPurchases!!.values.first { it.skus[0] == expectedTermSkuA }
        checkPurchaseIsCorrect(
            purchaseA,
            expectedTermSkuA,
            dummyUserData,
            PurchaseState.UNSPECIFIED_STATE
        )

        val purchaseB = receivedPurchases!!.values.first { it.skus[0] == "sub_sku_b.monthly" }
        checkPurchaseIsCorrect(
            purchaseB,
            "sub_sku_b.monthly",
            dummyUserData,
            PurchaseState.UNSPECIFIED_STATE
        )
    }

    @Test
    fun `If there are errors getting receipt data for all receipts, an InvalidReceiptError is passed`() {
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
        val skus = setOf("sku", "sku_2")
        val marketplace = "ES"
        val dummyUserData = dummyUserData(marketplace = marketplace)

        every {
            mockUserDataHandler.getUserData(captureLambda(), any())
        } answers {
            lambda<(UserData) -> Unit>().captured.invoke(dummyUserData)
        }

        val marketplaceSlot = slot<String>()
        val storeProduct = listOf(dummyAmazonProduct().toStoreProduct(marketplace))
        every {
            mockProductDataHandler.getProductData(skus, capture(marketplaceSlot), captureLambda(), any())
        } answers {
            lambda<(List<StoreProduct>) -> Unit>().captured.invoke(storeProduct)
        }

        var onReceiveCalled = false
        underTest.querySkuDetailsAsync(
            productType = com.revenuecat.purchases.ProductType.SUBS,
            skus = skus,
            onReceive = {
                onReceiveCalled = true
            },
            onError = {
                fail("should be a success")
            }
        )

        assertThat(onReceiveCalled).isTrue()
        assertThat(marketplaceSlot.captured).isEqualTo(marketplace)
    }

    @Test
    fun `If purchase state is pending, purchase is not fulfilled`() {
        underTest.consumeAndSave(
            shouldTryToConsume = true,
            purchase = dummyReceipt().toStoreTransaction(
                sku = "sku.monthly",
                presentedOfferingIdentifier = null,
                purchaseState = PurchaseState.PENDING,
                storeUserID = "store_user_id"
            )
        )

        verify(exactly = 0) {
            mockPurchasingServiceProvider.notifyFulfillment(any(), any())
        }
    }

    @Test
    fun `If purchase state is "purchased", purchase is fulfilled and cached`() {
        val dummyReceipt = dummyReceipt()

        every {
            mockPurchasingServiceProvider.notifyFulfillment(dummyReceipt.receiptId, FulfillmentResult.FULFILLED)
        } just Runs

        every {
            mockCache.addSuccessfullyPostedToken(dummyReceipt.receiptId)
        } just Runs

        underTest.consumeAndSave(
            shouldTryToConsume = true,
            purchase = dummyReceipt.toStoreTransaction(
                sku = "sku.monthly",
                presentedOfferingIdentifier = null,
                purchaseState = PurchaseState.PURCHASED,
                storeUserID = "store_user_id"
            )
        )

        verify(exactly = 1) {
            mockPurchasingServiceProvider.notifyFulfillment(dummyReceipt.receiptId, FulfillmentResult.FULFILLED)
        }

        verify(exactly = 1) {
            mockCache.addSuccessfullyPostedToken(dummyReceipt.receiptId)
        }
    }

    @Test
    fun `If purchase shouldn't be consumed, purchase is not fulfilled, but it is cached`() {
        val dummyReceipt = dummyReceipt()

        every {
            mockPurchasingServiceProvider.notifyFulfillment(dummyReceipt.receiptId, FulfillmentResult.FULFILLED)
        } just Runs

        every {
            mockCache.addSuccessfullyPostedToken(dummyReceipt.receiptId)
        } just Runs

        underTest.consumeAndSave(
            shouldTryToConsume = false,
            purchase = dummyReceipt.toStoreTransaction(
                sku = "sku.monthly",
                presentedOfferingIdentifier = null,
                purchaseState = PurchaseState.PURCHASED,
                storeUserID = "store_user_id"
            )
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

    @Test
    fun `Term sku is used when purchasing subscriptions`() {
        val appUserID = "appUserID"
        val storeProduct = dummyAmazonProduct().toStoreProduct("US")
        val dummyReceipt = dummyReceipt()
        val dummyUserData = dummyUserData()
        val expectedTermSku = "sku.monthly"

        every {
            mockPurchasingServiceProvider.registerListener(mockContext, any())
        } just Runs

        var receivedPurchases: List<StoreTransaction>? = null
        underTest.purchasesUpdatedListener = object : BillingAbstract.PurchasesUpdatedListener {
            override fun onPurchasesUpdated(purchases: List<StoreTransaction>) {
                receivedPurchases = purchases
            }

            override fun onPurchasesFailedToUpdate(purchasesError: PurchasesError) {
                fail<String>("should be success")
            }
        }

        every {
            mockPurchaseHandler.purchase(appUserID, storeProduct, null, captureLambda(), any())
        } answers {
            lambda<(Receipt, UserData) -> Unit>().captured.invoke(dummyReceipt, dummyUserData)
        }

        mockGetAmazonReceiptData(dummyReceipt, dummyUserData, expectedTermSku)

        underTest.makePurchaseAsync(
            mockk(),
            appUserID,
            storeProduct = storeProduct,
            replaceSkuInfo = null,
            presentedOfferingIdentifier = null
        )

        assertThat(receivedPurchases).isNotNull
        assertThat(receivedPurchases!!.size).isOne
        checkPurchaseIsCorrect(
            receivedPurchases!![0],
            expectedTermSku,
            dummyUserData,
            PurchaseState.PURCHASED
        )
    }

    @Test
    fun `Sku is used when purchasing consumables`() {
        val appUserID = "appUserID"
        val sku = "sku"
        val storeProduct = dummyAmazonProduct(
            sku = sku,
            productType = ProductType.CONSUMABLE
        ).toStoreProduct("US")
        val dummyReceipt = dummyReceipt(
            sku = sku,
            productType = ProductType.CONSUMABLE
        )
        val dummyUserData = dummyUserData()

        every {
            mockPurchasingServiceProvider.registerListener(mockContext, any())
        } just Runs

        var receivedPurchases: List<StoreTransaction>? = null
        underTest.purchasesUpdatedListener = object : BillingAbstract.PurchasesUpdatedListener {
            override fun onPurchasesUpdated(purchases: List<StoreTransaction>) {
                receivedPurchases = purchases
            }

            override fun onPurchasesFailedToUpdate(purchasesError: PurchasesError) {
                fail<String>("should be success")
            }
        }

        every {
            mockPurchaseHandler.purchase(appUserID, storeProduct, null, captureLambda(), any())
        } answers {
            lambda<(Receipt, UserData) -> Unit>().captured.invoke(dummyReceipt, dummyUserData)
        }

        underTest.makePurchaseAsync(
            mockk(),
            appUserID,
            storeProduct = storeProduct,
            replaceSkuInfo = null,
            presentedOfferingIdentifier = null
        )

        assertThat(receivedPurchases).isNotNull
        assertThat(receivedPurchases!!.size).isOne
        checkPurchaseIsCorrect(
            receivedPurchases!![0],
            sku,
            dummyUserData,
            PurchaseState.PURCHASED
        )
    }

    @Test
    fun `if observerMode, registerListener not called`() {
        observerModeSetup()
        every {
            mockPurchasingServiceProvider.registerListener(mockContext, any())
        } just Runs

        underTest.startConnection()
        verify(exactly = 0) {
            mockPurchasingServiceProvider.registerListener(any(), any())
        }
    }

    @Test
    fun `if not observerMode, registerListener called`() {
        every {
            mockPurchasingServiceProvider.registerListener(mockContext, any())
        } just Runs

        underTest.startConnection()
        verify(exactly = 1) {
            mockPurchasingServiceProvider.registerListener(any(), any())
        }
    }

    private fun observerModeSetup() {
        underTest = AmazonBilling(
            applicationContext = mockContext,
            amazonBackend = mockAmazonBackend,
            cache = mockCache,
            observerMode = true,
            mainHandler = handler,
            purchasingServiceProvider = mockPurchasingServiceProvider,
            productDataHandler = mockProductDataHandler,
            purchaseHandler = mockPurchaseHandler,
            purchaseUpdatesHandler = mockPurchaseUpdatesHandler,
            userDataHandler = mockUserDataHandler
        )
    }

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
}
