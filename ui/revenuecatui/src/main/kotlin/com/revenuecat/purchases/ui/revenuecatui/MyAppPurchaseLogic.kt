package com.revenuecat.purchases.ui.revenuecatui

import android.app.Activity
import com.android.billingclient.api.Purchase
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.models.PurchaseState
import com.revenuecat.purchases.models.PurchaseType
import com.revenuecat.purchases.models.StoreTransaction
import org.json.JSONObject

/**
 * Code for handling in-app purchases and restorations directly by the application rather than by RevenueCat.
 * These methods are called by a RevenueCat Paywall only when `Purchases.purchasesAreCompletedBy` is `MY_APP`.
 *
 * @property performPurchase Performs an in-app purchase.
 * @property performRestore Perform a purchase restore for the given customer.
 */
class MyAppPurchaseLogic(
    val performPurchase: suspend ((Activity, Package) -> MyAppPurchaseResult),
    val performRestore: suspend ((CustomerInfo) -> MyAppRestoreResult),
)

/**
 * Represents the result of a purchase attempt made by custom app-based code (not RevenueCat).
 */
sealed interface MyAppPurchaseResult {
    /**
     * Indicates a successful purchase.
     *
     * @property purchase The purchase details if available. This object is only used to call the
     * `PaywallListener` `onPurchaseCompleted` callback. If this callback is not needed, it does not need to be
     * provided, and the callback will not be called.
     */
    data class Success(val purchase: MyAppPurchase? = null) : MyAppPurchaseResult

    /**
     * Indicates the purchase was cancelled.
     */
    object Cancellation : MyAppPurchaseResult

    /**
     * Indicates an error occurred during the purchase attempt.
     *
     * @property error Details of the error that occurred.
     */
    data class Error(val error: PurchasesError) : MyAppPurchaseResult
}

/**
 * Represents the result of a restore purchases attempt.
 */
sealed interface MyAppRestoreResult {
    /**
     * Indicates a successful restore operation.
     */
    object Success : MyAppRestoreResult

    /**
     * Indicates an error occurred during the restore attempt.
     *
     * @property error Details of the error that occurred.
     */
    data class Error(val error: PurchasesError) : MyAppRestoreResult
}

/**
 * Represents a purchase made in the application.
 *
 * @property purchase The actual purchase object from the store.
 * @property productType The type of product being purchased. Defaults to [ProductType.UNKNOWN].
 * @property presentedOfferingContext Context of the offering presented to the user, if applicable.
 * @property subscriptionOptionId The subscription option ID from Google, if applicable.
 * @property replacementMode The replacement mode for Google subscriptions, if applicable.
 */
class MyAppPurchase(
    val purchase: Purchase,
    internal val productType: ProductType = ProductType.UNKNOWN,
    internal val presentedOfferingContext: PresentedOfferingContext? = null,
    internal val subscriptionOptionId: String? = null,
    internal val replacementMode: GoogleReplacementMode? = null,
) {
    internal fun Int.toRevenueCatPurchaseState(): PurchaseState {
        return when (this) {
            Purchase.PurchaseState.UNSPECIFIED_STATE -> PurchaseState.UNSPECIFIED_STATE
            Purchase.PurchaseState.PURCHASED -> PurchaseState.PURCHASED
            Purchase.PurchaseState.PENDING -> PurchaseState.PENDING
            else -> PurchaseState.UNSPECIFIED_STATE
        }
    }
}

/**
 * Converts the [MyAppPurchase] instance to a [StoreTransaction] object.
 *
 * @return A [StoreTransaction] representing the purchase.
 */
internal fun MyAppPurchase.toStoreTransaction(): StoreTransaction = StoreTransaction(
    orderId = purchase.orderId,
    productIds = purchase.products,
    type = productType,
    purchaseTime = purchase.purchaseTime,
    purchaseToken = purchase.purchaseToken,
    purchaseState = purchase.purchaseState.toRevenueCatPurchaseState(),
    isAutoRenewing = purchase.isAutoRenewing,
    signature = purchase.signature,
    originalJson = JSONObject(purchase.originalJson),
    presentedOfferingContext = presentedOfferingContext,
    storeUserID = null,
    purchaseType = PurchaseType.GOOGLE_PURCHASE,
    marketplace = null,
    subscriptionOptionId = subscriptionOptionId,
    replacementMode = replacementMode,
)
