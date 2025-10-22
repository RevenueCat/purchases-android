package com.revenuecat.purchases.google

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult

internal fun BillingResult.toHumanReadableDescription() =
    "DebugMessage: $debugMessage. ErrorCode: ${responseCode.getBillingResponseCodeName()}."

internal fun BillingResult.isSuccessful() = responseCode == BillingClient.BillingResponseCode.OK
