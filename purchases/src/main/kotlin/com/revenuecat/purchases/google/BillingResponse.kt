package com.revenuecat.purchases.google

import com.android.billingclient.api.BillingClient.BillingResponseCode

internal sealed class BillingResponse {

    public object FeatureNotSupported : BillingResponse()
    public object ServiceDisconnected : BillingResponse()
    public object OK : BillingResponse()
    public object UserCanceled : BillingResponse()
    public object ServiceUnavailable : BillingResponse()
    public object BillingUnavailable : BillingResponse()
    public object ItemUnavailable : BillingResponse()
    public object DeveloperError : BillingResponse()
    public object Error : BillingResponse()
    public object ItemAlreadyOwned : BillingResponse()
    public object ItemNotOwned : BillingResponse()
    public object NetworkError : BillingResponse()
    public object Unknown : BillingResponse()

    public companion object {
        @Suppress("CyclomaticComplexMethod")
        public fun fromCode(code: Int): BillingResponse {
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
