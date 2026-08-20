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
class CheckpointEventsRequestSerializationTest {

    private val request = EventsRequest(
        listOf(
            BackendEvent.Checkpoint(
                id = "498207f4-87af-4b57-a581-eb27bcc6e009",
                version = BackendEvent.CHECKPOINT_EVENT_SCHEMA_VERSION,
                type = BackendEvent.CHECKPOINT_EVENT_TYPE,
                identifier = "onboarding_complete",
                appUserID = "app_user_id",
                appSessionID = "315107f4-98bf-4b68-a582-eb27bcb6e111",
                timestamp = 1699270688995,
            ),
        ),
    )

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
                        "\"app_user_id\":\"app_user_id\"," +
                        "\"app_session_id\":\"315107f4-98bf-4b68-a582-eb27bcb6e111\"," +
                        "\"timestamp\":1699270688995" +
                    "}" +
                "]" +
            "}",
        )
    }

    @Test
    fun `round-trips encode and decode`() {
        val requestString = JsonProvider.defaultJson.encodeToString(request)

        assertThat(JsonProvider.defaultJson.decodeFromString<EventsRequest>(requestString)).isEqualTo(request)
    }
}
