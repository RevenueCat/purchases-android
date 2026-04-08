package com.revenuecat.purchases.paywalls.events

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.common.JsonProvider
import com.revenuecat.purchases.common.events.BackendEvent
import com.revenuecat.purchases.common.events.BackendStoredEvent
import com.revenuecat.purchases.common.events.EventsRequest
import com.revenuecat.purchases.common.events.toBackendEvent
import kotlinx.serialization.encodeToString
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaywallEventsRequestSerializationTest {

    private val request = EventsRequest(listOf(
        BackendStoredEvent.Paywalls(
            BackendEvent.Paywalls(
                id = "id",
                version = 1,
                type = PaywallEventType.CANCEL.value,
                appUserID = "appUserID",
                sessionID = "sessionID",
                offeringID = "offeringID",
                paywallID = "paywallID",
                paywallRevision = 5,
                timestamp = 123456789,
                displayMode = "footer",
                darkMode = true,
                localeIdentifier = "en_US",
            )
        )
    ).map { it.toBackendEvent() })

    @Test
    fun `can encode paywall event request correctly`() {
        val requestString = JsonProvider.defaultJson.encodeToString(request)
        Assertions.assertThat(requestString).isEqualTo(
            "{" +
                "\"events\":[" +
                    "{" +
                        "\"discriminator\":\"paywalls\"," +
                        "\"id\":\"id\"," +
                        "\"version\":1," +
                        "\"type\":\"paywall_cancel\"," +
                        "\"app_user_id\":\"appUserID\"," +
                        "\"session_id\":\"sessionID\"," +
                        "\"offering_id\":\"offeringID\"," +
                        "\"paywall_id\":\"paywallID\"," +
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
        val requestString = JsonProvider.defaultJson.encodeToString(request)
        val decodedRequest = JsonProvider.defaultJson.decodeFromString<EventsRequest>(requestString)
        Assertions.assertThat(decodedRequest).isEqualTo(request)
    }

    @Test
    fun `can encode paywall control interaction event with component fields`() {
        val controlRequest = EventsRequest(
            listOf(
                BackendStoredEvent.Paywalls(
                    BackendEvent.Paywalls(
                        id = "cid",
                        version = 1,
                        type = PaywallEventType.CONTROL_INTERACTION.value,
                        appUserID = "user",
                        sessionID = "sess",
                        offeringID = "off",
                        paywallID = "pw",
                        paywallRevision = 1,
                        timestamp = 100L,
                        displayMode = "fullscreen",
                        darkMode = false,
                        localeIdentifier = "en_US",
                        componentType = "button",
                        componentName = "restore_cta",
                        componentValue = "restore_purchases",
                        componentUrl = null,
                    ),
                ),
            ).map { it.toBackendEvent() },
        )
        val requestString = JsonProvider.defaultJson.encodeToString(controlRequest)
        Assertions.assertThat(requestString).isEqualTo(
            "{" +
                "\"events\":[" +
                    "{" +
                        "\"discriminator\":\"paywalls\"," +
                        "\"id\":\"cid\"," +
                        "\"version\":1," +
                        "\"type\":\"paywall_control_interaction\"," +
                        "\"app_user_id\":\"user\"," +
                        "\"session_id\":\"sess\"," +
                        "\"offering_id\":\"off\"," +
                        "\"paywall_id\":\"pw\"," +
                        "\"paywall_revision\":1," +
                        "\"timestamp\":100," +
                        "\"display_mode\":\"fullscreen\"," +
                        "\"dark_mode\":false," +
                        "\"locale\":\"en_US\"," +
                        "\"component_type\":\"button\"," +
                        "\"component_name\":\"restore_cta\"," +
                        "\"component_value\":\"restore_purchases\"" +
                    "}" +
                "]" +
            "}",
        )
    }

    @Test
    fun `can encode paywall control interaction with package lifecycle fields`() {
        val controlRequest = EventsRequest(
            listOf(
                BackendStoredEvent.Paywalls(
                    BackendEvent.Paywalls(
                        id = "cid",
                        version = 1,
                        type = PaywallEventType.CONTROL_INTERACTION.value,
                        appUserID = "user",
                        sessionID = "sess",
                        offeringID = "off",
                        paywallID = "pw",
                        paywallRevision = 1,
                        timestamp = 100L,
                        displayMode = "fullscreen",
                        darkMode = false,
                        localeIdentifier = "en_US",
                        componentType = "package_selection_sheet",
                        componentName = "sheet_a",
                        componentValue = "close",
                        componentUrl = null,
                        currentPackageIdentifier = "monthly",
                        resultingPackageIdentifier = "annual",
                        currentProductIdentifier = "com.monthly",
                        resultingProductIdentifier = "com.annual",
                    ),
                ),
            ).map { it.toBackendEvent() },
        )
        val requestString = JsonProvider.defaultJson.encodeToString(controlRequest)
        Assertions.assertThat(requestString).isEqualTo(
            "{" +
                "\"events\":[" +
                    "{" +
                        "\"discriminator\":\"paywalls\"," +
                        "\"id\":\"cid\"," +
                        "\"version\":1," +
                        "\"type\":\"paywall_control_interaction\"," +
                        "\"app_user_id\":\"user\"," +
                        "\"session_id\":\"sess\"," +
                        "\"offering_id\":\"off\"," +
                        "\"paywall_id\":\"pw\"," +
                        "\"paywall_revision\":1," +
                        "\"timestamp\":100," +
                        "\"display_mode\":\"fullscreen\"," +
                        "\"dark_mode\":false," +
                        "\"locale\":\"en_US\"," +
                        "\"component_type\":\"package_selection_sheet\"," +
                        "\"component_name\":\"sheet_a\"," +
                        "\"component_value\":\"close\"," +
                        "\"current_package_id\":\"monthly\"," +
                        "\"resulting_package_id\":\"annual\"," +
                        "\"current_product_id\":\"com.monthly\"," +
                        "\"resulting_product_id\":\"com.annual\"" +
                    "}" +
                "]" +
            "}",
        )
    }
}
