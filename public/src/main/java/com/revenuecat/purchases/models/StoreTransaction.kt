package com.revenuecat.purchases.models

import android.os.Parcelable
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.parceler.JSONObjectParceler
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.json.JSONObject

/**
 * Represents an in-app billing purchase.
 */
@Parcelize
@TypeParceler<JSONObject, JSONObjectParceler>()
data class StoreTransaction(
    /**
     * Unique Google order identifier for the purchased transaction.
     *
     * Only available for non-restored Google purchases. Always null for Amazon.
     */
    val orderId: String?,

    /**
     * Product IDs purchased.
     */
    val productIds: List<String>,

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
    val purchaseState: PurchaseState,

    /**
     * Whether the subscription renews automatically.
     *
     * Null for Google restored purchases.
     */
    val isAutoRenewing: Boolean?,

    /**
     * String containing the signature of the Google purchase data that was signed with the private key of
     * the developer. Always null for Amazon.
     */
    val signature: String?,

    /**
     * Returns a JSONObject format that contains details about the purchase.
     */
    val originalJson: JSONObject,

    /**
     * Offering that was presented when making the purchase. Always null for restored purchases.
     */
    val presentedOfferingIdentifier: String?,

    /**
     * Amazon's store user id. Null for Google
     */
    val storeUserID: String?,

    /**
     * One of [PurchaseType] indicating the type of purchase.
     */
    val purchaseType: PurchaseType,

    /**
     * Amazon's marketplace. Null for Google
     */
    val marketplace: String?,

    /**
     * The id of the PurchaseOption purchased.
     * In Google, this will either be the basePlanId or the offerId.
     * Null for restored transactions and purchases initiated outside of the app.
     */
    val purchaseOptionId: String?
) : Parcelable {

    /**
     * Skus associated with the transaction
     */
    @IgnoredOnParcel
    @Deprecated(
        "Replaced with productIds",
        ReplaceWith("productIds")
    )
    val skus: List<String>
        get() = productIds

}

enum class PurchaseType {
    GOOGLE_PURCHASE,
    GOOGLE_RESTORED_PURCHASE,
    AMAZON_PURCHASE
}
