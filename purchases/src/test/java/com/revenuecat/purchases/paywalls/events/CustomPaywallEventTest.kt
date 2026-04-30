package com.revenuecat.purchases.paywalls.events

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.common.events.BackendEvent
import com.revenuecat.purchases.common.events.BackendStoredEvent
import com.revenuecat.purchases.common.events.toBackendStoredEvent
import com.revenuecat.purchases.utils.stubStoreProduct
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
    fun `CustomPaywallEvent Impression is created with offeringId`() {
        val event = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallEvent.Impression.Data(
                paywallId = "my-paywall",
                offeringId = "offering-123",
            ),
        )

        assertThat(event.data.offeringId).isEqualTo("offering-123")
    }

    @Test
    fun `CustomPaywallEvent Impression offeringId defaults to null`() {
        val event = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallEvent.Impression.Data(paywallId = "my-paywall"),
        )

        assertThat(event.data.offeringId).isNull()
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
    fun `toBackendStoredEvent converts event with offeringId correctly`() {
        val event = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallEvent.Impression.Data(
                paywallId = "my-paywall",
                offeringId = "offering-123",
            ),
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
                offeringID = "offering-123",
            ),
        )
        assertThat(storedEvent).isEqualTo(expectedStoredEvent)
    }

    @Test
    fun `toBackendStoredEvent converts event with null offeringId correctly`() {
        val event = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallEvent.Impression.Data(
                paywallId = "my-paywall",
                offeringId = null,
            ),
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
                offeringID = null,
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
            data = CustomPaywallEvent.Impression.Data(
                paywallId = "my-paywall",
                offeringId = "offering-123",
            ),
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
        assertThat(decodedEvent.offeringID).isEqualTo("offering-123")
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
    fun `BackendStoredEvent CustomPaywall JSON roundtrip with null offeringId`() {
        val event = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallEvent.Impression.Data(
                paywallId = "my-paywall",
                offeringId = null,
            ),
        )
        val storedEvent = event.toBackendStoredEvent(appUserID, appSessionID)

        val jsonString = json.encodeToString(BackendStoredEvent.serializer(), storedEvent)
        val decoded = json.decodeFromString(BackendStoredEvent.serializer(), jsonString)

        assertThat(decoded).isInstanceOf(BackendStoredEvent.CustomPaywall::class.java)
        val decodedEvent = (decoded as BackendStoredEvent.CustomPaywall).event
        assertThat(decodedEvent.offeringID).isNull()
    }

    @Test
    fun `BackendStoredEvent CustomPaywall JSON contains offering_id key`() {
        val event = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallEvent.Impression.Data(
                paywallId = "my-paywall",
                offeringId = "offering-123",
            ),
        )
        val storedEvent = event.toBackendStoredEvent(appUserID, appSessionID)

        val jsonString = json.encodeToString(BackendStoredEvent.serializer(), storedEvent)

        assertThat(jsonString).contains("\"offering_id\":\"offering-123\"")
    }

    @Test
    fun `BackendStoredEvent CustomPaywall JSON omits offering_id when null`() {
        val event = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallEvent.Impression.Data(
                paywallId = "my-paywall",
                offeringId = null,
            ),
        )
        val storedEvent = event.toBackendStoredEvent(appUserID, appSessionID)

        val jsonString = json.encodeToString(BackendStoredEvent.serializer(), storedEvent)

        assertThat(jsonString).doesNotContain("offering_id")
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

    // region Params: Offering-based init

    @Test
    fun `CustomPaywallImpressionParams default has null offering`() {
        val params = CustomPaywallImpressionParams()
        assertThat(params.offering).isNull()
    }

    @Test
    fun `CustomPaywallImpressionParams string-id init does not store offering`() {
        val params = CustomPaywallImpressionParams(paywallId = "pw", offeringId = "my-offering")
        assertThat(params.offering).isNull()
    }

    @Test
    fun `CustomPaywallImpressionParams Offering-based init populates offeringId from offering`() {
        val offering = makeOffering(identifier = "my-offering")
        val params = CustomPaywallImpressionParams(paywallId = "pw", offering = offering)

        assertThat(params.paywallId).isEqualTo("pw")
        assertThat(params.offeringId).isEqualTo("my-offering")
    }

    @Test
    fun `CustomPaywallImpressionParams Offering-based init stores offering`() {
        val offering = makeOffering(identifier = "my-offering")
        val params = CustomPaywallImpressionParams(paywallId = "pw", offering = offering)

        assertThat(params.offering).isSameAs(offering)
    }

    @Test
    fun `CustomPaywallImpressionParams Offering-only init defaults paywallId to null`() {
        val offering = makeOffering(identifier = "offering_1")
        val params = CustomPaywallImpressionParams(offering = offering)

        assertThat(params.paywallId).isNull()
        assertThat(params.offeringId).isEqualTo("offering_1")
        assertThat(params.offering).isSameAs(offering)
    }

    // endregion

    // region Data: placement and targeting fields

    @Test
    fun `CustomPaywallEvent Data placement and targeting default to null`() {
        val data = CustomPaywallEvent.Impression.Data(paywallId = "pw", offeringId = "off")

        assertThat(data.placementIdentifier).isNull()
        assertThat(data.targetingRevision).isNull()
        assertThat(data.targetingRuleId).isNull()
    }

    @Test
    fun `CustomPaywallEvent Data preserves placement and targeting`() {
        val data = CustomPaywallEvent.Impression.Data(
            paywallId = "pw",
            offeringId = "off",
            placementIdentifier = "home_banner",
            targetingRevision = 3,
            targetingRuleId = "rule_abc123",
        )

        assertThat(data.placementIdentifier).isEqualTo("home_banner")
        assertThat(data.targetingRevision).isEqualTo(3)
        assertThat(data.targetingRuleId).isEqualTo("rule_abc123")
    }

    // endregion

    // region toBackendStoredEvent: presented offering context

    @Test
    fun `toBackendStoredEvent populates presentedOfferingContext when all fields set`() {
        val event = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(id = fixedId, date = fixedDate),
            data = CustomPaywallEvent.Impression.Data(
                paywallId = "pw",
                offeringId = "off",
                placementIdentifier = "home_banner",
                targetingRevision = 3,
                targetingRuleId = "rule_abc123",
            ),
        )

        val storedEvent = event.toBackendStoredEvent(appUserID, appSessionID) as BackendStoredEvent.CustomPaywall

        assertThat(storedEvent.event.presentedOfferingContext).isEqualTo(
            BackendEvent.CustomPaywallPresentedOfferingContextData(
                placementIdentifier = "home_banner",
                targetingRevision = 3,
                targetingRuleId = "rule_abc123",
            ),
        )
    }

    @Test
    fun `toBackendStoredEvent populates presentedOfferingContext with placement only`() {
        val event = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(id = fixedId, date = fixedDate),
            data = CustomPaywallEvent.Impression.Data(
                paywallId = "pw",
                offeringId = "off",
                placementIdentifier = "home_banner",
            ),
        )

        val storedEvent = event.toBackendStoredEvent(appUserID, appSessionID) as BackendStoredEvent.CustomPaywall

        assertThat(storedEvent.event.presentedOfferingContext).isEqualTo(
            BackendEvent.CustomPaywallPresentedOfferingContextData(placementIdentifier = "home_banner"),
        )
    }

    @Test
    fun `toBackendStoredEvent leaves presentedOfferingContext null when all fields null`() {
        val event = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(id = fixedId, date = fixedDate),
            data = CustomPaywallEvent.Impression.Data(paywallId = "pw", offeringId = "off"),
        )

        val storedEvent = event.toBackendStoredEvent(appUserID, appSessionID) as BackendStoredEvent.CustomPaywall

        assertThat(storedEvent.event.presentedOfferingContext).isNull()
    }

    // endregion

    // region Wire encoding: presented_offering_context

    @Test
    fun `BackendStoredEvent CustomPaywall JSON nests presented_offering_context with all fields`() {
        val event = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(id = fixedId, date = fixedDate),
            data = CustomPaywallEvent.Impression.Data(
                paywallId = "pw",
                offeringId = "off",
                placementIdentifier = "home_banner",
                targetingRevision = 3,
                targetingRuleId = "rule_abc123",
            ),
        )
        val storedEvent = event.toBackendStoredEvent(appUserID, appSessionID)

        val jsonString = json.encodeToString(BackendStoredEvent.serializer(), storedEvent)

        assertThat(jsonString).contains(
            "\"presented_offering_context\":{" +
                "\"placement_identifier\":\"home_banner\"," +
                "\"targeting_revision\":3," +
                "\"targeting_rule_id\":\"rule_abc123\"" +
                "}",
        )
    }

    @Test
    fun `BackendStoredEvent CustomPaywall JSON nests presented_offering_context with placement only`() {
        val event = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(id = fixedId, date = fixedDate),
            data = CustomPaywallEvent.Impression.Data(
                paywallId = "pw",
                offeringId = "off",
                placementIdentifier = "home_banner",
            ),
        )
        val storedEvent = event.toBackendStoredEvent(appUserID, appSessionID)

        val jsonString = json.encodeToString(BackendStoredEvent.serializer(), storedEvent)

        assertThat(jsonString).contains("\"presented_offering_context\":{\"placement_identifier\":\"home_banner\"}")
        assertThat(jsonString).doesNotContain("\"targeting_revision\"")
        assertThat(jsonString).doesNotContain("\"targeting_rule_id\"")
    }

    @Test
    fun `BackendStoredEvent CustomPaywall JSON omits presented_offering_context when null`() {
        val event = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(id = fixedId, date = fixedDate),
            data = CustomPaywallEvent.Impression.Data(paywallId = "pw", offeringId = "off"),
        )
        val storedEvent = event.toBackendStoredEvent(appUserID, appSessionID)

        val jsonString = json.encodeToString(BackendStoredEvent.serializer(), storedEvent)

        assertThat(jsonString).doesNotContain("presented_offering_context")
    }

    @Test
    fun `BackendStoredEvent CustomPaywall JSON roundtrip preserves presented_offering_context`() {
        val event = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(id = fixedId, date = fixedDate),
            data = CustomPaywallEvent.Impression.Data(
                paywallId = "pw",
                offeringId = "off",
                placementIdentifier = "home_banner",
                targetingRevision = 3,
                targetingRuleId = "rule_abc123",
            ),
        )
        val storedEvent = event.toBackendStoredEvent(appUserID, appSessionID)

        val jsonString = json.encodeToString(BackendStoredEvent.serializer(), storedEvent)
        val decoded = json.decodeFromString(BackendStoredEvent.serializer(), jsonString)

        val decodedEvent = (decoded as BackendStoredEvent.CustomPaywall).event
        assertThat(decodedEvent.presentedOfferingContext).isEqualTo(
            BackendEvent.CustomPaywallPresentedOfferingContextData(
                placementIdentifier = "home_banner",
                targetingRevision = 3,
                targetingRuleId = "rule_abc123",
            ),
        )
    }

    // endregion

    private fun makeOffering(
        identifier: String,
        presentedOfferingContext: PresentedOfferingContext = PresentedOfferingContext(identifier),
    ): Offering {
        val storeProduct = stubStoreProduct("monthly_product")
        val packageObject = Package(
            identifier = "\$rc_monthly",
            packageType = PackageType.MONTHLY,
            product = storeProduct,
            presentedOfferingContext = presentedOfferingContext,
        )
        return Offering(
            identifier = identifier,
            serverDescription = "",
            metadata = emptyMap(),
            availablePackages = listOf(packageObject),
        )
    }
}
