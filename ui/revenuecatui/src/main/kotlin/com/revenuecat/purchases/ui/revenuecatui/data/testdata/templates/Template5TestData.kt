package com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates

import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.paywalls.PaywallColor
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import java.net.URL

internal val TestData.template5: PaywallData
    get() = PaywallData(
        templateName = PaywallTemplate.TEMPLATE_5.id,
        config = PaywallData.Configuration(
            packageIds = listOf(
                PackageType.ANNUAL.identifier!!,
                PackageType.MONTHLY.identifier!!,
            ),
            defaultPackage = PackageType.ANNUAL.identifier!!,
            images = PaywallData.Configuration.Images(
                header = "954459_1692992845.png",
            ),
            displayRestorePurchases = true,
            privacyURL = URL("https://revenuecat.com/privacy"),
            colors = PaywallData.Configuration.ColorInformation(
                light = PaywallData.Configuration.Colors(
                    background = PaywallColor(stringRepresentation = "#FFFFFF"),
                    text1 = PaywallColor(stringRepresentation = "#000000"),
                    text2 = PaywallColor(stringRepresentation = "#adf5c5"),
                    text3 = PaywallColor(stringRepresentation = "#b15d5d"),
                    callToActionBackground = PaywallColor(stringRepresentation = "#45c186"),
                    callToActionForeground = PaywallColor(stringRepresentation = "#ffffff"),
                    accent1 = PaywallColor(stringRepresentation = "#b24010"),
                    accent2 = PaywallColor(stringRepresentation = "#027424"),
                    accent3 = PaywallColor(stringRepresentation = "#D1D1D1"),
                ),
            ),
        ),
        localization = mapOf(
            "en_US" to PaywallData.LocalizedConfiguration(
                title = "Spice Up Your Kitchen - Go Pro for Exclusive Benefits!",
                callToAction = "Continue",
                callToActionWithIntroOffer = "Start your Free Trial",
                offerDetails = "{{ total_price_and_per_month }}",
                offerDetailsWithIntroOffer = "Free for {{ sub_offer_duration }}, then {{ total_price_and_per_month }}",
                offerName = "{{ sub_period }}",
                features = listOf(
                    PaywallData.LocalizedConfiguration.Feature(
                        title = "Unique gourmet recipes",
                        iconID = "tick",
                    ),
                    PaywallData.LocalizedConfiguration.Feature(
                        title = "Advanced nutritional recipes",
                        iconID = "apple",
                    ),
                    PaywallData.LocalizedConfiguration.Feature(
                        title = "Personalized support from our Chef",
                        iconID = "warning",
                    ),
                    PaywallData.LocalizedConfiguration.Feature(
                        title = "Unlimited receipt collections",
                        iconID = "bookmark",
                    ),
                ),
            ),
        ),
        assetBaseURL = TestData.Constants.assetBaseURL,
    )
