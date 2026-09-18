@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.WorkflowScreen
import com.revenuecat.purchases.common.workflows.WorkflowStep
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.text.TextComponentView
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class WorkflowSheetRelativeDiscountTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dispatcher = StandardTestDispatcher()
    private val offeringId = "sheet_discount"
    private val localeId = LocaleId("en_US")
    private val discountKey = LocalizationKey("discount")
    private val localizations = nonEmptyMapOf(
        localeId to nonEmptyMapOf(discountKey to LocalizationData.Text("{{ product.relative_discount }} OFF")),
    )
    private val textComponent = TextComponent(
        text = discountKey,
        color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `workflow discount includes monthly and quarterly plans inside unopened sheet`() = runTest {
        val state = makeWorkflowState(includeSheet = true)
        advanceUntilIdle()

        assertThat(state.paywallPackages.map { it.identifier }).containsExactly(
            PackageType.ANNUAL.identifier, PackageType.THREE_MONTH.identifier, PackageType.MONTHLY.identifier,
        )
        assertThat(state.mostExpensivePricePerMonthMicros).isEqualTo(10_990_000L)
        assertThat(state.selectedPackageInfo?.rcPackage?.packageType).isEqualTo(PackageType.ANNUAL)
        assertDiscountText(state, "47% OFF")

        composeTestRule.runOnIdle { state.update(requireNotNull(PackageType.THREE_MONTH.identifier)) }
        composeTestRule.onNodeWithTag("discount").onChild().assertTextEquals("24% OFF")
        composeTestRule.runOnIdle { state.update(requireNotNull(PackageType.MONTHLY.identifier)) }
        composeTestRule.onNodeWithTag("discount").onChild().assertTextEquals("OFF")
    }

    @Test
    fun `workflow annual-only paywall ignores other plans in offering for discount`() = runTest {
        val state = makeWorkflowState(includeSheet = false)
        advanceUntilIdle()

        assertThat(state.paywallPackages.map { it.packageType }).containsExactly(PackageType.ANNUAL)
        assertDiscountText(state, "OFF")
    }

    private fun assertDiscountText(state: PaywallState.Loaded.Components, expected: String) {
        val factory = StyleFactory(localizations = localizations, offering = state.offering)
        val style = factory.create(textComponent).getOrThrow().componentStyle as TextComponentStyle
        composeTestRule.setContent {
            TextComponentView(style = style, state = state, modifier = Modifier.testTag("discount"))
        }
        composeTestRule.onNodeWithTag("discount").onChild().assertTextEquals(expected)
    }

    private fun makeWorkflowState(includeSheet: Boolean): PaywallState.Loaded.Components {
        val packages = listOf(
            makePackage(PackageType.ANNUAL, 69_990_000, Period(1, Period.Unit.YEAR, "P1Y")),
            makePackage(PackageType.THREE_MONTH, 24_990_000, Period(3, Period.Unit.MONTH, "P3M")),
            makePackage(PackageType.MONTHLY, 10_990_000, Period(1, Period.Unit.MONTH, "P1M")),
        )
        val offering = Offering(
            identifier = offeringId, serverDescription = "Test", metadata = emptyMap(), availablePackages = packages,
        )
        val offerings = Offerings(offering, mapOf(offeringId to offering))
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
            componentsLocalizations = localizations,
            defaultLocaleIdentifier = localeId,
            offeringIdentifier = offeringId,
        )
        val workflow = PublishedWorkflow(
            id = "workflow", displayName = "Test", initialStepId = "paywall", singleStepFallbackId = "paywall",
            steps = mapOf("paywall" to WorkflowStep(
                id = "paywall", type = "screen", screenId = "screen",
                triggers = emptyList(), triggerActions = emptyMap(),
                paramValues = mapOf("offering" to JsonObject(mapOf("identifier" to JsonPrimitive(offeringId)))),
            )),
            screens = mapOf("screen" to screen), metadata = emptyMap(),
        )
        val purchases = mockk<PurchasesType>(relaxed = true) {
            every { storefrontCountryCode } returns "US"
            every { preferredUILocaleOverride } returns null
            every { purchasesAreCompletedBy } returns PurchasesAreCompletedBy.REVENUECAT
            coEvery { awaitOfferings() } returns offerings
            coEvery { awaitCustomerInfo(any()) } returns mockk {
                every { activeSubscriptions } returns emptySet()
                every { nonSubscriptionTransactions } returns emptyList()
            }
        }
        val viewModel = PaywallViewModelImpl(
            resourceProvider = MockResourceProvider(), purchases = purchases,
            options = PaywallOptions.Builder(dismissRequest = {}).build(),
            colorScheme = TestData.Constants.currentColorScheme, isDarkMode = false,
            shouldDisplayBlock = null, backgroundDispatcher = dispatcher,
        )
        viewModel.startWorkflowPresentationFromResult(workflow, offerings, null, UiConfig())
        return requireNotNull(viewModel.workflowState.value?.stepStates?.get("paywall"))
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
