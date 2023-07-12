package com.revenuecat.purchases.subscriberattributes

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.CustomerInfoFactory
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.PostReceiptDataErrorCallback
import com.revenuecat.purchases.common.PostReceiptErrorHandlingBehavior
import com.revenuecat.purchases.common.ReceiptInfo
import com.revenuecat.purchases.common.SubscriberAttributeError
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.utils.Responses
import com.revenuecat.purchases.utils.SyncDispatcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.net.URL

private const val API_KEY = "TEST_API_KEY"

@RunWith(AndroidJUnit4::class)
class SubscriberAttributesPosterTests {
    private var mockClient: HTTPClient = mockk(relaxed = true)
    private val mockBaseURL = URL("http://mock-api-test.revenuecat.com/")
    private val mockAppConfig = mockk<AppConfig>().apply {
        every { baseURL } returns mockBaseURL
    }
    private val appUserID = "jerry"
    private val dispatcher = SyncDispatcher()
    private var backendHelper = BackendHelper(
        API_KEY,
        dispatcher,
        mockAppConfig,
        mockClient
    )
    private var backend: Backend = Backend(
        mockAppConfig,
        dispatcher,
        dispatcher,
        mockClient,
        backendHelper
    )
    private var subscriberAttributesPoster = SubscriberAttributesPoster(backendHelper)

    private var receivedError: PurchasesError? = null
    private var receivedSyncedSuccessfully: Boolean? = null
    private var receivedPostReceiptErrorHandlingBehavior: PostReceiptErrorHandlingBehavior? = null
    private var receivedAttributeErrors: List<SubscriberAttributeError>? = null
    private var receivedCustomerInfo: CustomerInfo? = null
    private var receivedOnSuccess = false

    private val expectedOnError: (PurchasesError, Boolean, List<SubscriberAttributeError>) -> Unit =
        { error, syncedSuccessfully, attributeErrors ->
            receivedError = error
            receivedSyncedSuccessfully = syncedSuccessfully
            receivedAttributeErrors = attributeErrors
        }

    private val expectedOnErrorPostReceipt: PostReceiptDataErrorCallback =
        { error, errorHandlingBehavior, body ->
            receivedError = error
            receivedPostReceiptErrorHandlingBehavior = errorHandlingBehavior
            receivedAttributeErrors = body.getAttributeErrors()
        }

    private val expectedOnSuccessPostReceipt: (CustomerInfo, body: JSONObject?) -> Unit =
        { info, body ->
            receivedCustomerInfo = info
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

    private val unexpectedOnErrorPostReceipt: PostReceiptDataErrorCallback =
        { _, _, _ ->
            fail("Shouldn't be success.")
        }

    private val unexpectedOnSuccessPostReceipt: (CustomerInfo, body: JSONObject?) -> Unit =
        { _, _ ->
            fail("Shouldn't be success.")
        }

    private val unexpectedOnSuccess: () -> Unit =
        {
            fail("Shouldn't be success.")
        }

    @Before
    fun setup() {
        mockkObject(CustomerInfoFactory)
        receivedError = null
        receivedSyncedSuccessfully = null
        receivedPostReceiptErrorHandlingBehavior = null
        receivedAttributeErrors = null
        receivedCustomerInfo = null
        receivedOnSuccess = false
    }

    @After
    fun tearDown() {
        unmockkObject(CustomerInfoFactory)
    }

    // region posting attributes

    @Test
    fun `posting subscriber attributes works`() {
        mockResponse(200)

        subscriberAttributesPoster.postSubscriberAttributes(
            mapOf("email" to SubscriberAttribute("email", "un@email.com").toBackendMap()),
            appUserID,
            expectedOnSuccess,
            unexpectedOnError
        )
        assertThat(receivedOnSuccess).isTrue()
    }

    @Test
    fun `posting null subscriber attributes works`() {
        mockResponse(200)

        subscriberAttributesPoster.postSubscriberAttributes(
            mapOf("email" to SubscriberAttribute("email", null).toBackendMap()),
            appUserID,
            expectedOnSuccess,
            unexpectedOnError
        )
        assertThat(receivedOnSuccess).isTrue()
    }

    @Test
    fun `error when posting attributes`() {
        mockResponse(0, clientException = IOException())

        subscriberAttributesPoster.postSubscriberAttributes(
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
            400,
            expectedResultBody = "{" +
                "'code': 7263," +
                "'message': 'Some subscriber attributes keys were unable to saved.'," +
                "'attribute_errors':" +
                "[{'key_name': 'email', 'message': 'Value is not a valid email address.'}]}"
        )

        subscriberAttributesPoster.postSubscriberAttributes(
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
            400,
            expectedResultBody = "{" +
                "'code': 7263," +
                "'message': 'Some subscriber attributes keys were unable to saved.'," +
                "'attribute_errors':[]}"
        )

        subscriberAttributesPoster.postSubscriberAttributes(
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
        mockResponse(503)

        subscriberAttributesPoster.postSubscriberAttributes(
            mapOf("email" to SubscriberAttribute("email", null)).toBackendMap(),
            appUserID,
            unexpectedOnSuccess,
            expectedOnError
        )

        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.UnknownBackendError)
        assertThat(receivedSyncedSuccessfully).isFalse()
        assertThat(receivedAttributeErrors).isEmpty()
    }

    @Test
    fun `Not found error when posting attributes`() {
        mockResponse(
            404,
            expectedResultBody = "{" +
                "'code': 7259," +
                "'message': 'Subscription not found for subscriber.'}"
        )

        subscriberAttributesPoster.postSubscriberAttributes(
            mapOf("email" to SubscriberAttribute("email", null)).toBackendMap(),
            appUserID,
            unexpectedOnSuccess,
            expectedOnError
        )

        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.UnknownBackendError)
        assertThat(receivedSyncedSuccessfully).isFalse()
        assertThat(receivedAttributeErrors!!.size).isEqualTo(0)
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

        val productInfo = ReceiptInfo(
            productIDs = listOf(productID)
        )
        backend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = mapOfSubscriberAttributes,
            receiptInfo = productInfo,
            storeAppUserID = null,
            onSuccess = expectedOnSuccessPostReceipt,
            onError = unexpectedOnErrorPostReceipt
        )

        assertThat(receivedCustomerInfo).isNotNull
        val actualPostReceiptBody = actualPostReceiptBodySlot.captured
        assertThat(actualPostReceiptBody).isNotNull()
        assertThat(actualPostReceiptBody["attributes"]).isNotNull
    }

    @Test
    fun `posting receipt without attributes skips them`() {
        mockPostReceiptResponse()

        val productInfo = ReceiptInfo(
            productIDs = listOf(productID)
        )
        backend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            receiptInfo = productInfo,
            storeAppUserID = null,
            onSuccess = expectedOnSuccessPostReceipt,
            onError = unexpectedOnErrorPostReceipt
        )

        assertThat(receivedCustomerInfo).isNotNull
        assertThat(receivedAttributeErrors).isEmpty()
        val actualPostReceiptBody = actualPostReceiptBodySlot.captured
        assertThat(actualPostReceiptBody).isNotNull()
        assertThat(actualPostReceiptBody["attributes"]).isNull()
    }

    @Test
    fun `200 but subscriber attribute errors when posting receipt`() {
        mockPostReceiptResponse(
            responseCode = 200,
            responseBody = Responses.subscriberAttributesErrorsPostReceiptResponse
        )
        val productInfo = ReceiptInfo(
            productIDs = listOf(productID)
        )
        backend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            receiptInfo = productInfo,
            storeAppUserID = null,
            onSuccess = expectedOnSuccessPostReceipt,
            onError = unexpectedOnErrorPostReceipt
        )

        assertThat(receivedCustomerInfo).isNotNull
        assertThat(receivedAttributeErrors).isNotEmpty
        assertThat(receivedAttributeErrors?.get(0)?.keyName).isEqualTo("invalid_name")
        assertThat(receivedAttributeErrors?.get(0)?.message).isEqualTo("Attribute key name is not valid.")
    }

    @Test
    fun `505 and subscriber attribute errors when posting receipt`() {
        mockPostReceiptResponse(
            responseCode = 505,
            responseBody = Responses.subscriberAttributesErrorsPostReceiptResponse
        )
        val productInfo = ReceiptInfo(
            productIDs = listOf(productID)
        )
        backend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            receiptInfo = productInfo,
            storeAppUserID = null,
            onSuccess = unexpectedOnSuccessPostReceipt,
            onError = expectedOnErrorPostReceipt
        )

        assertThat(receivedCustomerInfo).isNull()
        assertThat(receivedAttributeErrors).isNotEmpty
        assertThat(receivedAttributeErrors?.get(0)?.keyName).isEqualTo("invalid_name")
        assertThat(receivedAttributeErrors?.get(0)?.message).isEqualTo("Attribute key name is not valid.")
    }

    @Test
    fun `304 and subscriber attribute errors when posting receipt`() {
        mockPostReceiptResponse(
            responseCode = 304,
            responseBody = Responses.subscriberAttributesErrorsPostReceiptResponse
        )
        val productInfo = ReceiptInfo(
            productIDs = listOf(productID)
        )
        backend.postReceiptData(
            purchaseToken = fetchToken,
            appUserID = appUserID,
            isRestore = false,
            observerMode = false,
            subscriberAttributes = emptyMap(),
            receiptInfo = productInfo,
            storeAppUserID = null,
            onSuccess = unexpectedOnSuccessPostReceipt,
            onError = expectedOnErrorPostReceipt
        )

        assertThat(receivedCustomerInfo).isNull()
        assertThat(receivedAttributeErrors).isNotEmpty
        assertThat(receivedAttributeErrors?.get(0)?.keyName).isEqualTo("invalid_name")
        assertThat(receivedAttributeErrors?.get(0)?.message).isEqualTo("Attribute key name is not valid.")
    }
    // endregion

    private fun mockResponse(
        responseCode: Int,
        expectedBody: Map<String, Any?>? = null,
        clientException: Exception? = null,
        expectedResultBody: String? = null
    ) {
        val everyMockedCall = every {
            mockClient.performRequest(
                mockBaseURL,
                Endpoint.PostAttributes(appUserID),
                (expectedBody ?: any()),
                postFieldsToSign = null,
                mapOf("Authorization" to "Bearer $API_KEY")
            )
        }

        if (clientException == null) {
            everyMockedCall answers {
                createResult(responseCode, expectedResultBody ?: "{}")
            }
        } else {
            everyMockedCall throws clientException
        }
    }

    private val actualPostReceiptBodySlot = slot<Map<String, Any?>>()
    private fun mockPostReceiptResponse(
        responseCode: Int = 200,
        responseBody: String = "{}",
    ) {
        every {
            mockClient.performRequest(
                mockBaseURL,
                Endpoint.PostReceipt,
                capture(actualPostReceiptBodySlot),
                any(),
                mapOf("Authorization" to "Bearer $API_KEY")
            )
        } answers {
            createResult(responseCode, responseBody).also {
                every {
                    CustomerInfoFactory.buildCustomerInfo(it)
                } returns mockk()
            }
        }
    }

    private fun createResult(
        responseCode: Int,
        responseBody: String
    ) = HTTPResult(responseCode, responseBody, HTTPResult.Origin.BACKEND, null, VerificationResult.NOT_REQUESTED)
}
