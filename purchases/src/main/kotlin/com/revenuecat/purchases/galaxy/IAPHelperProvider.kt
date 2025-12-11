package com.revenuecat.purchases.galaxy

import com.samsung.android.sdk.iap.lib.listener.OnGetProductsDetailsListener
import com.samsung.android.sdk.iap.lib.listener.OnPaymentListener

internal interface IAPHelperProvider {
    fun getProductsDetails(
        productIDs: String,
        onGetProductsDetailsListener: OnGetProductsDetailsListener,
    )

    /**
     * Starts a purchase flow for the given product through Samsung IAP.
     *
     * @param itemId Galaxy Store product identifier.
     * @param obfuscatedAccountId A unique, obfuscated value of up to 64 bytes, tied exclusively to the customer's
     * account. This value helps the Galaxy Store detect payment fraud. Storing unencrypted personal data
     * in this field may result in the purchase being rejected. Use a one-way hash function to create this value.
     * @param obfuscatedProfileId An obfuscated value (up to 64 bytes) that is strictly associated with a customer's
     * profile in your app is required. If this value is set, the obfuscatedAccountId must also be set.
     * If your app supports multiple profiles under one account, use this parameter for the obfuscated profile ID.
     * @param onPaymentListener Callback that receives purchase success or failure results.
     * @return `true` if the request was dispatched to the store and a response will arrive
     * through [OnPaymentListener]; `false` if the request could not be sent.
     */
    fun startPayment(
        itemId: String,
        obfuscatedAccountId: String?,
        obfuscatedProfileId: String?,
        onPaymentListener: OnPaymentListener
    ): Boolean

}
