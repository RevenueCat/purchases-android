package com.revenuecat.purchases.models

import android.os.Parcelable
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.parceler.JSONObjectParceler
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.TypeParceler
import org.json.JSONObject

@Parcelize
@TypeParceler<JSONObject, JSONObjectParceler>()
data class ProductDetails(
    val sku: String,
    val type: ProductType,
    val price: String, // For example $3.00

    val priceAmountMicros: Long,
    val priceCurrencyCode: String,

    // Null for amazon
    val originalPrice: String?,

    // 0 for amazon
    val originalPriceAmountMicros: Long,

    val title: String,

    val description: String,

    // Null if no subscriptionPeriod. Null for amazon
    val subscriptionPeriod: String?,

    // Null if no freeTrialPeriod. Null for amazon
    val freeTrialPeriod: String?,

    // Null if no introductoryPrice. Null for amazon
    val introductoryPrice: String?,

    // 0 for no intro price and amazon
    val introductoryPriceAmountMicros: Long,

    // Null if no introductoryPricePeriod. Null for amazon
    val introductoryPricePeriod: String?,

    // Null if no introductoryPriceCycles. 0 for amazon
    val introductoryPriceCycles: Int,

    val iconUrl: String,

    val originalJson: JSONObject
) : Parcelable {

    @SuppressWarnings("ComplexMethod")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProductDetails

        if (sku != other.sku) return false
        if (type != other.type) return false
        if (price != other.price) return false
        if (priceAmountMicros != other.priceAmountMicros) return false
        if (priceCurrencyCode != other.priceCurrencyCode) return false
        if (originalPrice != other.originalPrice) return false
        if (originalPriceAmountMicros != other.originalPriceAmountMicros) return false
        if (title != other.title) return false
        if (description != other.description) return false
        if (subscriptionPeriod != other.subscriptionPeriod) return false
        if (freeTrialPeriod != other.freeTrialPeriod) return false
        if (introductoryPrice != other.introductoryPrice) return false
        if (introductoryPriceAmountMicros != other.introductoryPriceAmountMicros) return false
        if (introductoryPricePeriod != other.introductoryPricePeriod) return false
        if (introductoryPriceCycles != other.introductoryPriceCycles) return false
        if (iconUrl != other.iconUrl) return false

        return true
    }
}
