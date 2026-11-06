@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.common.workflows.WorkflowScreen
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.extensions.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.validatePaywallComponentsDataOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.workflow.WorkflowScreenMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(AndroidJUnit4::class)
class WorkflowSheetRelativeDiscountTests {

    private val offeringId = "sheet_discount"
    private val localeId = LocaleId("en_US")

    @Test
    fun `workflow discount baseline includes monthly and quarterly plans inside unopened sheet`() {
        val state = makeWorkflowState(includeSheet = true)

        assertThat(state.paywallPackages.map { it.identifier }).containsExactlyInAnyOrder(
            PackageType.ANNUAL.identifier, PackageType.THREE_MONTH.identifier, PackageType.MONTHLY.identifier,
        )
        assertThat(state.mostExpensivePricePerMonthMicros).isEqualTo(10_990_000L)
        assertThat(state.selectedPackageInfo?.rcPackage?.packageType).isEqualTo(PackageType.ANNUAL)

        state.update(requireNotNull(PackageType.THREE_MONTH.identifier))
        assertThat(state.selectedPackageInfo?.rcPackage?.packageType).isEqualTo(PackageType.THREE_MONTH)
        assertThat(state.mostExpensivePricePerMonthMicros).isEqualTo(10_990_000L)
    }

    @Test
    fun `workflow annual-only paywall ignores other plans in offering for discount`() {
        val state = makeWorkflowState(includeSheet = false)

        assertThat(state.paywallPackages.map { it.packageType }).containsExactly(PackageType.ANNUAL)
        assertThat(state.selectedPackageInfo?.rcPackage?.packageType).isEqualTo(PackageType.ANNUAL)
        assertThat(state.mostExpensivePricePerMonthMicros).isEqualTo(5_832_500L)
    }

    private fun makeWorkflowState(includeSheet: Boolean): PaywallState.Loaded.Components {
        val packages = listOf(
            makePackage(PackageType.ANNUAL, 69_990_000, Period(1, Period.Unit.YEAR, "P1Y")),
            makePackage(PackageType.THREE_MONTH, 24_990_000, Period(3, Period.Unit.MONTH, "P3M")),
            makePackage(PackageType.MONTHLY, 10_990_000, Period(1, Period.Unit.MONTH, "P1M")),
        )
        val annual = packageComponent(PackageType.ANNUAL, isDefault = true)
        val sheetButton = ButtonComponent(
            action = ButtonComponent.Action.NavigateTo(ButtonComponent.Destination.Sheet(
                id = "all_plans",
                stack = StackComponent(components = packages.map { packageComponent(it.packageType) }),
            )),
            stack = StackComponent(components = emptyList()),
        )
        val screen = WorkflowScreen(
            name = "Paywall", templateName = "template_v2", revision = 1,
            assetBaseURL = URL("https://example.com"),
            componentsConfig = ComponentsConfig(base = PaywallComponentsConfig(
                stack = StackComponent(components = emptyList()),
                stickyFooter = StickyFooterComponent(stack = StackComponent(
                    components = if (includeSheet) listOf(annual, sheetButton) else listOf(annual),
                )),
                background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
            )),
            componentsLocalizations = mapOf(localeId to mapOf(LocalizationKey("key") to LocalizationData.Text("value"))),
            defaultLocaleIdentifier = localeId,
            offeringIdentifier = offeringId,
        )
        val offering = Offering(
            identifier = offeringId, serverDescription = "Test", metadata = emptyMap(), availablePackages = packages,
            paywallComponents = WorkflowScreenMapper.toPaywallComponents(screen, "screen", UiConfig()),
        )
        val validated = requireNotNull(offering.validatePaywallComponentsDataOrNull()).getOrThrow()
        return offering.toComponentsPaywallState(validated, storefrontCountryCode = "US")
    }

    private fun packageComponent(type: PackageType, isDefault: Boolean = false) = PackageComponent(
        packageId = requireNotNull(type.identifier), isSelectedByDefault = isDefault,
        stack = StackComponent(components = emptyList()),
    )

    private fun makePackage(type: PackageType, priceMicros: Long, period: Period) = Package(
        identifier = requireNotNull(type.identifier), packageType = type,
        presentedOfferingContext = PresentedOfferingContext(offeringId),
        product = TestStoreProduct(
            id = type.name, name = type.name, title = type.name, description = type.name,
            price = Price(amountMicros = priceMicros, currencyCode = "USD", formatted = "USD $priceMicros micros"),
            period = period,
            presentedOfferingContext = PresentedOfferingContext(offeringId),
        ),
    )
}
