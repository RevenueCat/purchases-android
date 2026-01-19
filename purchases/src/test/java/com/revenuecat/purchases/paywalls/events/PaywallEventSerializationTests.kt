package com.revenuecat.purchases.paywalls.events

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import kotlinx.serialization.encodeToString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PaywallEventSerializationTests {

    private val impressionEvent = PaywallStoredEvent(
        event = PaywallEvent(
            creationData = PaywallEvent.CreationData(
                id = UUID.fromString("298207f4-87af-4b57-a581-eb27bcc6e009"),
                date = Date(1699270688884)
            ),
            data = PaywallEvent.Data(
                paywallIdentifier = "paywallID",
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

    private val exitOfferEvent = PaywallStoredEvent(
        event = PaywallEvent(
            creationData = PaywallEvent.CreationData(
                id = UUID.fromString("398207f4-97af-4b57-a581-eb27bcc6e010"),
                date = Date(1699270688999)
            ),
            data = PaywallEvent.Data(
                paywallIdentifier = "paywallID",
                offeringIdentifier = "offeringID",
                paywallRevision = 3,
                sessionIdentifier = UUID.fromString("415107f4-98bf-4b68-a582-eb27bcb6e222"),
                displayMode = "fullscreen",
                localeIdentifier = "en_US",
                darkMode = false,
                exitOfferType = ExitOfferType.DISMISS,
                exitOfferingIdentifier = "exit-offering-id"
            ),
            type = PaywallEventType.EXIT_OFFER,
        ),
        userID = "testAppUserId",
    )

    @Test
    fun `can encode paywall event correctly`() {
        val eventString = PaywallStoredEvent.json.encodeToString(impressionEvent)
        assertThat(eventString).isEqualTo(
            "{" +
                "\"event\":{" +
                    "\"creationData\":{" +
                        "\"id\":\"298207f4-87af-4b57-a581-eb27bcc6e009\"," +
                        "\"date\":1699270688884" +
                    "}," +
                    "\"data\":{" +
                        "\"paywallIdentifier\":\"paywallID\"," +
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
        val eventString = PaywallStoredEvent.json.encodeToString(impressionEvent)
        val decodedEvent = PaywallStoredEvent.json.decodeFromString<PaywallStoredEvent>(eventString)
        assertThat(decodedEvent).isEqualTo(impressionEvent)
    }

    @Test
    fun `can encode exit offer event correctly`() {
        val eventString = PaywallStoredEvent.json.encodeToString(exitOfferEvent)
        assertThat(eventString).isEqualTo(
            "{" +
                "\"event\":{" +
                    "\"creationData\":{" +
                        "\"id\":\"398207f4-97af-4b57-a581-eb27bcc6e010\"," +
                        "\"date\":1699270688999" +
                    "}," +
                    "\"data\":{" +
                        "\"paywallIdentifier\":\"paywallID\"," +
                        "\"offeringIdentifier\":\"offeringID\"," +
                        "\"paywallRevision\":3," +
                        "\"sessionIdentifier\":\"415107f4-98bf-4b68-a582-eb27bcb6e222\"," +
                        "\"displayMode\":\"fullscreen\"," +
                        "\"localeIdentifier\":\"en_US\"," +
                        "\"darkMode\":false," +
                        "\"exitOfferType\":\"DISMISS\"," +
                        "\"exitOfferingIdentifier\":\"exit-offering-id\"" +
                    "}," +
                    "\"type\":\"EXIT_OFFER\"" +
                "}," +
                "\"userID\":\"testAppUserId\"" +
            "}"
        )
    }

    @Test
    fun `can encode and decode exit offer event correctly`() {
        val eventString = PaywallStoredEvent.json.encodeToString(exitOfferEvent)
        val decodedEvent = PaywallStoredEvent.json.decodeFromString<PaywallStoredEvent>(eventString)
        assertThat(decodedEvent).isEqualTo(exitOfferEvent)
    }
}
