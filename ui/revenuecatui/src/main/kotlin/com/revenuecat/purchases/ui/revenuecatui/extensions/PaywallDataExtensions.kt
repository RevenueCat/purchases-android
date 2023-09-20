package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.os.LocaleListCompat
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.PaywallColor
import com.revenuecat.purchases.paywalls.PaywallData
import java.net.URL

@Composable
@ReadOnlyComposable
internal fun PaywallData.Companion.createDefault(packages: List<Package>): PaywallData {
    return PaywallData.createDefault(packages.map { it.identifier })
}

@Composable
@ReadOnlyComposable
internal fun PaywallData.Companion.createDefault(packageIdentifiers: List<String>): PaywallData {
    val locale = LocaleListCompat.getDefault()[0].toString()

    return PaywallData(
        templateName = PaywallData.defaultTemplate,
        config = PaywallData.Configuration(
            packages = packageIdentifiers,
            images = PaywallData.Configuration.Images(
                background = PaywallData.defaultBackgroundImage,
                icon = PaywallData.defaultAppIconPlaceholder,
            ),
            colors = PaywallData.defaultColors,
            blurredBackgroundImage = true,
            displayRestorePurchases = true,
        ),
        localization = mapOf(locale to PaywallData.defaultLocalization), // TODO-PAYWALLS: test this
        assetBaseURL = PaywallData.defaultTemplateBaseURL, // TODO-PAYWALLS: we need to load a resource not a URL
        revision = PaywallData.revisionID,
    )
}

// region Internal defaults

internal val PaywallData.Companion.defaultTemplate: String // TODO-PAYWALLS: use enum
    get() = "2"

internal val PaywallData.Companion.defaultAppIconPlaceholder: String // TODO-PAYWALLS: use real icon
    get() = "revenuecatui_default_paywall_app_icon"

internal val PaywallData.Companion.revisionID: Int
    get() = -1

// endregion

// region Private defaults

private val PaywallData.Companion.defaultBackgroundImage: String
    get() = "background.jpg"

private val PaywallData.Companion.defaultTemplateBaseURL: URL // TODO-PAYWALLS: use real URL
    get() = URL("https://")

private val PaywallData.Companion.defaultColors: PaywallData.Configuration.ColorInformation
    @Composable
    @ReadOnlyComposable
    get() {
        return PaywallData.Configuration.ColorInformation(
            light = getCurrentColors(background = PaywallColor(colorInt = Color.White.toArgb())),
            dark = getCurrentColors(background = PaywallColor(colorInt = Color.Black.toArgb())),
        )
    }

private val PaywallData.Companion.defaultLocalization: PaywallData.LocalizedConfiguration
    get() = PaywallData.LocalizedConfiguration(
        title = "{{ app_name }}",
        callToAction = "Continue",
        offerDetails = "{{ total_price_and_per_month }}",
        offerDetailsWithIntroOffer = "Start your {{ sub_offer_duration }} trial, then {{ total_price_and_per_month }}.",
    )

// endregion

@Composable
@ReadOnlyComposable
private fun getCurrentColors(
    background: PaywallColor,
): PaywallData.Configuration.Colors {
    return PaywallData.Configuration.Colors(
        background = background,
        text1 = MaterialTheme.colors.primary.asPaywallColor(),
        callToActionBackground = MaterialTheme.colors.secondary.asPaywallColor(),
        callToActionForeground = background,
        accent1 = MaterialTheme.colors.secondary.asPaywallColor(),
        accent2 = MaterialTheme.colors.primary.asPaywallColor(),
    )
}

private fun Color.asPaywallColor(): PaywallColor = PaywallColor(colorInt = this.toArgb())
