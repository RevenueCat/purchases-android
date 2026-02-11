package com.revenuecat.purchases.common.networking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class WebBillingProductsResponse(
    @SerialName("product_details") public val productDetails: List<WebBillingProductResponse>,
)

@Serializable
internal data class WebBillingProductResponse(
    public val identifier: String,
    @SerialName("product_type") public val productType: String,
    public val title: String,
    public val description: String? = null,
    @SerialName("default_purchase_option_id") public val defaultPurchaseOptionId: String? = null,
    @SerialName("purchase_options") public val purchaseOptions: Map<String, WebBillingPurchaseOption>,
)

@Serializable
internal data class WebBillingPurchaseOption(
    @SerialName("base_price") public val basePrice: WebBillingPrice? = null,
    public val base: WebBillingPhase? = null,
    public val trial: WebBillingPhase? = null,
    @SerialName("intro_price") public val introPrice: WebBillingPhase? = null,
)

@Serializable
internal data class WebBillingPhase(
    public val price: WebBillingPrice? = null,
    @SerialName("period_duration") public val periodDuration: String? = null,
    @SerialName("cycle_count") public val cycleCount: Int = 1,
)

@Serializable
internal data class WebBillingPrice(
    @SerialName("amount_micros") public val amountMicros: Long,
    public val currency: String,
)
