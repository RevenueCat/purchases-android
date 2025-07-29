package com.revenuecat.purchases.common.networking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class WebBillingProductsResponse(
    @SerialName("product_details") val productDetails: List<WebBillingProductResponse>,
)

@Serializable
internal data class WebBillingProductResponse(
    val identifier: String,
    @SerialName("product_type") val productType: String,
    val title: String,
    val description: String? = null,
    @SerialName("default_purchase_option_id") val defaultPurchaseOptionId: String? = null,
    @SerialName("purchase_options") val purchaseOptions: Map<String, WebBillingPurchaseOption>,
)

@Serializable
internal data class WebBillingPurchaseOption(
    @SerialName("base_price") val basePrice: WebBillingPrice? = null,
    val base: WebBillingPhase? = null,
    val trial: WebBillingPhase? = null,
    @SerialName("intro_price") val introPrice: WebBillingPhase? = null,
)

@Serializable
internal data class WebBillingPhase(
    val price: WebBillingPrice? = null,
    @SerialName("period_duration") val periodDuration: String? = null,
    @SerialName("cycle_count") val cycleCount: Int = 1,
)

@Serializable
internal data class WebBillingPrice(
    @SerialName("amount_micros") val amountMicros: Long,
    val currency: String,
)
