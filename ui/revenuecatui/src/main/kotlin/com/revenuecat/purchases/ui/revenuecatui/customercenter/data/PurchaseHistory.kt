package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import androidx.compose.runtime.Immutable

/**
 * The customer's full purchase history, grouped the way the history screen renders it.
 *
 * The groups come from the customer info collections themselves rather than from
 * [PurchaseInformation.isSubscription] and [PurchaseInformation.isExpired]. Those are display
 * flags: `isSubscription` excludes promotional purchases and `isExpired` follows entitlement
 * activity, so regrouping by them moves purchases into the wrong section.
 */
@Immutable
internal data class PurchaseHistory(
    val activeSubscriptions: List<PurchaseInformation> = emptyList(),
    val inactiveSubscriptions: List<PurchaseInformation> = emptyList(),
    val nonSubscriptions: List<PurchaseInformation> = emptyList(),
) {
    val all: List<PurchaseInformation>
        get() = activeSubscriptions + inactiveSubscriptions + nonSubscriptions

    val isEmpty: Boolean
        get() = activeSubscriptions.isEmpty() && inactiveSubscriptions.isEmpty() && nonSubscriptions.isEmpty()
}
