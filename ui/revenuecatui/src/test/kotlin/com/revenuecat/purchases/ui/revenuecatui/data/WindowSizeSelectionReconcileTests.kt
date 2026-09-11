package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PartialPackageComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(InternalRevenueCatAPI::class)
internal class WindowSizeSelectionReconcileTests {

    private val wideWindow = DpSize(800.dp, 600.dp)
    private val narrowWindow = DpSize(400.dp, 800.dp)

    private fun packageComponent(
        packageId: String,
        isSelectedByDefault: Boolean = false,
        overrides: List<ComponentOverride<PartialPackageComponent>> = emptyList(),
    ) = PackageComponent(
        packageId = packageId,
        isSelectedByDefault = isSelectedByDefault,
        stack = StackComponent(components = emptyList()),
        overrides = overrides,
    )

    private fun hiddenWhenWiderThan(width: Double) = ComponentOverride(
        conditions = listOf(
            ComponentOverride.Condition.WindowWidthRule(
                operator = ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL,
                value = width,
            ),
        ),
        properties = PartialPackageComponent(visible = false),
    )

    private fun stateWithDefaultHiddenOnWideWindows(): PaywallState.Loaded.Components =
        FakePaywallState(
            components = listOf(
                packageComponent(
                    packageId = TestData.Packages.monthly.identifier,
                    isSelectedByDefault = true,
                    overrides = listOf(hiddenWhenWiderThan(width = 700.0)),
                ),
                packageComponent(packageId = TestData.Packages.annual.identifier),
            ),
            packages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
        )

    @Test
    fun `moves selection off a package hidden at the measured window size`() {
        val state = stateWithDefaultHiddenOnWideWindows()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.monthly)

        state.reconcileSelectionForWindowSize(wideWindow)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.annual)
    }

    @Test
    fun `keeps a selection that is visible at the measured window size`() {
        val state = stateWithDefaultHiddenOnWideWindows()

        state.reconcileSelectionForWindowSize(narrowWindow)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.monthly)
    }

    @Test
    fun `does nothing while the window size is unknown`() {
        val state = stateWithDefaultHiddenOnWideWindows()

        state.reconcileSelectionForWindowSize(null)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.monthly)
    }

    // The nothing-visible case lives in PaywallStateLoadedComponentsPackageSelectionTests: this
    // file's FakePaywallState synthesizes an always-visible component per package, which would
    // make that test pass vacuously.

    @Test
    fun `keeps the replacement when resizing back makes the original visible again`() {
        val state = stateWithDefaultHiddenOnWideWindows()
        state.reconcileSelectionForWindowSize(wideWindow)
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.annual)

        state.reconcileSelectionForWindowSize(narrowWindow)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.annual)
    }

    @Test
    fun `evaluates height rules against the frame height`() {
        val state = FakePaywallState(
            components = listOf(
                packageComponent(
                    packageId = TestData.Packages.monthly.identifier,
                    isSelectedByDefault = true,
                    overrides = listOf(
                        ComponentOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.WindowHeightRule(
                                    operator = ComponentOverride.ComparisonOperator.LESS_THAN,
                                    value = 500.0,
                                ),
                            ),
                            properties = PartialPackageComponent(visible = false),
                        ),
                    ),
                ),
                packageComponent(packageId = TestData.Packages.annual.identifier),
            ),
            packages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
        )

        state.reconcileSelectionForWindowSize(DpSize(800.dp, 400.dp))

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.annual)
    }

    @Test
    fun `prefers a visible authored default over the first visible package`() {
        val state = FakePaywallState(
            components = listOf(
                packageComponent(
                    packageId = TestData.Packages.monthly.identifier,
                    isSelectedByDefault = true,
                    overrides = listOf(hiddenWhenWiderThan(width = 700.0)),
                ),
                packageComponent(packageId = TestData.Packages.weekly.identifier),
                packageComponent(
                    packageId = TestData.Packages.annual.identifier,
                    isSelectedByDefault = true,
                ),
            ),
            packages = listOf(TestData.Packages.monthly, TestData.Packages.weekly, TestData.Packages.annual),
        )

        state.reconcileSelectionForWindowSize(wideWindow)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.annual)
    }
}
