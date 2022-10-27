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
    override val storeProductId: String,
    override val type: ProductType,
    override val oneTimeProductPrice: Price?
    override val title: String,
    override val description: String,
    override val subscriptionPeriod: String?,
    val freeTrialPeriod: String?,
    val introductoryPrice: String?,
    val introductoryPriceAmountMicros: Long,
    val introductoryPricePeriod: String?,
    val introductoryPriceCycles: Int,
    val iconUrl: String,
    val originalJson: JSONObject,
    val originalPrice: String?,
    val originalPriceAmountMicros: Long,
    val originalJson: JSONObject,
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
        if (type == ProductType.INAPP) Price(price, priceAmountMicros, priceCurrencyCode) else null,
        title,
        description,
        subscriptionPeriod.takeIf { it.isNotBlank() },
        freeTrialPeriod.takeIf { it.isNotBlank() },
        iconUrl,
        JSONObject(originalJson),
        this
    )