package com.revenuecat.purchases.ui.revenuecatui.data.testdata.offerings

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.paywalls.PaywallColor
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import java.net.URL

internal val TestData.offeringWithMultiPackagePaywall: Offering
    get() = Offering(
        identifier = "offeringWithMultiPackagePaywall",
        serverDescription = "Offering",
        metadata = mapOf(),
        paywall = PaywallData(
            templateName = PaywallTemplate.TEMPLATE_2.id,
            config = PaywallData.Configuration(
                packages = listOf(PackageType.ANNUAL.identifier!!, PackageType.MONTHLY.identifier!!),
                images = TestData.Constants.images,
                colors = PaywallData.Configuration.ColorInformation(
                    light = PaywallData.Configuration.Colors(
                        background = PaywallColor("#FFFFFF"),
                        text1 = PaywallColor("#111111"),
                        callToActionBackground = PaywallColor("#EC807C"),
                        callToActionForeground = PaywallColor("#FFFFFF"),
                        accent1 = PaywallColor("#BC66FF"),
                        accent2 = PaywallColor("#111100"),
                    ),
                    dark = PaywallData.Configuration.Colors(
                        background = PaywallColor("#000000"),
                        text1 = PaywallColor("#EEEEEE"),
                        callToActionBackground = PaywallColor("#ACD27A"),
                        callToActionForeground = PaywallColor("#000000"),
                        accent1 = PaywallColor("#B022BB"),
                        accent2 = PaywallColor("#EEDDEE"),
                    ),
                ),
                blurredBackgroundImage = true,
                privacyURL = URL("https://revenuecat.com/tos"),
            ),
            localization = mapOf("en_US" to TestData.Constants.localization),
            assetBaseURL = TestData.Constants.assetBaseURL,
        ),
        availablePackages = listOf(
            TestData.Packages.weekly,
            TestData.Packages.monthly,
            TestData.Packages.annual,
        ),
    )
