package com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates

import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.paywalls.PaywallColor
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import java.net.URL

internal val TestData.template1: PaywallData
    get() = PaywallData(
        templateName = PaywallTemplate.TEMPLATE_1.id,
        config = PaywallData.Configuration(
            packageIds = listOf(
                PackageType.MONTHLY.identifier!!,
            ),
            defaultPackage = PackageType.MONTHLY.identifier!!,
            images = TestData.Constants.images,
            blurredBackgroundImage = true,
            displayRestorePurchases = true,
            termsOfServiceURL = URL("https://revenuecat.com/tos"),
            colors = PaywallData.Configuration.ColorInformation(
                light = PaywallData.Configuration.Colors(
                    background = PaywallColor(stringRepresentation = "#FFFFFF"),
                    text1 = PaywallColor(stringRepresentation = "#000000"),
                    callToActionBackground = PaywallColor(stringRepresentation = "#5CD27A"),
                    callToActionForeground = PaywallColor(stringRepresentation = "#FFFFFF"),
                    accent1 = PaywallColor(stringRepresentation = "#BC66FF"),
                ),
                dark = PaywallData.Configuration.Colors(
                    background = PaywallColor(stringRepresentation = "#000000"),
                    text1 = PaywallColor(stringRepresentation = "#FFFFFF"),
                    callToActionBackground = PaywallColor(stringRepresentation = "#ACD27A"),
                    callToActionForeground = PaywallColor(stringRepresentation = "#000000"),
                    accent1 = PaywallColor(stringRepresentation = "#B022BB"),
                ),
            ),
        ),
        localization = mapOf(
            "en_US" to PaywallData.LocalizedConfiguration(
                title = "Ignite your child's curiosity",
                subtitle = "Get access to all our educational content trusted by thousands of parents.",
                callToAction = "Subscribe for {{ sub_price_per_month }}",
                callToActionWithIntroOffer = "Purchase for {{ sub_price_per_month }} per month",
                offerDetails = "{{ sub_price_per_month }} per month",
                offerDetailsWithIntroOffer = "Start your {{ sub_offer_duration }} trial, " +
                    "then {{ sub_price_per_month }} per month",
            ),
        ),
        assetBaseURL = TestData.Constants.assetBaseURL,
    )
