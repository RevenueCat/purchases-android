package com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates

import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.paywalls.PaywallColor
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import java.net.URL

internal val TestData.template7CustomPackages: PaywallData
    get() = PaywallData(
        templateName = "7",
        config = PaywallData.Configuration(
            packageIds = emptyList(),
            imagesByTier = mapOf(
                "basic" to PaywallData.Configuration.Images(
                    header = "954459_1703109702.png",
                ),
                "premium" to PaywallData.Configuration.Images(
                    header = "954459_1701267532.jpeg",
                ),
            ),
            tiers = listOf(
                PaywallData.Configuration.Tier(
                    id = "basic",
                    packageIds = listOf(
                        PackageType.ANNUAL.identifier!!,
                        PackageType.MONTHLY.identifier!!,
                        PackageType.SIX_MONTH.identifier!!,
                    ),
                    defaultPackageId = PackageType.ANNUAL.identifier!!,
                ),
                PaywallData.Configuration.Tier(
                    id = "premium",
                    packageIds = listOf(
                        PackageType.LIFETIME.identifier!!,
                    ),
                    defaultPackageId = PackageType.THREE_MONTH.identifier!!,
                ),
            ),
            displayRestorePurchases = true,
            termsOfServiceURL = URL("https://revenuecat.com/tos"),
            privacyURL = URL("https://revenuecat.com/privacy"),

            colors = PaywallData.Configuration.ColorInformation(
                light = PaywallData.Configuration.Colors(
                    background = PaywallColor(stringRepresentation = "#FFFFFF"),
                    text1 = PaywallColor(stringRepresentation = "#000000"),
                    callToActionBackground = PaywallColor(stringRepresentation = "#45c186"),
                    callToActionForeground = PaywallColor(stringRepresentation = "#ffffff"),
                ),
            ),

            colorsByTier = mapOf(
                "basic" to PaywallData.Configuration.ColorInformation(
                    light = PaywallData.Configuration.Colors(
                        background = PaywallColor(stringRepresentation = "#FFFFFF"),
                        text1 = PaywallColor(stringRepresentation = "#000000"),
                        text2 = PaywallColor(stringRepresentation = "#ffffff"),
                        text3 = PaywallColor(stringRepresentation = "#30A0F8AA"),
                        callToActionBackground = PaywallColor(stringRepresentation = "#3fc1f7"),
                        callToActionForeground = PaywallColor(stringRepresentation = "#ffffff"),
                        accent1 = PaywallColor(stringRepresentation = "#2d7fc1"),
                        accent2 = PaywallColor(stringRepresentation = "#7676801F"),
                        accent3 = PaywallColor(stringRepresentation = "#100031"),
                        tierControlBackground = PaywallColor(stringRepresentation = "#eeeef0"),
                        tierControlForeground = PaywallColor(stringRepresentation = "#000000"),
                        tierControlSelectedBackground = PaywallColor(stringRepresentation = "#2d7fc1"),
                        tierControlSelectedForeground = PaywallColor(stringRepresentation = "#000000"),
                    ),
                ),
                "premium" to PaywallData.Configuration.ColorInformation(
                    light = PaywallData.Configuration.Colors(
                        background = PaywallColor(stringRepresentation = "#FFFFFF"),
                        text1 = PaywallColor(stringRepresentation = "#000000"),
                        text2 = PaywallColor(stringRepresentation = "#ffffff"),
                        text3 = PaywallColor(stringRepresentation = "#30A0F8AA"),
                        callToActionBackground = PaywallColor(stringRepresentation = "#94d269"),
                        callToActionForeground = PaywallColor(stringRepresentation = "#ffffff"),
                        accent1 = PaywallColor(stringRepresentation = "#76c343"),
                        accent2 = PaywallColor(stringRepresentation = "#7676801F"),
                        accent3 = PaywallColor(stringRepresentation = "#213711"),
                        tierControlBackground = PaywallColor(stringRepresentation = "#eeeef0"),
                        tierControlForeground = PaywallColor(stringRepresentation = "#000000"),
                        tierControlSelectedBackground = PaywallColor(stringRepresentation = "#76c343"),
                        tierControlSelectedForeground = PaywallColor(stringRepresentation = "#000000"),
                    ),
                ),
            ),
        ),
        assetBaseURL = TestData.Constants.assetBaseURL,
        localization = mapOf(
            "en_US" to PaywallData.LocalizedConfiguration(
                title = "Get started with our Basic plan",
                callToAction = "{{ price_per_period }}",
                offerDetails = "{{ total_price_and_per_month }}",
                offerName = "{{ sub_period }}",
            ),
        ),
        localizationByTier = mapOf(
            "en_US" to mapOf(
                "basic" to PaywallData.LocalizedConfiguration(
                    title = "Get started with our Basic plan",
                    callToAction = "Subscribe for {{ price_per_period }}",
                    callToActionWithIntroOffer = "Start your {{ sub_offer_duration }} free trial",
                    offerDetails = "{{ total_price_and_per_month }}",
                    offerDetailsWithIntroOffer = "{{ total_price_and_per_month }} after {{ sub_offer_duration }} trial",
                    offerName = "{{ sub_period }}",
                    features = listOf(
                        PaywallData.LocalizedConfiguration.Feature(
                            title = "Access to all 150 of our cinematic LUTs",
                            iconID = "tick",
                        ),
                    ),
                    tierName = "Basic",
                    offerOverrides = mapOf(
                        PackageType.MONTHLY.identifier!! to PaywallData.LocalizedConfiguration.OfferOverride(
                            offerName = "{{ sub_period }}",
                            offerDetails = "{{ total_price_and_per_month }}",
                            offerBadge = null,
                        ),
                        PackageType.ANNUAL.identifier!! to PaywallData.LocalizedConfiguration.OfferOverride(
                            offerName = "NAME OVERRIDE {{ sub_period }}",
                            offerDetails = "DETAIL OVERRIDE {{ total_price_and_per_month }}",
                            offerBadge = null,
                        ),
                        PackageType.SIX_MONTH.identifier!! to PaywallData.LocalizedConfiguration.OfferOverride(
                            offerName = "NAME OVERRIDE {{ sub_period }}",
                            offerDetails = "DETAIL OVERRIDE {{ total_price_and_per_month }}",
                            offerBadge = "BADGE OVERRIDE {{ sub_relative_discount }}",
                        ),
                    )
                ),
                "premium" to PaywallData.LocalizedConfiguration(
                    title = "Master the art of video editing",
                    callToAction = "Subscribe for {{ price_per_period }}",
                    callToActionWithIntroOffer = "Start your {{ sub_offer_duration }} free trial",
                    offerDetails = "{{ total_price_and_per_month }}",
                    offerDetailsWithIntroOffer = "{{ total_price_and_per_month }} after {{ sub_offer_duration }} trial",
                    offerName = "{{ sub_period }}",
                    features = listOf(
                        PaywallData.LocalizedConfiguration.Feature(
                            title = "Access to all 150 of our cinematic LUTs",
                            iconID = "tick",
                        ),
                        PaywallData.LocalizedConfiguration.Feature(
                            title = "Custom design tools and transition effects",
                            iconID = "tick",
                        ),
                        PaywallData.LocalizedConfiguration.Feature(
                            title = "100+ exclusive templates",
                            iconID = "tick",
                        ),
                    ),
                    tierName = "Premium",
                ),
            ),
        ),
    )
