@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.paywallstester

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.revenuecat.paywallstester.paywalls.tabsWithButtons
import com.revenuecat.paywallstester.paywalls.tabsWithToggle
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.UiConfig.AppConfig.FontsConfig
import com.revenuecat.purchases.UiConfig.AppConfig.FontsConfig.FontInfo
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.paywalls.PaywallColor
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Dimension.Vertical
import com.revenuecat.purchases.paywalls.components.properties.Dimension.ZLayer
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution.END
import com.revenuecat.purchases.paywalls.components.properties.FontWeight
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment.LEADING
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Shape
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment.BOTTOM
import java.net.URL

class SamplePaywallsLoader {
    private val primaryLocalFont = FontAlias("primary")
    private val secondaryGoogleFont = FontAlias("secondary")

    fun offeringForTemplate(template: SamplePaywalls.SampleTemplate): Offering {
        val paywall = paywallForTemplate(template)
        val localeId = when (paywall) {
            is SampleData.Legacy -> LocaleId("en_US")
            is SampleData.Components -> paywall.data.defaultLocaleIdentifier
        }

        return Offering(
            "$SamplePaywalls.offeringIdentifier_${template.name}",
            SamplePaywalls.offeringIdentifier,
            emptyMap(),
            SamplePaywalls.packages,
            paywall = (paywall as? SampleData.Legacy)?.data,
            paywallComponents = (paywall as? SampleData.Components)?.data?.let { data ->
                Offering.PaywallComponents(
                    uiConfig = UiConfig(
                        app = UiConfig.AppConfig(
                            fonts = mapOf(
                                primaryLocalFont to FontsConfig(android = FontInfo.Name("lobster_two")),
                                secondaryGoogleFont to FontsConfig(android = FontInfo.GoogleFonts("Barrio")),
                            ),
                        ),
                        localizations = mapOf(localeId to variableLocalizationKeysForEnUs()),
                    ),
                    data = data,
                )
            },
        )
    }

    private fun paywallForTemplate(template: SamplePaywalls.SampleTemplate): SampleData {
        return when (template) {
            SamplePaywalls.SampleTemplate.TEMPLATE_1 -> SamplePaywalls.template1()
            SamplePaywalls.SampleTemplate.TEMPLATE_2 -> SamplePaywalls.template2()
            SamplePaywalls.SampleTemplate.TEMPLATE_3 -> SamplePaywalls.template3()
            SamplePaywalls.SampleTemplate.TEMPLATE_4 -> SamplePaywalls.template4()
            SamplePaywalls.SampleTemplate.TEMPLATE_5 -> SamplePaywalls.template5()
            SamplePaywalls.SampleTemplate.TEMPLATE_7 -> SamplePaywalls.template7()
            SamplePaywalls.SampleTemplate.COMPONENTS_BLESS -> SamplePaywalls.bless()
            SamplePaywalls.SampleTemplate.COMPONENTS_BLESS_LOCAL_FONT -> SamplePaywalls.bless(font = primaryLocalFont)
            SamplePaywalls.SampleTemplate.COMPONENTS_BLESS_GOOGLE_FONT ->
                SamplePaywalls.bless(font = secondaryGoogleFont)

            SamplePaywalls.SampleTemplate.TABS_BUTTONS -> tabsWithButtons()
            SamplePaywalls.SampleTemplate.TABS_TOGGLE -> tabsWithToggle()
            SamplePaywalls.SampleTemplate.UNRECOGNIZED_TEMPLATE -> SamplePaywalls.unrecognizedTemplate()
        }
    }
}

sealed interface SampleData {
    data class Legacy(val data: PaywallData) : SampleData

    data class Components(val data: PaywallComponentsData) : SampleData
}

@SuppressWarnings("LongMethod", "LargeClass")
object SamplePaywalls {

    enum class SampleTemplate(val displayableName: String) {
        TEMPLATE_1("#1: Minimalist"),
        TEMPLATE_2("#2: Bold packages"),
        TEMPLATE_3("#3: Feature list"),
        TEMPLATE_4("#4: Horizontal packages"),
        TEMPLATE_5("#5: Minimalist with small banner"),
        TEMPLATE_7("#7: Multi-tier"),
        COMPONENTS_BLESS("#8: Components - bless."),
        COMPONENTS_BLESS_LOCAL_FONT("#9: Components - bless. - local font"),
        COMPONENTS_BLESS_GOOGLE_FONT("#10: Components - bless. - Google font"),
        TABS_BUTTONS("#11: Tabs - buttons"),
        TABS_TOGGLE("#12 Tabs - toggle"),
        UNRECOGNIZED_TEMPLATE("Default template"),
    }

    const val offeringIdentifier = "offering"

    private val zeroDecimalPlaceCountries = listOf("PH", "KZ", "TW", "MX", "TH")

    private val tosURL = URL("https://revenuecat.com/tos")
    private val images = PaywallData.Configuration.Images(
        header = "9a17e0a7_1689854430..jpeg",
        background = "9a17e0a7_1689854342..jpg",
        icon = "9a17e0a7_1689854430..jpeg",
    )
    private val paywallAssetBaseURL = URL("https://assets.pawwalls.com")

    private val weeklyProduct = TestStoreProduct(
        id = "com.revenuecat.product_1",
        name = "Weekly",
        title = "Weekly (App name)",
        price = Price(amountMicros = 1_990_000, currencyCode = "USD", formatted = "$1.99"),
        description = "PRO Weekly",
        period = Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W"),
    )

    private val monthlyProduct = TestStoreProduct(
        id = "com.revenuecat.product_2",
        name = "Monthly",
        title = "Monthly (App name)",
        price = Price(amountMicros = 6_990_000, currencyCode = "USD", formatted = "$6.99"),
        description = "PRO Monthly",
        period = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
        freeTrialPeriod = Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W"),
    )

    private val twoMonthProduct = TestStoreProduct(
        id = "com.revenuecat.product_3",
        name = "Two Months",
        title = "Two Months (App name)",
        price = Price(amountMicros = 14_990_000, currencyCode = "USD", formatted = "$14.99"),
        description = "PRO Two Months",
        period = Period(value = 2, unit = Period.Unit.MONTH, iso8601 = "P2M"),
        freeTrialPeriod = null,
    )

    private val threeMonthProduct = TestStoreProduct(
        id = "com.revenuecat.product_4",
        name = "Three Months",
        title = "Three Months (App name)",
        price = Price(amountMicros = 24_990_000, currencyCode = "USD", formatted = "$24.99"),
        description = "PRO Three Months",
        period = Period(value = 3, unit = Period.Unit.MONTH, iso8601 = "P3M"),
        freeTrialPeriod = Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W"),
    )

    private val sixMonthProduct = TestStoreProduct(
        id = "com.revenuecat.product_5",
        name = "Six Months",
        title = "Six Months (App name)",
        price = Price(amountMicros = 34_990_000, currencyCode = "USD", formatted = "$34.99"),
        description = "PRO Six Months",
        period = Period(value = 6, unit = Period.Unit.MONTH, iso8601 = "P6M"),
        freeTrialPeriod = Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W"),
    )

    private val annualProduct = TestStoreProduct(
        id = "com.revenuecat.product_6",
        name = "Annual",
        title = "Annual (App name)",
        price = Price(amountMicros = 53_990_000, currencyCode = "USD", formatted = "$53.99"),
        description = "PRO Annual",
        period = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
        freeTrialPeriod = Period(value = 2, unit = Period.Unit.WEEK, iso8601 = "P2W"),
    )

    private val lifetimeProduct = TestStoreProduct(
        id = "com.revenuecat.product_lifetime",
        name = "Lifetime",
        title = "Lifetime (App name)",
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

    private val twoMonthPackage = Package(
        PackageType.TWO_MONTH.identifier!!,
        PackageType.TWO_MONTH,
        twoMonthProduct,
        offeringIdentifier,
    )

    private val threeMonthPackage = Package(
        PackageType.THREE_MONTH.identifier!!,
        PackageType.THREE_MONTH,
        threeMonthProduct,
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
        twoMonthPackage,
        threeMonthPackage,
        sixMonthPackage,
        annualPackage,
        lifetimePackage,
    )

    fun template1(): SampleData.Legacy = SampleData.Legacy(
        data = PaywallData(
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
            zeroDecimalPlaceCountries = zeroDecimalPlaceCountries,
        ),
    )

    fun template2(): SampleData.Legacy = SampleData.Legacy(
        data = PaywallData(
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
            zeroDecimalPlaceCountries = zeroDecimalPlaceCountries,
        ),
    )

    fun template3(): SampleData.Legacy = SampleData.Legacy(
        data = PaywallData(
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
                        closeButton = PaywallColor("#00FF00"),
                    ),
                    dark = PaywallData.Configuration.Colors(
                        background = PaywallColor("#272727"),
                        text1 = PaywallColor("#FFFFFF"),
                        text2 = PaywallColor("#B7B7B7"),
                        callToActionBackground = PaywallColor("#FFFFFF"),
                        callToActionForeground = PaywallColor("#000000"),
                        accent1 = PaywallColor("#F4E971"),
                        accent2 = PaywallColor("#4A4A4A"),
                        closeButton = PaywallColor("#00FF00"),
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
            zeroDecimalPlaceCountries = zeroDecimalPlaceCountries,
        ),
    )

    fun template4(): SampleData.Legacy = SampleData.Legacy(
        data = PaywallData(
            templateName = "4",
            config = PaywallData.Configuration(
                images = PaywallData.Configuration.Images(
                    background = "300883_1690710097.jpg",
                ),
                colors = PaywallData.Configuration.ColorInformation(
                    light = PaywallData.Configuration.Colors(
                        background = PaywallColor("#FFFFFF"),
                        text1 = PaywallColor("#111111"),
                        text2 = PaywallColor(stringRepresentation = "#333333"),
                        text3 = PaywallColor(stringRepresentation = "#999999"),
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
            zeroDecimalPlaceCountries = zeroDecimalPlaceCountries,
        ),
    )

    fun template5(): SampleData.Legacy = SampleData.Legacy(
        data = PaywallData(
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
            zeroDecimalPlaceCountries = zeroDecimalPlaceCountries,
        ),
    )

    fun template7(): SampleData.Legacy = SampleData.Legacy(
        data = PaywallData(
            templateName = "7",
            config = PaywallData.Configuration(
                packageIds = emptyList(),
                imagesByTier = mapOf(
                    "basic" to PaywallData.Configuration.Images(
                        header = "954459_1703109702.png",
                    ),
                    "standard" to PaywallData.Configuration.Images(
                        header = "954459_1692992845.png",
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
                        ),
                        defaultPackageId = PackageType.ANNUAL.identifier!!,
                    ),
                    PaywallData.Configuration.Tier(
                        id = "standard",
                        packageIds = listOf(
                            PackageType.TWO_MONTH.identifier!!,
                            PackageType.SIX_MONTH.identifier!!,
                        ),
                        defaultPackageId = PackageType.SIX_MONTH.identifier!!,
                    ),
                    PaywallData.Configuration.Tier(
                        id = "premium",
                        packageIds = listOf(
                            PackageType.THREE_MONTH.identifier!!,
                            PackageType.LIFETIME.identifier!!,
                        ),
                        defaultPackageId = PackageType.SIX_MONTH.identifier!!,
                    ),
                ),
                defaultTier = "standard",
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
                            accent2 = PaywallColor(stringRepresentation = "#100031"),
                            accent3 = PaywallColor(stringRepresentation = "#7676801F"),
                            tierControlBackground = PaywallColor(stringRepresentation = "#eeeef0"),
                            tierControlForeground = PaywallColor(stringRepresentation = "#000000"),
                            tierControlSelectedBackground = PaywallColor(stringRepresentation = "#2d7fc1"),
                            tierControlSelectedForeground = PaywallColor(stringRepresentation = "#000000"),
                        ),
                    ),
                    "standard" to PaywallData.Configuration.ColorInformation(
                        light = PaywallData.Configuration.Colors(
                            background = PaywallColor(stringRepresentation = "#FFFFFF"),
                            text1 = PaywallColor(stringRepresentation = "#000000"),
                            text2 = PaywallColor(stringRepresentation = "#ffffff"),
                            text3 = PaywallColor(stringRepresentation = "#30A0F8AA"),
                            callToActionBackground = PaywallColor(stringRepresentation = "#da4079"),
                            callToActionForeground = PaywallColor(stringRepresentation = "#ffffff"),
                            accent1 = PaywallColor(stringRepresentation = "#cd0654"),
                            accent2 = PaywallColor(stringRepresentation = "#100031"),
                            accent3 = PaywallColor(stringRepresentation = "#7676801F"),
                            tierControlBackground = PaywallColor(stringRepresentation = "#eeeef0"),
                            tierControlForeground = PaywallColor(stringRepresentation = "#000000"),
                            tierControlSelectedBackground = PaywallColor(stringRepresentation = "#cd0654"),
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
                            accent2 = PaywallColor(stringRepresentation = "#100031"),
                            accent3 = PaywallColor(stringRepresentation = "#7676801F"),
                            tierControlBackground = PaywallColor(stringRepresentation = "#eeeef0"),
                            tierControlForeground = PaywallColor(stringRepresentation = "#000000"),
                            tierControlSelectedBackground = PaywallColor(stringRepresentation = "#cd0654"),
                            tierControlSelectedForeground = PaywallColor(stringRepresentation = "#000000"),
                        ),
                    ),
                ),
            ),
            assetBaseURL = paywallAssetBaseURL,
            localization = mapOf(
                "en_US" to PaywallData.LocalizedConfiguration(
                    title = "Get started with our Basic plan",
                    callToAction = "{{ price_per_period }}",
                    callToActionWithIntroOffer = "Start your {{ sub_offer_duration }} free trial",
                    offerDetails = "{{ total_price_and_per_month }}",
                    offerDetailsWithIntroOffer = "Free for {{ sub_offer_duration }}," +
                        " then {{ total_price_and_per_month }}",
                    offerName = "{{ sub_period }}",
                    features = listOf(
                        PaywallData.LocalizedConfiguration.Feature(
                            title = "Access to 10 cinematic LUTs",
                            iconID = "tick",
                        ),
                        PaywallData.LocalizedConfiguration.Feature(
                            title = "Standard fonts",
                            iconID = "tick",
                        ),
                        PaywallData.LocalizedConfiguration.Feature(
                            title = "2 templates",
                            iconID = "tick",
                        ),
                    ),
                    tierName = "Basic",
                ),
            ),
            localizationByTier = mapOf(
                "en_US" to mapOf(
                    "basic" to PaywallData.LocalizedConfiguration(
                        title = "Get started with our Basic plan",
                        callToAction = "{{ price_per_period }}",
                        callToActionWithIntroOffer = "Start your {{ sub_offer_duration }} free trial",
                        offerDetails = "{{ total_price_and_per_month }}",
                        offerDetailsWithIntroOffer = "Free for {{ sub_offer_duration }}," +
                            " then {{ total_price_and_per_month }}",
                        offerName = "{{ sub_period }}",
                        features = listOf(
                            PaywallData.LocalizedConfiguration.Feature(
                                title = "Access to 10 cinematic LUTs",
                                iconID = "tick",
                            ),
                            PaywallData.LocalizedConfiguration.Feature(
                                title = "Standard fonts",
                                iconID = "tick",
                            ),
                            PaywallData.LocalizedConfiguration.Feature(
                                title = "2 templates",
                                iconID = "tick",
                            ),
                        ),
                        tierName = "Basic",
                        offerOverrides = mapOf(
                            PackageType.MONTHLY.identifier!! to PaywallData.LocalizedConfiguration.OfferOverride(
                                offerName = "OVERRIDE Monthly",
                                offerDetails = "OVERRIDE Monthly details {{ total_price_and_per_month }}",
                                offerDetailsWithIntroOffer = "OVERRIDE weekly Free for {{ sub_offer_duration }}," +
                                    " then {{ total_price_and_per_month }}",
                                offerDetailsWithMultipleIntroOffers = "OVERRIDE Monthly details with multiple offers",
                                offerBadge = "Worst Deal",
                            ),
                        ),
                    ),
                    "standard" to PaywallData.LocalizedConfiguration(
                        title = "Get started with our Standard plan",
                        callToAction = "{{ price_per_period }}",
                        callToActionWithIntroOffer = "Start your {{ sub_offer_duration }} free trial",
                        offerDetails = "{{ total_price_and_per_month }}",
                        offerDetailsWithIntroOffer = "Free for {{ sub_offer_duration }}," +
                            " then {{ total_price_and_per_month }}",
                        offerName = "{{ sub_period }}",
                        features = listOf(
                            PaywallData.LocalizedConfiguration.Feature(
                                title = "Access to 30 cinematic LUTs",
                                iconID = "tick",
                            ),
                            PaywallData.LocalizedConfiguration.Feature(
                                title = "Pro fonts and transition effects",
                                iconID = "tick",
                            ),
                            PaywallData.LocalizedConfiguration.Feature(
                                title = "10+ templates",
                                iconID = "tick",
                            ),
                        ),
                        tierName = "Standard",
                    ),
                    "premium" to PaywallData.LocalizedConfiguration(
                        title = "Master the art of video editing",
                        callToAction = "{{ price_per_period }}",
                        callToActionWithIntroOffer = "Start your {{ sub_offer_duration }} free trial",
                        offerDetails = "{{ total_price_and_per_month }}",
                        offerDetailsWithIntroOffer = "Free for {{ sub_offer_duration }}," +
                            " then {{ total_price_and_per_month }}",
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
            zeroDecimalPlaceCountries = zeroDecimalPlaceCountries,
        ),
    )

    fun unrecognizedTemplate(): SampleData.Legacy = SampleData.Legacy(
        data = PaywallData(
            templateName = "unrecognized",
            config = template4().data.config,
            assetBaseURL = paywallAssetBaseURL,
            localization = mapOf(
                "en_US" to template4().data.localizedConfiguration.second,
            ),
            zeroDecimalPlaceCountries = zeroDecimalPlaceCountries,
        ),
    )

    /**
     * [Inspiration](https://mobbin.com/screens/fd110266-4c8b-4673-9b51-48de70a4ae51)
     */
    fun bless(font: FontAlias? = null): SampleData.Components {
        val textColor = ColorScheme(
            light = ColorInfo.Hex(Color.Black.toArgb()),
            dark = ColorInfo.Hex(Color.White.toArgb()),
        )
        val backgroundColor = ColorScheme(
            light = ColorInfo.Hex(Color.White.toArgb()),
            dark = ColorInfo.Hex(Color.Black.toArgb()),
        )

        return SampleData.Components(
            data = PaywallComponentsData(
                templateName = "template",
                assetBaseURL = URL("https://assets.pawwalls.com"),
                componentsConfig = ComponentsConfig(
                    base = PaywallComponentsConfig(
                        stack = StackComponent(
                            components = listOf(
                                StackComponent(
                                    components = emptyList(),
                                    dimension = ZLayer(alignment = TwoDimensionalAlignment.CENTER),
                                    size = Size(width = Fill, height = Fill),
                                    backgroundColor = ColorScheme(
                                        light = ColorInfo.Gradient.Linear(
                                            degrees = 60f,
                                            points = listOf(
                                                ColorInfo.Gradient.Point(
                                                    color = Color(red = 0xFF, green = 0xFF, blue = 0xFF, alpha = 0xFF)
                                                        .toArgb(),
                                                    percent = 40f,
                                                ),
                                                ColorInfo.Gradient.Point(
                                                    color = Color(red = 5, green = 124, blue = 91).toArgb(),
                                                    percent = 100f,
                                                ),
                                            ),
                                        ),
                                    ),
                                ),
                                StackComponent(
                                    components = listOf(
                                        TextComponent(
                                            text = LocalizationKey("title"),
                                            color = textColor,
                                            fontName = font,
                                            fontWeight = FontWeight.BOLD,
                                            fontSize = 28,
                                            horizontalAlignment = LEADING,
                                            size = Size(width = Fill, height = Fit),
                                            margin = Padding(top = 0.0, bottom = 40.0, leading = 0.0, trailing = 0.0),
                                        ),
                                        TextComponent(
                                            text = LocalizationKey("feature-1"),
                                            color = textColor,
                                            fontName = font,
                                            horizontalAlignment = LEADING,
                                            size = Size(width = Fill, height = Fit),
                                            margin = Padding(top = 8.0, bottom = 8.0, leading = 0.0, trailing = 0.0),
                                        ),
                                        TextComponent(
                                            text = LocalizationKey("feature-2"),
                                            color = textColor,
                                            fontName = font,
                                            horizontalAlignment = LEADING,
                                            size = Size(width = Fill, height = Fit),
                                            margin = Padding(top = 8.0, bottom = 8.0, leading = 0.0, trailing = 0.0),
                                        ),
                                        TextComponent(
                                            text = LocalizationKey("feature-3"),
                                            color = textColor,
                                            fontName = font,
                                            horizontalAlignment = LEADING,
                                            size = Size(width = Fill, height = Fit),
                                            margin = Padding(top = 8.0, bottom = 8.0, leading = 0.0, trailing = 0.0),
                                        ),
                                        TextComponent(
                                            text = LocalizationKey("feature-4"),
                                            color = textColor,
                                            fontName = font,
                                            horizontalAlignment = LEADING,
                                            size = Size(width = Fill, height = Fit),
                                            margin = Padding(top = 8.0, bottom = 8.0, leading = 0.0, trailing = 0.0),
                                        ),
                                        TextComponent(
                                            text = LocalizationKey("feature-5"),
                                            color = textColor,
                                            fontName = font,
                                            horizontalAlignment = LEADING,
                                            size = Size(width = Fill, height = Fit),
                                            margin = Padding(top = 8.0, bottom = 8.0, leading = 0.0, trailing = 0.0),
                                        ),
                                        TextComponent(
                                            text = LocalizationKey("feature-6"),
                                            color = textColor,
                                            fontName = font,
                                            horizontalAlignment = LEADING,
                                            size = Size(width = Fill, height = Fit),
                                            margin = Padding(top = 8.0, bottom = 8.0, leading = 0.0, trailing = 0.0),
                                        ),
                                        TextComponent(
                                            text = LocalizationKey("offer"),
                                            color = textColor,
                                            fontName = font,
                                            horizontalAlignment = LEADING,
                                            size = Size(width = Fill, height = Fit),
                                            margin = Padding(top = 48.0, bottom = 8.0, leading = 0.0, trailing = 0.0),
                                        ),
                                        StackComponent(
                                            components = listOf(
                                                TextComponent(
                                                    text = LocalizationKey("cta"),
                                                    color = ColorScheme(
                                                        light = ColorInfo.Hex(Color.White.toArgb()),
                                                    ),
                                                    fontName = font,
                                                    fontWeight = FontWeight.BOLD,
                                                ),
                                            ),
                                            dimension = ZLayer(alignment = TwoDimensionalAlignment.CENTER),
                                            size = Size(width = Fit, height = Fit),
                                            backgroundColor = ColorScheme(
                                                light = ColorInfo.Hex(Color(red = 5, green = 124, blue = 91).toArgb()),
                                            ),
                                            padding = Padding(top = 8.0, bottom = 8.0, leading = 32.0, trailing = 32.0),
                                            margin = Padding(top = 8.0, bottom = 8.0, leading = 0.0, trailing = 0.0),
                                            shape = Shape.Pill,
                                        ),
                                        TextComponent(
                                            text = LocalizationKey("terms"),
                                            color = textColor,
                                            fontName = font,
                                        ),
                                    ),
                                    dimension = Vertical(alignment = LEADING, distribution = END),
                                    size = Size(width = Fill, height = Fill),
                                    padding = Padding(top = 16.0, bottom = 16.0, leading = 32.0, trailing = 32.0),
                                ),
                            ),
                            dimension = ZLayer(alignment = BOTTOM),
                            size = Size(width = Fill, height = Fill),
                            backgroundColor = backgroundColor,
                        ),
                        background = Background.Color(backgroundColor),
                        stickyFooter = null,
                    ),
                ),
                componentsLocalizations = mapOf(
                    LocaleId("en_US") to mapOf(
                        LocalizationKey("title") to LocalizationData.Text("Unlock bless."),
                        LocalizationKey("feature-1") to LocalizationData.Text("✓ Enjoy a 7 day trial"),
                        LocalizationKey("feature-2") to LocalizationData.Text("✓ Change currencies"),
                        LocalizationKey("feature-3") to LocalizationData.Text("✓ Access more trend charts"),
                        LocalizationKey("feature-4") to LocalizationData.Text("✓ Create custom categories"),
                        LocalizationKey("feature-5") to LocalizationData.Text("✓ Get a special premium icon"),
                        LocalizationKey("feature-6") to LocalizationData.Text(
                            "✓ Receive our love and gratitude for your support",
                        ),
                        LocalizationKey("offer") to LocalizationData.Text(
                            "Try 7 days free, then $19.98/year. Cancel anytime.",
                        ),
                        LocalizationKey("cta") to LocalizationData.Text("Continue"),
                        LocalizationKey("terms") to LocalizationData.Text("Privacy & Terms"),
                    ),
                ),
                defaultLocaleIdentifier = LocaleId("en_US"),
            ),
        )
    }
}

@Suppress("CyclomaticComplexMethod")
private fun variableLocalizationKeysForEnUs(): Map<VariableLocalizationKey, String> =
    VariableLocalizationKey.values().associateWith { key ->
        when (key) {
            VariableLocalizationKey.ANNUAL -> "annual"
            VariableLocalizationKey.ANNUAL_SHORT -> "yr"
            VariableLocalizationKey.ANNUALLY -> "annually"
            VariableLocalizationKey.DAILY -> "daily"
            VariableLocalizationKey.DAY -> "day"
            VariableLocalizationKey.DAY_SHORT -> "day"
            VariableLocalizationKey.FREE_PRICE -> "free"
            VariableLocalizationKey.MONTH -> "month"
            VariableLocalizationKey.MONTH_SHORT -> "mo"
            VariableLocalizationKey.MONTHLY -> "monthly"
            VariableLocalizationKey.LIFETIME -> "lifetime"
            VariableLocalizationKey.NUM_DAY_FEW -> "%d days"
            VariableLocalizationKey.NUM_DAY_MANY -> "%d days"
            VariableLocalizationKey.NUM_DAY_ONE -> "%d day"
            VariableLocalizationKey.NUM_DAY_OTHER -> "%d days"
            VariableLocalizationKey.NUM_DAY_TWO -> "%d days"
            VariableLocalizationKey.NUM_DAY_ZERO -> "%d day"
            VariableLocalizationKey.NUM_MONTH_FEW -> "%d months"
            VariableLocalizationKey.NUM_MONTH_MANY -> "%d months"
            VariableLocalizationKey.NUM_MONTH_ONE -> "%d month"
            VariableLocalizationKey.NUM_MONTH_OTHER -> "%d months"
            VariableLocalizationKey.NUM_MONTH_TWO -> "%d months"
            VariableLocalizationKey.NUM_MONTH_ZERO -> "%d month"
            VariableLocalizationKey.NUM_WEEK_FEW -> "%d weeks"
            VariableLocalizationKey.NUM_WEEK_MANY -> "%d weeks"
            VariableLocalizationKey.NUM_WEEK_ONE -> "%d week"
            VariableLocalizationKey.NUM_WEEK_OTHER -> "%d weeks"
            VariableLocalizationKey.NUM_WEEK_TWO -> "%d weeks"
            VariableLocalizationKey.NUM_WEEK_ZERO -> "%d week"
            VariableLocalizationKey.NUM_YEAR_FEW -> "%d years"
            VariableLocalizationKey.NUM_YEAR_MANY -> "%d years"
            VariableLocalizationKey.NUM_YEAR_ONE -> "%d year"
            VariableLocalizationKey.NUM_YEAR_OTHER -> "%d years"
            VariableLocalizationKey.NUM_YEAR_TWO -> "%d years"
            VariableLocalizationKey.NUM_YEAR_ZERO -> "%d year"
            VariableLocalizationKey.PERCENT -> "%d%%"
            VariableLocalizationKey.WEEK -> "week"
            VariableLocalizationKey.WEEK_SHORT -> "wk"
            VariableLocalizationKey.WEEKLY -> "weekly"
            VariableLocalizationKey.YEAR -> "year"
            VariableLocalizationKey.YEAR_SHORT -> "yr"
            VariableLocalizationKey.YEARLY -> "yearly"
            VariableLocalizationKey.NUM_DAYS_SHORT -> "%dd"
            VariableLocalizationKey.NUM_WEEKS_SHORT -> "%dwk"
            VariableLocalizationKey.NUM_MONTHS_SHORT -> "%dmo"
            VariableLocalizationKey.NUM_YEARS_SHORT -> "%dyr"
        }
    }
