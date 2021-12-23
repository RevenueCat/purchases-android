package com.revenuecat.apitesterkotlin

import android.net.Uri
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfos
import com.revenuecat.purchases.models.Transaction
import java.util.Date

@Suppress("unused")
private class CustomerInfoAPI {
    fun check(customerInfo: CustomerInfo) {
        val entitlementInfo: EntitlementInfos = customerInfo.entitlements
        val asubs = customerInfo.activeSubscriptions
        val skus: Set<String> = customerInfo.allPurchasedSkus
        val led: Date? = customerInfo.latestExpirationDate
        val nst: List<Transaction> = customerInfo.nonSubscriptionTransactions
        val opd: Date? = customerInfo.originalPurchaseDate
        val rd: Date = customerInfo.requestDate
        val fs: Date = customerInfo.firstSeen
        val oaui: String = customerInfo.originalAppUserId
        val mu: Uri? = customerInfo.managementURL
        val eds: Date? = customerInfo.getExpirationDateForSku("")
        val pds: Date? = customerInfo.getPurchaseDateForSku("")
        val ede: Date? = customerInfo.getExpirationDateForEntitlement("")
        val pde: Date? = customerInfo.getPurchaseDateForEntitlement("")
    }
}
