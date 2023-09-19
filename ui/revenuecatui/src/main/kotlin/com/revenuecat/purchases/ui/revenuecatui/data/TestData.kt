package com.revenuecat.purchases.ui.revenuecatui.data

import android.content.Context
import androidx.lifecycle.ViewModel
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.SubscriptionOptions
import com.revenuecat.purchases.paywalls.PaywallColor
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.ApplicationContext
import com.revenuecat.purchases.ui.revenuecatui.helpers.toPaywallViewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URL

internal object TestData {
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
        ),
        assetBaseURL = Constants.assetBaseURL,
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
                period = Period(value = 12, unit = Period.Unit.MONTH, iso8601 = "P1Y"),
            ),
        )
    }
}

internal class MockApplicationContext : ApplicationContext {
    override fun getApplicationName(): String {
        return "Mock Paywall"
    }
}

internal class MockViewModel(
    private val offering: Offering,
) : ViewModel(), PaywallViewModel {
    override val state: StateFlow<PaywallViewState>
        get() = _state.asStateFlow()
    private val _state = MutableStateFlow(offering.toPaywallViewState(VariableDataProvider(MockApplicationContext())))

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
) : StoreProduct {
    override val type: ProductType
        get() = ProductType.SUBS
    override val subscriptionOptions: SubscriptionOptions?
        get() = null
    override val defaultOption: SubscriptionOption?
        get() = null
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
}
