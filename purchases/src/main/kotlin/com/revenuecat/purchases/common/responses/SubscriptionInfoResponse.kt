package com.revenuecat.purchases.common.responses

import com.revenuecat.purchases.OwnershipType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.common.Constants.MICRO_MULTIPLIER
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.utils.serializers.ISO8601DateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.NumberFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

@Serializable
@SuppressWarnings("LongParameterList")
internal data class SubscriptionInfoResponse(
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
    @SerialName("auto_resume_date") @Serializable(with = ISO8601DateSerializer::class)
    val autoResumeDate: Date?,
    @SerialName("display_name")
    val displayName: String?,
    @SerialName("price")
    val price: PriceResponse?,
    @SerialName("product_plan_identifier")
    val productPlanIdentifier: String?,
) {

    @Serializable
    internal data class PriceResponse(
        @SerialName("amount")
        val amount: Double,
        @SerialName("currency")
        val currencyCode: String,
    ) {

        @JvmSynthetic
        fun toPrice(locale: Locale): Price {
            val numberFormat = NumberFormat.getCurrencyInstance(locale)
            numberFormat.currency = Currency.getInstance(currencyCode)

            val formatted = numberFormat.format(amount)

            return Price(formatted, (amount * MICRO_MULTIPLIER).toLong(), currencyCode)
        }
    }
}
