package com.revenuecat.purchases

import android.app.Activity
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.ProductChangeCallback
import com.revenuecat.purchases.interfaces.SyncAttributesAndOfferingsCallback
import com.revenuecat.purchases.interfaces.SyncPurchasesCallback
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.SubscriptionOption

internal fun logInSuccessListener(
    onSuccess: (customerInfo: CustomerInfo, created: Boolean) -> Unit?,
    onError: (error: PurchasesError) -> Unit?,
) = object : LogInCallback {
    override fun onReceived(customerInfo: CustomerInfo, created: Boolean) {
        onSuccess?.invoke(customerInfo, created)
    }

    override fun onError(error: PurchasesError) {
        onError?.invoke(error)
    }
}

internal fun productChangeCompletedListener(
    onSuccess: (purchase: StoreTransaction?, customerInfo: CustomerInfo) -> Unit,
    onError: (error: PurchasesError, userCancelled: Boolean) -> Unit,
) = object : ProductChangeCallback {
    override fun onCompleted(purchase: StoreTransaction?, customerInfo: CustomerInfo) {
        onSuccess(purchase, customerInfo)
    }

    override fun onError(error: PurchasesError, userCancelled: Boolean) {
        onError(error, userCancelled)
    }
}

internal fun syncPurchasesListener(
    onSuccess: (CustomerInfo) -> Unit,
    onError: (error: PurchasesError) -> Unit,
) = object : SyncPurchasesCallback {
    override fun onSuccess(customerInfo: CustomerInfo) {
        onSuccess(customerInfo)
    }

    override fun onError(error: PurchasesError) {
        onError(error)
    }
}

internal fun syncAttributesAndOfferingsListener(
    onSuccess: (Offerings) -> Unit,
    onError: (error: PurchasesError) -> Unit,
) = object : SyncAttributesAndOfferingsCallback {
    override fun onSuccess(offerings: Offerings) {
        onSuccess(offerings)
    }

    override fun onError(error: PurchasesError) {
        onError(error)
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

/**
 * This method will send all the purchases to the RevenueCat backend. Call this when using your own implementation
 * for subscriptions anytime a sync is needed, such as when migrating existing users to RevenueCat. The
 * [onSuccess] callback will be called if all purchases have been synced successfully or
 * there are no purchases. Otherwise, the [onError] callback will be called with a
 * [PurchasesError] indicating the first error found.
 *
 * @param [onError] Called when there was an error syncing one or more of the purchases. Will return the first error
 * found syncing the purchases.
 * @param [onSuccess] Called when all purchases have been successfully synced with the backend or if no purchases are
 * present.
 * @warning This function should only be called if you're migrating to RevenueCat or in observer mode.
 * @warning This function could take a relatively long time to execute, depending on the amount of purchases
 * the user has. Consider that when waiting for this operation to complete.
 */
fun Purchases.syncPurchasesWith(
    onError: (error: PurchasesError) -> Unit = ON_ERROR_STUB,
    onSuccess: (CustomerInfo) -> Unit,
) {
    syncPurchases(syncPurchasesListener(onSuccess, onError))
}

/**
 * Syncs subscriber attributes and then fetches the configured offerings for this user. This method is intended to
 * be called when using Targeting Rules with Custom Attributes. Any subscriber attributes should be set before
 * calling this method to ensure the returned offerings are applied with the latest subscriber attributes.
 *
 * This method is rate limited to 5 calls per minute. It will log a warning and return offerings cache when reached.
 *
 * Refer to [the guide](https://www.revenuecat.com/docs/tools/targeting) for more targeting information
 * For more offerings information, see [getOfferings]
 *
 * @param [onError] Called when there was an error syncing attributes or fetching offerings. Will return the first error
 * found syncing the purchases.
 * @param [onSuccess] Called when all attributes are synced and offerings are fetched.
 */
@Suppress("unused")
fun Purchases.syncAttributesAndOfferingsIfNeededWith(
    onError: (error: PurchasesError) -> Unit = ON_ERROR_STUB,
    onSuccess: (Offerings) -> Unit,
) {
    syncAttributesAndOfferingsIfNeeded(syncAttributesAndOfferingsListener(onSuccess, onError))
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
