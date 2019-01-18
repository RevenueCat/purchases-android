package com.revenuecat.purchases

/**
 * This class represents an error
 * @param domain Domain of the error
 * @param code Error code
 * @param message Message explaining the error
 */
data class PurchasesError(
    val domain: Purchases.ErrorDomains,
    val code: Int,
    val message: String?
)