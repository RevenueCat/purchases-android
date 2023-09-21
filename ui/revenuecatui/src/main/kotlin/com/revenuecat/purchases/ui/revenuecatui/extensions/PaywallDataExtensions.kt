package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.PaywallColor
import com.revenuecat.purchases.paywalls.PaywallData
import java.net.URL

/***
 * Default [PaywallData] to display when attempting to present a [PaywallView] with an offering that has no paywall
 * configuration, or when that configuration is invalid.
 */
@Composable
@ReadOnlyComposable
internal fun PaywallData.Companion.createDefault(packages: List<Package>): PaywallData {
    return PaywallData.createDefaultForIdentifiers(packages.map { it.identifier })
}

@Composable
@ReadOnlyComposable
private fun PaywallData.Companion.createDefaultForIdentifiers(packageIdentifiers: List<String>): PaywallData {
    val context = LocalContext.current
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
        assetBaseURL = PaywallData.defaultTemplateBaseURL(context.packageName),
        revision = PaywallData.revisionID,
    )
}

// region Private defaults

private val PaywallData.Companion.defaultTemplate: String // TODO-PAYWALLS: use enum
    get() = "2"

private val PaywallData.Companion.defaultAppIconPlaceholder: String // TODO-PAYWALLS: use real icon
    get() = "revenuecatui_default_paywall_app_icon"

private val PaywallData.Companion.revisionID: Int
    get() = -1

private val PaywallData.Companion.defaultBackgroundImage: String
    get() = "R.drawable.background"

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

private fun PaywallData.Companion.defaultTemplateBaseURL(packageName: String): URL =
    URL("android.resource://$packageName/")

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
