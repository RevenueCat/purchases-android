package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(InternalRevenueCatAPI::class)
@RunWith(JUnit4::class)
class WorkflowJsonParserTest {

    @Test
    fun `parseWorkflowsListResponse maps workflows and ui_config`() {
        val json = """
            {
              "workflows": [
                { "id": "wf_1", "display_name": "Flow A" }
              ],
              "ui_config": {
                "app": { "colors": {}, "fonts": {} },
                "localizations": {},
                "variable_config": {}
              }
            }
        """.trimIndent()

        val parsed = WorkflowJsonParser.parseWorkflowsListResponse(json)

        assertThat(parsed.workflows).hasSize(1)
        assertThat(parsed.workflows.single().id).isEqualTo("wf_1")
        assertThat(parsed.workflows.single().displayName).isEqualTo("Flow A")
    }

    @Test
    fun `parsePublishedWorkflow maps steps and trigger_actions`() {
        val json = """
            {
              "id": "wf_test",
              "display_name": "Test",
              "initial_step_id": "step_1",
              "steps": {
                "step_1": {
                  "id": "step_1",
                  "type": "screen",
                  "param_values": {},
                  "triggers": [],
                  "outputs": {},
                  "trigger_actions": {
                    "btn_1": { "type": "step", "value": "step_2" }
                  },
                  "metadata": null
                }
              },
              "screens": {},
              "ui_config": {
                "app": { "colors": {}, "fonts": {} },
                "localizations": {},
                "variable_config": {}
              },
              "content_max_width": 100,
              "metadata": {}
            }
        """.trimIndent()

        val parsed = WorkflowJsonParser.parsePublishedWorkflow(json)

        assertThat(parsed.id).isEqualTo("wf_test")
        assertThat(parsed.initialStepId).isEqualTo("step_1")
        assertThat(parsed.steps["step_1"]?.triggerActions?.get("btn_1")?.resolvedTargetStepId).isEqualTo("step_2")
        assertThat(parsed.contentMaxWidth).isEqualTo(100)
    }
}
