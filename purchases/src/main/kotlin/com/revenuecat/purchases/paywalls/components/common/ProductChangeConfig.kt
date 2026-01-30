package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.models.GoogleReplacementMode
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Configuration for product changes (upgrades/downgrades) in a paywall.
 *
 * This configuration is used when a user with an active subscription purchases a different
 * product that grants the same entitlement. Without this configuration, such purchases
 * result in parallel subscriptions.
 */
@InternalRevenueCatAPI
@Poko
@Serializable
class ProductChangeConfig(
    /**
     * Replacement mode to use for upgrades (moving to a higher price per unit time).
     * Defaults to CHARGE_PRORATED_PRICE as recommended by Google.
     */
    @get:JvmSynthetic
    @SerialName("upgrade_replacement_mode")
    val upgradeReplacementMode: SerializableReplacementMode = SerializableReplacementMode.CHARGE_PRORATED_PRICE,

    /**
     * Replacement mode to use for downgrades (moving to a lower price per unit time).
     * Defaults to DEFERRED.
     */
    @get:JvmSynthetic
    @SerialName("downgrade_replacement_mode")
    val downgradeReplacementMode: SerializableReplacementMode = SerializableReplacementMode.DEFERRED,
)

/**
 * Serializable replacement mode enum for paywall configuration.
 * Maps to [GoogleReplacementMode] for actual purchase operations.
 */
@InternalRevenueCatAPI
@Serializable
enum class SerializableReplacementMode {
    /**
     * Old subscription is cancelled, and new subscription takes effect immediately.
     * User is charged for the full price of the new subscription on the old subscription's
     * expiration date.
     */
    @SerialName("without_proration")
    WITHOUT_PRORATION,

    /**
     * Old subscription is cancelled, and new subscription takes effect immediately.
     * Any time remaining on the old subscription is used to push out the first payment date
     * for the new subscription.
     */
    @SerialName("with_time_proration")
    WITH_TIME_PRORATION,

    /**
     * Replacement takes effect immediately, and the user is charged full price of new plan
     * and is given a full billing cycle of subscription, plus remaining prorated time from
     * the old plan.
     */
    @SerialName("charge_full_price")
    CHARGE_FULL_PRICE,

    /**
     * Replacement takes effect immediately, and the billing cycle remains the same.
     * The price difference is charged immediately.
     * Only available for upgrades.
     */
    @SerialName("charge_prorated_price")
    CHARGE_PRORATED_PRICE,

    /**
     * Replacement takes effect when the old plan expires, and the new price will be charged
     * at the same time.
     */
    @SerialName("deferred")
    DEFERRED,
    ;

    /**
     * Converts this serializable replacement mode to the corresponding [GoogleReplacementMode].
     */
    @InternalRevenueCatAPI
    fun toGoogleReplacementMode(): GoogleReplacementMode = when (this) {
        WITHOUT_PRORATION -> GoogleReplacementMode.WITHOUT_PRORATION
        WITH_TIME_PRORATION -> GoogleReplacementMode.WITH_TIME_PRORATION
        CHARGE_FULL_PRICE -> GoogleReplacementMode.CHARGE_FULL_PRICE
        CHARGE_PRORATED_PRICE -> GoogleReplacementMode.CHARGE_PRORATED_PRICE
        DEFERRED -> GoogleReplacementMode.DEFERRED
    }
}
