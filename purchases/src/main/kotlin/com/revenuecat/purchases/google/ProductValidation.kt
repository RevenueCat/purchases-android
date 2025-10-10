package com.revenuecat.purchases.google

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.platformProductId
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct
import kotlin.jvm.Throws

/**
 * Maximum number of add-on products allowed in a multi-line purchase.
 */
internal const val MAX_NUMBER_OF_ADD_ON_PRODUCTS = 49

/**
 * Validates and filters add-on products for multi-line purchases.
 *
 * Performs compatibility checks and filters add-ons to ensure they are compatible with the base product.
 *
 * @param baseProductPurchasingData The purchase data for primary product that add-ons will be bundled with
 * @param addOnProducts The list of potential add-on products to validate and filter
 * @return A filtered list containing only add-ons that match the base product's purchasing data type
 * @throws PurchasesException If the add-ons failed validation for some reason.
 */
@ExperimentalPreviewRevenueCatPurchasesAPI
@Throws(PurchasesException::class)
@Suppress("ThrowsCount")
internal fun validateAndFilterCompatibleAddOnProducts(
    baseProductPurchasingData: PurchasingData,
    addOnProducts: List<StoreProduct>,
): List<GooglePurchasingData> {
    if (baseProductPurchasingData !is GooglePurchasingData.Subscription) {
        val error = PurchasesError(
            PurchasesErrorCode.PurchaseInvalidError,
            "Add-ons are currently only supported for Google subscriptions.",
        ).also { errorLog(it) }

        throw PurchasesException(error)
    }
    val addOnProductsWithSameProductType: MutableList<GooglePurchasingData> = ArrayList()
    val billingPeriods = mutableSetOf<Period>()
    var productIds = mutableSetOf<String>()
    productIds.add(baseProductPurchasingData.productId)

    for (addOnProduct in addOnProducts) {
        val addOnPurchasingData = addOnProduct.purchasingData
        if (addOnPurchasingData is GooglePurchasingData &&
            addOnPurchasingData::class == baseProductPurchasingData::class
        ) {
            addOnProductsWithSameProductType.add(addOnPurchasingData)
            addOnProduct.period?.let { billingPeriods.add(it) }

            val addOnProductId = addOnProduct.platformProductId().productId
            if (productIds.contains(addOnProductId)) {
                // The developer is attempting to purchase multiple options on the same product.
                val error = PurchasesError(
                    PurchasesErrorCode.PurchaseInvalidError,
                    "Multi-line purchases cannot contain multiple purchases for the same product." +
                        " Multiple purchases for the product $addOnProductId were provided.",
                ).also { errorLog(it) }

                throw PurchasesException(error)
            } else {
                productIds.add(addOnProductId)
            }
        }
    }

    if (addOnProductsWithSameProductType.count() > MAX_NUMBER_OF_ADD_ON_PRODUCTS) {
        val error = PurchasesError(
            PurchasesErrorCode.PurchaseInvalidError,
            "Multi-line purchases cannot contain more than ${MAX_NUMBER_OF_ADD_ON_PRODUCTS + 1} products " +
                "(1 base + $MAX_NUMBER_OF_ADD_ON_PRODUCTS add-ons).",
        ).also { errorLog(it) }

        throw PurchasesException(error)
    }

    if (billingPeriods.size > 1) {
        val error = PurchasesError(
            PurchasesErrorCode.PurchaseInvalidError,
            "All items in a multi-line purchase must have the same billing period.",
        ).also { errorLog(it) }

        throw PurchasesException(error)
    }

    return addOnProductsWithSameProductType
}
