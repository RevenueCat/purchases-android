package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.CountdownComponent
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
import java.util.Date

@OptIn(InternalRevenueCatAPI::class)
@RunWith(AndroidJUnit4::class)
internal class PackageSelectionInteractionTests {
    private val monthly = TestData.Packages.monthly
    private val annual = TestData.Packages.annual
    private val color = ColorScheme(light = ColorInfo.Hex(0xFF000000.toInt()))
    private var nowMillis = 10_000L

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `tab publication reveals and selects its default before tracking the click`() {
        val tabs = TabsComponent(
            tabs = listOf(
                tab("first", pkg(annual, default = true)),
                tab(
                    "second",
                    StackComponent(
                        components = listOf(pkg(monthly, default = true)),
                        visible = false,
                        overrides = listOf(ComponentOverride(
                            conditions = listOf(ComponentOverride.Condition.State(
                                operator = ComponentOverride.EqualityOperator.EQUALS,
                                name = "active_tab",
                                value = JsonPrimitive("second"),
                            )),
                            properties = PartialStackComponent(visible = true),
                        )),
                    ),
                    pkg(annual),
                ),
            ),
            control = TabsComponent.TabControl.Buttons(StackComponent(listOf(
                TabControlButtonComponent(0, "first", StackComponent(listOf(text("first")))),
                TabControlButtonComponent(1, "second", StackComponent(listOf(text("second")))),
            ))),
            stateUpdates = listOf(StateUpdate.Set("active_tab", StateUpdateValue.PayloadReference)),
        )
        val state = state(tabs, declarations = mapOf("active_tab" to StateDeclaration(
            type = StateDeclaration.ValueType.STRING,
            defaultValue = JsonPrimitive("first"),
        )))
        val observed = mutableListOf<Pair<Package?, JsonPrimitive?>>()
        show(state, PaywallComponentInteractionTracker {
            observed.add(state.selectedPackageInfo?.rcPackage to state.stateStore.currentValueOrDefault("active_tab"))
        })
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)

        composeTestRule.onNodeWithText("Plans").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Monthly plan").assertIsDisplayed()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
        assertThat(observed).containsExactly(monthly to JsonPrimitive("second"))
    }

    @Test
    fun `countdown expiry selects the newly displayed default`() {
        val state = state(CountdownComponent(
            style = CountdownComponent.CountdownStyle(type = "date", date = Date(12_000L)),
            countdownStack = StackComponent(listOf(pkg(annual, default = true))),
            endStack = StackComponent(listOf(pkg(monthly, default = true))),
        ))
        show(state)
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)

        composeTestRule.runOnIdle { nowMillis = 12_000L }
        composeTestRule.mainClock.advanceTimeBy(1_100)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Annual plan").assertDoesNotExist()
        composeTestRule.onNodeWithText("Monthly plan").assertIsDisplayed()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
    }

    private fun show(
        state: PaywallState.Loaded.Components,
        tracker: PaywallComponentInteractionTracker = PaywallComponentInteractionTracker { },
    ) {
        composeTestRule.setContent {
            MeasurePaywallBounds(state, Modifier.requiredSize(300.dp, 400.dp)) {
                ComponentView(state.stack, state, onClick = {}, componentInteractionTracker = tracker)
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun state(
        vararg components: PaywallComponent,
        declarations: Map<String, StateDeclaration> = emptyMap(),
    ) = FakePaywallState(
        components = components.toList(),
        packages = listOf(monthly, annual),
        localizations = nonEmptyMapOf(LocaleId("en_US") to nonEmptyMapOf(
            LocalizationKey(monthly.identifier) to LocalizationData.Text("Monthly plan"),
            LocalizationKey(annual.identifier) to LocalizationData.Text("Annual plan"),
            LocalizationKey("first") to LocalizationData.Text("First"),
            LocalizationKey("second") to LocalizationData.Text("Plans"),
        )),
        addPackageComponents = false,
        stateDeclarations = declarations,
        dateProvider = { Date(nowMillis) },
    )

    private fun tab(id: String, vararg children: PaywallComponent) = TabsComponent.Tab(
        id = id,
        stack = StackComponent(listOf(TabControlComponent) + children),
    )

    private fun pkg(pkg: Package, default: Boolean = false) = PackageComponent(
        packageId = pkg.identifier,
        isSelectedByDefault = default,
        stack = StackComponent(listOf(text(pkg.identifier))),
    )

    private fun text(key: String) = TextComponent(LocalizationKey(key), color)
}
