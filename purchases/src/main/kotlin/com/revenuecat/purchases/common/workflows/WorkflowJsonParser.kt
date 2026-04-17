@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools

internal object WorkflowJsonParser {

    fun parsePublishedWorkflow(payload: String): PublishedWorkflow =
        JsonTools.json.decodeFromString<PublishedWorkflow>(payload)

    fun parseWorkflowDetailResponse(payload: String): WorkflowDetailResponse =
        JsonTools.json.decodeFromString<WorkflowDetailResponse>(payload)
}
