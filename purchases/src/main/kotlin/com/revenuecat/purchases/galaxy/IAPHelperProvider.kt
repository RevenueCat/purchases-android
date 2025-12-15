package com.revenuecat.purchases.galaxy

import android.content.Context
import com.revenuecat.purchases.galaxy.utils.GalaxySerialOperation
import com.samsung.android.sdk.iap.lib.constants.HelperDefine
import com.samsung.android.sdk.iap.lib.listener.OnAcknowledgePurchasesListener
import com.samsung.android.sdk.iap.lib.listener.OnConsumePurchasedItemsListener
import com.samsung.android.sdk.iap.lib.listener.OnGetProductsDetailsListener
import com.samsung.android.sdk.iap.lib.listener.OnPaymentListener

internal interface IAPHelperProvider {

    fun setOperationMode(
        mode: HelperDefine.OperationMode,
    )

    /**
     * Whether or not purchases can be acknowledged.
     */
    fun isAcknowledgeAvailable(
        context: Context,
    ): Boolean

    @GalaxySerialOperation
    fun getProductsDetails(
        productIDs: String,
        onGetProductsDetailsListener: OnGetProductsDetailsListener,
    )

    /**
     * Starts a purchase flow for the given product through the Galaxy Store.
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
    @GalaxySerialOperation
    fun startPayment(
        itemId: String,
        obfuscatedAccountId: String?,
        obfuscatedProfileId: String?,
        onPaymentListener: OnPaymentListener,
    ): Boolean

    /**
     * Sends an acknowledge request for one or more completed purchases.
     *
     * @param purchaseIds Comma-separated purchase and payment identifiers for the non-consumable items or
     * subscriptions being acknowledged.
     * @param onAcknowledgePurchasesListener Callback that receives the acknowledgement result.
     * @return `true` if the request was handed off to Galaxy Store and results will arrive via
     * [OnAcknowledgePurchasesListener]; `false` if the request could not be sent.
     */
    @GalaxySerialOperation
    fun acknowledgePurchases(
        purchaseIds: String,
        onAcknowledgePurchasesListener: OnAcknowledgePurchasesListener,
    ): Boolean

    /**
     * Sends an consume request for one or more consumable purchases.
     *
     * @param purchaseIds Comma-separated purchase and payment identifiers for the consumable items being consumed.
     * @param onConsumePurchasedItemsListener Callback that receives the consumption result.
     * @return `true` if the request was handed off to Galaxy Store and results will arrive via
     * [OnConsumePurchasedItemsListener]; `false` if the request could not be sent.
     */
    @GalaxySerialOperation
    fun consumePurchasedItems(
        purchaseIds: String,
        onConsumePurchasedItemsListener: OnConsumePurchasedItemsListener,
    ): Boolean
}
