package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.PartialPackageComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverride
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedPackagePartial
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.previewStackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptySetOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlinx.serialization.json.JsonPrimitive
import java.util.Date

/**
 * Tests for [PaywallState.Loaded.Components] package selection behavior.
 *
 * These tests verify the fix for MON-1823: paywall view not showing prices in variables correctly
 * on initial load when packages are only in tabs and no initialSelectedTabIndex is provided.
 */
@RunWith(RobolectricTestRunner::class)
internal class PaywallStateLoadedComponentsPackageSelectionTests {

    private val localeId = LocaleId("en_US")

    @Test
    fun `Should select default package from tab 0 when initialSelectedTabIndex is null`() {
        // Arrange: packages only in tabs, no initialSelectedTabIndex
        val defaultPackage = TestData.Packages.monthly
        val state = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = mapOf(
                0 to listOf(
                    packageInfo(defaultPackage, isSelectedByDefault = true),
                    packageInfo(TestData.Packages.annual, isSelectedByDefault = false),
                ),
            ),
            initialSelectedTabIndex = null, // This is the key - null initialSelectedTabIndex
        )

        // Act & Assert
        assertThat(state.selectedPackageInfo).isNotNull
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(defaultPackage)
    }

    @Test
    fun `Should select first package from tab 0 when no package is marked as default`() {
        // Arrange: no package marked as default
        val firstPackage = TestData.Packages.monthly
        val state = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = mapOf(
                0 to listOf(
                    packageInfo(firstPackage, isSelectedByDefault = false),
                    packageInfo(TestData.Packages.annual, isSelectedByDefault = false),
                ),
            ),
            initialSelectedTabIndex = null,
        )

        // Act & Assert
        assertThat(state.selectedPackageInfo).isNotNull
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(firstPackage)
    }

    @Test
    fun `Should select package outside tabs when available and marked as default`() {
        // Arrange: package outside tabs is marked as default
        val defaultPackageOutsideTabs = TestData.Packages.weekly
        val state = paywallState(
            packagesOutsideTabs = listOf(
                packageInfo(defaultPackageOutsideTabs, isSelectedByDefault = true),
            ),
            packagesByTab = mapOf(
                0 to listOf(
                    packageInfo(TestData.Packages.monthly, isSelectedByDefault = true),
                ),
            ),
            initialSelectedTabIndex = null,
        )

        // Act & Assert
        assertThat(state.selectedPackageInfo).isNotNull
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(defaultPackageOutsideTabs)
    }

    @Test
    fun `Should select package from specified tab when initialSelectedTabIndex is provided`() {
        // Arrange: initialSelectedTabIndex points to tab 1
        val defaultPackageOnTab1 = TestData.Packages.annual
        val state = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = mapOf(
                0 to listOf(
                    packageInfo(TestData.Packages.monthly, isSelectedByDefault = true),
                ),
                1 to listOf(
                    packageInfo(defaultPackageOnTab1, isSelectedByDefault = true),
                ),
            ),
            initialSelectedTabIndex = 1,
        )

        // Act & Assert
        assertThat(state.selectedPackageInfo).isNotNull
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(defaultPackageOnTab1)
    }

    @Test
    fun `Should have null selectedPackageInfo when no packages are available`() {
        // Arrange: no packages at all
        val state = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = emptyMap(),
            initialSelectedTabIndex = null,
        )

        // Act & Assert
        assertThat(state.selectedPackageInfo).isNull()
    }

    @Test
    fun `Should select package from tab 0 even when tab 1 has default package and initialSelectedTabIndex is null`() {
        // Arrange: tab 1 has a default package, but initialSelectedTabIndex is null (defaults to tab 0)
        val defaultPackageOnTab0 = TestData.Packages.monthly
        val state = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = mapOf(
                0 to listOf(
                    packageInfo(defaultPackageOnTab0, isSelectedByDefault = true),
                ),
                1 to listOf(
                    packageInfo(TestData.Packages.annual, isSelectedByDefault = true),
                ),
            ),
            initialSelectedTabIndex = null,
        )

        // Act & Assert
        assertThat(state.selectedPackageInfo).isNotNull
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(defaultPackageOnTab0)
        assertThat(state.selectedTabIndex).isEqualTo(0)
    }

    // region Visibility-aware default selection

    @Test
    fun `Should skip a default package hidden by a variable rule`() {
        val state = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = mapOf(
                0 to listOf(
                    packageInfo(
                        TestData.Packages.annual,
                        isSelectedByDefault = true,
                        visibilityOverrides = listOf(canTrialOverride(false, visible = false)),
                    ),
                    packageInfo(TestData.Packages.monthly, isSelectedByDefault = false),
                ),
            ),
            initialSelectedTabIndex = null,
            customVariables = mapOf("can_trial" to CustomVariableValue.Boolean(false)),
        )

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.monthly)
    }

    @Test
    fun `Should keep the default package when its variable rule does not match`() {
        val state = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = mapOf(
                0 to listOf(
                    packageInfo(
                        TestData.Packages.annual,
                        isSelectedByDefault = true,
                        visibilityOverrides = listOf(canTrialOverride(false, visible = false)),
                    ),
                    packageInfo(TestData.Packages.monthly, isSelectedByDefault = false),
                ),
            ),
            initialSelectedTabIndex = null,
            customVariables = mapOf("can_trial" to CustomVariableValue.Boolean(true)),
        )

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.annual)
    }

    @Test
    fun `Should fall back to the first visible package in document order`() {
        val state = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = mapOf(
                0 to listOf(
                    packageInfo(
                        TestData.Packages.annual,
                        isSelectedByDefault = true,
                        visibilityOverrides = listOf(canTrialOverride(false, visible = false)),
                    ),
                    // Only visible because a rule turns it on, and it precedes monthly.
                    packageInfo(
                        TestData.Packages.weekly,
                        isSelectedByDefault = false,
                        visible = false,
                        visibilityOverrides = listOf(canTrialOverride(false, visible = true)),
                    ),
                    packageInfo(TestData.Packages.monthly, isSelectedByDefault = false),
                ),
            ),
            initialSelectedTabIndex = null,
            customVariables = mapOf("can_trial" to CustomVariableValue.Boolean(false)),
        )

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.weekly)
    }

    @Test
    fun `Should skip a statically hidden default package`() {
        val state = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = mapOf(
                0 to listOf(
                    packageInfo(TestData.Packages.annual, isSelectedByDefault = true, visible = false),
                    packageInfo(TestData.Packages.monthly, isSelectedByDefault = false),
                ),
            ),
            initialSelectedTabIndex = null,
        )

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.monthly)
    }

    @Test
    fun `Should select nothing when every package is hidden`() {
        val state = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = mapOf(
                0 to listOf(
                    packageInfo(TestData.Packages.annual, isSelectedByDefault = true, visible = false),
                    packageInfo(TestData.Packages.monthly, isSelectedByDefault = false, visible = false),
                ),
            ),
            initialSelectedTabIndex = null,
        )

        assertThat(state.selectedPackageInfo).isNull()
    }

    @Test
    fun `Should fall back to a visible package outside tabs when the default is hidden`() {
        val state = paywallState(
            packagesOutsideTabs = listOf(
                packageInfo(
                    TestData.Packages.annual,
                    isSelectedByDefault = true,
                    visibilityOverrides = listOf(canTrialOverride(false, visible = false)),
                ),
                packageInfo(TestData.Packages.monthly, isSelectedByDefault = false),
            ),
            packagesByTab = emptyMap(),
            initialSelectedTabIndex = null,
            customVariables = mapOf("can_trial" to CustomVariableValue.Boolean(false)),
        )

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.monthly)
    }

    @Test
    fun `Should restore the visible fallback after a sheet dismiss`() {
        val state = paywallState(
            packagesOutsideTabs = listOf(
                packageInfo(
                    TestData.Packages.annual,
                    isSelectedByDefault = true,
                    visibilityOverrides = listOf(canTrialOverride(false, visible = false)),
                ),
                packageInfo(TestData.Packages.monthly, isSelectedByDefault = false),
            ),
            packagesByTab = emptyMap(),
            initialSelectedTabIndex = null,
            customVariables = mapOf("can_trial" to CustomVariableValue.Boolean(false)),
        )

        state.resetToDefaultPackage()

        assertThat(state.peekDefaultPackageUniqueIdAfterSheetDismiss()).isNotNull()
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.monthly)
    }

    @Test
    fun `Should prefer an outside-tabs default over a tab package that is not a default`() {
        val state = paywallState(
            packagesOutsideTabs = listOf(packageInfo(TestData.Packages.annual, isSelectedByDefault = true)),
            packagesByTab = mapOf(
                0 to listOf(packageInfo(TestData.Packages.monthly, isSelectedByDefault = false)),
            ),
            initialSelectedTabIndex = 0,
        )

        state.resetToDefaultPackage()

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.annual)
    }

    @Test
    fun `Should fall back outside tabs when switching to a tab with nothing visible`() {
        val state = paywallState(
            packagesOutsideTabs = listOf(
                packageInfo(
                    TestData.Packages.annual,
                    isSelectedByDefault = true,
                    visibilityOverrides = listOf(canTrialOverride(false, visible = false)),
                ),
                packageInfo(TestData.Packages.monthly, isSelectedByDefault = false),
            ),
            packagesByTab = mapOf(
                0 to listOf(packageInfo(TestData.Packages.weekly, isSelectedByDefault = true)),
                1 to listOf(
                    packageInfo(
                        TestData.Packages.lifetime,
                        isSelectedByDefault = false,
                        visibilityOverrides = listOf(canTrialOverride(false, visible = false)),
                    ),
                ),
            ),
            initialSelectedTabIndex = 0,
            customVariables = mapOf("can_trial" to CustomVariableValue.Boolean(false)),
        )

        state.update(selectedTabIndex = 1)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.monthly)
    }

    @Test
    fun `Should select nothing outside tabs when no package is selected by default`() {
        val state = paywallState(
            packagesOutsideTabs = listOf(
                packageInfo(TestData.Packages.annual, isSelectedByDefault = false),
                packageInfo(TestData.Packages.monthly, isSelectedByDefault = false),
            ),
            packagesByTab = emptyMap(),
            initialSelectedTabIndex = null,
        )

        assertThat(state.selectedPackageInfo).isNull()
    }

    @Test
    fun `Should keep a default inside a tab ahead of a package outside tabs`() {
        val state = paywallState(
            packagesOutsideTabs = listOf(packageInfo(TestData.Packages.monthly, isSelectedByDefault = false)),
            packagesByTab = mapOf(
                0 to listOf(packageInfo(TestData.Packages.annual, isSelectedByDefault = true)),
            ),
            initialSelectedTabIndex = 0,
        )

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.annual)
    }

    @Test
    fun `paywallPackages lists packages outside tabs first, then by tab, without duplicates`() {
        val state = paywallState(
            packagesOutsideTabs = listOf(packageInfo(TestData.Packages.annual, isSelectedByDefault = true)),
            packagesByTab = mapOf(
                1 to listOf(packageInfo(TestData.Packages.weekly, isSelectedByDefault = false)),
                0 to listOf(
                    packageInfo(TestData.Packages.monthly, isSelectedByDefault = false),
                    packageInfo(TestData.Packages.annual, isSelectedByDefault = false),
                ),
            ),
            initialSelectedTabIndex = 0,
        )

        assertThat(state.paywallPackages).containsExactly(
            TestData.Packages.annual,
            TestData.Packages.monthly,
            TestData.Packages.weekly,
        )
    }

    @Test
    fun `Should not select a hidden package when switching to a tab with nothing visible`() {
        val state = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = mapOf(
                0 to listOf(packageInfo(TestData.Packages.weekly, isSelectedByDefault = true)),
                1 to listOf(
                    packageInfo(
                        TestData.Packages.annual,
                        isSelectedByDefault = false,
                        visibilityOverrides = listOf(canTrialOverride(false, visible = false)),
                    ),
                ),
            ),
            initialSelectedTabIndex = 0,
            customVariables = mapOf("can_trial" to CustomVariableValue.Boolean(false)),
        )

        state.update(selectedTabIndex = 1)

        assertThat(state.selectedPackageInfo).isNull()
    }

    // endregion

    // region window size rules

    @Test
    fun `Switching tabs reconciles a remembered selection hidden by a window size rule`() {
        val state = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = mapOf(
                0 to listOf(packageInfo(TestData.Packages.weekly, isSelectedByDefault = true)),
                1 to listOf(
                    packageInfo(
                        TestData.Packages.monthly,
                        isSelectedByDefault = true,
                        visibilityOverrides = listOf(hiddenWhenWiderThanOverride(width = 700.0)),
                    ),
                    packageInfo(TestData.Packages.annual, isSelectedByDefault = false),
                ),
            ),
            initialSelectedTabIndex = 0,
        )
        state.paywallBoundsDp = DpSize(800.dp, 600.dp)

        state.update(selectedTabIndex = 1)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.annual)
    }

    @Test
    fun `Resetting to the default package reconciles a default hidden by a window size rule`() {
        val hiddenDefault = packageInfo(
            TestData.Packages.monthly,
            isSelectedByDefault = true,
            visibilityOverrides = listOf(hiddenWhenWiderThanOverride(width = 700.0)),
        )
        val visiblePackage = packageInfo(TestData.Packages.annual, isSelectedByDefault = false)
        val state = paywallState(
            packagesOutsideTabs = listOf(hiddenDefault, visiblePackage),
            packagesByTab = emptyMap(),
            initialSelectedTabIndex = null,
        )
        state.paywallBoundsDp = DpSize(800.dp, 600.dp)
        state.update(selectedPackageUniqueId = visiblePackage.uniqueId)

        state.resetToDefaultPackage()

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.annual)
    }

    @Test
    fun `Reconciles a user-selected package hidden by a window size rule`() {
        val hideable = packageInfo(
            TestData.Packages.monthly,
            isSelectedByDefault = false,
            visibilityOverrides = listOf(hiddenWhenWiderThanOverride(width = 700.0)),
        )
        val visibleDefault = packageInfo(TestData.Packages.annual, isSelectedByDefault = true)
        val state = paywallState(
            packagesOutsideTabs = listOf(hideable, visibleDefault),
            packagesByTab = emptyMap(),
            initialSelectedTabIndex = null,
        )
        state.update(selectedPackageUniqueId = hideable.uniqueId)

        state.reconcileSelectionForWindowSize(DpSize(800.dp, 600.dp))

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.annual)
    }

    @Test
    fun `Reconcile keeps the selection when nothing is visible at the measured window size`() {
        val hiddenDefault = packageInfo(
            TestData.Packages.monthly,
            isSelectedByDefault = true,
            visibilityOverrides = listOf(hiddenWhenWiderThanOverride(width = 700.0)),
        )
        val hiddenOther = packageInfo(
            TestData.Packages.annual,
            isSelectedByDefault = false,
            visibilityOverrides = listOf(hiddenWhenWiderThanOverride(width = 700.0)),
        )
        val state = paywallState(
            packagesOutsideTabs = listOf(hiddenDefault, hiddenOther),
            packagesByTab = emptyMap(),
            initialSelectedTabIndex = null,
        )

        state.reconcileSelectionForWindowSize(DpSize(800.dp, 600.dp))

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.monthly)
    }

    @Test
    fun `Reconcile keeps a selected package that is visible only under the medium size class`() {
        val mediumOnly = packageInfo(
            TestData.Packages.monthly,
            isSelectedByDefault = false,
            visible = false,
            visibilityOverrides = listOf(visibleWhenOverride(ComponentOverride.Condition.Medium)),
        )
        val other = packageInfo(TestData.Packages.annual, isSelectedByDefault = true)
        val state = paywallState(
            packagesOutsideTabs = listOf(mediumOnly, other),
            packagesByTab = emptyMap(),
            initialSelectedTabIndex = null,
        )
        state.update(selectedPackageUniqueId = mediumOnly.uniqueId)
        state.windowScreenCondition = ScreenCondition.MEDIUM

        // The renderer shows this package on a medium window, so reconcile keeps it.
        state.reconcileSelectionForWindowSize(DpSize(700.dp, 600.dp))

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.monthly)
    }

    @Test
    fun `Reconcile keeps a selected package that is visible only while selected`() {
        val selectedOnly = packageInfo(
            TestData.Packages.monthly,
            isSelectedByDefault = false,
            visible = false,
            visibilityOverrides = listOf(visibleWhenOverride(ComponentOverride.Condition.Selected)),
        )
        val other = packageInfo(TestData.Packages.annual, isSelectedByDefault = true)
        val state = paywallState(
            packagesOutsideTabs = listOf(selectedOnly, other),
            packagesByTab = emptyMap(),
            initialSelectedTabIndex = null,
        )
        state.update(selectedPackageUniqueId = selectedOnly.uniqueId)

        state.reconcileSelectionForWindowSize(DpSize(400.dp, 800.dp))

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.monthly)
    }

    @Test
    fun `Reconcile does not move the selection onto a package hidden at the current size class`() {
        val hiddenCurrent = packageInfo(
            TestData.Packages.weekly,
            isSelectedByDefault = false,
            visibilityOverrides = listOf(hiddenWhenWiderThanOverride(width = 650.0)),
        )
        val mediumHiddenDefault = packageInfo(
            TestData.Packages.monthly,
            isSelectedByDefault = true,
            visibilityOverrides = listOf(hiddenWhenOverride(ComponentOverride.Condition.Medium)),
        )
        val visibleOther = packageInfo(TestData.Packages.annual, isSelectedByDefault = false)
        val state = paywallState(
            packagesOutsideTabs = listOf(hiddenCurrent, mediumHiddenDefault, visibleOther),
            packagesByTab = emptyMap(),
            initialSelectedTabIndex = null,
        )
        state.update(selectedPackageUniqueId = hiddenCurrent.uniqueId)
        state.windowScreenCondition = ScreenCondition.MEDIUM

        // The authored default is hidden on a medium window too, so it can't be the replacement.
        state.reconcileSelectionForWindowSize(DpSize(700.dp, 600.dp))

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.annual)
    }

    @Test
    fun `Reconcile evaluates the active tab's copy of the selected package`() {
        val visibleCopy = packageInfo(TestData.Packages.monthly, isSelectedByDefault = false)
        val hiddenCopy = packageInfo(
            TestData.Packages.monthly,
            isSelectedByDefault = false,
            visibilityOverrides = listOf(hiddenWhenWiderThanOverride(width = 700.0)),
        )
        val tabDefault = packageInfo(TestData.Packages.annual, isSelectedByDefault = true)
        val state = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = mapOf(0 to listOf(visibleCopy), 1 to listOf(hiddenCopy, tabDefault)),
            initialSelectedTabIndex = 1,
        )
        state.update(selectedPackageUniqueId = hiddenCopy.uniqueId)

        // The visible tab-0 copy must not mask the hidden copy on the active tab.
        state.reconcileSelectionForWindowSize(DpSize(800.dp, 600.dp))

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.annual)
    }

    @Test
    fun `Reconcile prefers the tab's authored default over one outside the tabs, matching sheet dismiss`() {
        val hiddenSelected = packageInfo(
            TestData.Packages.weekly,
            isSelectedByDefault = false,
            visibilityOverrides = listOf(hiddenWhenWiderThanOverride(width = 700.0)),
        )
        val outsideDefault = packageInfo(TestData.Packages.monthly, isSelectedByDefault = true)
        val tabDefault = packageInfo(TestData.Packages.annual, isSelectedByDefault = true)
        val state = paywallState(
            packagesOutsideTabs = listOf(hiddenSelected, outsideDefault),
            packagesByTab = mapOf(0 to listOf(tabDefault)),
            initialSelectedTabIndex = 0,
        )
        state.update(selectedPackageUniqueId = hiddenSelected.uniqueId)

        state.reconcileSelectionForWindowSize(DpSize(800.dp, 600.dp))

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.annual)
    }

    @Test
    fun `Reconcile keeps the selection while any on-screen copy is visible`() {
        val hiddenOutsideCopy = packageInfo(
            TestData.Packages.monthly,
            isSelectedByDefault = false,
            visibilityOverrides = listOf(hiddenWhenWiderThanOverride(width = 700.0)),
        )
        val visibleTabCopy = packageInfo(TestData.Packages.monthly, isSelectedByDefault = false)
        val tabDefault = packageInfo(TestData.Packages.annual, isSelectedByDefault = true)
        val state = paywallState(
            packagesOutsideTabs = listOf(hiddenOutsideCopy),
            packagesByTab = mapOf(0 to listOf(visibleTabCopy, tabDefault)),
            initialSelectedTabIndex = 0,
        )
        state.update(selectedPackageUniqueId = visibleTabCopy.uniqueId)

        // The hidden outside-tabs copy must not mask the visible copy on the active tab.
        state.reconcileSelectionForWindowSize(DpSize(800.dp, 600.dp))

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.monthly)
    }

    @Test
    fun `Peek after sheet dismiss skips a default hidden at the measured bounds`() {
        val hiddenDefault = packageInfo(
            TestData.Packages.monthly,
            isSelectedByDefault = true,
            visibilityOverrides = listOf(hiddenWhenWiderThanOverride(width = 700.0)),
        )
        val visibleOther = packageInfo(TestData.Packages.annual, isSelectedByDefault = false)
        val state = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = mapOf(0 to listOf(hiddenDefault, visibleOther)),
            initialSelectedTabIndex = 0,
        )
        state.paywallBoundsDp = DpSize(800.dp, 600.dp)

        assertThat(state.peekDefaultPackageUniqueIdAfterSheetDismiss()).isEqualTo(visibleOther.uniqueId)
    }

    private fun hiddenWhenWiderThanOverride(width: Double) = PresentedOverride(
        conditions = listOf(
            ComponentOverride.Condition.WindowWidthRule(
                operator = ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL,
                value = width,
            ),
        ),
        properties = PresentedPackagePartial(partial = PartialPackageComponent(visible = false)),
    )

    private fun visibleWhenOverride(condition: ComponentOverride.Condition) = PresentedOverride(
        conditions = listOf(condition),
        properties = PresentedPackagePartial(partial = PartialPackageComponent(visible = true)),
    )

    private fun hiddenWhenOverride(condition: ComponentOverride.Condition) = PresentedOverride(
        conditions = listOf(condition),
        properties = PresentedPackagePartial(partial = PartialPackageComponent(visible = false)),
    )

    // endregion

    private fun canTrialOverride(
        canTrial: Boolean,
        visible: Boolean,
    ) = PresentedOverride(
        conditions = listOf(
            ComponentOverride.Condition.Variable(
                operator = ComponentOverride.EqualityOperator.EQUALS,
                variable = "can_trial",
                value = JsonPrimitive(canTrial),
            ),
        ),
        properties = PresentedPackagePartial(partial = PartialPackageComponent(visible = visible)),
    )

    private fun packageInfo(
        pkg: com.revenuecat.purchases.Package,
        isSelectedByDefault: Boolean,
        visible: Boolean = true,
        visibilityOverrides: List<PresentedOverride<PresentedPackagePartial>> = emptyList(),
    ) = PaywallState.Loaded.Components.AvailablePackages.Info(
        pkg = pkg,
        isSelectedByDefault = isSelectedByDefault,
        visible = visible,
        visibilityOverrides = visibilityOverrides,
    )

    private fun paywallState(
        packagesOutsideTabs: List<PaywallState.Loaded.Components.AvailablePackages.Info>,
        packagesByTab: Map<Int, List<PaywallState.Loaded.Components.AvailablePackages.Info>>,
        initialSelectedTabIndex: Int?,
        customVariables: Map<String, CustomVariableValue> = emptyMap(),
    ) = PaywallState.Loaded.Components(
        stack = previewStackComponentStyle(children = emptyList()),
        header = null,
        stickyFooter = null,
        background = BackgroundStyles.Color(color = ColorStyles(light = ColorStyle.Solid(Color.White))),
        showPricesWithDecimals = true,
        variableConfig = UiConfig.VariableConfig(
            variableCompatibilityMap = emptyMap(),
            functionCompatibilityMap = emptyMap(),
        ),
        variableDataProvider = VariableDataProvider(MockResourceProvider()),
        offering = Offering(
            identifier = "test-offering",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = (packagesOutsideTabs.map { it.pkg } +
                packagesByTab.values.flatten().map { it.pkg }).distinct(),
            paywall = null,
            paywallComponents = null,
        ),
        locales = nonEmptySetOf(localeId),
        storefrontCountryCode = "US",
        dateProvider = { Date() },
        packages = PaywallState.Loaded.Components.AvailablePackages(
            packagesOutsideTabs = packagesOutsideTabs,
            packagesByTab = packagesByTab,
        ),
        initialLocaleList = LocaleList("en-US"),
        initialSelectedTabIndex = initialSelectedTabIndex,
        purchases = MockPurchasesType(),
        customVariables = customVariables,
    )
}
