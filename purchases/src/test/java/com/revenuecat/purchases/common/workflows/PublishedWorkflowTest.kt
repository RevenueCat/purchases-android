package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.ExitOffer
import com.revenuecat.purchases.paywalls.components.common.ExitOffers
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.net.URL

@OptIn(InternalRevenueCatAPI::class)
class PublishedWorkflowTest {

    private val defaultLocaleId = LocaleId("en_US")
    private val componentsConfig = ComponentsConfig(
        base = PaywallComponentsConfig(
            stack = StackComponent(components = emptyList()),
            background = Background.Color(ColorScheme(light = ColorInfo.Hex(0))),
            stickyFooter = null,
        ),
    )

    @Test
    fun `dismissExitOffer uses terminal step when singleStepFallbackId is absent`() {
        val workflow = workflow(
            steps = mapOf(
                "step-1" to step("step-1", "screen-1", nextStepId = "step-2"),
                "step-2" to step("step-2", "screen-2"),
            ),
            screens = mapOf(
                "screen-1" to screen("screen-1"),
                "screen-2" to screen("screen-2", exitOfferingId = "exit-offering"),
            ),
        )

        assertThat(workflow.dismissExitOffer).isEqualTo(
            WorkflowExitOffer(offeringId = "exit-offering", stepId = "step-2"),
        )
    }

    @Test
    fun `dismissExitOffer uses singleStepFallbackId when present`() {
        val workflow = workflow(
            steps = mapOf(
                "step-1" to step("step-1", "screen-1", nextStepId = "step-2"),
                "step-2" to step("step-2", "screen-2"),
            ),
            screens = mapOf(
                "screen-1" to screen("screen-1", exitOfferingId = "exit-offering"),
                "screen-2" to screen("screen-2"),
            ),
            singleStepFallbackId = "step-1",
        )

        assertThat(workflow.dismissExitOffer).isEqualTo(
            WorkflowExitOffer(offeringId = "exit-offering", stepId = "step-1"),
        )
    }

    @Test
    fun `dismissExitOffer is null when canonical step has no dismiss exit offer`() {
        val workflow = workflow(
            steps = mapOf("step-1" to step("step-1", "screen-1")),
            screens = mapOf("screen-1" to screen("screen-1")),
        )

        assertThat(workflow.dismissExitOffer).isNull()
    }

    private fun workflow(
        steps: Map<String, WorkflowStep>,
        screens: Map<String, WorkflowScreen>,
        singleStepFallbackId: String? = null,
    ) = PublishedWorkflow(
        id = "workflow",
        displayName = "Workflow",
        initialStepId = "step-1",
        steps = steps,
        screens = screens,
        uiConfig = UiConfig(),
        singleStepFallbackId = singleStepFallbackId,
    )

    private fun step(id: String, screenId: String, nextStepId: String? = null) = WorkflowStep(
        id = id,
        type = "screen",
        screenId = screenId,
        triggerActions = nextStepId?.let {
            mapOf("next" to WorkflowTriggerAction.Step(stepId = it))
        } ?: emptyMap(),
    )

    private fun screen(screenId: String, exitOfferingId: String? = null) = WorkflowScreen(
        name = screenId,
        templateName = "template_v2",
        revision = 1,
        assetBaseURL = URL("https://assets.paywalls.com"),
        componentsConfig = componentsConfig,
        componentsLocalizations = mapOf(defaultLocaleId to emptyMap()),
        defaultLocaleIdentifier = defaultLocaleId,
        offeringIdentifier = "offering",
        exitOffers = exitOfferingId?.let { ExitOffers(dismiss = ExitOffer(offeringId = it)) },
    )
}
