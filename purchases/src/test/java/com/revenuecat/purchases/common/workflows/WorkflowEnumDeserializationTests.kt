@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class WorkflowTriggerTypeDeserializationTests(
    private val serialized: String,
    private val expected: WorkflowTriggerType,
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
            arrayOf("\"on_press\"", WorkflowTriggerType.ON_PRESS),
            arrayOf("\"on_long_press\"", WorkflowTriggerType.UNKNOWN),
            arrayOf("\"some_future_unknown_trigger_type\"", WorkflowTriggerType.UNKNOWN),
        )
    }

    @Test
    fun `Should properly deserialize WorkflowTriggerType`() {
        val actual = JsonTools.json.decodeFromString<WorkflowTriggerType>(serialized)
        assertThat(actual).isEqualTo(expected)
    }
}

@RunWith(Parameterized::class)
internal class WorkflowTriggerActionDeserializationTests(
    private val serialized: String,
    private val expected: WorkflowTriggerAction,
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
            arrayOf("""{"type":"step","step_id":"s1"}""", WorkflowTriggerAction.Step(stepId = "s1")),
            arrayOf("""{"type":"step"}""", WorkflowTriggerAction.Unknown),
            arrayOf("""{"type":"navigate"}""", WorkflowTriggerAction.Unknown),
            arrayOf("""{"type":"some_future_unknown_action_type"}""", WorkflowTriggerAction.Unknown),
        )
    }

    @Test
    fun `Should properly deserialize WorkflowTriggerAction`() {
        val actual = JsonTools.json.decodeFromString<WorkflowTriggerAction>(serialized)
        assertThat(actual).isEqualTo(expected)
    }
}
