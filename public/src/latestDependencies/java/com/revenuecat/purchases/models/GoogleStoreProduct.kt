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
data class GoogleStoreProduct(
    override val sku: String, // TODOBC5 rename?
    override val type: ProductType,
    override val oneTimeProductPrice: Price?,
    override val title: String,
    override val description: String,
    override val subscriptionPeriod: String?,
    override val purchaseOptions: List<PurchaseOption>,
    val productDetails: @RawValue ProductDetails // TODO parcelize?
) : StoreProduct, Parcelable

val StoreProduct.googleProduct: GoogleStoreProduct?
    get() = this as? GoogleStoreProduct
