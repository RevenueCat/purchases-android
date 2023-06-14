package com.revenuecat.purchases

import android.os.Parcelable
import com.revenuecat.purchases.models.RawDataContainer
import com.revenuecat.purchases.utils.JSONObjectParceler
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.json.JSONObject
import java.util.Date

/**
 * This object gives you access to all of the information about the status of a user's entitlements.
 * @property identifier The entitlement identifier configured in the RevenueCat dashboard.
 * @property isActive True if the user has access to this entitlement.
 * @property willRenew True if the underlying subscription is set to renew at the end of the billing
 * period (expirationDate). Will always be True if entitlement is for lifetime access.
 * @property periodType The last period type this entitlement was in Either: NORMAL, INTRO or TRIAL.
 * @property latestPurchaseDate The latest purchase or renewal date for the entitlement.
 * @property originalPurchaseDate The first date this entitlement was purchased.
 * @property expirationDate The expiration date for the entitlement, can be `null` for lifetime
 * access. If the `periodType` is `TRIAL`, this is the trial expiration date.
 * @property store The store where this entitlement was unlocked from. Either: APP_STORE,
 * MAC_APP_STORE, PLAY_STORE, STRIPE, PROMOTIONAL or UNKNOWN_STORE.
 * @property productIdentifier The product identifier that unlocked this entitlement.
 * For Google subscriptions, this is the subscription ID.
 * For Amazon subscriptions, this is the termSku.
 * For INAPP purchases, this is simply the productId.
 * @property productPlanIdentifier The base plan identifier that unlocked this entitlement (Google only).
 * @property isSandbox False if this entitlement is unlocked via a production purchase.
 * @property unsubscribeDetectedAt The date an unsubscribe was detected. Can be `null`.
 * Note: Entitlement may still be active even if user has unsubscribed. Check the `isActive` property.
 * @property billingIssueDetectedAt The date a billing issue was detected. Can be `null` if there is
 * no billing issue or an issue has been resolved. Note: Entitlement may still be active even if
 * there is a billing issue. Check the `isActive` property.
 * @property verification If entitlement verification was enabled, the result of that verification.
 * If not, [VerificationResult.NOT_REQUESTED]
 */
@Parcelize
@TypeParceler<JSONObject, JSONObjectParceler>()
data class EntitlementInfo(
    val identifier: String,
    val isActive: Boolean,
    val willRenew: Boolean,
    val periodType: PeriodType,
    val latestPurchaseDate: Date,
    val originalPurchaseDate: Date,
    val expirationDate: Date?,
    val store: Store,
    val productIdentifier: String,
    val productPlanIdentifier: String?,
    val isSandbox: Boolean,
    val unsubscribeDetectedAt: Date?,
    val billingIssueDetectedAt: Date?,
    val ownershipType: OwnershipType,
    private val jsonObject: JSONObject,
    @ExperimentalPreviewRevenueCatPurchasesAPI
    val verification: VerificationResult = VerificationResult.NOT_REQUESTED,
) : Parcelable, RawDataContainer<JSONObject> {

    @Deprecated(
        "Use the constructor with the verification parameter",
        ReplaceWith(
            "EntitlementInfo(identifier, isActive, willRenew, periodType, latestPurchaseDate, " +
                "originalPurchaseDate, expirationDate, store, productIdentifier, productPlanIdentifier, isSandbox, " +
                "unsubscribeDetectedAt, billingIssueDetectedAt, ownershipType, jsonObject, " +
                "VerificationResult.NOT_REQUESTED)",
            "com.revenuecat.purchases.VerificationResult",
        ),
    )
    constructor(
        identifier: String,
        isActive: Boolean,
        willRenew: Boolean,
        periodType: PeriodType,
        latestPurchaseDate: Date,
        originalPurchaseDate: Date,
        expirationDate: Date?,
        store: Store,
        productIdentifier: String,
        productPlanIdentifier: String?,
        isSandbox: Boolean,
        unsubscribeDetectedAt: Date?,
        billingIssueDetectedAt: Date?,
        ownershipType: OwnershipType,
        jsonObject: JSONObject,
    ) : this(
        identifier,
        isActive,
        willRenew,
        periodType,
        latestPurchaseDate,
        originalPurchaseDate,
        expirationDate, store,
        productIdentifier,
        productPlanIdentifier,
        isSandbox,
        unsubscribeDetectedAt,
        billingIssueDetectedAt,
        ownershipType,
        jsonObject,
        VerificationResult.NOT_REQUESTED,
    )

    @IgnoredOnParcel
    override val rawData: JSONObject
        get() = jsonObject

    /** @suppress */
    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    override fun toString(): String {
        return "EntitlementInfo(" +
            "identifier='$identifier', " +
            "isActive=$isActive, " +
            "willRenew=$willRenew, " +
            "periodType=$periodType, " +
            "latestPurchaseDate=$latestPurchaseDate, " +
            "originalPurchaseDate=$originalPurchaseDate, " +
            "expirationDate=$expirationDate, " +
            "store=$store, " +
            "productIdentifier='$productIdentifier', " +
            "productPlanIdentifier='$productPlanIdentifier', " +
            "isSandbox=$isSandbox, " +
            "unsubscribeDetectedAt=$unsubscribeDetectedAt, " +
            "billingIssueDetectedAt=$billingIssueDetectedAt, " +
            "ownershipType=$ownershipType, " +
            "verification=$verification)"
    }

    /** @suppress */
    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EntitlementInfo

        if (identifier != other.identifier) return false
        if (isActive != other.isActive) return false
        if (willRenew != other.willRenew) return false
        if (periodType != other.periodType) return false
        if (latestPurchaseDate != other.latestPurchaseDate) return false
        if (originalPurchaseDate != other.originalPurchaseDate) return false
        if (expirationDate != other.expirationDate) return false
        if (store != other.store) return false
        if (productIdentifier != other.productIdentifier) return false
        if (productPlanIdentifier != other.productPlanIdentifier) return false
        if (isSandbox != other.isSandbox) return false
        if (unsubscribeDetectedAt != other.unsubscribeDetectedAt) return false
        if (billingIssueDetectedAt != other.billingIssueDetectedAt) return false
        if (ownershipType != other.ownershipType) return false
        if (verification != other.verification) return false

        return true
    }

    override fun hashCode(): Int {
        var result = identifier.hashCode()
        result = 31 * result + isActive.hashCode()
        result = 31 * result + willRenew.hashCode()
        result = 31 * result + periodType.hashCode()
        result = 31 * result + latestPurchaseDate.hashCode()
        result = 31 * result + originalPurchaseDate.hashCode()
        result = 31 * result + (expirationDate?.hashCode() ?: 0)
        result = 31 * result + store.hashCode()
        result = 31 * result + productIdentifier.hashCode()
        result = 31 * result + productPlanIdentifier.hashCode()
        result = 31 * result + isSandbox.hashCode()
        result = 31 * result + (unsubscribeDetectedAt?.hashCode() ?: 0)
        result = 31 * result + (billingIssueDetectedAt?.hashCode() ?: 0)
        result = 31 * result + ownershipType.hashCode()
        return result
    }
}

/**
 * Enum of supported stores
 */
enum class Store {
    /**
     * For entitlements granted via Apple App Store.
     */
    APP_STORE,

    /**
     * For entitlements granted via Apple Mac App Store.
     */
    MAC_APP_STORE,

    /**
     * For entitlements granted via Google Play Store.
     */
    PLAY_STORE,

    /**
     * For entitlements granted via Stripe.
     */
    STRIPE,

    /**
     * For entitlements granted via a promo in RevenueCat.
     */
    PROMOTIONAL,

    /**
     * For entitlements granted via an unknown store.
     */
    UNKNOWN_STORE,

    /**
     * For entitlements granted via Amazon store.
     */
    AMAZON,
}

/**
 * Enum of supported period types for an entitlement.
 */
enum class PeriodType {
    /**
     * If the entitlement is not under an introductory or trial period.
     */
    NORMAL,

    /**
     * If the entitlement is under a introductory price period.
     */
    INTRO,

    /**
     * If the entitlement is under a trial period.
     */
    TRIAL,
}

/**
 * Enum of supported ownership types for an entitlement.
 */
enum class OwnershipType {
    /**
     * The purchase was made directly by this user.
     */
    PURCHASED,

    /**
     * The purchase has been shared to this user by a family member.
     */
    FAMILY_SHARED,

    /**
     * The purchase has no or an unknown ownership type.
     */
    UNKNOWN,
}
