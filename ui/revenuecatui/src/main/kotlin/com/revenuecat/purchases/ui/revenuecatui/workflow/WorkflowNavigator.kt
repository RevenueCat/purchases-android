package com.revenuecat.purchases.ui.revenuecatui.workflow

import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.WorkflowActionID
import com.revenuecat.purchases.common.workflows.WorkflowStep
import com.revenuecat.purchases.common.workflows.WorkflowStepID
import com.revenuecat.purchases.common.workflows.WorkflowTriggerAction
import com.revenuecat.purchases.common.workflows.WorkflowTriggerType
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
internal class WorkflowNavigator(private val workflow: PublishedWorkflow) {

    private var currentStepBranches = emptyMap<WorkflowActionID, WorkflowStepID>()

    fun recordResolvedBranches(resolved: Map<WorkflowActionID, WorkflowStepID>, forStepId: String) {
        if (forStepId != currentStepId) return
        currentStepBranches = resolved
    }

    private var currentStepId: String = workflow.initialStepId

    private val backStack = ArrayDeque<String>()
    val backStackSnapshot: List<String> get() = backStack.toList()

    val currentStep: WorkflowStep? get() = workflow.steps[currentStepId]

    @Suppress("ReturnCount")
    fun peekTriggerStep(componentId: String, triggerType: WorkflowTriggerType): WorkflowStep? {
        val step = currentStep ?: return null
        val trigger = step.triggers.firstOrNull { it.componentId == componentId && it.type == triggerType }
            ?: return null
        val stepId = nextStepId(step.triggerActions[trigger.actionId], trigger.actionId) ?: return null
        return workflow.steps[stepId]
    }

    val peekBackStep: WorkflowStep?
        get() = backStack.lastOrNull()?.let { workflow.steps[it] }

    @Suppress("ReturnCount")
    fun triggerAction(componentId: String, triggerType: WorkflowTriggerType): WorkflowStep? {
        val step = currentStep ?: return null
        val trigger = step.triggers.firstOrNull { it.componentId == componentId && it.type == triggerType } ?: run {
            Logger.w("No trigger found for componentId '$componentId' and type '$triggerType' in step '${step.id}'")
            return null
        }
        val action = step.triggerActions[trigger.actionId] ?: run {
            Logger.w("No trigger action found for actionId '${trigger.actionId}' in step '${step.id}'")
            return null
        }
        val stepId = nextStepId(action, trigger.actionId) ?: run {
            Logger.w("Workflow trigger action '${trigger.actionId}' leads nowhere, ignoring")
            return null
        }
        val nextStep = workflow.steps[stepId] ?: run {
            Logger.w("Step '$stepId' not found in workflow '${workflow.id}'")
            return null
        }
        backStack.addLast(currentStepId)
        currentStepBranches = emptyMap()
        currentStepId = stepId
        return nextStep
    }

    fun navigateBack(): WorkflowStep? {
        if (backStack.isEmpty()) return null
        val prevStepId = backStack.removeLast()
        currentStepBranches = emptyMap()
        currentStepId = prevStepId
        return workflow.steps[prevStepId]
    }

    val canNavigateBack: Boolean
        get() = backStack.isNotEmpty()

    /** If the branch has not been resolved, pick the fallback. */
    private fun nextStepId(action: WorkflowTriggerAction?, actionId: String): String? = when (action) {
        is WorkflowTriggerAction.Step -> action.stepId
        is WorkflowTriggerAction.Branch ->
            currentStepBranches[actionId]?.takeIf { workflow.steps.containsKey(it) } ?: action.fallbackStepId
        WorkflowTriggerAction.Unknown, null -> null
    }
}
