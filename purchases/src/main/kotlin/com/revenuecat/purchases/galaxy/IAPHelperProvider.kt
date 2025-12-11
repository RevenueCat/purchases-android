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
     * @param obfuscatedAccountId
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
