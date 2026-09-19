@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.net.URL

class WorkflowScreenOrderTest {

    @Test
    fun `orders screens breadth first from the initial step`() {
        val workflow = workflow(
            initialStepId = "step_1",
            steps = listOf(
                step("step_1", screenId = "screen_1", next = listOf("step_2")),
                step("step_2", screenId = "screen_2", next = listOf("step_3")),
                step("step_3", screenId = "screen_3"),
            ),
            screens = screens("screen_3", "screen_1", "screen_2"),
        )

        assertThat(workflow.screensInVisitOrder().map { it.name })
            .containsExactly("screen_1", "screen_2", "screen_3")
    }

    @Test
    fun `follows trigger declaration order for sibling steps`() {
        val firstStep = WorkflowStep(
            id = "step_1",
            type = "screen",
            screenId = "screen_1",
            triggers = listOf(
                trigger(actionId = "action_b"),
                trigger(actionId = "action_a"),
            ),
            triggerActions = mapOf(
                "action_a" to WorkflowTriggerAction.Step("step_a"),
                "action_b" to WorkflowTriggerAction.Step("step_b"),
            ),
        )
        val workflow = workflow(
            initialStepId = "step_1",
            steps = listOf(firstStep, step("step_a", screenId = "screen_a"), step("step_b", screenId = "screen_b")),
            screens = screens("screen_1", "screen_a", "screen_b"),
        )

        assertThat(workflow.screensInVisitOrder().map { it.name })
            .containsExactly("screen_1", "screen_b", "screen_a")
    }

    @Test
    fun `appends screens no step reaches`() {
        val workflow = workflow(
            initialStepId = "step_1",
            steps = listOf(step("step_1", screenId = "screen_1")),
            screens = screens("screen_orphan", "screen_1"),
        )

        assertThat(workflow.screensInVisitOrder().map { it.name })
            .containsExactly("screen_1", "screen_orphan")
    }

    @Test
    fun `walks a step whose action no trigger references`() {
        val firstStep = WorkflowStep(
            id = "step_1",
            type = "screen",
            screenId = "screen_1",
            triggers = emptyList(),
            triggerActions = mapOf("action_a" to WorkflowTriggerAction.Step("step_2")),
        )
        val workflow = workflow(
            initialStepId = "step_1",
            steps = listOf(firstStep, step("step_2", screenId = "screen_2")),
            screens = screens("screen_2", "screen_1"),
        )

        assertThat(workflow.screensInVisitOrder().map { it.name })
            .containsExactly("screen_1", "screen_2")
    }

    @Test
    fun `terminates on a cycle`() {
        val workflow = workflow(
            initialStepId = "step_1",
            steps = listOf(
                step("step_1", screenId = "screen_1", next = listOf("step_2")),
                step("step_2", screenId = "screen_2", next = listOf("step_1")),
            ),
            screens = screens("screen_1", "screen_2"),
        )

        assertThat(workflow.screensInVisitOrder().map { it.name })
            .containsExactly("screen_1", "screen_2")
    }

    @Test
    fun `roots at the single step fallback as well as the initial step`() {
        val workflow = workflow(
            initialStepId = "step_1",
            steps = listOf(
                step("step_1", screenId = "screen_1"),
                step("step_fallback", screenId = "screen_fallback", next = listOf("step_deep")),
                step("step_deep", screenId = "screen_deep"),
            ),
            screens = screens("screen_deep", "screen_fallback", "screen_1"),
            singleStepFallbackId = "step_fallback",
        )

        assertThat(workflow.screensInVisitOrder().map { it.name })
            .containsExactly("screen_1", "screen_fallback", "screen_deep")
    }

    @Test
    fun `ignores a missing step and a screen id with no screen`() {
        val workflow = workflow(
            initialStepId = "step_1",
            steps = listOf(
                step("step_1", screenId = "screen_missing", next = listOf("step_absent", "step_2")),
                step("step_2", screenId = "screen_2"),
            ),
            screens = screens("screen_2"),
        )

        assertThat(workflow.screensInVisitOrder().map { it.name }).containsExactly("screen_2")
    }

    @Test
    fun `is empty when the workflow has no screens`() {
        val workflow = workflow(
            initialStepId = "step_1",
            steps = listOf(step("step_1", screenId = null)),
            screens = emptyMap(),
        )

        assertThat(workflow.screensInVisitOrder()).isEmpty()
    }

    private fun workflow(
        initialStepId: String,
        steps: List<WorkflowStep>,
        screens: Map<String, WorkflowScreen>,
        singleStepFallbackId: String? = null,
    ): PublishedWorkflow = PublishedWorkflow(
        id = "wf_1",
        displayName = "Workflow",
        initialStepId = initialStepId,
        steps = steps.associateBy { it.id },
        screens = screens,
        singleStepFallbackId = singleStepFallbackId,
    )

    private fun step(id: String, screenId: String?, next: List<String> = emptyList()): WorkflowStep {
        val actions = next.associate { stepId -> "action_to_$stepId" to WorkflowTriggerAction.Step(stepId) }
        return WorkflowStep(
            id = id,
            type = "screen",
            screenId = screenId,
            triggers = actions.keys.map { actionId -> trigger(actionId) },
            triggerActions = actions,
        )
    }

    private fun trigger(actionId: String) = WorkflowTrigger(
        name = "trigger_$actionId",
        type = WorkflowTriggerType.ON_PRESS,
        actionId = actionId,
        componentId = "component_$actionId",
    )

    private fun screens(vararg ids: String): Map<String, WorkflowScreen> =
        ids.associateWith { id -> screen(id) }

    private fun screen(name: String) = WorkflowScreen(
        name = name,
        templateName = "template",
        assetBaseURL = URL("https://assets.revenuecat.com"),
        componentsConfig = ComponentsConfig(
            PaywallComponentsConfig(
                stack = StackComponent(components = emptyList()),
                background = Background.Color(ColorScheme(light = ColorInfo.Alias(ColorAlias("")))),
                stickyFooter = null,
            ),
        ),
        componentsLocalizations = emptyMap(),
        defaultLocaleIdentifier = LocaleId("en_US"),
    )
}
