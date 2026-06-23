package com.revenuecat.paywallstester.ui.screens.main.offerings

import android.content.Context
import com.revenuecat.paywallstester.BundledPaywalls
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchasesError

sealed class OfferingsState {
    data class Loaded(
        val offerings: Offerings,
        val searchQuery: String = "",
        val recentOfferingIds: List<String> = emptyList(),
    ) : OfferingsState()
    object Loading : OfferingsState()
    data class Error(val purchasesError: PurchasesError) : OfferingsState()
}

/**
 * Returns a copy of this [OfferingsState.Loaded] with the app's [BundledPaywalls] merged into the
 * offerings fetched from the server, so they show up alongside the real offerings in the list.
 */
fun OfferingsState.Loaded.withBundledPaywalls(context: Context): OfferingsState.Loaded {
    val bundled = BundledPaywalls.offerings(context)
    if (bundled.isEmpty()) return this
    val mergedAll = offerings.all + bundled.associateBy { it.identifier }
    return copy(offerings = Offerings(current = offerings.current, all = mergedAll))
}
