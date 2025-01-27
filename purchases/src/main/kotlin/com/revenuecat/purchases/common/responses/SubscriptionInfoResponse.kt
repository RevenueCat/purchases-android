package com.revenuecat.purchases.common.responses

import com.revenuecat.purchases.OwnershipType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.utils.serializers.ISO8601DateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
@SuppressWarnings("LongParameterList")
internal class SubscriptionInfoResponse(
    @SerialName("purchase_date") @Serializable(with = ISO8601DateSerializer::class)
    val purchaseDate: Date,
    @SerialName("original_purchase_date") @Serializable(with = ISO8601DateSerializer::class)
    val originalPurchaseDate: Date?,
    @SerialName("expires_date") @Serializable(with = ISO8601DateSerializer::class)
    val expiresDate: Date?,
    @SerialName("store")
    val store: Store,
    @SerialName("is_sandbox")
    val isSandbox: Boolean,
    @SerialName("unsubscribe_detected_at") @Serializable(with = ISO8601DateSerializer::class)
    val unsubscribeDetectedAt: Date?,
    @SerialName("billing_issues_detected_at") @Serializable(with = ISO8601DateSerializer::class)
    val billingIssuesDetectedAt: Date?,
    @SerialName("grace_period_expires_date") @Serializable(with = ISO8601DateSerializer::class)
    val gracePeriodExpiresDate: Date?,
    @SerialName("ownership_type")
    val ownershipType: OwnershipType = OwnershipType.UNKNOWN,
    @SerialName("period_type")
    val periodType: PeriodType,
    @SerialName("refunded_at") @Serializable(with = ISO8601DateSerializer::class)
    val refundedAt: Date?,
    @SerialName("store_transaction_id")
    val storeTransactionId: String?,
) {

    override fun toString(): String {
        return """
            SubscriptionInfo {
                purchaseDate: $purchaseDate,
                originalPurchaseDate: $originalPurchaseDate,
                expiresDate: $expiresDate,
                store: $store,
                isSandbox: $isSandbox,
                unsubscribeDetectedAt: $unsubscribeDetectedAt,
                billingIssuesDetectedAt: $billingIssuesDetectedAt,
                gracePeriodExpiresDate: $gracePeriodExpiresDate,
                ownershipType: $ownershipType,
                periodType: $periodType,
                refundedAt: $refundedAt,
                storeTransactionId: $storeTransactionId,
            }
        """.trimIndent()
    }
}
