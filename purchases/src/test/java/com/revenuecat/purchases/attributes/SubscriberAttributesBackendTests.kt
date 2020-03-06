package com.revenuecat.purchases.attributes

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Backend
import com.revenuecat.purchases.HTTPClient
import com.revenuecat.purchases.PurchaserInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.SyncDispatcher
import com.revenuecat.purchases.buildPurchaserInfo
import com.revenuecat.purchases.toBackendMap
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.json.JSONObject
import org.junit.Before
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

    @Before
    fun setup() = mockkStatic("com.revenuecat.purchases.FactoriesKt")

    // region posting attributes

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
            { _, _ ->
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
            { error, syncedSuccessfully ->
                receivedError = error
                receivedSyncedSuccessfully = syncedSuccessfully
            }
        )
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.NetworkError)
        assertThat(receivedSyncedSuccessfully).isFalse()
    }

    @Test
    fun `attributes validation error when posting attributes`() {
        mockResponse("/subscribers/$appUserID/attributes", 400, expectedResultBody = "{\n" +
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
            { error, syncedSuccessfully ->
                receivedError = error
                receivedSyncedSuccessfully = syncedSuccessfully
            }
        )
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.UnknownBackendError)
        assertThat(receivedSyncedSuccessfully).isFalse()
    }

    // endregion

    // region posting attributes when posting receipt

    private val subscriberAttribute1 = SubscriberAttribute("key", "value")
    private val subscriberAttribute2 = SubscriberAttribute("key1", null)
    private val mapOfSubscriberAttributes = mapOf<String, SubscriberAttribute>(
        "key" to subscriberAttribute1,
        "key1" to subscriberAttribute2
    )
    private val fetchToken = "fetch_token"
    private val productID = "product_id"

    @Test
    fun `posting receipt with attributes works`() {
        mockPostReceiptResponse(mapOfSubscriberAttributes)

        var receivedPurchaserInfo: PurchaserInfo? = null
        underTest.postReceiptData(
            fetchToken,
            appUserID,
            productID,
            false,
            null,
            false,
            null,
            null,
            mapOfSubscriberAttributes,
            {
                receivedPurchaserInfo = it
            },
            { _ , _ ->

            }
        )

        assertThat(receivedPurchaserInfo).isNotNull
        val actualPostReceiptBody = actualPostReceiptBodySlot.captured
        assertThat(actualPostReceiptBody).isNotNull()
        assertThat(actualPostReceiptBody["attributes"]).isNotNull
    }

    @Test
    fun `posting receipt without attributes skips them`() {
        mockPostReceiptResponse(mapOfSubscriberAttributes)

        var receivedPurchaserInfo: PurchaserInfo? = null
        underTest.postReceiptData(
            fetchToken,
            appUserID,
            productID,
            false,
            null,
            false,
            null,
            null,
            emptyMap(),
            {
                receivedPurchaserInfo = it
            },
            { _ , _ ->

            }
        )

        assertThat(receivedPurchaserInfo).isNotNull
        val actualPostReceiptBody = actualPostReceiptBodySlot.captured
        assertThat(actualPostReceiptBody).isNotNull()
        assertThat(actualPostReceiptBody["attributes"]).isNull()
    }

    // endregion

    private fun mockResponse(
        path: String,
        responseCode: Int,
        expectedBody: Map<String, Any?>? = null,
        clientException: Exception? = null,
        expectedResultBody: String? = null
    ) {
        val everyMockedCall = every {
            mockClient.performRequest(
                path,
                (expectedBody ?: any()) as Map<*, *>,
                mapOf("Authorization" to "Bearer $API_KEY")
            )
        }

        if (clientException == null) {
            everyMockedCall answers {
                HTTPClient.Result().also {
                    it.responseCode = responseCode
                    it.body = JSONObject(expectedResultBody ?: "{}")
                }
            }
        } else {
            everyMockedCall throws clientException
        }
    }

    private val actualPostReceiptBodySlot = slot<Map<*, *>>()
    private fun mockPostReceiptResponse(
        attributes: Map<String, SubscriberAttribute>?
    ) {
        val expectedBody = mapOf(
            "fetch_token" to fetchToken,
            "app_user_id" to appUserID,
            "product_id" to productID,
            "is_restore" to false,
            "observer_mode" to false,
            "attributes" to attributes?.toBackendMap()
        ).mapNotNull { (key, value) ->
            value?.let { key to it }
        }.toMap()

        every {
            mockClient.performRequest(
                "/receipts",
                capture(actualPostReceiptBodySlot),
                mapOf("Authorization" to "Bearer $API_KEY")
            )
        } answers {
            HTTPClient.Result().also {
                it.responseCode = 200
                it.body = JSONObject( "{}")
                every {
                    it.body!!.buildPurchaserInfo()
                } returns mockk()
            }
        }
    }
}