@file:OptIn(ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.checkpoints.CheckpointResolution
import com.revenuecat.purchases.common.localrules.RulesDimensionValue
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class CheckpointsManagerTest {

    private val checkpointId = "test_checkpoint"
    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var mockPurchases: Purchases
    private lateinit var mockActivity: Activity
    private lateinit var mockPresenter: CheckpointWorkflowPresenter
    private val presentedCallIds = mutableListOf<String>()
    private val results = mutableListOf<FlowResult?>()

    private lateinit var manager: CheckpointsManager

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        mockkObject(Logger)
        every { Logger.e(any()) } just runs
        every { Logger.w(any()) } just runs
        presentedCallIds.clear()
        results.clear()
        mockActivity = mockk(relaxed = true)
        mockPresenter = mockk(relaxed = true)
        mockPurchases = mockk {
            every { currentActivity } returns mockActivity
            every { getCustomerInfo(CacheFetchPolicy.CACHE_ONLY, any()) } answers {
                secondArg<ReceiveCustomerInfoCallback>()
                    .onError(PurchasesError(PurchasesErrorCode.CustomerInfoError, "No cache."))
            }
        }
        manager = CheckpointsManager { callId, _ ->
            presentedCallIds += callId
            mockPresenter
        }
    }

    @After
    fun tearDown() {
        unmockkObject(Logger)
        Dispatchers.resetMain()
    }

    @Test
    fun `valid checkpoint identifier is resolved`() = runTest(dispatcher) {
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.NO_MATCH))

        checkpoint(identifier = "A-1_b")

        coVerify(exactly = 1) { mockPurchases.internalResolveCp("A-1_b", emptyMap()) }
    }

    @Test
    fun `invalid checkpoint identifier is logged and skips resolution`() = runTest(dispatcher) {
        val invalidIdentifier = " checkout😀"

        checkpoint(identifier = invalidIdentifier)

        assertThat(results).containsExactly(null)
        coVerify(exactly = 0) { mockPurchases.internalResolveCp(any(), any()) }
        verify(exactly = 1) {
            Logger.e(CheckpointIdentifierValidator.invalidIdentifierLogMessage(invalidIdentifier))
        }
    }

    @Test
    fun `unmatched checkpoint passes null without a flow`() =
        runTest(dispatcher) {
            resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.UNKNOWN_CHECKPOINT))

            checkpoint()

            assertThat(results).containsExactly(null)
        }

    @Test
    fun `offering checkpoint presents the fallback paywall and resolves when it finishes`() = runTest(dispatcher) {
        val offering = mockk<Offering>()
        resolvesTo(CheckpointResolution.MatchedOffering(offering, checkpointRuleId = null))

        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }

        assertThat(run).isNull()
        val content = manager.presentation(currentCallId())!!.content
        assertThat((content as CheckpointFlowContent.OfferingFlow).offering).isEqualTo(offering)

        finishPaywall(CheckpointFlowOutcome.Dismissed)
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Dismissed)
    }

    @Test
    fun `offering checkpoint cannot present while a UI checkpoint is being presented`() = runTest(dispatcher) {
        coEvery { mockPurchases.internalResolveCp(any(), any()) } returnsMany listOf(
            CheckpointResolution.MatchedWorkflow(mockk(), mockk(), mockk(), checkpointRuleId = null),
            CheckpointResolution.MatchedOffering(mockk(), checkpointRuleId = null),
        )
        val presentedCall = launch { runCheckpoint() }

        val run = runCheckpoint()

        assertThat(run.blockedByPresentedFlow).isTrue
        assertThat(run.flowOutcome).isNull()
        assertThat(presentedCallIds).hasSize(1)
        presentedCall.cancel()
    }

    @Test
    fun `resolution failure passes null without a flow`() = runTest(dispatcher) {
        val error = PurchasesError(PurchasesErrorCode.ConfigurationError, "Simulated.")
        coEvery { mockPurchases.internalResolveCp(any(), any()) } throws PurchasesException(error)

        checkpoint()

        assertThat(results).containsExactly(null)
    }

    @Test
    fun `a presentation failure completes with null without throwing`() = runTest(dispatcher) {
        every { mockPurchases.currentActivity } returns null
        resolvesToWorkflow()

        checkpoint()

        assertThat(results).containsExactly(null)
        verify {
            Logger.e(
                PurchasesError(
                    PurchasesErrorCode.ConfigurationError,
                    "Cannot present checkpoint workflow: no started Activity found.",
                ).toString(),
            )
        }
    }

    @Test
    fun `matched checkpoint presents the workflow and completes when the paywall finishes`() = runTest(dispatcher) {
        resolvesToWorkflow()

        checkpoint(CheckpointParams { customVariables { "goal" to "test" } })

        assertThat(results).isEmpty()

        finishPaywall(CheckpointFlowOutcome.Dismissed)

        assertThat(results).containsExactly(FlowResult(obtainedEntitlements = emptySet()))
    }

    @Test
    fun `custom variables reach the resolver as rule dimensions`() = runTest(dispatcher) {
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.NO_MATCH))

        runCheckpoint(
            CheckpointParams {
                customVariables {
                    "goal" to "test"
                    "attempt" to 2
                    "flag" to true
                }
            },
        )

        val customVariables = slot<Map<String, RulesDimensionValue>>()
        coVerify { mockPurchases.internalResolveCp(checkpointId, capture(customVariables)) }
        assertThat(customVariables.captured).isEqualTo(
            mapOf(
                "goal" to RulesDimensionValue.StringValue("test"),
                "attempt" to RulesDimensionValue.DoubleValue(2.0),
                "flag" to RulesDimensionValue.BoolValue(true),
            ),
        )
    }

    @Test
    fun `custom variables are exposed to the presented paywall`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val call = launch {
            runCheckpoint(
                CheckpointParams {
                    customVariables {
                        "gate" to "hard"
                        "attempt" to 2
                        "ratio" to 0.5
                        "flag" to true
                    }
                },
            )
        }

        assertThat(manager.presentation(currentCallId())!!.customVariables).isEqualTo(
            mapOf(
                "gate" to CustomVariableValue.String("hard"),
                "attempt" to CustomVariableValue.Number(2),
                "ratio" to CustomVariableValue.Number(0.5),
                "flag" to CustomVariableValue.Boolean(true),
            ),
        )

        finishPaywall(CheckpointFlowOutcome.Dismissed)
        call.join()
    }

    @Test
    fun `the recorded outcome is the one delivered`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val customerInfo = mockk<CustomerInfo>()
        val storeTransaction = mockk<StoreTransaction>()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }

        finishPaywall(CheckpointFlowOutcome.Purchased(customerInfo, storeTransaction))
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Purchased(customerInfo, storeTransaction))
    }

    @Test
    fun `a web checkout outcome is delivered when the paywall dismisses`() = runTest(dispatcher) {
        resolvesToWorkflow()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }

        finishPaywall(CheckpointFlowOutcome.WebCheckoutOpened)
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.WebCheckoutOpened)
    }

    @Test
    fun `a later outcome replaces an earlier web checkout outcome`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val customerInfo = mockk<CustomerInfo>()
        val storeTransaction = mockk<StoreTransaction>()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }
        manager.recordOutcome(currentCallId(), CheckpointFlowOutcome.WebCheckoutOpened)

        finishPaywall(CheckpointFlowOutcome.Purchased(customerInfo, storeTransaction))
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Purchased(customerInfo, storeTransaction))
    }

    @Test
    fun `a concurrent checkpoint is blocked by the presented flow`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val firstCall = launch { runCheckpoint() }

        val run = runCheckpoint()

        assertThat(run.blockedByPresentedFlow).isTrue
        assertThat(run.flowOutcome).isNull()
        firstCall.cancel()
    }

    @Test
    fun `a checkpoint blocked by the presented flow never invokes its callback`() = runTest(dispatcher) {
        resolvesToWorkflow()
        checkpoint()

        checkpoint()

        assertThat(results).isEmpty()
        finishPaywall(CheckpointFlowOutcome.Dismissed)
        assertThat(results).containsExactly(FlowResult(obtainedEntitlements = emptySet()))
        assertThat(presentedCallIds).hasSize(1)
    }

    @Test
    fun `an unmatched checkpoint still passes null while another flow is presented`() = runTest(dispatcher) {
        resolvesToWorkflow()
        checkpoint()
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.NO_MATCH))

        checkpoint()

        assertThat(results).containsExactly(null)
    }

    @Test
    fun `checkpoint can present again after the previous workflow finishes`() = runTest(dispatcher) {
        resolvesToWorkflow()

        val firstCall = launch { runCheckpoint() }
        finishPaywall(CheckpointFlowOutcome.Dismissed)
        firstCall.join()

        val secondCall = launch { runCheckpoint() }
        finishPaywall(CheckpointFlowOutcome.Dismissed)
        secondCall.join()

        assertThat(presentedCallIds).hasSize(2)
    }

    @Test
    fun `checkpoint can present again after the presenter fails to show`() = runTest(dispatcher) {
        resolvesToWorkflow()
        every { mockPresenter.show(any()) } throws RuntimeException("show failed")

        checkpointFailsWith(
            PurchasesErrorCode.ConfigurationError,
            "Failed to present checkpoint workflow: java.lang.RuntimeException: show failed",
        )

        every { mockPresenter.show(any()) } just runs
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }
        finishPaywall(CheckpointFlowOutcome.Dismissed)
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Dismissed)
    }

    @Test
    fun `checkpoint can present again after being cancelled`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val firstCall = launch { runCheckpoint() }

        firstCall.cancel()

        var run: CheckpointRun? = null
        val secondCall = launch { run = runCheckpoint() }
        finishPaywall(CheckpointFlowOutcome.Dismissed)
        secondCall.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Dismissed)
    }

    @Test
    fun `cancelling the caller abandons the presented paywall`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val call = launch { runCheckpoint() }

        call.cancel()
        call.join()

        verify { mockPresenter.abandon() }
    }

    @Test
    fun `a report from an abandoned presentation does not disturb the call that replaced it`() =
        runTest(dispatcher) {
            resolvesToWorkflow()
            val firstCall = launch { runCheckpoint() }
            val abandonedCallId = currentCallId()
            firstCall.cancel()
            firstCall.join()

            var run: CheckpointRun? = null
            val secondCall = launch { run = runCheckpoint() }
            manager.recordOutcome(abandonedCallId, CheckpointFlowOutcome.Error(mockk()))
            manager.onPresentationFinished(abandonedCallId)

            assertThat(run).isNull()

            finishPaywall(CheckpointFlowOutcome.Dismissed)
            secondCall.join()

            assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Dismissed)
        }

    @Test
    fun `the callback runs before the presented flow is released`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val events = mutableListOf<String>()
        manager.checkpoint(mockPurchases, checkpointId, null) { events += "callback" }

        manager.onPresentationFinished(currentCallId()) { events += "released" }

        assertThat(events).containsExactly("callback", "released")
    }

    @Test
    fun `a backed-out flow is released without invoking the callback`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val events = mutableListOf<String>()
        manager.checkpoint(mockPurchases, checkpointId, null) { events += "callback" }

        manager.onPresentationFinished(currentCallId(), navigatedBack = true) { events += "released" }

        assertThat(events).containsExactly("released")
    }

    @Test
    fun `finishing an unknown callId still releases what it was given`() {
        var released = false

        manager.onPresentationFinished("unknown-call-id") { released = true }

        assertThat(released).isTrue
    }

    @Test
    fun `finishing an unknown callId is a no-op`() {
        manager.onPresentationFinished("unknown-call-id")
        manager.recordOutcome("unknown-call-id", CheckpointFlowOutcome.Dismissed)

        assertThat(manager.presentation("unknown-call-id")).isNull()
    }

    @Test
    fun `finishing the presentation releases the pending call`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val customerInfo = mockk<CustomerInfo>()
        val storeTransaction = mockk<StoreTransaction>()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }
        val callId = currentCallId()
        manager.recordOutcome(callId, CheckpointFlowOutcome.Purchased(customerInfo, storeTransaction))

        manager.onPresentationFinished(callId)
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Purchased(customerInfo, storeTransaction))
        assertThat(manager.presentation(callId)).isNull()
    }

    @Test
    fun `backing out without a recorded outcome resolves Dismissed and backed out`() = runTest(dispatcher) {
        resolvesToWorkflow()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }

        finishPaywall(outcome = null, navigatedBack = true)
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Dismissed)
        assertThat(run!!.backedOut).isTrue
        assertThat(manager.presentation(currentCallId())).isNull()
    }

    @Test
    fun `backing out after a recorded error keeps the error and is backed out`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "boom")
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }

        finishPaywall(CheckpointFlowOutcome.Error(error), navigatedBack = true)
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Error(error))
        assertThat(run!!.backedOut).isTrue
    }

    @Test
    fun `backing out after a recorded restore is not backed out`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val customerInfo = mockk<CustomerInfo>()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }

        finishPaywall(CheckpointFlowOutcome.Restored(customerInfo), navigatedBack = true)
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Restored(customerInfo))
        assertThat(run!!.backedOut).isFalse
    }

    @Test
    fun `closing without backing out is not backed out`() = runTest(dispatcher) {
        resolvesToWorkflow()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }

        finishPaywall(CheckpointFlowOutcome.Dismissed)
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Dismissed)
        assertThat(run!!.backedOut).isFalse
    }

    private fun resolvesTo(resolution: CheckpointResolution) {
        coEvery { mockPurchases.internalResolveCp(any(), any()) } returns resolution
    }

    private fun resolvesToWorkflow() {
        resolvesTo(CheckpointResolution.MatchedWorkflow(mockk(), mockk(), mockk(), checkpointRuleId = null))
    }

    private fun currentCallId(): String = presentedCallIds.last()

    // Mirrors what CheckpointWorkflowPresenter does: read the pending call for the presented callId, record
    // the outcome, then report the paywall as finished.
    private fun finishPaywall(outcome: CheckpointFlowOutcome?, navigatedBack: Boolean = false) {
        val callId = currentCallId()
        assertThat(manager.presentation(callId)).isNotNull
        outcome?.let { manager.recordOutcome(callId, it) }
        manager.onPresentationFinished(callId, navigatedBack)
    }

    // The callback API; completes synchronously here because the manager's scope runs on the unconfined main
    // dispatcher, except while a presented paywall is waiting to finish.
    private fun checkpoint(params: CheckpointParams? = null, identifier: String = checkpointId) {
        manager.checkpoint(mockPurchases, identifier, params) { results += it }
    }

    private suspend fun runCheckpoint(params: CheckpointParams? = null): CheckpointRun =
        manager.runCheckpoint(mockPurchases, checkpointId, params)

    // Presentation failures are logged rather than returned: the run just presents nothing.
    private suspend fun checkpointFailsWith(code: PurchasesErrorCode, message: String) {
        assertThat(runCheckpoint().flowOutcome).isNull()
        verify { Logger.e(PurchasesError(code, message).toString()) }
    }
}
