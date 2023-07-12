package com.revenuecat.purchases.amazon

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.amazon.helpers.successfulRVSResponse
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.utils.SyncDispatcher
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.net.URL

private const val API_KEY = "TEST_API_KEY"

@RunWith(AndroidJUnit4::class)
class AmazonBackendTest {

    private lateinit var mockClient: HTTPClient
    private lateinit var mockAppConfig: AppConfig
    private lateinit var dispatcher: SyncDispatcher
    private lateinit var backendHelper: BackendHelper

    private val baseURL = URL("http://mock-api-test.revenuecat.com/")

    private var receivedOnSuccess: JSONObject? = null
    private var receivedError: PurchasesError? = null
    private lateinit var underTest: AmazonBackend

    @Before
    fun setup() {
        mockClient = mockk()
        mockAppConfig = mockk<AppConfig>().apply {
            every { baseURL } returns this@AmazonBackendTest.baseURL
        }
        dispatcher = SyncDispatcher()
        backendHelper = BackendHelper(API_KEY, dispatcher, mockAppConfig, mockClient)
        receivedOnSuccess = null
        receivedError = null
        underTest = AmazonBackend(backendHelper)
    }

    @After
    fun tearDown() {
        receivedOnSuccess = null
        receivedError = null
        clearAllMocks()
    }

    private val expectedOnSuccess: (JSONObject) -> Unit = {
        receivedOnSuccess = it
    }

    private val unexpectedOnSuccess: (JSONObject) -> Unit = {
        Assertions.fail("Shouldn't be success.")
    }

    private val expectedOnError: (PurchasesError) -> Unit = {
        receivedError = it
    }

    private val unexpectedOnError: (PurchasesError) -> Unit = { _ ->
        Assertions.fail("Shouldn't be error.")
    }

    private var successfulResult = HTTPResult(
        responseCode = 200,
        payload = successfulRVSResponse(),
        origin = HTTPResult.Origin.BACKEND,
        requestDate = null,
        verificationResult = VerificationResult.NOT_REQUESTED
    )
    private var unsuccessfulResult = HTTPResult(
        responseCode = 401,
        payload = """
                {
                    "code":7225,"message":
                    "Invalid API Key."
                }
            """.trimIndent(),
        origin = HTTPResult.Origin.BACKEND,
        requestDate = null,
        verificationResult = VerificationResult.NOT_REQUESTED
    )

    @Test
    fun `When getting Amazon receipt data is successful, onSuccess is called`() {
        every {
            mockClient.performRequest(
                baseURL = baseURL,
                endpoint = Endpoint.GetAmazonReceipt("store_user_id", "receipt_id"),
                body = null,
                postFieldsToSign = null,
                requestHeaders = mapOf("Authorization" to "Bearer $API_KEY")
            )
        } returns successfulResult

        underTest.getAmazonReceiptData(
            receiptId = "receipt_id",
            storeUserID = "store_user_id",
            onSuccess = expectedOnSuccess,
            onError = unexpectedOnError
        )

        assertThat(receivedOnSuccess!!).isNotNull
    }

    @Test
    fun `when Amazon receipt data call returns an error, errors are passed along`() {
        every {
            mockClient.performRequest(
                baseURL = baseURL,
                endpoint = Endpoint.GetAmazonReceipt("store_user_id", "receipt_id"),
                body = null,
                postFieldsToSign = null,
                requestHeaders = mapOf("Authorization" to "Bearer $API_KEY")
            )
        } returns unsuccessfulResult

        underTest.getAmazonReceiptData(
            receiptId = "receipt_id",
            storeUserID = "store_user_id",
            onSuccess = unexpectedOnSuccess,
            onError = expectedOnError
        )

        assertThat(receivedError!!).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.InvalidCredentialsError)
    }

    @Test
    fun `when Amazon receipt data call fails, errors are passed along`() {
        every {
            mockClient.performRequest(
                baseURL = baseURL,
                endpoint = Endpoint.GetAmazonReceipt("store_user_id", "receipt_id"),
                body = null,
                postFieldsToSign = null,
                requestHeaders = mapOf("Authorization" to "Bearer $API_KEY")
            )
        } throws IOException()

        underTest.getAmazonReceiptData(
            receiptId = "receipt_id",
            storeUserID = "store_user_id",
            onSuccess = unexpectedOnSuccess,
            onError = expectedOnError
        )

        assertThat(receivedError!!).isNotNull
        assertThat(receivedError!!.code).isEqualTo(PurchasesErrorCode.NetworkError)
    }
}
