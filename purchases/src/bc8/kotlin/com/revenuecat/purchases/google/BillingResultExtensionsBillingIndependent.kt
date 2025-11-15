package com.revenuecat.purchases.google

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult

internal fun BillingResult.toHumanReadableDescription() =
    "DebugMessage: $debugMessage. " +
        "ErrorCode: ${responseCode.getBillingResponseCodeName()}. " +
        "SubResponseCode: ${onPurchasesUpdatedSubResponseCode.getOnPurchasesUpdatedSubResponseCodeName()}."

@Suppress("MaximumLineLength", "MaxLineLength")
private fun @receiver:BillingClient.OnPurchasesUpdatedSubResponseCode Int.getOnPurchasesUpdatedSubResponseCodeName() = when (this) {
    BillingClient.OnPurchasesUpdatedSubResponseCode.NO_APPLICABLE_SUB_RESPONSE_CODE ->
        "NO_APPLICABLE_SUB_RESPONSE_CODE"
    BillingClient.OnPurchasesUpdatedSubResponseCode.PAYMENT_DECLINED_DUE_TO_INSUFFICIENT_FUNDS ->
        "PAYMENT_DECLINED_DUE_TO_INSUFFICIENT_FUNDS"
    BillingClient.OnPurchasesUpdatedSubResponseCode.USER_INELIGIBLE -> "USER_INELIGIBLE"
    else -> "UNKNOWN_SUB_RESPONSE_CODE ($this)"
}
