package com.revenuecat.purchases.simulatedstore

import android.app.Activity
import android.os.Handler
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.InternalRevenueCatAPI
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

@OptIn(InternalRevenueCatAPI::class)
@Suppress("TooManyFunctions")
internal class SimulatedStoreBillingWrapper(
    private val deviceCache: DeviceCache,
    private val mainHandler: Handler,
    purchasesStateProvider: PurchasesStateProvider,
    private val backend: Backend,
    private val dialogHelper: AlertDialogHelper = DefaultAlertDialogHelper(),
) : BillingAbstract(purchasesStateProvider) {

    @Volatile
    private var connected = false

    override fun startConnectionOnMainThread(delayMilliseconds: Long) {
        mainHandler.postDelayed({
            startConnection()
        }, delayMilliseconds)
    }

    override fun startConnection() {
        debugLog { "SimulatedStoreBillingAbstract: Starting connection" }
        connected = true
        stateListener?.onConnected()
    }

    override fun endConnection() {
        debugLog { "SimulatedStoreBillingAbstract: Ending connection" }
        connected = false
    }

    override fun queryAllPurchases(
        appUserID: String,
        onReceivePurchaseHistory: (List<StoreTransaction>) -> Unit,
        onReceivePurchaseHistoryError: PurchasesErrorCallback,
    ) {
        debugLog { "SimulatedStoreBillingAbstract: queryAllPurchases - returning empty list" }
        onReceivePurchaseHistory(emptyList())
    }

    override fun queryProductDetailsAsync(
        productType: ProductType,
        productIds: Set<String>,
        onReceive: StoreProductsCallback,
        onError: PurchasesErrorCallback,
    ) {
        debugLog { "SimulatedStoreBillingAbstract: queryProductDetailsAsync for products: $productIds" }

        backend.getWebBillingProducts(
            appUserID = deviceCache.getCachedAppUserID() ?: "",
            productIds = productIds,
            onSuccess = { response ->
                try {
                    val storeProducts = response.productDetails.map { productResponse ->
                        SimulatedStoreProductConverter.convertToStoreProduct(productResponse)
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
        debugLog { "SimulatedStoreBillingAbstract: consumeAndSave - no-op for test store" }
    }

    override fun findPurchaseInPurchaseHistory(
        appUserID: String,
        productType: ProductType,
        productId: String,
        onCompletion: (StoreTransaction) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        debugLog {
            "SimulatedStoreBillingAbstract: findPurchaseInPurchaseHistory for product: $productId will always fail"
        }

        onError(
            PurchasesError(
                PurchasesErrorCode.PurchaseNotAllowedError,
                "No active purchase found for product: $productId",
            ),
        )
    }

    override fun makePurchaseAsync(
        activity: Activity,
        appUserID: String,
        purchasingData: PurchasingData,
        replaceProductInfo: ReplaceProductInfo?,
        presentedOfferingContext: PresentedOfferingContext?,
        isPersonalizedPrice: Boolean?,
    ) {
        debugLog { "SimulatedStoreBillingAbstract: makePurchaseAsync for product: ${purchasingData.productId}" }
        val simulatedStorePurchasingData = purchasingData as? SimulatedStorePurchasingData
            ?: throw PurchasesException(
                PurchasesError(
                    PurchasesErrorCode.ProductNotAvailableForPurchaseError,
                    "Purchasing data is not a valid SimulatedStorePurchasingData: ${purchasingData.productId}",
                ),
            )
        val storeProduct = simulatedStorePurchasingData.storeProduct
        showPurchaseDialog(activity, storeProduct, presentedOfferingContext)
    }

    override fun isConnected(): Boolean = connected

    override fun queryPurchases(
        appUserID: String,
        onSuccess: (Map<String, StoreTransaction>) -> Unit,
        onError: (PurchasesError) -> Unit,
    ) {
        debugLog { "SimulatedStoreBillingAbstract: queryPurchases - returning empty map" }
        onSuccess(emptyMap())
    }

    override fun showInAppMessagesIfNeeded(
        activity: Activity,
        inAppMessageTypes: List<InAppMessageType>,
        subscriptionStatusChange: () -> Unit,
    ) {
        debugLog { "SimulatedStoreBillingAbstract: showInAppMessagesIfNeeded - no-op for test store" }
    }

    override fun getStorefront(
        onSuccess: (String) -> Unit,
        onError: PurchasesErrorCallback,
    ) {
        debugLog { "SimulatedStoreBillingAbstract: getStorefront - returning US by default" }
        onSuccess("US")
    }

    private fun showPurchaseDialog(
        activity: Activity,
        product: StoreProduct,
        presentedOfferingContext: PresentedOfferingContext?,
    ) {
        val message = buildString {
            append(
                "This is a test purchase and should only be used during development. In production, " +
                    "use a Google/Amazon API key from RevenueCat.\n\n",
            )
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
            positiveButtonText = "Test valid Purchase",
            negativeButtonText = "Test failed Purchase",
            neutralButtonText = "Cancel",
            onPositiveButtonClicked = {
                debugLog { "[Test store] Performing test purchase. This purchase won't appear in production." }
                completePurchase(product, presentedOfferingContext)
            },
            onNegativeButtonClicked = {
                debugLog { "[Test store] Purchase failure simulated successfully in Test Store." }
                purchasesUpdatedListener?.onPurchasesFailedToUpdate(
                    PurchasesError(
                        PurchasesErrorCode.TestStoreSimulatedPurchaseError,
                        "Simulated error successfully.",
                    ),
                )
            },
            onNeutralButtonClicked = {
                purchasesUpdatedListener?.onPurchasesFailedToUpdate(
                    PurchasesError(
                        PurchasesErrorCode.PurchaseCancelledError,
                        "Purchase cancelled by user",
                    ),
                )
            },
        )
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    private fun completePurchase(
        product: StoreProduct,
        presentedOfferingContext: PresentedOfferingContext?,
    ) {
        val purchaseTime = Date().time

        val purchaseToken = "test_${purchaseTime}_${UUID.randomUUID()}"

        val storeTransaction = StoreTransaction(
            orderId = purchaseToken,
            productIds = listOf(product.id),
            type = product.type,
            purchaseTime = purchaseTime,
            purchaseToken = purchaseToken,
            purchaseState = PurchaseState.PURCHASED,
            isAutoRenewing = product.type == ProductType.SUBS,
            signature = null,
            originalJson = JSONObject().apply {
                put("orderId", purchaseToken)
                put("productId", product.id)
                put("purchaseTime", purchaseTime)
                put("purchaseToken", purchaseToken)
                put("purchaseState", PurchaseState.PURCHASED.ordinal)
            },
            presentedOfferingContext = presentedOfferingContext,
            storeUserID = null,
            purchaseType = PurchaseType.GOOGLE_PURCHASE, // WIP: Specify a new purchase type for the simulated store
            marketplace = null,
            subscriptionOptionId = product.defaultOption?.id,
            subscriptionOptionIdForProductIDs = product.defaultOption?.id?.let {
                mapOf(product.id to it)
            } ?: emptyMap(),
            replacementMode = null,
        )

        purchasesUpdatedListener?.onPurchasesUpdated(listOf(storeTransaction))
    }
}
