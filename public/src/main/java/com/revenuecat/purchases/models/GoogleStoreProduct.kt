package com.revenuecat.purchases.models

import android.os.Parcelable
import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class GoogleStoreProduct(
    override val productId: String,
    override val type: ProductType,
    override val oneTimeProductPrice: Price?,
    override val title: String,
    override val description: String,
    override val subscriptionPeriod: String?,
    override val purchaseOptions: List<GooglePurchaseOption>,
    override val defaultOption: PurchaseOption?,
    val productDetails: @RawValue ProductDetails // TODO parcelize?
) : StoreProduct, Parcelable {

    override val purchaseInfo: GooglePurchaseInfo
        get() = GooglePurchaseInfo.NotSubscription(
                    productId,
                    type,
                    productDetails
                )

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
