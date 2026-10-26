package com.revenuecat.purchases.common.backend

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.SyncDispatcher
import com.revenuecat.purchases.common.createResult
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.utils.Responses
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException
import java.net.URL

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class BackendSpendVirtualCurrenciesTest {

    private val apiKey = "test-api-key"
    private val appUserID = "jerry"
    private val baseURL = URL("http://mock-api-test.revenuecat.com/")
    private val appConfig = mockk<AppConfig>().apply {
        every { baseURL } returns this@BackendSpendVirtualCurrenciesTest.baseURL
        every { fallbackBaseURLs } returns emptyList()
    }
    private val httpClient = mockk<HTTPClient>()
    private val bodySlot = slot<Map<String, Any?>>()
    private val headersSlot = mutableListOf<Map<String, String>>()
    private lateinit var backend: Backend

    private var received: VirtualCurrencies? = null
    private var receivedError: PurchasesError? = null

    @Before
    fun setUp() {
        val dispatcher = SyncDispatcher()
        val helper = BackendHelper(apiKey, dispatcher, appConfig, httpClient)
        backend = Backend(appConfig, dispatcher, dispatcher, httpClient, helper)
    }

    @Test
    fun `spend posts the adjustments and reference with the user's IAM credentials`() {
        respond(HTTPResult.createResult(payload = Responses.validFullVirtualCurrenciesResponse))

        spend(mapOf("COIN" to 3, "GEM" to 1), reference = "order-1")

        assertThat(bodySlot.captured).isEqualTo(
            mapOf("adjustments" to mapOf("COIN" to 3, "GEM" to 1), "reference" to "order-1"),
        )
        verify {
            httpClient.performRequest(
                baseURL,
                Endpoint.SpendVirtualCurrencies,
                any(),
                null,
                any(),
                iamAppUserID = appUserID,
            )
        }
        assertThat(received?.all?.keys).containsExactlyInAnyOrder("COIN", "RC_COIN")
        assertThat(receivedError).isNull()
    }

    @Test
    fun `spend omits a null reference`() {
        respond(HTTPResult.createResult(payload = Responses.validFullVirtualCurrenciesResponse))

        spend(mapOf("COIN" to 3), reference = null)

        assertThat(bodySlot.captured).isEqualTo(mapOf("adjustments" to mapOf("COIN" to 3)))
    }

    @Test
    fun `each spend sends its own idempotency key alongside the API key`() {
        respond(HTTPResult.createResult(payload = Responses.validFullVirtualCurrenciesResponse))

        spend(mapOf("COIN" to 1), reference = null)
        spend(mapOf("COIN" to 1), reference = null)

        assertThat(headersSlot).hasSize(2)
        headersSlot.forEach { assertThat(it).containsEntry("Authorization", "Bearer $apiKey") }
        val keys = headersSlot.map { it["Idempotency-Key"] }
        assertThat(keys).doesNotContainNull()
        assertThat(keys.toSet()).hasSize(2)
    }

    @Test
    fun `identical spends are not deduplicated`() {
        respond(HTTPResult.createResult(payload = Responses.validFullVirtualCurrenciesResponse))

        spend(mapOf("COIN" to 1), reference = "same")
        spend(mapOf("COIN" to 1), reference = "same")

        verify(exactly = 2) {
            httpClient.performRequest(any(), Endpoint.SpendVirtualCurrencies, any(), any(), any(), iamAppUserID = any())
        }
    }

    @Test
    fun `a backend error reaches the error handler`() {
        respond(HTTPResult.createResult(responseCode = RCHTTPStatusCodes.BAD_REQUEST, payload = "{}"))

        spend(mapOf("COIN" to 1), reference = null)

        assertThat(received).isNull()
        assertThat(receivedError).isNotNull
    }

    @Test
    fun `a network error reaches the error handler`() {
        every {
            httpClient.performRequest(any(), any(), any(), any(), any(), iamAppUserID = any())
        } throws IOException()

        spend(mapOf("COIN" to 1), reference = null)

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.NetworkError)
    }

    @Test
    fun `an undecodable response reaches the error handler`() {
        respond(HTTPResult.createResult(payload = "{\"virtual_currencies\": 5}"))

        spend(mapOf("COIN" to 1), reference = null)

        assertThat(received).isNull()
        assertThat(receivedError).isNotNull
    }

    private fun respond(result: HTTPResult) {
        every {
            httpClient.performRequest(
                any(),
                any(),
                capture(bodySlot),
                any(),
                capture(headersSlot),
                iamAppUserID = any(),
            )
        } returns result
    }

    private fun spend(amounts: Map<String, Int>, reference: String?) {
        backend.spendVirtualCurrencies(
            appUserID,
            amounts,
            reference,
            onSuccessHandler = { received = it },
            onErrorHandler = { receivedError = it },
        )
    }
}
