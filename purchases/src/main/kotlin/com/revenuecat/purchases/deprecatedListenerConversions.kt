package com.revenuecat.purchases

import android.app.Activity
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.interfaces.MakePurchaseListener
import com.revenuecat.purchases.interfaces.ProductChangeListener

@Deprecated("Purchase replaced with StoreTransaction and PurchaserInfo with CustomerInfo")
private typealias DeprecatedPurchaseCompletedFunction = (purchase: Purchase, purchaserInfo: PurchaserInfo) -> Unit
@Deprecated("Purchase replaced with StoreTransaction and PurchaserInfo with CustomerInfo")
private typealias DeprecatedProductChangeCompletedFunction = (purchase: Purchase?, purchaserInfo: PurchaserInfo) -> Unit

@Deprecated("onCompleted Purchase changed with StoreTransaction")
internal fun deprecatedPurchaseCompletedListener(
    onSuccess: DeprecatedPurchaseCompletedFunction,
    onError: PurchaseErrorFunction
) = object : MakePurchaseListener {
    override fun onCompleted(purchase: Purchase, purchaserInfo: PurchaserInfo) {
        onSuccess(purchase, purchaserInfo)
    }

    override fun onError(error: PurchasesError, userCancelled: Boolean) {
        onError(error, userCancelled)
    }
}

@Deprecated("onCompleted Purchase changed with StoreTransaction")
internal fun deprecatedProductChangeCompletedListener(
    onSuccess: DeprecatedProductChangeCompletedFunction,
    onError: PurchaseErrorFunction
) = object : ProductChangeListener {
    override fun onCompleted(purchase: Purchase?, purchaserInfo: PurchaserInfo) {
        onSuccess(purchase, purchaserInfo)
    }

    override fun onError(error: PurchasesError, userCancelled: Boolean) {
        onError(error, userCancelled)
    }
}

/**
 * Get latest available purchaser info.
 * @param onSuccess Called when purchaser info is available and not stale. Called immediately if
 * purchaser info is cached.
 * @param onError Will be called after the call has completed with an error.
 */
@Suppress("unused")
@Deprecated(
    "Renamed to getCustomerInfoWith",
    replaceWith = ReplaceWith("Purchases.sharedInstance.getCustomerInfoWith(onError, onSuccess)")
)
fun Purchases.getPurchaserInfoWith(
    onError: ErrorFunction = ON_ERROR_STUB,
    onSuccess: (purchaserInfo: PurchaserInfo) -> Unit
) {
    getCustomerInfoWith(onError, onSuccess = ::PurchaserInfo)
}

/**
 * Purchase product.
 * @param [activity] Current activity
 * @param [skuDetails] The skuDetails of the product you wish to purchase
 * @param [onSuccess] Will be called after the purchase has completed
 * @param [onError] Will be called after the purchase has completed with error
 */
@Deprecated(
    message = "SkuDetails parameter replaced with StoreProduct. " +
        "The callback now returns a StoreProduct and a CustomerInfo.",
    replaceWith = ReplaceWith(
        "purchaseProductWith(activity, storeProduct, onError, onSuccess)"
    )
)
fun Purchases.purchaseProductWith(
    activity: Activity,
    skuDetails: SkuDetails,
    onError: PurchaseErrorFunction = ON_PURCHASE_ERROR_STUB,
    onSuccess: DeprecatedPurchaseCompletedFunction
) {
    purchaseProduct(activity, skuDetails, deprecatedPurchaseCompletedListener(onSuccess, onError))
}

/**
 * Make a purchase.
 * @param [activity] Current activity
 * @param [skuDetails] The skuDetails of the product you wish to purchase
 * @param [upgradeInfo] The upgradeInfo you wish to upgrade from, containing the oldSku and the optional prorationMode.
 * @param [onSuccess] Will be called after the purchase has completed
 * @param [onError] Will be called after the purchase has completed with error
 */
@Deprecated(
    message = "SkuDetails parameter replaced with StoreProduct. " +
        "The callback now returns a StoreProduct and a CustomerInfo.",
    replaceWith = ReplaceWith(
        "purchaseProductWith(activity, storeProduct, onError, onSuccess)"
    )
)
fun Purchases.purchaseProductWith(
    activity: Activity,
    skuDetails: SkuDetails,
    upgradeInfo: UpgradeInfo,
    onError: PurchaseErrorFunction = ON_PURCHASE_ERROR_STUB,
    onSuccess: DeprecatedProductChangeCompletedFunction
) {
    purchaseProduct(activity, skuDetails, upgradeInfo,
        deprecatedProductChangeCompletedListener(onSuccess, onError)
    )
}
