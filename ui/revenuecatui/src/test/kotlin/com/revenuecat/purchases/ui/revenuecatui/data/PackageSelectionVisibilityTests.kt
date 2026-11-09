package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PartialPackageComponent
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.PartialStickyFooterComponent
import com.revenuecat.purchases.paywalls.components.PaywallComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(InternalRevenueCatAPI::class)
@RunWith(RobolectricTestRunner::class)
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
    fun `hidden footer default does not override a visible body package`() {
        val state = FakePaywallState(
            components = listOf(pkg(monthly)),
            packages = listOf(monthly, annual),
            addPackageComponents = false,
            stickyFooter = StickyFooterComponent(
                stack = StackComponent(listOf(pkg(annual, default = true))),
                layout = StickyFooterComponent.Layout.HIDDEN,
            ),
        )

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
        assertThat(state.defaultPackageForPackageRowAnalytics()).isEqualTo(monthly)
    }

    @Test
    fun `hidden footer default is selected when every package is hidden`() {
        val state = FakePaywallState(
            packages = listOf(monthly, annual),
            addPackageComponents = false,
            stickyFooter = StickyFooterComponent(
                stack = StackComponent(listOf(pkg(monthly), pkg(annual, default = true))),
                layout = StickyFooterComponent.Layout.HIDDEN,
            ),
        )

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
        assertThat(state.defaultPackageForPackageRowAnalytics()).isEqualTo(annual)
    }

    @Test
    fun `visible footer default takes precedence over a visible body package`() {
        val state = FakePaywallState(
            components = listOf(pkg(monthly)),
            packages = listOf(monthly, annual),
            addPackageComponents = false,
            stickyFooter = StickyFooterComponent(StackComponent(listOf(pkg(annual, default = true)))),
        )

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
    }

    @Test
    fun `hiding a footer on resize replaces its default with a visible body package`() {
        val state = FakePaywallState(
            components = listOf(pkg(monthly)),
            packages = listOf(monthly, annual),
            addPackageComponents = false,
            stickyFooter = StickyFooterComponent(
                stack = StackComponent(listOf(pkg(annual, default = true))),
                overrides = listOf(ComponentOverride(
                    conditions = listOf(wideCondition),
                    properties = PartialStickyFooterComponent(layout = StickyFooterComponent.Layout.HIDDEN),
                )),
            ),
        )
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)

        state.reconcileSelectionForWindowSize(wide)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
    }

    @Test
    fun `hidden tab default does not override a visible root package`() {
        val state = state(pkg(monthly), tabs(listOf(StackComponent(
            components = listOf(pkg(annual, default = true)),
            visible = false,
        ))))

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
    }

    @Test
    fun `all hidden sheet selects its authored default ahead of the visible root package`() {
        val state = state(
            pkg(weekly, default = true),
            ButtonComponent(
                action = ButtonComponent.Action.NavigateTo(ButtonComponent.Destination.Sheet(
                    id = "plans",
                    stack = StackComponent(listOf(pkg(monthly), pkg(annual, default = true)), visible = false),
                )),
                stack = StackComponent(emptyList()),
            ),
        )
        val button = (state.stack as StackComponentStyle).children[1] as ButtonComponentStyle
        val sheet = (button.action as ButtonComponentStyle.Action.NavigateTo).destination
            as ButtonComponentStyle.Action.NavigateTo.Destination.Sheet

        state.sheet.show(sheet, state, PaywallComponentInteractionTracker { }, onClick = {})

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
        state.sheet.hide()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(weekly)
    }

    @Test
    fun `initial selection skips hidden containers when no default is authored`() {
        val state = state(
            StackComponent(components = listOf(pkg(monthly)), visible = false),
            pkg(annual),
            pkg(weekly),
        )

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
    }

    @Test
    fun `initial selection ignores packages in a closed sheet when no default is authored`() {
        val state = state(
            ButtonComponent(
                action = ButtonComponent.Action.NavigateTo(
                    ButtonComponent.Destination.Sheet(
                        id = "plans",
                        stack = StackComponent(listOf(pkg(monthly))),
                    ),
                ),
                stack = StackComponent(emptyList()),
            ),
            pkg(annual),
        )

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
    }

    @Test
    fun `selected-only default content is selected initially and remains the analytics default`() {
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

        assertThat(state.defaultPackageForPackageRowAnalytics()).isEqualTo(monthly)
    }

    @Test
    fun `hidden default remains selected when measurement reveals it`() {
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
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)

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
        state.reconcileSelectionForWindowSize(wide)
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(weekly)
        state.sheet.show(sheet, state, PaywallComponentInteractionTracker { }, onClick = {})
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)

        state.sheet.hide()

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(weekly)
    }

    @Test
    fun `hidden fallback honors ancestor visibility and prefers any visible selectable package`() {
        val allHidden = state(StackComponent(listOf(pkg(monthly), pkg(annual)), visible = false))
        assertThat(allHidden.selectedPackageInfo?.rcPackage).isEqualTo(monthly)

        val visibleAlternative = state(
            StackComponent(listOf(pkg(monthly), pkg(annual)), visible = false),
            pkg(weekly),
        )
        assertThat(visibleAlternative.selectedPackageInfo?.rcPackage).isEqualTo(weekly)
    }

    @Test
    fun `a visible self hiding card is not an automatic all hidden fallback`() {
        val selfHiding = pkg(monthly, content = StackComponent(
            components = emptyList(),
            overrides = listOf(ComponentOverride(
                conditions = listOf(ComponentOverride.Condition.Selected),
                properties = PartialStackComponent(visible = false),
            )),
        ))
        val state = state(selfHiding)
        assertThat(state.selectedPackageInfo).isNull()

        // Explicit selection remains possible even though automatic replacement cannot choose it.
        state.update(selectedPackageUniqueId = monthly.identifier)
        state.reconcileSelectionForWindowSize(wide)
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
    }

    @Test
    fun `all hidden fallback does not invent an unavailable package`() {
        val state = state(PackageComponent(
            packageId = "not-in-offering",
            isSelectedByDefault = false,
            stack = StackComponent(emptyList()),
            visible = false,
        ))

        assertThat(state.selectedPackageInfo).isNull()
    }

    @Test
    fun `resize during sheet presentation does not restore a hidden root package`() {
        val state = state(
            hiddenStack(pkg(monthly, default = true)),
            pkg(weekly),
            ButtonComponent(
                action = ButtonComponent.Action.NavigateTo(ButtonComponent.Destination.Sheet(
                    id = "plans", stack = StackComponent(listOf(pkg(annual, default = true))),
                )),
                stack = StackComponent(emptyList()),
            ),
        )
        val button = (state.stack as StackComponentStyle).children[2] as ButtonComponentStyle
        val sheet = (button.action as ButtonComponentStyle.Action.NavigateTo).destination
            as ButtonComponentStyle.Action.NavigateTo.Destination.Sheet
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
        state.sheet.show(sheet, state, PaywallComponentInteractionTracker { }, onClick = {})
        state.paywallBoundsDp = wide
        state.reconcileSelectionForWindowSize(wide)
        assertThat(state.peekSelectedPackageInfoAfterSheetDismiss()?.rcPackage).isEqualTo(weekly)
        state.sheet.hide()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(weekly)
    }

    @Test
    fun `replacing a package sheet with an empty sheet restores the original root selection`() {
        fun button(id: String, children: List<PaywallComponent>) = ButtonComponent(
            action = ButtonComponent.Action.NavigateTo(ButtonComponent.Destination.Sheet(
                id = id, stack = StackComponent(children),
            )),
            stack = StackComponent(emptyList()),
        )
        val state = state(
            pkg(monthly, default = true),
            pkg(weekly),
            button("plans", listOf(pkg(annual, default = true))),
            button("terms", emptyList()),
        )
        fun sheet(index: Int) = (((state.stack as StackComponentStyle).children[index] as ButtonComponentStyle)
            .action as ButtonComponentStyle.Action.NavigateTo).destination
            as ButtonComponentStyle.Action.NavigateTo.Destination.Sheet
        state.paywallBoundsDp = wide
        state.update(selectedPackageUniqueId = weekly.identifier)
        state.sheet.show(sheet(2), state, PaywallComponentInteractionTracker { }, onClick = {})
        state.update(selectedPackageUniqueId = annual.identifier)
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
        state.sheet.show(sheet(3), state, PaywallComponentInteractionTracker { }, onClick = {})
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(weekly)
        state.sheet.hide()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(weekly)
    }

    @Test
    fun `terms sheet preserves explicit self hiding choice`() {
        val state = state(
            pkg(annual, default = true),
            pkg(monthly, content = StackComponent(
                components = emptyList(),
                overrides = listOf(ComponentOverride(
                    conditions = listOf(ComponentOverride.Condition.Selected),
                    properties = PartialStackComponent(visible = false),
                )),
            )),
            ButtonComponent(
                action = ButtonComponent.Action.NavigateTo(ButtonComponent.Destination.Sheet(
                    id = "terms", stack = StackComponent(emptyList()),
                )),
                stack = StackComponent(emptyList()),
            ),
        )
        state.paywallBoundsDp = wide
        state.update(selectedPackageUniqueId = monthly.identifier)
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
        val button = (state.stack as StackComponentStyle).children[2] as ButtonComponentStyle
        val sheet = (button.action as ButtonComponentStyle.Action.NavigateTo).destination
            as ButtonComponentStyle.Action.NavigateTo.Destination.Sheet
        state.sheet.show(sheet, state, PaywallComponentInteractionTracker { }, onClick = {})
        state.sheet.hide()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
    }

    @Test
    fun `remeasurement preserves a self hiding choice until its container is hidden`() {
        val state = state(
            pkg(annual, default = true),
            hiddenStack(pkg(monthly, content = StackComponent(
                components = emptyList(),
                overrides = listOf(ComponentOverride(
                    conditions = listOf(ComponentOverride.Condition.Selected),
                    properties = PartialStackComponent(visible = false),
                )),
            ))),
        )
        val narrow = DpSize(400.dp, 600.dp)
        state.paywallBoundsDp = narrow
        state.update(selectedPackageUniqueId = monthly.identifier)
        state.reconcileSelectionForWindowSize(narrow)
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)

        state.paywallBoundsDp = wide
        state.reconcileSelectionForWindowSize(wide)
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
    }

    @Test
    fun `returning to a tab preserves an explicit self hiding choice until its container is hidden`() {
        val state = state(tabs(
            listOf(pkg(annual, default = true), hiddenStack(selfHidingPackage(monthly))),
            listOf(pkg(weekly, default = true)),
        ))
        state.paywallBoundsDp = DpSize(400.dp, 600.dp)
        state.update(selectedPackageUniqueId = monthly.identifier)

        state.update(selectedTabIndex = 1)
        state.update(selectedTabIndex = 0)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)

        state.update(selectedTabIndex = 1)
        state.paywallBoundsDp = wide
        state.update(selectedTabIndex = 0)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
    }

    @Test
    fun `switching to a content only tab preserves an explicit self hiding root choice`() {
        val state = state(
            pkg(annual, default = true),
            selfHidingPackage(monthly),
            tabs(emptyList(), emptyList()),
        )
        state.update(selectedPackageUniqueId = monthly.identifier)

        state.update(selectedTabIndex = 1)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)
    }

    @Test
    fun `sheet resize selects a visible sheet package ahead of root and tab defaults`() {
        val state = state(
            pkg(annual, default = true),
            tabs(listOf(pkg(annual, default = true))),
            ButtonComponent(
                action = ButtonComponent.Action.NavigateTo(ButtonComponent.Destination.Sheet(
                    id = "plans",
                    stack = StackComponent(listOf(hiddenStack(pkg(monthly)), pkg(weekly))),
                )),
                stack = StackComponent(emptyList()),
            ),
        )
        state.paywallBoundsDp = DpSize(400.dp, 600.dp)
        val button = (state.stack as StackComponentStyle).children[2] as ButtonComponentStyle
        val sheet = (button.action as ButtonComponentStyle.Action.NavigateTo).destination
            as ButtonComponentStyle.Action.NavigateTo.Destination.Sheet
        state.sheet.show(sheet, state, PaywallComponentInteractionTracker { }, onClick = {})
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(monthly)

        state.paywallBoundsDp = wide
        state.reconcileSelectionForWindowSize(wide)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(weekly)
        state.sheet.hide()
        state.restorePackageAfterSheetDismiss()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(annual)
    }

    private fun selfHidingPackage(pkg: Package) = pkg(pkg, content = StackComponent(
        components = emptyList(),
        overrides = listOf(ComponentOverride(
            conditions = listOf(ComponentOverride.Condition.Selected),
            properties = PartialStackComponent(visible = false),
        )),
    ))

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
