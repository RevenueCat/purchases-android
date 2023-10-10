package com.revenuecat.paywallstester

import android.annotation.SuppressLint
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
            SamplePaywalls.offeringIdentifier,
            SamplePaywalls.offeringIdentifier,
            emptyMap(),
            SamplePaywalls.packages,
            paywall = paywallForTemplate(template),
        )
    }

    fun paywallForTemplate(template: SamplePaywalls.SampleTemplate): PaywallData {
        return when (template) {
            SamplePaywalls.SampleTemplate.TEMPLATE_1 -> SamplePaywalls.template1()
//            SampleTemplate.TEMPLATE_2 -> template2()
        }
    }
}

object SamplePaywalls {

    enum class SampleTemplate(val id: String, val displayableName: String) {
        TEMPLATE_1("1", "#1: Minimalist"),
//            TEMPLATE_2("2", "#2: Bold packages"),
//            TEMPLATE_3("3", "#3: Feature list"),
//            TEMPLATE_4("4", "#4: Horizontal packages"),
    }

    const val offeringIdentifier = "offering"

    private val tosURL = URL("https://revenuecat.com/tos")
    private val images = PaywallData.Configuration.Images(
        header = "9a17e0a7_1689854430..jpeg",
        background = "9a17e0a7_1689854342..jpg",
        icon = "9a17e0a7_1689854430..jpeg",
    )
    val paywallAssetBaseURL = URL("https://assets.pawwalls.com")

    @SuppressLint("VisibleForTests")
    private val weeklyProduct = TestStoreProduct(
        id = "com.revenuecat.product_1",
        title = "Weekly",
        price = Price(amountMicros = 1_990_000, currencyCode = "USD", formatted = "$1.99"),
        description = "PRO Weekly",
        period = Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W"),
    )

    @SuppressLint("VisibleForTests")
    private val monthlyProduct = TestStoreProduct(
        id = "com.revenuecat.product_2",
        title = "Monthly",
        price = Price(amountMicros = 6_990_000, currencyCode = "USD", formatted = "$6.99"),
        description = "PRO Weekly",
        period = Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W"),
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
                    subtitle = "Get access to all our educational content trusted by thousands of parents.",
                    callToAction = "Purchase for {{ price }}",
                    callToActionWithIntroOffer = "Purchase for {{ sub_price_per_month }} per month",
                    offerDetails = "{{ sub_price_per_month }} per month",
                    offerDetailsWithIntroOffer = "Start your {{ sub_offer_duration }} trial, then {{ sub_price_per_month }} per month",
                ),
            ),
        )
    }

//    fun template2(): PaywallData {
//
//    }
}
