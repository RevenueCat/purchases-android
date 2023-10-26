package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.revenuecat.purchases.ui.revenuecatui.UIConstant
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.selectedLocalization
import com.revenuecat.purchases.ui.revenuecatui.extensions.introEligibility

/**
 * This displays the offer details of the selected package. For templates that allow to select
 * multiple packages, each package needs its own offer details so we won't use this composable.
 */
@Composable
internal fun OfferDetails(
    state: PaywallState.Loaded,
    color: (TemplateConfiguration.Colors) -> Color = { it.text1 }
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
            color = color(state.templateConfiguration.getCurrentColors()),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
