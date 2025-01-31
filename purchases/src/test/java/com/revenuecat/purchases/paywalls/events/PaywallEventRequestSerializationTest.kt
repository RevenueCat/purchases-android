package com.revenuecat.purchases.paywalls.events

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.events.BackendEvent
import com.revenuecat.purchases.common.events.EventRequest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@RunWith(AndroidJUnit4::class)
class PaywallEventRequestSerializationTest {

    private val request = EventRequest(listOf(
        BackendEvent.Paywalls(
            PaywallBackendEvent(
                id = "id",
                version = 1,
                type = PaywallEventType.CANCEL.value,
                appUserID = "appUserID",
                sessionID = "sessionID",
                offeringID = "offeringID",
                paywallRevision = 5,
                timestamp = 123456789,
                displayMode = "footer",
                darkMode = true,
                localeIdentifier = "en_US",
            )
        )
    ))

    @Test
    fun `can encode paywall event request correctly`() {
        val requestString = EventRequest.json.encodeToString(request)
        Assertions.assertThat(requestString).isEqualTo(
            "{" +
                "\"events\":[" +
                    "{" +
                        "\"id\":\"id\"," +
                        "\"version\":1," +
                        "\"type\":\"paywall_cancel\"," +
                        "\"app_user_id\":\"appUserID\"," +
                        "\"session_id\":\"sessionID\"," +
                        "\"offering_id\":\"offeringID\"," +
                        "\"paywall_revision\":5," +
                        "\"timestamp\":123456789," +
                        "\"display_mode\":\"footer\"," +
                        "\"dark_mode\":true," +
                        "\"locale\":\"en_US\"" +
                    "}" +
                "]" +
            "}"
        )
    }

    @Test
    fun `can encode and decode event correctly`() {
        val requestString = EventRequest.json.encodeToString(request)
        val decodedRequest = EventRequest.json.decodeFromString<EventRequest>(requestString)
        Assertions.assertThat(decodedRequest).isEqualTo(request)
    }
}
