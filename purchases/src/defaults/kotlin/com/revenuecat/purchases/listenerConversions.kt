package com.revenuecat.purchases

import android.app.Activity
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.SubscriptionOption

internal fun logInSuccessListener(
    onSuccess: (customerInfo: CustomerInfo, created: Boolean) -> Unit?,
    onError: (error: PurchasesError) -> Unit?,
) = object : LogInCallback {
    override fun onReceived(customerInfo: CustomerInfo, created: Boolean) {
        onSuccess.invoke(customerInfo, created)
    }

    override fun onError(error: PurchasesError) {
        onError.invoke(error)
    }
}

/**
 * Purchase product. If purchasing a subscription, it will choose the default [SubscriptionOption].
 * @param [activity] Current activity
 * @param [storeProduct] The storeProduct of the product you wish to purchase
 * @param [onSuccess] Will be called after the purchase has completed
 * @param [onError] Will be called if there was an error with the purchase
 */
@Deprecated(
    "Use purchase() and PurchaseParams.Builder instead",
    ReplaceWith("purchase()"),
)
fun Purchases.purchaseProductWith(
    activity: Activity,
    storeProduct: StoreProduct,
    onError: (error: PurchasesError, userCancelled: Boolean) -> Unit = ON_PURCHASE_ERROR_STUB,
    onSuccess: (purchase: StoreTransaction, customerInfo: CustomerInfo) -> Unit,
) {
    purchaseProduct(activity, storeProduct, purchaseCompletedCallback(onSuccess, onError))
}

/**
 * Make a purchase upgrading from a previous sku. If purchasing a subscription, it will choose the
 * default [SubscriptionOption].
 * @param [activity] Current activity
 * @param [storeProduct] The storeProduct of the product you wish to purchase
 * @param [upgradeInfo] The upgradeInfo you wish to upgrade from, containing the oldSku and the optional prorationMode.
 * Amazon Appstore doesn't support changing products so upgradeInfo is ignored for Amazon purchases.
 * @param [onSuccess] Will be called after the purchase has completed
 * @param [onError] Will be called if there was an error with the purchase
 */
@Deprecated(
    "Use purchaseWith and PurchaseParams.Builder instead",
    ReplaceWith("purchaseWith()"),
)
fun Purchases.purchaseProductWith(
    activity: Activity,
    storeProduct: StoreProduct,
    upgradeInfo: UpgradeInfo,
    onError: (error: PurchasesError, userCancelled: Boolean) -> Unit = ON_PURCHASE_ERROR_STUB,
    onSuccess: (purchase: StoreTransaction?, customerInfo: CustomerInfo) -> Unit,
) {
    purchaseProduct(activity, storeProduct, upgradeInfo, productChangeCompletedListener(onSuccess, onError))
}

/**
 * Make a purchase upgrading from a previous sku. If purchasing a subscription, it will choose the
 * default [SubscriptionOption].
 * @param [activity] Current activity
 * @param [packageToPurchase] The Package you wish to purchase
 * @param [upgradeInfo] The upgradeInfo you wish to upgrade from, containing the oldSku and the optional prorationMode.
 * Amazon Appstore doesn't support changing products so upgradeInfo is ignored for Amazon purchases.
 * @param [onSuccess] Will be called after the purchase has completed
 * @param [onError] Will be called if there was an error with the purchase
 */
@Deprecated(
    "Use purchaseWith and PurchaseParams.Builder instead",
    ReplaceWith("purchaseWith()"),
)
fun Purchases.purchasePackageWith(
    activity: Activity,
    packageToPurchase: Package,
    upgradeInfo: UpgradeInfo,
    onError: (error: PurchasesError, userCancelled: Boolean) -> Unit = ON_PURCHASE_ERROR_STUB,
    onSuccess: (purchase: StoreTransaction?, customerInfo: CustomerInfo) -> Unit,
) {
    purchasePackage(activity, packageToPurchase, upgradeInfo, productChangeCompletedListener(onSuccess, onError))
}

/**
 * Make a purchase. If purchasing a subscription, it will choose the default [SubscriptionOption].
 * @param [activity] Current activity
 * @param [packageToPurchase] The Package you wish to purchase
 * @param [onSuccess] Will be called after the purchase has completed
 * @param [onError] Will be called if there was an error with the purchase
 */
@Deprecated(
    "Use purchaseWith and PurchaseParams.Builder instead",
    ReplaceWith("purchaseWith()"),
)
fun Purchases.purchasePackageWith(
    activity: Activity,
    packageToPurchase: Package,
    onError: (error: PurchasesError, userCancelled: Boolean) -> Unit = ON_PURCHASE_ERROR_STUB,
    onSuccess: (purchase: StoreTransaction, customerInfo: CustomerInfo) -> Unit,
) {
    purchasePackage(activity, packageToPurchase, purchaseCompletedCallback(onSuccess, onError))
}

/**
 * This function will change the current appUserID.
 * Typically this would be used after a log out to identify a new user without calling configure
 * @param appUserID The new appUserID that should be linked to the currently user
 * @param [onSuccess] Will be called after the call has completed.
 * @param [onError] Will be called after the call has completed with an error.
 */
@Suppress("unused")
fun Purchases.logInWith(
    appUserID: String,
    onError: (error: PurchasesError) -> Unit = ON_ERROR_STUB,
    onSuccess: (customerInfo: CustomerInfo, created: Boolean) -> Unit,
) {
    logIn(appUserID, logInSuccessListener(onSuccess, onError))
}

/**
 * Logs out the Purchases client clearing the save appUserID. This will generate a random user
 * id and save it in the cache.
 * @param [onSuccess] Will be called after the call has completed.
 * @param [onError] Will be called if there was an error with the purchase.
 */
@Suppress("unused")
fun Purchases.logOutWith(
    onError: (error: PurchasesError) -> Unit = ON_ERROR_STUB,
    onSuccess: (customerInfo: CustomerInfo) -> Unit,
) {
    logOut(receiveCustomerInfoCallback(onSuccess, onError))
}

/**
 * Get latest available customer info.
 * @param onSuccess Called when customer info is available and not stale. Called immediately if
 * customer info is cached.
 * @param onError Will be called if there was an error with the purchase.
 */
@Suppress("unused")
fun Purchases.getCustomerInfoWith(
    onError: (error: PurchasesError) -> Unit = ON_ERROR_STUB,
    onSuccess: (customerInfo: CustomerInfo) -> Unit,
) {
    getCustomerInfo(receiveCustomerInfoCallback(onSuccess, onError))
}

/**
 * Get customer info from cache or network depending on fetch policy.
 * @param fetchPolicy Specifies cache behavior for customer info retrieval
 * @param onSuccess Called when customer info is available depending on the fetchPolicy parameter, this can be called
 * immediately or after a fetch has happened.
 * @param onError Will be called if there was an error with the purchase.
 */
@Suppress("unused")
fun Purchases.getCustomerInfoWith(
    fetchPolicy: CacheFetchPolicy,
    onError: (error: PurchasesError) -> Unit = ON_ERROR_STUB,
    onSuccess: (customerInfo: CustomerInfo) -> Unit,
) {
    getCustomerInfo(fetchPolicy, receiveCustomerInfoCallback(onSuccess, onError))
}

// region Deprecated

/**
 * Gets the SKUDetails for the given list of subscription skus.
 * @param [skus] List of skus
 * @param [onError] Will be called if there was an error with the purchase
 * @param [onReceiveSkus] Will be called after fetching subscriptions
 */
@Deprecated(
    "Replaced with getProductsWith() which returns both subscriptions and non-subscriptions",
    ReplaceWith("getProductsWith()"),
)
fun Purchases.getSubscriptionSkusWith(
    skus: List<String>,
    onError: (error: PurchasesError) -> Unit = ON_ERROR_STUB,
    onReceiveSkus: (storeProducts: List<StoreProduct>) -> Unit,
) {
    getProducts(skus, ProductType.SUBS, getStoreProductsCallback(onReceiveSkus, onError))
}

/**
 * Gets the StoreProduct for the given list of non-subscription skus.
 * @param [skus] List of skus
 * @param [onError] Will be called if there was an error with the purchase
 * @param [onReceiveSkus] Will be called after fetching StoreProduct
 */
@Deprecated(
    "Replaced with getProductsWith() which returns both subscriptions and non-subscriptions",
    ReplaceWith("getProductsWith()"),
)
fun Purchases.getNonSubscriptionSkusWith(
    skus: List<String>,
    onError: (error: PurchasesError) -> Unit,
    onReceiveSkus: (storeProducts: List<StoreProduct>) -> Unit,
) {
    getProducts(skus, ProductType.INAPP, getStoreProductsCallback(onReceiveSkus, onError))
}

// endregion
