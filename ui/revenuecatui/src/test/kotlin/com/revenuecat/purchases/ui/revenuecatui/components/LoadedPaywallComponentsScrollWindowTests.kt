package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.WorkflowPaywallUiState
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.extensions.toComponentsPaywallState
import com.revenuecat.purchases.ui.revenuecatui.extensions.validatePaywallComponentsDataOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.windowChangingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.net.URL

/** Exercises both scroll owners together, including changes without Activity recreation (foldables). */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w500dp-h1000dp")
internal class LoadedPaywallComponentsScrollWindowTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `expanded root enables vertical scrolling when unfolding and restores outer scrolling when folding`() {
        assertResizeDoesNotNestVerticalScroll(
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(ComponentOverride.Condition.Expanded),
                    properties = PartialStackComponent(overflow = StackComponent.Overflow.SCROLL),
                ),
            ),
        )
    }

    @Test
    fun `measured width enables and disables root scrolling while resizing`() {
        assertResizeDoesNotNestVerticalScroll(
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.WindowWidthRule(
                            operator = ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL,
                            value = 840.0,
                        ),
                    ),
                    properties = PartialStackComponent(overflow = StackComponent.Overflow.SCROLL),
                ),
            ),
        )
    }

    @Test
    fun `workflow step resolves measured width scrolling on first render and after resizing`() {
        assertResizeDoesNotNestVerticalScroll(
            workflow = true,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(
                        ComponentOverride.Condition.WindowWidthRule(
                            operator = ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL,
                            value = 400.0,
                        ),
                    ),
                    properties = PartialStackComponent(overflow = StackComponent.Overflow.SCROLL),
                ),
            ),
        )
    }

    @Test
    fun `horizontal scroll root becomes vertically scrolling when unfolded`() {
        assertResizeDoesNotNestVerticalScroll(
            dimension = Dimension.Horizontal(VerticalAlignment.CENTER, FlexDistribution.START),
            overflow = StackComponent.Overflow.SCROLL,
            initiallyScrollsHorizontally = true,
            overrides = listOf(
                ComponentOverride(
                    conditions = listOf(ComponentOverride.Condition.Expanded),
                    properties = PartialStackComponent(
                        dimension = Dimension.Vertical(HorizontalAlignment.CENTER, FlexDistribution.START),
                        overflow = StackComponent.Overflow.SCROLL,
                    ),
                ),
            ),
        )
    }

    private fun assertResizeDoesNotNestVerticalScroll(
        dimension: Dimension = Dimension.Vertical(HorizontalAlignment.CENTER, FlexDistribution.START),
        overflow: StackComponent.Overflow? = null,
        initiallyScrollsHorizontally: Boolean = false,
        workflow: Boolean = false,
        overrides: List<ComponentOverride<PartialStackComponent>>,
    ): Unit = with(composeTestRule) {
        val state = createState(dimension, overflow, overrides)
        windowChangingTest(
            arrange = { state },
            act = {
                if (workflow) {
                    LoadedWorkflowPaywall(
                        workflowState = WorkflowPaywallUiState(
                            currentStepId = "step",
                            stepStates = mapOf("step" to it),
                        ),
                        onTransitionComplete = { },
                        clickHandler = { },
                        componentInteractionTracker = PaywallComponentInteractionTracker { _ -> },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    LoadedPaywallComponents(state = it, clickHandler = { }, modifier = Modifier.fillMaxSize())
                }
            },
            assert = { controller ->
                assertSingleVerticalScroll()
                assertHorizontalScrollCount(if (initiallyScrollsHorizontally) 1 else 0)
                controller.setWindowSizeInexact(width = 1000.dp, height = 1000.dp)
                assertSingleVerticalScroll()
                assertHorizontalScrollCount(0)
                runOnIdle { assertThat(state.paywallBoundsDp!!.width.value).isGreaterThanOrEqualTo(840f) }
                controller.setWindowSizeInexact(width = if (workflow) 350.dp else 500.dp, height = 1000.dp)
                assertSingleVerticalScroll()
                assertHorizontalScrollCount(if (initiallyScrollsHorizontally) 1 else 0)
                runOnIdle { assertThat(state.paywallBoundsDp!!.width.value).isLessThan(600f) }
            },
        )
    }

    private fun assertSingleVerticalScroll() {
        composeTestRule.onNodeWithText(TEXT).assertIsDisplayed()
        composeTestRule.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange),
            useUnmergedTree = true,
        ).assertCountEquals(1)
    }

    private fun assertHorizontalScrollCount(expected: Int) {
        composeTestRule.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange),
            useUnmergedTree = true,
        ).assertCountEquals(expected)
    }

    private fun createState(
        dimension: Dimension = Dimension.Vertical(HorizontalAlignment.CENTER, FlexDistribution.START),
        overflow: StackComponent.Overflow? = null,
        overrides: List<ComponentOverride<PartialStackComponent>>,
    ): PaywallState.Loaded.Components {
        // FakePaywallState wraps supplied components in another stack. Use the actual configured root instead.
        val textKey = LocalizationKey("foldable-text")
        val data = PaywallComponentsData(
            id = "foldable-paywall",
            templateName = "components",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        components = listOf(
                            TextComponent(text = textKey, color = ColorScheme(ColorInfo.Hex(Color.Black.toArgb()))),
                            TestData.Components.monthlyPackageComponent,
                        ),
                        dimension = dimension,
                        overflow = overflow,
                        overrides = overrides,
                    ),
                    background = Background.Color(ColorScheme(ColorInfo.Hex(Color.White.toArgb()))),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = mapOf(
                LocaleId("en_US") to mapOf(textKey to LocalizationData.Text(TEXT)),
            ),
            defaultLocaleIdentifier = LocaleId("en_US"),
        )
        val offering = Offering(
            identifier = "foldable-offering",
            serverDescription = "Foldable regression test",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly),
            paywallComponents = Offering.PaywallComponents(UiConfig(), data),
        )
        val validated = offering.validatePaywallComponentsDataOrNull()?.getOrThrow()!!
        return offering.toComponentsPaywallState(validated)
    }

    private companion object {
        const val TEXT = "Foldable paywall content"
    }
}
