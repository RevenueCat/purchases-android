@file:OptIn(ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.checkpoints.CheckpointResolution
import com.revenuecat.purchases.common.localrules.RulesDimensionValue
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
import io.mockk.verifyOrder
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
    private lateinit var mockListener: CheckpointListener
    private lateinit var mockPresenter: CheckpointWorkflowPresenter
    private val presentedCallIds = mutableListOf<String>()

    private lateinit var manager: CheckpointsManager

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        mockkObject(Logger)
        every { Logger.e(any()) } just runs
        presentedCallIds.clear()
        mockActivity = mockk(relaxed = true)
        mockPresenter = mockk(relaxed = true)
        mockListener = mockk(relaxed = true)
        mockPurchases = mockk {
            every { currentActivity } returns mockActivity
        }
        manager = CheckpointsManager { callId, _ ->
            presentedCallIds += callId
            mockPresenter
        }
        manager.checkpointListener = mockListener
    }

    @After
    fun tearDown() {
        unmockkObject(Logger)
        Dispatchers.resetMain()
    }

    @Test
    fun `valid checkpoint identifier reaches listener and resolution`() = runTest(dispatcher) {
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.NO_MATCH))

        manager.checkpoint(mockPurchases, "A-1_b", null)

        coVerify(exactly = 1) { mockPurchases.resolveCheckpoint("A-1_b", emptyMap()) }
        verify(exactly = 1) { mockListener.onCheckpointHit(match { it.identifier == "A-1_b" }) }
        verify(exactly = 1) { mockListener.onCheckpointCompleted(match { it.identifier == "A-1_b" }) }
    }

    @Test
    fun `invalid checkpoint identifier is logged and reported to listener without resolution`() = runTest(dispatcher) {
        val invalidIdentifier = " checkout😀"

        val result = manager.checkpoint(mockPurchases, invalidIdentifier, null) as CheckpointResult.NoAction

        assertThat(result.reason).isEqualTo(CheckpointResult.NoAction.Reason.INVALID_CHECKPOINT_IDENTIFIER)
        coVerify(exactly = 0) { mockPurchases.resolveCheckpoint(any(), any()) }
        verifyOrder {
            mockListener.onCheckpointHit(CheckpointHitContext(invalidIdentifier, emptyMap()))
            mockListener.onCheckpointCompleted(CheckpointCompletedContext(invalidIdentifier, emptyMap(), result))
        }
        verify(exactly = 1) {
            Logger.e(CheckpointIdentifierValidator.invalidIdentifierLogMessage(invalidIdentifier))
        }
    }

    @Test
    fun `unmatched checkpoint resolves NoAction with the mapped reason`() = runTest(dispatcher) {
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.UNKNOWN_CHECKPOINT))

        val result = checkpoint() as CheckpointResult.NoAction

        assertThat(result.reason).isEqualTo(CheckpointResult.NoAction.Reason.UNKNOWN_CHECKPOINT)
        verifyOrder {
            mockListener.onCheckpointHit(CheckpointHitContext(checkpointId, emptyMap()))
            mockListener.onCheckpointCompleted(CheckpointCompletedContext(checkpointId, emptyMap(), result))
        }
    }

    @Test
    fun `offering checkpoint presents the fallback paywall and resolves when it finishes`() = runTest(dispatcher) {
        val offering = mockk<Offering>()
        resolvesTo(CheckpointResolution.MatchedOffering(offering))

        var result: CheckpointResult? = null
        val call = launch { result = checkpoint() }

        assertThat(result).isNull()
        val content = manager.presentation(currentCallId())!!.content
        assertThat((content as CheckpointPaywallContent.OfferingPaywall).offering).isEqualTo(offering)

        finishPaywall(CheckpointPaywallOutcome.Dismissed)
        call.join()

        assertThat((result as CheckpointResult.PaywallPresented).paywallOutcome)
            .isEqualTo(CheckpointPaywallOutcome.Dismissed)
    }

    @Test
    fun `offering checkpoint cannot present while a UI checkpoint is being presented`() = runTest(dispatcher) {
        coEvery { mockPurchases.resolveCheckpoint(any(), any()) } returnsMany listOf(
            CheckpointResolution.MatchedWorkflow(mockk(), mockk(), mockk()),
            CheckpointResolution.MatchedOffering(mockk()),
        )
        val presentedCall = launch { checkpoint() }

        assertThat(checkpointErrorCode()).isEqualTo(PurchasesErrorCode.OperationAlreadyInProgressError)

        assertThat(presentedCallIds).hasSize(1)
        presentedCall.cancel()
    }

    @Test
    fun `a registered presenter presents a matched offering and its report resolves the checkpoint`() =
        runTest(dispatcher) {
            val offering = mockk<Offering>()
            val customerInfo = mockk<CustomerInfo>()
            val storeTransaction = mockk<StoreTransaction>()
            var presented: Offering? = null
            var completion: CheckpointOfferingCompletion? = null
            manager.checkpointOfferingPresenter = CheckpointOfferingPresenter { presentedOffering, presentation ->
                presented = presentedOffering
                completion = presentation
            }
            resolvesTo(CheckpointResolution.MatchedOffering(offering))

            var result: CheckpointResult? = null
            val call = launch { result = checkpoint() }

            assertThat(presented).isEqualTo(offering)
            assertThat(result).isNull()
            verify(exactly = 0) { mockActivity.startActivity(any()) }

            completion!!.purchased(customerInfo, storeTransaction)
            call.join()

            assertThat((result as CheckpointResult.PaywallPresented).paywallOutcome)
                .isEqualTo(CheckpointPaywallOutcome.Purchased(customerInfo, storeTransaction))
        }

    @Test
    fun `only the presenter's first report counts`() = runTest(dispatcher) {
        var completion: CheckpointOfferingCompletion? = null
        manager.checkpointOfferingPresenter = CheckpointOfferingPresenter { _, presentation ->
            completion = presentation
        }
        resolvesTo(CheckpointResolution.MatchedOffering(mockk()))
        var result: CheckpointResult? = null
        val call = launch { result = checkpoint() }

        completion!!.dismissed()
        completion!!.purchased(mockk(), mockk())
        call.join()

        assertThat((result as CheckpointResult.PaywallPresented).paywallOutcome)
            .isEqualTo(CheckpointPaywallOutcome.Dismissed)
    }

    @Test
    fun `an app-owned presentation claims the one-presentation slot`() = runTest(dispatcher) {
        manager.checkpointOfferingPresenter = CheckpointOfferingPresenter { _, _ -> }
        coEvery { mockPurchases.resolveCheckpoint(any(), any()) } returnsMany listOf(
            CheckpointResolution.MatchedOffering(mockk()),
            CheckpointResolution.MatchedWorkflow(mockk(), mockk(), mockk()),
        )
        val presenterCall = launch { checkpoint() }

        assertThat(checkpointErrorCode()).isEqualTo(PurchasesErrorCode.OperationAlreadyInProgressError)

        presenterCall.cancel()
    }

    @Test
    fun `a throwing presenter errors and releases the slot`() = runTest(dispatcher) {
        manager.checkpointOfferingPresenter = CheckpointOfferingPresenter { _, _ -> error("Simulated.") }
        resolvesTo(CheckpointResolution.MatchedOffering(mockk()))

        assertThat(checkpointErrorCode()).isEqualTo(PurchasesErrorCode.ConfigurationError)

        manager.checkpointOfferingPresenter = null
        var result: CheckpointResult? = null
        val call = launch { result = checkpoint() }
        finishPaywall(CheckpointPaywallOutcome.Dismissed)
        call.join()
        assertThat(result).isInstanceOf(CheckpointResult.PaywallPresented::class.java)
    }

    @Test
    fun `cancelling an app-owned presentation releases the slot and ignores a late report`() = runTest(dispatcher) {
        var completion: CheckpointOfferingCompletion? = null
        manager.checkpointOfferingPresenter = CheckpointOfferingPresenter { _, presentation ->
            completion = presentation
        }
        resolvesTo(CheckpointResolution.MatchedOffering(mockk()))
        val presenterCall = launch { checkpoint() }

        presenterCall.cancel()
        presenterCall.join()
        // The pending call died with the caller, so this report must be a no-op.
        completion!!.dismissed()

        manager.checkpointOfferingPresenter = null
        var result: CheckpointResult? = null
        val secondCall = launch { result = checkpoint() }
        finishPaywall(CheckpointPaywallOutcome.Dismissed)
        secondCall.join()
        assertThat(result).isInstanceOf(CheckpointResult.PaywallPresented::class.java)
    }

    @Test
    fun `every no-action reason maps to its result counterpart`() = runTest(dispatcher) {
        CheckpointResolution.NoAction.Reason.values().forEach { reason ->
            resolvesTo(CheckpointResolution.NoAction(reason))

            val result = checkpoint() as CheckpointResult.NoAction

            assertThat(result.reason.value).isEqualTo(reason.name)
        }
    }

    @Test
    fun `resolution failure propagates to the caller`() = runTest(dispatcher) {
        coEvery { mockPurchases.resolveCheckpoint(any(), any()) } throws PurchasesException(
            PurchasesError(PurchasesErrorCode.ConfigurationError, "Simulated."),
        )

        assertThat(checkpointErrorCode()).isEqualTo(PurchasesErrorCode.ConfigurationError)
    }

    @Test
    fun `checkpoint errors with ConfigurationError when no activity is available`() = runTest(dispatcher) {
        every { mockPurchases.currentActivity } returns null
        resolvesToWorkflow()

        assertThat(checkpointErrorCode()).isEqualTo(PurchasesErrorCode.ConfigurationError)
    }

    @Test
    fun `matched checkpoint presents the workflow and resolves when the paywall finishes`() = runTest(dispatcher) {
        resolvesToWorkflow()

        var result: CheckpointResult? = null
        val call = launch {
            result = checkpoint(CheckpointParams { customVariables { "goal" to "test" } })
        }

        assertThat(result).isNull()

        finishPaywall(CheckpointPaywallOutcome.Dismissed)
        call.join()

        val presented = result as CheckpointResult.PaywallPresented
        assertThat(presented.paywallOutcome).isEqualTo(CheckpointPaywallOutcome.Dismissed)
        val customVariables = mapOf("goal" to CustomVariableValue.String("test"))
        verifyOrder {
            mockListener.onCheckpointHit(CheckpointHitContext(checkpointId, customVariables))
            mockListener.onCheckpointCompleted(CheckpointCompletedContext(checkpointId, customVariables, presented))
        }
    }

    @Test
    fun `custom variables reach the resolver as rule dimensions`() = runTest(dispatcher) {
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.NO_MATCH))

        checkpoint(
            CheckpointParams {
                customVariables {
                    "goal" to "test"
                    "attempt" to 2
                    "flag" to true
                }
            },
        )

        val customVariables = slot<Map<String, RulesDimensionValue>>()
        coVerify { mockPurchases.resolveCheckpoint(checkpointId, capture(customVariables)) }
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
            checkpoint(
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

        finishPaywall(CheckpointPaywallOutcome.Dismissed)
        call.join()
    }

    @Test
    fun `the recorded outcome is the one delivered`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val customerInfo = mockk<CustomerInfo>()
        val storeTransaction = mockk<StoreTransaction>()
        var result: CheckpointResult? = null
        val call = launch { result = checkpoint() }

        finishPaywall(CheckpointPaywallOutcome.Purchased(customerInfo, storeTransaction))
        call.join()

        assertThat((result as CheckpointResult.PaywallPresented).paywallOutcome)
            .isEqualTo(CheckpointPaywallOutcome.Purchased(customerInfo, storeTransaction))
    }

    @Test
    fun `a web checkout outcome is delivered when the paywall dismisses`() = runTest(dispatcher) {
        resolvesToWorkflow()
        var result: CheckpointResult? = null
        val call = launch { result = checkpoint() }

        finishPaywall(CheckpointPaywallOutcome.WebCheckoutOpened)
        call.join()

        assertThat((result as CheckpointResult.PaywallPresented).paywallOutcome)
            .isEqualTo(CheckpointPaywallOutcome.WebCheckoutOpened)
    }

    @Test
    fun `a later outcome replaces an earlier web checkout outcome`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val customerInfo = mockk<CustomerInfo>()
        val storeTransaction = mockk<StoreTransaction>()
        var result: CheckpointResult? = null
        val call = launch { result = checkpoint() }
        val callId = currentCallId()
        manager.recordOutcome(callId, CheckpointPaywallOutcome.WebCheckoutOpened)

        finishPaywall(CheckpointPaywallOutcome.Purchased(customerInfo, storeTransaction))
        call.join()

        assertThat((result as CheckpointResult.PaywallPresented).paywallOutcome)
            .isEqualTo(CheckpointPaywallOutcome.Purchased(customerInfo, storeTransaction))
    }

    @Test
    fun `concurrent checkpoint errors with OperationAlreadyInProgressError`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val firstCall = launch { checkpoint() }

        assertThat(checkpointErrorCode()).isEqualTo(PurchasesErrorCode.OperationAlreadyInProgressError)

        firstCall.cancel()
    }

    @Test
    fun `checkpoint can present again after the previous workflow finishes`() = runTest(dispatcher) {
        resolvesToWorkflow()

        val firstCall = launch { checkpoint() }
        finishPaywall(CheckpointPaywallOutcome.Dismissed)
        firstCall.join()

        val secondCall = launch { checkpoint() }
        finishPaywall(CheckpointPaywallOutcome.Dismissed)
        secondCall.join()

        assertThat(presentedCallIds).hasSize(2)
    }

    @Test
    fun `checkpoint can present again after the presenter fails to show`() = runTest(dispatcher) {
        resolvesToWorkflow()
        every { mockPresenter.show(any()) } throws RuntimeException("show failed")

        assertThat(checkpointErrorCode()).isEqualTo(PurchasesErrorCode.ConfigurationError)

        every { mockPresenter.show(any()) } just runs
        var result: CheckpointResult? = null
        val call = launch { result = checkpoint() }
        finishPaywall(CheckpointPaywallOutcome.Dismissed)
        call.join()

        assertThat(result).isInstanceOf(CheckpointResult.PaywallPresented::class.java)
    }

    @Test
    fun `checkpoint can present again after being cancelled`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val firstCall = launch { checkpoint() }

        firstCall.cancel()

        var result: CheckpointResult? = null
        val secondCall = launch { result = checkpoint() }
        finishPaywall(CheckpointPaywallOutcome.Dismissed)
        secondCall.join()

        assertThat(result).isInstanceOf(CheckpointResult.PaywallPresented::class.java)
    }

    @Test
    fun `cancelling the caller abandons the presented paywall`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val call = launch { checkpoint() }

        call.cancel()
        call.join()

        verify { mockPresenter.abandon() }
    }

    @Test
    fun `a report from an abandoned presentation does not disturb the call that replaced it`() =
        runTest(dispatcher) {
            resolvesToWorkflow()
            val firstCall = launch { checkpoint() }
            val abandonedCallId = currentCallId()
            firstCall.cancel()
            firstCall.join()

            var result: CheckpointResult? = null
            val secondCall = launch { result = checkpoint() }
            manager.recordOutcome(abandonedCallId, CheckpointPaywallOutcome.Error(mockk()))
            manager.onPresentationFinished(abandonedCallId)

            assertThat(result).isNull()

            finishPaywall(CheckpointPaywallOutcome.Dismissed)
            secondCall.join()

            assertThat((result as CheckpointResult.PaywallPresented).paywallOutcome)
                .isEqualTo(CheckpointPaywallOutcome.Dismissed)
        }

    @Test
    fun `finishing an unknown callId is a no-op`() {
        manager.onPresentationFinished("unknown-call-id")
        manager.recordOutcome("unknown-call-id", CheckpointPaywallOutcome.Dismissed)

        assertThat(manager.presentation("unknown-call-id")).isNull()
    }

    @Test
    fun `finishing the presentation releases the pending call`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val customerInfo = mockk<CustomerInfo>()
        val storeTransaction = mockk<StoreTransaction>()
        var result: CheckpointResult? = null
        val call = launch { result = checkpoint() }
        val callId = currentCallId()
        manager.recordOutcome(callId, CheckpointPaywallOutcome.Purchased(customerInfo, storeTransaction))

        manager.onPresentationFinished(callId)
        call.join()

        assertThat((result as CheckpointResult.PaywallPresented).paywallOutcome)
            .isEqualTo(CheckpointPaywallOutcome.Purchased(customerInfo, storeTransaction))
        assertThat(manager.presentation(callId)).isNull()
    }

    @Test
    fun `checkpoint works without a listener`() = runTest(dispatcher) {
        manager.checkpointListener = null
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.NO_MATCH))

        assertThat(checkpoint()).isInstanceOf(CheckpointResult.NoAction::class.java)
    }

    private fun resolvesTo(resolution: CheckpointResolution) {
        coEvery { mockPurchases.resolveCheckpoint(any(), any()) } returns resolution
    }

    private fun resolvesToWorkflow() {
        resolvesTo(CheckpointResolution.MatchedWorkflow(mockk(), mockk(), mockk()))
    }

    private fun currentCallId(): String = presentedCallIds.last()

    // Mirrors what CheckpointWorkflowPresenter does: read the pending call for the presented callId, record
    // the outcome, then report the paywall as finished.
    private fun finishPaywall(outcome: CheckpointPaywallOutcome) {
        val callId = currentCallId()
        assertThat(manager.presentation(callId)).isNotNull
        manager.recordOutcome(callId, outcome)
        manager.onPresentationFinished(callId)
    }

    private suspend fun checkpoint(params: CheckpointParams? = null): CheckpointResult =
        manager.checkpoint(mockPurchases, checkpointId, params)

    private suspend fun checkpointErrorCode(): PurchasesErrorCode? = try {
        checkpoint()
        null
    } catch (e: PurchasesException) {
        e.code
    }
}
