package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

internal enum class IntroOfferEligibility {
    INELIGIBLE,
    ELIGIBLE,
}

/**
 * A composable that can display different data based on intro eligibility status.
 */
@Composable
internal fun IntroEligibilityStateView(
    textWithNoIntroOffer: String?,
    textWithIntroOffer: String?,
    eligibility: IntroOfferEligibility,
    color: Color,
) {
    val text: String = if (textWithIntroOffer != null && eligibility == IntroOfferEligibility.ELIGIBLE) {
        textWithIntroOffer
    } else {
        // Display text with intro offer as a backup to ensure layout does not change
        // when switching states.
        textWithNoIntroOffer ?: textWithIntroOffer ?: ""
    }

    Text(text, color = color)
}

@Preview(showBackground = true)
@Composable
private fun IntroEligibilityPreview() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IntroEligibilityStateView(
            textWithNoIntroOffer = "$3.99/mo",
            textWithIntroOffer = null,
            eligibility = IntroOfferEligibility.ELIGIBLE,
            color = Color.Black,
        )

        IntroEligibilityStateView(
            textWithNoIntroOffer = "$3.99/mo",
            textWithIntroOffer = "7 day trial, then $3.99/mo",
            eligibility = IntroOfferEligibility.INELIGIBLE,
            color = Color.Black,
        )

        IntroEligibilityStateView(
            textWithNoIntroOffer = "$3.99/mo",
            textWithIntroOffer = "7 day trial, then $3.99/mo",
            eligibility = IntroOfferEligibility.ELIGIBLE,
            color = Color.Black,
        )
    }
}
