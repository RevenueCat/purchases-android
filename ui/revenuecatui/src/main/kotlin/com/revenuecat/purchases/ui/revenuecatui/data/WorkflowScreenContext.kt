package com.revenuecat.purchases.ui.revenuecatui.data

/** Each step's state carries its own, so a component reads the step it belongs to. */
internal data class WorkflowScreenContext(
    val workflowId: String,
    val stepId: String,
    val stepType: String,
    /** `null` when the backend did not tag the step; see `WorkflowStep.stepScreenType`. */
    val screenType: List<String>?,
    /**
     * `false` when neither the step nor its screen declares an offering; the state's offering is then a
     * package-less placeholder.
     */
    val hasOffering: Boolean = true,
)
