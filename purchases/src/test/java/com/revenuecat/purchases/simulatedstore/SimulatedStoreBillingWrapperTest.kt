package com.revenuecat.purchases.simulatedstore

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
public class SimulatedStoreBillingWrapperTest {

    private lateinit var deviceCache: DeviceCache
    private lateinit var mainHandler: Handler
    private lateinit var purchasesStateProvider: PurchasesStateProvider
    private lateinit var urlConnectionFactory: UrlConnectionFactory
    private lateinit var backend: Backend
    private lateinit var purchaseDialogHelper: AlertDialogHelper
    private lateinit var testStoreBilling: SimulatedStoreBillingWrapper
    private lateinit var stateListener: BillingAbstract.StateListener
    private lateinit var purchasesUpdatedListener: BillingAbstract.PurchasesUpdatedListener

    @Before
    public fun setup() {
        deviceCache = mockk()
        mainHandler = mockk()
        purchasesStateProvider = mockk()
        urlConnectionFactory = mockk()
        backend = mockk()
        purchaseDialogHelper = mockk()
        stateListener = mockk()
        
        // Create actual listener object for testing purchase flows
        purchasesUpdatedListener = this@SimulatedStoreBillingWrapperTest.TestPurchasesListener()
        
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
        
        testStoreBilling = SimulatedStoreBillingWrapper(
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
    fun `getStorefront returns US`() {
        var result: String? = null
        
        testStoreBilling.getStorefront(
            onSuccess = { result = it },
            onError = { }
        )
        
        assertThat(result).isEqualTo("US")
    }

    @Test
    fun `makePurchaseAsync with dialog simulated error calls onPurchasesFailedToUpdate with simulated error`() {
        // Given
        val activity = mockk<Activity>()
        val productId = "test_product_123"
        val presentedOfferingContext = mockk<PresentedOfferingContext>()

        // Mock product response from backend
        val productResponse = createMockProductResponse(productId)
        val product = SimulatedStoreProductConverter.convertToStoreProduct(productResponse)
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
                any(), any(), any(), any(), any(), any(), any(), onNegativeButtonClicked = captureLambda(), any(),
            )
        } answers {
            lambda<() -> Unit>().captured.invoke()
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
        assertThat(listenerImpl.lastError?.code).isEqualTo(PurchasesErrorCode.TestStoreSimulatedPurchaseError)
        assertThat(listenerImpl.lastError?.message).isEqualTo("Purchase failure simulated successfully in Test Store.")
        assertThat(listenerImpl.lastError?.underlyingErrorMessage).isEqualTo("Simulated error successfully.")
        assertThat(listenerImpl.lastPurchases).isNull()
    }

    @Test
    fun `makePurchaseAsync with dialog cancellation calls onPurchasesFailedToUpdate with cancelled error`() {
        // Given
        val activity = mockk<Activity>()
        val productId = "test_product_123"
        val presentedOfferingContext = mockk<PresentedOfferingContext>()
        
        // Mock product response from backend
        val productResponse = createMockProductResponse(productId)
        val product = SimulatedStoreProductConverter.convertToStoreProduct(productResponse)
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
            val onNeutralClicked = lastArg<() -> Unit>()
            onNeutralClicked()
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
        val product = SimulatedStoreProductConverter.convertToStoreProduct(productResponse)
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
        val listenerImpl = purchasesUpdatedListener as SimulatedStoreBillingWrapperTest.TestPurchasesListener
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
    fun `queryAllPurchases returns empty list`() {
        var cachedPurchases: List<StoreTransaction>? = null
        testStoreBilling.queryAllPurchases(
            appUserID = "test_user",
            onReceivePurchaseHistory = { cachedPurchases = it },
            onReceivePurchaseHistoryError = { fail("Expected success") }
        )
        
        assertThat(cachedPurchases).isNotNull()
        assertThat(cachedPurchases).hasSize(0)
    }

    @Test
    fun `queryPurchases returns empty map`() {
        var cachedPurchases: Map<String, StoreTransaction>? = null
        testStoreBilling.queryPurchases(
            appUserID = "test_user",
            onSuccess = { cachedPurchases = it },
            onError = { fail("Should succeed") }
        )
        
        assertThat(cachedPurchases).isNotNull()
        assertThat(cachedPurchases).hasSize(0)
    }

    @Test
    fun `findPurchaseInPurchaseHistory returns error`() {
        var error: PurchasesError? = null
        testStoreBilling.findPurchaseInPurchaseHistory(
            appUserID = "test_user",
            productType = ProductType.SUBS,
            productId = "test-product-id",
            onCompletion = { fail("Should error") },
            onError = { error = it }
        )
        
        assertThat(error).isNotNull()
        assertThat(error?.code).isEqualTo(PurchasesErrorCode.PurchaseNotAllowedError)
        assertThat(error?.underlyingErrorMessage).isEqualTo("No active purchase found for product: test-product-id")
    }

    @Test
    fun `findPurchaseInPurchaseHistory returns error when purchase not found`() {
        // Given - no purchases in cache
        val nonExistentProductId = "non_existent_product"
        
        // When - try to find non-existent purchase
        var foundPurchase: StoreTransaction? = null
        var errorFound: PurchasesError? = null
        
        testStoreBilling.findPurchaseInPurchaseHistory(
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
