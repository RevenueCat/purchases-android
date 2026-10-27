package com.revenuecat.purchases.ui.revenuecatui.components.tabs

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TabControlButtonComponent
import com.revenuecat.purchases.paywalls.components.TabControlComponent
import com.revenuecat.purchases.paywalls.components.TabControlToggleComponent
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
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import kotlinx.serialization.json.JsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(InternalRevenueCatAPI::class)
@RunWith(AndroidJUnit4::class)
internal class TabSelectionStateTests {
    private val monthly = TestData.Packages.monthly
    private val annual = TestData.Packages.annual
    private val color = ColorScheme(light = ColorInfo.Hex(0xFF000000.toInt()))
    private val stateKey = "active_tab"

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `tab button publishes state and resolves the revealed default before tracking the interaction`() {
        val state = state()
        val observed = mutableListOf<Pair<Package?, JsonPrimitive?>>()
        show(state, PaywallComponentInteractionTracker {
            observed.add(state.selectedPackageInfo?.rcPackage to state.stateStore.currentValueOrDefault(stateKey))
        })
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)

        composeTestRule.onNodeWithText("Plans").performClick()
        composeTestRule.waitForIdle()

        assertDestinationSelected(state)
        assertThat(observed).containsExactly(monthly to JsonPrimitive("second"))
    }

    @Test
    fun `returning to a tab preserves the user's visible choice`() {
        val state = state()
        show(state)
        composeTestRule.onNodeWithText("Plans").performClick()
        composeTestRule.waitForIdle()
        assertDestinationSelected(state)

        composeTestRule.onNodeWithText("Annual backup").performClick()
        composeTestRule.waitForIdle()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
        composeTestRule.onNodeWithText("First").performClick()
        composeTestRule.onNodeWithText("Plans").performClick()
        composeTestRule.waitForIdle()

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
        assertThat(state.stateStore.currentValueOrDefault(stateKey)).isEqualTo(JsonPrimitive("second"))
    }

    @Test
    fun `tab toggle publishes state before resolving and tracking the revealed default`() {
        val state = state(toggle = true)
        val observed = mutableListOf<Pair<Package?, JsonPrimitive?>>()
        show(state, PaywallComponentInteractionTracker {
            observed.add(state.selectedPackageInfo?.rcPackage to state.stateStore.currentValueOrDefault(stateKey))
        })

        composeTestRule.onNode(isToggleable()).performClick()
        composeTestRule.waitForIdle()

        assertDestinationSelected(state)
        assertThat(observed).containsExactly(monthly to JsonPrimitive("second"))
    }

    @Test
    fun `clicking the selected tab does not republish state changed by another component`() {
        val state = state()
        show(state)
        composeTestRule.runOnIdle {
            state.stateStore.applyUpdates(listOf(StateUpdate.Set(
                stateKey,
                StateUpdateValue.Literal(JsonPrimitive("external")),
            )))
        }

        composeTestRule.onNodeWithText("First").performClick()
        composeTestRule.waitForIdle()

        assertThat(state.stateStore.currentValueOrDefault(stateKey)).isEqualTo(JsonPrimitive("external"))
    }

    private fun show(
        state: PaywallState.Loaded.Components,
        tracker: PaywallComponentInteractionTracker = PaywallComponentInteractionTracker { },
    ) {
        composeTestRule.setContent {
            MeasurePaywallBounds(state, Modifier.requiredSize(300.dp, 400.dp)) {
                ComponentView(style = state.stack, state = state, onClick = {}, componentInteractionTracker = tracker)
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun assertDestinationSelected(state: PaywallState.Loaded.Components) {
        assertThat(state.selectedTabIndex).isEqualTo(1)
        assertThat(state.stateStore.currentValueOrDefault(stateKey)).isEqualTo(JsonPrimitive("second"))
        composeTestRule.onNodeWithText("Monthly plan").assertIsDisplayed()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
    }

    private fun state(toggle: Boolean = false): PaywallState.Loaded.Components {
        val tabs = TabsComponent(
            tabs = listOf(
                TabsComponent.Tab(
                    id = "first",
                    stack = StackComponent(listOf(TabControlComponent, pkg(annual, true, "entry"))),
                ),
                TabsComponent.Tab(
                    id = "second",
                    stack = StackComponent(listOf(
                        TabControlComponent,
                        StackComponent(
                            components = listOf(pkg(monthly, true, "monthly")),
                            visible = false,
                            overrides = listOf(ComponentOverride(
                                conditions = listOf(ComponentOverride.Condition.State(
                                    operator = ComponentOverride.EqualityOperator.EQUALS,
                                    name = stateKey,
                                    value = JsonPrimitive("second"),
                                )),
                                properties = PartialStackComponent(visible = true),
                            )),
                        ),
                        pkg(annual, false, "backup"),
                    )),
                ),
            ),
            control = if (toggle) toggleControl() else buttonControl(),
            stateUpdates = listOf(StateUpdate.Set(stateKey, StateUpdateValue.PayloadReference)),
        )
        return FakePaywallState(
            components = listOf(tabs),
            packages = listOf(monthly, annual),
            localizations = nonEmptyMapOf(LocaleId("en_US") to nonEmptyMapOf(
                LocalizationKey("entry") to LocalizationData.Text("Annual entry"),
                LocalizationKey("monthly") to LocalizationData.Text("Monthly plan"),
                LocalizationKey("backup") to LocalizationData.Text("Annual backup"),
                LocalizationKey("tab0") to LocalizationData.Text("First"),
                LocalizationKey("tab1") to LocalizationData.Text("Plans"),
            )),
            addPackageComponents = false,
            stateDeclarations = mapOf(stateKey to StateDeclaration(
                type = StateDeclaration.ValueType.STRING,
                defaultValue = JsonPrimitive("first"),
            )),
        )
    }

    private fun buttonControl() = TabsComponent.TabControl.Buttons(StackComponent(listOf(
        TabControlButtonComponent(0, "first", StackComponent(listOf(text("tab0")))),
        TabControlButtonComponent(1, "second", StackComponent(listOf(text("tab1")))),
    )))

    private fun toggleControl() = TabsComponent.TabControl.Toggle(StackComponent(listOf(TabControlToggleComponent(
        defaultValue = false,
        thumbColorOn = color,
        thumbColorOff = color,
        trackColorOn = color,
        trackColorOff = color,
    ))))

    private fun pkg(pkg: Package, default: Boolean, textKey: String) = PackageComponent(
        packageId = pkg.identifier,
        isSelectedByDefault = default,
        stack = StackComponent(listOf(text(textKey))),
    )

    private fun text(key: String) = TextComponent(LocalizationKey(key), color)
}
