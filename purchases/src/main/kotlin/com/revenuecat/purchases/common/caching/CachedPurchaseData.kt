package com.revenuecat.purchases.common.caching

import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.paywalls.events.PaywallPostReceiptData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Contains ephemeral data associated with a purchase that may be lost during retry attempts.
 * This data is cached before posting receipts and retrieved when retrying failed posts.
 */
@Serializable
internal data class CachedPurchaseData(
    @SerialName("receipt_info")
    val receiptInfo: ReceiptInfo,

    @SerialName("paywall_data")
    val paywallPostReceiptData: PaywallPostReceiptData? = null,

    @SerialName("observer_mode")
    val observerMode: Boolean? = null,

    @SerialName("schema_version")
    val schemaVersion: Int = SCHEMA_VERSION,
) {
    companion object {
        const val SCHEMA_VERSION = 1

        fun from(
            receiptInfo: ReceiptInfo,
            paywallPostReceiptData: PaywallPostReceiptData?,
            observerMode: Boolean,
        ): CachedPurchaseData {
            return CachedPurchaseData(
                receiptInfo = receiptInfo,
                paywallPostReceiptData = paywallPostReceiptData,
                observerMode = observerMode,
            )
        }
    }
}
