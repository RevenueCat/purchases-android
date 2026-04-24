@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
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
        assert(actual == expected)
    }
}

@RunWith(Parameterized::class)
internal class WorkflowTriggerActionTypeDeserializationTests(
    private val serialized: String,
    private val expected: WorkflowTriggerActionType,
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
            arrayOf("\"step\"", WorkflowTriggerActionType.STEP),
            arrayOf("\"navigate\"", WorkflowTriggerActionType.UNKNOWN),
            arrayOf("\"some_future_unknown_action_type\"", WorkflowTriggerActionType.UNKNOWN),
        )
    }

    @Test
    fun `Should properly deserialize WorkflowTriggerActionType`() {
        val actual = JsonTools.json.decodeFromString<WorkflowTriggerActionType>(serialized)
        assert(actual == expected)
    }
}
