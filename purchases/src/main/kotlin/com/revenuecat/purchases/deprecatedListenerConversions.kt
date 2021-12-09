package com.revenuecat.purchases

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
