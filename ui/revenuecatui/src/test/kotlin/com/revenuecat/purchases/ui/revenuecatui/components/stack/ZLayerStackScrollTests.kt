package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.PaywallComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.LoadedPaywallComponents
import com.revenuecat.purchases.ui.revenuecatui.components.LoadedWorkflowPaywall
import com.revenuecat.purchases.ui.revenuecatui.data.WorkflowPaywallUiState
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallComponentInteractionTracker
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w500dp-h1000dp")
internal class ZLayerStackScrollTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var clicks = 0
    private var viewportHeight by mutableStateOf(400.dp)

    @Test
    fun `automatic root scroll preserves nested Fill tap target height`() {
        assertFullHeightTapTarget(workflow = false)
    }

    @Test
    fun `workflow automatic root scroll preserves nested Fill tap target height`() {
        assertFullHeightTapTarget(workflow = true)
    }

    @Test
    fun `Fill layers follow both increases and decreases in viewport height`() {
        render(layer(listOf(layer(listOf(button(Size(Fill(), Fill())))))))
        composeTestRule.onNode(hasClickAction()).assertHeightIsEqualTo(400.dp)
        composeTestRule.runOnIdle { viewportHeight = 600.dp }
        composeTestRule.onNode(hasClickAction()).assertHeightIsEqualTo(600.dp)
        composeTestRule.runOnIdle { viewportHeight = 250.dp }
        composeTestRule.onNode(hasClickAction()).assertHeightIsEqualTo(250.dp)
    }

    @Test
    fun `Fill layers reserve the sticky footer height`() {
        render(
            root = layer(listOf(button(Size(Fill(), Fill())))),
            footer = StickyFooterComponent(
                stack = StackComponent(components = emptyList(), size = Size(Fill(), Fixed(100u))),
            ),
        )
        composeTestRule.onNode(hasClickAction()).assertHeightIsEqualTo(300.dp)
    }

    @Test
    fun `tall layer content still scrolls to the last button`() {
        val content = StackComponent(
            components = listOf(
                StackComponent(components = emptyList(), size = Size(Fill(), Fixed(650u))),
                button(Size(Fill(), Fixed(80u)), listOf(text())),
            ),
            size = Size(Fill(), Fit()),
        )
        render(layer(listOf(content)))

        composeTestRule.onNodeWithText(TEXT).performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.runOnIdle { assertThat(clicks).isEqualTo(1) }
    }

    private fun assertFullHeightTapTarget(workflow: Boolean) {
        val row = StackComponent(
            components = listOf(button(Size(Fill(), Fill()))),
            dimension = Dimension.Horizontal(VerticalAlignment.CENTER, FlexDistribution.START),
            size = Size(Fill(), Fill()),
        )
        val column = StackComponent(components = listOf(row), size = Size(Fill(), Fill()))
        render(layer(listOf(layer(listOf(column)))), workflow)
        composeTestRule.onNode(hasClickAction())
            .assertHeightIsEqualTo(400.dp)
            .performTouchInput { click(Offset(center.x, height * 0.75f)) }
        composeTestRule.runOnIdle { assertThat(clicks).isEqualTo(1) }
    }

    private fun render(root: StackComponent, workflow: Boolean = false, footer: StickyFooterComponent? = null) {
        val state = createState(root, footer)
        composeTestRule.setContent {
            Box(Modifier.requiredSize(200.dp, viewportHeight)) {
                if (workflow) {
                    LoadedWorkflowPaywall(
                        workflowState = WorkflowPaywallUiState(currentStepId = "step", stepStates = mapOf("step" to state)),
                        onTransitionComplete = {},
                        clickHandler = { clicks++ },
                        componentInteractionTracker = PaywallComponentInteractionTracker {},
                    )
                } else {
                    LoadedPaywallComponents(state = state, clickHandler = { clicks++ })
                }
            }
        }
    }

    private fun layer(children: List<PaywallComponent>) = StackComponent(
        components = children,
        dimension = Dimension.ZLayer(TwoDimensionalAlignment.CENTER),
        size = Size(Fill(), Fill()),
    )

    private fun button(
        size: Size,
        children: List<PaywallComponent> = emptyList(),
    ) = ButtonComponent(
        action = ButtonComponent.Action.NavigateBack,
        stack = StackComponent(components = children, size = size),
    )

    private fun text() = TextComponent(
        text = LocalizationKey("label"),
        color = ColorScheme(ColorInfo.Hex(Color.Black.toArgb())),
    )

    private fun createState(root: StackComponent, footer: StickyFooterComponent?) = FakePaywallState(
        rootStack = root,
        stickyFooter = footer,
        packages = listOf(TestData.Packages.monthly),
        addPackageComponents = false,
        localizations = nonEmptyMapOf(
            LocaleId("en_US") to nonEmptyMapOf(LocalizationKey("label") to LocalizationData.Text(TEXT)),
        ),
    )

    private companion object {
        const val TEXT = "Continue"
    }
}
