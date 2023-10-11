package com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates

import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.paywalls.PaywallColor
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import java.net.URL

internal val TestData.template2: PaywallData
    get() = PaywallData(
        templateName = PaywallTemplate.TEMPLATE_2.id,
        config = PaywallData.Configuration(
            packageIds = listOf(
                PackageType.ANNUAL.identifier!!,
                PackageType.MONTHLY.identifier!!,
            ),
            defaultPackage = PackageType.MONTHLY.identifier!!,
            images = TestData.Constants.images,
            blurredBackgroundImage = true,
            displayRestorePurchases = true,
            termsOfServiceURL = URL("https://revenuecat.com/tos"),
            privacyURL = URL("https://revenuecat.com/privacy"),
            colors = PaywallData.Configuration.ColorInformation(
                light = PaywallData.Configuration.Colors(
                    background = PaywallColor(stringRepresentation = "#FFFFFF"),
                    text1 = PaywallColor(stringRepresentation = "#000000"),
                    callToActionBackground = PaywallColor(stringRepresentation = "#EC807C"),
                    callToActionForeground = PaywallColor(stringRepresentation = "#FFFFFF"),
                    accent1 = PaywallColor(stringRepresentation = "#BC66FF"),
                    accent2 = PaywallColor(stringRepresentation = "#222222"),
                ),
            ),
        ),
        localization = mapOf(
            "en_US" to PaywallData.LocalizedConfiguration(
                title = "Call to **action** for _better_ conversion.",
                subtitle = "**Lorem ipsum** is simply ~dummy~ text of the _printing_ and *typesetting* industry.",
                callToAction = "Subscribe for {{ price_per_period }}",
                offerDetails = "{{ total_price_and_per_month }}",
                offerDetailsWithIntroOffer = "{{ total_price_and_per_month }} after {{ sub_offer_duration }} trial",
                offerName = "{{ sub_period }}",
            ),
            "es_ES" to PaywallData.LocalizedConfiguration(
                title = "Título en español",
                subtitle = "Un lorem ipsum en español que es más largo para mostrar un subtítulo multilinea.",
                callToAction = "Suscribete for {{ price_per_period }}",
                offerDetails = "{{ total_price_and_per_month }}",
                offerDetailsWithIntroOffer = "{{ total_price_and_per_month }} con {{ sub_offer_duration }} de prueba",
                offerName = "{{ sub_period }}",
            ),
        ),
        assetBaseURL = TestData.Constants.assetBaseURL,
    )
