package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.PartialPackageComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.windowChangingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end wiring for reconcile-on-resize: [MeasurePaywallBounds] measures the content area
 * and moves the selection off a package that a window size rule hides at the new size.
 */
@OptIn(InternalRevenueCatAPI::class)
@RunWith(AndroidJUnit4::class)
internal class WindowSizeSelectionReconcileWindowTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `growing the window past a hide rule moves the selection`(): Unit = with(composeTestRule) {
        val state = FakePaywallState(
            components = listOf(
                PackageComponent(
                    packageId = TestData.Packages.monthly.identifier,
                    isSelectedByDefault = true,
                    stack = StackComponent(components = emptyList()),
                    overrides = listOf(
                        ComponentOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.WindowWidthRule(
                                    operator = ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL,
                                    value = 700.0,
                                ),
                            ),
                            properties = PartialPackageComponent(visible = false),
                        ),
                    ),
                ),
                PackageComponent(
                    packageId = TestData.Packages.annual.identifier,
                    isSelectedByDefault = false,
                    stack = StackComponent(components = emptyList()),
                ),
            ),
            packages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
        )

        windowChangingTest(
            arrange = { state },
            act = { paywallState ->
                MeasurePaywallBounds(paywallState) {
                    Box(Modifier.fillMaxSize())
                }
            },
            assert = { controller ->
                controller.setWindowSizeInexact(width = 400.dp, height = 800.dp)
                waitForIdle()
                assertThat(state.paywallBoundsDp).isNotNull()
                assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.monthly)

                controller.setWindowSizeInexact(width = 900.dp, height = 600.dp)
                waitForIdle()
                assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.annual)
            },
        )
    }

    @Test
    fun `a state added after bounds settle is reconciled too`(): Unit = with(composeTestRule) {
        fun hiddenOnWideState() = FakePaywallState(
            components = listOf(
                PackageComponent(
                    packageId = TestData.Packages.monthly.identifier,
                    isSelectedByDefault = true,
                    stack = StackComponent(components = emptyList()),
                    overrides = listOf(
                        ComponentOverride(
                            conditions = listOf(
                                ComponentOverride.Condition.WindowWidthRule(
                                    operator = ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL,
                                    value = 700.0,
                                ),
                            ),
                            properties = PartialPackageComponent(visible = false),
                        ),
                    ),
                ),
                PackageComponent(
                    packageId = TestData.Packages.annual.identifier,
                    isSelectedByDefault = false,
                    stack = StackComponent(components = emptyList()),
                ),
            ),
            packages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
        )

        val firstState = hiddenOnWideState()
        val lateState = hiddenOnWideState()
        val states = mutableStateOf(listOf(firstState))

        windowChangingTest(
            arrange = { states },
            act = { statesState ->
                MeasurePaywallBounds(statesState.value) {
                    Box(Modifier.fillMaxSize())
                }
            },
            assert = { controller ->
                controller.setWindowSizeInexact(width = 900.dp, height = 600.dp)
                waitForIdle()
                assertThat(firstState.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.annual)
                // Selection was resolved before any bounds were known.
                assertThat(lateState.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.monthly)

                // A prewarmed state joining while the bounds stay constant must be reconciled too.
                states.value = listOf(firstState, lateState)
                waitForIdle()
                assertThat(lateState.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.annual)
            },
        )
    }
}
