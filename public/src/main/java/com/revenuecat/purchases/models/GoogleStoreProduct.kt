package com.revenuecat.purchases.models

import android.os.Parcelable
import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class GoogleStoreProduct(
    override val productId: String,
    override val type: ProductType,
    override val oneTimeProductPrice: Price?,
    override val title: String,
    override val description: String,
    override val subscriptionPeriod: String?,
    override val subscriptionOptions: List<GoogleSubscriptionOption>,
    override val defaultOption: GoogleSubscriptionOption?
) : StoreProduct, Parcelable {

    override val purchasingData: PurchasingData
        get() = if (type == ProductType.SUBS && defaultOption != null) {
            defaultOption.purchasingData
        } else {
            GooglePurchasingData.InAppProduct(
                productId,
                productDetails
            )
        }

    // @IgnoredOnParcel isn't allowed in primary constructor without default value
    // so to avoid having to provide a default value, productDetails lives outside of the constructor
    // TODO maddie this works, but as a public class it is bug-prone. users cuold use the primary constructor
    // and then try to access productDetails
    constructor(
        productId: String,
        type: ProductType,
        oneTimeProductPrice: Price?,
        title: String,
        description: String,
        subscriptionPeriod: String?,
        subscriptionOptions: List<GoogleSubscriptionOption>,
        defaultOption: GoogleSubscriptionOption?,
        productDetails: ProductDetails
    ) :
        this(
            productId,
            type,
            oneTimeProductPrice,
            title,
            description,
            subscriptionPeriod,
            subscriptionOptions,
            defaultOption
        ) {
        this.productDetails = productDetails
    }

    @IgnoredOnParcel
    lateinit var productDetails: ProductDetails

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
