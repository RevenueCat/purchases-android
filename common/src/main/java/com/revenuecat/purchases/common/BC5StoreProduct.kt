package com.revenuecat.purchases.common

import android.os.Parcelable
import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.parceler.JSONObjectParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlinx.parcelize.TypeParceler
import org.json.JSONObject

@Parcelize
@TypeParceler<JSONObject, JSONObjectParceler>()
data class BC5StoreProduct(
    override val sku: String,
    override val type: ProductType,
    override val price: String,
    override val priceAmountMicros: Long,
    override val priceCurrencyCode: String,
    override val originalPrice: String?,
    override val originalPriceAmountMicros: Long,
    override val title: String,
    override val description: String,
    override val subscriptionPeriod: String?,
    override val freeTrialPeriod: String?,
    override val introductoryPrice: String?,
    override val introductoryPriceAmountMicros: Long,
    override val introductoryPricePeriod: String?,
    override val introductoryPriceCycles: Int,
    override val iconUrl: String,
    override val originalJson: JSONObject,
    val productDetails: @RawValue ProductDetails?,
    val offerToken: String?,
    val pricingPhases: @RawValue ProductDetails.PricingPhases,
) : Parcelable, StoreProduct() {
}
