package com.revenuecat.purchases.models

import android.os.Parcelable
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.parceler.JSONObjectParceler
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.TypeParceler
import org.json.JSONObject

/**
 * Represents an in-app billing purchase.
 */
@Parcelize
@TypeParceler<JSONObject, JSONObjectParceler>()
data class PurchaseDetails(
    /**
     * Unique Google order identifier for the purchased transaction.
     *
     * Only available for non-restored Google purchases.
     */
    val orderId: String?,

    /**
     * Product Id.
     */
    val sku: String,

    /**
     * Type of the product associated with the purchase.
     */
    val type: ProductType,

    /**
     * Time the product was purchased, in milliseconds since the epoch.
     */
    val purchaseTime: Long,

    /**
     * Token that uniquely identifies a purchase.
     */
    val purchaseToken: String,

    /**
     * State of the purchase.
     */
    val purchaseState: RevenueCatPurchaseState,

    /**
     * Whether the subscription renews automatically.
     *
     * Null for Google restored purchases.
     */
    val isAutoRenewing: Boolean?,

    /**
     * String containing the signature of the Google purchase data that was signed with the private key of
     * the developer.
     */
    val signature: String?,

    /**
     * Returns a JSONObject format that contains details about the purchase.
     */
    val originalJson: JSONObject,

    /**
     * Offering that was presented when making the purchase. Not available for restored purchases.
     */
    val presentedOfferingIdentifier: String?,

    /**
     * Null for Google
     */
    val storeUserID: String?,

    /**
     * One of [PurchaseType] indicating the type of purchase.
     */
    val purchaseType: PurchaseType
) : Parcelable

enum class PurchaseType {
    GOOGLE_PURCHASE,
    GOOGLE_RESTORED_PURCHASE,
    AMAZON_PURCHASE
}
