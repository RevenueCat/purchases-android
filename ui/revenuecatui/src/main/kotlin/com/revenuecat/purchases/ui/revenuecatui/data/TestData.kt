package com.revenuecat.purchases.ui.revenuecatui.data

import android.content.Context
import androidx.lifecycle.ViewModel
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions
import com.revenuecat.purchases.paywalls.PaywallColor
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewMode
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.ApplicationContext
import com.revenuecat.purchases.ui.revenuecatui.helpers.toPaywallViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URL

internal object TestData {
    val template1 = PaywallData(
        templateName = "1", // TODO-PAYWALLS: use enum
        config = PaywallData.Configuration(
            packages = listOf(
                PackageType.MONTHLY.identifier!!,
            ),
            defaultPackage = PackageType.MONTHLY.identifier!!,
            images = Constants.images,
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
        assetBaseURL = Constants.assetBaseURL,
    )
    val template2 = PaywallData(
        templateName = "2", // TODO-PAYWALLS: use enum
        config = PaywallData.Configuration(
            packages = listOf(
                PackageType.WEEKLY.identifier!!,
                PackageType.MONTHLY.identifier!!,
                PackageType.ANNUAL.identifier!!,
            ),
            defaultPackage = PackageType.MONTHLY.identifier!!,
            images = Constants.images,
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
                title = "Call to action for better conversion.",
                subtitle = "Lorem ipsum is simply dummy text of the printing and typesetting industry.",
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
        assetBaseURL = Constants.assetBaseURL,
    )

    val template1Offering = Offering(
        identifier = "Template1",
        availablePackages = listOf(
            Packages.monthly,
        ),
        metadata = mapOf(),
        paywall = template1,
        serverDescription = "",
    )

    val template2Offering = Offering(
        identifier = "Template2",
        availablePackages = listOf(
            Packages.weekly,
            Packages.monthly,
            Packages.annual,
        ),
        metadata = mapOf(),
        paywall = template2,
        serverDescription = "",
    )

    object Packages {
        val weekly = Package(
            packageType = PackageType.WEEKLY,
            identifier = PackageType.WEEKLY.identifier!!,
            offering = "offering",
            product = TestStoreProduct(
                id = "com.revenuecat.weekly_product",
                title = "Weekly",
                price = Price(amountMicros = 1_990_000, currencyCode = "USD", formatted = "$1.99"),
                description = "Weekly",
                period = Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W"),
            ),
        )
        val monthly = Package(
            packageType = PackageType.MONTHLY,
            identifier = PackageType.MONTHLY.identifier!!,
            offering = "offering",
            product = TestStoreProduct(
                id = "com.revenuecat.monthly_product",
                title = "Monthly",
                price = Price(amountMicros = 7_990_000, currencyCode = "USD", formatted = "$7.99"),
                description = "Monthly",
                period = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
            ),
        )
        val annual = Package(
            packageType = PackageType.ANNUAL,
            identifier = PackageType.ANNUAL.identifier!!,
            offering = "offering",
            product = TestStoreProduct(
                id = "com.revenuecat.annual_product",
                title = "Annual",
                price = Price(amountMicros = 67_990_000, currencyCode = "USD", formatted = "$67.99"),
                description = "Annual",
                period = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
                freeTrialPeriod = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
            ),
        )
        val lifetime = Package(
            packageType = PackageType.LIFETIME,
            identifier = PackageType.LIFETIME.identifier!!,
            offering = "offering",
            product = TestStoreProduct(
                id = "com.revenuecat.lifetime_product",
                title = "Lifetime",
                price = Price(amountMicros = 1_000_000_000, currencyCode = "USD", formatted = "$1,000"),
                description = "Lifetime",
                period = null,
            ),
        )
        val bimonthly = Package(
            packageType = PackageType.TWO_MONTH,
            identifier = PackageType.TWO_MONTH.identifier!!,
            offering = "offering",
            product = TestStoreProduct(
                id = "com.revenuecat.bimonthly_product",
                title = "2 month",
                price = Price(amountMicros = 15_990_000, currencyCode = "USD", formatted = "$15.99"),
                description = "2 month",
                period = Period(value = 2, unit = Period.Unit.MONTH, iso8601 = "P2M"),
                introPrice = Price(amountMicros = 3_990_000, currencyCode = "USD", formatted = "$3.99"),
            ),
        )
        val quarterly = Package(
            packageType = PackageType.THREE_MONTH,
            identifier = PackageType.THREE_MONTH.identifier!!,
            offering = "offering",
            product = TestStoreProduct(
                id = "com.revenuecat.quarterly_product",
                title = "3 month",
                price = Price(amountMicros = 23_990_000, currencyCode = "USD", formatted = "$23.99"),
                description = "3 month",
                period = Period(value = 3, unit = Period.Unit.MONTH, iso8601 = "P3M"),
                freeTrialPeriod = Period(value = 2, unit = Period.Unit.WEEK, iso8601 = "P2W"),
                introPrice = Price(amountMicros = 3_990_000, currencyCode = "USD", formatted = "$3.99"),
            ),
        )
        val semester = Package(
            packageType = PackageType.SIX_MONTH,
            identifier = PackageType.SIX_MONTH.identifier!!,
            offering = "offering",
            product = TestStoreProduct(
                id = "com.revenuecat.semester_product",
                title = "6 month",
                price = Price(amountMicros = 39_990_000, currencyCode = "USD", formatted = "$39.99"),
                description = "6 month",
                period = Period(value = 6, unit = Period.Unit.MONTH, iso8601 = "P6M"),
            ),
        )
    }
}

internal class MockApplicationContext : ApplicationContext {
    override fun getApplicationName(): String {
        return "Mock Paywall"
    }

    // This is hardcoding the english version of the strings for now. We can't access the actual resources since
    // we don't have access to a real context in some cases here.
    override fun getString(resId: Int): String {
        return when (resId) {
            R.string.restore_purchases -> "Restore purchases"
            R.string.annual -> "Annual"
            R.string.semester -> "6 month"
            R.string.quarter -> "3 month"
            R.string.bimonthly -> "2 month"
            R.string.monthly -> "Monthly"
            R.string.weekly -> "Weekly"
            R.string.lifetime -> "Lifetime"
            else -> error("Unknown string resource $resId")
        }
    }
}

internal class MockViewModel(
    private val mode: PaywallViewMode = PaywallViewMode.default,
    offering: Offering,
) : ViewModel(), PaywallViewModel {
    override val state: StateFlow<PaywallViewState>
        get() = _state.asStateFlow()
    private val _state = MutableStateFlow(
        offering.toPaywallViewState(
            VariableDataProvider(MockApplicationContext()),
            mode,
            TestData.template2,
            PaywallTemplate.TEMPLATE_2,
        ),
    )

    override fun refreshState() = Unit

    override fun selectPackage(packageToSelect: TemplateConfiguration.PackageInfo) {
        error("Not supported")
    }

    override fun purchaseSelectedPackage(context: Context) {
        error("Can't purchase mock view model")
    }

    override fun restorePurchases() {
        error("Can't restore purchases")
    }

    override fun openURL(url: URL, context: Context) {
        error("Can't open URL")
    }
}

private object Constants {
    val images = PaywallData.Configuration.Images(
        header = "9a17e0a7_1689854430..jpeg",
        background = "9a17e0a7_1689854342..jpg",
        icon = "9a17e0a7_1689854430..jpeg",
    )

    val assetBaseURL = URL("https://assets.pawwalls.com")
}

private data class TestStoreProduct(
    override val id: String,
    override val title: String,
    override val price: Price,
    override val description: String,
    override val period: Period?,
    private val freeTrialPeriod: Period? = null,
    private val introPrice: Price? = null,
) : StoreProduct {
    override val type: ProductType
        get() = if (period == null) ProductType.INAPP else ProductType.SUBS
    override val subscriptionOptions: SubscriptionOptions?
        get() = buildSubscriptionOptions()
    override val defaultOption: SubscriptionOption?
        get() = subscriptionOptions?.defaultOffer
    override val purchasingData: PurchasingData
        get() = object : PurchasingData {
            override val productId: String
                get() = id
            override val productType: ProductType
                get() = ProductType.SUBS
        }
    override val presentedOfferingIdentifier: String?
        get() = null
    override val sku: String
        get() = id

    override fun copyWithOfferingId(offeringId: String): StoreProduct {
        return this
    }

    private fun buildSubscriptionOptions(): SubscriptionOptions? {
        if (period == null) return null
        val freePhase = freeTrialPeriod?.let { freeTrialPeriod ->
            PricingPhase(
                billingPeriod = freeTrialPeriod,
                recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                billingCycleCount = 1,
                price = Price(amountMicros = 0, currencyCode = price.currencyCode, formatted = "Free"),
            )
        }
        val introPhase = introPrice?.let { introPrice ->
            PricingPhase(
                billingPeriod = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
                recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                billingCycleCount = 1,
                price = introPrice,
            )
        }
        val basePricePhase = PricingPhase(
            billingPeriod = period,
            recurrenceMode = RecurrenceMode.INFINITE_RECURRING,
            billingCycleCount = null,
            price = price,
        )
        val subscriptionOptionsList = listOfNotNull(
            TestSubscriptionOption(
                id,
                listOfNotNull(freePhase, introPhase, basePricePhase),
            ).takeIf { freeTrialPeriod != null || introPhase != null },
            TestSubscriptionOption(
                id,
                listOf(basePricePhase),
            ),
        )
        return SubscriptionOptions(subscriptionOptionsList)
    }
}

private class TestSubscriptionOption(
    val productIdentifier: String,
    override val pricingPhases: List<PricingPhase>,
    val basePlanId: String = "testBasePlanId",
    override val tags: List<String> = emptyList(),
    override val presentedOfferingIdentifier: String? = "offering",
) : SubscriptionOption {
    override val id: String
        get() = if (pricingPhases.size == 1) basePlanId else "$basePlanId:testOfferId"

    override val purchasingData: PurchasingData
        get() = object : PurchasingData {
            override val productId: String
                get() = productIdentifier
            override val productType: ProductType
                get() = ProductType.SUBS
        }
}
