package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.paywalls.components.ButtonComponent
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.URL

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS-w402dp-h874dp")
class TabControlDiscountTests {
    @get:Rule
    val compose = createComposeRule()

    private val localizations = mutableMapOf<LocalizationKey, LocalizationData>()
    private val annual = productPackage("essential_annual", 94_990_000, true)
    private val otherAnnual = productPackage("other_annual", 69_990_000, true, introMicros = 1_990_000)
    private val monthly = productPackage("essential_monthly", 13_990_000, false)
    private val sheetAnnual = productPackage("premium_annual", 129_990_000, true, introMicros = 4_990_000)
    private val sheetMonthly = productPackage("premium_monthly", 19_990_000, false)

    @Test
    fun `Tab badge uses its own plans when another tab is selected`() {
        render()
        compose.onNodeWithText("Yearly: 43%").assertExists()
        compose.onNodeWithText("Selected: $94.99").assertExists()

        compose.onNodeWithText("Monthly").performClick()

        compose.onNodeWithText("Yearly: 43%").assertExists()
        compose.onNodeWithText("Selected: $13.99").assertExists()
    }

    @Test
    fun `Tab badge preserves remembered tab selection while sheet selection stays shared`() {
        render()
        compose.onNodeWithText("Other annual").performClick()
        compose.onNodeWithText("Yearly: 58%", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Yearly offer: $1.99", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("All plans").performClick()
        compose.onNodeWithText("Premium annual").performClick()

        compose.onNodeWithText("Selected: $129.99", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Selected offer: $4.99", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Yearly offer: $1.99", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Yearly: 58%", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `Tab without packages keeps the shared package context`() {
        render(emptyAnnualTab = true)
        compose.onNodeWithText("Yearly: 46%").assertExists()
    }

    private fun render(emptyAnnualTab: Boolean = false) {
        val annualPlans = if (emptyAnnualTab) {
            listOf(TabControlComponent, text("No plans"))
        } else {
            listOf(
                TabControlComponent,
                plan(annual, "Essential annual", true),
                plan(monthly, "Essential monthly", false),
                plan(otherAnnual, "Other annual", false),
            )
        }
        val tabs = TabsComponent(
            control = TabsComponent.TabControl.Buttons(
                StackComponent(components = listOf(
                    TabControlButtonComponent(
                        tabIndex = 0,
                        tabId = "annual",
                        stack = StackComponent(components = listOf(
                            text("Yearly: {{ product.relative_discount }}"),
                            text("Yearly offer: {{ product.offer_price }}"),
                        )),
                    ),
                    TabControlButtonComponent(
                        tabIndex = 1,
                        tabId = "monthly",
                        stack = StackComponent(components = listOf(text("Monthly"))),
                    ),
                )),
            ),
            tabs = listOf(
                TabsComponent.Tab("annual", stack = StackComponent(components = annualPlans)),
                TabsComponent.Tab("monthly", stack = StackComponent(components = listOf(
                    TabControlComponent,
                    plan(monthly, "Monthly plan", true),
                ))),
            ),
            defaultTabId = "annual",
        )
        val sheet = ButtonComponent(
            action = ButtonComponent.Action.NavigateTo(ButtonComponent.Destination.Sheet(
                id = "all-plans",
                stack = StackComponent(components = listOf(
                    plan(sheetAnnual, "Premium annual", true), plan(sheetMonthly, "Premium monthly", false),
                )),
            )),
            stack = StackComponent(components = listOf(text("All plans"))),
        )
        val root = StackComponent(components = listOf(
            tabs, sheet, text("Selected: {{ product.price }}"), text("Selected offer: {{ product.offer_price }}"),
        ))
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
            availablePackages = listOf(annual, monthly, otherAnnual, sheetAnnual, sheetMonthly),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )
        val state = offering.toComponentsPaywallState(
            offering.validatePaywallComponentsDataOrNull()!!.getOrThrow(), storefrontCountryCode = "US",
        )
        compose.setContent { LoadedPaywallComponents(state, clickHandler = {}, modifier = Modifier.fillMaxSize()) }
    }

    private fun plan(pkg: Package, label: String, selected: Boolean) = PackageComponent(
        packageId = pkg.identifier, isSelectedByDefault = selected,
        stack = StackComponent(components = listOf(text(label))),
    )

    private fun text(value: String): TextComponent {
        val key = LocalizationKey(value)
        localizations[key] = LocalizationData.Text(value)
        return TextComponent(text = key, color = ColorScheme(ColorInfo.Hex(Color.Black.toArgb())))
    }

    private fun productPackage(id: String, micros: Long, annual: Boolean, introMicros: Long? = null) = Package(
        identifier = id, packageType = PackageType.CUSTOM,
        presentedOfferingContext = PresentedOfferingContext("offering"),
        product = TestStoreProduct(
            id = id, name = id, title = id, description = id,
            price = Price(amountMicros = micros, currencyCode = "USD", formatted = "$${micros / 1_000_000.0}"),
            period = Period(1, if (annual) Period.Unit.YEAR else Period.Unit.MONTH, if (annual) "P1Y" else "P1M"),
            introPricePricingPhase = introMicros?.let {
                PricingPhase(
                    billingPeriod = Period(1, Period.Unit.MONTH, "P1M"),
                    recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                    billingCycleCount = 1,
                    price = Price(amountMicros = it, currencyCode = "USD", formatted = "$${it / 1_000_000.0}"),
                )
            },
        ),
    )
}
