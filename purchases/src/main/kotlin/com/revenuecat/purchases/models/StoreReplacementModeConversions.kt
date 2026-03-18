package com.revenuecat.purchases.models

import com.android.billingclient.api.BillingFlowParams
import com.revenuecat.purchases.ReplacementMode
import com.revenuecat.purchases.Store

private data class StoreReplacementModeMapping(
    @get:BillingFlowParams.SubscriptionUpdateParams.ReplacementMode
    val playBillingClientMode: Int,
    val legacyPlayBackendName: String,
    val galaxyBackendName: String?,
    val googleReplacementMode: GoogleReplacementMode,
)

// This mapping table doesn't include the Galaxy mappings from StoreReplacementMode -> HelperDefine.ProrationMode
// because the Galaxy SDK isn't available in the main purchases module. Those conversions are located in the galaxy
// module's StoreReplacementModeConversions.kt file.
private val StoreReplacementMode.mapping: StoreReplacementModeMapping
    get() = when (this) {
        StoreReplacementMode.WITHOUT_PRORATION -> StoreReplacementModeMapping(
            playBillingClientMode = BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITHOUT_PRORATION,
            legacyPlayBackendName = "IMMEDIATE_WITHOUT_PRORATION",
            galaxyBackendName = "INSTANT_NO_PRORATION",
            googleReplacementMode = GoogleReplacementMode.WITHOUT_PRORATION,
        )
        StoreReplacementMode.WITH_TIME_PRORATION -> StoreReplacementModeMapping(
            playBillingClientMode = BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION,
            legacyPlayBackendName = "IMMEDIATE_WITH_TIME_PRORATION",
            galaxyBackendName = "INSTANT_PRORATED_DATE",
            googleReplacementMode = GoogleReplacementMode.WITH_TIME_PRORATION,
        )
        StoreReplacementMode.CHARGE_FULL_PRICE -> StoreReplacementModeMapping(
            playBillingClientMode = BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE,
            legacyPlayBackendName = "IMMEDIATE_AND_CHARGE_FULL_PRICE",
            galaxyBackendName = null,
            googleReplacementMode = GoogleReplacementMode.CHARGE_FULL_PRICE,
        )
        StoreReplacementMode.CHARGE_PRORATED_PRICE -> StoreReplacementModeMapping(
            playBillingClientMode = BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_PRORATED_PRICE,
            legacyPlayBackendName = "IMMEDIATE_AND_CHARGE_PRORATED_PRICE",
            galaxyBackendName = "INSTANT_PRORATED_CHARGE",
            googleReplacementMode = GoogleReplacementMode.CHARGE_PRORATED_PRICE,
        )
        StoreReplacementMode.DEFERRED -> StoreReplacementModeMapping(
            playBillingClientMode = BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.DEFERRED,
            legacyPlayBackendName = "DEFERRED",
            galaxyBackendName = "DEFERRED",
            googleReplacementMode = GoogleReplacementMode.DEFERRED,
        )
    }

@BillingFlowParams.SubscriptionUpdateParams.ReplacementMode
internal fun StoreReplacementMode.toPlayBillingClientMode(): Int {
    return mapping.playBillingClientMode
}

internal fun StoreReplacementMode.legacyPlayBackendName(): String {
    return mapping.legacyPlayBackendName
}

internal fun StoreReplacementMode.storeBackendName(store: Store): String? {
    return when (store) {
        Store.PLAY_STORE -> legacyPlayBackendName()
        Store.GALAXY -> mapping.galaxyBackendName
        else -> null
    }
}

internal fun StoreReplacementMode.toGoogleReplacementMode(): GoogleReplacementMode {
    return mapping.googleReplacementMode
}

internal fun GoogleReplacementMode.toStoreReplacementMode(): StoreReplacementMode {
    return when (this) {
        GoogleReplacementMode.WITHOUT_PRORATION -> StoreReplacementMode.WITHOUT_PRORATION
        GoogleReplacementMode.WITH_TIME_PRORATION -> StoreReplacementMode.WITH_TIME_PRORATION
        GoogleReplacementMode.CHARGE_FULL_PRICE -> StoreReplacementMode.CHARGE_FULL_PRICE
        GoogleReplacementMode.CHARGE_PRORATED_PRICE -> StoreReplacementMode.CHARGE_PRORATED_PRICE
        GoogleReplacementMode.DEFERRED -> StoreReplacementMode.DEFERRED
    }
}

internal fun ReplacementMode?.toStoreReplacementModeOrNull(): StoreReplacementMode? {
    return when (this) {
        null -> null
        is StoreReplacementMode -> this
        is GoogleReplacementMode -> this.toStoreReplacementMode()
        else -> null
    }
}
