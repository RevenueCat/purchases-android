package com.revenuecat.purchases.google

import com.android.billingclient.api.BillingResult

internal fun BillingResult.toHumanReadableDescription() =
    "DebugMessage: $debugMessage. " +
        "ErrorCode: ${responseCode.getBillingResponseCodeName()}."
