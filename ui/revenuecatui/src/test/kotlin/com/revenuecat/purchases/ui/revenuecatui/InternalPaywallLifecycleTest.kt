@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.WorkflowScreen
import com.revenuecat.purchases.common.workflows.WorkflowScreenType
import com.revenuecat.purchases.common.workflows.WorkflowStep
import com.revenuecat.purchases.common.workflows.WorkflowTrigger
import com.revenuecat.purchases.common.workflows.WorkflowTriggerAction
import com.revenuecat.purchases.common.workflows.WorkflowTriggerType
import com.revenuecat.purchases.paywalls.components.ButtonComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.data.MockPurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModel
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(AndroidJUnit4::class)
class InternalPaywallLifecycleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `paywall refresh uses the forced theme and responds when the override changes`() {
        val model = mockk<PaywallViewModel>(relaxed = true)
        every { model.state } returns MutableStateFlow(PaywallState.Loading)
        val options = PaywallOptions.Builder(dismissRequest = {}).build()
        val darkMode = mutableStateOf(true)
        val configuration = Configuration().apply { uiMode = Configuration.UI_MODE_NIGHT_NO }
        composeTestRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                InternalPaywall(options, model, isDarkModeOverride = darkMode.value)
            }
        }
        composeTestRule.runOnIdle {
            verify { model.refreshStateIfColorsChanged(any(), isDark = true) }
            clearMocks(model, answers = false)
            darkMode.value = false
        }
        composeTestRule.runOnIdle {
            verify { model.refreshStateIfColorsChanged(any(), isDark = false) }
        }
    }

    @Test
    fun `Compose paywall resets to Loading only after leaving composition`(): Unit = with(composeTestRule) {
        // Loaded paywalls can contain animations that are unrelated to this lifecycle assertion.
        mainClock.autoAdvance = false
        var showPaywall by mutableStateOf(true)
        val options = PaywallOptions.Builder(dismissRequest = { showPaywall = false })
            .setOffering(TestData.template1Offering)
            .build()
        val viewModel = PaywallViewModelImpl(
            resourceProvider = MockResourceProvider(),
            purchases = MockPurchasesType(storefrontCountryCode = "US"),
            options = options,
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false,
            shouldDisplayBlock = null,
        )

        setContent {
            if (showPaywall) {
                InternalPaywall(options, viewModel)
            }
        }
        waitUntil { viewModel.state.value is PaywallState.Loaded }

        runOnUiThread {
            viewModel.closePaywall()
            assertThat(viewModel.state.value).isInstanceOf(PaywallState.Loaded::class.java)
        }

        mainClock.advanceTimeBy(100)
        runOnIdle {
            assertThat(viewModel.state.value).isEqualTo(PaywallState.Loading)
        }
    }

    @Test
    fun `Compose reports one paywall impression per visit when navigating forward and back`(): Unit =
        with(composeTestRule) {
            val impressions = mutableListOf<FlowEvent>()
            val offering = TestData.template1Offering
            val options = PaywallOptions.Builder(dismissRequest = { error("Paywall should stay open") })
                .injectedWorkflow(twoStepPaywallFlow(offering.identifier), offering, UiConfig())
                .setListener(object : PaywallListener {
                    override fun onFlowEvent(event: FlowEvent) {
                        if (event.getProperty(FlowEvent.Keys.EVENT_TYPE) == FlowEvent.EventTypes.PAYWALL_IMPRESSION) {
                            impressions += event
                        }
                    }
                })
                .build()
            val viewModel = PaywallViewModelImpl(
                resourceProvider = MockResourceProvider(),
                purchases = MockPurchasesType(storefrontCountryCode = "US"),
                options = options,
                colorScheme = TestData.Constants.currentColorScheme,
                isDarkMode = false,
                shouldDisplayBlock = null,
            )

            setContent { InternalPaywall(options, viewModel) }
            waitUntil { viewModel.state.value !is PaywallState.Loading }
            assertThat(viewModel.state.value).isInstanceOf(PaywallState.Loaded.Components::class.java)

            onNodeWithText("Next").performClick()
            onNodeWithText("Back").performClick()

            runOnIdle {
                assertThat(impressions.map { it.getProperty(FlowEvent.Keys.STEP_ID) })
                    .containsExactly("A", "B", "A")
                val sessionIds = impressions.map { it.getProperty(FlowEvent.Keys.SESSION_ID) }
                assertThat(sessionIds).doesNotContainNull().doesNotHaveDuplicates()
            }
        }

    private fun twoStepPaywallFlow(offeringId: String): PublishedWorkflow {
        val next = LocalizationKey("next-button")
        val back = LocalizationKey("back-button")
        val locale = LocaleId("en_US")
        val textColor = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb()))

        fun button(label: LocalizationKey, action: ButtonComponent.Action, id: String) = ButtonComponent(
            action = action,
            id = id,
            stack = StackComponent(components = listOf(TextComponent(text = label, color = textColor))),
        )

        // Reuse the same screen and offering so a new impression depends on the step visit changing.
        val screen = WorkflowScreen(
            templateName = "template_v2",
            revision = 1,
            assetBaseURL = URL("https://assets.paywalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(
                        components = listOf(
                            TestData.Components.monthlyPackageComponent,
                            button(next, ButtonComponent.Action.WorkflowTrigger, "next-button"),
                            button(back, ButtonComponent.Action.NavigateBack, "back-button"),
                        ),
                    ),
                    stickyFooter = null,
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
                ),
            ),
            componentsLocalizations = mapOf(
                locale to mapOf(next to LocalizationData.Text("Next"), back to LocalizationData.Text("Back")),
            ),
            defaultLocaleIdentifier = locale,
            offeringIdentifier = offeringId,
        )
        val paywallMetadata = JsonObject(
            mapOf(WorkflowScreenType.METADATA_KEY to JsonArray(listOf(JsonPrimitive(WorkflowScreenType.PAYWALL)))),
        )
        val first = WorkflowStep(
            id = "A",
            type = "screen",
            screenId = "paywall",
            triggers = listOf(WorkflowTrigger("Next", WorkflowTriggerType.ON_PRESS, "next", "next-button")),
            triggerActions = mapOf("next" to WorkflowTriggerAction.Step("B")),
            metadata = paywallMetadata,
        )
        return PublishedWorkflow(
            id = "paywall-flow",
            displayName = "Paywall Flow",
            initialStepId = first.id,
            steps = mapOf(
                first.id to first,
                "B" to WorkflowStep(id = "B", type = "screen", screenId = "paywall", metadata = paywallMetadata),
            ),
            screens = mapOf("paywall" to screen),
        )
    }
}
