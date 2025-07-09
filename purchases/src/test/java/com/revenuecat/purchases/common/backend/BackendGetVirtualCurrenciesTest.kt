package com.revenuecat.purchases.common.backend

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.SyncDispatcher
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.utils.Responses
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrenciesFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class BackendGetVirtualCurrenciesTest {

    private val mockBaseURL = URL("http://mock-api-test.revenuecat.com/")

    private lateinit var appConfig: AppConfig
    private lateinit var httpClient: HTTPClient

    private lateinit var backend: Backend
    private lateinit var asyncBackend: Backend

    private val expectedVirtualCurrencies = VirtualCurrenciesFactory.buildVirtualCurrencies(
        Responses.validFullVirtualCurrenciesResponse
    )

    @Before
    fun setUp() {
        appConfig = mockk<AppConfig>().apply {
            every { baseURL } returns mockBaseURL
            every { fallbackBaseURLs } returns emptyList()
        }
        httpClient = mockk()
        val backendHelper = BackendHelper("TEST_API_KEY", SyncDispatcher(), appConfig, httpClient)

        val asyncDispatcher1 = createAsyncDispatcher()
        val asyncDispatcher2 = createAsyncDispatcher()

        val asyncBackendHelper = BackendHelper("TEST_API_KEY", asyncDispatcher1, appConfig, httpClient)

        backend = Backend(
            appConfig,
            SyncDispatcher(),
            SyncDispatcher(),
            httpClient,
            backendHelper,
        )

        asyncBackend = Backend(
            appConfig,
            asyncDispatcher1,
            asyncDispatcher2,
            httpClient,
            asyncBackendHelper,
        )
    }


    @Test
    fun `getVirtualCurrencies gets correctly`() {
        mockHttpResult()
        var virtualCurrencies: VirtualCurrencies? = null
        backend.getVirtualCurrencies(
            appUserID = "test-user-id",
            appInBackground = false,
            onSuccess = { virtualCurrencies = it },
            onError = { error -> fail("Expected success. Got error: $error") },
        )
        assertThat(virtualCurrencies).isEqualTo(expectedVirtualCurrencies)
    }

    @Test
    fun `getVirtualCurrencies errors propagate correctly`() {
        mockHttpResult(responseCode = RCHTTPStatusCodes.ERROR)
        var obtainedError: PurchasesError? = null
        backend.getVirtualCurrencies(
            appUserID = "test-user-id",
            appInBackground = false,
            onSuccess = { fail("Expected error. Got success") },
            onError = { error -> obtainedError = error },
        )
        assertThat(obtainedError).isNotNull
        assertThat(obtainedError?.code).isEqualTo(PurchasesErrorCode.UnknownBackendError)
    }

    @Test
    fun `given multiple getVirtualCurrencies calls for same subscriber same body, only one is triggered`() {
        mockHttpResult(delayMs = 200)
        val lock = CountDownLatch(2)
        asyncBackend.getVirtualCurrencies(
            "test-user-id",
            appInBackground = false,
            onSuccess = {
                lock.countDown()
            },
            onError = {
                fail("Expected success. Got error: $it")
            },
        )
        asyncBackend.getVirtualCurrencies(
            "test-user-id",
            appInBackground = false,
            onSuccess = {
                lock.countDown()
            },
            onError = {
                fail("Expected success. Got error: $it")
            },
        )
        lock.await(5.seconds.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 1) {
            httpClient.performRequest(
                mockBaseURL,
                Endpoint.GetVirtualCurrencies("test-user-id"),
                body = null,
                postFieldsToSign = null,
                any()
            )
        }
    }

    private fun mockHttpResult(
        responseCode: Int = RCHTTPStatusCodes.SUCCESS,
        responseBody: String = Responses.validFullVirtualCurrenciesResponse,
        delayMs: Long? = null
    ) {
        every {
            httpClient.performRequest(
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } answers {
            if (delayMs != null) {
                Thread.sleep(delayMs)
            }
            HTTPResult(
                responseCode,
                responseBody,
                HTTPResult.Origin.BACKEND,
                requestDate = null,
                VerificationResult.NOT_REQUESTED
            )
        }
    }

    private fun createAsyncDispatcher(): Dispatcher {
        return Dispatcher(
            ThreadPoolExecutor(
                1,
                2,
                0,
                TimeUnit.MILLISECONDS,
                LinkedBlockingQueue()
            )
        )
    }
}