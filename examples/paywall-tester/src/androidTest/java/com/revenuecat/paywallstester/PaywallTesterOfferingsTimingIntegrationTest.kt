package com.revenuecat.paywallstester

import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.awaitSyncAttributesAndOfferingsIfNeeded
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(AndroidJUnit4::class)
class PaywallTesterOfferingsTimingIntegrationTest {

    @get:Rule
    val activityScenarioRule = activityScenarioRule<MainActivity>()

    private lateinit var originalLogHandler: LogHandler
    private lateinit var recordingLogHandler: RecordingLogHandler

    @Before
    fun setUp() {
        assumeTrue(
            "Set PAYWALL_TESTER_API_KEY_A in local.properties or pass -PPAYWALL_TESTER_API_KEY_A=...",
            BuildConfig.PAYWALL_TESTER_API_KEY_A.startsWith("goog_"),
        )

        originalLogHandler = Purchases.logHandler
        recordingLogHandler = RecordingLogHandler(originalLogHandler)
        Purchases.logHandler = recordingLogHandler
        Purchases.logLevel = LogLevel.DEBUG
    }

    @After
    fun tearDown() {
        Purchases.logHandler = originalLogHandler
    }

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun fetchingOfferingsAndWorkflowEmitsTimingLogs() = runBlocking {
        val offerings = withTimeout(TIMEOUT_MILLIS) {
            Purchases.sharedInstance.awaitSyncAttributesAndOfferingsIfNeeded()
        }

        assertFalse("Expected at least one offering", offerings.all.isEmpty())
        val currentOffering = offerings.current ?: offerings.all.values.first()
        val workflowId = Purchases.sharedInstance.workflowIdForOfferingId(currentOffering.identifier)
            ?: currentOffering.identifier

        val workflowResult = withTimeout(TIMEOUT_MILLIS) {
            Purchases.sharedInstance.awaitGetWorkflow(workflowId)
        }
        assertTrue("Expected a resolved workflow id", workflowResult.workflow.id.isNotEmpty())

        val messages = recordingLogHandler.messages
        assertTrue(messages.containsLog("Offerings backend request completed"))
        assertTrue(messages.containsLog("Offerings fetch completed"))
        assertTrue(messages.containsLog("Workflows list backend request completed"))
        assertTrue(messages.containsLog("Workflows list gate completed"))
        assertTrue(messages.any { it.contains("Workflow ") && it.contains(" resolved in ") })
        assertTrue(messages.containsLog("Workflow CDN fetch completed"))
    }

    private fun List<String>.containsLog(message: String): Boolean =
        any { it.contains(message) }

    private class RecordingLogHandler(
        private val delegate: LogHandler,
    ) : LogHandler {

        val messages = CopyOnWriteArrayList<String>()

        override fun v(tag: String, msg: String) {
            record(tag, msg)
            delegate.v(tag, msg)
        }

        override fun d(tag: String, msg: String) {
            record(tag, msg)
            delegate.d(tag, msg)
        }

        override fun i(tag: String, msg: String) {
            record(tag, msg)
            delegate.i(tag, msg)
        }

        override fun w(tag: String, msg: String) {
            record(tag, msg)
            delegate.w(tag, msg)
        }

        override fun e(tag: String, msg: String, throwable: Throwable?) {
            record(tag, msg)
            delegate.e(tag, msg, throwable)
        }

        private fun record(tag: String, msg: String) {
            messages += "$tag $msg"
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 60_000L
    }
}
