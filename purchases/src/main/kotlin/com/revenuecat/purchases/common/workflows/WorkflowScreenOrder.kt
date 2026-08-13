@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * This workflow's screens in the order a customer is likely to reach them: the initial step's screen, then
 * the screens of the steps it can navigate to, breadth first. Warming is bounded and gets cut short by a
 * paywall opening or a memory trim, so the order decides which pages are warm when that happens.
 *
 * Screens no step reaches come last rather than being dropped: [WorkflowTriggerAction] has an `Unknown`
 * variant and [WorkflowStep.type] is an open string, so a navigation this SDK version cannot read would
 * otherwise silently stop warming a page that really does get shown.
 *
 * Rooted at [PublishedWorkflow.singleStepFallbackId] as well as [PublishedWorkflow.initialStepId], since
 * nothing guarantees the fallback step is reachable through triggers.
 */
internal fun PublishedWorkflow.screensInWarmOrder(): List<WorkflowScreen> {
    val screenIds = linkedSetOf<String>()
    val seenSteps = mutableSetOf<String>()
    val frontier = ArrayDeque<String>()
    listOfNotNull(initialStepId, singleStepFallbackId).forEach { stepId ->
        if (seenSteps.add(stepId)) frontier.addLast(stepId)
    }
    while (frontier.isNotEmpty()) {
        val step = steps[frontier.removeFirst()] ?: continue
        step.screenId?.let(screenIds::add)
        step.nextStepIds().forEach { stepId -> if (seenSteps.add(stepId)) frontier.addLast(stepId) }
    }
    // Already-added ids keep their position, so this appends only the screens the walk never reached.
    screenIds.addAll(screens.keys)
    return screenIds.mapNotNull { screenId -> screens[screenId] }
}

/**
 * The steps this one can navigate to, in the order its triggers are declared, which is the order the
 * components firing them appear. An action no trigger references still counts, so an entry point this
 * SDK version does not model cannot hide a step.
 */
private fun WorkflowStep.nextStepIds(): List<String> {
    val triggeredActionIds = triggers.map { trigger -> trigger.actionId }
    val actionIds = triggeredActionIds + triggerActions.keys.filterNot { it in triggeredActionIds }
    return actionIds.mapNotNull { actionId ->
        (triggerActions[actionId] as? WorkflowTriggerAction.Step)?.stepId
    }
}
