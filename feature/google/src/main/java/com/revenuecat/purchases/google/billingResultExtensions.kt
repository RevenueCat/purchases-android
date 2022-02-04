package com.revenuecat.purchases.google

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult

fun BillingResult.toHumanReadableDescription() =
    "DebugMessage: $debugMessage. ErrorCode: ${responseCode.getBillingResponseCodeName()}."

fun BillingResult.isSuccessful() = responseCode == BillingClient.BillingResponseCode.OK
