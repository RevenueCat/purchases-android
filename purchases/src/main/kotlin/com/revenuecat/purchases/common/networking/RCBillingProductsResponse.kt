package com.revenuecat.purchases.common.networking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RCBillingProductsResponse(
    @SerialName("product_details") val productDetails: List<RCBillingProductResponse>,
)

@Serializable
internal data class RCBillingProductResponse(
    val identifier: String,
    @SerialName("product_type") val productType: String,
    val title: String,
    val description: String? = null,
    @SerialName("default_purchase_option_id") val defaultPurchaseOptionId: String? = null,
    @SerialName("purchase_options") val purchaseOptions: Map<String, RCBillingPurchaseOption>,
)

@Serializable
internal data class RCBillingPurchaseOption(
    @SerialName("base_price") val basePrice: RCBillingPrice? = null,
    val base: RCBillingPhase? = null,
    val trial: RCBillingPhase? = null,
    @SerialName("intro_price") val introPrice: RCBillingPhase? = null,
)

@Serializable
internal data class RCBillingPhase(
    val price: RCBillingPrice? = null,
    @SerialName("period_duration") val periodDuration: String? = null,
    @SerialName("cycle_count") val cycleCount: Int? = null,
)

@Serializable
internal data class RCBillingPrice(
    @SerialName("amount_micros") val amountMicros: Long,
    val currency: String,
)
