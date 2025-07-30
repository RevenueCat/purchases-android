package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.animation.Crossfade
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.purchases.ui.revenuecatui.UIConstant

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
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    allowLinks: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = introEligibilityText(
            eligibility,
            textWithIntroOffer,
            textWithMultipleIntroOffers,
            textWithNoIntroOffer,
        ),
        animationSpec = UIConstant.defaultAnimation(),
        label = "IntroEligibilityStateView",
    ) {
        Markdown(
            it,
            color = color,
            style = style,
            fontWeight = fontWeight,
            textAlign = textAlign,
            allowLinks = allowLinks,
            textFillMaxWidth = true,
            applyFontSizeToParagraph = false,
            modifier = modifier,
        )
    }
}

internal fun introEligibilityText(
    eligibility: IntroOfferEligibility,
    textWithIntroOffer: String?,
    textWithMultipleIntroOffers: String?,
    textWithNoIntroOffer: String?,
): String {
    return when (eligibility) {
        IntroOfferEligibility.SINGLE_OFFER_ELIGIBLE -> textWithIntroOffer
        IntroOfferEligibility.MULTIPLE_OFFERS_ELIGIBLE -> textWithMultipleIntroOffers
        else -> textWithNoIntroOffer
    } // Display text with intro offer as a backup to ensure layout does not change when switching states.
        ?: textWithNoIntroOffer
        ?: textWithIntroOffer
        ?: ""
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
