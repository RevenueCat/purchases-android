package com.revenuecat.purchases.customercenter.events

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.JsonProvider
import com.revenuecat.purchases.common.events.BackendEvent
import com.revenuecat.purchases.common.events.BackendStoredEvent
import com.revenuecat.purchases.common.events.EventsRequest
import com.revenuecat.purchases.common.events.toBackendEvent
import com.revenuecat.purchases.common.events.toBackendStoredEvent
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import kotlinx.serialization.encodeToString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@OptIn(InternalRevenueCatAPI::class)
@RunWith(AndroidJUnit4::class)
class CustomerCenterEventsSerializationTest {

    @Test
    fun `CustomerCenterImpressionEvent serialization has null promo fields`() {
        val event = CustomerCenterImpressionEvent(
            data = CustomerCenterImpressionEvent.Data(
                timestamp = Date(123456789),
                darkMode = true,
                locale = "en_US",
                version = 1,
                revisionID = 1,
                displayMode = CustomerCenterDisplayMode.FULL_SCREEN,
            )
        )

        val backendEvent = event.toBackendStoredEvent("appUserID", "sessionID")
        val request = EventsRequest(listOf(backendEvent.toBackendEvent()))
        val requestString = JsonProvider.defaultJson.encodeToString(request)

        // Verify promo fields are null in JSON
        assertThat(requestString).contains("\"store_promo_offer_id\":null")
        assertThat(requestString).contains("\"product_id\":null")
        assertThat(requestString).contains("\"target_product_id\":null")

        // Verify required fields are present
        assertThat(requestString).contains("customer_center_impression")
        assertThat(requestString).contains("\"dark_mode\":true")
        assertThat(requestString).contains("\"locale\":\"en_US\"")
    }

    @Test
    fun `CustomerCenterSurveyOptionChosenEvent serialization has null promo fields`() {
        val event = CustomerCenterSurveyOptionChosenEvent(
            data = CustomerCenterSurveyOptionChosenEvent.Data(
                timestamp = Date(123456789),
                darkMode = false,
                locale = "en_GB",
                version = 1,
                revisionID = 2,
                displayMode = CustomerCenterDisplayMode.FULL_SCREEN,
                path = CustomerCenterConfigData.HelpPath.PathType.CANCEL,
                url = null,
                surveyOptionID = "option_123",
                additionalContext = null,
            )
        )

        val backendEvent = event.toBackendStoredEvent("appUserID", "sessionID")
        val request = EventsRequest(listOf(backendEvent.toBackendEvent()))
        val requestString = JsonProvider.defaultJson.encodeToString(request)

        // Verify promo fields are null in JSON
        assertThat(requestString).contains("\"store_promo_offer_id\":null")
        assertThat(requestString).contains("\"product_id\":null")
        assertThat(requestString).contains("\"target_product_id\":null")

        // Verify required fields are present
        assertThat(requestString).contains("customer_center_survey_option_chosen")
        assertThat(requestString).contains("\"survey_option_id\":\"option_123\"")
        assertThat(requestString).contains("\"dark_mode\":false")
        assertThat(requestString).contains("\"locale\":\"en_GB\"")
    }

    @Test
    fun `CustomerCenterPromoOfferEvent success serialization includes all promo fields`() {
        val event = CustomerCenterPromoOfferEvent(
            data = CustomerCenterPromoOfferEvent.Data(
                type = CustomerCenterEventType.PROMO_OFFER_SUCCESS,
                timestamp = Date(123456789),
                darkMode = true,
                locale = "en_US",
                version = 1,
                revisionID = 1,
                displayMode = CustomerCenterDisplayMode.FULL_SCREEN,
                path = CustomerCenterConfigData.HelpPath.PathType.CANCEL,
                url = null,
                surveyOptionID = null,
                storeOfferID = "store_offer_456",
                productID = "product_abc",
                targetProductID = "product_xyz",
                error = null,
                transactionID = "transaction_789",
            )
        )

        val backendEvent = event.toBackendStoredEvent("appUserID", "sessionID")
        val request = EventsRequest(listOf(backendEvent.toBackendEvent()))
        val requestString = JsonProvider.defaultJson.encodeToString(request)

        // Verify all promo fields are present
        assertThat(requestString).contains("\"store_promo_offer_id\":\"store_offer_456\"")
        assertThat(requestString).contains("\"product_id\":\"product_abc\"")
        assertThat(requestString).contains("\"target_product_id\":\"product_xyz\"")
        assertThat(requestString).contains("\"transaction_id\":\"transaction_789\"")

        // Verify event type
        assertThat(requestString).contains("customer_center_promo_offer_success")
    }

    @Test
    fun `CustomerCenterPromoOfferEvent error serialization includes error field`() {
        val event = CustomerCenterPromoOfferEvent(
            data = CustomerCenterPromoOfferEvent.Data(
                type = CustomerCenterEventType.PROMO_OFFER_ERROR,
                timestamp = Date(123456789),
                darkMode = false,
                locale = "fr_FR",
                version = 1,
                revisionID = 1,
                displayMode = CustomerCenterDisplayMode.FULL_SCREEN,
                path = CustomerCenterConfigData.HelpPath.PathType.CANCEL,
                url = null,
                surveyOptionID = null,
                storeOfferID = "store_offer_456",
                productID = "product_abc",
                targetProductID = "product_xyz",
                error = "Purchase failed",
                transactionID = null,
            )
        )

        val backendEvent = event.toBackendStoredEvent("appUserID", "sessionID")
        val request = EventsRequest(listOf(backendEvent.toBackendEvent()))
        val requestString = JsonProvider.defaultJson.encodeToString(request)

        // Verify error field is present
        assertThat(requestString).contains("\"error\":\"Purchase failed\"")

        // Verify event type
        assertThat(requestString).contains("customer_center_promo_offer_error")
    }

    @Test
    fun `CustomerCenterPromoOfferEvent cancel serialization`() {
        val event = CustomerCenterPromoOfferEvent(
            data = CustomerCenterPromoOfferEvent.Data(
                type = CustomerCenterEventType.PROMO_OFFER_CANCEL,
                timestamp = Date(123456789),
                darkMode = true,
                locale = "de_DE",
                version = 1,
                revisionID = 1,
                displayMode = CustomerCenterDisplayMode.FULL_SCREEN,
                path = CustomerCenterConfigData.HelpPath.PathType.CANCEL,
                url = null,
                surveyOptionID = null,
                storeOfferID = "store_offer_456",
                productID = "product_abc",
                targetProductID = "product_xyz",
                error = null,
                transactionID = null,
            )
        )

        val backendEvent = event.toBackendStoredEvent("appUserID", "sessionID")
        val request = EventsRequest(listOf(backendEvent.toBackendEvent()))
        val requestString = JsonProvider.defaultJson.encodeToString(request)

        // Verify event type
        assertThat(requestString).contains("customer_center_promo_offer_cancel")

        // Verify promo fields are present
        assertThat(requestString).contains("\"store_promo_offer_id\":\"store_offer_456\"")
    }

    @Test
    fun `CustomerCenterPromoOfferEvent rejected serialization`() {
        val event = CustomerCenterPromoOfferEvent(
            data = CustomerCenterPromoOfferEvent.Data(
                type = CustomerCenterEventType.PROMO_OFFER_REJECTED,
                timestamp = Date(123456789),
                darkMode = false,
                locale = "es_ES",
                version = 1,
                revisionID = 1,
                displayMode = CustomerCenterDisplayMode.FULL_SCREEN,
                path = CustomerCenterConfigData.HelpPath.PathType.CANCEL,
                url = null,
                surveyOptionID = null,
                source = PromoOfferRejectionSource.CANCEL,
                storeOfferID = "store_offer_456",
                productID = "product_abc",
                targetProductID = "product_xyz",
                error = null,
                transactionID = null,
            )
        )

        val backendEvent = event.toBackendStoredEvent("appUserID", "sessionID")
        val request = EventsRequest(listOf(backendEvent.toBackendEvent()))
        val requestString = JsonProvider.defaultJson.encodeToString(request)

        // Verify event type
        assertThat(requestString).contains("customer_center_promo_offer_rejected")

        // Verify promo fields are present
        assertThat(requestString).contains("\"source\":\"cancel\"")
        assertThat(requestString).contains("\"target_product_id\":\"product_xyz\"")
    }

    @Test
    fun `CustomerCenterPromoOfferEvent impression serialization includes all fields`() {
        val event = CustomerCenterPromoOfferEvent(
            data = CustomerCenterPromoOfferEvent.Data(
                type = CustomerCenterEventType.PROMO_OFFER_IMPRESSION,
                timestamp = Date(123456789),
                darkMode = true,
                locale = "en_US",
                version = 1,
                revisionID = 1,
                displayMode = CustomerCenterDisplayMode.FULL_SCREEN,
                path = CustomerCenterConfigData.HelpPath.PathType.CANCEL,
                url = null,
                surveyOptionID = "too_expensive",
                storeOfferID = "store_offer_123",
                productID = "premium_monthly",
                targetProductID = "premium_monthly_promo",
                error = null,
                transactionID = null,
            )
        )

        val backendEvent = event.toBackendStoredEvent("appUserID", "sessionID")
        val request = EventsRequest(listOf(backendEvent.toBackendEvent()))
        val requestString = JsonProvider.defaultJson.encodeToString(request)

        assertThat(requestString).contains("customer_center_promo_offer_impression")
        assertThat(requestString).contains("\"store_promo_offer_id\":\"store_offer_123\"")
        assertThat(requestString).contains("\"product_id\":\"premium_monthly\"")
        assertThat(requestString).contains("\"target_product_id\":\"premium_monthly_promo\"")
        assertThat(requestString).contains("\"survey_option_id\":\"too_expensive\"")
        assertThat(requestString).contains("\"path\":\"CANCEL\"")
        assertThat(requestString).contains("\"dark_mode\":true")
    }

    @Test
    fun `can encode and decode impression event correctly`() {
        val event = CustomerCenterImpressionEvent(
            data = CustomerCenterImpressionEvent.Data(
                timestamp = Date(123456789),
                darkMode = true,
                locale = "en_US",
            )
        )

        val backendEvent = event.toBackendStoredEvent("appUserID", "sessionID")
        val request = EventsRequest(listOf(backendEvent.toBackendEvent()))

        val requestString = JsonProvider.defaultJson.encodeToString(request)
        val decodedRequest = JsonProvider.defaultJson.decodeFromString<EventsRequest>(requestString)

        assertThat(decodedRequest).isEqualTo(request)
    }

    @Test
    fun `can encode and decode survey option chosen event correctly`() {
        val event = CustomerCenterSurveyOptionChosenEvent(
            data = CustomerCenterSurveyOptionChosenEvent.Data(
                timestamp = Date(123456789),
                darkMode = false,
                locale = "en_GB",
                path = CustomerCenterConfigData.HelpPath.PathType.CANCEL,
                url = null,
                surveyOptionID = "option_123",
            )
        )

        val backendEvent = event.toBackendStoredEvent("appUserID", "sessionID")
        val request = EventsRequest(listOf(backendEvent.toBackendEvent()))

        val requestString = JsonProvider.defaultJson.encodeToString(request)
        val decodedRequest = JsonProvider.defaultJson.decodeFromString<EventsRequest>(requestString)

        assertThat(decodedRequest).isEqualTo(request)
    }

    @Test
    fun `can encode and decode promo offer event correctly`() {
        val event = CustomerCenterPromoOfferEvent(
            data = CustomerCenterPromoOfferEvent.Data(
                type = CustomerCenterEventType.PROMO_OFFER_SUCCESS,
                timestamp = Date(123456789),
                darkMode = true,
                locale = "en_US",
                path = CustomerCenterConfigData.HelpPath.PathType.CANCEL,
                url = null,
                surveyOptionID = null,
                storeOfferID = "store_offer_456",
                productID = "product_abc",
                targetProductID = "product_xyz",
                error = null,
                transactionID = "transaction_789",
            )
        )

        val backendEvent = event.toBackendStoredEvent("appUserID", "sessionID")
        val request = EventsRequest(listOf(backendEvent.toBackendEvent()))

        val requestString = JsonProvider.defaultJson.encodeToString(request)
        val decodedRequest = JsonProvider.defaultJson.decodeFromString<EventsRequest>(requestString)

        assertThat(decodedRequest).isEqualTo(request)
    }
}
