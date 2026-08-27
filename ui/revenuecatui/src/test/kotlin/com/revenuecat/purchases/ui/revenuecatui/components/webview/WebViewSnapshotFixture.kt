package com.revenuecat.purchases.ui.revenuecatui.components.webview

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.components.style.WebViewComponentStyle
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.JsonObject

/** A snapshot for tests that care about one section, or about the frames rather than the payload. */
internal fun testContextSnapshot(
    customVariables: Map<String, CustomVariableValue> = emptyMap(),
    offering: Offering? = null,
    componentPackage: Package? = null,
    selectedPackage: Package? = null,
    store: Store = Store.PLAY_STORE,
    storefrontCountryCode: String? = "US",
    locale: String = "en-US",
    darkMode: Boolean = false,
): JsonObject = webViewContextSnapshot(
    WebViewContextInput(
        customVariables = customVariables,
        offering = offering,
        componentPackage = componentPackage,
        selectedPackage = selectedPackage,
        store = store,
        storefrontCountryCode = storefrontCountryCode,
        locale = locale,
        darkMode = darkMode,
    ),
)

/**
 * A package on a prepaid base plan. `TestStoreProduct` always builds an `INFINITE_RECURRING` base
 * pricing phase, so the non-renewing case has no fixture to reuse.
 */
internal fun prepaidPackage(): Package {
    val period = Period.create("P1M")
    val price = Price(formatted = "$2.99", amountMicros = 2_990_000L, currencyCode = "USD")
    val basePhase = PricingPhase(
        billingPeriod = period,
        recurrenceMode = RecurrenceMode.NON_RECURRING,
        billingCycleCount = null,
        price = price,
    )
    val option = mockk<SubscriptionOption>()
    // `fullPricePhase` derives from `pricingPhases` on the interface, but mockk stubs it as its own
    // member, so stubbing the list alone leaves it unanswered.
    every { option.pricingPhases } returns listOf(basePhase)
    every { option.fullPricePhase } returns basePhase

    val product = mockk<StoreProduct>()
    every { product.id } returns "prepaid_monthly"
    every { product.name } returns "Prepaid Monthly"
    every { product.period } returns period
    every { product.price } returns price
    every { product.defaultOption } returns option

    return Package(
        identifier = "prepaid",
        packageType = PackageType.MONTHLY,
        product = product,
        presentedOfferingContext = PresentedOfferingContext("offering"),
    )
}

internal fun testWebViewStyle(rcPackage: Package? = null) = WebViewComponentStyle(
    url = "https://assets.example.com/promo/index.html",
    visible = true,
    size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fit()),
    componentId = "promo_web_view",
    overrides = emptyList(),
    rcPackage = rcPackage,
    tabIndex = null,
)
