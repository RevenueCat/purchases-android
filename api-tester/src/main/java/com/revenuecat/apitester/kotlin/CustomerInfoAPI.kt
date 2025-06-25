package com.revenuecat.apitester.kotlin

import android.net.Uri
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfos
import com.revenuecat.purchases.models.Transaction
import java.util.Date

@Suppress("unused", "UNUSED_VARIABLE")
private class CustomerInfoAPI {
    fun check(customerInfo: CustomerInfo) {
        with(customerInfo) {
            val entitlementInfo: EntitlementInfos = entitlements
            val asubs = activeSubscriptions
            val skus: Set<String> = allPurchasedSkus
            val productIds: Set<String> = allPurchasedProductIds
            val led: Date? = latestExpirationDate
            val nst: List<Transaction> = nonSubscriptionTransactions
            val opd: Date? = originalPurchaseDate
            val rd: Date = requestDate
            val fs: Date = firstSeen
            val oaui: String = originalAppUserId
            val mu: Uri? = managementURL
            val eds: Date? = getExpirationDateForSku("")
            val edpi: Date? = getExpirationDateForProductId("")
            val pds: Date? = getPurchaseDateForSku("")
            val pdpi: Date? = getPurchaseDateForProductId("")
            val ede: Date? = getExpirationDateForEntitlement("")
            val pde: Date? = getPurchaseDateForEntitlement("")
            val allExpirationDatesByProduct: Map<String, Date?> = allExpirationDatesByProduct
            val allPurchaseDatesByProduct: Map<String, Date?> = allPurchaseDatesByProduct
        }
    }
}
