package com.revenuecat.purchases.ui.revenuecatui.data.testdata

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.loaded
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates.template1
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates.template2
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates.template3
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates.template4
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates.template5
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.toPaywallState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URL
import java.util.Locale

internal object TestData {
    object Constants {
        val images = PaywallData.Configuration.Images(
            header = "9a17e0a7_1689854430..jpeg",
            background = "9a17e0a7_1689854342..jpg",
            icon = "9a17e0a7_1689854430..jpeg",
        )

        val assetBaseURL = URL("https://assets.pawwalls.com")

        val localization = PaywallData.LocalizedConfiguration(
            title = "Call to action for _better_ conversion.",
            subtitle = "Lorem ipsum is simply dummy text of the ~printing and~ typesetting industry.",
            callToAction = "Subscribe for {{ sub_price_per_month }}/mo",
            offerDetails = "{{ total_price_and_per_month }}",
            offerDetailsWithIntroOffer = "{{ total_price_and_per_month }} after {{ sub_offer_duration }} trial",
            offerName = "{{ sub_period }}",
            features = emptyList(),
        )

        val currentColorScheme = ColorScheme(
            primary = Color.White,
            onPrimary = Color.White,
            primaryContainer = Color.White,
            onPrimaryContainer = Color.White,
            inversePrimary = Color.Green,
            secondary = Color.Black,
            onSecondary = Color.Black,
            secondaryContainer = Color.Black,
            onSecondaryContainer = Color.Black,
            tertiary = Color.Cyan,
            onTertiary = Color.Black,
            tertiaryContainer = Color.Gray,
            onTertiaryContainer = Color.White,
            background = Color.White,
            onBackground = Color.Black,
            surface = Color.Gray,
            onSurface = Color.Black,
            surfaceVariant = Color.DarkGray,
            onSurfaceVariant = Color.White,
            surfaceTint = Color.LightGray,
            inverseSurface = Color.Black,
            inverseOnSurface = Color.White,
            error = Color.Red,
            onError = Color.White,
            errorContainer = Color.Red,
            onErrorContainer = Color.White,
            outline = Color.Transparent,
            outlineVariant = Color.LightGray,
            scrim = Color.Gray,
        )
    }

    val offeringWithNoPaywall = Offering(
        identifier = "Template1",
        availablePackages = listOf(
            Packages.monthly,
        ),
        metadata = mapOf(),
        paywall = null,
        serverDescription = "",
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
            Packages.lifetime,
        ),
        metadata = mapOf(),
        paywall = template2,
        serverDescription = "",
    )

    val template3Offering = Offering(
        identifier = "Template3",
        availablePackages = listOf(
            Packages.monthly,
        ),
        metadata = mapOf(),
        paywall = template3,
        serverDescription = "",
    )

    val template4Offering = Offering(
        identifier = "Template4",
        availablePackages = listOf(
            Packages.monthly,
            Packages.semester,
            Packages.annual,
            Packages.weekly,
        ),
        metadata = mapOf(),
        paywall = template4,
        serverDescription = "",
    )

    val template5Offering = Offering(
        identifier = "Template5",
        availablePackages = listOf(
            Packages.monthly,
            Packages.annual,
        ),
        metadata = mapOf(),
        paywall = template5,
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

internal class MockResourceProvider : ResourceProvider {
    override fun getApplicationName(): String {
        return "Mock Paywall"
    }

    // This is hardcoding the english version of the strings for now. We can't access the actual resources since
    // we don't have access to a real context in some cases here.
    override fun getString(resId: Int, vararg formatArgs: Any): String {
        return when (resId) {
            R.string.restore_purchases -> "Restore purchases"
            R.string.annual -> "Annual"
            R.string.semester -> "6 month"
            R.string.quarter -> "3 month"
            R.string.bimonthly -> "2 month"
            R.string.monthly -> "Monthly"
            R.string.weekly -> "Weekly"
            R.string.lifetime -> "Lifetime"
            R.string.continue_cta -> "Continue"
            R.string.default_offer_details_with_intro_offer ->
                "Start your {{ sub_offer_duration }} trial, " +
                    "then {{ total_price_and_per_month }}."
            R.string.package_discount -> "${formatArgs[0]}% off"
            else -> error("Unknown string resource $resId")
        }
    }

    override fun getLocale(): Locale {
        return Locale.getDefault()
    }
}

@Suppress("TooManyFunctions")
internal class MockViewModel(
    mode: PaywallMode = PaywallMode.default,
    offering: Offering,
    private val allowsPurchases: Boolean = false,
    private val shouldErrorOnUnsupportedMethods: Boolean = true,
) : ViewModel(), PaywallViewModel {
    override val resourceProvider: ResourceProvider
        get() = MockResourceProvider()
    override val state: StateFlow<PaywallState>
        get() = _state.asStateFlow()
    override val actionInProgress: State<Boolean>
        get() = _actionInProgress
    override val actionError: State<PurchasesError?>
        get() = _actionError

    fun loadedState(): PaywallState.Loaded? {
        return state.value.loaded()
    }

    private val _state = MutableStateFlow(
        offering.toPaywallState(
            variableDataProvider = VariableDataProvider(resourceProvider),
            activelySubscribedProductIdentifiers = setOf(),
            nonSubscriptionProductIdentifiers = setOf(),
            mode = mode,
            validatedPaywallData = offering.paywall!!,
            template = PaywallTemplate.fromId(offering.paywall!!.templateName)!!,
            shouldDisplayDismissButton = false,
        ),
    )

    private val _actionInProgress = mutableStateOf(false)
    private val _actionError = mutableStateOf<PurchasesError?>(null)

    var trackPaywallImpressionIfNeededCallCount = 0
        private set
    override fun trackPaywallImpressionIfNeeded() {
        trackPaywallImpressionIfNeededCallCount++
    }

    var refreshStateIfLocaleChangedCallCount = 0
        private set
    override fun refreshStateIfLocaleChanged() {
        refreshStateIfLocaleChangedCallCount++
    }

    var refreshStateIfColorsChangedCallCount = 0
        private set
    override fun refreshStateIfColorsChanged(colorScheme: ColorScheme, isDarkMode: Boolean) {
        refreshStateIfColorsChangedCallCount++
    }

    var selectPackageCallCount = 0
        private set
    var selectPackageCallParams = mutableListOf<TemplateConfiguration.PackageInfo>()
        private set
    override fun selectPackage(packageToSelect: TemplateConfiguration.PackageInfo) {
        selectPackageCallCount++
        selectPackageCallParams.add(packageToSelect)
        unsupportedMethod()
    }

    var closePaywallCallCount = 0
        private set
    override fun closePaywall() {
        closePaywallCallCount++
        unsupportedMethod()
    }

    var purchaseSelectedPackageCallCount = 0
        private set
    var purchaseSelectedPackageParams = mutableListOf<Activity?>()
        private set
    override fun purchaseSelectedPackage(activity: Activity?) {
        purchaseSelectedPackageCallCount++
        purchaseSelectedPackageParams.add(activity)
        if (allowsPurchases) {
            simulateActionInProgress()
        } else {
            unsupportedMethod("Can't purchase mock view model")
        }
    }

    var restorePurchasesCallCount = 0
        private set
    override fun restorePurchases() {
        restorePurchasesCallCount++
        if (allowsPurchases) {
            simulateActionInProgress()
        } else {
            unsupportedMethod("Can't restore purchases")
        }
    }

    var clearActionErrorCallCount = 0
        private set
    override fun clearActionError() {
        clearActionErrorCallCount++
        _actionError.value = null
    }

    private fun simulateActionInProgress() {
        viewModelScope.launch {
            _actionInProgress.value = true
            delay(fakePurchaseDelayMillis)
            _actionInProgress.value = false
        }
    }

    private fun unsupportedMethod(errorMessage: String = "Not supported") {
        if (shouldErrorOnUnsupportedMethods) {
            error(errorMessage)
        }
    }

    private companion object {
        const val fakePurchaseDelayMillis: Long = 2000
    }
}
