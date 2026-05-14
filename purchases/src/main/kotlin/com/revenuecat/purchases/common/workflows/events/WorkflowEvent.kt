package com.revenuecat.purchases.common.workflows.events

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.events.FeatureEvent
import com.revenuecat.purchases.utils.serializers.DateSerializer
import com.revenuecat.purchases.utils.serializers.UUIDSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date
import java.util.UUID

/**
 * Workflow lifecycle events. Sibling to PaywallEvent. Internal-only.
 */
@InternalRevenueCatAPI
@Serializable
public sealed class WorkflowEvent : FeatureEvent {

    public abstract val creationData: CreationData
    public abstract val workflowId: String
    public abstract val stepId: String
    public abstract val workflowType: String?
    public abstract val stepType: String?
    public abstract val screenType: List<String>

    override val isPriorityEvent: Boolean get() = false

    @Serializable
    public data class CreationData(
        @Serializable(with = UUIDSerializer::class) public val id: UUID,
        @Serializable(with = DateSerializer::class) public val date: Date,
    )

    @Serializable
    @SerialName("workflow_step_started")
    public data class StepStarted(
        override val creationData: CreationData,
        override val workflowId: String,
        override val stepId: String,
        override val workflowType: String? = null,
        override val stepType: String? = null,
        override val screenType: List<String> = emptyList(),
        public val fromStepId: String? = null,
        public val entryReason: String? = null,
        public val isFirstStep: Boolean? = null,
        public val isLastStep: Boolean? = null,
    ) : WorkflowEvent()

    @Serializable
    @SerialName("workflow_step_completed")
    public data class StepCompleted(
        override val creationData: CreationData,
        override val workflowId: String,
        override val stepId: String,
        override val workflowType: String? = null,
        override val stepType: String? = null,
        override val screenType: List<String> = emptyList(),
        public val toStepId: String? = null,
        public val isFirstStep: Boolean? = null,
        public val isLastStep: Boolean? = null,
    ) : WorkflowEvent()
}
