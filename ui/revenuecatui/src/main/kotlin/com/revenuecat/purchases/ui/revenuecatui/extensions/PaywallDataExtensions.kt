package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.PaywallColor
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.InternalPaywallView
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewOptions
import com.revenuecat.purchases.ui.revenuecatui.data.TestData
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import java.net.URL
import java.util.Locale

/***
 * Default [PaywallData] to display when attempting to present a PaywallView with an offering that has no paywall
 * configuration, or when that configuration is invalid.
 */
internal fun PaywallData.Companion.createDefault(
    packages: List<Package>,
    currentColorScheme: ColorScheme,
): PaywallData {
    return PaywallData.createDefaultForIdentifiers(packages.map { it.identifier }, currentColorScheme)
}

internal fun PaywallData.Companion.createDefaultForIdentifiers(
    packageIdentifiers: List<String>,
    currentColors: ColorScheme,
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
    get() = URL("https://")

private fun PaywallData.Companion.defaultColors(
    currentColorScheme: ColorScheme,
): PaywallData.Configuration.ColorInformation {
    return PaywallData.Configuration.ColorInformation(
        light = getThemeColors(background = PaywallColor(colorInt = Color.White.toArgb()), currentColorScheme),
        dark = getThemeColors(background = PaywallColor(colorInt = Color.Black.toArgb()), currentColorScheme),
    )
}

// endregion

private fun getThemeColors(
    background: PaywallColor,
    currentColorScheme: ColorScheme,
): PaywallData.Configuration.Colors {
    return PaywallData.Configuration.Colors(
        background = background,
        text1 = currentColorScheme.primary.asPaywallColor(),
        callToActionBackground = currentColorScheme.secondary.asPaywallColor(),
        callToActionForeground = background,
        accent1 = currentColorScheme.secondary.asPaywallColor(),
        accent2 = currentColorScheme.primary.asPaywallColor(),
    )
}

private fun Color.asPaywallColor(): PaywallColor = PaywallColor(colorInt = this.toArgb())

@Preview(showBackground = true, locale = "en-rUS")
@Preview(showBackground = true, locale = "es-rES")
@Composable
internal fun Template2PaywallPreview() {
    val availablePackages = listOf(
        TestData.Packages.weekly,
        TestData.Packages.monthly,
        TestData.Packages.annual,
    )
    val paywallData = PaywallData.createDefault(
        availablePackages,
        MaterialTheme.colorScheme,
    )
    val template2Offering = Offering(
        identifier = "Template2",
        availablePackages = availablePackages,
        metadata = mapOf(),
        paywall = paywallData,
        serverDescription = "",
    )
    InternalPaywallView(options = PaywallViewOptions.Builder().setOffering(template2Offering).build())
}
