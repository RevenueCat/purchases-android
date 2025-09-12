@file:JvmName("ProductValidation")

package com.revenuecat.purchases.google

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.googleProduct
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
// TODO: Write tests for this
@ExperimentalPreviewRevenueCatPurchasesAPI
@Throws(PurchasesException::class)
internal fun validateAndFilterCompatibleAddOnProducts(
    baseProductPurchasingData: PurchasingData,
    addOnProducts: List<StoreProduct>
): List<GooglePurchasingData> {
    if (baseProductPurchasingData !is GooglePurchasingData.Subscription) {
        throw PurchasesException(
            PurchasesError(
                PurchasesErrorCode.PurchaseInvalidError,
                "Add-ons are currently only supported for Google subscriptions."
            )
        )
    }
    val addOnProductsWithSameProductType: MutableList<GooglePurchasingData> = ArrayList()
    val billingPeriods = mutableSetOf<Period>()
    
    for (addOnProduct in addOnProducts) {
        // Only process add-ons whose product type matches that of the baseProduct.
        val addOnPurchasingData = addOnProduct.purchasingData
        if (addOnProduct.googleProduct != null &&
            addOnPurchasingData is GooglePurchasingData &&
            addOnPurchasingData::class == baseProductPurchasingData::class
        ) {
            addOnProductsWithSameProductType.add(addOnPurchasingData)
            addOnProduct.period?.let { billingPeriods.add(it) }
        }
    }

    val addOnProductsWithSameProductTypeCount = addOnProductsWithSameProductType.count()
    if (addOnProductsWithSameProductTypeCount == 0) {
        throw PurchasesException(
            PurchasesError(
                PurchasesErrorCode.PurchaseInvalidError,
                "At least one add-on with the same product type as the base product must be provided."
            )
        )
    }

    if (addOnProductsWithSameProductTypeCount > MAX_NUMBER_OF_ADD_ON_PRODUCTS) {
        throw PurchasesException(
            PurchasesError(
                PurchasesErrorCode.PurchaseInvalidError,
                "Multi-line purchases cannot contain more than ${MAX_NUMBER_OF_ADD_ON_PRODUCTS + 1} products " +
                    "(1 base + $MAX_NUMBER_OF_ADD_ON_PRODUCTS add-ons)."
            )
        )
    }

    if (billingPeriods.size > 1) {
        throw PurchasesException(
            PurchasesError(
                PurchasesErrorCode.PurchaseInvalidError,
                "All items in a multi-line purchase must have the same billing period."
            )
        )
    }

    return addOnProductsWithSameProductType
}
