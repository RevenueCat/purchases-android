package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.ExitOffer
import com.revenuecat.purchases.paywalls.components.common.ExitOffers
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
    fun `dismissExitOffer is null when singleStepFallbackId is absent`() {
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

        assertThat(workflow.dismissExitOffer).isNull()
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

    @Test
    fun `offeringIdentifierFor prefers the step's own offering over its screen's`() {
        val step = step("step-1", "screen-1", offeringIdentifier = "step-offering")
        val workflow = workflow(
            steps = mapOf("step-1" to step),
            screens = mapOf("screen-1" to screen("screen-1", offeringIdentifier = "screen-offering")),
        )

        assertThat(workflow.offeringIdentifierFor(step)).isEqualTo("step-offering")
    }

    @Test
    fun `offeringIdentifierFor falls back to the screen's offering when the step has none`() {
        val step = step("step-1", "screen-1")
        val workflow = workflow(
            steps = mapOf("step-1" to step),
            screens = mapOf("screen-1" to screen("screen-1", offeringIdentifier = "screen-offering")),
        )

        assertThat(workflow.offeringIdentifierFor(step)).isEqualTo("screen-offering")
    }

    @Test
    fun `offeringIdentifierFor is null when neither the step nor its screen has an offering`() {
        val step = step("step-1", "screen-1")
        val workflow = workflow(
            steps = mapOf("step-1" to step),
            screens = mapOf("screen-1" to screen("screen-1", offeringIdentifier = null)),
        )

        assertThat(workflow.offeringIdentifierFor(step)).isNull()
    }

    @Test
    fun `offeringIdentifierFor is null when the step has no offering and its screen is unknown`() {
        val stepWithoutScreen = step("step-1", screenId = null)
        val stepWithMissingScreen = step("step-2", "missing-screen")
        val workflow = workflow(
            steps = mapOf("step-1" to stepWithoutScreen, "step-2" to stepWithMissingScreen),
            screens = mapOf("screen-1" to screen("screen-1")),
        )

        assertThat(workflow.offeringIdentifierFor(stepWithoutScreen)).isNull()
        assertThat(workflow.offeringIdentifierFor(stepWithMissingScreen)).isNull()
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
        singleStepFallbackId = singleStepFallbackId,
    )

    private fun step(
        id: String,
        screenId: String?,
        nextStepId: String? = null,
        offeringIdentifier: String? = null,
    ) = WorkflowStep(
        id = id,
        type = "screen",
        screenId = screenId,
        paramValues = offeringIdentifier?.let {
            mapOf("offering" to JsonObject(mapOf("identifier" to JsonPrimitive(it))))
        }.orEmpty(),
        triggerActions = nextStepId?.let {
            mapOf("next" to WorkflowTriggerAction.Step(stepId = it))
        } ?: emptyMap(),
    )

    private fun screen(
        screenId: String,
        exitOfferingId: String? = null,
        offeringIdentifier: String? = "offering",
    ) = WorkflowScreen(
        name = screenId,
        templateName = "template_v2",
        revision = 1,
        assetBaseURL = URL("https://assets.paywalls.com"),
        componentsConfig = componentsConfig,
        componentsLocalizations = mapOf(defaultLocaleId to emptyMap()),
        defaultLocaleIdentifier = defaultLocaleId,
        offeringIdentifier = offeringIdentifier,
        exitOffers = exitOfferingId?.let { ExitOffers(dismiss = ExitOffer(offeringId = it)) },
    )
}
