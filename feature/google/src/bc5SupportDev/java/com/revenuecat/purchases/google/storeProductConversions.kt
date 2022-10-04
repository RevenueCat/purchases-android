package com.revenuecat.purchases.google

import android.os.Parcelable
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.StoreProductImpl
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.parceler.JSONObjectParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlinx.parcelize.TypeParceler
import org.json.JSONObject

fun SkuDetails.toStoreProduct() =
    StoreProductImpl(
        sku,
        type.toRevenueCatProductType(),
        price,
        priceAmountMicros,
        priceCurrencyCode,
        originalPrice,
        originalPriceAmountMicros,
        title,
        description,
        subscriptionPeriod.takeIf { it.isNotBlank() },
        freeTrialPeriod.takeIf { it.isNotBlank() },
        introductoryPrice.takeIf { it.isNotBlank() },
        introductoryPriceAmountMicros,
        introductoryPricePeriod.takeIf { it.isNotBlank() },
        introductoryPriceCycles,
        iconUrl,
        JSONObject(originalJson)
    )

fun ProductDetails.toStoreProduct(offerToken: String) =
    BC5StoreProduct(
        productId,
        ProductType.SUBS,
        "price",
        100,
        "USD",
        "originalPrice",
        100,
        title,
        description,
        "P1Y",
        "P1W",
        "introPrice",
        90,
        "P1M",
        1,
        "icon",
        JSONObject("{}"),
        this,
        offerToken
    )


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
    // TODO add PricingPhases so we don't have to dig in ProductDetails (which has many)
) : Parcelable, StoreProduct() {

}
