package com.revenuecat.purchases.galaxy.conversions

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.galaxy.GalaxyStrings
import com.revenuecat.purchases.galaxy.logging.LogIntent
import com.revenuecat.purchases.galaxy.logging.log
import com.revenuecat.purchases.models.StoreReplacementMode
import com.samsung.android.sdk.iap.lib.constants.HelperDefine

private val galaxyReplacementModeMappings: Map<StoreReplacementMode, HelperDefine.ProrationMode> = mapOf(
    StoreReplacementMode.WITHOUT_PRORATION to HelperDefine.ProrationMode.INSTANT_NO_PRORATION,
    StoreReplacementMode.WITH_TIME_PRORATION to HelperDefine.ProrationMode.INSTANT_PRORATED_DATE,
    StoreReplacementMode.CHARGE_PRORATED_PRICE to HelperDefine.ProrationMode.INSTANT_PRORATED_CHARGE,
    StoreReplacementMode.DEFERRED to HelperDefine.ProrationMode.DEFERRED,
)

@Throws(PurchasesException::class)
internal fun StoreReplacementMode.toGalaxyReplacementMode(): HelperDefine.ProrationMode {
    return galaxyReplacementModeMappings[this] ?: run {
        val message = GalaxyStrings.CHARGE_FULL_PRICE_NOT_SUPPORTED
        log(LogIntent.GALAXY_ERROR) { message }
        throw PurchasesException(
            PurchasesError(
                PurchasesErrorCode.UnsupportedError,
                message,
            ),
        )
    }
}
