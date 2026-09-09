@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.checkpoints

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class CheckpointEventTest {

    private val identifier = "onboarding_complete"
    private val timestamp = Date(1699270688995)

    @Test
    fun `a matched workflow reports present_ui with the workflow and rule but no offering`() {
        val event = matchedWorkflow(checkpointRuleId = "rule_wf1234").toEvent()

        assertThat(event.identifier).isEqualTo(identifier)
        assertThat(event.checkpointType).isEqualTo(CheckpointType.CUSTOM)
        assertThat(event.result).isEqualTo(CheckpointHitResult.PRESENT_UI)
        assertThat(event.workflowId).isEqualTo("wf1234")
        assertThat(event.offeringId).isNull()
        assertThat(event.checkpointRuleId).isEqualTo("rule_wf1234")
        assertThat(event.timestamp).isEqualTo(timestamp)
    }

    @Test
    fun `a matched offering reports return_data without a workflow id`() {
        val event = matchedOffering(checkpointRuleId = "rule_wf1234").toEvent()

        assertThat(event.result).isEqualTo(CheckpointHitResult.RETURN_DATA)
        assertThat(event.workflowId).isNull()
        assertThat(event.offeringId).isEqualTo("default")
        assertThat(event.checkpointRuleId).isEqualTo("rule_wf1234")
    }

    @Test
    fun `a match reports no rule id when the rules topic omits it`() {
        assertThat(matchedWorkflow(checkpointRuleId = null).toEvent().checkpointRuleId).isNull()
        assertThat(matchedOffering(checkpointRuleId = null).toEvent().checkpointRuleId).isNull()
    }

    @Test
    fun `each no-action reason maps to its result and carries no workflow, offering or rule`() {
        val expectedResults = mapOf(
            CheckpointResolution.NoAction.Reason.NO_MATCH to CheckpointHitResult.NO_MATCH,
            CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE to
                CheckpointHitResult.CONFIGURATION_UNAVAILABLE,
            CheckpointResolution.NoAction.Reason.UNKNOWN_CHECKPOINT to CheckpointHitResult.UNKNOWN_CHECKPOINT,
        )

        expectedResults.forEach { (reason, expectedResult) ->
            val event = CheckpointResolution.NoAction(reason).toEvent()

            assertThat(event.result).isEqualTo(expectedResult)
            assertThat(event.checkpointType).isEqualTo(CheckpointType.CUSTOM)
            assertThat(event.workflowId).isNull()
            assertThat(event.offeringId).isNull()
            assertThat(event.checkpointRuleId).isNull()
        }
    }

    private fun CheckpointResolution.toEvent(): CheckpointEvent =
        toCheckpointEvent(identifier = identifier, timestamp = timestamp)

    private fun matchedWorkflow(checkpointRuleId: String?) = CheckpointResolution.MatchedWorkflow(
        workflow = workflow("wf1234"),
        uiConfig = mockk<UiConfig>(),
        offerings = mockk(),
        checkpointRuleId = checkpointRuleId,
    )

    private fun matchedOffering(checkpointRuleId: String?) = CheckpointResolution.MatchedOffering(
        offering = offering("default"),
        checkpointRuleId = checkpointRuleId,
    )

    private fun workflow(id: String) = mockk<PublishedWorkflow> {
        every { this@mockk.id } returns id
    }

    private fun offering(identifier: String) = mockk<Offering> {
        every { this@mockk.identifier } returns identifier
    }
}
