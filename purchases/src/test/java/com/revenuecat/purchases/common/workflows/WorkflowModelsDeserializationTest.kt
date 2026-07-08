@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class WorkflowModelsDeserializationTest {

    @Test
    fun `WorkflowStep stepScreenType reads paywall from metadata`() {
        val json = """
            {"id": "step_1", "type": "screen", "metadata": {"screen_type": ["paywall"]}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.stepScreenType).containsExactly("paywall")
    }

    @Test
    fun `WorkflowStep stepScreenType is empty when tagged with empty array`() {
        // A step the backend tagged with no known type. Empty (not null) means "explicitly not a
        // paywall", which suppresses paywall events.
        val json = """
            {"id": "step_1", "type": "screen", "metadata": {"screen_type": []}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.stepScreenType).isEmpty()
    }

    @Test
    fun `WorkflowStep stepScreenType is null when screen_type key absent`() {
        // Older workflows omit screen_type. Null (not empty) preserves the always-report behavior.
        val json = """
            {"id": "step_1", "type": "screen", "metadata": {"other_key": "value"}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.stepScreenType).isNull()
    }

    @Test
    fun `WorkflowStep stepScreenType is null when metadata is null`() {
        val json = """
            {"id": "step_1", "type": "screen", "metadata": null}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.stepScreenType).isNull()
    }

    @Test
    fun `WorkflowStep stepScreenType is null when metadata is absent`() {
        val json = """
            {"id": "step_1", "type": "screen"}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.stepScreenType).isNull()
    }

    @Test
    fun `WorkflowStep stepScreenType ignores non-string entries`() {
        val json = """
            {"id": "step_1", "type": "screen", "metadata": {"screen_type": ["paywall", 1, null]}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.stepScreenType).containsExactly("paywall")
    }

    // A present-but-non-array `screen_type` is treated as untagged (null), matching iOS. The backend
    // only ships `screen_type` as a JSON array; these pin the conservative fallback for malformed shapes.

    @Test
    fun `WorkflowStep stepScreenType is null when screen_type is a scalar`() {
        val json = """
            {"id": "step_1", "type": "screen", "metadata": {"screen_type": "paywall"}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.stepScreenType).isNull()
    }

    @Test
    fun `WorkflowStep stepScreenType is null when screen_type is an object`() {
        val json = """
            {"id": "step_1", "type": "screen", "metadata": {"screen_type": {"value": "paywall"}}}
        """.trimIndent()
        val step = JsonTools.json.decodeFromString(WorkflowStep.serializer(), json)
        assertThat(step.stepScreenType).isNull()
    }
}
