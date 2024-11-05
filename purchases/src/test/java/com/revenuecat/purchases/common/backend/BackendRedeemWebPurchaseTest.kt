package com.revenuecat.purchases.common.backend

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.SyncDispatcher
import com.revenuecat.purchases.common.createCustomerInfo
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.utils.Responses
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
class BackendRedeemWebPurchaseTest {

    private val mockBaseURL = URL("http://mock-api-test.revenuecat.com/")

    private lateinit var appConfig: AppConfig
    private lateinit var httpClient: HTTPClient

    private lateinit var backend: Backend
    private lateinit var asyncBackend: Backend

    private val expectedCustomerInfo = createCustomerInfo(Responses.validFullPurchaserResponse)

    @Before
    fun setUp() {
        appConfig = mockk<AppConfig>().apply {
            every { baseURL } returns mockBaseURL
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
    fun `postRedeemWebPurchase posts correctly`() {
        mockHttpResult()
        var receivedCustomerInfo: CustomerInfo? = null
        backend.postRedeemWebPurchase(
            appUserID = "test-user-id",
            redemptionToken = "test-redemption-token",
            onSuccessHandler = { receivedCustomerInfo = it },
            onErrorHandler = { error -> fail("Expected success. Got error: $error") },
        )
        assertThat(receivedCustomerInfo).isEqualTo(expectedCustomerInfo)
    }

    @Test
    fun `postRedeemWebPurchase errors propagate correctly`() {
        mockHttpResult(responseCode = RCHTTPStatusCodes.ERROR)
        var obtainedError: PurchasesError? = null
        backend.postRedeemWebPurchase(
            appUserID = "test-user-id",
            redemptionToken = "test-redemption-token",
            onSuccessHandler = { fail("Expected error. Got success") },
            onErrorHandler = { error -> obtainedError = error },
        )
        assertThat(obtainedError).isNotNull
    }

    @Test
    fun `given multiple postRedeemWebPurchase calls for same token and user ID, only one is triggered`() {
        mockHttpResult(delayMs = 200)
        val lock = CountDownLatch(2)
        asyncBackend.postRedeemWebPurchase(
            appUserID = "test-user-id",
            redemptionToken = "test-redemption-token",
            onSuccessHandler = {
                lock.countDown()
            },
            onErrorHandler = {
                fail("Expected success. Got error: $it")
            },
        )
        asyncBackend.postRedeemWebPurchase(
            appUserID = "test-user-id",
            redemptionToken = "test-redemption-token",
            onSuccessHandler = {
                lock.countDown()
            },
            onErrorHandler = {
                fail("Expected success. Got error: $it")
            },
        )
        lock.await(5.seconds.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 1) {
            httpClient.performRequest(
                mockBaseURL,
                Endpoint.PostRedeemWebPurchase,
                body = mapOf("redemption_token" to "test-redemption-token", "app_user_id" to "test-user-id"),
                postFieldsToSign = null,
                any()
            )
        }
    }

    private fun mockHttpResult(
        responseCode: Int = RCHTTPStatusCodes.SUCCESS,
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
                Responses.validFullPurchaserResponse,
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
