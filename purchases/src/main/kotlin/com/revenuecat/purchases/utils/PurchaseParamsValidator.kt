package com.revenuecat.purchases.utils

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.strings.PurchaseStrings
import kotlin.jvm.Throws

/**
 * Maximum number of add-on products allowed in a multi-line purchase.
 */
internal const val MAX_NUMBER_OF_ADD_ON_PRODUCTS = 49

internal class PurchaseParamsValidator {

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    @Throws(PurchasesException::class)
    fun validate(purchaseParams: PurchaseParams): Result<Unit, PurchasesError> {
        if (!purchaseParams.addOnProducts.isNullOrEmpty()) {
            val addOnProductsValidationError = validateAddOnProducts(purchaseParams = purchaseParams)
            if (addOnProductsValidationError is Result.Error) {
                return addOnProductsValidationError
            }
        }

        return Result.Success(Unit)
    }

    @ExperimentalPreviewRevenueCatPurchasesAPI
    @Suppress("ReturnCount", "CyclomaticComplexMethod", "LongMethod")
    private fun validateAddOnProducts(
        purchaseParams: PurchaseParams,
    ): Result<Unit, PurchasesError> {
        val isGoogleSubscriptionPurchase = purchaseParams.purchasingData is GooglePurchasingData.Subscription
        if (!isGoogleSubscriptionPurchase && !purchaseParams.addOnProducts.isNullOrEmpty()) {
            val error = PurchasesError(
                PurchasesErrorCode.PurchaseInvalidError,
                "Add-ons are currently only supported for Google subscriptions.",
            ).also { errorLog(it) }

            return Result.Error(error)
        }

        val googleSubscriptionPurchasingData = purchaseParams.purchasingData as GooglePurchasingData.Subscription
        val addOnProducts = googleSubscriptionPurchasingData.addOnProducts ?: return Result.Success(Unit)

        if (addOnProducts.isEmpty()) {
            log(LogIntent.DEBUG) { PurchaseStrings.EMPTY_ADD_ONS_LIST_PASSED }
            return Result.Success(Unit)
        }

        if (addOnProducts.count() > MAX_NUMBER_OF_ADD_ON_PRODUCTS) {
            val error = PurchasesError(
                PurchasesErrorCode.PurchaseInvalidError,
                "Multi-line purchases cannot contain more than ${MAX_NUMBER_OF_ADD_ON_PRODUCTS + 1} products " +
                    "(1 base + $MAX_NUMBER_OF_ADD_ON_PRODUCTS add-ons).",
            ).also { errorLog(it) }

            return Result.Error(error)
        }

        var productIds = mutableSetOf<String>()
        productIds.add(googleSubscriptionPurchasingData.productId)
        for (addOnProduct in addOnProducts) {
            if (addOnProduct is GooglePurchasingData.Subscription) {
                val addOnProductId = addOnProduct.productId
                if (productIds.contains(addOnProductId)) {
                    // The developer is attempting to purchase multiple options on the same product.
                    val error = PurchasesError(
                        PurchasesErrorCode.PurchaseInvalidError,
                        "Multi-line purchases cannot contain multiple purchases for the same product." +
                            " Multiple purchases for the product $addOnProductId were provided.",
                    ).also { errorLog(it) }

                    return Result.Error(error)
                } else {
                    productIds.add(addOnProductId)
                }
            } else {
                // Product isn't a subscription
                val error = PurchasesError(
                    PurchasesErrorCode.PurchaseInvalidError,
                    "Add-ons are currently only supported for Google subscriptions.",
                ).also { errorLog(it) }

                return Result.Error(error)
            }
        }

        val billingPeriods = mutableSetOf<Period>()
        purchaseParams.baseItemProduct?.period?.let { billingPeriods.add(it) }
        val addOnBillingPeriods = purchaseParams.addOnProducts?.mapNotNull { it.period } ?: emptyList()
        for (billingPeriod in addOnBillingPeriods) {
            if (!billingPeriods.contains(billingPeriod)) {
                val error = PurchasesError(
                    PurchasesErrorCode.PurchaseInvalidError,
                    "All items in a multi-line purchase must have the same billing period.",
                ).also { errorLog(it) }

                return Result.Error(error)
            }
        }

        return Result.Success(Unit)
    }
}
