package com.revenuecat.purchases.paywalls.events

import com.revenuecat.purchases.common.events.BackendEvent
import com.revenuecat.purchases.common.events.BackendStoredEvent
import com.revenuecat.purchases.common.events.toBackendStoredEvent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.Date
import java.util.UUID

class CustomPaywallEventTest {

    private val appUserID = "test-user-123"
    private val appSessionID = "session-abc-456"
    private val fixedId = UUID.fromString("298207f4-87af-4b57-a581-eb27bcc6e009")
    private val fixedDate = Date(1699270688884)

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        serializersModule = SerializersModule {
            polymorphic(BackendStoredEvent::class) {
                subclass(BackendStoredEvent.CustomerCenter::class, BackendStoredEvent.CustomerCenter.serializer())
                subclass(BackendStoredEvent.Paywalls::class, BackendStoredEvent.Paywalls.serializer())
                subclass(BackendStoredEvent.Ad::class, BackendStoredEvent.Ad.serializer())
                subclass(
                    BackendStoredEvent.CustomPaywall::class,
                    BackendStoredEvent.CustomPaywall.serializer(),
                )
            }
        }
        explicitNulls = false
    }

    @Test
    fun `CustomPaywallEvent Impression is created with paywallId`() {
        val event = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallEvent.Impression.Data(paywallId = "my-paywall"),
        )

        assertThat(event.creationData.id).isEqualTo(fixedId)
        assertThat(event.creationData.date).isEqualTo(fixedDate)
        assertThat(event.data.paywallId).isEqualTo("my-paywall")
    }

    @Test
    fun `CustomPaywallEvent Impression is created with null paywallId`() {
        val event = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallEvent.Impression.Data(paywallId = null),
        )

        assertThat(event.data.paywallId).isNull()
    }

    @Test
    fun `toBackendStoredEvent converts event with paywallId correctly`() {
        val event = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallEvent.Impression.Data(paywallId = "my-paywall"),
        )

        val storedEvent = event.toBackendStoredEvent(appUserID, appSessionID)

        val expectedStoredEvent = BackendStoredEvent.CustomPaywall(
            BackendEvent.CustomPaywall(
                id = fixedId.toString(),
                version = BackendEvent.CUSTOM_PAYWALL_EVENT_SCHEMA_VERSION,
                type = "custom_paywall_impression",
                appUserID = appUserID,
                appSessionID = appSessionID,
                timestamp = fixedDate.time,
                paywallID = "my-paywall",
            ),
        )
        assertThat(storedEvent).isEqualTo(expectedStoredEvent)
    }

    @Test
    fun `toBackendStoredEvent converts event with null paywallId correctly`() {
        val event = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallEvent.Impression.Data(paywallId = null),
        )

        val storedEvent = event.toBackendStoredEvent(appUserID, appSessionID)

        val expectedStoredEvent = BackendStoredEvent.CustomPaywall(
            BackendEvent.CustomPaywall(
                id = fixedId.toString(),
                version = BackendEvent.CUSTOM_PAYWALL_EVENT_SCHEMA_VERSION,
                type = "custom_paywall_impression",
                appUserID = appUserID,
                appSessionID = appSessionID,
                timestamp = fixedDate.time,
                paywallID = null,
            ),
        )
        assertThat(storedEvent).isEqualTo(expectedStoredEvent)
    }

    @Test
    fun `BackendStoredEvent CustomPaywall JSON roundtrip`() {
        val event = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallEvent.Impression.Data(paywallId = "my-paywall"),
        )
        val storedEvent = event.toBackendStoredEvent(appUserID, appSessionID)

        val jsonString = json.encodeToString(BackendStoredEvent.serializer(), storedEvent)
        val decoded = json.decodeFromString(BackendStoredEvent.serializer(), jsonString)

        assertThat(decoded).isInstanceOf(BackendStoredEvent.CustomPaywall::class.java)
        val decodedEvent = (decoded as BackendStoredEvent.CustomPaywall).event
        assertThat(decodedEvent.id).isEqualTo(fixedId.toString())
        assertThat(decodedEvent.version).isEqualTo(BackendEvent.CUSTOM_PAYWALL_EVENT_SCHEMA_VERSION)
        assertThat(decodedEvent.type).isEqualTo("custom_paywall_impression")
        assertThat(decodedEvent.appUserID).isEqualTo(appUserID)
        assertThat(decodedEvent.appSessionID).isEqualTo(appSessionID)
        assertThat(decodedEvent.timestamp).isEqualTo(fixedDate.time)
        assertThat(decodedEvent.paywallID).isEqualTo("my-paywall")
    }

    @Test
    fun `BackendStoredEvent CustomPaywall JSON roundtrip with null paywallId`() {
        val event = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallEvent.Impression.Data(paywallId = null),
        )
        val storedEvent = event.toBackendStoredEvent(appUserID, appSessionID)

        val jsonString = json.encodeToString(BackendStoredEvent.serializer(), storedEvent)
        val decoded = json.decodeFromString(BackendStoredEvent.serializer(), jsonString)

        assertThat(decoded).isInstanceOf(BackendStoredEvent.CustomPaywall::class.java)
        val decodedEvent = (decoded as BackendStoredEvent.CustomPaywall).event
        assertThat(decodedEvent.paywallID).isNull()
    }

    @Test
    fun `version is 1 and type is custom_paywall_impression`() {
        val event = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallEvent.Impression.Data(paywallId = "test"),
        )
        val storedEvent = event.toBackendStoredEvent(appUserID, appSessionID) as BackendStoredEvent.CustomPaywall

        assertThat(storedEvent.event.version).isEqualTo(1)
        assertThat(storedEvent.event.type).isEqualTo("custom_paywall_impression")
    }

    @Test
    fun `CustomPaywallEventParams default has null paywallId`() {
        val params = CustomPaywallEventParams()
        assertThat(params.paywallId).isNull()
    }

    @Test
    fun `CustomPaywallEventParams with paywallId`() {
        val params = CustomPaywallEventParams(paywallId = "my-paywall")
        assertThat(params.paywallId).isEqualTo("my-paywall")
    }

    @Test
    fun `toBackendStoredEvent includes appSessionID in JSON`() {
        val event = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallEvent.Impression.Data(paywallId = "my-paywall"),
        )
        val storedEvent = event.toBackendStoredEvent(appUserID, appSessionID)

        val jsonString = json.encodeToString(BackendStoredEvent.serializer(), storedEvent)

        assertThat(jsonString).contains("\"app_session_id\":\"$appSessionID\"")
    }
}
