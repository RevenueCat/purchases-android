@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.state

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import com.revenuecat.purchases.ui.revenuecatui.composables.OfferEligibility
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState

internal class PackageAwareDelegate(
    private val style: PackageContext,
    private val selectedPackageInfoProvider: () -> PaywallState.Loaded.Components.SelectedPackageInfo?,
    private val selectedTabIndexProvider: () -> Int,
    private val selectedOfferEligibilityProvider: () -> OfferEligibility,
) {
    val isSelected by derivedStateOf {
        style.computeIsSelected(
            selectedPackageInfo = selectedPackageInfoProvider(),
            selectedTabIndex = selectedTabIndexProvider(),
        )
    }

    val offerEligibility by derivedStateOf {
        style.resolveOfferEligibility(
            selectedOfferEligibility = selectedOfferEligibilityProvider(),
        )
    }
}
