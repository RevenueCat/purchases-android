package com.revenuecat.purchases.models

import android.os.Parcelable
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.ReplacementMode
import com.revenuecat.purchases.utils.JSONObjectParceler
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.json.JSONObject

/**
 * Represents an in-app billing purchase.
 */
@Parcelize
@TypeParceler<JSONObject, JSONObjectParceler>()
@Poko
public class StoreTransaction @ExperimentalPreviewRevenueCatPurchasesAPI constructor(
    /**
     * Unique Google order identifier for the purchased transaction.
     *
     * Only available for non-restored Google purchases. Always null for Amazon.
     */
    public val orderId: String?,

    /**
     * Product IDs purchased.
     *
     * If size > 1, indicates that a multi-line purchase occurred.
     */
    public val productIds: List<String>,

    /**
     * Type of the product associated with the purchase.
     */
    public val type: ProductType,

    /**
     * Time the product was purchased, in milliseconds since the epoch.
     */
    public val purchaseTime: Long,

    /**
     * Token that uniquely identifies a purchase.
     */
    public val purchaseToken: String,

    /**
     * State of the purchase.
     */
    public val purchaseState: PurchaseState,

    /**
     * Whether the subscription renews automatically.
     *
     * Null for Google restored purchases.
     */
    public val isAutoRenewing: Boolean?,

    /**
     * String containing the signature of the Google purchase data that was signed with the private key of
     * the developer. Always null for Amazon and Galaxy.
     */
    public val signature: String?,

    /**
     * Returns a JSONObject format that contains details about the purchase.
     */
    public val originalJson: JSONObject,

    /**
     * Context of the offering that was presented when making the purchase.
     */
    public val presentedOfferingContext: PresentedOfferingContext?,

    /**
     * Amazon's store user id. Null for Google and Galaxy
     */
    public val storeUserID: String?,

    /**
     * One of [PurchaseType] indicating the type of purchase.
     */
    public val purchaseType: PurchaseType,

    /**
     * Amazon's marketplace. Null for Google and Galaxy
     */
    public val marketplace: String?,

    /**
     * The id of the SubscriptionOption purchased.
     * In Google, this will be calculated from the basePlanId and offerId
     * Null for restored transactions and purchases initiated outside of the app.
     */
    public val subscriptionOptionId: String?,

    /**
     * The id of the SubscriptionOption purchased for each product ID.
     *
     * In Google, this will be calculated from the basePlanId and offerId
     * Null in Google for restored transactions and purchases initiated outside of the app.
     * Null for Amazon and Galaxy purchases.
     */
    // We've marked this with @get:JvmSynthetic because its synthesized
    // getter was not getting the @ExperimentalPreviewRevenueCatPurchasesAPI annotation
    // applied to it, and there doesn't appear to be a way to do so.
    // We can remove this @get:JvmSynthetic annotation when we remove the experimental annotation from this
    // property.
    @ExperimentalPreviewRevenueCatPurchasesAPI
    @get:JvmSynthetic
    public val subscriptionOptionIdForProductIDs: Map<String, String>?,

    /**
     * The replacementMode used to perform the upgrade/downgrade of this purchase.
     * Null if it was not an upgrade/downgrade or if the purchase was restored.
     * This is not available for Amazon purchases.
     */
    public val replacementMode: ReplacementMode?,
) : Parcelable {

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    public constructor(
        orderId: String?,
        productIds: List<String>,
        type: ProductType,
        purchaseTime: Long,
        purchaseToken: String,
        purchaseState: PurchaseState,
        isAutoRenewing: Boolean?,
        signature: String?,
        originalJson: JSONObject,
        presentedOfferingContext: PresentedOfferingContext?,
        storeUserID: String?,
        purchaseType: PurchaseType,
        marketplace: String?,
        subscriptionOptionId: String?,
        replacementMode: ReplacementMode?,
    ) : this(
        orderId = orderId,
        productIds = productIds,
        type = type,
        purchaseTime = purchaseTime,
        purchaseToken = purchaseToken,
        purchaseState = purchaseState,
        isAutoRenewing = isAutoRenewing,
        signature = signature,
        originalJson = originalJson,
        presentedOfferingContext = presentedOfferingContext,
        storeUserID = storeUserID,
        purchaseType = purchaseType,
        marketplace = marketplace,
        subscriptionOptionId = subscriptionOptionId,
        subscriptionOptionIdForProductIDs = emptyMap(),
        replacementMode = replacementMode,
    )

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Deprecated("Use constructor with presentedOfferingContext instead")
    public constructor(
        orderId: String?,
        productIds: List<String>,
        type: ProductType,
        purchaseTime: Long,
        purchaseToken: String,
        purchaseState: PurchaseState,
        isAutoRenewing: Boolean?,
        signature: String?,
        originalJson: JSONObject,
        presentedOfferingIdentifier: String?,
        storeUserID: String?,
        purchaseType: PurchaseType,
        marketplace: String?,
        subscriptionOptionId: String?,
        replacementMode: ReplacementMode?,
    ) : this(
        orderId,
        productIds,
        type,
        purchaseTime,
        purchaseToken,
        purchaseState,
        isAutoRenewing,
        signature,
        originalJson,
        presentedOfferingIdentifier?.let { PresentedOfferingContext(it) },
        storeUserID,
        purchaseType,
        marketplace,
        subscriptionOptionId,
        emptyMap(),
        replacementMode,
    )

    /**
     * Offering that was presented when making the purchase. Always null for restored purchases.
     */
    @Deprecated(
        "Use presentedOfferingContext",
        ReplaceWith("presentedOfferingContext.offeringIdentifier"),
    )
    public val presentedOfferingIdentifier: String?
        get() = presentedOfferingContext?.offeringIdentifier

    /**
     * Skus associated with the transaction
     */
    @IgnoredOnParcel
    @Deprecated(
        "Replaced with productIds",
        ReplaceWith("productIds"),
    )
    public val skus: List<String>
        get() = productIds

    public override fun equals(other: Any?): Boolean = other is StoreTransaction &&
        ComparableData(this) == ComparableData(other)
    public override fun hashCode(): Int = ComparableData(this).hashCode()
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
    val presentedOfferingContext: PresentedOfferingContext?,
    val storeUserID: String?,
    val purchaseType: PurchaseType,
    val marketplace: String?,
    val subscriptionOptionId: String?,
) {
    constructor(
        storeTransaction: StoreTransaction,
    ) : this(
        orderId = storeTransaction.orderId,
        productIds = storeTransaction.productIds,
        type = storeTransaction.type,
        purchaseTime = storeTransaction.purchaseTime,
        purchaseToken = storeTransaction.purchaseToken,
        purchaseState = storeTransaction.purchaseState,
        isAutoRenewing = storeTransaction.isAutoRenewing,
        signature = storeTransaction.signature,
        presentedOfferingContext = storeTransaction.presentedOfferingContext,
        storeUserID = storeTransaction.storeUserID,
        purchaseType = storeTransaction.purchaseType,
        marketplace = storeTransaction.marketplace,
        subscriptionOptionId = storeTransaction.subscriptionOptionId,
    )
}

public enum class PurchaseType {
    GOOGLE_PURCHASE,
    GOOGLE_RESTORED_PURCHASE,
    AMAZON_PURCHASE,
    GALAXY_PURCHASE,
}
