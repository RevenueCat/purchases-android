package com.revenuecat.purchases.common.events

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.checkpoints.CheckpointHitResult
import com.revenuecat.purchases.checkpoints.CheckpointType
import com.revenuecat.purchases.common.JsonProvider
import kotlinx.serialization.encodeToString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], manifest = Config.NONE)
class CheckpointEventsRequestSerializationTest {

    private val request = requestWith()

    @Test
    fun `encodes checkpoint event to khepri-compatible shape`() {
        val requestString = JsonProvider.defaultJson.encodeToString(request)

        assertThat(requestString).isEqualTo(
            "{" +
                "\"events\":[" +
                    "{" +
                        "\"discriminator\":\"checkpoint\"," +
                        "\"id\":\"498207f4-87af-4b57-a581-eb27bcc6e009\"," +
                        "\"version\":1," +
                        "\"type\":\"checkpoint_hit\"," +
                        "\"identifier\":\"onboarding_complete\"," +
                        "\"checkpoint_type\":\"custom\"," +
                        "\"app_user_id\":\"app_user_id\"," +
                        "\"app_session_id\":\"315107f4-98bf-4b68-a582-eb27bcb6e111\"," +
                        "\"timestamp\":1699270688995," +
                        "\"result\":\"workflow\"," +
                        "\"workflow_id\":\"wf_123\"," +
                        "\"offering_id\":\"offering_id\"," +
                        "\"checkpoint_rule_id\":\"rule_123\"" +
                    "}" +
                "]" +
            "}",
        )
    }

    @Test
    fun `encodes a matched offering without a workflow id`() {
        val requestString = JsonProvider.defaultJson.encodeToString(
            requestWith(result = CheckpointHitResult.OFFERING, workflowID = null),
        )

        assertThat(requestString).contains("\"result\":\"offering\",\"offering_id\":\"offering_id\"")
        assertThat(requestString).contains("\"checkpoint_rule_id\":\"rule_123\"")
        assertThat(requestString).doesNotContain("workflow_id")
    }

    @Test
    fun `encodes each no-action result without workflow or offering ids`() {
        val expectedResultValues = mapOf(
            CheckpointHitResult.NO_MATCH to "no_match",
            CheckpointHitResult.CONFIGURATION_UNAVAILABLE to "configuration_unavailable",
            CheckpointHitResult.UNKNOWN_CHECKPOINT to "unknown_checkpoint",
        )

        expectedResultValues.forEach { (result, expectedValue) ->
            val requestString = JsonProvider.defaultJson.encodeToString(
                requestWith(
                    result = result,
                    workflowID = null,
                    offeringID = null,
                    checkpointRuleID = null,
                ),
            )

            assertThat(requestString).contains("\"result\":\"$expectedValue\"")
            assertThat(requestString).doesNotContain("workflow_id")
            assertThat(requestString).doesNotContain("offering_id")
            assertThat(requestString).doesNotContain("checkpoint_rule_id")
        }
    }

    @Test
    fun `omits the outcome fields when they are absent`() {
        val requestString = JsonProvider.defaultJson.encodeToString(
            requestWith(result = null, workflowID = null, offeringID = null, checkpointRuleID = null),
        )

        assertThat(requestString).endsWith("\"timestamp\":1699270688995}]}")
        assertThat(requestString).contains("\"checkpoint_type\":\"custom\"")
    }

    @Test
    fun `round-trips encode and decode`() {
        val requestString = JsonProvider.defaultJson.encodeToString(request)

        assertThat(JsonProvider.defaultJson.decodeFromString<EventsRequest>(requestString)).isEqualTo(request)
    }

    private fun requestWith(
        result: CheckpointHitResult? = CheckpointHitResult.WORKFLOW,
        workflowID: String? = "wf_123",
        offeringID: String? = "offering_id",
        checkpointRuleID: String? = "rule_123",
    ) = EventsRequest(
        listOf(
            BackendEvent.Checkpoint(
                id = "498207f4-87af-4b57-a581-eb27bcc6e009",
                version = BackendEvent.CHECKPOINT_EVENT_SCHEMA_VERSION,
                type = BackendEvent.CHECKPOINT_EVENT_TYPE,
                identifier = "onboarding_complete",
                checkpointType = CheckpointType.CUSTOM,
                appUserID = "app_user_id",
                appSessionID = "315107f4-98bf-4b68-a582-eb27bcb6e111",
                timestamp = 1699270688995,
                result = result,
                workflowID = workflowID,
                offeringID = offeringID,
                checkpointRuleID = checkpointRuleID,
            ),
        ),
    )
}
