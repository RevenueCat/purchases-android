package com.revenuecat.purchases.models

import android.os.Parcelable
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.parceler.JSONObjectParceler
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.TypeParceler
import org.json.JSONObject

@Parcelize
@TypeParceler<JSONObject, JSONObjectParceler>()
open class ProductDetails(
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
) : Parcelable

// TODO: add JvmName
val ProductDetails.skuDetails: SkuDetails
    get() = SkuDetails(this.originalJson.toString())
