package com.revenuecat.checkpointtester.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.checkpoints.CheckpointPaywallOutcome
import com.revenuecat.purchases.checkpoints.CheckpointResult

/**
 * Display-ready form of a [CheckpointResult], shared by every screen and by [CheckpointEventLog].
 *
 * [grantedAccess] is the only interpretation the SDK doesn't make for us: it's what gating screens branch on.
 */
data class CheckpointResultUi(
    val title: String,
    val detail: String,
    val raw: String,
    val isError: Boolean = false,
    val grantedAccess: Boolean = false,
) {
    val summary: String
        get() = if (detail.isEmpty()) title else "$title · $detail"
}

@OptIn(InternalRevenueCatAPI::class)
internal fun CheckpointResult.toUi(): CheckpointResultUi = when (this) {
    is CheckpointResult.PaywallPresented -> CheckpointResultUi(
        title = "Paywall presented",
        detail = paywallOutcome.describe(),
        raw = toString(),
        grantedAccess = paywallOutcome is CheckpointPaywallOutcome.Purchased ||
            paywallOutcome is CheckpointPaywallOutcome.Restored,
    )
    is CheckpointResult.NoAction -> CheckpointResultUi(
        title = "No action",
        detail = "Reason: ${reason.value}",
        raw = toString(),
    )
    else -> CheckpointResultUi(
        title = "Unknown result",
        detail = "",
        raw = toString(),
    )
}

internal fun PurchasesException.toCheckpointResultUi(): CheckpointResultUi = CheckpointResultUi(
    title = "Error",
    detail = "$code: $message",
    raw = error.toString(),
    isError = true,
)

@OptIn(InternalRevenueCatAPI::class)
internal fun CheckpointPaywallOutcome.describe(): String = when (this) {
    is CheckpointPaywallOutcome.Purchased -> "Purchased"
    is CheckpointPaywallOutcome.Restored -> "Restored"
    is CheckpointPaywallOutcome.Error -> "Paywall error: ${error.message}"
    CheckpointPaywallOutcome.Dismissed -> "Dismissed"
    else -> toString()
}
