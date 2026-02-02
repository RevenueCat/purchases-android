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
    eligibility: OfferEligibility,
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
    eligibility: OfferEligibility,
    textWithIntroOffer: String?,
    textWithMultipleIntroOffers: String?,
    textWithNoIntroOffer: String?,
): String {
    return when (eligibility) {
        OfferEligibility.IntroOfferSingle,
        OfferEligibility.PromoOfferSingle,
        -> textWithIntroOffer

        OfferEligibility.IntroOfferMultiple,
        OfferEligibility.PromoOfferMultiple,
        -> textWithMultipleIntroOffers

        OfferEligibility.Ineligible,
        OfferEligibility.PromoOfferIneligible,
        -> textWithNoIntroOffer
    } // Display text with intro offer as a backup to ensure layout does not change when switching states.
        ?: textWithNoIntroOffer
        ?: textWithIntroOffer
        ?: ""
}

/**
 * Represents the eligibility status for subscription offers.
 * Combines the offer type (intro vs promo) with the number of discounted phases.
 */
internal sealed class OfferEligibility {
    /** Default option with no discounted phases */
    object Ineligible : OfferEligibility()

    /** Default option with a single discounted phase (either free trial OR intro price) */
    object IntroOfferSingle : OfferEligibility()

    /** Default option with multiple discounted phases (both free trial AND intro price) */
    object IntroOfferMultiple : OfferEligibility()

    /** Configured promo offer with a single discounted phase */
    object PromoOfferSingle : OfferEligibility()

    /** Configured promo offer with multiple discounted phases */
    object PromoOfferMultiple : OfferEligibility()

    /** Configured promo offer with no discounted phases (misconfigured or base plan only) */
    object PromoOfferIneligible : OfferEligibility()

    /** Whether this represents any offer (intro OR promo) with multiple discounted phases */
    val hasMultipleDiscountedPhases: Boolean
        get() = this is IntroOfferMultiple || this is PromoOfferMultiple

    /** Whether this represents a single-phase intro offer (for IntroOffer condition matching) */
    val isSinglePhaseIntroOffer: Boolean
        get() = this is IntroOfferSingle

    /** Whether this represents a single-phase promo offer (for PromoOffer condition matching) */
    val isSinglePhasePromoOffer: Boolean
        get() = this is PromoOfferSingle
}

@Preview(showBackground = true)
@Composable
private fun IntroEligibilityPreviewNoOffer() {
    IntroEligibilityStateView(
        textWithNoIntroOffer = "$3.99/mo",
        textWithIntroOffer = null,
        textWithMultipleIntroOffers = null,
        eligibility = OfferEligibility.IntroOfferSingle,
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
        eligibility = OfferEligibility.Ineligible,
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
        eligibility = OfferEligibility.IntroOfferSingle,
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
        eligibility = OfferEligibility.IntroOfferMultiple,
        color = Color.Black,
    )
}
