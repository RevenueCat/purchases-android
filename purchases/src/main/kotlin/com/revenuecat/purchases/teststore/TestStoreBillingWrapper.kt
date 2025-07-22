package com.revenuecat.purchases.teststore

import android.app.Activity
import android.os.Handler
import com.revenuecat.purchases.PostReceiptInitiationSource
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCallback
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesStateProvider
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.ReplaceProductInfo
import com.revenuecat.purchases.common.StoreProductsCallback
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.debugLog
import com.revenuecat.purchases.models.InAppMessageType
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.utils.AlertDialogHelper
import com.revenuecat.purchases.utils.DefaultAlertDialogHelper
import org.json.JSONObject
import java.util.Date
import java.util.UUID

@Suppress("TooManyFunctions")
internal class TestStoreBillingWrapper(
    private val deviceCache: DeviceCache,
    private val mainHandler: Handler,
    purchasesStateProvider: PurchasesStateProvider,
    private val backend: Backend,
    private val dialogHelper: AlertDialogHelper = DefaultAlertDialogHelper(),
) : BillingAbstract(purchasesStateProvider) {

    @Volatile
    private var connected = false

    private val purchaseCache = mutableMapOf<String, StoreTransaction>()

    override fun startConnectionOnMainThread(delayMilliseconds: Long) {
        mainHandler.postDelayed({
            startConnection()
        }, delayMilliseconds)
    }

    override fun startConnection() {
        debugLog { "TestStoreBillingAbstract: Starting connection" }
        connected = true
        stateListener?.onConnected()
    }

    override fun endConnection() {
        debugLog { "TestStoreBillingAbstract: Ending connection" }
        connected = false
    }

    override fun queryAllPurchases(
        appUserID: String,
        onReceivePurchaseHistory: (List<StoreTransaction>) -> Unit,
        onReceivePurchaseHistoryError: PurchasesErrorCallback,
    ) {
        debugLog { "TestStoreBillingAbstract: queryAllPurchases - returning cached purchases" }
        onReceivePurchaseHistory(purchaseCache.values.toList())
    }

    override fun queryProductDetailsAsync(
        productType: ProductType,
        productIds: Set<String>,
        onReceive: StoreProductsCallback,
        onError: PurchasesErrorCallback,
    ) {
        debugLog { "TestStoreBillingAbstract: queryProductDetailsAsync for products: $productIds" }

        backend.getRCBillingProducts(
            appUserID = deviceCache.getCachedAppUserID() ?: "",
            productIds = productIds,
            onSuccess = { response ->
                try {
                    val storeProducts = response.productDetails.map { productResponse ->
                        TestStoreProductConverter.convertToStoreProduct(productResponse)
                    }
                    onReceive(storeProducts)
                } catch (e: PurchasesException) {
                    onError(e.error)
                }
            },
            onError = onError,
        )
    }

    override fun consumeAndSave(
        finishTransactions: Boolean,
        purchase: StoreTransaction,
        shouldConsume: Boolean,
        initiationSource: PostReceiptInitiationSource,
    ) {
        debugLog { "TestStoreBillingAbstract: consumeAndSave - no-op for test store" }
    }

    override fun findPurchaseInActivePurchases(
        appUserID: String,
        productType: ProductType,
        productId: String,
        onCompletion: (StoreTransaction) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        debugLog { "TestStoreBillingAbstract: findPurchaseInActivePurchases for product: $productId" }

        val purchase = purchaseCache.values.find { transaction ->
            transaction.productIds.contains(productId) && transaction.type == productType
        }

        if (purchase != null) {
            onCompletion(purchase)
        } else {
            onError(
                PurchasesError(
                    PurchasesErrorCode.PurchaseNotAllowedError,
                    "No active purchase found for product: $productId",
                ),
            )
        }
    }

    override fun makePurchaseAsync(
        activity: Activity,
        appUserID: String,
        purchasingData: PurchasingData,
        replaceProductInfo: ReplaceProductInfo?,
        presentedOfferingContext: PresentedOfferingContext?,
        isPersonalizedPrice: Boolean?,
    ) {
        debugLog { "TestStoreBillingAbstract: makePurchaseAsync for product: ${purchasingData.productId}" }
        val testStorePurchasingData = purchasingData as? TestStorePurchasingData
            ?: throw PurchasesException(
                PurchasesError(
                    PurchasesErrorCode.ProductNotAvailableForPurchaseError,
                    "Purchasing data is not a valid TestStorePurchasingData: ${purchasingData.productId}",
                ),
            )
        val storeProduct = testStorePurchasingData.storeProduct
        showPurchaseDialog(activity, storeProduct, presentedOfferingContext)
    }

    override fun isConnected(): Boolean = connected

    override fun queryPurchases(
        appUserID: String,
        onSuccess: (Map<String, StoreTransaction>) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        debugLog { "TestStoreBillingAbstract: queryPurchases - returning cached purchases" }
        onSuccess(purchaseCache)
    }

    override fun showInAppMessagesIfNeeded(
        activity: Activity,
        inAppMessageTypes: List<InAppMessageType>,
        subscriptionStatusChange: () -> Unit,
    ) {
        debugLog { "TestStoreBillingAbstract: showInAppMessagesIfNeeded - no-op for test store" }
    }

    override fun getStorefront(
        onSuccess: (String) -> Unit,
        onError: PurchasesErrorCallback,
    ) {
        debugLog { "TestStoreBillingAbstract: getStorefront - returning US by default" }
        onSuccess("US")
    }

    private fun showPurchaseDialog(
        activity: Activity,
        product: StoreProduct,
        presentedOfferingContext: PresentedOfferingContext?,
    ) {
        val message = buildString {
            append("Product: ${product.id}\n")
            append("Price: ${product.price.formatted}\n")
            product.defaultOption?.let { option ->
                option.pricingPhases.forEach { phase ->
                    append("Phase: ${phase.price.formatted} for ${phase.billingPeriod.iso8601}\n")
                }
            }
        }

        dialogHelper.showDialog(
            activity = activity,
            title = "Test Store Purchase",
            message = message,
            positiveButtonText = "Purchase",
            negativeButtonText = "Cancel",
            onPositiveButtonClicked = {
                completePurchase(product, presentedOfferingContext)
            },
            onNegativeButtonClicked = {
                purchasesUpdatedListener?.onPurchasesFailedToUpdate(
                    PurchasesError(
                        PurchasesErrorCode.PurchaseCancelledError,
                        "Purchase cancelled by user",
                    ),
                )
            },
        )
    }

    private fun completePurchase(
        product: StoreProduct,
        presentedOfferingContext: PresentedOfferingContext?,
    ) {
        // TODO: Implement actual purchase logic by sending purchase details to server.
        val purchaseToken = UUID.randomUUID().toString()
        val purchaseTime = Date().time

        val storeTransaction = StoreTransaction(
            orderId = "test_order_${UUID.randomUUID()}",
            productIds = listOf(product.id),
            type = product.type,
            purchaseTime = purchaseTime,
            purchaseToken = purchaseToken,
            purchaseState = PurchaseState.PURCHASED,
            isAutoRenewing = product.type == ProductType.SUBS,
            signature = null,
            originalJson = JSONObject().apply {
                put("orderId", "test_order_${UUID.randomUUID()}")
                put("productId", product.id)
                put("purchaseTime", purchaseTime)
                put("purchaseToken", purchaseToken)
                put("purchaseState", PurchaseState.PURCHASED.ordinal)
                put("test_store", true)
            },
            presentedOfferingContext = presentedOfferingContext,
            storeUserID = null,
            purchaseType = PurchaseType.TEST_PURCHASE,
            marketplace = null,
            subscriptionOptionId = product.defaultOption?.id,
            replacementMode = null,
        )

        purchaseCache[purchaseToken] = storeTransaction

        purchasesUpdatedListener?.onPurchasesUpdated(listOf(storeTransaction))
    }
}
