@file:OptIn(com.revenuecat.purchases.InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import kotlinx.serialization.json.Json

internal object WorkflowJsonParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parseWorkflowsListResponse(payload: String): WorkflowsListResponse {
        return json.decodeFromString(payload)
    }

    fun parsePublishedWorkflow(payload: String): PublishedWorkflow {
        return json.decodeFromString(payload)
    }
}
