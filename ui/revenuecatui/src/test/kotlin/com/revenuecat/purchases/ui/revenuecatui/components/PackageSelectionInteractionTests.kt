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
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PartialPackageComponent
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
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabsComponentStyle
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
    private val monthlyXl = Package("monthly_xl", PackageType.CUSTOM, monthly.product, monthly.presentedOfferingContext)
    private val annualXl = Package("annual_xl", PackageType.CUSTOM, annual.product, annual.presentedOfferingContext)
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

    @Test
    fun `competing defaults resolve on initial tab entry and sheet entry without a sheet cycle`() {
        val tabs = TabsComponent(
            tabs = listOf(tab("first", pkg(annual, default = true)), tab("second", pkg(monthlyXl, default = true))),
            control = TabsComponent.TabControl.Buttons(StackComponent(listOf(
                TabControlButtonComponent(0, "first", StackComponent(listOf(text("first")))),
                TabControlButtonComponent(1, "second", StackComponent(listOf(text("second")))),
            ))),
        )
        val state = state(pkg(monthly, default = true), tabs, sheetButton(pkg(annualXl, default = true)))
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
        show(state)

        composeTestRule.onNodeWithText("Plans").performClick()
        composeTestRule.waitForIdle()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthlyXl)

        val button = (state.stack as StackComponentStyle).children[2] as ButtonComponentStyle
        composeTestRule.runOnIdle {
            state.sheet.show(sheetDestination(button), state, PaywallComponentInteractionTracker { }, onClick = {})
            assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annualXl)
            state.sheet.hide()
            assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthlyXl)
        }
    }

    @Test
    fun `sheet entry before measurement and sheet choices do not overwrite destination tab memory`() {
        val tabs = TabsComponent(
            tabs = listOf(
                tab("first", pkg(monthly), pkg(annual), sheetButton(pkg(monthly, default = true))),
                tab("second", pkg(monthlyXl, default = true)),
            ),
            control = TabsComponent.TabControl.Buttons(StackComponent(emptyList())),
        )
        val state = state(tabs)
        state.update(selectedPackageUniqueId = annual.identifier)
        val tabStyle = (state.stack as StackComponentStyle).children[0] as TabsComponentStyle
        val button = tabStyle.tabs[0].stack.children[3] as ButtonComponentStyle

        state.sheet.show(sheetDestination(button), state, PaywallComponentInteractionTracker { }, onClick = {})

        assertThat(state.paywallBoundsDp).isNull()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
        // The sheet's package also occurs on the tab, so neither entry nor manual selection may save it there.
        state.update(selectedPackageUniqueId = monthly.identifier)
        state.sheet.hide()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
        state.update(selectedTabIndex = 1)
        state.update(selectedTabIndex = 0)
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
    }

    @Test
    fun `a manual self hiding selection is not replaced by defaults during recomposition`() {
        val state = state(
            pkg(annual, default = true),
            PackageComponent(
                packageId = monthly.identifier,
                isSelectedByDefault = false,
                stack = StackComponent(listOf(text(monthly.identifier))),
                overrides = listOf(ComponentOverride(
                    conditions = listOf(ComponentOverride.Condition.Selected),
                    properties = PartialPackageComponent(visible = false),
                )),
            ),
        )
        show(state)
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)

        composeTestRule.onNodeWithText("Monthly plan").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Monthly plan").assertDoesNotExist()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
    }

    private fun sheetButton(vararg children: PaywallComponent) = ButtonComponent(
        action = ButtonComponent.Action.NavigateTo(ButtonComponent.Destination.Sheet(
            id = "plans",
            stack = StackComponent(children.toList()),
        )),
        stack = StackComponent(emptyList()),
    )

    private fun sheetDestination(button: ButtonComponentStyle) =
        (button.action as ButtonComponentStyle.Action.NavigateTo).destination
            as ButtonComponentStyle.Action.NavigateTo.Destination.Sheet

    private fun show(
        state: PaywallState.Loaded.Components,
        tracker: PaywallComponentInteractionTracker = PaywallComponentInteractionTracker { },
    ) {
        composeTestRule.setContent {
            MeasurePaywallBounds(state, Modifier.requiredSize(300.dp, 400.dp)) {
                resolveHeroInsets(state.stack, state)
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
        packages = listOf(monthly, annual, monthlyXl, annualXl),
        localizations = nonEmptyMapOf(LocaleId("en_US") to nonEmptyMapOf(
            LocalizationKey(monthly.identifier) to LocalizationData.Text("Monthly plan"),
            LocalizationKey(annual.identifier) to LocalizationData.Text("Annual plan"),
            LocalizationKey(monthlyXl.identifier) to LocalizationData.Text("Monthly XL plan"),
            LocalizationKey(annualXl.identifier) to LocalizationData.Text("Annual XL plan"),
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
