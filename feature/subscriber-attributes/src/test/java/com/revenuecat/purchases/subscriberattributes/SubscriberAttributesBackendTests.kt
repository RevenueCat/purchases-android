package com.revenuecat.purchases.subscriberattributes

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchaserInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.ProductInfo
import com.revenuecat.purchases.common.SubscriberAttributeError
import com.revenuecat.purchases.common.buildPurchaserInfo
import com.revenuecat.purchases.utils.Responses
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
    private var backend: Backend = Backend(
        API_KEY,
        SyncDispatcher(),
        mockClient
    )
    private var subscriberAttributesBackend = SubscriberAttributesBackend(backend)

    private var receivedError: PurchasesError? = null
    private var receivedSyncedSuccessfully: Boolean? = null
    private var receivedAttributeErrors: List<SubscriberAttributeError>? = null
    private var receivedPurchaserInfo: PurchaserInfo? = null
    private var receivedOnSuccess = false

    private val expectedOnError: (PurchasesError, Boolean, List<SubscriberAttributeError>) -> Unit =
        { error, syncedSuccessfully, attributeErrors ->
            receivedError = error
            receivedSyncedSuccessfully = syncedSuccessfully
            receivedAttributeErrors = attributeErrors
        }

    private val expectedOnErrorPostReceipt: (PurchasesError, Boolean, JSONObject?) -> Unit =
        { error, syncedSuccessfully, body ->
            receivedError = error
            receivedSyncedSuccessfully = syncedSuccessfully
            receivedAttributeErrors = body.getAttributeErrors()
        }

    private val expectedOnSuccessPostReceipt: (PurchaserInfo, body: JSONObject?) -> Unit =
        { info, body ->
            receivedPurchaserInfo = info
            receivedAttributeErrors = body.getAttributeErrors()
        }

    private val expectedOnSuccess: () -> Unit =
        {
            receivedOnSuccess = true
        }

    private val unexpectedOnError: (PurchasesError, Boolean, List<SubscriberAttributeError>) -> Unit =
        { _, _, _ ->
            fail("Shouldn't be error.")
        }

    private val unexpectedOnErrorPostReceipt: (PurchasesError, Boolean, JSONObject?) -> Unit =
        { _, _, _ ->
            fail("Shouldn't be success.")
        }

    private val unexpectedOnSuccessPostReceipt: (PurchaserInfo, body: JSONObject?) -> Unit =
        { _, _ ->
            fail("Shouldn't be success.")
        }

    private val unexpectedOnSuccess: () -> Unit =
        {
            fail("Shouldn't be success.")
        }

    @Before
    fun setup() {
        mockkStatic("com.revenuecat.purchases.common.FactoriesKt")
        receivedError = null
        receivedSyncedSuccessfully = null
        receivedAttributeErrors = null
        receivedPurchaserInfo = null
        receivedOnSuccess = false
    }

    // region posting attributes

    @Test
    fun `posting subscriber attributes works`() {
        mockResponse("/subscribers/$appUserID/attributes", 200)

        subscriberAttributesBackend.postSubscriberAttributes(
            mapOf("email" to SubscriberAttribute("email", "un@email.com").toBackendMap()),
            appUserID,
            expectedOnSuccess,
            unexpectedOnError
        )
        assertThat(receivedOnSuccess).isTrue()
    }

    @Test
    fun `posting null subscriber attributes works`() {
        mockResponse("/subscribers/$appUserID/attributes", 200)

        subscriberAttributesBackend.postSubscriberAttributes(
            mapOf("email" to SubscriberAttribute("email", null).toBackendMap()),
            appUserID,
            expectedOnSuccess,
            unexpectedOnError
        )
        assertThat(receivedOnSuccess).isTrue()
    }

    @Test
    fun `error when posting attributes`() {
        mockResponse("/subscribers/$appUserID/attributes", 0, clientException = IOException())

        subscriberAttributesBackend.postSubscriberAttributes(
            mapOf("email" to SubscriberAttribute("email", null)).toBackendMap(),
            appUserID,
            unexpectedOnSuccess,
            expectedOnError
        )

        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.NetworkError)
        assertThat(receivedSyncedSuccessfully).isFalse()
        assertThat(receivedAttributeErrors).isEmpty()
    }

    @Test
    fun `attributes validation error when posting attributes`() {
        mockResponse(
            "/subscribers/$appUserID/attributes",
            400,
            expectedResultBody = "{" +
                "'code': 7263," +
                "'message': 'Some subscriber attributes keys were unable to saved.'," +
                "'attribute_errors':" +
                "[{'key_name': 'email', 'message': 'Value is not a valid email address.'}]}"
        )

        subscriberAttributesBackend.postSubscriberAttributes(
            mapOf("email" to SubscriberAttribute("email", null)).toBackendMap(),
            appUserID,
            unexpectedOnSuccess,
            expectedOnError
        )

        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.InvalidSubscriberAttributesError)
        assertThat(receivedSyncedSuccessfully).isTrue()
        assertThat(receivedAttributeErrors!!.size).isEqualTo(1)
        assertThat(receivedAttributeErrors!![0].keyName).isEqualTo("email")
        assertThat(receivedAttributeErrors!![0].message).isEqualTo("Value is not a valid email address.")
    }

    @Test
    fun `empty attributes validation errors when posting attributes`() {
        mockResponse(
            "/subscribers/$appUserID/attributes",
            400,
            expectedResultBody = "{" +
                "'code': 7263," +
                "'message': 'Some subscriber attributes keys were unable to saved.'," +
                "'attribute_errors':[]}"
        )

        subscriberAttributesBackend.postSubscriberAttributes(
            mapOf("email" to SubscriberAttribute("email", null)).toBackendMap(),
            appUserID,
            unexpectedOnSuccess,
            expectedOnError
        )

        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.InvalidSubscriberAttributesError)
        assertThat(receivedSyncedSuccessfully).isTrue()
        assertThat(receivedAttributeErrors).isEmpty()
    }

    @Test
    fun `backend error when posting attributes`() {
        mockResponse("/subscribers/$appUserID/attributes", 503)

        subscriberAttributesBackend.postSubscriberAttributes(
            mapOf("email" to SubscriberAttribute("email", null)).toBackendMap(),
            appUserID,
            unexpectedOnSuccess,
            expectedOnError
        )

        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.UnknownBackendError)
        assertThat(receivedSyncedSuccessfully).isFalse()
        assertThat(receivedAttributeErrors).isEmpty()
    }

    // endregion

    // region posting attributes when posting receipt

    private val subscriberAttribute1 = SubscriberAttribute("key", "value")
    private val subscriberAttribute2 = SubscriberAttribute("key1", null)
    private val mapOfSubscriberAttributes = mapOf(
        "key" to subscriberAttribute1,
        "key1" to subscriberAttribute2
    ).toBackendMap()
    private val fetchToken = "fetch_token"
    private val productID = "product_id"

    @Test
    fun `posting receipt with attributes works`() {
        mockPostReceiptResponse()

        val productInfo = ProductInfo(
            productID = productID
        )
        backend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = mapOfSubscriberAttributes,
            productInfo = productInfo,
            onSuccess = expectedOnSuccessPostReceipt,
            onError = unexpectedOnErrorPostReceipt
        )

        assertThat(receivedPurchaserInfo).isNotNull
        val actualPostReceiptBody = actualPostReceiptBodySlot.captured
        assertThat(actualPostReceiptBody).isNotNull()
        assertThat(actualPostReceiptBody["attributes"]).isNotNull
    }

    @Test
    fun `posting receipt without attributes skips them`() {
        mockPostReceiptResponse()

        val productInfo = ProductInfo(
            productID = productID
        )
        backend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            productInfo = productInfo,
            onSuccess = expectedOnSuccessPostReceipt,
            onError = unexpectedOnErrorPostReceipt
        )

        assertThat(receivedPurchaserInfo).isNotNull
        assertThat(receivedAttributeErrors).isEmpty()
        val actualPostReceiptBody = actualPostReceiptBodySlot.captured
        assertThat(actualPostReceiptBody).isNotNull()
        assertThat(actualPostReceiptBody["attributes"]).isNull()
    }

    @Test
    fun `200 but subscriber attribute errors when posting receipt`() {
        mockPostReceiptResponse(
            responseCode = 200,
            responseBody = JSONObject(Responses.subscriberAttributesErrorsPostReceiptResponse)
        )
        val productInfo = ProductInfo(
            productID = productID
        )
        backend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            productInfo = productInfo,
            onSuccess = expectedOnSuccessPostReceipt,
            onError = unexpectedOnErrorPostReceipt
        )

        assertThat(receivedPurchaserInfo).isNotNull
        assertThat(receivedAttributeErrors).isNotEmpty
        assertThat(receivedAttributeErrors?.get(0)?.keyName).isEqualTo("invalid_name")
        assertThat(receivedAttributeErrors?.get(0)?.message).isEqualTo("Attribute key name is not valid.")
    }

    @Test
    fun `505 and subscriber attribute errors when posting receipt`() {
        mockPostReceiptResponse(
            responseCode = 505,
            responseBody = JSONObject(Responses.subscriberAttributesErrorsPostReceiptResponse)
        )
        val productInfo = ProductInfo(
            productID = productID
        )
        backend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            productInfo = productInfo,
            onSuccess = unexpectedOnSuccessPostReceipt,
            onError = expectedOnErrorPostReceipt
        )

        assertThat(receivedPurchaserInfo).isNull()
        assertThat(receivedAttributeErrors).isNotEmpty
        assertThat(receivedAttributeErrors?.get(0)?.keyName).isEqualTo("invalid_name")
        assertThat(receivedAttributeErrors?.get(0)?.message).isEqualTo("Attribute key name is not valid.")
    }

    @Test
    fun `304 and subscriber attribute errors when posting receipt`() {
        mockPostReceiptResponse(
            responseCode = 304,
            responseBody = JSONObject(Responses.subscriberAttributesErrorsPostReceiptResponse)
        )
        val productInfo = ProductInfo(
            productID = productID
        )
        backend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            productInfo = productInfo,
            onSuccess = unexpectedOnSuccessPostReceipt,
            onError = expectedOnErrorPostReceipt
        )

        assertThat(receivedPurchaserInfo).isNull()
        assertThat(receivedAttributeErrors).isNotEmpty
        assertThat(receivedAttributeErrors?.get(0)?.keyName).isEqualTo("invalid_name")
        assertThat(receivedAttributeErrors?.get(0)?.message).isEqualTo("Attribute key name is not valid.")
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
                (expectedBody ?: any()),
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

    private val actualPostReceiptBodySlot = slot<Map<String, Any?>>()
    private fun mockPostReceiptResponse(
        responseCode: Int = 200,
        responseBody: JSONObject = JSONObject("{}")
    ) {
        every {
            mockClient.performRequest(
                "/receipts",
                capture(actualPostReceiptBodySlot),
                mapOf("Authorization" to "Bearer $API_KEY")
            )
        } answers {
            HTTPClient.Result().also {
                it.responseCode = responseCode
                it.body = responseBody
                every {
                    it.body!!.buildPurchaserInfo()
                } returns mockk()
            }
        }
    }
}
