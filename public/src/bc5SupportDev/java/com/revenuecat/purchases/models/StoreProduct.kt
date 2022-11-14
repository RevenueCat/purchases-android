package com.revenuecat.purchases.models

import android.os.Parcelable
import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.parceler.JSONObjectParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlinx.parcelize.TypeParceler
import org.json.JSONObject

@Parcelize
@TypeParceler<JSONObject, JSONObjectParceler>()
data class StoreProduct(
    val sku: String,
    val type: ProductType,
    val price: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val originalPrice: String?,
    val originalPriceAmountMicros: Long,
    val title: String,
    val description: String,
    val subscriptionPeriod: String?,
    val freeTrialPeriod: String?,
    val introductoryPrice: String?,
    val introductoryPriceAmountMicros: Long,
    val introductoryPricePeriod: String?,
    val introductoryPriceCycles: Int,
    val iconUrl: String,
    val originalJson: JSONObject,
    val pricingPhases: List<PricingPhase>,
    val productDetails: @RawValue ProductDetails?,
    val offerToken: String?,
    val basePlan: String?,
) : Parcelable {
    override fun toString(): String {
        return "[" + listOf(title, description, sku, price).joinToString() + "]"
    }

    // We use this to not include the originalJSON in the equals
    override fun equals(other: Any?) = other is StoreProduct && ComparableData(this) == ComparableData(other)
    override fun hashCode() = ComparableData(this).hashCode()
}

@Parcelize
data class PricingPhase(
    val billingPeriod: String,
    val billingCycleCount: Int,
    val formattedPrice: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val recurrenceMode: Int,
) : Parcelable {
    companion object {
        const val FINITE_RECURRING = 2
        const val INFINITE_RECURRING = 1
        const val NON_RECURRING = 3

        const val FORMATTED_PRICE_FREE = "Free"
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "billingPeriod" to this.billingPeriod,
            "billingCycleCount" to this.billingCycleCount,
            "formattedPrice" to this.formattedPrice,
            "priceAmountMicros" to this.priceAmountMicros,
            "priceCurrencyCode" to this.priceCurrencyCode,
            "recurrenceMode" to this.recurrenceMode
        )
    }
}

private data class ComparableData(
    val sku: String,
    val type: ProductType,
    val price: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val originalPrice: String?,
    val originalPriceAmountMicros: Long,
    val title: String,
    val description: String,
    val subscriptionPeriod: String?,
    val freeTrialPeriod: String?,
    val introductoryPrice: String?,
    val introductoryPriceAmountMicros: Long,
    val introductoryPricePeriod: String?,
    val introductoryPriceCycles: Int,
    val iconUrl: String
) {
    constructor(
        storeProduct: StoreProduct
    ) : this(
        sku = storeProduct.sku,
        type = storeProduct.type,
        price = storeProduct.price,
        priceAmountMicros = storeProduct.priceAmountMicros,
        priceCurrencyCode = storeProduct.priceCurrencyCode,
        originalPrice = storeProduct.originalPrice,
        originalPriceAmountMicros = storeProduct.originalPriceAmountMicros,
        title = storeProduct.title,
        description = storeProduct.description,
        subscriptionPeriod = storeProduct.subscriptionPeriod,
        freeTrialPeriod = storeProduct.freeTrialPeriod,
        introductoryPrice = storeProduct.introductoryPrice,
        introductoryPriceAmountMicros = storeProduct.introductoryPriceAmountMicros,
        introductoryPricePeriod = storeProduct.introductoryPricePeriod,
        introductoryPriceCycles = storeProduct.introductoryPriceCycles,
        iconUrl = storeProduct.iconUrl
    )
}

