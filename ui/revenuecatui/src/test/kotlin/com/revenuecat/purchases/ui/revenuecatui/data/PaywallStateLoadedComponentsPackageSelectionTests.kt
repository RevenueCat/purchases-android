package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.intl.LocaleList
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
import com.revenuecat.purchases.ui.revenuecatui.components.LoadedPaywallComponents
import com.revenuecat.purchases.ui.revenuecatui.components.LoadedWorkflowPaywall
import com.revenuecat.purchases.ui.revenuecatui.components.previewStackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptySetOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker
import com.revenuecat.purchases.ui.revenuecatui.helpers.windowChangingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
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

    @get:Rule
    val composeTestRule = createComposeRule()

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
    fun `Should move selection when an expanded screen hides the default package`() {
        val state = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = mapOf(
                0 to listOf(
                    packageInfo(
                        TestData.Packages.annual,
                        isSelectedByDefault = true,
                        visibilityOverrides = listOf(expandedVisibilityOverride(visible = false)),
                    ),
                    packageInfo(TestData.Packages.monthly, isSelectedByDefault = false),
                ),
            ),
            initialSelectedTabIndex = null,
        )
        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.annual)

        state.update(screenCondition = ScreenCondition.EXPANDED)

        assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.monthly)
    }

    @Test
    fun `Should update a workflow fallback when screen visibility changes its source selection`() {
        val packageState = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = mapOf(
                0 to listOf(
                    packageInfo(
                        TestData.Packages.annual,
                        isSelectedByDefault = true,
                        visibilityOverrides = listOf(expandedVisibilityOverride(visible = false)),
                    ),
                    packageInfo(TestData.Packages.monthly, isSelectedByDefault = false),
                ),
            ),
            initialSelectedTabIndex = null,
        )
        val packageLessWorkflowStep = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = emptyMap(),
            initialSelectedTabIndex = null,
        )
        packageLessWorkflowStep.setDefaultPackage(packageState)

        packageState.update(screenCondition = ScreenCondition.EXPANDED)
        packageLessWorkflowStep.update(screenCondition = ScreenCondition.EXPANDED)

        assertThat(packageLessWorkflowStep.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.monthly)
    }

    @Test
    fun `Should preserve a visible workflow fallback across later screen changes`() {
        val packageState = paywallStateWithExpandedHiddenDefault()
        val packageLessWorkflowStep = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = emptyMap(),
            initialSelectedTabIndex = null,
        )
        packageLessWorkflowStep.setDefaultPackage(packageState)
        packageLessWorkflowStep.update(screenCondition = ScreenCondition.EXPANDED)
        assertThat(packageLessWorkflowStep.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.monthly)

        packageLessWorkflowStep.update(screenCondition = ScreenCondition.COMPACT)

        assertThat(packageLessWorkflowStep.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.monthly)
    }

    @Test
    fun `Should ignore a duplicate package visible outside the workflow fallback tab`() {
        val annualInInitialTab = packageInfo(
            TestData.Packages.annual,
            isSelectedByDefault = true,
            visibilityOverrides = listOf(expandedVisibilityOverride(visible = false)),
        )
        val packageState = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = mapOf(
                0 to listOf(
                    annualInInitialTab,
                    packageInfo(TestData.Packages.monthly, isSelectedByDefault = false),
                ),
                1 to listOf(packageInfo(TestData.Packages.annual, isSelectedByDefault = true)),
            ),
            initialSelectedTabIndex = 0,
        )
        val packageLessWorkflowStep = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = emptyMap(),
            initialSelectedTabIndex = null,
        )
        packageLessWorkflowStep.setDefaultPackage(packageState)

        packageState.update(screenCondition = ScreenCondition.EXPANDED)
        packageLessWorkflowStep.update(screenCondition = ScreenCondition.EXPANDED)

        assertThat(packageLessWorkflowStep.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.monthly)
    }

    @Test
    fun `Should keep the initial workflow fallback when attached after a user selection`() {
        val monthlyInfo = packageInfo(TestData.Packages.monthly, isSelectedByDefault = false)
        val packageState = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = mapOf(
                0 to listOf(
                    packageInfo(TestData.Packages.annual, isSelectedByDefault = true),
                    monthlyInfo,
                ),
            ),
            initialSelectedTabIndex = null,
        )
        val packageLessWorkflowStep = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = emptyMap(),
            initialSelectedTabIndex = null,
        )
        packageState.update(monthlyInfo.uniqueId)

        packageLessWorkflowStep.setDefaultPackage(packageState)

        assertThat(packageLessWorkflowStep.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.annual)
    }

    @Test
    fun `Should propagate the live window size to a component paywall selection`() = with(composeTestRule) {
        val state = paywallStateWithExpandedHiddenDefault()

        windowChangingTest(
            arrange = { state },
            act = { LoadedPaywallComponents(state = it, clickHandler = { }) },
            assert = { windowSizeController ->
                windowSizeController.setWindowSizeInexact(width = 900.dp, height = 1_000.dp)
                runOnIdle {
                    assertThat(state.selectedPackageInfo?.rcPackage).isEqualTo(TestData.Packages.monthly)
                }
            },
        )
    }

    @Test
    fun `Should propagate the live window size to a workflow fallback selection`() = with(composeTestRule) {
        val packageState = paywallStateWithExpandedHiddenDefault()
        val packageLessWorkflowStep = paywallState(
            packagesOutsideTabs = emptyList(),
            packagesByTab = emptyMap(),
            initialSelectedTabIndex = null,
        )
        packageLessWorkflowStep.setDefaultPackage(packageState)

        windowChangingTest(
            arrange = {
                WorkflowPaywallUiState(
                    currentStepId = "package-less",
                    stepStates = mapOf(
                        "packages" to packageState,
                        "package-less" to packageLessWorkflowStep,
                    ),
                )
            },
            act = { workflowState ->
                LoadedWorkflowPaywall(
                    workflowState = workflowState,
                    onTransitionComplete = { },
                    clickHandler = { },
                    componentInteractionTracker = PaywallComponentInteractionTracker { _ -> },
                )
            },
            assert = { windowSizeController ->
                windowSizeController.setWindowSizeInexact(width = 900.dp, height = 1_000.dp)
                runOnIdle {
                    assertThat(packageLessWorkflowStep.selectedPackageInfo?.rcPackage)
                        .isEqualTo(TestData.Packages.monthly)
                }
            },
        )
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

    private fun expandedVisibilityOverride(visible: Boolean) = PresentedOverride(
        conditions = listOf(ComponentOverride.Condition.Expanded),
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

    private fun paywallStateWithExpandedHiddenDefault() = paywallState(
        packagesOutsideTabs = emptyList(),
        packagesByTab = mapOf(
            0 to listOf(
                packageInfo(
                    TestData.Packages.annual,
                    isSelectedByDefault = true,
                    visibilityOverrides = listOf(expandedVisibilityOverride(visible = false)),
                ),
                packageInfo(TestData.Packages.monthly, isSelectedByDefault = false),
            ),
        ),
        initialSelectedTabIndex = null,
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
