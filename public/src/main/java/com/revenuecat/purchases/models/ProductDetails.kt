package com.revenuecat.purchases.models

import android.os.Parcelable
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.parceler.JSONObjectParceler
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.TypeParceler
import org.json.JSONObject

/**
 * Represents an in-app product's or subscription's listing details.
 */
@Parcelize
@TypeParceler<JSONObject, JSONObjectParceler>()
class ProductDetails(
    /**
     * The product ID.
     */
    val sku: String,

    /**
     * Type of product. One of [ProductType].
     */
    val type: ProductType,

    /**
     * Formatted price of the item, including its currency sign. For example $3.00.
     */
    val price: String,

    /**
     * Price in micro-units, where 1,000,000 micro-units equal one unit of the currency.
     *
     * For example, if price is "€7.99", price_amount_micros is 7,990,000. This value represents
     * the localized, rounded price for a particular currency.
     */
    val priceAmountMicros: Long,

    /**
     * Returns ISO 4217 currency code for price and original price.
     *
     * For example, if price is specified in British pounds sterling, price_currency_code is "GBP".
     */
    val priceCurrencyCode: String,

    /**
     * Formatted original price of the item, including its currency sign.
     *
     * Note: returned only for Google products. Not available for Amazon.
     */
    val originalPrice: String?,

    /**
     * Returns the original price in micro-units, where 1,000,000 micro-units equal one unit
     * of the currency.
     *
     * Note: returned only for Google products. Not available for Amazon.
     */
    val originalPriceAmountMicros: Long,

    /**
     * Title of the product.
     */
    val title: String,

    /**
     * The description of the product.
     */
    val description: String,

    /**
     * Subscription period, specified in ISO 8601 format. For example, P1W equates to one week,
     * P1M equates to one month, P3M equates to three months, P6M equates to six months,
     * and P1Y equates to one year.
     *
     * Note: Returned only for Google subscriptions. Not available for Amazon.
     */
    val subscriptionPeriod: String?,

    /**
     * Subscription period, specified in ISO 8601 format. For example, P1W equates to one week,
     * P1M equates to one month, P3M equates to three months, P6M equates to six months,
     * and P1Y equates to one year.
     *
     * Note: Returned only for Google subscriptions. Not available for Amazon.
     */
    val freeTrialPeriod: String?,

    /**
     * The billing period of the introductory price, specified in ISO 8601 format.
     *
     * Note: Returned only for Google subscriptions which have an introductory period configured.
     * Not available for Amazon.
     */
    val introductoryPrice: String?,

    /**
     * Introductory price in micro-units. The currency is the same as price_currency_code.
     *
     * Note: Returns 0 if the product is not Google a subscription or doesn't
     * have an introductory period. 0 for Amazon subscriptions.
     */
    val introductoryPriceAmountMicros: Long,

    /**
     * The billing period of the introductory price, specified in ISO 8601 format.
     *
     * Note: Returned only for Google subscriptions which have an introductory period configured.
     * Not available for Amazon.
     */
    val introductoryPricePeriod: String?,

    /**
     * The number of subscription billing periods for which the user will be given the
     * introductory price, such as 3.
     *
     * Note: Returns 0 if the SKU is not a Google subscription or doesn't
     * have an introductory period. 0 for Amazon subscriptions.
     */
    val introductoryPriceCycles: Int,

    /**
     * The icon of the product if present.
     */
    val iconUrl: String,

    /**
     * JSONObject representing the original product class from Google or Amazon.
     *
     * Note: there's a convenience extension property that can be used to get the original
     * SkuDetails class: `ProductDetails.skuDetails`.
     * Alternatively, the original SkuDetails can be built doing the following:
     * `SkuDetails(this.originalJson.toString())`
     *
     * For Amazon, the original Product can be obtained using `ProductDetails.amazonProduct`
     */
    val originalJson: JSONObject
) : Parcelable {

    @SuppressWarnings("ComplexMethod")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProductDetails

        if (sku != other.sku) return false
        if (type != other.type) return false
        if (price != other.price) return false
        if (priceAmountMicros != other.priceAmountMicros) return false
        if (priceCurrencyCode != other.priceCurrencyCode) return false
        if (originalPrice != other.originalPrice) return false
        if (originalPriceAmountMicros != other.originalPriceAmountMicros) return false
        if (title != other.title) return false
        if (description != other.description) return false
        if (subscriptionPeriod != other.subscriptionPeriod) return false
        if (freeTrialPeriod != other.freeTrialPeriod) return false
        if (introductoryPrice != other.introductoryPrice) return false
        if (introductoryPriceAmountMicros != other.introductoryPriceAmountMicros) return false
        if (introductoryPricePeriod != other.introductoryPricePeriod) return false
        if (introductoryPriceCycles != other.introductoryPriceCycles) return false
        if (iconUrl != other.iconUrl) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sku.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + price.hashCode()
        result = 31 * result + priceAmountMicros.hashCode()
        result = 31 * result + priceCurrencyCode.hashCode()
        result = 31 * result + (originalPrice?.hashCode() ?: 0)
        result = 31 * result + originalPriceAmountMicros.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + (subscriptionPeriod?.hashCode() ?: 0)
        result = 31 * result + (freeTrialPeriod?.hashCode() ?: 0)
        result = 31 * result + (introductoryPrice?.hashCode() ?: 0)
        result = 31 * result + introductoryPriceAmountMicros.hashCode()
        result = 31 * result + (introductoryPricePeriod?.hashCode() ?: 0)
        result = 31 * result + introductoryPriceCycles
        result = 31 * result + iconUrl.hashCode()
        result = 31 * result + originalJson.hashCode()
        return result
    }
}
