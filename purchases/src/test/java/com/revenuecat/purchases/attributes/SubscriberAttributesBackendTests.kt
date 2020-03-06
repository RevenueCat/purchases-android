package com.revenuecat.purchases.attributes

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Backend
import com.revenuecat.purchases.HTTPClient
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.SyncDispatcher
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

private const val API_KEY = "TEST_API_KEY"

@RunWith(AndroidJUnit4::class)
class SubscriberAttributesBackendTests {
    private var mockClient: HTTPClient = mockk(relaxed = true)
    private val appUserID = "jerry"
    private var underTest: Backend = Backend(
        API_KEY,
        SyncDispatcher(),
        mockClient
    )

    @Test
    fun `posting subscriber attributes works`() {
        mockResponse("/subscribers/$appUserID/attributes", 200)

        var received = false
        underTest.postSubscriberAttributes(
            mapOf("email" to SubscriberAttribute("email", "un@email.com")),
            appUserID,
            {
                received = true
            },
            { _, _ ->
                fail("should be success")
            }
        )
        assertThat(received).isTrue()
    }

    @Test
    fun `posting null subscriber attributes works`() {
        mockResponse("/subscribers/$appUserID/attributes", 200)

        var received = false
        underTest.postSubscriberAttributes(
            mapOf("email" to SubscriberAttribute("email", null)),
            appUserID,
            {
                received = true
            },
            { _,  _ ->
                fail("should be success")
            }
        )
        assertThat(received).isTrue()
    }

    @Test
    fun `error when posting attributes`() {
        mockResponse("/subscribers/$appUserID/attributes", 0, clientException = IOException())

        var receivedError: PurchasesError? = null
        var receivedSyncedSuccessfully: Boolean? = null
        underTest.postSubscriberAttributes(
            mapOf("email" to SubscriberAttribute("email", null)),
            appUserID,
            {
                fail("should be failure")
            },
            { error,  syncedSuccessfully ->
                receivedError = error
                receivedSyncedSuccessfully = syncedSuccessfully
            }
        )
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.NetworkError)
        assertThat(receivedSyncedSuccessfully).isFalse()
    }

    @Test
    fun `backend error when posting attributes`() {
        mockResponse("/subscribers/$appUserID/attributes", 503)

        var receivedError: PurchasesError? = null
        var receivedSyncedSuccessfully: Boolean? = null
        underTest.postSubscriberAttributes(
            mapOf("email" to SubscriberAttribute("email", null)),
            appUserID,
            {
                fail("should be failure")
            },
            { error,  syncedSuccessfully ->
                receivedError = error
                receivedSyncedSuccessfully = syncedSuccessfully
            }
        )
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.UnknownBackendError)
        assertThat(receivedSyncedSuccessfully).isFalse()
    }

    @Test
    fun `attributes validation error when posting attributes`() {
        mockResponse("/subscribers/$appUserID/attributes", 400, resultBody = "{\n" +
            "  \"code\": 7262,\n" +
            "  \"message\": \"Some subscriber attributes keys were unable to saved.\",\n" +
            "  \"attribute_erors\": [\n" +
            "    {\n" +
            "      \"key_name\": \"email\",\n" +
            "      \"message\": \"Value is not a valid email address.\"\n" +
            "    }\n" +
            "  ]\n" +
            "}")

        var receivedError: PurchasesError? = null
        var receivedSyncedSuccessfully: Boolean? = null
        underTest.postSubscriberAttributes(
            mapOf("email" to SubscriberAttribute("email", null)),
            appUserID,
            {
                fail("should be failure")
            },
            { error,  syncedSuccessfully ->
                receivedError = error
                receivedSyncedSuccessfully = syncedSuccessfully
            }
        )
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.InvalidSubscriberAttributesError)
        assertThat(receivedSyncedSuccessfully).isTrue()
    }

    private fun mockResponse(
        path: String,
        responseCode: Int,
        body: Map<String, Any?>? = null,
        clientException: Exception? = null,
        resultBody: String? = null
    ) {
        val everyMockedCall = every {
            mockClient.performRequest(
                path,
                (body ?: any()) as Map<*, *>,
                mapOf("Authorization" to "Bearer $API_KEY")
            )
        }

        if (clientException == null) {
            everyMockedCall answers {
                HTTPClient.Result().also {
                    it.responseCode = responseCode
                    it.body = JSONObject(resultBody ?: "{}")
                }
            }
        } else {
            everyMockedCall throws clientException
        }
    }
}