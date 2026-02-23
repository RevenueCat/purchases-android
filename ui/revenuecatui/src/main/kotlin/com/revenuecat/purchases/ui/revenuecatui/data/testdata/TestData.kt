package com.revenuecat.purchases.ui.revenuecatui.data.testdata

import android.app.Activity
import android.content.res.AssetManager
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.paywalls.DownloadedFontFamily
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.events.ExitOfferType
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.loadedLegacy
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfiguration
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates.template1
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates.template2
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates.template3
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates.template4
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates.template5
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates.template7
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.templates.template7CustomPackages
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallValidationResult
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResolvedOffer
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.toLegacyPaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.validatedPaywall
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URL
import java.util.Date
import java.util.Locale

internal object TestData {
    object Constants {
        val images = PaywallData.Configuration.Images(
            header = "9a17e0a7_1689854430..jpeg",
            background = "9a17e0a7_1689854342..jpg",
            icon = "9a17e0a7_1689854430..jpeg",
        )

        val assetBaseURL = URL("https://assets.pawwalls.com")

        val zeroDecimalPlaceCountries = listOf("PH", "KZ", "TW", "MX", "TH")

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

    private const val TEMPLATE_1_ID = "Template1"
    val offeringWithNoPaywall = Offering(
        identifier = TEMPLATE_1_ID,
        availablePackages = listOf(
            Packages.monthly.copy(TEMPLATE_1_ID),
        ),
        metadata = mapOf(),
        paywall = null,
        serverDescription = "",
    )

    val template1Offering = Offering(
        identifier = TEMPLATE_1_ID,
        availablePackages = listOf(
            Packages.monthly.copy(TEMPLATE_1_ID),
        ),
        metadata = mapOf(),
        paywall = template1,
        serverDescription = "",
    )

    val template1OfferingNoFooter = Offering(
        identifier = TEMPLATE_1_ID,
        availablePackages = listOf(
            Packages.monthly.copy(TEMPLATE_1_ID),
        ),
        metadata = mapOf(),
        paywall = template1.copy(
            config = template1.config.copy(
                displayRestorePurchases = false,
                termsOfServiceURL = null,
                privacyURL = null,
            ),
        ),
        serverDescription = "",
    )

    private const val TEMPLATE_2_ID = "Template2"
    val template2Offering = Offering(
        identifier = TEMPLATE_2_ID,
        availablePackages = listOf(
            Packages.weekly.copy(TEMPLATE_2_ID),
            Packages.monthly.copy(TEMPLATE_2_ID),
            Packages.annual.copy(TEMPLATE_2_ID),
            Packages.lifetime.copy(TEMPLATE_2_ID),
        ),
        metadata = mapOf(),
        paywall = template2,
        serverDescription = "",
    )

    private const val TEMPLATE_3_ID = "Template3"
    val template3Offering = Offering(
        identifier = TEMPLATE_3_ID,
        availablePackages = listOf(
            Packages.monthly.copy(TEMPLATE_3_ID),
        ),
        metadata = mapOf(),
        paywall = template3,
        serverDescription = "",
    )

    private const val TEMPLATE_4_ID = "Template4"
    val template4Offering = Offering(
        identifier = TEMPLATE_4_ID,
        availablePackages = listOf(
            Packages.monthly.copy(TEMPLATE_4_ID),
            Packages.semester.copy(TEMPLATE_4_ID),
            Packages.annual.copy(TEMPLATE_4_ID),
            Packages.weekly.copy(TEMPLATE_4_ID),
        ),
        metadata = mapOf(),
        paywall = template4,
        serverDescription = "",
    )

    private const val TEMPLATE_5_ID = "Template5"
    val template5Offering = Offering(
        identifier = TEMPLATE_5_ID,
        availablePackages = listOf(
            Packages.monthly.copy(TEMPLATE_5_ID),
            Packages.annual.copy(TEMPLATE_5_ID),
        ),
        metadata = mapOf(),
        paywall = template5,
        serverDescription = "",
    )

    private const val TEMPLATE_7_ID = "Template7"
    val template7Offering = Offering(
        identifier = TEMPLATE_7_ID,
        availablePackages = listOf(
            Packages.monthly.copy(TEMPLATE_7_ID),
            Packages.annual.copy(TEMPLATE_7_ID),
            Packages.bimonthly.copy(TEMPLATE_7_ID),
            Packages.quarterly.copy(TEMPLATE_7_ID),
            Packages.semester.copy(TEMPLATE_7_ID),
            Packages.lifetime.copy(TEMPLATE_7_ID),
        ),
        metadata = mapOf(),
        paywall = template7,
        serverDescription = "",
    )

    private const val TEMPLATE_7_CUSTOM_PACKAGE_ID = "Template7CustomPackage"
    val template7CustomPackageOffering = Offering(
        identifier = TEMPLATE_7_CUSTOM_PACKAGE_ID,
        availablePackages = listOf(
            Packages.monthly.copy(TEMPLATE_7_CUSTOM_PACKAGE_ID),
            Packages.annual.copy(TEMPLATE_7_CUSTOM_PACKAGE_ID),
            Packages.bimonthly.copy(TEMPLATE_7_CUSTOM_PACKAGE_ID),
            Packages.quarterly.copy(TEMPLATE_7_CUSTOM_PACKAGE_ID),
            Packages.semester.copy(TEMPLATE_7_CUSTOM_PACKAGE_ID),
            Packages.lifetime.copy(TEMPLATE_7_CUSTOM_PACKAGE_ID),
        ),
        metadata = mapOf(),
        paywall = template7CustomPackages,
        serverDescription = "",
    )

    object Packages {
        val weekly = Package(
            packageType = PackageType.WEEKLY,
            identifier = PackageType.WEEKLY.identifier!!,
            offering = "offering",
            product = TestStoreProduct(
                id = "com.revenuecat.weekly_product",
                name = "Weekly",
                title = "Weekly (App name)",
                price = Price(amountMicros = 1_490_000, currencyCode = "USD", formatted = "$1.49"),
                description = "Weekly",
                period = Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W"),
            ),
        )
        val monthly = Package(
            packageType = PackageType.MONTHLY,
            identifier = PackageType.MONTHLY.identifier!!,
            product = TestStoreProduct(
                id = "com.revenuecat.monthly_product",
                name = "Monthly",
                title = "Monthly (App name)",
                price = Price(amountMicros = 7_990_000, currencyCode = "USD", formatted = "$7.99"),
                description = "Monthly",
                period = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
            ),
            presentedOfferingContext = PresentedOfferingContext(offeringIdentifier = "offering"),
            webCheckoutURL = URL(
                "https://test-web-billing.revenuecat.com?rc_package=${PackageType.MONTHLY.identifier}",
            ),
        )
        val annual = Package(
            packageType = PackageType.ANNUAL,
            identifier = PackageType.ANNUAL.identifier!!,
            product = TestStoreProduct(
                id = "com.revenuecat.annual_product",
                name = "Annual",
                title = "Annual (App name)",
                price = Price(amountMicros = 67_990_000, currencyCode = "USD", formatted = "$67.99"),
                description = "Annual",
                period = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
                freeTrialPeriod = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
            ),
            presentedOfferingContext = PresentedOfferingContext(offeringIdentifier = "offering"),
            webCheckoutURL = URL("https://test-web-billing.revenuecat.com?rc_package=${PackageType.ANNUAL.identifier}"),
        )

        val annualEuros = Package(
            packageType = PackageType.ANNUAL,
            identifier = PackageType.ANNUAL.identifier!!,
            offering = "offering",
            product = TestStoreProduct(
                id = "com.revenuecat.annual_product",
                name = "Annual",
                title = "Annual (App name)",
                price = Price(amountMicros = 67_990_000, currencyCode = "EUR", formatted = "67,99 €"),
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
                name = "Lifetime",
                title = "Lifetime (App name)",
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
                name = "2 month",
                title = "2 month (App name)",
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
                name = "3 month",
                title = "3 month (App name)",
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
                name = "6 month",
                title = "6 month (App name)",
                price = Price(amountMicros = 39_990_000, currencyCode = "USD", formatted = "$39.99"),
                description = "6 month",
                period = Period(value = 6, unit = Period.Unit.MONTH, iso8601 = "P6M"),
            ),
        )
        val custom = Package(
            packageType = PackageType.CUSTOM,
            identifier = "Custom",
            offering = "offering",
            product = TestStoreProduct(
                id = "com.revenuecat.semester_product",
                name = "6 month",
                title = "6 month (App name)",
                price = Price(amountMicros = 39_990_000, currencyCode = "USD", formatted = "$39.99"),
                description = "6 month",
                period = Period(value = 6, unit = Period.Unit.MONTH, iso8601 = "P6M"),
            ),
        )
        val unknown = Package(
            packageType = PackageType.UNKNOWN,
            identifier = "Unknown",
            offering = "offering",
            product = TestStoreProduct(
                id = "com.revenuecat.semester_product",
                name = "6 month",
                title = "6 month (App name)",
                price = Price(amountMicros = 39_990_000, currencyCode = "USD", formatted = "$39.99"),
                description = "6 month",
                period = Period(value = 6, unit = Period.Unit.MONTH, iso8601 = "P6M"),
            ),
        )
        val annualTaiwan = Package(
            packageType = PackageType.ANNUAL,
            identifier = PackageType.ANNUAL.identifier!!,
            offering = "offering",
            product = TestStoreProduct(
                id = "com.revenuecat.annual_product",
                name = "Annual",
                title = "Annual (App name)",
                price = Price(amountMicros = 67_000_000, currencyCode = "TWD", formatted = "NT$67.00"),
                description = "Annual",
                period = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
                freeTrialPeriod = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
            ),
        )
        val monthlyMexico = Package(
            packageType = PackageType.MONTHLY,
            identifier = PackageType.MONTHLY.identifier!!,
            offering = "offering",
            product = TestStoreProduct(
                id = "com.revenuecat.monthly_product",
                name = "Monthly",
                title = "Monthly (App name)",
                price = Price(amountMicros = 8_000_000, currencyCode = "MXN", formatted = "$8.00"),
                description = "Monthly",
                period = Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M"),
            ),
        )
        val quarterlyThailand = Package(
            packageType = PackageType.THREE_MONTH,
            identifier = PackageType.THREE_MONTH.identifier!!,
            offering = "offering",
            product = TestStoreProduct(
                id = "com.revenuecat.quarterly_product",
                name = "3 month",
                title = "3 month (App name)",
                price = Price(amountMicros = 24_000_000, currencyCode = "THB", formatted = "THB24.00"),
                description = "3 month",
                period = Period(value = 3, unit = Period.Unit.MONTH, iso8601 = "P3M"),
                introPrice = Price(amountMicros = 4_000_000, currencyCode = "THB", formatted = "THB4.00"),
            ),
        )
    }

    object Components {
        val monthlyPackageComponent = PackageComponent(
            packageId = PackageType.MONTHLY.identifier!!,
            isSelectedByDefault = false,
            stack = StackComponent(components = emptyList()),
        )
    }

    fun Package.copy(offeringId: String): Package {
        val presentedOfferingContext = PresentedOfferingContext(offeringId)
        return Package(
            identifier = this.identifier,
            packageType = this.packageType,
            product = this.product.copyWithPresentedOfferingContext(presentedOfferingContext),
            presentedOfferingContext = presentedOfferingContext,
            webCheckoutURL = this.webCheckoutURL,
        )
    }
}

internal class MockResourceProvider(
    /**
     * A map of resource type to a map of resource name to resource ID. For instance, to specify a font resource, do:
     *
     * ```kotlin
     * mapOf(
     *     "font" to mapOf("Roboto" to 100)
     * )
     * ```
     */
    private val resourceIds: Map<String, Map<String, Int>> = emptyMap(),
    private val assetPaths: List<String> = emptyList(),
    private val downloadedFilesByUrl: Map<String, DownloadedFontFamily> = emptyMap(),
    private val fontFamiliesByXmlResourceId: Map<Int, FontFamily> = emptyMap(),
    private val mockAssetManager: AssetManager? = null,
) : ResourceProvider {
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
            R.string.external_purchase_error -> "An error occurred during the purchase. Please try again."
            R.string.external_restore_error -> "An error occurred while restoring purchases. Please try again."
            else -> error("Unknown string resource $resId")
        }
    }

    override fun getLocale(): Locale {
        return Locale.getDefault()
    }

    override fun getResourceIdentifier(name: String, type: String): Int =
        resourceIds[type]?.get(name) ?: 0

    override fun getXmlFontFamily(resourceId: Int): FontFamily? {
        return fontFamiliesByXmlResourceId[resourceId]
    }

    override fun getAssetFontPaths(names: List<String>): Map<String, String>? {
        val foundPaths = names.associateWith { name ->
            val nameWithExtension = if (name.endsWith(".ttf")) name else "$name.ttf"
            "${ResourceProvider.ASSETS_FONTS_DIR}/$nameWithExtension"
        }

        return foundPaths.filter { assetPaths.contains(it.value) }
    }

    override fun getCachedFontFamilyOrStartDownload(
        fontInfo: UiConfig.AppConfig.FontsConfig.FontInfo.Name,
    ): DownloadedFontFamily? {
        return downloadedFilesByUrl[fontInfo.url]
    }

    override fun getAssetManager(): AssetManager? {
        return mockAssetManager
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
    override val actionErrorMessage: State<String?>
        get() = _actionErrorMessage
    override val purchaseCompleted: State<Boolean> = mutableStateOf(false)
    override val preloadedExitOffering: State<Offering?> = mutableStateOf(null)

    fun loadedLegacyState(): PaywallState.Loaded.Legacy? {
        return state.value.loadedLegacy()
    }

    private val _state = MutableStateFlow(
        when (val validated = offering.validatedPaywall(TestData.Constants.currentColorScheme, resourceProvider)) {
            is PaywallValidationResult.Legacy -> offering.toLegacyPaywallState(
                variableDataProvider = VariableDataProvider(resourceProvider),
                mode = mode,
                validatedPaywallData = validated.displayablePaywall,
                template = validated.template,
                shouldDisplayDismissButton = false,
                storefrontCountryCode = "US",
            )
            is PaywallValidationResult.Components -> offering.toComponentsPaywallState(
                validationResult = validated,
                storefrontCountryCode = null,
                dateProvider = { Date(MILLIS_2025_01_25) },
                purchases = MockPurchasesType(),
            )
        },
    )

    private val _actionInProgress = mutableStateOf(false)
    private val _actionError = mutableStateOf<PurchasesError?>(null)
    private val _actionErrorMessage = mutableStateOf<String?>(null)

    var trackPaywallImpressionIfNeededCallCount = 0
        private set
    override fun trackPaywallImpressionIfNeeded() {
        trackPaywallImpressionIfNeededCallCount++
    }

    var trackExitOfferCallCount = 0
        private set
    var trackExitOfferParams = mutableListOf<Pair<ExitOfferType, String>>()
        private set
    override fun trackExitOffer(exitOfferType: ExitOfferType, exitOfferingIdentifier: String) {
        trackExitOfferCallCount++
        trackExitOfferParams.add(Pair(exitOfferType, exitOfferingIdentifier))
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
    }

    var getWebCheckoutUrlCallCount = 0
        private set
    var getWebCheckoutUrlParams = mutableListOf<PaywallAction.External.LaunchWebCheckout>()
        private set
    override fun getWebCheckoutUrl(launchWebCheckout: PaywallAction.External.LaunchWebCheckout): String? {
        getWebCheckoutUrlCallCount++
        getWebCheckoutUrlParams.add(launchWebCheckout)
        return null
    }

    var invalidateCustomerInfoCacheCallCount = 0
        private set
    override fun invalidateCustomerInfoCache() {
        invalidateCustomerInfoCacheCallCount++
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

    var handlePackagePurchaseCount = 0
        private set
    var handlePackagePurchaseParams = mutableListOf<Pair<Activity, Package?>>()
        private set
    override suspend fun handlePackagePurchase(activity: Activity, pkg: Package?, resolvedOffer: ResolvedOffer?) {
        handlePackagePurchaseCount++
        handlePackagePurchaseParams.add(activity to pkg)
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

    var handleRestorePurchasesCallCount = 0
        private set
    override suspend fun handleRestorePurchases() {
        handleRestorePurchasesCallCount++
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

    var preloadExitOfferingCallCount = 0
        private set
    override fun preloadExitOffering() {
        preloadExitOfferingCallCount++
    }

    var updateOptionsCallCount = 0
        private set
    var updateOptionsParams = mutableListOf<PaywallOptions>()
        private set
    fun updateOptions(options: PaywallOptions) {
        updateOptionsCallCount++
        updateOptionsParams.add(options)
    }

    private fun simulateActionInProgress() {
        viewModelScope.launch {
            awaitSimulateActionInProgress()
        }
    }

    private suspend fun awaitSimulateActionInProgress() {
        _actionInProgress.value = true
        delay(fakePurchaseDelayMillis)
        _actionInProgress.value = false
    }

    private fun unsupportedMethod(errorMessage: String = "Not supported") {
        if (shouldErrorOnUnsupportedMethods) {
            error(errorMessage)
        }
    }

    private companion object {
        const val fakePurchaseDelayMillis: Long = 2000
        private const val MILLIS_2025_01_25 = 1737763200000
    }
}
