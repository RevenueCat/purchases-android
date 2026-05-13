package com.revenuecat.purchases.common.events

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.JsonProvider
import kotlinx.serialization.encodeToString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], manifest = Config.NONE)
class WorkflowEventsRequestSerializationTest {

    private val request = EventsRequest(
        listOf(
            BackendEvent.Workflows(
                id = "evt_id",
                eventName = "workflows_step_started",
                timestampMs = 123456789L,
                appUserID = "appUserID",
                context = BackendEvent.Workflows.Context(locale = "en_US"),
                properties = BackendEvent.Workflows.Properties(
                    workflowId = "wfl_abc",
                    stepId = "step-1",
                    traceId = "trace-1",
                    entryReason = "start",
                    isFirstStep = true,
                    isLastStep = false,
                ),
            ),
        ),
    )

    @Test
    fun `encodes workflows event to khepri-compatible shape`() {
        val requestString = JsonProvider.defaultJson.encodeToString(request)
        assertThat(requestString).isEqualTo(
            "{" +
                "\"events\":[" +
                    "{" +
                        "\"discriminator\":\"workflows\"," +
                        "\"id\":\"evt_id\"," +
                        "\"event_name\":\"workflows_step_started\"," +
                        "\"timestamp_ms\":123456789," +
                        "\"app_user_id\":\"appUserID\"," +
                        "\"context\":{\"locale\":\"en_US\"}," +
                        "\"properties\":{" +
                            "\"workflow_id\":\"wfl_abc\"," +
                            "\"step_id\":\"step-1\"," +
                            "\"trace_id\":\"trace-1\"," +
                            "\"entry_reason\":\"start\"," +
                            "\"is_first_step\":true," +
                            "\"is_last_step\":false" +
                        "}" +
                    "}" +
                "]" +
            "}"
        )
    }

    @Test
    fun `round-trips encode and decode`() {
        val requestString = JsonProvider.defaultJson.encodeToString(request)
        val decoded = JsonProvider.defaultJson.decodeFromString<EventsRequest>(requestString)
        assertThat(decoded).isEqualTo(request)
    }
}
