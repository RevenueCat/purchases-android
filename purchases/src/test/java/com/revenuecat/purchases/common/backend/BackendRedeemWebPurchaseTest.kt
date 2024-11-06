package com.revenuecat.purchases.common.backend

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
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
import com.revenuecat.purchases.interfaces.RedeemWebPurchaseListener
import com.revenuecat.purchases.utils.Responses
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
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
        performPostAndExpectResult(RedeemWebPurchaseListener.Result.Success(expectedCustomerInfo))
    }

    @Test
    fun `postRedeemWebPurchase errors propagate correctly`() {
        mockHttpResult(responseCode = RCHTTPStatusCodes.ERROR)
        performPostAndExpectResult(RedeemWebPurchaseListener.Result.Error(PurchasesError(
            PurchasesErrorCode.UnknownBackendError,
            "Backend Code: N/A - ",
        )))
    }

    @Test
    fun `postRedeemWebPurchase errors with invalid token if backend returns that code`() {
        val responseBody = """
            {
                "code": 7849,
                "message": "Invalid RevenueCat Billing redemption token."
            }
        """.trimIndent()
        mockHttpResult(responseCode = RCHTTPStatusCodes.BAD_REQUEST, responseBody = responseBody)
        performPostAndExpectResult(RedeemWebPurchaseListener.Result.InvalidToken)
    }

    @Test
    fun `postRedeemWebPurchase errors with already redeemed token if backend returns that code`() {
        val responseBody = """
            {
                "code": 7852,
                "message": "The purchase has already been redeemed."
            }
        """.trimIndent()
        mockHttpResult(responseCode = RCHTTPStatusCodes.FORBIDDEN, responseBody = responseBody)
        performPostAndExpectResult(RedeemWebPurchaseListener.Result.AlreadyRedeemed)
    }

    @Test
    fun `postRedeemWebPurchase errors with token expired if backend returns that code`() {
        val responseBody = """
            {
                "code": 7853,
                "message": "The link has expired.",
                "purchase_redemption_error_info": {
                   "obfuscated_email": "t***@r******t.com",
                   "was_email_sent": true
                }
            }
        """.trimIndent()
        mockHttpResult(responseCode = RCHTTPStatusCodes.FORBIDDEN, responseBody = responseBody)
        performPostAndExpectResult(
            RedeemWebPurchaseListener.Result.Expired(obfuscatedEmail = "t***@r******t.com", wasEmailSent = true)
        )
    }

    @Test
    fun `given multiple postRedeemWebPurchase calls for same token and user ID, only one is triggered`() {
        mockHttpResult(delayMs = 200)
        val lock = CountDownLatch(2)
        asyncBackend.postRedeemWebPurchase(
            appUserID = "test-user-id",
            redemptionToken = "test-redemption-token",
            onResultHandler = {
                assertThat(it).isEqualTo(RedeemWebPurchaseListener.Result.Success(expectedCustomerInfo))
                lock.countDown()
            },
        )
        asyncBackend.postRedeemWebPurchase(
            appUserID = "test-user-id",
            redemptionToken = "test-redemption-token",
            onResultHandler = {
                assertThat(it).isEqualTo(RedeemWebPurchaseListener.Result.Success(expectedCustomerInfo))
                lock.countDown()
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

    private fun performPostAndExpectResult(expectedResult: RedeemWebPurchaseListener.Result) {
        var receivedResult: RedeemWebPurchaseListener.Result? = null
        backend.postRedeemWebPurchase(
            appUserID = "test-user-id",
            redemptionToken = "test-redemption-token",
            onResultHandler = { receivedResult = it },
        )
        assertThat(receivedResult).isEqualTo(expectedResult)
    }

    private fun mockHttpResult(
        responseCode: Int = RCHTTPStatusCodes.SUCCESS,
        responseBody: String = Responses.validFullPurchaserResponse,
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
