package com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates

import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.paywalls.PaywallColor
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import java.net.URL

internal val TestData.template4: PaywallData
    get() = PaywallData(
        templateName = PaywallTemplate.TEMPLATE_4.id,
        config = PaywallData.Configuration(
            packageIds = listOf(
                PackageType.MONTHLY.identifier!!,
                PackageType.SIX_MONTH.identifier!!,
                PackageType.ANNUAL.identifier!!,
                PackageType.WEEKLY.identifier!!,
            ),
            defaultPackage = PackageType.SIX_MONTH.identifier!!,
            images = PaywallData.Configuration.Images(
                background = "300883_1690710097.jpg",
            ),
            displayRestorePurchases = true,
            termsOfServiceURL = URL("https://revenuecat.com/tos"),
            privacyURL = URL("https://revenuecat.com/privacy"),
            colors = PaywallData.Configuration.ColorInformation(
                light = PaywallData.Configuration.Colors(
                    background = PaywallColor(stringRepresentation = "#FFFFFF"),
                    text1 = PaywallColor(stringRepresentation = "#111111"),
                    text2 = PaywallColor(stringRepresentation = "#333333"),
                    text3 = PaywallColor(stringRepresentation = "#999999"),
                    callToActionBackground = PaywallColor(stringRepresentation = "#06357D"),
                    callToActionForeground = PaywallColor(stringRepresentation = "#FFFFFF"),
                    accent1 = PaywallColor(stringRepresentation = "#D4B5FC"),
                    accent2 = PaywallColor(stringRepresentation = "#DFDFDF"),
                ),
            ),
        ),
        localization = mapOf(
            "en_US" to PaywallData.LocalizedConfiguration(
                title = "Get _unlimited_ access",
                callToAction = "Continue",
                offerDetails = "Cancel anytime",
                offerDetailsWithIntroOffer = "Includes {{ sub_offer_duration }} **free** trial",
                offerName = "{{ sub_duration_in_months }}",
            ),
        ),
        assetBaseURL = TestData.Constants.assetBaseURL,
        zeroDecimalPlaceCountries = listOf("PH", "KZ", "TW", "MX", "TH"),
    )
