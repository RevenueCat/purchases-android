@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.JsonProvider

internal object WorkflowJsonParser {

    fun parseWorkflowsListResponse(payload: String): WorkflowsListResponse =
        JsonProvider.defaultJson.decodeFromString<WorkflowsListResponse>(payload)

    fun parsePublishedWorkflow(payload: String): PublishedWorkflow =
        JsonProvider.defaultJson.decodeFromString<PublishedWorkflow>(payload)
}
