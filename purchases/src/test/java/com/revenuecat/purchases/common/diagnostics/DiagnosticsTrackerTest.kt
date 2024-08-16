package com.revenuecat.purchases.common.diagnostics

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfos
import com.revenuecat.purchases.PurchasesAreCompletedBy.MY_APP
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.SyncDispatcher
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.playServicesVersionName
import com.revenuecat.purchases.common.playStoreVersionName
import com.revenuecat.purchases.strings.OfflineEntitlementsStrings
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class DiagnosticsTrackerTest {

    private val testDiagnosticsEntry = DiagnosticsEntry(
        name = DiagnosticsEntryName.HTTP_REQUEST_PERFORMED,
        properties = mapOf("test-key-1" to "test-value-1")
    )

    private lateinit var diagnosticsFileHelper: DiagnosticsFileHelper
    private lateinit var dispatcher: Dispatcher
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor

    private lateinit var diagnosticsTracker: DiagnosticsTracker

    private lateinit var context: Context
    private lateinit var appConfig: AppConfig

    @Before
    fun setup() {
        mockkStatic("com.revenuecat.purchases.common.UtilsKt")
        context = mockk<Context>(relaxed = true).apply {
            every { playStoreVersionName } returns "123"
            every { playServicesVersionName } returns "456"
        }
        appConfig = AppConfig(
            context = context,
            purchasesAreCompletedBy = MY_APP,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE
        )
        diagnosticsFileHelper = mockk<DiagnosticsFileHelper>().apply {
            every { isDiagnosticsFileTooBig() } returns false
        }
        dispatcher = SyncDispatcher()

        mockSharedPreferences()

        diagnosticsTracker = DiagnosticsTracker(
            appConfig,
            diagnosticsFileHelper,
            DiagnosticsHelper(mockk(), diagnosticsFileHelper, lazy { sharedPreferences }),
            dispatcher
        )
    }

    @After
    fun tearDown() {
        unmockkStatic("com.revenuecat.purchases.common.UtilsKt")
    }

    @Test
    fun `trackEvent performs correct calls`() {
        every { diagnosticsFileHelper.appendEvent(testDiagnosticsEntry) } just Runs
        diagnosticsTracker.trackEvent(testDiagnosticsEntry)
        verify(exactly = 1) { diagnosticsFileHelper.appendEvent(testDiagnosticsEntry) }
        verify(exactly = 0) { diagnosticsFileHelper.deleteFile() }
        verify(exactly = 0) { diagnosticsFileHelper.appendEvent(match {
            it.name == DiagnosticsEntryName.MAX_EVENTS_STORED_LIMIT_REACHED
        })}
    }

    @Test
    fun `trackEvent clears diagnostics file if too big, then adds event`() {
        every { diagnosticsFileHelper.deleteFile() } just Runs
        every { diagnosticsFileHelper.appendEvent(any()) } just Runs
        every { diagnosticsFileHelper.isDiagnosticsFileTooBig() }.returnsMany(true, false)
        diagnosticsTracker.trackEvent(testDiagnosticsEntry)
        verifySequence {
            diagnosticsFileHelper.isDiagnosticsFileTooBig()
            diagnosticsFileHelper.deleteFile()
            diagnosticsFileHelper.appendEvent(match {
                it.name == DiagnosticsEntryName.MAX_EVENTS_STORED_LIMIT_REACHED
            })
            diagnosticsFileHelper.appendEvent(match {
                it == testDiagnosticsEntry
            })
        }
    }

    @Test
    fun `trackEvent handles IOException`() {
        every { diagnosticsFileHelper.appendEvent(any()) } throws IOException()
        diagnosticsTracker.trackEvent(testDiagnosticsEntry)
    }

    @Test
    fun `trackEventInCurrentThread does not enqueue request`() {
        dispatcher.close()
        every { diagnosticsFileHelper.appendEvent(any()) } throws IOException()
        diagnosticsTracker.trackEventInCurrentThread(testDiagnosticsEntry)
    }

    @Test
    fun `trackHttpRequestPerformed tracks correct event when coming from cache`() {
        val expectedProperties = mapOf(
            "endpoint_name" to "post_receipt",
            "response_time_millis" to 1234L,
            "successful" to true,
            "response_code" to 200,
            "etag_hit" to true,
            "verification_result" to "NOT_REQUESTED"
        )
        every { diagnosticsFileHelper.appendEvent(any()) } just Runs
        diagnosticsTracker.trackHttpRequestPerformed(
            Endpoint.PostReceipt,
            1234L.milliseconds,
            true,
            200,
            HTTPResult.Origin.CACHE,
            VerificationResult.NOT_REQUESTED
        )
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEvent(match { event ->
                event.name == DiagnosticsEntryName.HTTP_REQUEST_PERFORMED && event.properties == expectedProperties
            })
        }
    }

    @Test
    fun `trackHttpRequestPerformed tracks correct event when coming from backend`() {
        val expectedProperties = mapOf(
            "endpoint_name" to "get_offerings",
            "response_time_millis" to 1234L,
            "successful" to true,
            "response_code" to 200,
            "etag_hit" to false,
            "verification_result" to "NOT_REQUESTED"
        )
        every { diagnosticsFileHelper.appendEvent(any()) } just Runs
        diagnosticsTracker.trackHttpRequestPerformed(
            Endpoint.GetOfferings("test id"),
            1234L.milliseconds,
            true,
            200,
            HTTPResult.Origin.BACKEND,
            VerificationResult.NOT_REQUESTED
        )
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEvent(match { event ->
                event.name == DiagnosticsEntryName.HTTP_REQUEST_PERFORMED && event.properties == expectedProperties
            })
        }
    }

    @Test
    fun `trackMaxEventsStoredLimitReached tracks correct event`() {
        every { diagnosticsFileHelper.appendEvent(any()) } just Runs
        diagnosticsTracker.trackMaxEventsStoredLimitReached()
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEvent(match { event ->
                event.name == DiagnosticsEntryName.MAX_EVENTS_STORED_LIMIT_REACHED &&
                    event.properties == emptyMap<String, Any>()
            })
        }
    }

    // region Google Billing

    @Test
    fun `trackGoogleQueryProductDetailsRequest tracks correct event`() {
        val expectedProperties = mapOf(
            "product_type_queried" to "subs",
            "billing_response_code" to 12,
            "billing_debug_message" to "test-debug-message",
            "response_time_millis" to 1234L
        )
        every { diagnosticsFileHelper.appendEvent(any()) } just Runs
        diagnosticsTracker.trackGoogleQueryProductDetailsRequest(
            productType = "subs",
            billingResponseCode = 12,
            billingDebugMessage = "test-debug-message",
            responseTime = 1234L.milliseconds
        )
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEvent(match { event ->
                event.name == DiagnosticsEntryName.GOOGLE_QUERY_PRODUCT_DETAILS_REQUEST &&
                    event.properties == expectedProperties
            })
        }
    }

    @Test
    fun `trackGoogleQueryPurchasesRequest tracks correct event`() {
        val expectedProperties = mapOf(
            "product_type_queried" to "subs",
            "billing_response_code" to 12,
            "billing_debug_message" to "test-debug-message",
            "response_time_millis" to 1234L
        )
        every { diagnosticsFileHelper.appendEvent(any()) } just Runs
        diagnosticsTracker.trackGoogleQueryPurchasesRequest(
            productType = "subs",
            billingResponseCode = 12,
            billingDebugMessage = "test-debug-message",
            responseTime = 1234L.milliseconds
        )
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEvent(match { event ->
                event.name == DiagnosticsEntryName.GOOGLE_QUERY_PURCHASES_REQUEST &&
                    event.properties == expectedProperties
            })
        }
    }

    @Test
    fun `trackGoogleQueryPurchaseHistoryRequest tracks correct event`() {
        val expectedProperties = mapOf(
            "product_type_queried" to "inapp",
            "billing_response_code" to 12,
            "billing_debug_message" to "test-debug-message",
            "response_time_millis" to 1234L
        )
        every { diagnosticsFileHelper.appendEvent(any()) } just Runs
        diagnosticsTracker.trackGoogleQueryPurchaseHistoryRequest(
            productType = "inapp",
            billingResponseCode = 12,
            billingDebugMessage = "test-debug-message",
            responseTime = 1234L.milliseconds
        )
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEvent(match { event ->
                event.name == DiagnosticsEntryName.GOOGLE_QUERY_PURCHASE_HISTORY_REQUEST &&
                    event.properties == expectedProperties
            })
        }
    }

    // endregion

    // region Amazon Billing

    @Test
    fun `trackAmazonQueryProductDetailsRequest tracks correct event`() {
        val expectedTags = mapOf(
            "successful" to true,
            "response_time_millis" to 1234L,
        )
        every { diagnosticsFileHelper.appendEvent(any()) } just Runs
        diagnosticsTracker.trackAmazonQueryProductDetailsRequest(
            wasSuccessful = true,
            responseTime = 1234L.milliseconds
        )
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEvent(match { event ->
                event.name == DiagnosticsEntryName.AMAZON_QUERY_PRODUCT_DETAILS_REQUEST &&
                    event.properties == expectedTags
            })
        }
    }

    @Test
    fun `trackAmazonQueryPurchasesRequest tracks correct event`() {
        val expectedTags = mapOf(
            "successful" to true,
            "response_time_millis" to 1234L,
        )
        every { diagnosticsFileHelper.appendEvent(any()) } just Runs
        diagnosticsTracker.trackAmazonQueryPurchasesRequest(
            wasSuccessful = true,
            responseTime = 1234L.milliseconds
        )
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEvent(match { event ->
                event.name == DiagnosticsEntryName.AMAZON_QUERY_PURCHASES_REQUEST &&
                    event.properties == expectedTags
            })
        }
    }

    // endregion

    @Test
    fun `trackFeatureNotSupported tracks correct event`() {
        val expectedProperties = mapOf(
            "play_store_version" to "123",
            "play_services_version" to "456",
            "billing_response_code" to -2,
            "billing_debug_message" to "debug message",
        )
        every { diagnosticsFileHelper.appendEvent(any()) } just Runs
        diagnosticsTracker.trackProductDetailsNotSupported(
            billingResponseCode = -2,
            billingDebugMessage = "debug message"
        )
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEvent(match { event ->
                event.name == DiagnosticsEntryName.PRODUCT_DETAILS_NOT_SUPPORTED &&
                    event.properties == expectedProperties
            })
        }
    }

    @Test
    fun `trackCustomerInfoVerificationResultIfNeeded does not track when verification not requested`() {
        val customerInfo = mockk<CustomerInfo>().apply {
            every { entitlements } returns mockk<EntitlementInfos>().apply {
                every { verification } returns VerificationResult.NOT_REQUESTED
            }
        }
        diagnosticsTracker.trackCustomerInfoVerificationResultIfNeeded(
            customerInfo = customerInfo,
        )
        verify(exactly = 0) {
            diagnosticsFileHelper.appendEvent(any())
        }
    }

    @Test
    fun `trackCustomerInfoVerificationResultIfNeeded tracks when verification is failed`() {
        val expectedProperties = mapOf(
            "verification_result" to "FAILED",
        )
        every { diagnosticsFileHelper.appendEvent(any()) } just Runs
        val customerInfo = mockk<CustomerInfo>().apply {
            every { entitlements } returns mockk<EntitlementInfos>().apply {
                every { verification } returns VerificationResult.FAILED
            }
        }
        diagnosticsTracker.trackCustomerInfoVerificationResultIfNeeded(
            customerInfo = customerInfo,
        )
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEvent(match { event ->
                event.name == DiagnosticsEntryName.CUSTOMER_INFO_VERIFICATION_RESULT &&
                    event.properties == expectedProperties
            })
        }
    }

    // region Offline Entitlements

    @Test
    fun `trackEnteredOfflineEntitlementsMode tracks correct data`() {
        val expectedProperties = mapOf<String, Any>()
        every { diagnosticsFileHelper.appendEvent(any()) } just Runs
        diagnosticsTracker.trackEnteredOfflineEntitlementsMode()
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEvent(match { event ->
                event.name == DiagnosticsEntryName.ENTERED_OFFLINE_ENTITLEMENTS_MODE &&
                    event.properties == expectedProperties
            })
        }
    }

    @Test
    fun `trackErrorEnteringOfflineEntitlementsMode tracks correct data for unknown error`() {
        val expectedProperties = mapOf(
            "offline_entitlement_error_reason" to "unknown",
            "error_message" to "Unknown error. Underlying error: test error message",
        )
        every { diagnosticsFileHelper.appendEvent(any()) } just Runs
        diagnosticsTracker.trackErrorEnteringOfflineEntitlementsMode(
            PurchasesError(
                PurchasesErrorCode.UnknownError,
                "test error message"
            )
        )
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEvent(match { event ->
                event.name == DiagnosticsEntryName.ERROR_ENTERING_OFFLINE_ENTITLEMENTS_MODE &&
                    event.properties == expectedProperties
            })
        }
    }

    @Test
    fun `trackErrorEnteringOfflineEntitlementsMode tracks correct data for one time purchase error`() {
        val expectedProperties = mapOf(
            "offline_entitlement_error_reason" to "one_time_purchase_found",
            "error_message" to "There was a problem with the operation. Looks like we don't support that yet. Check the underlying error for more details. Underlying error: Offline entitlements are not supported for one time purchases. Found one time purchases. See for more info: https://rev.cat/offline-entitlements",
        )
        every { diagnosticsFileHelper.appendEvent(any()) } just Runs
        diagnosticsTracker.trackErrorEnteringOfflineEntitlementsMode(
            PurchasesError(
                PurchasesErrorCode.UnsupportedError,
                underlyingErrorMessage = OfflineEntitlementsStrings.OFFLINE_ENTITLEMENTS_UNSUPPORTED_INAPP_PURCHASES,
            )
        )
        verify(exactly = 1) {
            diagnosticsFileHelper.appendEvent(match { event ->
                event.name == DiagnosticsEntryName.ERROR_ENTERING_OFFLINE_ENTITLEMENTS_MODE &&
                    event.properties == expectedProperties
            })
        }
    }

    // endregion

    private fun mockSharedPreferences() {
        sharedPreferences = mockk()
        sharedPreferencesEditor = mockk()
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.apply() } just Runs
        every {
            sharedPreferencesEditor.remove(DiagnosticsHelper.CONSECUTIVE_FAILURES_COUNT_KEY)
        } returns sharedPreferencesEditor
        every {
            sharedPreferencesEditor.putInt(DiagnosticsHelper.CONSECUTIVE_FAILURES_COUNT_KEY, any())
        } returns sharedPreferencesEditor
        every {
            sharedPreferences.getInt(DiagnosticsHelper.CONSECUTIVE_FAILURES_COUNT_KEY, 0)
        } returns 0
    }
}
