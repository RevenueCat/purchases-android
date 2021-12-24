package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.OwnershipType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.Store
import java.util.Date

@Suppress("unused", "UNUSED_VARIABLE")
private class EntitlementInfoAPI {
    fun check(entitlementInfo: EntitlementInfo) {
        val identifier: String = entitlementInfo.identifier
        val active: Boolean = entitlementInfo.isActive
        val willRenew: Boolean = entitlementInfo.willRenew
        val periodType: PeriodType = entitlementInfo.periodType
        val latestPurchaseDate: Date = entitlementInfo.latestPurchaseDate
        val originalPurchaseDate: Date = entitlementInfo.originalPurchaseDate
        val expirationDate: Date? = entitlementInfo.expirationDate
        val store: Store = entitlementInfo.store
        val productIdentifier: String = entitlementInfo.productIdentifier
        val sandbox: Boolean = entitlementInfo.isSandbox
        val unsubscribeDetectedAt: Date? = entitlementInfo.unsubscribeDetectedAt
        val billingIssueDetectedAt: Date? = entitlementInfo.billingIssueDetectedAt
        val ownershipType: OwnershipType = entitlementInfo.ownershipType
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
        }
    }

    fun periodType(type: PeriodType) {
        when (type) {
            PeriodType.NORMAL,
            PeriodType.INTRO,
            PeriodType.TRIAL
            -> {}
        }
    }

    fun ownershipType(type: OwnershipType) {
        when (type) {
            OwnershipType.PURCHASED,
            OwnershipType.FAMILY_SHARED,
            OwnershipType.UNKNOWN -> {}
        }
    }
}
