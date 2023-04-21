package com.revenuecat.purchases.models

import android.os.Parcelable
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.ProrationMode
import com.revenuecat.purchases.utils.JSONObjectParceler
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
     *
     * If size > 1, indicates that a multi-line purchase occurred, which RevenueCat does not support.
     * Only the first productId will be processed by the SDK.
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
     * The id of the SubscriptionOption purchased.
     * In Google, this will be calculated from the basePlanId and offerId
     * Null for restored transactions and purchases initiated outside of the app.
     */
    val subscriptionOptionId: String?,

    /**
     * The name of the prorationMode used to perform the upgrade/downgrade of this purchase.
     * Null if it was not an upgrade/downgrade or if the purchase was restored.
     * This is not available for Amazon purchases.
     */
    val prorationMode: ProrationMode?
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

    override fun equals(other: Any?) = other is StoreTransaction &&
        ComparableData(this) == ComparableData(other)
    override fun hashCode() = ComparableData(this).hashCode()
}

/**
 * Contains fields to be used for equality, which ignores jsonObject.
 * jsonObject is excluded because we're already using the parsed fields for comparisons,
 * and to avoid complicating parcelization
 */
private data class ComparableData(
    val orderId: String?,
    val productIds: List<String>,
    val type: ProductType,
    val purchaseTime: Long,
    val purchaseToken: String,
    val purchaseState: PurchaseState,
    val isAutoRenewing: Boolean?,
    val signature: String?,
    val presentedOfferingIdentifier: String?,
    val storeUserID: String?,
    val purchaseType: PurchaseType,
    val marketplace: String?,
    val subscriptionOptionId: String?
) {
    constructor(
        storeTransaction: StoreTransaction
    ) : this(
        orderId = storeTransaction.orderId,
            productIds = storeTransaction.productIds,
            type = storeTransaction.type,
            purchaseTime = storeTransaction.purchaseTime,
            purchaseToken = storeTransaction.purchaseToken,
            purchaseState = storeTransaction.purchaseState,
            isAutoRenewing = storeTransaction.isAutoRenewing,
            signature = storeTransaction.signature,
            presentedOfferingIdentifier = storeTransaction.presentedOfferingIdentifier,
            storeUserID = storeTransaction.storeUserID,
            purchaseType = storeTransaction.purchaseType,
            marketplace = storeTransaction.marketplace,
            subscriptionOptionId = storeTransaction.subscriptionOptionId
    )
}

enum class PurchaseType {
    GOOGLE_PURCHASE,
    GOOGLE_RESTORED_PURCHASE,
    AMAZON_PURCHASE
}
