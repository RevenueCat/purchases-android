package com.revenuecat.purchases.common.workflows

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WorkflowModelsDeserializationTest {

    @Test
    fun `WorkflowSummary deserializes with all fields`() {
        val json = """
            {"workflows": [{"id": "wf_1", "display_name": "Flow A", "offering_id": "default", "prefetch": true}]}
        """.trimIndent()
        val result = WorkflowJsonParser.parseWorkflowsListResponse(json)
        assertThat(result.workflows).hasSize(1)
        with(result.workflows[0]) {
            assertThat(id).isEqualTo("wf_1")
            assertThat(displayName).isEqualTo("Flow A")
            assertThat(offeringId).isEqualTo("default")
            assertThat(prefetch).isTrue()
        }
    }

    @Test
    fun `WorkflowSummary deserializes with null offering_id when field is absent`() {
        val json = """{"workflows": [{"id": "wf_2", "display_name": "Flow B", "prefetch": true}]}"""
        val result = WorkflowJsonParser.parseWorkflowsListResponse(json)
        assertThat(result.workflows[0].offeringId).isNull()
    }

    @Test
    fun `WorkflowSummary deserializes with explicit null offering_id`() {
        val json = """{"workflows": [{"id": "wf_3", "display_name": "Flow C", "offering_id": null, "prefetch": false}]}"""
        val result = WorkflowJsonParser.parseWorkflowsListResponse(json)
        assertThat(result.workflows[0].offeringId).isNull()
    }

    @Test
    fun `WorkflowSummary defaults prefetch to false when field is absent`() {
        val json = """{"workflows": [{"id": "wf_4", "display_name": "Flow D"}]}"""
        val result = WorkflowJsonParser.parseWorkflowsListResponse(json)
        assertThat(result.workflows[0].prefetch).isFalse()
    }

    @Test
    fun `WorkflowsListResponse deserializes empty workflows array`() {
        val json = """{"workflows": []}"""
        val result = WorkflowJsonParser.parseWorkflowsListResponse(json)
        assertThat(result.workflows).isEmpty()
    }

    @Test
    fun `WorkflowsListResponse tolerates unknown top-level fields`() {
        val json = """{"workflows": [], "ui_config": {}, "extra": "ignored"}"""
        val result = WorkflowJsonParser.parseWorkflowsListResponse(json)
        assertThat(result.workflows).isEmpty()
    }

    @Test
    fun `WorkflowSummary tolerates unknown fields`() {
        val json = """{"workflows": [{"id": "wf_5", "display_name": "Flow E", "unknown_field": "value"}]}"""
        val result = WorkflowJsonParser.parseWorkflowsListResponse(json)
        assertThat(result.workflows[0].id).isEqualTo("wf_5")
    }
}
