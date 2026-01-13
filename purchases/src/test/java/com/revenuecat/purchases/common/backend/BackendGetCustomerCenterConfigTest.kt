package com.revenuecat.purchases.common.backend

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
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
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.HelpPath
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.Screen
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.Screen.ScreenType
import com.revenuecat.purchases.customercenter.RCColor
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class BackendGetCustomerCenterConfigTest {

    private val mockBaseURL = URL("http://mock-api-test.revenuecat.com/")
    private val MOCK_RESPONSE_FILENAME = "get_customer_center_config_success.json"

    private lateinit var appConfig: AppConfig
    private lateinit var httpClient: HTTPClient

    private lateinit var backend: Backend
    private lateinit var asyncBackend: Backend

    private val expectedCustomerCenterConfigData = CustomerCenterConfigData(
        screens = mapOf(
            ScreenType.MANAGEMENT to Screen(
                type = Screen.ScreenType.MANAGEMENT,
                title = "How can we help?",
                paths = listOf(
                    HelpPath(
                        id = "ownmsldfow",
                        title = "Didn't receive purchase",
                        type = HelpPath.PathType.MISSING_PURCHASE
                    ),
                    HelpPath(
                        id = "nwodkdnfaoeb",
                        title = "Request a refund",
                        type = HelpPath.PathType.REFUND_REQUEST,
                        promotionalOffer = HelpPath.PathDetail.PromotionalOffer(
                            androidOfferId = "rc-refund-offer",
                            eligible = true,
                            title = "Wait!",
                            subtitle = "Before you go, here's a one-time offer to continue at a discount.",
                            productMapping = mapOf("monthly_subscription" to "rc-refund-offer"),
                            crossProductPromotions = mapOf(
                                "monthly_subscription" to HelpPath.PathDetail.PromotionalOffer.CrossProductPromotion(
                                    storeOfferIdentifier = "rc-refund-offer",
                                    targetProductId = "yearly_subscription",
                                )
                            )
                        )
                    ),
                    HelpPath(
                        id = "nfoaiodifj9",
                        title = "Change plans",
                        type = HelpPath.PathType.CHANGE_PLANS
                    ),
                    HelpPath(
                        id = "jnkasldfhas",
                        title = "Cancel subscription",
                        type = HelpPath.PathType.CANCEL,
                        feedbackSurvey = HelpPath.PathDetail.FeedbackSurvey(
                            title = "Why are you cancelling?",
                            options = listOf(
                                HelpPath.PathDetail.FeedbackSurvey.Option(
                                    id = "cancel_survey_too_expensive",
                                    title = "Too expensive",
                                    promotionalOffer = HelpPath.PathDetail.PromotionalOffer(
                                        androidOfferId = "rc-cancel-offer",
                                        eligible = true,
                                        title = "Wait!",
                                        subtitle = "Before you go, here's a one-time offer to continue at a discount.",
                                        productMapping = mapOf("monthly_subscription" to "rc-cancel-offer"),
                                        crossProductPromotions = mapOf(
                                            "monthly_subscription" to HelpPath.PathDetail.PromotionalOffer.CrossProductPromotion(
                                                storeOfferIdentifier = "rc-cancel-offer",
                                                targetProductId = "yearly_subscription",
                                            )
                                        )
                                    )
                                ),
                                HelpPath.PathDetail.FeedbackSurvey.Option(
                                    id = "cancel_survey_usage",
                                    title = "Don't use the app",
                                    promotionalOffer = HelpPath.PathDetail.PromotionalOffer(
                                        androidOfferId = "rc-cancel-offer",
                                        eligible = true,
                                        title = "Wait!",
                                        subtitle = "Before you go, here's a one-time offer to continue at a discount.",
                                        productMapping = mapOf("monthly_subscription" to "rc-cancel-offer"),
                                        crossProductPromotions = mapOf(
                                            "monthly_subscription" to HelpPath.PathDetail.PromotionalOffer.CrossProductPromotion(
                                                storeOfferIdentifier = "rc-cancel-offer",
                                                targetProductId = "yearly_subscription",
                                            )
                                        )
                                    )
                                ),
                                HelpPath.PathDetail.FeedbackSurvey.Option(
                                    id = "cancel_survey_mistake",
                                    title = "Bought by mistake"
                                )
                            )
                        )
                    ),
                    HelpPath(
                        id = "path_ZD-yiHSBN",
                        title = "RevenueCat",
                        type = HelpPath.PathType.CUSTOM_URL,
                        promotionalOffer = null,
                        feedbackSurvey = null,
                        url = "https://revenuecat.com",
                        openMethod = HelpPath.OpenMethod.EXTERNAL,
                    ),
                )
            ),
            Screen.ScreenType.NO_ACTIVE to Screen(
                type = Screen.ScreenType.NO_ACTIVE,
                title = "No subscriptions found",
                subtitle = "You currently have no active subscriptions",
                paths = listOf(
                    HelpPath(
                        id = "9q9719171o",
                        title = "Check purchases",
                        type = HelpPath.PathType.MISSING_PURCHASE
                    ),
                    HelpPath(
                        id = "path_ZD-yiHSDN",
                        title = "RevenueCat",
                        type = HelpPath.PathType.CUSTOM_URL,
                        promotionalOffer = null,
                        feedbackSurvey = null,
                        url = "https://revenuecat.com",
                        openMethod = HelpPath.OpenMethod.EXTERNAL,
                    ),
                )
            )
        ),
        appearance = CustomerCenterConfigData.Appearance(
            light = CustomerCenterConfigData.Appearance.ColorInformation(
                accentColor = RCColor("#a28339"),
                textColor = RCColor("#000000"),
                backgroundColor = RCColor("#ffffff"),
                buttonTextColor = RCColor("#ffffff"),
                buttonBackgroundColor = RCColor("#000000")
            ),
            dark = CustomerCenterConfigData.Appearance.ColorInformation(
                accentColor = RCColor("#F4A900"),
                textColor = RCColor("#ffffff"),
                backgroundColor = RCColor("#000000"),
                buttonTextColor = RCColor("#000000"),
                buttonBackgroundColor = RCColor("#ffffff")
            )
        ),
        localization = CustomerCenterConfigData.Localization(
            locale = "en",
            localizedStrings = mapOf(
                "amazon_subscription_manage" to "You can manage your subscription in the Amazon Appstore app on an Amazon device.",
                "done" to "Done",
            )
        ),
        support = CustomerCenterConfigData.Support(
            email = "support@revenuecat.com",
            shouldWarnCustomerToUpdate = true,
            supportTickets = CustomerCenterConfigData.Support.SupportTickets(),
        ),
        lastPublishedAppVersion = null
    )

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
    fun `getCustomerCenterConfigData gets correctly`() {
        mockHttpResult()
        var customerCenterConfigData: CustomerCenterConfigData? = null
        backend.getCustomerCenterConfig(
            appUserID = "test-user-id",
            onSuccessHandler = { customerCenterConfigData = it },
            onErrorHandler = { error -> fail("Expected success. Got error: $error") },
        )
        assertThat(customerCenterConfigData).isEqualTo(expectedCustomerCenterConfigData)
    }

    @Test
    fun `getCustomerCenterConfigData errors propagate correctly`() {
        mockHttpResult(responseCode = RCHTTPStatusCodes.ERROR)
        var obtainedError: PurchasesError? = null
        backend.getCustomerCenterConfig(
            appUserID = "test-user-id",
            onSuccessHandler = { fail("Expected error. Got success") },
            onErrorHandler = { error -> obtainedError = error },
        )
        assertThat(obtainedError).isNotNull
    }

    @Test
    fun `given multiple getCustomerCenterConfig calls for same subscriber same body, only one is triggered`() {
        mockHttpResult(delayMs = 200)
        val lock = CountDownLatch(2)
        asyncBackend.getCustomerCenterConfig(
            "test-user-id",
            onSuccessHandler = {
                lock.countDown()
            },
            onErrorHandler = {
                fail("Expected success. Got error: $it")
            },
        )
        asyncBackend.getCustomerCenterConfig(
            "test-user-id",
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
                Endpoint.GetCustomerCenterConfig("test-user-id"),
                body = null,
                postFieldsToSign = null,
                requestHeaders = any(),
            )
        }
    }

    private fun mockHttpResult(
        responseCode: Int = RCHTTPStatusCodes.SUCCESS,
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
                loadJSON(MOCK_RESPONSE_FILENAME),
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
                LinkedBlockingQueue()
            )
        )
    }

    private fun loadJSON(jsonFileName: String) = File(javaClass.classLoader!!.getResource(jsonFileName).file).readText()
}
