package com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates

import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.paywalls.PaywallColor
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import java.net.URL

internal val TestData.template3: PaywallData
    get() = PaywallData(
        templateName = "3", // TODO-PAYWALLS: use enum
        config = PaywallData.Configuration(
            packages = listOf(
                PackageType.MONTHLY.identifier!!,
            ),
            images = PaywallData.Configuration.Images(
                icon = "954459_1695680948.jpg",
            ),
            displayRestorePurchases = true,
            termsOfServiceURL = URL("https://revenuecat.com/tos"),
            privacyURL = URL("https://revenuecat.com/privacy"),
            colors = PaywallData.Configuration.ColorInformation(
                light = PaywallData.Configuration.Colors(
                    background = PaywallColor(stringRepresentation = "#FFFFFF"),
                    text1 = PaywallColor(stringRepresentation = "#000000"),
                    text2 = PaywallColor(stringRepresentation = "#B7B7B7"),
                    callToActionBackground = PaywallColor(stringRepresentation = "#EC807C"),
                    callToActionForeground = PaywallColor(stringRepresentation = "#FFFFFF"),
                    accent1 = PaywallColor(stringRepresentation = "#BC66FF"),
                    accent2 = PaywallColor(stringRepresentation = "#222222"),
                ),
                dark = PaywallData.Configuration.Colors(
                    background = PaywallColor(stringRepresentation = "#1c1c1c"),
                    text1 = PaywallColor(stringRepresentation = "#ffffff"),
                    text2 = PaywallColor(stringRepresentation = "#B7B7B7"),
                    callToActionBackground = PaywallColor(stringRepresentation = "#45c186"),
                    callToActionForeground = PaywallColor(stringRepresentation = "#FFFFFF"),
                    accent1 = PaywallColor(stringRepresentation = "#F4E971"),
                    accent2 = PaywallColor(stringRepresentation = "#4a4a4a"),
                ),
            ),
        ),
        localization = mapOf(
            "en_US" to PaywallData.LocalizedConfiguration(
                title = "How your free trial works",
                callToAction = "Start",
                features = listOf(
                    PaywallData.LocalizedConfiguration.Feature(
                        title = "Today",
                        content = "Full access to 1000+ workouts plus free meal plan worth {{ price }}.",
                        iconID = "tick",
                    ),
                    PaywallData.LocalizedConfiguration.Feature(
                        title = "Day 7",
                        content = "Get a reminder about when your trial is about to end.",
                        iconID = "notification",
                    ),
                    PaywallData.LocalizedConfiguration.Feature(
                        title = "Day 14",
                        content = "You'll automatically get subscribed. " +
                            "Cancel anytime before if you didn't love our app.",
                        iconID = "attachment",
                    ),
                    PaywallData.LocalizedConfiguration.Feature(
                        title = "Today",
                        content = "Full access to 1000+ workouts plus free meal plan worth {{ price }}.",
                        iconID = "tick",
                    ),
                    PaywallData.LocalizedConfiguration.Feature(
                        title = "Day 7",
                        content = "Get a reminder about when your trial is about to end.",
                        iconID = "notification",
                    ),
                    PaywallData.LocalizedConfiguration.Feature(
                        title = "Day 14",
                        content = "You'll automatically get subscribed. " +
                            "Cancel anytime before if you didn't love our app.",
                        iconID = "attachment",
                    ),
                    PaywallData.LocalizedConfiguration.Feature(
                        title = "Day 14",
                        content = "You'll automatically get subscribed. " +
                            "Cancel anytime before if you didn't love our app.",
                        iconID = "attachment",
                    ),
                ),
                offerDetails = "{{ total_price_and_per_month }}",
                offerDetailsWithIntroOffer = "{{ total_price_and_per_month }} after {{ sub_offer_duration }} trial",
            ),
            "es_ES" to PaywallData.LocalizedConfiguration(
                title = "Cómo funciona tu prueba gratuita",
                callToAction = "Comenzar",
                features = listOf(
                    PaywallData.LocalizedConfiguration.Feature(
                        title = "Hoy",
                        content = "Acceso completo a más de 1000 entrenamientos más un plan de comidas gratuito " +
                            "valorado en {{ price }}.",
                        iconID = "tick",
                    ),
                    PaywallData.LocalizedConfiguration.Feature(
                        title = "Día 7",
                        content = "Recibirás un recordatorio cuando tu prueba esté a punto de finalizar.",
                        iconID = "notification",
                    ),
                    PaywallData.LocalizedConfiguration.Feature(
                        title = "Día 14",
                        content = "Serás suscrito automáticamente. Cancela en cualquier momento antes si no te " +
                            "encanta nuestra aplicación.",
                        iconID = "attachment",
                    ),
                ),
                offerDetails = "{{ total_price_and_per_month }}",
                offerDetailsWithIntroOffer = "{{ total_price_and_per_month }} después de" +
                    " {{ sub_offer_duration }} de prueba",
            ),
        ),
        assetBaseURL = TestData.Constants.assetBaseURL,
    )
