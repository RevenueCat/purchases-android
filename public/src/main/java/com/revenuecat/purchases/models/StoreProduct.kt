package com.revenuecat.purchases.models

import android.os.Parcelable
import com.android.billingclient.api.SkuDetails
import com.revenuecat.purchases.ProductType
import org.json.JSONObject

/**
 * Represents an in-app product's or subscription's listing details.
 */
interface StoreProduct : Parcelable {
    /**
     * sku for BC4 and Amazon, subId for BC5
     */
    val storeProductId: String

    /**
     * Type of product. One of [ProductType].
     */
    val type: ProductType

    /**
     * Formatted price of the item, including its currency sign. For example $3.00.
     */
    val price: String

    /**
     * Price in micro-units, where 1,000,000 micro-units equal one unit of the currency.
     *
     * For example, if price is "€7.99", price_amount_micros is 7,990,000. This abstract value represents
     * the localized, rounded price for a particular currency.
     */
    val priceAmountMicros: Long

    /**
     * Returns ISO 4217 currency code for price and original price.
     *
     * For example, if price is specified in British pounds sterling, price_currency_code is "GBP".
     *
     * If currency code cannot be determined, currency symbol is returned.
     */
    val priceCurrencyCode: String

    /**
     * Title of the product.
     */
    val title: String

    /**
     * The description of the product.
     */
    val description: String

    /**
     * Subscription period, specified in ISO 8601 format. For example, P1W equates to one week,
     * P1M equates to one month, P3M equates to three months, P6M equates to six months,
     * and P1Y equates to one year.
     *
     * Note: Returned only for Google subscriptions. Not available for Amazon.
     *
     * maddie note: this used to come from BC, now we get it from getOfferings
     */
    val subscriptionPeriod: String?
}

data class ComparableData(
    val storeProductId: String,
    val type: ProductType,
    val price: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val title: String,
    val description: String,
    val subscriptionPeriod: String?
) {
    constructor(
        storeProduct: StoreProduct
    ) : this(
        storeProductId = storeProduct.storeProductId,
        type = storeProduct.type,
        price = storeProduct.price,
        priceAmountMicros = storeProduct.priceAmountMicros,
        priceCurrencyCode = storeProduct.priceCurrencyCode,
        title = storeProduct.title,
        description = storeProduct.description,
        subscriptionPeriod = storeProduct.subscriptionPeriod
    )
}
