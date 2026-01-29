package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.ui.revenuecatui.composables.IntroOfferEligibility

internal data class IntroOfferAvailability(
    val hasAnyIntroOfferEligiblePackage: Boolean = false,
    val hasAnyMultipleIntroOffersEligiblePackage: Boolean = false,
)

internal data class IntroOfferSnapshot(
    val eligibility: IntroOfferEligibility,
    val availability: IntroOfferAvailability,
)
