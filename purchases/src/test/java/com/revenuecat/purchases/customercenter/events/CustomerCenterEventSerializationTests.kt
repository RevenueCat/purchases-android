package com.revenuecat.purchases.customercenter.events

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@RunWith(AndroidJUnit4::class)
public class CustomerCenterEventSerializationTests {

    val event = CustomerCenterEvent(
        creationData = CustomerCenterEvent.CreationData(
            id = UUID.fromString("298207f4-87af-4b57-a581-eb27bcc6e009"),
            date = Date(1699270688884)
        ),
        data = CustomerCenterEvent.Data(
            type = CustomerCenterEventType.IMPRESSION,
            timestamp = Date(1699270688884),
            darkMode = true,
            locale = "en_US",
            isSandbox = true,
            sessionIdentifier = UUID.fromString("298207f4-87af-4b57-a581-eb27bcc6e009"),
        )
    )

    @Test
    fun `can encode customer center event correctly`() {
        val eventString: String = CustomerCenterEvent.json.encodeToString(event)
        val expectedJson = "{\"creationData\":{\"id\":\"298207f4-87af-4b57-a581-eb27bcc6e009\",\"date\":1699270688884},\"data\":{\"type\":\"customer_center_impression\",\"timestamp\":1699270688884,\"sessionIdentifier\":\"298207f4-87af-4b57-a581-eb27bcc6e009\",\"darkMode\":true,\"locale\":\"en_US\",\"isSandbox\":true}}"

        assertThat(eventString).isEqualTo(expectedJson)
    }

    @Test
    fun `can encode and decode event correctly`() {
        val eventString = CustomerCenterEvent.json.encodeToString(event)
        print(eventString)
        val decodedEvent = CustomerCenterEvent.json.decodeFromString<CustomerCenterEvent>(eventString)
        assertThat(decodedEvent).isEqualTo(event)
    }
}