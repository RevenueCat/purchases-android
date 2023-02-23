package com.revenuecat.purchases.models

import android.os.Parcelable
import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class GoogleStoreProduct(
    val productId: String,
    val basePlanId: String?,
    override val type: ProductType,
    override val price: Price,
    override val title: String,
    override val description: String,
    override val period: Period?,
    override val subscriptionOptions: List<GoogleSubscriptionOption>,
    override val defaultOption: GoogleSubscriptionOption?,
    val productDetails: @RawValue ProductDetails // TODO parcelize?
) : StoreProduct, Parcelable {

    override val id: String
        get() = basePlanId?.let {
            "$productId:$basePlanId"
        } ?: productId

    override val purchasingData: PurchasingData
        get() = if (type == ProductType.SUBS && defaultOption != null) {
            defaultOption.purchasingData
        } else {
            GooglePurchasingData.InAppProduct(
                id,
                productDetails
            )
        }

    /**
     * The sku of the StoreProduct
     */
    @IgnoredOnParcel
    @Deprecated(
        "Replaced with productId",
        ReplaceWith("productId")
    )
    override val sku: String
        get() = sku
}

val StoreProduct.googleProduct: GoogleStoreProduct?
    get() = this as? GoogleStoreProduct
