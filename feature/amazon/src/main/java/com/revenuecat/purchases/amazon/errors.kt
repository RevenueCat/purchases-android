package com.revenuecat.purchases.amazon

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import org.json.JSONObject

internal fun errorGettingReceiptInfo(error: PurchasesError) = PurchasesError(
    PurchasesErrorCode.InvalidReceiptError,
    "Couldn't get Amazon receipt data from RevenueCat backend. Error: $error",
)

internal fun missingTermSkuError(response: JSONObject) = PurchasesError(
    PurchasesErrorCode.UnexpectedBackendResponseError,
    "Amazon receipt data response is missing termSku. Response:\n$response",
)
