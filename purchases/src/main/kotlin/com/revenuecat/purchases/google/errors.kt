package com.revenuecat.purchases.google

import com.android.billingclient.api.BillingClient
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode

internal fun @receiver:BillingClient.BillingResponseCode Int.getBillingResponseCodeName(): String {
    val allPossibleBillingResponseCodes = BillingClient.BillingResponseCode::class.java.declaredFields
    return allPossibleBillingResponseCodes
        .firstOrNull { it.getInt(it) == this }
        ?.name
        ?: "$this"
}

internal fun Int.billingResponseToPurchasesError(underlyingErrorMessage: String): PurchasesError {
    val errorCode = when (this) {
        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
        BillingClient.BillingResponseCode.ITEM_NOT_OWNED,
        BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
        -> PurchasesErrorCode.PurchaseNotAllowedError
        BillingClient.BillingResponseCode.ERROR,
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
        BillingClient.BillingResponseCode.SERVICE_TIMEOUT,
        -> PurchasesErrorCode.StoreProblemError
        BillingClient.BillingResponseCode.OK -> PurchasesErrorCode.UnknownError
        BillingClient.BillingResponseCode.USER_CANCELED -> PurchasesErrorCode.PurchaseCancelledError
        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> PurchasesErrorCode.ProductNotAvailableForPurchaseError
        BillingClient.BillingResponseCode.DEVELOPER_ERROR -> PurchasesErrorCode.PurchaseInvalidError
        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> PurchasesErrorCode.ProductAlreadyPurchasedError
        BillingClient.BillingResponseCode.NETWORK_ERROR -> PurchasesErrorCode.NetworkError
        else -> PurchasesErrorCode.UnknownError
    }
    return PurchasesError(errorCode, underlyingErrorMessage)
}
