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
    fun `parsePublishedWorkflow maps steps, triggers, and trigger_actions`() {
        val json = """
            {
              "id": "wf_test",
              "display_name": "Test",
              "initial_step_id": "step_1",
              "steps": {
                "step_1": {
                  "id": "step_1",
                  "type": "screen",
                  "screen_id": "pw458e23295b7841f8",
                  "param_values": {
                    "experiment_id": "expeae100d588",
                    "experiment_variant": "b",
                    "is_last_variant_step": true
                  },
                  "triggers": [
                    {
                      "name": "Button",
                      "type": "on_press",
                      "action_id": "btn_wagcLsIVjN",
                      "component_id": "wagcLsIVjN"
                    }
                  ],
                  "outputs": {},
                  "trigger_actions": {
                    "btn_wagcLsIVjN": { "type": "step", "step_id": "step_2" }
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
        assertThat(parsed.contentMaxWidth).isEqualTo(100)

        val step = parsed.steps["step_1"]!!
        assertThat(step.screenId).isEqualTo("pw458e23295b7841f8")
        assertThat(step.triggers).hasSize(1)
        assertThat(step.triggers[0].name).isEqualTo("Button")
        assertThat(step.triggers[0].type).isEqualTo("on_press")
        assertThat(step.triggers[0].actionId).isEqualTo("btn_wagcLsIVjN")
        assertThat(step.triggers[0].componentId).isEqualTo("wagcLsIVjN")
        assertThat(step.triggerActions["btn_wagcLsIVjN"]?.resolvedTargetStepId).isEqualTo("step_2")
    }
}
