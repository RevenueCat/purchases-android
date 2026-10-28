package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PartialPackageComponent
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.PaywallComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TabsComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.ui.revenuecatui.components.show
import com.revenuecat.purchases.ui.revenuecatui.components.style.ButtonComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.TabsComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

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
    fun `selected-only default content is selected initially and after reset`() {
        val selectedOnly = StackComponent(
            components = emptyList(),
            visible = false,
            overrides = listOf(ComponentOverride(
                conditions = listOf(ComponentOverride.Condition.Selected),
                properties = PartialStackComponent(visible = true),
            )),
        )
        val state = state(pkg(monthly, default = true, content = selectedOnly), pkg(annual))
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
        state.paywallBoundsDp = wide
        state.update(selectedPackageUniqueId = annual.identifier)

        state.resetToDefaultPackage()

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
    }

    @Test
    fun `measurement selects a newly revealed default`() {
        val state = state(PackageComponent(
            packageId = monthly.identifier,
            isSelectedByDefault = true,
            visible = false,
            stack = StackComponent(emptyList()),
            overrides = listOf(ComponentOverride(
                conditions = listOf(wideCondition),
                properties = PartialPackageComponent(visible = true),
            )),
        ))
        assertThat(state.selectedPackageInfo).isNull()

        state.reconcileSelectionForWindowSize(wide)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
    }

    @Test
    fun `a hidden default cannot borrow default status from a visible copy`() {
        val state = state(
            StackComponent(components = listOf(pkg(monthly, default = true)), visible = false),
            pkg(monthly),
            tabs(listOf(pkg(annual, default = true))),
        )

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
    }

    @Test
    fun `replacement skips hidden containers and hidden package content`() {
        val state = state(
            hiddenStack(pkg(monthly, default = true)),
            pkg(annual, content = hiddenStack()),
            pkg(weekly),
        )

        state.reconcileSelectionForWindowSize(wide)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(weekly)
    }

    @Test
    fun `identical cards in different tabs retain their own visibility paths`() {
        val state = state(tabs(
            listOf(pkg(monthly, default = true)),
            listOf(hiddenStack(pkg(monthly, default = true)), pkg(annual)),
        ))
        state.paywallBoundsDp = wide

        state.update(selectedTabIndex = 1)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
    }

    @Test
    fun `dismissing a tab sheet restores a root fallback visible at current bounds`() {
        val sheetButton = ButtonComponent(
            action = ButtonComponent.Action.NavigateTo(
                ButtonComponent.Destination.Sheet(
                    id = "plans",
                    stack = StackComponent(listOf(pkg(annual, default = true))),
                ),
            ),
            stack = StackComponent(emptyList()),
        )
        val state = state(hiddenStack(pkg(monthly)), pkg(weekly), tabs(listOf(sheetButton)))
        val tabStyle = (state.stack as StackComponentStyle).children[2] as TabsComponentStyle
        val button = tabStyle.tabs[0].stack.children[0] as ButtonComponentStyle
        val sheet = (button.action as ButtonComponentStyle.Action.NavigateTo).destination
            as ButtonComponentStyle.Action.NavigateTo.Destination.Sheet
        state.paywallBoundsDp = wide
        state.sheet.show(sheet, state, PaywallComponentInteractionTracker { }, onClick = {})
        state.update(selectedPackageUniqueId = annual.identifier)

        state.sheet.hide()

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(weekly)
    }

    private fun tabs(vararg contents: List<PaywallComponent>) = TabsComponent(
        tabs = contents.mapIndexed { index, children ->
            TabsComponent.Tab(id = index.toString(), stack = StackComponent(children))
        },
        control = TabsComponent.TabControl.Buttons(StackComponent(emptyList())),
    )

    private fun pkg(
        pkg: Package,
        default: Boolean = false,
        content: StackComponent = StackComponent(emptyList()),
    ) = PackageComponent(
        packageId = pkg.identifier,
        isSelectedByDefault = default,
        stack = content,
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
