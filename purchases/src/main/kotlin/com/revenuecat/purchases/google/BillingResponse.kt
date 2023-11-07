package com.revenuecat.purchases.google

import com.android.billingclient.api.BillingClient.BillingResponseCode

sealed class BillingResponse {

    object FeatureNotSupported : BillingResponse()
    object ServiceDisconnected : BillingResponse()
    object OK : BillingResponse()
    object UserCanceled : BillingResponse()
    object ServiceUnavailable : BillingResponse()
    object BillingUnavailable : BillingResponse()
    object ItemUnavailable : BillingResponse()
    object DeveloperError : BillingResponse()
    object Error : BillingResponse()
    object ItemAlreadyOwned : BillingResponse()
    object ItemNotOwned : BillingResponse()
    object NetworkError : BillingResponse()
    object Unknown : BillingResponse()

    companion object {
        @Suppress("CyclomaticComplexMethod")
        fun fromCode(code: Int): BillingResponse {
            return when (code) {
                BillingResponseCode.SERVICE_TIMEOUT -> ServiceUnavailable
                BillingResponseCode.FEATURE_NOT_SUPPORTED -> FeatureNotSupported
                BillingResponseCode.SERVICE_DISCONNECTED -> ServiceDisconnected
                BillingResponseCode.OK -> OK
                BillingResponseCode.USER_CANCELED -> UserCanceled
                BillingResponseCode.SERVICE_UNAVAILABLE -> ServiceUnavailable
                BillingResponseCode.BILLING_UNAVAILABLE -> BillingUnavailable
                BillingResponseCode.ITEM_UNAVAILABLE -> ItemUnavailable
                BillingResponseCode.DEVELOPER_ERROR -> DeveloperError
                BillingResponseCode.ERROR -> Error
                BillingResponseCode.ITEM_ALREADY_OWNED -> ItemAlreadyOwned
                BillingResponseCode.ITEM_NOT_OWNED -> ItemNotOwned
                BillingResponseCode.NETWORK_ERROR -> NetworkError
                else -> Unknown
            }
        }
    }
}
