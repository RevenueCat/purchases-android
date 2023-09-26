package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.compose.material.Colors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.PaywallColor
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import java.net.URL
import java.util.Locale

/***
 * Default [PaywallData] to display when attempting to present a [PaywallView] with an offering that has no paywall
 * configuration, or when that configuration is invalid.
 */
internal fun PaywallData.Companion.createDefault(
    packages: List<Package>,
    currentColors: Colors,
): PaywallData {
    return PaywallData.createDefaultForIdentifiers(packages.map { it.identifier }, currentColors)
}

internal fun PaywallData.Companion.createDefaultForIdentifiers(
    packageIdentifiers: List<String>,
    currentColors: Colors,
): PaywallData {
    return PaywallData(
        templateName = PaywallData.defaultTemplate.id,
        config = PaywallData.Configuration(
            packages = packageIdentifiers,
            images = PaywallData.Configuration.Images(
                background = PaywallData.defaultBackgroundImage,
                icon = PaywallData.defaultAppIconPlaceholder,
            ),
            colors = PaywallData.defaultColors(currentColors),
            blurredBackgroundImage = true,
            displayRestorePurchases = true,
        ),
        localization = mapOf(Locale.US.toString() to PaywallData.defaultLocalization),
        assetBaseURL = PaywallData.defaultTemplateBaseURL,
        revision = PaywallData.revisionID,
    )
}

// region Internal defaults

internal val PaywallData.Companion.defaultTemplate: PaywallTemplate
    get() = PaywallTemplate.TEMPLATE_2

internal val PaywallData.Companion.defaultAppIconPlaceholder: String // TODO-PAYWALLS: use real icon
    get() = "revenuecatui_default_paywall_app_icon"

// endregion

// region Private defaults

private val PaywallData.Companion.revisionID: Int
    get() = -1

private val PaywallData.Companion.defaultBackgroundImage: String
    get() = "default_background" // TODO-PAYWALLS: make sure this works

private val PaywallData.Companion.defaultLocalization: PaywallData.LocalizedConfiguration
    get() = PaywallData.LocalizedConfiguration(
        title = "{{ app_name }}",
        callToAction = "Continue",
        offerDetails = "{{ total_price_and_per_month }}",
        offerDetailsWithIntroOffer = "Start your {{ sub_offer_duration }} trial, then {{ total_price_and_per_month }}.",
    )

private val PaywallData.Companion.defaultTemplateBaseURL: URL
    get() = URL("")

private fun PaywallData.Companion.defaultColors(currentColors: Colors): PaywallData.Configuration.ColorInformation {
    return PaywallData.Configuration.ColorInformation(
        light = getThemeColors(background = PaywallColor(colorInt = Color.White.toArgb()), currentColors),
        dark = getThemeColors(background = PaywallColor(colorInt = Color.Black.toArgb()), currentColors),
    )
}

// endregion

private fun getThemeColors(
    background: PaywallColor,
    currentColors: Colors,
): PaywallData.Configuration.Colors {
    return PaywallData.Configuration.Colors(
        background = background,
        text1 = currentColors.primary.asPaywallColor(),
        callToActionBackground = currentColors.secondary.asPaywallColor(),
        callToActionForeground = background,
        accent1 = currentColors.secondary.asPaywallColor(),
        accent2 = currentColors.primary.asPaywallColor(),
    )
}

private fun Color.asPaywallColor(): PaywallColor = PaywallColor(colorInt = this.toArgb())
