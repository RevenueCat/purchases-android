package com.revenuecat.purchases.google

import com.android.billingclient.api.ProductDetails
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct

// In-apps don't have base plan nor offers
fun ProductDetails.toStoreProduct(): StoreProduct = this.toStoreProduct(null, emptyList())

fun ProductDetails.toStoreProduct(
    basePlan: ProductDetails.SubscriptionOfferDetails?,
    offers: List<ProductDetails.SubscriptionOfferDetails>
) =
    GoogleStoreProduct(
        productId,
        productType.toRevenueCatProductType(),
        createOneTimeProductPrice(),
        title,
        description,
        basePlan?.pricingPhases?.pricingPhaseList?.get(0)?.billingPeriod,
        offers.map { it.toPurchaseOption() },
        this
    )

private fun ProductDetails.createOneTimeProductPrice(): Price? {
    return if (productType.toRevenueCatProductType() == ProductType.INAPP) {
        oneTimePurchaseOfferDetails?.let {
            Price(
                it.formattedPrice,
                it.priceAmountMicros,
                it.priceCurrencyCode
            )
        }
    } else null
}

fun List<ProductDetails>.toStoreProducts(): List<StoreProduct> {
    val storeProducts = mutableListOf<StoreProduct>()
    forEach { productDetails ->
        val basePlans = productDetails.subscriptionOfferDetails?.filter {
            it.pricingPhases.pricingPhaseList.size == 1
        } ?: emptyList()

        val offersBySubPeriod = productDetails.subscriptionOfferDetails?.groupBy {
            it.subscriptionBillingPeriod
        } ?: emptyMap()
        basePlans.takeUnless { it.isEmpty() }?.forEach { basePlan ->
            val basePlanBillingPeriod = basePlan.subscriptionBillingPeriod
            val offers = offersBySubPeriod[basePlanBillingPeriod] ?: emptyList()
            productDetails.toStoreProduct(basePlan, offers).let { storeProducts.add(it) }
        } ?: productDetails.toStoreProduct().let { storeProducts.add(it) }
    }
    return storeProducts
}
