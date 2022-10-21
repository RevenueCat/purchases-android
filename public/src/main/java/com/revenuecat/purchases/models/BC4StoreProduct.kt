package com.revenuecat.purchases.models

import android.os.Parcelable
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.ComparableData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.parceler.JSONObjectParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlinx.parcelize.TypeParceler
import org.json.JSONObject

@Parcelize
@TypeParceler<JSONObject, JSONObjectParceler>()
private data class BC4StoreProduct(
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
    val skuDetails: @RawValue SkuDetails // TODO figure out parcelization needs
) : StoreProduct, Parcelable {


    // We use this to not include the originalJSON in the equals
    override fun equals(other: Any?) = other is StoreProduct && ComparableData(this) == ComparableData(other)
    override fun hashCode() = ComparableData(this).hashCode()
}

val StoreProduct.skuDetails: SkuDetails?
    get() = (this as? BC4StoreProduct)?.skuDetails

private fun SkuDetails.toStoreProduct() =
    BC4StoreProduct(
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
        JSONObject(originalJson),
        this
    )