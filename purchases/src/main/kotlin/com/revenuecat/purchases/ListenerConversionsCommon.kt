package com.revenuecat.purchases

import com.revenuecat.purchases.interfaces.GetStoreProductsCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.models.MyAppPurchaseLogic
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction

internal val ON_ERROR_STUB: (error: PurchasesError) -> Unit = {}
internal val ON_PURCHASE_ERROR_STUB: (error: PurchasesError, userCancelled: Boolean) -> Unit = { _, _ -> }

internal fun purchaseCompletedCallback(
    onSuccess: (purchase: StoreTransaction, customerInfo: CustomerInfo) -> Unit,
    onError: (error: PurchasesError, userCancelled: Boolean) -> Unit,
) = object : PurchaseCallback {
    override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
        onSuccess(storeTransaction, customerInfo)
    }

    override fun onError(error: PurchasesError, userCancelled: Boolean) {
        onError(error, userCancelled)
    }
}

internal fun getStoreProductsCallback(
    onReceived: (storeProducts: List<StoreProduct>) -> Unit,
    onError: (error: PurchasesError) -> Unit,
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
    onError: (error: PurchasesError) -> Unit,
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
    onError: (error: PurchasesError) -> Unit?,
) = object : ReceiveCustomerInfoCallback {
    override fun onReceived(customerInfo: CustomerInfo) {
        onSuccess?.invoke(customerInfo)
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
    onSuccess: (offerings: Offerings) -> Unit,
) {
    getOfferings(receiveOfferingsCallback(onSuccess, onError))
}

fun Purchases.purchaseWith(
    purchaseParams: PurchaseParams,
    onError: (error: PurchasesError, userCancelled: Boolean) -> Unit = ON_PURCHASE_ERROR_STUB,
    onSuccess: (purchase: StoreTransaction?, customerInfo: CustomerInfo) -> Unit,
) {
    purchase(purchaseParams, purchaseCompletedCallback(onSuccess, onError))
}

/**
 * Gets the StoreProduct(s) for the given list of product ids for all product types.
 * @param [productIds] List of productIds
 * @param [onError] Will be called if there was an error with the purchase
 * @param [onGetStoreProducts] Will be called after fetching StoreProducts
 */
@Suppress("unused")
fun Purchases.getProductsWith(
    productIds: List<String>,
    onError: (error: PurchasesError) -> Unit = ON_ERROR_STUB,
    onGetStoreProducts: (storeProducts: List<StoreProduct>) -> Unit,
) {
    getProducts(productIds, getStoreProductsCallback(onGetStoreProducts, onError))
}

/**
 * Gets the StoreProduct(s) for the given list of product ids of type [type]
 * @param [productIds] List of productIds
 * @param [type] A product type to filter by
 * @param [onError] Will be called if there was an error with the purchase
 * @param [onGetStoreProducts] Will be called after fetching StoreProducts with the list of [StoreProduct] that
 * have been able to be successfully fetched from the store. Not found products will be ignored.
 */
@Suppress("unused")
fun Purchases.getProductsWith(
    productIds: List<String>,
    type: ProductType?,
    onError: (error: PurchasesError) -> Unit = ON_ERROR_STUB,
    onGetStoreProducts: (storeProducts: List<StoreProduct>) -> Unit,
) {
    getProducts(productIds, type, getStoreProductsCallback(onGetStoreProducts, onError))
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
    onSuccess: (customerInfo: CustomerInfo) -> Unit,
    myAppPurchaseLogic: MyAppPurchaseLogic?
) {
    restorePurchases(receiveCustomerInfoCallback(onSuccess, onError), myAppPurchaseLogic)
}
