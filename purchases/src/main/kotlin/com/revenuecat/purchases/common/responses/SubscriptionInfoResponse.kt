package com.revenuecat.purchases.common.responses

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.OwnershipType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.common.SharedConstants.MICRO_MULTIPLIER
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
    public val purchaseDate: Date,
    @SerialName("original_purchase_date") @Serializable(with = ISO8601DateSerializer::class)
    public val originalPurchaseDate: Date? = null,
    @SerialName("expires_date") @Serializable(with = ISO8601DateSerializer::class)
    public val expiresDate: Date? = null,
    @SerialName("store")
    public val store: Store,
    @SerialName("is_sandbox")
    public val isSandbox: Boolean,
    @SerialName("unsubscribe_detected_at") @Serializable(with = ISO8601DateSerializer::class)
    public val unsubscribeDetectedAt: Date? = null,
    @SerialName("billing_issues_detected_at") @Serializable(with = ISO8601DateSerializer::class)
    public val billingIssuesDetectedAt: Date? = null,
    @SerialName("grace_period_expires_date") @Serializable(with = ISO8601DateSerializer::class)
    public val gracePeriodExpiresDate: Date? = null,
    @SerialName("ownership_type")
    public val ownershipType: OwnershipType = OwnershipType.UNKNOWN,
    @SerialName("period_type")
    public val periodType: PeriodType,
    @SerialName("refunded_at") @Serializable(with = ISO8601DateSerializer::class)
    public val refundedAt: Date? = null,
    @SerialName("store_transaction_id")
    public val storeTransactionId: String? = null,
    @SerialName("auto_resume_date") @Serializable(with = ISO8601DateSerializer::class)
    public val autoResumeDate: Date? = null,
    @SerialName("display_name")
    public val displayName: String? = null,
    @SerialName("price")
    public val price: PriceResponse? = null,
    @SerialName("product_plan_identifier")
    public val productPlanIdentifier: String? = null,
    @SerialName("management_url")
    public val managementURL: String? = null,
) {

    @Serializable
    internal data class PriceResponse(
        @SerialName("amount")
        public val amount: Double,
        @SerialName("currency")
        public val currencyCode: String,
    ) {

        @OptIn(InternalRevenueCatAPI::class)
        @JvmSynthetic
        public fun toPrice(locale: Locale): Price {
            val numberFormat = NumberFormat.getCurrencyInstance(locale)
            numberFormat.currency = Currency.getInstance(currencyCode)

            val formatted = numberFormat.format(amount)

            return Price(formatted, (amount * MICRO_MULTIPLIER).toLong(), currencyCode)
        }
    }
}
