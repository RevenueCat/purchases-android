package com.revenuecat.paywallstester.ui.screens.main.offerings

import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchasesError

sealed class OfferingsState {
    data class Loaded(val offerings: Offerings) : OfferingsState()
    object Loading : OfferingsState()
    data class Error(val purchasesError: PurchasesError) : OfferingsState()
}
