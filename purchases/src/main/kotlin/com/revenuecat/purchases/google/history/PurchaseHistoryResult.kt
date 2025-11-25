package com.revenuecat.purchases.google.history

internal data class PurchaseHistoryResult(
    val responseCode: Int,
    val records: List<PurchaseHistoryRecord>,
    val continuationToken: String?,
) {
    fun isSuccess() = responseCode == BillingConstants.BILLING_RESPONSE_RESULT_OK

    fun getResponseCodeString() = BillingConstants.getResponseCodeString(responseCode)
}
