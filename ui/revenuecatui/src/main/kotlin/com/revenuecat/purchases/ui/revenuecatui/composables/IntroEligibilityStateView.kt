package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview

/**
 * A composable that can display different data based on intro eligibility status.
 */
@SuppressWarnings("LongParameterList")
@Composable
internal fun IntroEligibilityStateView(
    textWithNoIntroOffer: String?,
    textWithIntroOffer: String?,
    textWithMultipleIntroOffers: String?,
    eligibility: IntroOfferEligibility,
    color: Color = Color.Unspecified,
    style: TextStyle = TextStyle.Default,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
) {
    val text: String = when (eligibility) {
        IntroOfferEligibility.SINGLE_OFFER_ELIGIBLE -> textWithIntroOffer
        IntroOfferEligibility.MULTIPLE_OFFERS_ELIGIBLE -> textWithMultipleIntroOffers
        else -> textWithNoIntroOffer
    } // Display text with intro offer as a backup to ensure layout does not change when switching states.
        ?: textWithNoIntroOffer
        ?: textWithIntroOffer
        ?: ""

    Markdown(
        text,
        color = color,
        style = style,
        fontWeight = fontWeight,
        textAlign = textAlign,
    )
}

internal enum class IntroOfferEligibility {
    INELIGIBLE,
    SINGLE_OFFER_ELIGIBLE,
    MULTIPLE_OFFERS_ELIGIBLE,
}

@Preview(showBackground = true)
@Composable
private fun IntroEligibilityPreviewNoOffer() {
    IntroEligibilityStateView(
        textWithNoIntroOffer = "$3.99/mo",
        textWithIntroOffer = null,
        textWithMultipleIntroOffers = null,
        eligibility = IntroOfferEligibility.SINGLE_OFFER_ELIGIBLE,
        color = Color.Black,
    )
}

@Preview(showBackground = true)
@Composable
private fun IntroEligibilityPreviewBothTextsIneligible() {
    IntroEligibilityStateView(
        textWithNoIntroOffer = "$3.99/mo",
        textWithIntroOffer = "7 day trial, then $3.99/mo",
        textWithMultipleIntroOffers = "7 days for free, then $1.99 for your first month, and just $4.99/mo thereafter.",
        eligibility = IntroOfferEligibility.INELIGIBLE,
        color = Color.Black,
    )
}

@Preview(showBackground = true)
@Composable
private fun IntroEligibilityPreviewBothTextsEligibleSingleOffer() {
    IntroEligibilityStateView(
        textWithNoIntroOffer = "$3.99/mo",
        textWithIntroOffer = "7 day trial, then $3.99/mo",
        textWithMultipleIntroOffers = "7 days for free, then $1.99 for your first month, and just $3.99/mo thereafter.",
        eligibility = IntroOfferEligibility.SINGLE_OFFER_ELIGIBLE,
        color = Color.Black,
    )
}

@Preview(showBackground = true)
@Composable
private fun IntroEligibilityPreviewBothTextsEligibleMultipleOffers() {
    IntroEligibilityStateView(
        textWithNoIntroOffer = "$3.99/mo",
        textWithIntroOffer = "7 day trial, then $3.99/mo",
        textWithMultipleIntroOffers = "7 days for free, then $1.99 for your first month, and just $4.99/mo thereafter.",
        eligibility = IntroOfferEligibility.MULTIPLE_OFFERS_ELIGIBLE,
        color = Color.Black,
    )
}
