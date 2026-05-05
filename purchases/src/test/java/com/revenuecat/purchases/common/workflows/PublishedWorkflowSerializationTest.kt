package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(InternalRevenueCatAPI::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class PublishedWorkflowSerializationTest {

    private val minimalUiConfigJson = """
        "ui_config": {
          "app": { "colors": {}, "fonts": {} },
          "localizations": {},
          "variable_config": {}
        }
    """.trimIndent()

    private fun minimalJson(extra: String = "") = """
        {
          "id": "wf_1",
          "display_name": "Test",
          "initial_step_id": "step_1",
          "steps": { "step_1": { "id": "step_1", "type": "screen" } },
          "screens": {},
          $minimalUiConfigJson
          ${if (extra.isNotEmpty()) ", $extra" else ""}
        }
    """.trimIndent()

    @Test
    fun `single_step_fallback_id is parsed when present`() {
        val workflow = WorkflowJsonParser.parsePublishedWorkflow(
            minimalJson(extra = """"single_step_fallback_id": "step_1""""),
        )
        assertThat(workflow.singleStepFallbackId).isEqualTo("step_1")
    }

    @Test
    fun `single_step_fallback_id defaults to null when absent`() {
        val workflow = WorkflowJsonParser.parsePublishedWorkflow(minimalJson())
        assertThat(workflow.singleStepFallbackId).isNull()
    }
}
