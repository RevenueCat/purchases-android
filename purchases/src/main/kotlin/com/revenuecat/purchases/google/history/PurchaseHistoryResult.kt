package com.revenuecat.purchases.google.history

import com.android.billingclient.api.BillingClient
import com.revenuecat.purchases.google.getBillingResponseCodeName

internal data class PurchaseHistoryResult(
    val responseCode: Int,
    val records: List<PurchaseHistoryRecord>,
    val continuationToken: String?,
) {
    fun isSuccess() = responseCode == BillingClient.BillingResponseCode.OK

    fun getResponseCodeString() = responseCode.getBillingResponseCodeName()
}
