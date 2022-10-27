package com.revenuecat.purchases.models

import android.os.Parcelable
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.parceler.JSONObjectParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.json.JSONObject

/**
 * Represents an in-app product's or subscription's listing details.
 */
abstract class StoreProduct : Parcelable {
    /**
     * The product ID.
     */
    abstract val sku: String

    /**
     * Type of product. One of [ProductType].
     */
    abstract val type: ProductType

    /**
     * Formatted price of the item, including its currency sign. For example $3.00.
     */
    abstract val price: String

    /**
     * Price in micro-units, where 1,000,000 micro-units equal one unit of the currency.
     *
     * For example, if price is "€7.99", price_amount_micros is 7,990,000. This abstract value represents
     * the localized, rounded price for a particular currency.
     */
    abstract val priceAmountMicros: Long

    /**
     * Returns ISO 4217 currency code for price and original price.
     *
     * For example, if price is specified in British pounds sterling, price_currency_code is "GBP".
     *
     * If currency code cannot be determined, currency symbol is returned.
     */
    abstract val priceCurrencyCode: String

    /**
     * Formatted original price of the item, including its currency sign.
     *
     * Note: returned only for Google products. Not available for Amazon.
     */
    abstract val originalPrice: String?

    /**
     * Returns the original price in micro-units, where 1,000,000 micro-units equal one unit
     * of the currency.
     *
     * Note: returned only for Google products. Always 0 for Amazon subscriptions.
     */
    abstract val originalPriceAmountMicros: Long

    /**
     * Title of the product.
     */
    abstract val title: String

    /**
     * The description of the product.
     */
    abstract val description: String

    /**
     * Subscription period, specified in ISO 8601 format. For example, P1W equates to one week,
     * P1M equates to one month, P3M equates to three months, P6M equates to six months,
     * and P1Y equates to one year.
     *
     * Note: Returned only for Google subscriptions. Not available for Amazon.
     */
    abstract val subscriptionPeriod: String?

    /**
     * Subscription period, specified in ISO 8601 format. For example, P1W equates to one week,
     * P1M equates to one month, P3M equates to three months, P6M equates to six months,
     * and P1Y equates to one year.
     *
     * Note: Returned only for Google subscriptions. Not available for Amazon.
     */
    abstract val freeTrialPeriod: String?

    /**
     * The billing period of the introductory price, specified in ISO 8601 format.
     *
     * Note: Returned only for Google subscriptions which have an introductory period configured.
     * Not available for Amazon.
     */
    abstract val introductoryPrice: String?

    /**
     * Introductory price in micro-units. The currency is the same as price_currency_code.
     *
     * Note: Returns 0 if the product is not Google a subscription or doesn't
     * have an introductory period. Always 0 for Amazon subscriptions.
     */
    abstract val introductoryPriceAmountMicros: Long

    /**
     * The billing period of the introductory price, specified in ISO 8601 format.
     *
     * Note: Returned only for Google subscriptions which have an introductory period configured.
     * Not available for Amazon.
     */
    abstract val introductoryPricePeriod: String?

    /**
     * The number of subscription billing periods for which the user will be given the
     * introductory price, such as 3.
     *
     * Note: Returns 0 if the SKU is not a Google subscription or doesn't
     * have an introductory period. Always 0 for Amazon subscriptions.
     */
    abstract val introductoryPriceCycles: Int

    /**
     * The icon of the product if present.
     */
    abstract val iconUrl: String

    /**
     * JSONObject representing the original product class from Google or Amazon.
     *
     * Note: there's a convenience extension property that can be used to get the original
     * SkuDetails class: `StoreProduct.skuDetails`.
     * Alternatively, the original SkuDetails can be built doing the following:
     * `SkuDetails(this.originalJson.toString())`
     *
     * For Amazon, the original Product can be obtained using `StoreProduct.amazonProduct`
     */
    abstract val originalJson: JSONObject

    // We use this to not include the originalJSON in the equals
    override fun equals(other: Any?) = other is StoreProduct && ComparableData(this) == ComparableData(other)
    override fun hashCode() = ComparableData(this).hashCode()
}

@Parcelize
@TypeParceler<JSONObject, JSONObjectParceler>()
data class StoreProductImpl(
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
    override val originalJson: JSONObject
) : StoreProduct(), Parcelable {}

private data class ComparableData(
    val sku: String,
    val type: ProductType,
    val price: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val originalPrice: String?,
    val originalPriceAmountMicros: Long,
    val title: String,
    val description: String,
    val subscriptionPeriod: String?,
    val freeTrialPeriod: String?,
    val introductoryPrice: String?,
    val introductoryPriceAmountMicros: Long,
    val introductoryPricePeriod: String?,
    val introductoryPriceCycles: Int,
    val iconUrl: String
) {
    constructor(
        storeProduct: StoreProduct
    ) : this(
        sku = storeProduct.sku,
        type = storeProduct.type,
        price = storeProduct.price,
        priceAmountMicros = storeProduct.priceAmountMicros,
        priceCurrencyCode = storeProduct.priceCurrencyCode,
        originalPrice = storeProduct.originalPrice,
        originalPriceAmountMicros = storeProduct.originalPriceAmountMicros,
        title = storeProduct.title,
        description = storeProduct.description,
        subscriptionPeriod = storeProduct.subscriptionPeriod,
        freeTrialPeriod = storeProduct.freeTrialPeriod,
        introductoryPrice = storeProduct.introductoryPrice,
        introductoryPriceAmountMicros = storeProduct.introductoryPriceAmountMicros,
        introductoryPricePeriod = storeProduct.introductoryPricePeriod,
        introductoryPriceCycles = storeProduct.introductoryPriceCycles,
        iconUrl = storeProduct.iconUrl
    )
}
