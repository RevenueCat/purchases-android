package com.revenuecat.purchases.ui.revenuecatui.workflow

import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.WorkflowStep
import com.revenuecat.purchases.common.workflows.WorkflowTriggerAction
import com.revenuecat.purchases.common.workflows.WorkflowTriggerType
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class WorkflowNavigator(private val workflow: PublishedWorkflow) {

    private val _currentStepId = MutableStateFlow(workflow.initialStepId)
    val currentStepId: StateFlow<String> = _currentStepId.asStateFlow()

    private val backStack = ArrayDeque<String>()

    val currentStep: WorkflowStep?
        get() = workflow.steps[_currentStepId.value]

    @Suppress("ReturnCount")
    fun peekTriggerStep(componentId: String, triggerType: WorkflowTriggerType): WorkflowStep? {
        val step = currentStep ?: return null
        val trigger = step.triggers.firstOrNull { it.componentId == componentId && it.type == triggerType }
            ?: return null
        val action = step.triggerActions[trigger.actionId] ?: return null
        if (action !is WorkflowTriggerAction.Step) return null
        return workflow.steps[action.stepId]
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
        if (action !is WorkflowTriggerAction.Step) {
            Logger.w("Unknown workflow trigger action type for actionId '${trigger.actionId}' — ignoring")
            return null
        }
        val stepId = action.stepId
        val nextStep = workflow.steps[stepId] ?: run {
            Logger.w("Step '$stepId' not found in workflow '${workflow.id}'")
            return null
        }
        backStack.addLast(_currentStepId.value)
        _currentStepId.value = stepId
        return nextStep
    }

    fun navigateBack(): WorkflowStep? {
        if (backStack.isEmpty()) return null
        val prevStepId = backStack.removeLast()
        _currentStepId.value = prevStepId
        return workflow.steps[prevStepId]
    }

    val canNavigateBack: Boolean
        get() = backStack.isNotEmpty()
}
