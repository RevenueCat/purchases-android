package com.revenuecat.purchases.amazon

import android.os.Parcelable
import com.amazon.device.iap.model.Product
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.MICROS_MULTIPLIER
import com.revenuecat.purchases.common.OfferingFactory
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.utils.JSONObjectParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.json.JSONObject
import java.math.BigDecimal
import java.util.regex.Pattern

sealed class AmazonPurchasingData : PurchasingData {
    data class Product(
        val storeProduct: AmazonStoreProduct,
    ) : AmazonPurchasingData() {
        override val productId: String
            get() = storeProduct.id
        override val productType: ProductType
            get() = storeProduct.type
    }
}

@Parcelize
@TypeParceler<JSONObject, JSONObjectParceler>()
data class AmazonStoreProduct(
    override val id: String,
    override val type: ProductType,
    override val title: String,
    override val description: String,
    override val period: Period?,

    override val price: com.revenuecat.purchases.models.Price,
    override val subscriptionOptions: List<SubscriptionOption>,
    override val defaultOption: SubscriptionOption?,
    val iconUrl: String,
    val originalJson: JSONObject,
    val amazonProduct: Product,
) : StoreProduct, Parcelable {

    override val purchasingData: AmazonPurchasingData
        get() = AmazonPurchasingData.Product(this)

    override val sku: String
        get() = id

    // We use this to not include the originalJSON in the equals
    /*override fun equals(other: Any?) = other is StoreProduct && ComparableData(this) == ComparableData(other)
    override fun hashCode() = ComparableData(this).hashCode()*/ // TODOBC5
}

fun Product.toStoreProduct(marketplace: String): StoreProduct? {
    if (price == null) {
        log(LogIntent.AMAZON_ERROR, AmazonStrings.PRODUCT_PRICE_MISSING.format(sku))
        return null
    }
    // By default, Amazon automatically converts the base list price of your IAP items into
    // the local currency of each marketplace where they can be sold, and customers will see IAP items in English.
    val (currencyCode, priceAmountMicros) = price.extractPrice(marketplace)

    val priceInfo = com.revenuecat.purchases.models.Price(price, priceAmountMicros, currencyCode)

    return AmazonStoreProduct(
        sku,
        productType.toRevenueCatProductType(),
        title,
        description,
        period = null,
        priceInfo,
        emptyList(),
        defaultOption = null,
        iconUrl = smallIconUrl,
        originalJson = toJSON(),
        amazonProduct = this
    )
}

internal fun String.extractPrice(marketplace: String): Price {
    val priceNumeric = this.parsePriceUsingRegex() ?: BigDecimal.ZERO
    val priceAmountMicros = (priceNumeric * BigDecimal(MICROS_MULTIPLIER)).toLong()
    val currencyCode = ISO3166Alpha2ToISO42170Converter.convertOrEmpty(marketplace)

    return Price(
        currencyCode,
        priceAmountMicros
    )
}

internal data class Price(
    val currencyCode: String,
    val priceAmountMicros: Long
)

// Explanations about the regexp:
// \\d+: match the first(s) number(s)
// [\\.,\\s]: match a "separator": a dot, comma or space
// \\d+ (the second one): match the number(s) after the separator.
// The lasts two are englobed in []*, as they can be repeated 0 or n times.
private val pattern: Pattern = Pattern.compile("(\\d+[[\\.,\\s]\\d+]*)")

internal fun String.parsePriceUsingRegex(): BigDecimal? {
    val matcher = pattern.matcher(this)
    return matcher.takeIf { it.find() }?.let {
        val dirtyPrice = matcher.group()
        // Amazon sends a nbsp character in countries with euros "5,80 €"
        // Android devices will match the nbsp, JVM (when running on unit tests will not match the nbsp)
        // So we remove them and trim just in case
        var price =
            dirtyPrice.replace(" ", "")
                .replace(" ", "") // This is a NBSP, some editors might render it as a space or a tab
                .replace("${Typography.nbsp}", "")
                .trim()
        val split = price.split(".", ",")
        if (split.size != 1) {
            // Assuming all prices we get have 2 decimal points
            // Most currencies but Dirhan use 2 decimals. Amazon doesn't support Dirhan at the moment
            price = if (split.last().length == 3) {
                price.replace(".", "").replace(",", "")
            } else {
                val intPart = split.dropLast(1).joinToString("")
                "$intPart.${split.last()}"
            }
        }
        price = price.trim()
        BigDecimal(price)
    }
}

class AmazonOfferingFactory : OfferingFactory() {
    override fun Map<String, List<StoreProduct>>.findMatchingProduct(
        productIdentifier: String,
        planIdentifier: String?
    ): StoreProduct? = this[productIdentifier]?.get(0)
}
