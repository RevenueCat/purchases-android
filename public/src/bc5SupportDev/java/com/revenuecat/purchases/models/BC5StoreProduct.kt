package com.revenuecat.purchases.models

import android.os.Parcelable
import com.android.billingclient.api.ProductDetails
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
private data class BC5StoreProduct(
    override val storeProductId: String,
    override val type: ProductType,
    override val price: String,
    override val priceAmountMicros: Long,
    override val priceCurrencyCode: String,
    override val title: String,
    override val description: String,
    override val subscriptionPeriod: String?,
    val productDetails: @RawValue ProductDetails,
    val purchaseOptions: List<PurchaseOption>
) : StoreProduct, Parcelable