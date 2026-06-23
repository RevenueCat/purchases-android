package com.revenuecat.purchases.common.caching

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class WorkflowMetadata(
    @SerialName("workflow_id")
    val workflowId: String,
    @SerialName("step_id")
    val stepId: String,
) {
    companion object {
        fun from(workflowId: String?, stepId: String?): WorkflowMetadata? =
            if (workflowId != null && stepId != null) WorkflowMetadata(workflowId, stepId) else null
    }
}
