package com.revenuecat.customercenter.events

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@RunWith(AndroidJUnit4::class)
class CustomerCenterEventRequestSerializationTest {

    private val request = CustomerCenterEventRequest(listOf(
        CustomerCenterBackendEvent(
            id = "id",
            version = 1,
            type = CustomerCenterEventType.LOGIN.value,
            appUserID = "appUserID",
            sessionID = "sessionID",
            customerID = "customerID",
            timestamp = 123456789,
            localeIdentifier = "en_US",
        )
    ))

    @Test
    fun `can encode customer center event request correctly`() {
        val requestString = CustomerCenterEventRequest.json.encodeToString(request)
        Assertions.assertThat(requestString).isEqualTo(
            "{" +
                "\"events\":[" +
                    "{" +
                        "\"id\":\"id\"," +
                        "\"version\":1," +
                        "\"type\":\"customer_center_login\"," +
                        "\"app_user_id\":\"appUserID\"," +
                        "\"session_id\":\"sessionID\"," +
                        "\"customer_id\":\"customerID\"," +
                        "\"timestamp\":123456789," +
                        "\"locale\":\"en_US\"" +
                    "}" +
                "]" +
            "}"
        )
    }

    @Test
    fun `can encode and decode customer center event correctly`() {
        val requestString = CustomerCenterEventRequest.json.encodeToString(request)
        val decodedRequest = CustomerCenterEventRequest.json.decodeFromString<CustomerCenterEventRequest>(requestString)
        Assertions.assertThat(decodedRequest).isEqualTo(request)
    }
} 