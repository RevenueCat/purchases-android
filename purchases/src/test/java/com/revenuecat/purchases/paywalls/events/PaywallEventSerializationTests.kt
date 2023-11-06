package com.revenuecat.purchases.paywalls.events

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@RunWith(AndroidJUnit4::class)
class PaywallEventSerializationTests {

    private val event = PaywallStoredEvent(
        event = PaywallEvent(
            creationData = PaywallEvent.CreationData(
                id = UUID.fromString("298207f4-87af-4b57-a581-eb27bcc6e009"),
                date = Date(1699270688884)
            ),
            data = PaywallEvent.Data(
                offeringIdentifier = "offeringID",
                paywallRevision = 5,
                sessionIdentifier = UUID.fromString("315107f4-98bf-4b68-a582-eb27bcb6e111"),
                displayMode = "footer",
                localeIdentifier = "es_ES",
                darkMode = true
            ),
            type = PaywallEventType.IMPRESSION,
        ),
        userID = "testAppUserId",
    )

    @Test
    fun `can encode paywall event correctly`() {
        val eventString = PaywallEventsManager.json.encodeToString(event)
        assertThat(eventString).isEqualTo(
            "{" +
                "\"event\":{" +
                    "\"creationData\":{" +
                        "\"id\":\"298207f4-87af-4b57-a581-eb27bcc6e009\"," +
                        "\"date\":1699270688884" +
                    "}," +
                    "\"data\":{" +
                        "\"offeringIdentifier\":\"offeringID\"," +
                        "\"paywallRevision\":5," +
                        "\"sessionIdentifier\":\"315107f4-98bf-4b68-a582-eb27bcb6e111\"," +
                        "\"displayMode\":\"footer\"," +
                        "\"localeIdentifier\":\"es_ES\"," +
                        "\"darkMode\":true" +
                    "}," +
                    "\"type\":\"IMPRESSION\"" +
                "}," +
                "\"userID\":\"testAppUserId\"" +
            "}"
        )
    }

    @Test
    fun `can encode and decode event correctly`() {
        val eventString = PaywallEventsManager.json.encodeToString(event)
        val decodedEvent = PaywallEventsManager.json.decodeFromString<PaywallStoredEvent>(eventString)
        assertThat(decodedEvent).isEqualTo(event)
    }
}
