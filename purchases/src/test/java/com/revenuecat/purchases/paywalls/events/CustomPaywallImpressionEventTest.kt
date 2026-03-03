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

class CustomPaywallImpressionEventTest {

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
                    BackendStoredEvent.CustomPaywallImpression::class,
                    BackendStoredEvent.CustomPaywallImpression.serializer(),
                )
            }
        }
        explicitNulls = false
    }

    @Test
    fun `CustomPaywallImpressionEvent is created with paywallId`() {
        val event = CustomPaywallImpressionEvent(
            creationData = CustomPaywallImpressionEvent.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallImpressionEvent.Data(paywallId = "my-paywall"),
        )

        assertThat(event.creationData.id).isEqualTo(fixedId)
        assertThat(event.creationData.date).isEqualTo(fixedDate)
        assertThat(event.data.paywallId).isEqualTo("my-paywall")
    }

    @Test
    fun `CustomPaywallImpressionEvent is created with null paywallId`() {
        val event = CustomPaywallImpressionEvent(
            creationData = CustomPaywallImpressionEvent.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallImpressionEvent.Data(paywallId = null),
        )

        assertThat(event.data.paywallId).isNull()
    }

    @Test
    fun `toBackendStoredEvent converts event with paywallId correctly`() {
        val event = CustomPaywallImpressionEvent(
            creationData = CustomPaywallImpressionEvent.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallImpressionEvent.Data(paywallId = "my-paywall"),
        )

        val storedEvent = event.toBackendStoredEvent(appUserID, appSessionID)

        assertThat(storedEvent).isInstanceOf(BackendStoredEvent.CustomPaywallImpression::class.java)
        val customStoredEvent = storedEvent as BackendStoredEvent.CustomPaywallImpression
        assertThat(customStoredEvent.event.id).isEqualTo(fixedId.toString())
        assertThat(customStoredEvent.event.version).isEqualTo(BackendEvent.CUSTOM_PAYWALL_IMPRESSION_EVENT_SCHEMA_VERSION)
        assertThat(customStoredEvent.event.type).isEqualTo("custom_paywall_impression")
        assertThat(customStoredEvent.event.appUserID).isEqualTo(appUserID)
        assertThat(customStoredEvent.event.appSessionId).isEqualTo(appSessionID)
        assertThat(customStoredEvent.event.timestamp).isEqualTo(fixedDate.time)
        assertThat(customStoredEvent.event.paywallId).isEqualTo("my-paywall")
    }

    @Test
    fun `toBackendStoredEvent converts event with null paywallId correctly`() {
        val event = CustomPaywallImpressionEvent(
            creationData = CustomPaywallImpressionEvent.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallImpressionEvent.Data(paywallId = null),
        )

        val storedEvent = event.toBackendStoredEvent(appUserID, appSessionID)

        assertThat(storedEvent).isInstanceOf(BackendStoredEvent.CustomPaywallImpression::class.java)
        val customStoredEvent = storedEvent as BackendStoredEvent.CustomPaywallImpression
        assertThat(customStoredEvent.event.paywallId).isNull()
    }

    @Test
    fun `BackendStoredEvent CustomPaywallImpression JSON roundtrip`() {
        val event = CustomPaywallImpressionEvent(
            creationData = CustomPaywallImpressionEvent.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallImpressionEvent.Data(paywallId = "my-paywall"),
        )
        val storedEvent = event.toBackendStoredEvent(appUserID, appSessionID)

        val jsonString = json.encodeToString(BackendStoredEvent.serializer(), storedEvent)
        val decoded = json.decodeFromString(BackendStoredEvent.serializer(), jsonString)

        assertThat(decoded).isInstanceOf(BackendStoredEvent.CustomPaywallImpression::class.java)
        val decodedEvent = (decoded as BackendStoredEvent.CustomPaywallImpression).event
        assertThat(decodedEvent.id).isEqualTo(fixedId.toString())
        assertThat(decodedEvent.version).isEqualTo(BackendEvent.CUSTOM_PAYWALL_IMPRESSION_EVENT_SCHEMA_VERSION)
        assertThat(decodedEvent.type).isEqualTo("custom_paywall_impression")
        assertThat(decodedEvent.appUserID).isEqualTo(appUserID)
        assertThat(decodedEvent.appSessionId).isEqualTo(appSessionID)
        assertThat(decodedEvent.timestamp).isEqualTo(fixedDate.time)
        assertThat(decodedEvent.paywallId).isEqualTo("my-paywall")
    }

    @Test
    fun `BackendStoredEvent CustomPaywallImpression JSON roundtrip with null paywallId`() {
        val event = CustomPaywallImpressionEvent(
            creationData = CustomPaywallImpressionEvent.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallImpressionEvent.Data(paywallId = null),
        )
        val storedEvent = event.toBackendStoredEvent(appUserID, appSessionID)

        val jsonString = json.encodeToString(BackendStoredEvent.serializer(), storedEvent)
        val decoded = json.decodeFromString(BackendStoredEvent.serializer(), jsonString)

        assertThat(decoded).isInstanceOf(BackendStoredEvent.CustomPaywallImpression::class.java)
        val decodedEvent = (decoded as BackendStoredEvent.CustomPaywallImpression).event
        assertThat(decodedEvent.paywallId).isNull()
    }

    @Test
    fun `version is 1 and type is custom_paywall_impression`() {
        val event = CustomPaywallImpressionEvent(
            creationData = CustomPaywallImpressionEvent.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallImpressionEvent.Data(paywallId = "test"),
        )
        val storedEvent = event.toBackendStoredEvent(appUserID, appSessionID) as BackendStoredEvent.CustomPaywallImpression

        assertThat(storedEvent.event.version).isEqualTo(1)
        assertThat(storedEvent.event.type).isEqualTo("custom_paywall_impression")
    }

    @Test
    fun `CustomPaywallImpressionParams default has null paywallId`() {
        val params = CustomPaywallImpressionParams()
        assertThat(params.paywallId).isNull()
    }

    @Test
    fun `CustomPaywallImpressionParams with paywallId`() {
        val params = CustomPaywallImpressionParams(paywallId = "my-paywall")
        assertThat(params.paywallId).isEqualTo("my-paywall")
    }

    @Test
    fun `toBackendStoredEvent includes appSessionId in JSON`() {
        val event = CustomPaywallImpressionEvent(
            creationData = CustomPaywallImpressionEvent.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallImpressionEvent.Data(paywallId = "my-paywall"),
        )
        val storedEvent = event.toBackendStoredEvent(appUserID, appSessionID)

        val jsonString = json.encodeToString(BackendStoredEvent.serializer(), storedEvent)

        assertThat(jsonString).contains("\"app_session_id\":\"$appSessionID\"")
    }
}
