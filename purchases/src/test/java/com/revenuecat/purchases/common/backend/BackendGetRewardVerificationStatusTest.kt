package com.revenuecat.purchases.common.backend

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.RewardVerificationStatus
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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
@OptIn(InternalRevenueCatAPI::class)
class BackendGetRewardVerificationStatusTest {

    private val appUserId = "user_1"
    private val clientTransactionId = "client_transaction_id_1"
    private val mockBaseURL = URL("http://mock-api-test.revenuecat.com/")

    private lateinit var appConfig: AppConfig
    private lateinit var httpClient: HTTPClient

    private lateinit var backend: Backend
    private lateinit var asyncBackend: Backend

    @Before
    fun setUp() {
        appConfig = mockk<AppConfig>().apply {
            every { baseURL } returns mockBaseURL
            every { customEntitlementComputation } returns false
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
    fun `getRewardVerificationStatus parses successful response`() {
        mockHttpResult(payload = """{"status":"verified"}""")

        var receivedStatus: RewardVerificationStatus? = null
        backend.getRewardVerificationStatus(
            appUserID = appUserId,
            clientTransactionId = clientTransactionId,
            onSuccess = { receivedStatus = it },
            onError = { error -> fail("Expected success. Got error: $error") },
        )

        assertThat(receivedStatus).isEqualTo(RewardVerificationStatus.VERIFIED)
        verify(exactly = 1) {
            httpClient.performRequest(
                mockBaseURL,
                Endpoint.GetRewardVerificationStatus(appUserId, clientTransactionId),
                body = null,
                postFieldsToSign = null,
                requestHeaders = any(),
            )
        }
    }

    @Test
    fun `getRewardVerificationStatus maps unknown statuses`() {
        mockHttpResult(payload = """{"status":"new_status"}""")

        var receivedStatus: RewardVerificationStatus? = null
        backend.getRewardVerificationStatus(
            appUserID = appUserId,
            clientTransactionId = clientTransactionId,
            onSuccess = { receivedStatus = it },
            onError = { error -> fail("Expected success. Got error: $error") },
        )

        assertThat(receivedStatus).isEqualTo(RewardVerificationStatus.UNKNOWN)
    }

    @Test
    fun `getRewardVerificationStatus returns error on malformed payload`() {
        mockHttpResult(payload = """{"status":1}""")
        var obtainedError: PurchasesError? = null
        backend.getRewardVerificationStatus(
            appUserID = appUserId,
            clientTransactionId = clientTransactionId,
            onSuccess = { fail("Expected error. Got success") },
            onError = { error -> obtainedError = error },
        )

        assertThat(obtainedError).isNotNull
        assertThat(obtainedError?.code).isEqualTo(PurchasesErrorCode.UnknownError)
    }

    @Test
    fun `getRewardVerificationStatus propagates HTTP errors`() {
        mockHttpResult(
            responseCode = RCHTTPStatusCodes.ERROR,
            payload = """{"code": 7000, "message": "internal error"}""",
        )
        var obtainedError: PurchasesError? = null
        backend.getRewardVerificationStatus(
            appUserID = appUserId,
            clientTransactionId = clientTransactionId,
            onSuccess = { fail("Expected error. Got success") },
            onError = { error -> obtainedError = error },
        )

        assertThat(obtainedError).isNotNull
    }

    @Test
    fun `getRewardVerificationStatus dedups concurrent calls`() {
        mockHttpResult(payload = """{"status":"pending"}""", delayMs = 200)
        val lock = CountDownLatch(2)
        asyncBackend.getRewardVerificationStatus(
            appUserID = appUserId,
            clientTransactionId = clientTransactionId,
            onSuccess = { lock.countDown() },
            onError = { fail("Expected success. Got error: $it") },
        )
        asyncBackend.getRewardVerificationStatus(
            appUserID = appUserId,
            clientTransactionId = clientTransactionId,
            onSuccess = { lock.countDown() },
            onError = { fail("Expected success. Got error: $it") },
        )
        lock.await(5.seconds.inWholeSeconds, TimeUnit.SECONDS)
        assertThat(lock.count).isEqualTo(0)
        verify(exactly = 1) {
            httpClient.performRequest(
                mockBaseURL,
                Endpoint.GetRewardVerificationStatus(appUserId, clientTransactionId),
                body = null,
                postFieldsToSign = null,
                requestHeaders = any(),
            )
        }
    }

    private fun mockHttpResult(
        responseCode: Int = RCHTTPStatusCodes.SUCCESS,
        payload: String,
        delayMs: Long? = null,
    ) {
        every {
            httpClient.performRequest(
                any(),
                any(),
                any(),
                any(),
                any(),
                fallbackBaseURLs = any(),
            )
        } answers {
            if (delayMs != null) {
                Thread.sleep(delayMs)
            }
            HTTPResult(
                responseCode,
                payload,
                HTTPResult.Origin.BACKEND,
                requestDate = null,
                VerificationResult.NOT_REQUESTED,
                isLoadShedderResponse = false,
                isFallbackURL = false,
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
                LinkedBlockingQueue(),
            ),
        )
    }
}
