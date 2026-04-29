@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.workflow

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.WorkflowStep
import com.revenuecat.purchases.common.workflows.WorkflowTrigger
import com.revenuecat.purchases.common.workflows.WorkflowTriggerAction
import com.revenuecat.purchases.common.workflows.WorkflowTriggerType
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import org.assertj.core.api.Assertions.assertThat

import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkflowNavigatorTest {

    private val step1 = WorkflowStep(
        id = "step-1",
        type = "screen",
        screenId = "screen-1",
        triggers = listOf(
            WorkflowTrigger(
                name = "Next",
                type = WorkflowTriggerType.ON_PRESS,
                actionId = "action-next",
                componentId = "btn-next",
            ),
        ),
        triggerActions = mapOf(
            "action-next" to WorkflowTriggerAction.Step(stepId = "step-2"),
        ),
    )

    private val step2 = WorkflowStep(
        id = "step-2",
        type = "screen",
        screenId = "screen-2",
        triggers = listOf(
            WorkflowTrigger(
                name = "Finish",
                type = WorkflowTriggerType.ON_PRESS,
                actionId = "action-finish",
                componentId = "btn-finish",
            ),
        ),
        triggerActions = mapOf(
            "action-finish" to WorkflowTriggerAction.Step(stepId = "step-3"),
        ),
    )

    private val step3 = WorkflowStep(
        id = "step-3",
        type = "screen",
        screenId = "screen-3",
    )

    private val workflow = PublishedWorkflow(
        id = "wfl-test",
        displayName = "Test Workflow",
        initialStepId = "step-1",
        steps = mapOf("step-1" to step1, "step-2" to step2, "step-3" to step3),
        screens = emptyMap(),
        uiConfig = UiConfig(),
        metadata = emptyMap(),
    )

    @Test
    fun `currentStep returns initial step`() {
        val navigator = WorkflowNavigator(workflow)
        assertThat(navigator.currentStep()).isEqualTo(step1)
    }

    @Test
    fun `triggerAction navigates to next step`() {
        val navigator = WorkflowNavigator(workflow)
        val result = navigator.triggerAction("btn-next", WorkflowTriggerType.ON_PRESS)
        assertThat(result).isEqualTo(step2)
        assertThat(navigator.currentStep()).isEqualTo(step2)
        assertThat(navigator.currentStep()?.id).isEqualTo("step-2")
    }

    @Test
    fun `triggerAction supports step type on second step`() {
        val navigator = WorkflowNavigator(workflow)
        navigator.triggerAction("btn-next", WorkflowTriggerType.ON_PRESS)
        val result = navigator.triggerAction("btn-finish", WorkflowTriggerType.ON_PRESS)
        assertThat(result).isEqualTo(step3)
        assertThat(navigator.currentStep()).isEqualTo(step3)
    }

    @Test
    fun `triggerAction with unknown componentId returns null and does not navigate`() {
        val navigator = WorkflowNavigator(workflow)
        val result = navigator.triggerAction("btn-unknown", WorkflowTriggerType.ON_PRESS)
        assertThat(result).isNull()
        assertThat(navigator.currentStep()).isEqualTo(step1)
    }

    @Test
    fun `canNavigateBack is false on initial step`() {
        val navigator = WorkflowNavigator(workflow)
        assertThat(navigator.canNavigateBack).isFalse()
    }

    @Test
    fun `canNavigateBack is true after forward navigation`() {
        val navigator = WorkflowNavigator(workflow)
        navigator.triggerAction("btn-next", WorkflowTriggerType.ON_PRESS)
        assertThat(navigator.canNavigateBack).isTrue()
    }

    @Test
    fun `navigateBack returns to previous step`() {
        val navigator = WorkflowNavigator(workflow)
        navigator.triggerAction("btn-next", WorkflowTriggerType.ON_PRESS)
        val result = navigator.navigateBack()
        assertThat(result).isEqualTo(step1)
        assertThat(navigator.currentStep()).isEqualTo(step1)
    }

    @Test
    fun `navigateBack returns null when backStack is empty`() {
        val navigator = WorkflowNavigator(workflow)
        val result = navigator.navigateBack()
        assertThat(result).isNull()
    }

    @Test
    fun `navigateBack restores canNavigateBack to false after popping last entry`() {
        val navigator = WorkflowNavigator(workflow)
        navigator.triggerAction("btn-next", WorkflowTriggerType.ON_PRESS)
        navigator.navigateBack()
        assertThat(navigator.canNavigateBack).isFalse()
    }

    @Test
    fun `multiple forward and back navigations work correctly`() {
        val navigator = WorkflowNavigator(workflow)
        navigator.triggerAction("btn-next", WorkflowTriggerType.ON_PRESS)
        navigator.triggerAction("btn-finish", WorkflowTriggerType.ON_PRESS)
        assertThat(navigator.currentStep()).isEqualTo(step3)

        val back1 = navigator.navigateBack()
        assertThat(back1).isEqualTo(step2)
        assertThat(navigator.currentStep()).isEqualTo(step2)
        assertThat(navigator.canNavigateBack).isTrue()

        val back2 = navigator.navigateBack()
        assertThat(back2).isEqualTo(step1)
        assertThat(navigator.currentStep()).isEqualTo(step1)
        assertThat(navigator.canNavigateBack).isFalse()
    }

    @Test
    fun `triggerAction with unknown action type returns null and does not navigate`() {
        val stepWithUnknownAction = WorkflowStep(
            id = "step-x",
            type = "screen",
            screenId = "screen-x",
            triggers = listOf(
                WorkflowTrigger(
                    name = "X",
                    type = WorkflowTriggerType.ON_PRESS,
                    actionId = "ax",
                    componentId = "btn-x",
                ),
            ),
            triggerActions = mapOf(
                "ax" to WorkflowTriggerAction.Unknown,
            ),
        )
        val wfl = workflow.copy(
            initialStepId = "step-x",
            steps = mapOf("step-x" to stepWithUnknownAction),
        )
        val navigator = WorkflowNavigator(wfl)
        val result = navigator.triggerAction("btn-x", WorkflowTriggerType.ON_PRESS)
        assertThat(result).isNull()
        assertThat(navigator.currentStep()).isEqualTo(stepWithUnknownAction)
    }

    // region backStack

    @Test
    fun `backStack is empty initially`() {
        val navigator = WorkflowNavigator(workflow)
        assertThat(navigator.backStack).isEmpty()
    }

    @Test
    fun `backStack contains visited step IDs in order after forward navigation`() {
        val navigator = WorkflowNavigator(workflow)
        navigator.triggerAction("btn-next", WorkflowTriggerType.ON_PRESS)
        navigator.triggerAction("btn-finish", WorkflowTriggerType.ON_PRESS)
        assertThat(navigator.backStack).containsExactly("step-1", "step-2")
    }

    @Test
    fun `backStack shrinks on navigateBack`() {
        val navigator = WorkflowNavigator(workflow)
        navigator.triggerAction("btn-next", WorkflowTriggerType.ON_PRESS)
        navigator.triggerAction("btn-finish", WorkflowTriggerType.ON_PRESS)
        navigator.navigateBack()
        assertThat(navigator.backStack).containsExactly("step-1")
    }

    // endregion

    // region peekBackStep

    @Test
    fun `peekBackStep is null on initial step`() {
        val navigator = WorkflowNavigator(workflow)
        assertThat(navigator.peekBackStep).isNull()
    }

    @Test
    fun `peekBackStep returns previous step after forward navigation`() {
        val navigator = WorkflowNavigator(workflow)
        navigator.triggerAction("btn-next", WorkflowTriggerType.ON_PRESS)
        assertThat(navigator.peekBackStep).isEqualTo(step1)
    }

    @Test
    fun `peekBackStep does not modify backStack`() {
        val navigator = WorkflowNavigator(workflow)
        navigator.triggerAction("btn-next", WorkflowTriggerType.ON_PRESS)
        navigator.peekBackStep
        navigator.peekBackStep
        assertThat(navigator.backStack).containsExactly("step-1")
    }

    // endregion

    // region peekTriggerStep

    @Test
    fun `peekTriggerStep returns target step for valid trigger`() {
        val navigator = WorkflowNavigator(workflow)
        val result = navigator.peekTriggerStep("btn-next", WorkflowTriggerType.ON_PRESS)
        assertThat(result).isEqualTo(step2)
    }

    @Test
    fun `peekTriggerStep returns null for unknown componentId`() {
        val navigator = WorkflowNavigator(workflow)
        val result = navigator.peekTriggerStep("btn-unknown", WorkflowTriggerType.ON_PRESS)
        assertThat(result).isNull()
    }

    @Test
    fun `peekTriggerStep returns null for non-Step trigger action`() {
        val stepWithUnknownAction = WorkflowStep(
            id = "step-x",
            type = "screen",
            screenId = "screen-x",
            triggers = listOf(
                WorkflowTrigger(
                    name = "X",
                    type = WorkflowTriggerType.ON_PRESS,
                    actionId = "ax",
                    componentId = "btn-x",
                ),
            ),
            triggerActions = mapOf("ax" to WorkflowTriggerAction.Unknown),
        )
        val wfl = workflow.copy(
            initialStepId = "step-x",
            steps = mapOf("step-x" to stepWithUnknownAction),
        )
        val navigator = WorkflowNavigator(wfl)
        assertThat(navigator.peekTriggerStep("btn-x", WorkflowTriggerType.ON_PRESS)).isNull()
    }

    @Test
    fun `peekTriggerStep does not mutate navigator state`() {
        val navigator = WorkflowNavigator(workflow)
        navigator.peekTriggerStep("btn-next", WorkflowTriggerType.ON_PRESS)
        assertThat(navigator.currentStep()).isEqualTo(step1)
        assertThat(navigator.backStack).isEmpty()
    }

    // endregion

    // region reachableSteps

    @Test
    fun `reachableSteps returns IDs of all steps reachable via triggers`() {
        val navigator = WorkflowNavigator(workflow)
        assertThat(navigator.reachableSteps()).containsExactly("step-2")
    }

    @Test
    fun `reachableSteps is empty for a step with no triggers`() {
        val navigator = WorkflowNavigator(workflow)
        navigator.triggerAction("btn-next", WorkflowTriggerType.ON_PRESS)
        navigator.triggerAction("btn-finish", WorkflowTriggerType.ON_PRESS)
        assertThat(navigator.reachableSteps()).isEmpty()
    }

    @Test
    fun `reachableSteps excludes non-Step trigger actions`() {
        val stepWithMixedActions = WorkflowStep(
            id = "step-mixed",
            type = "screen",
            screenId = "screen-mixed",
            triggers = listOf(
                WorkflowTrigger(
                    name = "Next",
                    type = WorkflowTriggerType.ON_PRESS,
                    actionId = "action-step",
                    componentId = "btn-step",
                ),
                WorkflowTrigger(
                    name = "Unknown",
                    type = WorkflowTriggerType.ON_PRESS,
                    actionId = "action-unknown",
                    componentId = "btn-unknown",
                ),
            ),
            triggerActions = mapOf(
                "action-step" to WorkflowTriggerAction.Step(stepId = "step-2"),
                "action-unknown" to WorkflowTriggerAction.Unknown,
            ),
        )
        val wfl = workflow.copy(
            initialStepId = "step-mixed",
            steps = mapOf(
                "step-mixed" to stepWithMixedActions,
                "step-2" to step2,
            ),
        )
        val navigator = WorkflowNavigator(wfl)
        assertThat(navigator.reachableSteps()).containsExactly("step-2")
    }

    // endregion

    @Test
    fun `triggerAction with actionId not in triggerActions returns null and does not navigate`() {
        val stepWithMissingAction = WorkflowStep(
            id = "step-x",
            type = "screen",
            screenId = "screen-x",
            triggers = listOf(
                WorkflowTrigger(
                    name = "X",
                    type = WorkflowTriggerType.ON_PRESS,
                    actionId = "action-missing",
                    componentId = "btn-x",
                ),
            ),
            triggerActions = emptyMap(),
        )
        val wfl = workflow.copy(
            initialStepId = "step-x",
            steps = mapOf("step-x" to stepWithMissingAction),
        )
        val navigator = WorkflowNavigator(wfl)
        val result = navigator.triggerAction("btn-x", WorkflowTriggerType.ON_PRESS)
        assertThat(result).isNull()
        assertThat(navigator.currentStep()).isEqualTo(stepWithMissingAction)
    }

    @Test
    fun `triggerAction with target step not in workflow returns null and does not navigate`() {
        val stepWithMissingTarget = WorkflowStep(
            id = "step-x",
            type = "screen",
            screenId = "screen-x",
            triggers = listOf(
                WorkflowTrigger(
                    name = "X",
                    type = WorkflowTriggerType.ON_PRESS,
                    actionId = "ax",
                    componentId = "btn-x",
                ),
            ),
            triggerActions = mapOf(
                "ax" to WorkflowTriggerAction.Step(stepId = "step-nonexistent"),
            ),
        )
        val wfl = workflow.copy(
            initialStepId = "step-x",
            steps = mapOf("step-x" to stepWithMissingTarget),
        )
        val navigator = WorkflowNavigator(wfl)
        val result = navigator.triggerAction("btn-x", WorkflowTriggerType.ON_PRESS)
        assertThat(result).isNull()
        assertThat(navigator.currentStep()).isEqualTo(stepWithMissingTarget)
    }
}
