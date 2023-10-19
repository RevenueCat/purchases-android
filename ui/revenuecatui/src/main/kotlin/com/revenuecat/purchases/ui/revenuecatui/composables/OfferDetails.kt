package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.selectedLocalization
import com.revenuecat.purchases.ui.revenuecatui.extensions.introEligibility

@Composable
internal fun OfferDetails(
    state: PaywallState.Loaded,
) {
    Box(
        modifier = Modifier
            .padding(bottom = UIConstant.defaultVerticalSpacing),
    ) {
        IntroEligibilityStateView(
            textWithNoIntroOffer = state.selectedLocalization.offerDetails,
            textWithIntroOffer = state.selectedLocalization.offerDetailsWithIntroOffer,
            textWithMultipleIntroOffers = state.selectedLocalization.offerDetailsWithMultipleIntroOffers,
            eligibility = state.selectedPackage.value.introEligibility,
            color = state.templateConfiguration.getCurrentColors().text1,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
