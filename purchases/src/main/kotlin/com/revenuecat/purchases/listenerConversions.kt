package com.revenuecat.purchases

import android.app.Activity
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.interfaces.GetStoreProductCallback
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.MakePurchaseListener
import com.revenuecat.purchases.interfaces.ProductChangeCallback
import com.revenuecat.purchases.interfaces.ProductChangeListener
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsListener
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoListener
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction

@Deprecated("Purchase replaced with StoreTransaction and PurchaserInfo with CustomerInfo")
private typealias DeprecatedPurchaseCompletedFunction = (purchase: Purchase, purchaserInfo: PurchaserInfo) -> Unit
private typealias PurchaseCompletedFunction = (purchase: StoreTransaction, customerInfo: CustomerInfo) -> Unit
@Deprecated("Purchase replaced with StoreTransaction")
private typealias DeprecatedProductChangeCompletedFunction = (purchase: Purchase?, customerInfo: CustomerInfo) -> Unit
private typealias ProductChangeCompletedFunction = (purchase: StoreTransaction?, customerInfo: CustomerInfo) -> Unit
private typealias ReceiveOfferingsSuccessFunction = (offerings: Offerings) -> Unit
private typealias ReceiveCustomerInfoSuccessFunction = (customerInfo: CustomerInfo) -> Unit
private typealias ReceiveLogInSuccessFunction = (customerInfo: CustomerInfo, created: Boolean) -> Unit
internal typealias ErrorFunction = (error: PurchasesError) -> Unit
private typealias PurchaseErrorFunction = (error: PurchasesError, userCancelled: Boolean) -> Unit

internal val ON_ERROR_STUB: ErrorFunction = {}
private val ON_PURCHASE_ERROR_STUB: PurchaseErrorFunction = { _, _ -> }

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

internal fun purchaseCompletedCallback(
    onSuccess: PurchaseCompletedFunction,
    onError: PurchaseErrorFunction
) = object : PurchaseCallback {
    override fun onCompleted(purchase: StoreTransaction, customerInfo: CustomerInfo) {
        onSuccess(purchase, customerInfo)
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
    override fun onCompleted(purchase: Purchase?, customerInfo: CustomerInfo) {
        onSuccess(purchase, customerInfo)
    }

    override fun onError(error: PurchasesError, userCancelled: Boolean) {
        onError(error, userCancelled)
    }
}

internal fun productChangeCompletedListener(
    onSuccess: ProductChangeCompletedFunction,
    onError: PurchaseErrorFunction
) = object : ProductChangeCallback {
    override fun onCompleted(purchase: StoreTransaction?, customerInfo: CustomerInfo) {
        onSuccess(purchase, customerInfo)
    }

    override fun onError(error: PurchasesError, userCancelled: Boolean) {
        onError(error, userCancelled)
    }
}

internal fun getStoreProductCallback(
    onReceived: (storeProducts: List<StoreProduct>) -> Unit,
    onError: ErrorFunction
) = object : GetStoreProductCallback {
    override fun onReceived(storeProducts: List<StoreProduct>) {
        onReceived(storeProducts)
    }

    override fun onError(error: PurchasesError) {
        onError(error)
    }
}

internal fun receiveOfferingsListener(
    onSuccess: ReceiveOfferingsSuccessFunction,
    onError: ErrorFunction
) = object : ReceiveOfferingsListener {
    override fun onReceived(offerings: Offerings) {
        onSuccess(offerings)
    }

    override fun onError(error: PurchasesError) {
        onError(error)
    }
}

internal fun receiveCustomerInfoListener(
    onSuccess: ReceiveCustomerInfoSuccessFunction?,
    onError: ErrorFunction?
) = object : ReceiveCustomerInfoListener {
    override fun onReceived(customerInfo: CustomerInfo) {
        onSuccess?.invoke(customerInfo)
    }

    override fun onError(error: PurchasesError) {
        onError?.invoke(error)
    }
}

internal fun logInSuccessListener(
    onSuccess: ReceiveLogInSuccessFunction?,
    onError: ErrorFunction?
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
    onError: ErrorFunction = ON_ERROR_STUB,
    onSuccess: ReceiveOfferingsSuccessFunction
) {
    getOfferings(receiveOfferingsListener(onSuccess, onError))
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
 * Purchase product.
 * @param [activity] Current activity
 * @param [storeProduct] The storeProduct of the product you wish to purchase
 * @param [onSuccess] Will be called after the purchase has completed
 * @param [onError] Will be called after the purchase has completed with error
 */
fun Purchases.purchaseProductWith(
    activity: Activity,
    storeProduct: StoreProduct,
    onError: PurchaseErrorFunction = ON_PURCHASE_ERROR_STUB,
    onSuccess: PurchaseCompletedFunction
) {
    purchaseProduct(activity, storeProduct, purchaseCompletedCallback(onSuccess, onError))
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
    message = "SkuDetails parameter replaced with StoreProduct. The callback now returns a StoreProduct.",
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

/**
 * Make a purchase.
 * @param [activity] Current activity
 * @param [storeProduct] The storeProduct of the product you wish to purchase
 * @param [upgradeInfo] The upgradeInfo you wish to upgrade from, containing the oldSku and the optional prorationMode.
 * @param [onSuccess] Will be called after the purchase has completed
 * @param [onError] Will be called after the purchase has completed with error
 */
fun Purchases.purchaseProductWith(
    activity: Activity,
    storeProduct: StoreProduct,
    upgradeInfo: UpgradeInfo,
    onError: PurchaseErrorFunction = ON_PURCHASE_ERROR_STUB,
    onSuccess: ProductChangeCompletedFunction
) {
    purchaseProduct(activity, storeProduct, upgradeInfo, productChangeCompletedListener(onSuccess, onError))
}

/**
 * Make a purchase.
 * @param [activity] Current activity
 * @param [packageToPurchase] The Package you wish to purchase
 * @param [upgradeInfo] The upgradeInfo you wish to upgrade from, containing the oldSku and the optional prorationMode.
 * @param [onSuccess] Will be called after the purchase has completed
 * @param [onError] Will be called after the purchase has completed with error
 */
fun Purchases.purchasePackageWith(
    activity: Activity,
    packageToPurchase: Package,
    upgradeInfo: UpgradeInfo,
    onError: PurchaseErrorFunction = ON_PURCHASE_ERROR_STUB,
    onSuccess: ProductChangeCompletedFunction
) {
    purchasePackage(activity, packageToPurchase, upgradeInfo, productChangeCompletedListener(onSuccess, onError))
}

/**
 * Make a purchase.
 * @param [activity] Current activity
 * @param [packageToPurchase] The Package you wish to purchase
 * @param [onSuccess] Will be called after the purchase has completed
 * @param [onError] Will be called after the purchase has completed with error
 */
fun Purchases.purchasePackageWith(
    activity: Activity,
    packageToPurchase: Package,
    onError: PurchaseErrorFunction = ON_PURCHASE_ERROR_STUB,
    onSuccess: PurchaseCompletedFunction
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
    onError: ErrorFunction = ON_ERROR_STUB,
    onSuccess: ReceiveCustomerInfoSuccessFunction
) {
    restorePurchases(receiveCustomerInfoListener(onSuccess, onError))
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
    onError: ErrorFunction = ON_ERROR_STUB,
    onSuccess: ReceiveLogInSuccessFunction
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
    onError: ErrorFunction = ON_ERROR_STUB,
    onSuccess: ReceiveCustomerInfoSuccessFunction
) {
    logOut(receiveCustomerInfoListener(onSuccess, onError))
}

/**
 * Get latest available purchaser info.
 * @param onSuccess Called when purchaser info is available and not stale. Called immediately if
 * purchaser info is cached.
 * @param onError Will be called after the call has completed with an error.
 */
@Suppress("unused")
fun Purchases.getCustomerInfoWith(
    onError: ErrorFunction = ON_ERROR_STUB,
    onSuccess: ReceiveCustomerInfoSuccessFunction
) {
    getCustomerInfo(receiveCustomerInfoListener(onSuccess, onError))
}

/**
 * Gets the SKUDetails for the given list of subscription skus.
 * @param [skus] List of skus
 * @param [onReceiveSkus] Will be called after fetching subscriptions
 */
@Suppress("unused")
fun Purchases.getSubscriptionSkusWith(
    skus: List<String>,
    onError: ErrorFunction = ON_ERROR_STUB,
    onReceiveSkus: (storeProducts: List<StoreProduct>) -> Unit
) {
    getSubscriptionSkus(skus, getStoreProductCallback(onReceiveSkus, onError))
}

/**
 * Gets the SKUDetails for the given list of non-subscription skus.
 * @param [skus] List of skus
 * @param [onReceiveSkus] Will be called after fetching SkuDetails
 */
@Suppress("unused")
fun Purchases.getNonSubscriptionSkusWith(
    skus: List<String>,
    onError: ErrorFunction,
    onReceiveSkus: (storeProducts: List<StoreProduct>) -> Unit
) {
    getNonSubscriptionSkus(skus, getStoreProductCallback(onReceiveSkus, onError))
}
