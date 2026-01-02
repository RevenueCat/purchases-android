package com.revenuecat.purchases

import android.os.Parcelable
import com.revenuecat.purchases.models.GalaxyReplacementMode
import com.revenuecat.purchases.models.GoogleReplacementMode
import kotlinx.android.parcel.IgnoredOnParcel

/**
 * Contains information about the replacement mode to use in case of a product upgrade.
 * Use the platform specific subclasses in each implementation.
 * @property name Identifier of the proration mode to be used
 */
interface ReplacementMode : Parcelable {
    @IgnoredOnParcel
    val name: String
}

/**
 * [GoogleReplacementMode] used to be `GoogleProrationMode`. The backend still expects these values, hence this enum.
 */
private enum class LegacyProrationMode {
    IMMEDIATE_WITHOUT_PRORATION,
    IMMEDIATE_WITH_TIME_PRORATION,
    IMMEDIATE_AND_CHARGE_FULL_PRICE,
    IMMEDIATE_AND_CHARGE_PRORATED_PRICE,
    DEFERRED,
}

private val GoogleReplacementMode.asLegacyProrationMode: LegacyProrationMode
    get() = when (this) {
        GoogleReplacementMode.WITHOUT_PRORATION -> LegacyProrationMode.IMMEDIATE_WITHOUT_PRORATION
        GoogleReplacementMode.WITH_TIME_PRORATION -> LegacyProrationMode.IMMEDIATE_WITH_TIME_PRORATION
        GoogleReplacementMode.CHARGE_FULL_PRICE -> LegacyProrationMode.IMMEDIATE_AND_CHARGE_FULL_PRICE
        GoogleReplacementMode.CHARGE_PRORATED_PRICE -> LegacyProrationMode.IMMEDIATE_AND_CHARGE_PRORATED_PRICE
        GoogleReplacementMode.DEFERRED -> LegacyProrationMode.DEFERRED
    }

/**
 * Returns the backend name for this [ReplacementMode].
 * For [GoogleReplacementMode], this returns the legacy proration mode name.
 * For [GalaxyReplacementMode], this returns the enum name directly.
 */
internal val ReplacementMode.backendName: String
    get() = when (this) {
        is GoogleReplacementMode -> this.asLegacyProrationMode.name
        is GalaxyReplacementMode -> this.name
        else -> this.name
    }
