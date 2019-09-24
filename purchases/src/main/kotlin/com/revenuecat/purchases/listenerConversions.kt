package com.revenuecat.purchases

import android.app.Activity
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.interfaces.GetSkusResponseListener
import com.revenuecat.purchases.interfaces.MakePurchaseListener
import com.revenuecat.purchases.interfaces.ReceiveOfferingsListener
import com.revenuecat.purchases.interfaces.ReceivePurchaserInfoListener

private typealias MakePurchaseCompletedSuccessFunction = (purchase: Purchase, purchaserInfo: PurchaserInfo) -> Unit
private typealias ReceiveOfferingsSuccessFunction = (offerings: Offerings) -> Unit
private typealias ReceivePurchaserInfoSuccessFunction = (purchaserInfo: PurchaserInfo) -> Unit
private typealias ErrorFunction = (error: PurchasesError) -> Unit
private typealias MakePurchaseErrorFunction = (error: PurchasesError, userCancelled: Boolean) -> Unit

private val onErrorStub: ErrorFunction = {}
private val onMakePurchaseErrorStub: MakePurchaseErrorFunction = { _, _ -> }

internal fun purchaseCompletedListener(
    onSuccess: MakePurchaseCompletedSuccessFunction,
    onError: MakePurchaseErrorFunction
) = object : MakePurchaseListener {
    override fun onCompleted(purchase: Purchase, purchaserInfo: PurchaserInfo) {
        onSuccess(purchase, purchaserInfo)
    }

    override fun onError(error: PurchasesError, userCancelled: Boolean) {
        onError(error, userCancelled)
    }
}

internal fun getSkusResponseListener(
    onReceived: (skus: List<SkuDetails>) -> Unit,
    onError: ErrorFunction
) = object : GetSkusResponseListener {
    override fun onReceived(skus: MutableList<SkuDetails>) {
        onReceived(skus)
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

internal fun receivePurchaserInfoListener(
    onSuccess: ReceivePurchaserInfoSuccessFunction?,
    onError: ErrorFunction?
) = object : ReceivePurchaserInfoListener {
    override fun onReceived(purchaserInfo: PurchaserInfo) {
        onSuccess?.invoke(purchaserInfo)
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
    onError: ErrorFunction = onErrorStub,
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
fun Purchases.purchaseProductWith(
    activity: Activity,
    skuDetails: SkuDetails,
    onError: MakePurchaseErrorFunction = onMakePurchaseErrorStub,
    onSuccess: MakePurchaseCompletedSuccessFunction
) {
    purchaseProduct(activity, skuDetails, purchaseCompletedListener(onSuccess, onError))
}

/**
 * Purchase product.
 * @param [activity] Current activity
 * @param [skuDetails] The skuDetails of the product you wish to purchase
 * @param [onSuccess] Will be called after the purchase has completed
 * @param [onError] Will be called after the purchase has completed with error
 */
fun Purchases.purchaseProductWith(
    activity: Activity,
    skuDetails: SkuDetails,
    oldSku: String,
    onError: MakePurchaseErrorFunction = onMakePurchaseErrorStub,
    onSuccess: MakePurchaseCompletedSuccessFunction
) {
    purchaseProduct(activity, skuDetails, oldSku, purchaseCompletedListener(onSuccess, onError))
}

/**
 * Make a purchase.
 * @param [activity] Current activity
 * @param [packageToPurchase] The Package you wish to purchase
 * @param [oldSku] The sku you wish to upgrade from.
 * @param [onSuccess] Will be called after the purchase has completed
 * @param [onError] Will be called after the purchase has completed with error
 */
fun Purchases.purchasePackageWith(
    activity: Activity,
    packageToPurchase: Package,
    oldSku: String,
    onError: MakePurchaseErrorFunction = onMakePurchaseErrorStub,
    onSuccess: MakePurchaseCompletedSuccessFunction
) {
    purchasePackage(activity, packageToPurchase, oldSku, purchaseCompletedListener(onSuccess, onError))
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
    onError: MakePurchaseErrorFunction = onMakePurchaseErrorStub,
    onSuccess: MakePurchaseCompletedSuccessFunction
) {
    purchasePackage(activity, packageToPurchase, purchaseCompletedListener(onSuccess, onError))
}

/**
 * Restores purchases made with the current Play Store account for the current user.
 * If you initialized Purchases with an `appUserID` any receipt tokens currently being used by
 * other users of your app will not be restored. If you used an anonymous id, i.e. you
 * initialized Purchases without an appUserID, any other anonymous users using the same
 * purchases will be merged.
 * @param [onSuccess] Will be called after the call has completed.
 * @param [onError] Will be called after the call has completed with an error.
 */
fun Purchases.restorePurchasesWith(
    onError: ErrorFunction = onErrorStub,
    onSuccess: ReceivePurchaserInfoSuccessFunction
) {
    restorePurchases(receivePurchaserInfoListener(onSuccess, onError))
}

/**
 * This function will alias two appUserIDs together.
 * @param [newAppUserID] The current user id will be aliased to the app user id passed in this parameter
 * @param [onSuccess] Will be called after the call has completed.
 * @param [onError] Will be called after the call has completed with an error.
 */
@Suppress("unused")
fun Purchases.createAliasWith(
    newAppUserID: String,
    onError: ErrorFunction = onErrorStub,
    onSuccess: ReceivePurchaserInfoSuccessFunction
) {
    createAlias(newAppUserID, receivePurchaserInfoListener(onSuccess, onError))
}

/**
 * This function will change the current appUserID.
 * Typically this would be used after a log out to identify a new user without calling configure
 * @param appUserID The new appUserID that should be linked to the currently user
 * @param [onSuccess] Will be called after the call has completed.
 * @param [onError] Will be called after the call has completed with an error.
 */
@Suppress("unused")
fun Purchases.identifyWith(
    appUserID: String,
    onError: ErrorFunction = onErrorStub,
    onSuccess: ReceivePurchaserInfoSuccessFunction
) {
    identify(appUserID, receivePurchaserInfoListener(onSuccess, onError))
}

/**
 * Resets the Purchases client clearing the save appUserID. This will generate a random user
 * id and save it in the cache.
 * @param [onSuccess] Will be called after the call has completed.
 * @param [onError] Will be called after the call has completed with an error.
 */
@Suppress("unused")
fun Purchases.resetWith(
    onError: ErrorFunction = onErrorStub,
    onSuccess: ReceivePurchaserInfoSuccessFunction
) {
    reset(receivePurchaserInfoListener(onSuccess, onError))
}

/**
 * Get latest available purchaser info.
 * @param onSuccess Called when purchaser info is available and not stale. Called immediately if
 * purchaser info is cached.
 * @param onError Will be called after the call has completed with an error.
 */
@Suppress("unused")
fun Purchases.getPurchaserInfoWith(
    onError: ErrorFunction = onErrorStub,
    onSuccess: ReceivePurchaserInfoSuccessFunction
) {
    getPurchaserInfo(receivePurchaserInfoListener(onSuccess, onError))
}

/**
 * Gets the SKUDetails for the given list of subscription skus.
 * @param [skus] List of skus
 * @param [onReceiveSkus] Will be called after fetching subscriptions
 */
@Suppress("unused")
fun Purchases.getSubscriptionSkusWith(
    skus: List<String>,
    onError: ErrorFunction = onErrorStub,
    onReceiveSkus: (skus: List<SkuDetails>) -> Unit
) {
    getSubscriptionSkus(skus, getSkusResponseListener(onReceiveSkus, onError))
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
    onReceiveSkus: (skus: List<SkuDetails>) -> Unit
) {
    getNonSubscriptionSkus(skus, getSkusResponseListener(onReceiveSkus, onError))
}


@Deprecated(
    message = "moved to getOfferingsWith()",
    replaceWith = ReplaceWith(expression = "getOfferingsWith(onError, onSuccess)"),
    level = DeprecationLevel.ERROR)
fun Purchases.getEntitlementsWith(
    onError: ErrorFunction = onErrorStub,
    onSuccess: (entitlementMap: Map<String, Any>) -> Unit
) {

}

@Deprecated(
    message = "moved to purchaseProductWith()",
    replaceWith = ReplaceWith(expression = "purchaseProductWith(activity, skuDetails, oldSku, onError, onSuccess)"),
    level = DeprecationLevel.ERROR)
fun Purchases.makePurchaseWith(
    activity: Activity,
    skuDetails: SkuDetails,
    oldSku: String,
    onError: MakePurchaseErrorFunction = onMakePurchaseErrorStub,
    onSuccess: MakePurchaseCompletedSuccessFunction
) {
    purchaseProductWith(activity, skuDetails, oldSku, onError, onSuccess)
}

@Deprecated(
    message = "moved to purchaseProductWith()",
    replaceWith = ReplaceWith(expression = "purchaseProductWith(activity, skuDetails, onError, onSuccess)"),
    level = DeprecationLevel.ERROR)
fun Purchases.makePurchaseWith(
    activity: Activity,
    skuDetails: SkuDetails,
    onError: MakePurchaseErrorFunction = onMakePurchaseErrorStub,
    onSuccess: MakePurchaseCompletedSuccessFunction
) {
    purchaseProductWith(activity, skuDetails, onError, onSuccess)
}