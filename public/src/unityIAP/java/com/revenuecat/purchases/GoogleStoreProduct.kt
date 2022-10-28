package com.revenuecat.purchases

import android.os.Parcelable
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.ComparableData
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PurchaseOption
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.parceler.JSONObjectParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlinx.parcelize.TypeParceler
import org.json.JSONObject

@Parcelize
@TypeParceler<JSONObject, JSONObjectParceler>()
data class GoogleStoreProduct(
    override val storeProductId: String,
    override val type: ProductType,
    override val oneTimeProductPrice: Price?,
    override val title: String,
    override val description: String,
    override val subscriptionPeriod: String?,
    override val purchaseOptions: List<PurchaseOption>,
    val freeTrialPeriod: String?,
    val introductoryPrice: String?,
    val introductoryPriceAmountMicros: Long,
    val introductoryPricePeriod: String?,
    val introductoryPriceCycles: Int,
    val iconUrl: String,
    val originalPrice: String?,
    val originalPriceAmountMicros: Long,
    val originalJson: JSONObject
) : StoreProduct, Parcelable {

    // We use this to not include the originalJSON in the equals
    override fun equals(other: Any?) = other is StoreProduct && ComparableData(this) == ComparableData(other)
    override fun hashCode() = ComparableData(this).hashCode()
}

val StoreProduct.skuDetails: SkuDetails?
    get() = SkuDetails((this as? GoogleStoreProduct)?.originalJson.toString())
