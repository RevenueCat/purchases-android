package com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates

import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.paywalls.PaywallColor
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import java.net.URL

internal val TestData.template3: PaywallData
    get() = PaywallData(
        templateName = PaywallTemplate.TEMPLATE_3.id,
        config = PaywallData.Configuration(
            packageIds = listOf(
                PackageType.MONTHLY.identifier!!,
            ),
            images = PaywallData.Configuration.Images(
                icon = "954459_1695680948.jpg",
            ),
            displayRestorePurchases = true,
            termsOfServiceURL = URL("https://revenuecat.com/tos"),
            colors = PaywallData.Configuration.ColorInformation(
                light = PaywallData.Configuration.Colors(
                    background = PaywallColor(stringRepresentation = "#FAFAFA"),
                    text1 = PaywallColor(stringRepresentation = "#000000"),
                    text2 = PaywallColor(stringRepresentation = "#2A2A2A"),
                    callToActionBackground = PaywallColor(stringRepresentation = "#222222"),
                    callToActionForeground = PaywallColor(stringRepresentation = "#FFFFFF"),
                    accent1 = PaywallColor(stringRepresentation = "#F4E971"),
                    accent2 = PaywallColor(stringRepresentation = "#121212"),
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
                callToActionWithIntroOffer = "Start your {{ sub_offer_duration }} free",
                features = listOf(
                    PaywallData.LocalizedConfiguration.Feature(
                        title = "Today",
                        content = "**Full** access to ~~100~~ 1000+ workouts plus free meal plan worth _{{ price }}_.",
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
                ),
                offerDetails = "Only {{ price_per_period }}",
                offerDetailsWithIntroOffer = "First {{ sub_offer_duration }} free, then\n" +
                    "{{ price }} per year ({{ sub_price_per_month }} per month)",
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
