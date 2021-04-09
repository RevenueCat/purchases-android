package com.revenuecat.purchases.amazon

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.amazon.helpers.successfulRVSResponse
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.utils.SyncDispatcher
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

private const val API_KEY = "TEST_API_KEY"

@RunWith(AndroidJUnit4::class)
class AmazonBackendTest {

    private var mockClient = mockk<HTTPClient>()

    private var backend: Backend = Backend(
        API_KEY,
        SyncDispatcher(),
        mockClient
    )

    private var receivedOnSuccess: JSONObject? = null
    private var receivedError: PurchasesError? = null

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

    private var underTest = AmazonBackend(backend)

    private var successfulResult = HTTPResult(
        responseCode = 200,
        payload = successfulRVSResponse()
    )
    private var unsuccessfulResult = HTTPResult(
        responseCode = 401,
        payload = """
                {
                    "code":7225,"message":
                    "Invalid API Key."
                }
            """.trimIndent()
    )

    @Test
    fun `When getting Amazon receipt data is successful, onSuccess is called`() {
        every {
            mockClient.performRequest(
                path = "/receipts/amazon/store_user_id/receipt_id",
                body = null,
                authenticationHeaders = mapOf("Authorization" to "Bearer $API_KEY")
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
                path = "/receipts/amazon/store_user_id/receipt_id",
                body = null,
                authenticationHeaders = mapOf("Authorization" to "Bearer $API_KEY")
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
                path = "/receipts/amazon/store_user_id/receipt_id",
                body = null,
                authenticationHeaders = mapOf("Authorization" to "Bearer $API_KEY")
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
