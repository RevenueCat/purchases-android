package com.revenuecat.purchases.paywalls.events

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PresentedOfferingContext
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
                presentedOfferingContext = PresentedOfferingContext(
                    offeringIdentifier = "offeringID",
                    placementIdentifier = "placementID",
                    targetingContext = PresentedOfferingContext.TargetingContext(
                        revision = 5,
                        ruleId = "ruleID",
                    )
                ),
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
                presentedOfferingContext = PresentedOfferingContext("offeringID"),
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
                        "\"presentedOfferingContext\":{" +
                            "\"offeringIdentifier\":\"offeringID\"," +
                            "\"placementIdentifier\":\"placementID\"," +
                            "\"targetingContext\":{\"revision\":5,\"ruleId\":\"ruleID\"}" +
                        "}," +
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
                        "\"presentedOfferingContext\":{" +
                            "\"offeringIdentifier\":\"offeringID\"," +
                            "\"placementIdentifier\":null," +
                            "\"targetingContext\":null" +
                        "}," +
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

    @Test
    fun `can decode old cached event with offeringIdentifier string field`() {
        // Old format with "offeringIdentifier" as a string field instead of "presentedOfferingContext" object
        val oldFormatJson = """
            {
                "event": {
                    "creationData": {
                        "id": "298207f4-87af-4b57-a581-eb27bcc6e009",
                        "date": 1699270688884
                    },
                    "data": {
                        "paywallIdentifier": "paywallID",
                        "offeringIdentifier": "offeringID",
                        "paywallRevision": 5,
                        "sessionIdentifier": "315107f4-98bf-4b68-a582-eb27bcb6e111",
                        "displayMode": "footer",
                        "localeIdentifier": "es_ES",
                        "darkMode": true
                    },
                    "type": "IMPRESSION"
                },
                "userID": "testAppUserId"
            }
        """.trimIndent()

        val decodedEvent = PaywallStoredEvent.json.decodeFromString<PaywallStoredEvent>(oldFormatJson)

        assertThat(decodedEvent.event.data.presentedOfferingContext.offeringIdentifier).isEqualTo("offeringID")
        assertThat(decodedEvent.event.data.presentedOfferingContext.placementIdentifier).isNull()
        assertThat(decodedEvent.event.data.presentedOfferingContext.targetingContext).isNull()
        assertThat(decodedEvent.event.data.paywallIdentifier).isEqualTo("paywallID")
        assertThat(decodedEvent.event.data.paywallRevision).isEqualTo(5)
        assertThat(decodedEvent.event.type).isEqualTo(PaywallEventType.IMPRESSION)
    }

    @Test
    fun `can decode old cached event with offeringIdentifier in exit offer event`() {
        // Old format exit offer event
        val oldFormatJson = """
            {
                "event": {
                    "creationData": {
                        "id": "398207f4-97af-4b57-a581-eb27bcc6e010",
                        "date": 1699270688999
                    },
                    "data": {
                        "paywallIdentifier": "paywallID",
                        "offeringIdentifier": "offeringID",
                        "paywallRevision": 3,
                        "sessionIdentifier": "415107f4-98bf-4b68-a582-eb27bcb6e222",
                        "displayMode": "fullscreen",
                        "localeIdentifier": "en_US",
                        "darkMode": false,
                        "exitOfferType": "DISMISS",
                        "exitOfferingIdentifier": "exit-offering-id"
                    },
                    "type": "EXIT_OFFER"
                },
                "userID": "testAppUserId"
            }
        """.trimIndent()

        val decodedEvent = PaywallStoredEvent.json.decodeFromString<PaywallStoredEvent>(oldFormatJson)

        assertThat(decodedEvent.event.data.presentedOfferingContext.offeringIdentifier).isEqualTo("offeringID")
        assertThat(decodedEvent.event.data.presentedOfferingContext.placementIdentifier).isNull()
        assertThat(decodedEvent.event.data.presentedOfferingContext.targetingContext).isNull()
        assertThat(decodedEvent.event.data.exitOfferType).isEqualTo(ExitOfferType.DISMISS)
        assertThat(decodedEvent.event.data.exitOfferingIdentifier).isEqualTo("exit-offering-id")
    }

    @Test
    fun `serialization always uses new format with presentedOfferingContext`() {
        // Create an event with PresentedOfferingContext
        val event = PaywallStoredEvent(
            event = PaywallEvent(
                creationData = PaywallEvent.CreationData(
                    id = UUID.fromString("298207f4-87af-4b57-a581-eb27bcc6e009"),
                    date = Date(1699270688884)
                ),
                data = PaywallEvent.Data(
                    paywallIdentifier = "paywallID",
                    presentedOfferingContext = PresentedOfferingContext("offeringID"),
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

        val eventString = PaywallStoredEvent.json.encodeToString(event)

        // Verify the serialized JSON uses "presentedOfferingContext" object structure
        assertThat(eventString).contains("\"presentedOfferingContext\":")
        // Verify it doesn't have offeringIdentifier as a direct field in data (old format)
        // by checking the structure has presentedOfferingContext before offeringIdentifier
        assertThat(eventString).contains("\"presentedOfferingContext\":{\"offeringIdentifier\":\"offeringID\"")
    }

    @Test
    fun `old format with purchase error fields can be decoded`() {
        val oldFormatJson = """
            {
                "event": {
                    "creationData": {
                        "id": "498207f4-97af-4b57-a581-eb27bcc6e333",
                        "date": 1699270689000
                    },
                    "data": {
                        "paywallIdentifier": "paywallID",
                        "offeringIdentifier": "offeringID",
                        "paywallRevision": 7,
                        "sessionIdentifier": "515107f4-98bf-4b68-a582-eb27bcb6e333",
                        "displayMode": "fullscreen",
                        "localeIdentifier": "en_US",
                        "darkMode": false,
                        "packageIdentifier": "monthly",
                        "productIdentifier": "com.example.monthly",
                        "errorCode": 123,
                        "errorMessage": "Purchase failed"
                    },
                    "type": "PURCHASE_ERROR"
                },
                "userID": "testAppUserId"
            }
        """.trimIndent()

        val decodedEvent = PaywallStoredEvent.json.decodeFromString<PaywallStoredEvent>(oldFormatJson)

        assertThat(decodedEvent.event.data.presentedOfferingContext.offeringIdentifier).isEqualTo("offeringID")
        assertThat(decodedEvent.event.data.packageIdentifier).isEqualTo("monthly")
        assertThat(decodedEvent.event.data.productIdentifier).isEqualTo("com.example.monthly")
        assertThat(decodedEvent.event.data.errorCode).isEqualTo(123)
        assertThat(decodedEvent.event.data.errorMessage).isEqualTo("Purchase failed")
        assertThat(decodedEvent.event.type).isEqualTo(PaywallEventType.PURCHASE_ERROR)
    }
}
