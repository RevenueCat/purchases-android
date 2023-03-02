package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.OwnershipType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.VerificationResult
import java.util.Date

@Suppress("unused", "UNUSED_VARIABLE")
private class EntitlementInfoAPI {
    fun check(entitlementInfo: EntitlementInfo) {
        with(entitlementInfo) {
            val identifier: String = identifier
            val active: Boolean = isActive
            val willRenew: Boolean = willRenew
            val periodType: PeriodType = periodType
            val latestPurchaseDate: Date = latestPurchaseDate
            val originalPurchaseDate: Date = originalPurchaseDate
            val expirationDate: Date? = expirationDate
            val store: Store = store
            val productIdentifier: String = productIdentifier
            val sandbox: Boolean = isSandbox
            val unsubscribeDetectedAt: Date? = unsubscribeDetectedAt
            val billingIssueDetectedAt: Date? = billingIssueDetectedAt
            val ownershipType: OwnershipType = ownershipType
            val verification: VerificationResult = verification
        }
    }

    fun store(store: Store) {
        when (store) {
            Store.APP_STORE,
            Store.MAC_APP_STORE,
            Store.PLAY_STORE,
            Store.STRIPE,
            Store.PROMOTIONAL,
            Store.UNKNOWN_STORE,
            Store.AMAZON
            -> {}
        }.exhaustive
    }

    fun periodType(type: PeriodType) {
        when (type) {
            PeriodType.NORMAL,
            PeriodType.INTRO,
            PeriodType.TRIAL
            -> {}
        }.exhaustive
    }

    fun ownershipType(type: OwnershipType) {
        when (type) {
            OwnershipType.PURCHASED,
            OwnershipType.FAMILY_SHARED,
            OwnershipType.UNKNOWN -> {}
        }.exhaustive
    }
}
