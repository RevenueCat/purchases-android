package com.revenuecat.purchases.ui.revenuecatui.customercenter.data

import com.revenuecat.purchases.CustomerInfo

/**
 * The maximum number of non-subscription purchases the relevant purchases screen shows
 * before the "See all purchases" link is needed.
 */
internal const val MAX_NON_SUBSCRIPTIONS_TO_SHOW: Int = 2

/**
 * Determines whether the "See all purchases" link should be shown.
 *
 * Returns true if:
 * - There are both active and inactive subscriptions.
 * - There are only inactive subscriptions and more than one of them.
 * - The number of non-subscription purchases exceeds [maxNonSubscriptions].
 *
 * This counts the raw customer info collections rather than the mapped [PurchaseInformation] list.
 * `isSubscription` and `isExpired` are display concerns (`isSubscription` excludes promotional
 * purchases, `isExpired` follows entitlement activity), so grouping by them undercounts here.
 */
internal fun CustomerInfo.shouldShowSeeAllPurchases(
    maxNonSubscriptions: Int = MAX_NON_SUBSCRIPTIONS_TO_SHOW,
): Boolean {
    val totalSubscriptions = subscriptionsByProductIdentifier.size
    val activeSubscriptionsCount = activeSubscriptions.size
    val inactiveSubscriptionsCount = totalSubscriptions - activeSubscriptionsCount

    val hasActiveAndInactiveSubscriptions = activeSubscriptionsCount > 0 && inactiveSubscriptionsCount > 0
    val hasSeveralInactiveSubscriptions = activeSubscriptionsCount == 0 && inactiveSubscriptionsCount > 1

    return hasActiveAndInactiveSubscriptions ||
        hasSeveralInactiveSubscriptions ||
        nonSubscriptionTransactions.size > maxNonSubscriptions
}
