package com.revenuecat.purchases.common.caching

import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.paywalls.events.PaywallPostReceiptData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Contains ephemeral data associated with purchases that may be lost during retry attempts.
 * This data is cached before posting receipts and cleared upon a successful post attempt.
 */
@Serializable
internal data class LocalTransactionMetadata(
    @SerialName("token")
    public val token: String,

    @SerialName("receipt_info")
    public val receiptInfo: ReceiptInfo,

    @SerialName("paywall_data")
    public val paywallPostReceiptData: PaywallPostReceiptData? = null,

    @SerialName("purchases_are_completed_by")
    public val purchasesAreCompletedBy: PurchasesAreCompletedBy,
)
