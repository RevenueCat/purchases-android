package com.revenuecat.purchases.google.history

import com.android.billingclient.api.BillingClient
import com.revenuecat.purchases.google.getBillingResponseCodeName

internal data class PurchaseHistoryResult(
    public val responseCode: Int,
    public val records: List<PurchaseHistoryRecord>,
    public val continuationToken: String?,
) {
    public fun isSuccess() = responseCode == BillingClient.BillingResponseCode.OK

    public fun getResponseCodeString() = responseCode.getBillingResponseCodeName()
}
