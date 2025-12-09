package com.revenuecat.purchases.galaxy

import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.samsung.android.sdk.iap.lib.vo.ProductVo

internal fun ProductVo.toStoreProduct(): StoreProduct {
    return GalaxyStoreProduct(
        id = this.itemId,
        type = this.type.convertSamsungIAPTypeStringToRevenueCatProductType(),
        price = TODO(),
        name = TODO(),
        title = TODO(),
        description = TODO(),
        period = TODO(),
        subscriptionOptions = TODO(),
        defaultOption = TODO(),
        presentedOfferingContext = TODO()
    )
}

internal fun ProductVo.createPrice(): Price {
    return Price(
        // Here, we manually build the formatted string instead of using ProductVo.itemPriceString,
        // because itemPriceString doesn't include the decimal values if the amount is an integer with no decimal value.
        // This way, we can get strings like "$3.00" instead of "$3"
        formatted = "$currencyUnit$itemPrice",
        amountMicros = itemPrice * 1_000_000,
        currencyCode = TODO()
    )
}

internal fun String.convertSamsungIAPTypeStringToRevenueCatProductType(): ProductType {
    return when(this.lowercase()) {
        "item" -> ProductType.INAPP
        "subscription" -> ProductType.SUBS
        else -> ProductType.UNKNOWN
    }
}
