package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.models.GoogleReplacementMode
import com.revenuecat.purchases.paywalls.components.common.serializers.DowngradeReplacementModeDeserializer
import com.revenuecat.purchases.paywalls.components.common.serializers.UpgradeReplacementModeDeserializer
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
    @Serializable(with = UpgradeReplacementModeDeserializer::class)
    @SerialName("upgrade_replacement_mode")
    val upgradeReplacementMode: GoogleReplacementMode = GoogleReplacementMode.CHARGE_PRORATED_PRICE,

    /**
     * Replacement mode to use for downgrades (moving to a lower price per unit time).
     * Defaults to DEFERRED.
     */
    @get:JvmSynthetic
    @Serializable(with = DowngradeReplacementModeDeserializer::class)
    @SerialName("downgrade_replacement_mode")
    val downgradeReplacementMode: GoogleReplacementMode = GoogleReplacementMode.DEFERRED,
)
