package com.revenuecat.checkpointtester.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.FlowResult

/**
 * One-line description of a checkpoint result, shared by the screens that only need to display what happened.
 * Null means nothing was presented; the logs have the reason.
 */
@OptIn(InternalRevenueCatAPI::class)
fun FlowResult?.summary(): String = when {
    this == null -> "Nothing presented."
    obtainedEntitlements.isNotEmpty() ->
        "Obtained ${obtainedEntitlements.joinToString { it.entitlementInfo.identifier }}."
    else -> "Flow finished without the user obtaining anything."
}
