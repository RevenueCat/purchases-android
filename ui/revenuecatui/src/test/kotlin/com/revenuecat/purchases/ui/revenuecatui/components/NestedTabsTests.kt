package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.TabControlToggleComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TabControlButtonComponent
import com.revenuecat.purchases.paywalls.components.TabControlComponent
import com.revenuecat.purchases.paywalls.components.TabsComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.extensions.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.validatePaywallComponentsDataOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.URL

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS-w402dp-h874dp")
class NestedTabsTests {
    @get:Rule
    val compose = createComposeRule()

    private val localizations = mutableMapOf<LocalizationKey, LocalizationData>()
    private val silverAnnual = productPackage("silver_annual", 60_000_000, true)
    private val silverMonthly = productPackage("silver_monthly", 6_000_000, false)
    private val goldAnnual = productPackage("gold_annual", 120_000_000, true)
    private val goldMonthly = productPackage("gold_monthly", 12_000_000, false)

    @Test
    fun `Nested tabs preserve tier and duration selections and select the visible product`() {
        val state = render()
        compose.onNodeWithText("Gold active").assertExists()
        compose.onNodeWithText("Annual active").assertExists()
        compose.onNodeWithText("Gold annual plan").assertExists()
        compose.onNodeWithText("Selected: $120.00").assertExists()
        compose.onNodeWithText("Monthly").performClick()
        compose.onNodeWithText("Gold active").assertExists()
        compose.onNodeWithText("Monthly active").assertExists()
        compose.onNodeWithText("Gold monthly plan").assertExists()
        compose.onNodeWithText("Selected: $12.00").assertExists()
        compose.runOnIdle { assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(goldMonthly) }
        compose.onNodeWithText("Silver").performClick()
        compose.onNodeWithText("Silver annual plan").assertExists()
        compose.onNodeWithText("Selected: $60.00").assertExists()
        compose.onNodeWithText("Gold").performClick()
        compose.onNodeWithText("Gold active").assertExists()
        compose.onNodeWithText("Monthly active").assertExists()
        compose.onNodeWithText("Gold monthly plan").assertExists()
        compose.onNodeWithText("Selected: $12.00").assertExists()
        compose.runOnIdle { assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(goldMonthly) }
    }

    @Test
    fun `Nested toggle default and changes do not change the parent tier`() {
        val state = render(toggle = true)
        compose.onNodeWithText("Gold monthly plan").assertExists()
        compose.onNode(isToggleable()).performClick()
        compose.onNodeWithText("Gold annual plan").assertExists()
        compose.onNodeWithText("Gold active").assertExists()
        compose.runOnIdle { assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(goldAnnual) }
    }

    @Test
    fun `Sheet tabs preserve the nested selection after dismissal`() {
        val state = render(includeSheet = true)
        compose.runOnIdle { assertThat(state.selectedTabIndex).isEqualTo(1) }
        compose.onNodeWithText("Monthly").performClick()
        compose.onNodeWithText("All plans").performClick()
        compose.onNodeWithText("Sheet annual plan", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Sheet monthly").performClick()
        compose.runOnIdle { assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(silverMonthly) }
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Gold monthly plan").assertExists()
        compose.runOnIdle { assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(goldMonthly) }
    }

    @Test
    fun `Sheet tabs prefer their default and remember a manual choice over a root package`() {
        val state = render(includeSheet = true, includeSheetRoot = true)
        compose.onNodeWithText("All plans").performClick()
        compose.runOnIdle { assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(silverAnnual) }
        compose.onNodeWithText("Sheet monthly").performClick()
        compose.onNodeWithText("Sheet annual").performClick()
        compose.runOnIdle { assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(silverAnnual) }
        compose.onNodeWithText("Sheet alternative", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Sheet monthly").performClick()
        compose.runOnIdle { assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(silverMonthly) }
        compose.onNodeWithText("Sheet annual").performClick()
        compose.runOnIdle { assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(goldAnnual) }
        compose.onNodeWithText("Back").performClick()
        compose.runOnIdle { assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(goldAnnual) }
    }

    @Test
    fun `Closed sheet tabs do not change the main paywall discount`() {
        val comparison = productPackage("comparison", 24_000_000, false)
        val expensiveSheet = productPackage("sheet_annual", 480_000_000, true)
        val sheet = ButtonComponent(
            action = ButtonComponent.Action.NavigateTo(ButtonComponent.Destination.Sheet(
                id = "expensive-sheet",
                stack = StackComponent(components = listOf(
                    tabs(listOf("Sheet plan"), listOf(StackComponent(components = listOf(
                        TabControlComponent, plan(expensiveSheet, "Sheet annual", true),
                    ))), 0),
                    ButtonComponent(action = ButtonComponent.Action.NavigateBack,
                        stack = StackComponent(components = listOf(text("Back")))),
                )),
            )),
            stack = StackComponent(components = listOf(text("All plans"))),
        )
        render(StackComponent(components = listOf(
            duration("Gold", goldAnnual, goldMonthly, false),
            plan(comparison, "Comparison plan", false),
            text("Discount: {{ product.relative_discount }}"), sheet,
        )), additionalPackages = listOf(comparison, expensiveSheet))
        compose.onNodeWithText("Discount:", substring = true, useUnmergedTree = true).assertTextEquals("Discount: 58%")
        compose.onNodeWithText("All plans").performClick()
        compose.onNodeWithText("Sheet annual", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Discount:", substring = true, useUnmergedTree = true).assertTextEquals("Discount: 58%")
    }

    @Test
    fun `Nested tabs without packages preserve a manual choice in their parent`() {
        val details = tabs(listOf("Less", "More"), listOf(
            StackComponent(components = listOf(TabControlComponent, text("Summary"))),
            StackComponent(components = listOf(TabControlComponent, text("Details"))),
        ), 0)
        val tiers = tabs(listOf("Silver", "Gold"), listOf(
            StackComponent(components = listOf(TabControlComponent, text("Silver tier"))),
            StackComponent(components = listOf(TabControlComponent,
                plan(goldAnnual, "Gold annual plan", true), plan(goldMonthly, "Gold monthly plan", false), details)),
        ), 1)
        val state = render(StackComponent(components = listOf(tiers)))
        compose.onNodeWithText("Gold monthly plan").performClick()
        compose.onNodeWithText("More").performClick()
        compose.onNodeWithText("Details").assertExists()
        compose.runOnIdle { assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(goldMonthly) }
    }

    @Test
    fun `Closed sheet products do not supply parent tab control prices`() {
        val sheet = ButtonComponent(
            action = ButtonComponent.Action.NavigateTo(ButtonComponent.Destination.Sheet(
                id = "tab-sheet",
                stack = StackComponent(components = listOf(
                    tabs(listOf("Sheet annual"), listOf(StackComponent(components = listOf(
                        TabControlComponent, plan(silverAnnual, "Sheet annual plan", true),
                    ))), 0),
                )),
            )),
            stack = StackComponent(components = listOf(text("All plans"))),
        )
        render(StackComponent(components = listOf(tabs(
            listOf("Other: {{ product.price }}", "Gold"),
            listOf(
                StackComponent(components = listOf(TabControlComponent, sheet)),
                StackComponent(components = listOf(TabControlComponent, plan(goldMonthly, "Gold plan", true))),
            ), 1,
        ))))
        compose.onNodeWithText("Other: $12.00").assertExists()
    }

    private fun tabs(labels: List<String>, contents: List<StackComponent>, default: Int, toggle: Boolean = false) = TabsComponent(
        control = if (toggle) TabsComponent.TabControl.Toggle(StackComponent(components = listOf(
            TabControlToggleComponent(defaultValue = true, thumbColorOn = color, thumbColorOff = color,
                trackColorOn = color, trackColorOff = color),
        ))) else TabsComponent.TabControl.Buttons(StackComponent(components = labels.mapIndexed { index, label ->
            TabControlButtonComponent(tabIndex = index, tabId = label,
                stack = StackComponent(components = listOf(controlText(label))))
        })),
        tabs = labels.zip(contents).map { (label, stack) -> TabsComponent.Tab(label, stack = stack) },
        defaultTabId = if (toggle) null else labels[default],
    )

    private fun duration(tier: String, annual: Package, monthly: Package, toggle: Boolean) = tabs(
        listOf("Annual", "Monthly"),
        listOf(annual, monthly).mapIndexed { index, pkg -> StackComponent(components = listOf(
            TabControlComponent, plan(pkg, "$tier ${if (index == 0) "annual" else "monthly"} plan", true),
        )) }, 0, toggle,
    )

    private fun render(toggle: Boolean = false, includeSheet: Boolean = false, includeSheetRoot: Boolean = false): PaywallState.Loaded.Components {
        val tiers = tabs(listOf("Silver", "Gold"), listOf(
            StackComponent(components = listOf(TabControlComponent, duration("Silver", silverAnnual, silverMonthly, toggle))),
            StackComponent(components = listOf(TabControlComponent, duration("Gold", goldAnnual, goldMonthly, toggle))),
        ), 1)
        val sheet = ButtonComponent(
            action = ButtonComponent.Action.NavigateTo(ButtonComponent.Destination.Sheet(
                id = "all-plans",
                stack = StackComponent(components = listOfNotNull(
                    plan(goldMonthly, "Sheet root", true).takeIf { includeSheetRoot },
                    tabs(listOf("Sheet annual", "Sheet monthly"), listOf(silverAnnual, silverMonthly).mapIndexed { index, pkg ->
                        StackComponent(components = listOfNotNull(TabControlComponent,
                            plan(pkg, "Sheet ${if (index == 0) "annual" else "monthly"} plan", true),
                            plan(goldAnnual, "Sheet alternative", false).takeIf { includeSheetRoot && index == 0 }))
                    }, 0),
                    ButtonComponent(action = ButtonComponent.Action.NavigateBack,
                        stack = StackComponent(components = listOf(text("Back")))),
                )),
            )),
            stack = StackComponent(components = listOf(text("All plans"))),
        )
        val root = StackComponent(components =
            (if (includeSheet) listOf(sheet) else emptyList()) +
                listOf(tiers, text("Selected: {{ product.price }}")))
        return render(root)
    }

    private fun render(root: StackComponent, additionalPackages: List<Package> = emptyList()): PaywallState.Loaded.Components {
        val data = PaywallComponentsData(
            templateName = "components", assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(base = PaywallComponentsConfig(
                stack = root, background = Background.Color(ColorScheme(ColorInfo.Hex(Color.White.toArgb()))),
                stickyFooter = null,
            )),
            componentsLocalizations = mapOf(LocaleId("en_US") to localizations),
            defaultLocaleIdentifier = LocaleId("en_US"),
        )
        val offering = Offering(
            identifier = "offering", serverDescription = "description", metadata = emptyMap(),
            availablePackages = listOf(silverAnnual, silverMonthly, goldAnnual, goldMonthly) + additionalPackages,
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )
        val state = offering.toComponentsPaywallState(
            offering.validatePaywallComponentsDataOrNull()!!.getOrThrow(), storefrontCountryCode = "US",
        )
        compose.setContent { LoadedPaywallComponents(state, clickHandler = {}, modifier = Modifier.fillMaxSize()) }
        return state
    }

    private fun plan(pkg: Package, label: String, selected: Boolean) = PackageComponent(
        packageId = pkg.identifier, isSelectedByDefault = selected,
        stack = StackComponent(components = listOf(text(label))),
    )

    private val color = ColorScheme(ColorInfo.Hex(Color.Black.toArgb()))

    private fun controlText(label: String): TextComponent {
        val active = text("$label active")
        return TextComponent(text = text(label).text, color = color, overrides = listOf(ComponentOverride(
            conditions = listOf(ComponentOverride.Condition.Selected),
            properties = PartialTextComponent(text = active.text),
        )))
    }

    private fun text(value: String): TextComponent {
        val key = LocalizationKey(value)
        localizations[key] = LocalizationData.Text(value)
        return TextComponent(text = key, color = ColorScheme(ColorInfo.Hex(Color.Black.toArgb())))
    }

    private fun productPackage(id: String, micros: Long, annual: Boolean) = Package(
        identifier = id, packageType = PackageType.CUSTOM,
        presentedOfferingContext = PresentedOfferingContext("offering"),
        product = TestStoreProduct(
            id = id, name = id, title = id, description = id,
            price = Price(amountMicros = micros, currencyCode = "USD", formatted = "$${micros / 1_000_000.0}"),
            period = Period(1, if (annual) Period.Unit.YEAR else Period.Unit.MONTH, if (annual) "P1Y" else "P1M"),
        ),
    )
}
