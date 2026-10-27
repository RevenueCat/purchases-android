package com.revenuecat.purchases.ui.revenuecatui.components.tabs

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.PaywallComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TabControlButtonComponent
import com.revenuecat.purchases.paywalls.components.TabControlComponent
import com.revenuecat.purchases.paywalls.components.TabsComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.StateDeclaration
import com.revenuecat.purchases.paywalls.components.common.StateUpdate
import com.revenuecat.purchases.paywalls.components.common.StateUpdateValue
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.MeasurePaywallBounds
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallStateStore
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import kotlinx.serialization.json.JsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(InternalRevenueCatAPI::class)
@RunWith(AndroidJUnit4::class)
internal class TabInitialSelectionTests {
    private val monthly = TestData.Packages.monthly
    private val annual = TestData.Packages.annual
    private val weekly = TestData.Packages.weekly
    private val color = ColorScheme(light = ColorInfo.Hex(0xFF000000.toInt()))
    private val declarations = mapOf("period" to StateDeclaration(
        type = StateDeclaration.ValueType.STRING,
        defaultValue = JsonPrimitive("monthly"),
    ))
    private val store = PaywallStateStore(declarations)
    private val mounted = mutableStateOf(true)

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `initial publication selects the revealed default instead of the inherited workflow fallback`() {
        val state = state()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)

        show(state)

        assertMonthlyPublished(state)
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
    }

    @Test
    fun `initial publication replaces an own selection that becomes hidden`() {
        val state = state(includeAnnual = true)
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)

        show(state)

        assertMonthlyPublished(state)
        composeTestRule.onNodeWithText("Annual plan").assertDoesNotExist()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
    }

    @Test
    fun `publication after remount preserves a user's still-visible selection`() {
        val state = state(includeWeekly = true)
        show(state)
        composeTestRule.onNodeWithText("Monthly plan").performClick()
        composeTestRule.onNodeWithText("Weekly plan").performClick()
        composeTestRule.waitForIdle()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(weekly)
        composeTestRule.runOnIdle { mounted.value = false }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            publishAnnual()
            mounted.value = true
        }
        composeTestRule.waitForIdle()

        assertMonthlyPublished(state)
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(weekly)
    }

    private fun show(state: PaywallState.Loaded.Components) {
        composeTestRule.setContent {
            MeasurePaywallBounds(state, Modifier.requiredSize(300.dp, 400.dp)) {
                if (mounted.value) ComponentView(state.stack, state, onClick = {})
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun assertMonthlyPublished(state: PaywallState.Loaded.Components) {
        assertThat(state.selectedTabIndex).isZero()
        assertThat(store.currentValueOrDefault("period")).isEqualTo(JsonPrimitive("monthly"))
        composeTestRule.onNodeWithText("Monthly plan").assertIsDisplayed()
    }

    private fun state(includeAnnual: Boolean = false, includeWeekly: Boolean = false): PaywallState.Loaded.Components {
        // A previous workflow step selected annual in the shared presentation store.
        publishAnnual()
        val previousStep = FakePaywallState(
            components = listOf(pkg(annual, default = true)),
            packages = listOf(annual),
            localizations = localizations(),
            addPackageComponents = false,
            stateDeclarations = declarations,
            stateStore = store,
        )
        val components = buildList {
            add(TabsComponent(
                defaultTabId = "monthly",
                tabs = listOf("monthly", "annual").map { id ->
                    TabsComponent.Tab(id = id, stack = StackComponent(listOf(TabControlComponent)))
                },
                control = TabsComponent.TabControl.Buttons(StackComponent(listOf(
                    TabControlButtonComponent(0, "monthly", StackComponent(listOf(text("tabMonthly")))),
                    TabControlButtonComponent(1, "annual", StackComponent(listOf(text("tabAnnual")))),
                ))),
                stateUpdates = listOf(StateUpdate.Set("period", StateUpdateValue.PayloadReference)),
            ))
            if (includeAnnual) add(visibleForPeriod("annual", pkg(annual, default = true)))
            add(visibleForPeriod("monthly", pkg(monthly, default = true)))
            if (includeWeekly) add(pkg(weekly))
        }
        return FakePaywallState(
            components = components,
            packages = listOf(monthly, annual, weekly),
            localizations = localizations(),
            addPackageComponents = false,
            stateDeclarations = declarations,
            stateStore = store,
        ).also { it.setDefaultPackage(previousStep.selectedPackageInfo!!) }
    }

    private fun publishAnnual() = store.applyUpdates(listOf(
        StateUpdate.Set("period", StateUpdateValue.Literal(JsonPrimitive("annual"))),
    ))

    private fun visibleForPeriod(period: String, component: PaywallComponent) = StackComponent(
        components = listOf(component),
        visible = false,
        overrides = listOf(ComponentOverride(
            conditions = listOf(ComponentOverride.Condition.State(
                operator = ComponentOverride.EqualityOperator.EQUALS,
                name = "period",
                value = JsonPrimitive(period),
            )),
            properties = PartialStackComponent(visible = true),
        )),
    )

    private fun pkg(pkg: Package, default: Boolean = false) = PackageComponent(
        packageId = pkg.identifier,
        isSelectedByDefault = default,
        stack = StackComponent(listOf(text(pkg.identifier))),
    )

    private fun text(key: String) = TextComponent(LocalizationKey(key), color)

    private fun localizations() = nonEmptyMapOf(LocaleId("en_US") to nonEmptyMapOf(
        LocalizationKey(monthly.identifier) to LocalizationData.Text("Monthly plan"),
        LocalizationKey(annual.identifier) to LocalizationData.Text("Annual plan"),
        LocalizationKey(weekly.identifier) to LocalizationData.Text("Weekly plan"),
        LocalizationKey("tabMonthly") to LocalizationData.Text("Monthly"),
        LocalizationKey("tabAnnual") to LocalizationData.Text("Annual"),
    ))
}
