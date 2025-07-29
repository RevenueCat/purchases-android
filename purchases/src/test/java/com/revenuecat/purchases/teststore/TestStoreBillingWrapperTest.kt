package com.revenuecat.purchases.teststore

import android.app.Activity
import android.os.Handler
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesStateProvider
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.networking.WebBillingPhase
import com.revenuecat.purchases.common.networking.WebBillingPrice
import com.revenuecat.purchases.common.networking.WebBillingProductResponse
import com.revenuecat.purchases.common.networking.WebBillingProductsResponse
import com.revenuecat.purchases.common.networking.WebBillingPurchaseOption
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.AlertDialogHelper
import com.revenuecat.purchases.utils.UrlConnectionFactory
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class TestStoreBillingWrapperTest {

    private lateinit var deviceCache: DeviceCache
    private lateinit var mainHandler: Handler
    private lateinit var purchasesStateProvider: PurchasesStateProvider
    private lateinit var urlConnectionFactory: UrlConnectionFactory
    private lateinit var backend: Backend
    private lateinit var purchaseDialogHelper: AlertDialogHelper
    private lateinit var testStoreBilling: TestStoreBillingWrapper
    private lateinit var stateListener: BillingAbstract.StateListener
    private lateinit var purchasesUpdatedListener: BillingAbstract.PurchasesUpdatedListener

    @Before
    fun setup() {
        deviceCache = mockk()
        mainHandler = mockk()
        purchasesStateProvider = mockk()
        urlConnectionFactory = mockk()
        backend = mockk()
        purchaseDialogHelper = mockk()
        stateListener = mockk()
        
        // Create actual listener object for testing purchase flows
        purchasesUpdatedListener = TestPurchasesListener()
        
        every { mainHandler.postDelayed(any(), any()) } answers {
            val runnable = firstArg<Runnable>()
            runnable.run()
            true
        }
        
        every { mainHandler.post(any()) } answers {
            val runnable = firstArg<Runnable>()
            runnable.run()
            true
        }
        
        testStoreBilling = TestStoreBillingWrapper(
            deviceCache = deviceCache,
            mainHandler = mainHandler,
            purchasesStateProvider = purchasesStateProvider,
            backend = backend,
            dialogHelper = purchaseDialogHelper
        )
        
        testStoreBilling.stateListener = stateListener
        testStoreBilling.purchasesUpdatedListener = purchasesUpdatedListener
    }

    @Test
    fun `startConnection sets connected to true and notifies listener`() {
        every { stateListener.onConnected() } just Runs
        
        assertThat(testStoreBilling.isConnected()).isFalse()
        
        testStoreBilling.startConnection()
        
        assertThat(testStoreBilling.isConnected()).isTrue()
        verify { stateListener.onConnected() }
    }

    @Test
    fun `startConnectionOnMainThread posts delayed and starts connection`() {
        every { stateListener.onConnected() } just Runs
        
        testStoreBilling.startConnectionOnMainThread(100)
        
        verify { mainHandler.postDelayed(any(), 100) }
        verify { stateListener.onConnected() }
    }

    @Test
    fun `close sets connected to false`() {
        every { stateListener.onConnected() } just Runs
        
        testStoreBilling.startConnection()
        assertThat(testStoreBilling.isConnected()).isTrue()
        
        testStoreBilling.close()
        assertThat(testStoreBilling.isConnected()).isFalse()
    }

    @Test
    fun `queryAllPurchases returns empty list`() {
        var result: List<*>? = null
        
        testStoreBilling.queryAllPurchases(
            appUserID = "test_user",
            onReceivePurchaseHistory = { result = it },
            onReceivePurchaseHistoryError = { }
        )
        
        assertThat(result).isEmpty()
    }

    @Test
    fun `queryPurchases returns empty map`() {
        var result: Map<*, *>? = null
        
        testStoreBilling.queryPurchases(
            appUserID = "test_user",
            onSuccess = { result = it },
            onError = { }
        )
        
        assertThat(result).isEmpty()
    }

    @Test
    fun `getStorefront returns US`() {
        var result: String? = null
        
        testStoreBilling.getStorefront(
            onSuccess = { result = it },
            onError = { }
        )
        
        assertThat(result).isEqualTo("US")
    }

    @Test
    fun `makePurchaseAsync with dialog cancellation calls onPurchasesFailedToUpdate with cancelled error`() {
        // Given
        val activity = mockk<Activity>()
        val productId = "test_product_123"
        val presentedOfferingContext = mockk<PresentedOfferingContext>()
        
        // Mock product response from backend
        val productResponse = createMockProductResponse(productId)
        val product = TestStoreProductConverter.convertToStoreProduct(productResponse)
        val purchasingData = product.purchasingData
        val billingResponse = WebBillingProductsResponse(listOf(productResponse))
        
        every { deviceCache.getCachedAppUserID() } returns "test_user"
        every { backend.getWebBillingProducts(any(), any(), any(), any()) } answers {
            val onSuccess = thirdArg<(WebBillingProductsResponse) -> Unit>()
            onSuccess(billingResponse)
        }
        
        // Mock dialog helper to simulate cancellation
        every { 
            purchaseDialogHelper.showDialog(
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
            ) 
        } answers {
            val onNegativeClicked = lastArg<() -> Unit>()
            onNegativeClicked()
        }
        
        // When
        testStoreBilling.makePurchaseAsync(
            activity = activity,
            appUserID = "test_user",
            purchasingData = purchasingData,
            replaceProductInfo = null,
            presentedOfferingContext = presentedOfferingContext,
            isPersonalizedPrice = null
        )
        
        // Then
        val listenerImpl = purchasesUpdatedListener as TestPurchasesListener
        assertThat(listenerImpl.lastError).isNotNull()
        assertThat(listenerImpl.lastError?.code).isEqualTo(PurchasesErrorCode.PurchaseCancelledError)
        assertThat(listenerImpl.lastError?.underlyingErrorMessage).isEqualTo("Purchase cancelled by user")
        assertThat(listenerImpl.lastPurchases).isNull()
    }

    @Test
    fun `makePurchaseAsync with successful purchase calls onPurchasesUpdated with transaction`() {
        // Given
        val activity = mockk<Activity>()
        val productId = "test_product_456"
        val presentedOfferingContext = mockk<PresentedOfferingContext>()
        
        // Mock product response from backend
        val productResponse = createMockProductResponse(productId)
        val product = TestStoreProductConverter.convertToStoreProduct(productResponse)
        val purchasingData = product.purchasingData
        val billingResponse = WebBillingProductsResponse(listOf(productResponse))
        
        every { deviceCache.getCachedAppUserID() } returns "test_user"
        every { backend.getWebBillingProducts(any(), any(), any(), any()) } answers {
            val onSuccess = thirdArg<(WebBillingProductsResponse) -> Unit>()
            onSuccess(billingResponse)
        }
        
        // Mock dialog helper to simulate successful purchase
        every { 
            purchaseDialogHelper.showDialog(
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
            ) 
        } answers {
            val onPositiveClicked = arg<() -> Unit>(6)
            onPositiveClicked()
        }
        
        // When
        testStoreBilling.makePurchaseAsync(
            activity = activity,
            appUserID = "test_user",
            purchasingData = purchasingData,
            replaceProductInfo = null,
            presentedOfferingContext = presentedOfferingContext,
            isPersonalizedPrice = null
        )
        
        // Then
        val listenerImpl = purchasesUpdatedListener as TestPurchasesListener
        assertThat(listenerImpl.lastPurchases).isNotNull()
        assertThat(listenerImpl.lastPurchases).hasSize(1)
        
        val transaction = listenerImpl.lastPurchases?.first()
        assertThat(transaction?.productIds).containsExactly(productId)
        assertThat(transaction?.type).isEqualTo(ProductType.SUBS)
        assertThat(transaction?.purchaseToken).isNotNull()
        assertThat(listenerImpl.lastError).isNull()
    }

    private fun createMockProductResponse(productId: String): WebBillingProductResponse {
        return WebBillingProductResponse(
            identifier = productId,
            productType = "subs",
            title = "Test Product",
            description = "Test product description",
            defaultPurchaseOptionId = "option1",
            purchaseOptions = mapOf(
                "option1" to WebBillingPurchaseOption(
                    basePrice = null,
                    base = WebBillingPhase(
                        price = WebBillingPrice(
                            amountMicros = 999000,
                            currency = "USD"
                        ),
                        periodDuration = "P1M",
                    ),
                    trial = null,
                    introPrice = null
                )
            )
        )
    }

    @Test
    fun `queryAllPurchases returns cached purchases after successful purchase`() {
        // Given
        val activity = mockk<Activity>()
        val productId = "test_product_cache"
        
        // Mock product response from backend
        val productResponse = createMockProductResponse(productId)
        val product = TestStoreProductConverter.convertToStoreProduct(productResponse)
        val purchasingData = product.purchasingData
        val billingResponse = WebBillingProductsResponse(listOf(productResponse))
        
        every { deviceCache.getCachedAppUserID() } returns "test_user"
        every { backend.getWebBillingProducts(any(), any(), any(), any()) } answers {
            val onSuccess = thirdArg<(WebBillingProductsResponse) -> Unit>()
            onSuccess(billingResponse)
        }
        
        // Mock dialog helper to simulate successful purchase
        every { 
            purchaseDialogHelper.showDialog(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            val onPositiveClicked = arg<() -> Unit>(6)
            onPositiveClicked()
        }
        
        // When - make purchase
        testStoreBilling.makePurchaseAsync(
            activity = activity,
            appUserID = "test_user",
            purchasingData = purchasingData,
            replaceProductInfo = null,
            presentedOfferingContext = null,
            isPersonalizedPrice = null
        )
        
        // Then - verify purchase was cached
        var cachedPurchases: List<StoreTransaction>? = null
        testStoreBilling.queryAllPurchases(
            appUserID = "test_user",
            onReceivePurchaseHistory = { cachedPurchases = it },
            onReceivePurchaseHistoryError = { }
        )
        
        assertThat(cachedPurchases).isNotNull()
        assertThat(cachedPurchases).hasSize(1)
        assertThat(cachedPurchases?.first()?.productIds).containsExactly(productId)
    }

    @Test
    fun `queryPurchases returns cached purchases as map after successful purchase`() {
        // Given
        val activity = mockk<Activity>()
        val productId = "test_product_map"
        
        // Mock product response from backend
        val productResponse = createMockProductResponse(productId)
        val product = TestStoreProductConverter.convertToStoreProduct(productResponse)
        val purchasingData = product.purchasingData
        val billingResponse = WebBillingProductsResponse(listOf(productResponse))
        
        every { deviceCache.getCachedAppUserID() } returns "test_user"
        every { backend.getWebBillingProducts(any(), any(), any(), any()) } answers {
            val onSuccess = thirdArg<(WebBillingProductsResponse) -> Unit>()
            onSuccess(billingResponse)
        }
        
        // Mock dialog helper to simulate successful purchase
        every { 
            purchaseDialogHelper.showDialog(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            val onPositiveClicked = arg<() -> Unit>(6)
            onPositiveClicked()
        }
        
        // When - make purchase
        testStoreBilling.makePurchaseAsync(
            activity = activity,
            appUserID = "test_user",
            purchasingData = purchasingData,
            replaceProductInfo = null,
            presentedOfferingContext = null,
            isPersonalizedPrice = null
        )
        
        // Then - verify purchase was cached as map
        var cachedPurchases: Map<String, StoreTransaction>? = null
        testStoreBilling.queryPurchases(
            appUserID = "test_user",
            onSuccess = { cachedPurchases = it },
            onError = { }
        )
        
        assertThat(cachedPurchases).isNotNull()
        assertThat(cachedPurchases).hasSize(1)
        val purchase = cachedPurchases?.values?.first()
        assertThat(purchase?.productIds).containsExactly(productId)
        assertThat(purchase?.type).isEqualTo(ProductType.SUBS)
    }

    @Test
    fun `findPurchaseInActivePurchases finds cached purchase by product ID and type`() {
        // Given
        val activity = mockk<Activity>()
        val productId = "test_product_find"

        // Mock product response from backend
        val productResponse = createMockProductResponse(productId)
        val product = TestStoreProductConverter.convertToStoreProduct(productResponse)
        val purchasingData = product.purchasingData
        val billingResponse = WebBillingProductsResponse(listOf(productResponse))
        
        every { deviceCache.getCachedAppUserID() } returns "test_user"
        every { backend.getWebBillingProducts(any(), any(), any(), any()) } answers {
            val onSuccess = thirdArg<(WebBillingProductsResponse) -> Unit>()
            onSuccess(billingResponse)
        }
        
        // Mock dialog helper to simulate successful purchase
        every { 
            purchaseDialogHelper.showDialog(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            val onPositiveClicked = arg<() -> Unit>(6)
            onPositiveClicked()
        }
        
        // When - make purchase first
        testStoreBilling.makePurchaseAsync(
            activity = activity,
            appUserID = "test_user",
            purchasingData = purchasingData,
            replaceProductInfo = null,
            presentedOfferingContext = null,
            isPersonalizedPrice = null
        )
        
        // Then - find the cached purchase
        var foundPurchase: StoreTransaction? = null

        testStoreBilling.findPurchaseInActivePurchases(
            appUserID = "test_user",
            productType = ProductType.SUBS,
            productId = productId,
            onCompletion = { foundPurchase = it },
            onError = { fail("Should succeed") }
        )
        
        assertThat(foundPurchase).isNotNull()
        assertThat(foundPurchase?.productIds).containsExactly(productId)
        assertThat(foundPurchase?.type).isEqualTo(ProductType.SUBS)
    }

    @Test
    fun `findPurchaseInActivePurchases returns error when purchase not found`() {
        // Given - no purchases in cache
        val nonExistentProductId = "non_existent_product"
        
        // When - try to find non-existent purchase
        var foundPurchase: StoreTransaction? = null
        var errorFound: PurchasesError? = null
        
        testStoreBilling.findPurchaseInActivePurchases(
            appUserID = "test_user",
            productType = ProductType.SUBS,
            productId = nonExistentProductId,
            onCompletion = { foundPurchase = it },
            onError = { errorFound = it }
        )
        
        // Then - should return error
        assertThat(foundPurchase).isNull()
        assertThat(errorFound).isNotNull()
        assertThat(errorFound?.code).isEqualTo(PurchasesErrorCode.PurchaseNotAllowedError)
        assertThat(errorFound?.underlyingErrorMessage).contains(nonExistentProductId)
    }

    @Test
    fun `cache persists multiple purchases with different tokens`() {
        // Given
        val activity = mockk<Activity>()
        val productId1 = "test_product_multi_1"
        val productId2 = "test_product_multi_2"
        
        // Mock product responses from backend
        val productResponse1 = createMockProductResponse(productId1)
        val product1 = TestStoreProductConverter.convertToStoreProduct(productResponse1)
        val purchasingData1 = product1.purchasingData
        val productResponse2 = createMockProductResponse(productId2)
        val product2 = TestStoreProductConverter.convertToStoreProduct(productResponse2)
        val purchasingData2 = product2.purchasingData
        
        every { deviceCache.getCachedAppUserID() } returns "test_user"
        
        // Mock backend responses for each product
        every { backend.getWebBillingProducts(any(), setOf(productId1), any(), any()) } answers {
            val onSuccess = thirdArg<(WebBillingProductsResponse) -> Unit>()
            onSuccess(WebBillingProductsResponse(listOf(productResponse1)))
        }
        
        every { backend.getWebBillingProducts(any(), setOf(productId2), any(), any()) } answers {
            val onSuccess = thirdArg<(WebBillingProductsResponse) -> Unit>()
            onSuccess(WebBillingProductsResponse(listOf(productResponse2)))
        }
        
        // Mock dialog helper to always accept
        every { 
            purchaseDialogHelper.showDialog(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            val onPositiveClicked = arg<() -> Unit>(6)
            onPositiveClicked()
        }
        
        // When - make two purchases
        testStoreBilling.makePurchaseAsync(
            activity = activity,
            appUserID = "test_user",
            purchasingData = purchasingData1,
            replaceProductInfo = null,
            presentedOfferingContext = null,
            isPersonalizedPrice = null
        )
        
        testStoreBilling.makePurchaseAsync(
            activity = activity,
            appUserID = "test_user",
            purchasingData = purchasingData2,
            replaceProductInfo = null,
            presentedOfferingContext = null,
            isPersonalizedPrice = null
        )
        
        // Then - verify both purchases are cached
        var cachedPurchases: List<StoreTransaction>? = null
        testStoreBilling.queryAllPurchases(
            appUserID = "test_user",
            onReceivePurchaseHistory = { cachedPurchases = it },
            onReceivePurchaseHistoryError = { }
        )
        
        assertThat(cachedPurchases).isNotNull()
        assertThat(cachedPurchases).hasSize(2)
        
        val productIds = cachedPurchases?.flatMap { it.productIds } ?: emptyList()
        assertThat(productIds).containsExactlyInAnyOrder(productId1, productId2)
    }

    private inner class TestPurchasesListener : BillingAbstract.PurchasesUpdatedListener {
        var lastPurchases: List<StoreTransaction>? = null
        var lastError: PurchasesError? = null
        
        override fun onPurchasesUpdated(purchases: List<StoreTransaction>) {
            lastPurchases = purchases
        }
        
        override fun onPurchasesFailedToUpdate(purchasesError: PurchasesError) {
            lastError = purchasesError
        }
    }
}
