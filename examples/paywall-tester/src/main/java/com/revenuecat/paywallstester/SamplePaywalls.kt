package com.revenuecat.paywallstester

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.paywalls.PaywallColor
import com.revenuecat.purchases.paywalls.PaywallData
import java.net.URL

class SamplePaywallsLoader {
    fun offeringForTemplate(template: SamplePaywalls.SampleTemplate): Offering {
        return Offering(
            "$SamplePaywalls.offeringIdentifier_${template.name}",
            SamplePaywalls.offeringIdentifier,
            emptyMap(),
            SamplePaywalls.packages,
            paywall = paywallForTemplate(template),
        )
    }

    private fun paywallForTemplate(template: SamplePaywalls.SampleTemplate): PaywallData {
        return when (template) {
            SamplePaywalls.SampleTemplate.TEMPLATE_1 -> SamplePaywalls.template1()
            SamplePaywalls.SampleTemplate.TEMPLATE_2 -> SamplePaywalls.template2()
            SamplePaywalls.SampleTemplate.TEMPLATE_3 -> SamplePaywalls.template3()
            SamplePaywalls.SampleTemplate.TEMPLATE_4 -> SamplePaywalls.template4()
            SamplePaywalls.SampleTemplate.TEMPLATE_5 -> SamplePaywalls.template5()
            SamplePaywalls.SampleTemplate.UNRECOGNIZED_TEMPLATE -> SamplePaywalls.unrecognizedTemplate()
        }
    }
}

@SuppressWarnings("LongMethod")
object SamplePaywalls {

    enum class SampleTemplate(val displayableName: String) {
        TEMPLATE_1("#1: Minimalist"),
        TEMPLATE_2("#2: Bold packages"),
        TEMPLATE_3("#3: Feature list"),
        TEMPLATE_4("#4: Horizontal packages"),
        TEMPLATE_5("#5: Minimalist with small banner"),
        UNRECOGNIZED_TEMPLATE("Default template"),
    }

    const val offeringIdentifier = "offering"

    private val tosURL = URL("https://revenuecat.com/tos")
    private val images = PaywallData.Configuration.Images(
        header = "9a17e0a7_1689854430..jpeg",
        background = "9a17e0a7_1689854342..jpg",
        icon = "9a17e0a7_1689854430..jpeg",
    )
    private val paywallAssetBaseURL = URL("https://assets.pawwalls.com")

    private val weeklyProduct = TestStoreProduct(
        id = "com.revenuecat.product_1",
        title = "Weekly",
        price = Price(amountMicros = 1_990_000, currencyCode = "USD", formatted = "$1.99"),
        description = "PRO Weekly",
        period = Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W"),
    )

    private val monthlyProduct = TestStoreProduct(
        id = "com.revenuecat.product_2",
        title = "Monthly",
        price = Price(amountMicros = 6_990_000, currencyCode = "USD", formatted = "$6.99"),
        description = "PRO Monthly",
        period = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
        freeTrialPeriod = Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W"),
    )

    private val sixMonthProduct = TestStoreProduct(
        id = "com.revenuecat.product_4",
        title = "Six Months",
        price = Price(amountMicros = 34_990_000, currencyCode = "USD", formatted = "$34.99"),
        description = "PRO Six Months",
        period = Period(value = 6, unit = Period.Unit.MONTH, iso8601 = "P6M"),
        freeTrialPeriod = Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W"),
    )

    private val annualProduct = TestStoreProduct(
        id = "com.revenuecat.product_3",
        title = "Annual",
        price = Price(amountMicros = 53_990_000, currencyCode = "USD", formatted = "$53.99"),
        description = "PRO Annual",
        period = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
        freeTrialPeriod = Period(value = 2, unit = Period.Unit.WEEK, iso8601 = "P2W"),
    )

    private val lifetimeProduct = TestStoreProduct(
        id = "com.revenuecat.product_lifetime",
        title = "Lifetime",
        price = Price(amountMicros = 119_490_000, currencyCode = "USD", formatted = "$119.49"),
        description = "Lifetime purchase",
        period = null,
    )

    private val weeklyPackage = Package(
        PackageType.WEEKLY.identifier!!,
        PackageType.WEEKLY,
        weeklyProduct,
        offeringIdentifier,
    )

    private val monthlyPackage = Package(
        PackageType.MONTHLY.identifier!!,
        PackageType.MONTHLY,
        monthlyProduct,
        offeringIdentifier,
    )

    private val sixMonthPackage = Package(
        PackageType.SIX_MONTH.identifier!!,
        PackageType.SIX_MONTH,
        sixMonthProduct,
        offeringIdentifier,
    )

    private val annualPackage = Package(
        PackageType.ANNUAL.identifier!!,
        PackageType.ANNUAL,
        annualProduct,
        offeringIdentifier,
    )

    private val lifetimePackage = Package(
        PackageType.LIFETIME.identifier!!,
        PackageType.LIFETIME,
        lifetimeProduct,
        offeringIdentifier,
    )

    val packages = listOf(
        weeklyPackage,
        monthlyPackage,
        sixMonthPackage,
        annualPackage,
        lifetimePackage,
    )

    fun template1(): PaywallData {
        return PaywallData(
            templateName = "1",
            config = PaywallData.Configuration(
                images = images,
                colors = PaywallData.Configuration.ColorInformation(
                    light = PaywallData.Configuration.Colors(
                        background = PaywallColor("#FFFFFF"),
                        text1 = PaywallColor("#000000"),
                        callToActionBackground = PaywallColor("#5CD27A"),
                        callToActionForeground = PaywallColor("#FFFFFF"),
                        accent1 = PaywallColor("#BC66FF"),
                    ),
                    dark = PaywallData.Configuration.Colors(
                        background = PaywallColor("#000000"),
                        text1 = PaywallColor("#FFFFFF"),
                        callToActionBackground = PaywallColor("#ACD27A"),
                        callToActionForeground = PaywallColor("#000000"),
                        accent1 = PaywallColor("#B022BB"),
                    ),
                ),
                termsOfServiceURL = tosURL,
                packageIds = listOf(PackageType.MONTHLY.identifier!!),
            ),
            assetBaseURL = paywallAssetBaseURL,
            localization = mapOf(
                "en_US" to PaywallData.LocalizedConfiguration(
                    title = "Ignite your child's curiosity",
                    subtitle = "Get access to **all our [educational content](https://rev.cat/paywalls)** " +
                        "trusted by _thousands_ of parents.",
                    callToAction = "Purchase for {{ price }}",
                    callToActionWithIntroOffer = "Purchase for {{ sub_price_per_month }} per month",
                    offerDetails = "{{ sub_price_per_month }} per month",
                    offerDetailsWithIntroOffer = "Start your {{ sub_offer_duration }} trial, " +
                        "then {{ sub_price_per_month }} per month",
                ),
            ),
        )
    }

    fun template2(): PaywallData {
        return PaywallData(
            templateName = "2",
            config = PaywallData.Configuration(
                images = images,
                colors = PaywallData.Configuration.ColorInformation(
                    light = PaywallData.Configuration.Colors(
                        background = PaywallColor("#FFFFFF"),
                        text1 = PaywallColor("#000000"),
                        callToActionBackground = PaywallColor("#EC807C"),
                        callToActionForeground = PaywallColor("#FFFFFF"),
                        accent1 = PaywallColor("#BC66FF"),
                        accent2 = PaywallColor("#222222"),
                    ),
                    dark = PaywallData.Configuration.Colors(
                        background = PaywallColor("#000000"),
                        text1 = PaywallColor("#FFFFFF"),
                        callToActionBackground = PaywallColor("#ACD27A"),
                        callToActionForeground = PaywallColor("#000000"),
                        accent1 = PaywallColor("#B022BB"),
                        accent2 = PaywallColor("#CCCCCC"),
                    ),
                ),
                termsOfServiceURL = tosURL,
                packageIds = listOf(
                    PackageType.WEEKLY.identifier!!,
                    PackageType.MONTHLY.identifier!!,
                    PackageType.ANNUAL.identifier!!,
                    PackageType.LIFETIME.identifier!!,
                ),
                blurredBackgroundImage = true,
            ),
            assetBaseURL = paywallAssetBaseURL,
            localization = mapOf(
                "en_US" to PaywallData.LocalizedConfiguration(
                    title = "Call to action for better conversion.",
                    subtitle = "Lorem ipsum is simply dummy text of the printing and typesetting industry.",
                    callToAction = "Subscribe for {{ price_per_period }}",
                    offerDetails = "{{ total_price_and_per_month }}",
                    offerDetailsWithIntroOffer = "{{ total_price_and_per_month }} after {{ sub_offer_duration }} trial",
                    offerName = "{{ sub_period }}",
                ),
                "es_ES" to PaywallData.LocalizedConfiguration(
                    title = "Despierta la curiosidad de tu hijo",
                    subtitle = "Accede a todo nuestro contenido educativo, confiado por miles de padres.",
                    callToAction = "Comprar",
                    offerDetails = "{{ total_price_and_per_month }}",
                    offerDetailsWithIntroOffer = "Comienza tu prueba de {{ sub_offer_duration }}, " +
                        "después {{ sub_price_per_month }} cada mes",
                    offerName = "{{ sub_period }}",
                ),
            ),
        )
    }

    fun template3(): PaywallData {
        return PaywallData(
            templateName = "3",
            config = PaywallData.Configuration(
                images = images,
                colors = PaywallData.Configuration.ColorInformation(
                    light = PaywallData.Configuration.Colors(
                        background = PaywallColor("#FAFAFA"),
                        text1 = PaywallColor("#000000"),
                        text2 = PaywallColor("#2A2A2A"),
                        callToActionBackground = PaywallColor("#222222"),
                        callToActionForeground = PaywallColor("#FFFFFF"),
                        accent1 = PaywallColor("#F4E971"),
                        accent2 = PaywallColor("#121212"),
                    ),
                    dark = PaywallData.Configuration.Colors(
                        background = PaywallColor("#272727"),
                        text1 = PaywallColor("#FFFFFF"),
                        text2 = PaywallColor("#B7B7B7"),
                        callToActionBackground = PaywallColor("#FFFFFF"),
                        callToActionForeground = PaywallColor("#000000"),
                        accent1 = PaywallColor("#F4E971"),
                        accent2 = PaywallColor("#4A4A4A"),
                    ),
                ),
                termsOfServiceURL = tosURL,
                packageIds = listOf(PackageType.ANNUAL.identifier!!),
            ),
            assetBaseURL = paywallAssetBaseURL,
            localization = mapOf(
                "en_US" to PaywallData.LocalizedConfiguration(
                    title = "How your free trial works",
                    callToAction = "Start",
                    callToActionWithIntroOffer = "Start your {{ sub_offer_duration }} free",
                    offerDetails = "Only {{ price_per_period }}",
                    offerDetailsWithIntroOffer = "First {{ sub_offer_duration }} free, then\n{{ price }} per year" +
                        " ({{ sub_price_per_month }} per month)",
                    features = listOf(
                        PaywallData.LocalizedConfiguration.Feature(
                            title = "Today",
                            content = "Full access to 1000+ workouts plus free meal plan worth $49.99.",
                            iconID = "tick",
                        ),
                        PaywallData.LocalizedConfiguration.Feature(
                            title = "Day 7",
                            content = "Get a reminder about when your trial is about to end.",
                            iconID = "notification",
                        ),
                        PaywallData.LocalizedConfiguration.Feature(
                            title = "Day 14",
                            content = "You'll automatically get subscribed. Cancel anytime before if you " +
                                "didn't love our app.",
                            iconID = "attachment",
                        ),

                    ),
                ),
            ),
        )
    }

    fun template4(): PaywallData {
        return PaywallData(
            templateName = "4",
            config = PaywallData.Configuration(
                images = PaywallData.Configuration.Images(
                    background = "300883_1690710097.jpg",
                ),
                colors = PaywallData.Configuration.ColorInformation(
                    light = PaywallData.Configuration.Colors(
                        background = PaywallColor("#FFFFFF"),
                        text1 = PaywallColor("#111111"),
                        callToActionBackground = PaywallColor("#06357D"),
                        callToActionForeground = PaywallColor("#FFFFFF"),
                        accent1 = PaywallColor("#D4B5FC"),
                        accent2 = PaywallColor("#DFDFDF"),
                    ),
                ),
                termsOfServiceURL = tosURL,
                packageIds = listOf(
                    PackageType.MONTHLY.identifier!!,
                    PackageType.SIX_MONTH.identifier!!,
                    PackageType.ANNUAL.identifier!!,
                    PackageType.LIFETIME.identifier!!,
                ),
                defaultPackage = PackageType.SIX_MONTH.identifier!!,
            ),
            assetBaseURL = paywallAssetBaseURL,
            localization = mapOf(
                "en_US" to PaywallData.LocalizedConfiguration(
                    title = "Get _unlimited_ access",
                    callToAction = "Continue",
                    offerDetails = "Cancel anytime",
                    offerDetailsWithIntroOffer = "Includes {{ sub_offer_duration }} **free** trial",
                    offerName = "{{ sub_duration_in_months }}",
                ),
            ),
        )
    }

    fun template5(): PaywallData {
        return PaywallData(
            templateName = "5",
            config = PaywallData.Configuration(
                packageIds = listOf(
                    PackageType.ANNUAL.identifier!!,
                    PackageType.MONTHLY.identifier!!,
                ),
                defaultPackage = PackageType.MONTHLY.identifier!!,
                images = PaywallData.Configuration.Images(
                    header = "954459_1692992845.png",
                ),
                displayRestorePurchases = true,
                termsOfServiceURL = URL("https://revenuecat.com/tos"),
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
            assetBaseURL = paywallAssetBaseURL,
            localization = mapOf(
                "en_US" to PaywallData.LocalizedConfiguration(
                    title = "Spice Up Your Kitchen - Go Pro for Exclusive Benefits!",
                    callToAction = "Continue",
                    callToActionWithIntroOffer = "Start your Free Trial",
                    offerDetails = "{{ total_price_and_per_month }}",
                    offerDetailsWithIntroOffer = "Free for {{ sub_offer_duration }}, " +
                        "then {{ total_price_and_per_month }}",
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
        )
    }

    fun unrecognizedTemplate(): PaywallData {
        return PaywallData(
            templateName = "unrecognized",
            config = template4().config,
            assetBaseURL = paywallAssetBaseURL,
            localization = mapOf(
                "en_US" to template4().localizedConfiguration.second,
            ),
        )
    }
}
