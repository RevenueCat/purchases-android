package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PartialPackageComponent
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.PaywallComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TabsComponent
import com.revenuecat.purchases.paywalls.components.TabControlComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.properties.Badge
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.show
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Date

@OptIn(InternalRevenueCatAPI::class)
internal class PackageSelectionVisibilityTests {
    private val monthly = TestData.Packages.monthly
    private val annual = TestData.Packages.annual
    private val weekly = TestData.Packages.weekly
    private val wide = DpSize(800.dp, 600.dp)
    private val wideCondition = ComponentOverride.Condition.WindowWidthRule(
        operator = ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL,
        value = 700.0,
    )
    private val hideStackOnWide = ComponentOverride(
        conditions = listOf(wideCondition),
        properties = PartialStackComponent(visible = false),
    )

    @Test
    fun `default revealed after measurement is selected`() {
        val state = state(
            PackageComponent(
                packageId = monthly.identifier,
                isSelectedByDefault = true,
                visible = false,
                stack = StackComponent(emptyList()),
                overrides = listOf(ComponentOverride(
                    conditions = listOf(wideCondition),
                    properties = PartialPackageComponent(visible = true),
                )),
            ),
        )
        assertThat(state.selectedPackageInfo).isNull()

        state.reconcileSelectionForWindowSize(wide)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
    }

    @Test
    fun `size class can reveal an initially unselected default in a narrow pane`() {
        val state = state(
            PackageComponent(
                packageId = monthly.identifier,
                isSelectedByDefault = true,
                visible = false,
                stack = StackComponent(emptyList()),
                overrides = listOf(ComponentOverride(
                    conditions = listOf(ComponentOverride.Condition.Medium),
                    properties = PartialPackageComponent(visible = true),
                )),
            ),
        )
        state.windowScreenCondition = ScreenCondition.MEDIUM

        state.reconcileSelectionForWindowSize(DpSize(400.dp, 600.dp))

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
    }

    @Test
    fun `a paywall without a default stays unselected after measurement`() {
        val state = state(pkg(monthly))

        state.reconcileSelectionForWindowSize(wide)

        assertThat(state.selectedPackageInfo).isNull()
    }

    @Test
    fun `selection moves off a package hidden by an enclosing stack`() {
        val state = state(hiddenStack(pkg(monthly, default = true)), pkg(annual))

        state.reconcileSelectionForWindowSize(wide)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
    }

    @Test
    fun `replacement skips both hidden containers and hidden package content`() {
        val hiddenCurrent = PackageComponent(
            packageId = monthly.identifier,
            isSelectedByDefault = true,
            stack = StackComponent(emptyList()),
            overrides = listOf(ComponentOverride(
                conditions = listOf(wideCondition),
                properties = PartialPackageComponent(visible = false),
            )),
        )
        val hiddenContent = PackageComponent(
            packageId = annual.identifier,
            isSelectedByDefault = false,
            stack = StackComponent(emptyList(), overrides = listOf(hideStackOnWide)),
        )
        val state = state(hiddenCurrent, hiddenContent, hiddenStack(pkg(annual)), pkg(weekly))

        state.reconcileSelectionForWindowSize(wide)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(weekly)
    }

    @Test
    fun `selected rules on the content stack cannot hide the replacement`() {
        val state = state(
            hiddenStack(pkg(monthly, default = true)),
            PackageComponent(
                packageId = annual.identifier,
                isSelectedByDefault = true,
                stack = StackComponent(
                    emptyList(),
                    overrides = listOf(ComponentOverride(
                        conditions = listOf(ComponentOverride.Condition.Selected),
                        properties = PartialStackComponent(visible = false),
                    )),
                ),
            ),
            pkg(weekly),
        )

        state.reconcileSelectionForWindowSize(wide)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(weekly)
    }

    @Test
    fun `identical copies in different tabs retain their own container paths`() {
        val state = state(
            TabsComponent(
                tabs = listOf(
                    TabsComponent.Tab(id = "first", stack = StackComponent(listOf(pkg(monthly, default = true)))),
                    TabsComponent.Tab(
                        id = "second",
                        stack = StackComponent(listOf(hiddenStack(pkg(monthly, default = true)), pkg(annual))),
                    ),
                ),
                control = TabsComponent.TabControl.Buttons(StackComponent(emptyList())),
            ),
        )
        state.paywallBoundsDp = wide

        state.update(selectedTabIndex = 1)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
    }

    @Test
    fun `a visible copy under another root container keeps the selection`() {
        val state = state(hiddenStack(pkg(monthly, default = true)), pkg(monthly), pkg(annual))

        state.reconcileSelectionForWindowSize(wide)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
    }

    @Test
    fun `an expired countdown cannot keep an invisible selection alive`() {
        val state = state(
            hiddenStack(pkg(monthly, default = true)),
            pkg(annual),
            CountdownComponent(
                style = CountdownComponent.CountdownStyle(type = "date", date = Date(0)),
                countdownStack = StackComponent(listOf(pkg(monthly))),
                endStack = StackComponent(emptyList()),
            ),
        )

        state.reconcileSelectionForWindowSize(wide)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
    }

    @Test
    fun `an unrendered countdown fallback cannot keep an invisible selection alive`() {
        val state = state(
            hiddenStack(pkg(monthly, default = true)),
            pkg(annual),
            CountdownComponent(
                style = CountdownComponent.CountdownStyle(type = "date", date = Date(0)),
                countdownStack = StackComponent(emptyList()),
                fallback = StackComponent(listOf(pkg(monthly))),
            ),
        )

        state.reconcileSelectionForWindowSize(wide)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
    }

    @Test
    fun `a replaced badge cannot keep an invisible selection alive`() {
        fun badge(vararg children: PaywallComponent) = Badge(
            stack = StackComponent(children.toList()),
            style = Badge.Style.Overlay,
            alignment = TwoDimensionalAlignment.TOP,
        )
        val state = state(
            hiddenStack(pkg(monthly, default = true)),
            pkg(annual),
            StackComponent(
                components = emptyList(),
                badge = badge(pkg(monthly)),
                overrides = listOf(ComponentOverride(
                    conditions = listOf(wideCondition),
                    properties = PartialStackComponent(badge = badge()),
                )),
            ),
        )

        state.reconcileSelectionForWindowSize(wide)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
    }

    @Test
    fun `a control used only by an inactive tab cannot keep a package selected`() {
        val state = state(
            hiddenStack(pkg(monthly, default = true)),
            pkg(annual),
            TabsComponent(
                tabs = listOf(
                    TabsComponent.Tab(id = "first", stack = StackComponent(listOf(TabControlComponent))),
                    TabsComponent.Tab(id = "second", stack = StackComponent(emptyList())),
                ),
                control = TabsComponent.TabControl.Buttons(StackComponent(listOf(pkg(monthly)))),
            ),
        )
        state.paywallBoundsDp = wide

        state.update(selectedTabIndex = 1)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
    }

    @Test
    fun `open sheets do not inherit the opening button's hidden containers`() {
        val state = state(
            hiddenStack(pkg(monthly, default = true)),
            pkg(annual),
            hiddenStack(
                ButtonComponent(
                    action = ButtonComponent.Action.NavigateTo(
                        ButtonComponent.Destination.Sheet(
                            id = "plans",
                            stack = StackComponent(listOf(pkg(monthly))),
                        ),
                    ),
                    stack = StackComponent(emptyList()),
                ),
            ),
        )
        val buttonContainer = (state.stack as StackComponentStyle).children[2] as StackComponentStyle
        val button = buttonContainer.children.single() as ButtonComponentStyle
        val sheet = (button.action as ButtonComponentStyle.Action.NavigateTo).destination
            as ButtonComponentStyle.Action.NavigateTo.Destination.Sheet
        state.paywallBoundsDp = wide

        state.sheet.show(sheet, state, PaywallComponentInteractionTracker { }, onClick = {})

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
        state.sheet.hide()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
    }

    private fun pkg(pkg: Package, default: Boolean = false) = PackageComponent(
        packageId = pkg.identifier,
        isSelectedByDefault = default,
        stack = StackComponent(emptyList()),
    )

    private fun hiddenStack(vararg children: PaywallComponent) = StackComponent(
        components = children.toList(),
        overrides = listOf(hideStackOnWide),
    )

    private fun state(vararg components: PaywallComponent) = FakePaywallState(
        components = components.toList(),
        packages = listOf(monthly, annual, weekly),
        addPackageComponents = false,
    )
}
