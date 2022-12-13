package com.revenuecat.purchases

import android.app.Activity
import com.revenuecat.purchases.interfaces.GetStoreProductsCallback
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.ProductChangeCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.models.PurchaseOption
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction

internal val ON_ERROR_STUB: (error: PurchasesError) -> Unit = {}
internal val ON_PURCHASE_ERROR_STUB: (error: PurchasesError, userCancelled: Boolean) -> Unit = { _, _ -> }

internal fun purchaseCompletedCallback(
    onSuccess: (purchase: StoreTransaction, customerInfo: CustomerInfo) -> Unit,
    onError: (error: PurchasesError, userCancelled: Boolean) -> Unit
) = object : PurchaseCallback {
    override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
        onSuccess(storeTransaction, customerInfo)
    }

    override fun onError(error: PurchasesError, userCancelled: Boolean) {
        onError(error, userCancelled)
    }
}

internal fun productChangeCompletedListener(
    onSuccess: (purchase: StoreTransaction?, customerInfo: CustomerInfo) -> Unit,
    onError: (error: PurchasesError, userCancelled: Boolean) -> Unit
) = object : ProductChangeCallback {
    override fun onCompleted(purchase: StoreTransaction?, customerInfo: CustomerInfo) {
        onSuccess(purchase, customerInfo)
    }

    override fun onError(error: PurchasesError, userCancelled: Boolean) {
        onError(error, userCancelled)
    }
}

internal fun getStoreProductsCallback(
    onReceived: (storeProducts: List<StoreProduct>) -> Unit,
    onError: (error: PurchasesError) -> Unit
) = object : GetStoreProductsCallback {
    override fun onReceived(storeProducts: List<StoreProduct>) {
        onReceived(storeProducts)
    }

    override fun onError(error: PurchasesError) {
        onError(error)
    }
}

internal fun receiveOfferingsCallback(
    onSuccess: (offerings: Offerings) -> Unit,
    onError: (error: PurchasesError) -> Unit
) = object : ReceiveOfferingsCallback {
    override fun onReceived(offerings: Offerings) {
        onSuccess(offerings)
    }

    override fun onError(error: PurchasesError) {
        onError(error)
    }
}

internal fun receiveCustomerInfoCallback(
    onSuccess: (customerInfo: CustomerInfo) -> Unit?,
    onError: (error: PurchasesError) -> Unit?
) = object : ReceiveCustomerInfoCallback {
    override fun onReceived(customerInfo: CustomerInfo) {
        onSuccess?.invoke(customerInfo)
    }

    override fun onError(error: PurchasesError) {
        onError?.invoke(error)
    }
}

internal fun logInSuccessListener(
    onSuccess: (customerInfo: CustomerInfo, created: Boolean) -> Unit?,
    onError: (error: PurchasesError) -> Unit?
) = object : LogInCallback {
    override fun onReceived(customerInfo: CustomerInfo, created: Boolean) {
        onSuccess?.invoke(customerInfo, created)
    }

    override fun onError(error: PurchasesError) {
        onError?.invoke(error)
    }
}

/**
 * Fetch the configured offerings for this users. Offerings allows you to configure your in-app
 * products vis RevenueCat and greatly simplifies management. See
 * [the guide](https://docs.revenuecat.com/offerings) for more info.
 *
 * Offerings will be fetched and cached on instantiation so that, by the time they are needed,
 * your prices are loaded for your purchase flow. Time is money.
 *
 * @param [onSuccess] Called when offerings are available. Called immediately if offerings are cached.
 * @param [onError] Will be called after an error fetching offerings.
 */
@Suppress("unused")
fun Purchases.getOfferingsWith(
    onError: (error: PurchasesError) -> Unit = ON_ERROR_STUB,
    onSuccess: (offerings: Offerings) -> Unit
) {
    getOfferings(receiveOfferingsCallback(onSuccess, onError))
}

/**
 * Purchase product. If purchasing a subscription, it will choose the default [PurchaseOption].
 * @param [activity] Current activity
 * @param [storeProduct] The storeProduct of the product you wish to purchase
 * @param [onSuccess] Will be called after the purchase has completed
 * @param [onError] Will be called after the purchase has completed with error
 */
fun Purchases.purchaseProductWith(
    activity: Activity,
    storeProduct: StoreProduct,
    onError: (error: PurchasesError, userCancelled: Boolean) -> Unit = ON_PURCHASE_ERROR_STUB,
    onSuccess: (purchase: StoreTransaction, customerInfo: CustomerInfo) -> Unit
) {
    purchaseProduct(activity, storeProduct, purchaseCompletedCallback(onSuccess, onError))
}

/**
 * Make a purchase upgrading from a previous sku. If purchasing a subscription, it will choose the
 * default [PurchaseOption].
 * @param [activity] Current activity
 * @param [storeProduct] The storeProduct of the product you wish to purchase
 * @param [upgradeInfo] The upgradeInfo you wish to upgrade from, containing the oldSku and the optional prorationMode.
 * Amazon Appstore doesn't support changing products so upgradeInfo is ignored for Amazon purchases.
 * @param [onSuccess] Will be called after the purchase has completed
 * @param [onError] Will be called after the purchase has completed with error
 */
fun Purchases.purchaseProductWith(
    activity: Activity,
    storeProduct: StoreProduct,
    upgradeInfo: UpgradeInfo,
    onError: (error: PurchasesError, userCancelled: Boolean) -> Unit = ON_PURCHASE_ERROR_STUB,
    onSuccess: (purchase: StoreTransaction?, customerInfo: CustomerInfo) -> Unit
) {
    purchaseProduct(activity, storeProduct, upgradeInfo, productChangeCompletedListener(onSuccess, onError))
}

// TODOBC5: Remove storeProduct parameter
/**
 * Purchase a subscription [StoreProduct]'s [PurchaseOption].
 * @param [activity] Current activity
 * @param [storeProduct] The storeProduct of the product you wish to purchase
 * @param [purchaseOption] Your choice of purchase options available for the subscription StoreProduct
 * @param [onSuccess] Will be called after the purchase has completed
 * @param [onError] Will be called after the purchase has completed with error
 */
fun Purchases.purchaseSubscriptionOptionWith(
    activity: Activity,
    storeProduct: StoreProduct,
    purchaseOption: PurchaseOption,
    onError: (error: PurchasesError, userCancelled: Boolean) -> Unit = ON_PURCHASE_ERROR_STUB,
    onSuccess: (purchase: StoreTransaction, customerInfo: CustomerInfo) -> Unit
) {
    purchaseSubscriptionOption(
        activity,
        storeProduct,
        purchaseOption,
        purchaseCompletedCallback(onSuccess, onError)
    )
}

// TODOBC5: Remove storeProduct parameter
/**
 * Purchase a subscription [StoreProduct]'s [PurchaseOption], upgrading from an old product.
 * @param [activity] Current activity
 * @param [storeProduct] The storeProduct of the product you wish to purchase
 * @param [purchaseOption] Your choice of purchase options available for the subscription StoreProduct
 * @param [upgradeInfo] The upgradeInfo you wish to upgrade from, containing the oldSku and the optional prorationMode.
 * Amazon Appstore doesn't support changing products so upgradeInfo is ignored for Amazon purchases.
 * @param [onSuccess] Will be called after the purchase has completed
 * @param [onError] Will be called after the purchase has completed with error
 */
@Suppress("LongParameterList")
fun Purchases.purchaseSubscriptionOptionWith(
    activity: Activity,
    storeProduct: StoreProduct,
    purchaseOption: PurchaseOption,
    upgradeInfo: UpgradeInfo,
    onError: (error: PurchasesError, userCancelled: Boolean) -> Unit = ON_PURCHASE_ERROR_STUB,
    onSuccess: (purchase: StoreTransaction?, customerInfo: CustomerInfo) -> Unit
) {
    purchaseSubscriptionOption(
        activity,
        storeProduct,
        purchaseOption,
        upgradeInfo,
        productChangeCompletedListener(onSuccess, onError)
    )
}

/**
 * Make a purchase upgrading from a previous sku. If purchasing a subscription, it will choose the
 * default [PurchaseOption].
 * @param [activity] Current activity
 * @param [packageToPurchase] The Package you wish to purchase
 * @param [upgradeInfo] The upgradeInfo you wish to upgrade from, containing the oldSku and the optional prorationMode.
 * Amazon Appstore doesn't support changing products so upgradeInfo is ignored for Amazon purchases.
 * @param [onSuccess] Will be called after the purchase has completed
 * @param [onError] Will be called after the purchase has completed with error
 */
fun Purchases.purchasePackageWith(
    activity: Activity,
    packageToPurchase: Package,
    upgradeInfo: UpgradeInfo,
    onError: (error: PurchasesError, userCancelled: Boolean) -> Unit = ON_PURCHASE_ERROR_STUB,
    onSuccess: (purchase: StoreTransaction?, customerInfo: CustomerInfo) -> Unit
) {
    purchasePackage(activity, packageToPurchase, upgradeInfo, productChangeCompletedListener(onSuccess, onError))
}

/**
 * Make a purchase. If purchasing a subscription, it will choose the default [PurchaseOption].
 * @param [activity] Current activity
 * @param [packageToPurchase] The Package you wish to purchase
 * @param [onSuccess] Will be called after the purchase has completed
 * @param [onError] Will be called after the purchase has completed with error
 */
fun Purchases.purchasePackageWith(
    activity: Activity,
    packageToPurchase: Package,
    onError: (error: PurchasesError, userCancelled: Boolean) -> Unit = ON_PURCHASE_ERROR_STUB,
    onSuccess: (purchase: StoreTransaction, customerInfo: CustomerInfo) -> Unit
) {
    purchasePackage(activity, packageToPurchase, purchaseCompletedCallback(onSuccess, onError))
}

/**
 * Restores purchases made with the current Play Store account for the current user.
 * This method will post all purchases associated with the current Play Store account to
 * RevenueCat and become associated with the current `appUserID`. If the receipt token is being
 * used by an existing user, the current `appUserID` will be aliased together with the
 * `appUserID` of the existing user. Going forward, either `appUserID` will be able to reference
 * the same user.
 *
 * You shouldn't use this method if you have your own account system. In that case
 * "restoration" is provided by your app passing the same `appUserId` used to purchase originally.
 * @param [onSuccess] Will be called after the call has completed.
 * @param [onError] Will be called after the call has completed with an error.
 */
fun Purchases.restorePurchasesWith(
    onError: (error: PurchasesError) -> Unit = ON_ERROR_STUB,
    onSuccess: (customerInfo: CustomerInfo) -> Unit
) {
    restorePurchases(receiveCustomerInfoCallback(onSuccess, onError))
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
    onSuccess: (customerInfo: CustomerInfo, created: Boolean) -> Unit
) {
    logIn(appUserID, logInSuccessListener(onSuccess, onError))
}

/**
 * Logs out the Purchases client clearing the save appUserID. This will generate a random user
 * id and save it in the cache.
 * @param [onSuccess] Will be called after the call has completed.
 * @param [onError] Will be called after the call has completed with an error.
 */
@Suppress("unused")
fun Purchases.logOutWith(
    onError: (error: PurchasesError) -> Unit = ON_ERROR_STUB,
    onSuccess: (customerInfo: CustomerInfo) -> Unit
) {
    logOut(receiveCustomerInfoCallback(onSuccess, onError))
}

/**
 * Get latest available customer info.
 * @param onSuccess Called when customer info is available and not stale. Called immediately if
 * customer info is cached.
 * @param onError Will be called after the call has completed with an error.
 */
@Suppress("unused")
fun Purchases.getCustomerInfoWith(
    onError: (error: PurchasesError) -> Unit = ON_ERROR_STUB,
    onSuccess: (customerInfo: CustomerInfo) -> Unit
) {
    getCustomerInfo(receiveCustomerInfoCallback(onSuccess, onError))
}

/**
 * Get customer info from cache or network depending on fetch policy.
 * @param fetchPolicy Specifies cache behavior for customer info retrieval
 * @param onSuccess Called when customer info is available depending on the fetchPolicy parameter, this can be called
 * immediately or after a fetch has happened.
 * @param onError Will be called after the call has completed with an error.
 */
@Suppress("unused")
fun Purchases.getCustomerInfoWith(
    fetchPolicy: CacheFetchPolicy,
    onError: (error: PurchasesError) -> Unit = ON_ERROR_STUB,
    onSuccess: (customerInfo: CustomerInfo) -> Unit
) {
    getCustomerInfo(fetchPolicy, receiveCustomerInfoCallback(onSuccess, onError))
}

/**
 * Gets the SKUDetails for the given list of subscription skus.
 * @param [skus] List of skus
 * @param [onReceiveSkus] Will be called after fetching subscriptions
 */
@Suppress("unused")
fun Purchases.getSubscriptionSkusWith(
    skus: List<String>,
    onError: (error: PurchasesError) -> Unit = ON_ERROR_STUB,
    onReceiveSkus: (storeProducts: List<StoreProduct>) -> Unit
) {
    getSubscriptionSkus(skus, getStoreProductsCallback(onReceiveSkus, onError))
}

/**
 * Gets the SKUDetails for the given list of non-subscription skus.
 * @param [skus] List of skus
 * @param [onReceiveSkus] Will be called after fetching SkuDetails
 */
@Suppress("unused")
fun Purchases.getNonSubscriptionSkusWith(
    skus: List<String>,
    onError: (error: PurchasesError) -> Unit,
    onReceiveSkus: (storeProducts: List<StoreProduct>) -> Unit
) {
    getNonSubscriptionSkus(skus, getStoreProductsCallback(onReceiveSkus, onError))
}
