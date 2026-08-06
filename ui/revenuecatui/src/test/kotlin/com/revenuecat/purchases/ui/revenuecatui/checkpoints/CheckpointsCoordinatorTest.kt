@file:OptIn(ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.checkpoints.CheckpointResolution
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
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
class CheckpointsCoordinatorTest {

    private val checkpointId = "test_checkpoint"
    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var mockPurchases: Purchases
    private lateinit var mockActivity: Activity
    private lateinit var mockListener: CheckpointListener
    private var listenerSlot: Any? = null
    private val startedIntents = mutableListOf<Intent>()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        CheckpointsCoordinator.resetForTesting()
        startedIntents.clear()
        mockActivity = mockk(relaxed = true)
        capturesStartedIntents()
        mockListener = mockk(relaxed = true)
        listenerSlot = mockListener
        mockPurchases = mockk {
            every { currentActivity } returns mockActivity
            every { checkpointListenerSlot } answers { listenerSlot }
            every { checkpointListenerSlot = any() } answers { listenerSlot = firstArg() }
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        CheckpointsCoordinator.resetForTesting()
    }

    @Test
    fun `the listener is stored on the Purchases instance`() {
        mockPurchases.checkpointListener = null
        assertThat(mockPurchases.checkpointListener).isNull()

        mockPurchases.checkpointListener = mockListener

        assertThat(mockPurchases.checkpointListener).isEqualTo(mockListener)
    }

    @Test
    fun `unmatched checkpoint resolves NoAction with the mapped reason`() = runTest(dispatcher) {
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.UNKNOWN_CHECKPOINT))

        val result = checkpoint() as CheckpointResult.NoAction

        assertThat(result.reason).isEqualTo(CheckpointResult.NoAction.Reason.UNKNOWN_CHECKPOINT)
        assertThat(result.checkpoint.identifier).isEqualTo(checkpointId)
        verifyOrder {
            mockListener.onCheckpointHit(result.checkpoint)
            mockListener.onCheckpointCompleted(result.checkpoint, result)
        }
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
        val call = launch { result = checkpoint(CheckpointParams("goal" to "test")) }

        assertThat(result).isNull()

        finishPaywall(CheckpointPaywallOutcome.Dismissed)
        call.join()

        val presented = result as CheckpointResult.PaywallPresented
        assertThat(presented.paywallOutcome).isEqualTo(CheckpointPaywallOutcome.Dismissed)
        assertThat(presented.checkpoint.identifier).isEqualTo(checkpointId)
        assertThat(presented.checkpoint.params.customProperties).isEqualTo(mapOf("goal" to "test"))
        verifyOrder {
            mockListener.onCheckpointHit(any())
            mockListener.onCheckpointCompleted(presented.checkpoint, presented)
        }
    }

    @Test
    fun `only valid custom properties are forwarded to the resolver`() = runTest(dispatcher) {
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.NO_MATCH))

        checkpoint(CheckpointParams("goal" to "test", "invalid" to Any()))

        val customProperties = slot<Map<String, Any>>()
        coVerify { mockPurchases.resolveCheckpoint(checkpointId, capture(customProperties)) }
        assertThat(customProperties.captured).isEqualTo(mapOf("goal" to "test"))
    }

    @Test
    fun `the recorded outcome is the one delivered`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val customerInfo = mockk<CustomerInfo>()
        var result: CheckpointResult? = null
        val call = launch { result = checkpoint() }

        finishPaywall(CheckpointPaywallOutcome.Purchased(customerInfo))
        call.join()

        assertThat((result as CheckpointResult.PaywallPresented).paywallOutcome)
            .isEqualTo(CheckpointPaywallOutcome.Purchased(customerInfo))
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

        verify(exactly = 2) { mockActivity.startActivity(any()) }
    }

    @Test
    fun `checkpoint can present again after startActivity throws`() = runTest(dispatcher) {
        resolvesToWorkflow()
        every { mockActivity.startActivity(any()) } throws RuntimeException("startActivity failed")

        assertThat(checkpointErrorCode()).isEqualTo(PurchasesErrorCode.ConfigurationError)

        capturesStartedIntents()
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
    fun `finishing an unknown callId is a no-op`() {
        CheckpointsCoordinator.onPaywallFinished("unknown-call-id")
        CheckpointsCoordinator.recordOutcome("unknown-call-id", CheckpointPaywallOutcome.Dismissed)

        assertThat(CheckpointsCoordinator.resolution("unknown-call-id")).isNull()
    }

    @Test
    fun `checkpoint works without a listener`() = runTest(dispatcher) {
        listenerSlot = null
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.NO_MATCH))

        assertThat(checkpoint()).isInstanceOf(CheckpointResult.NoAction::class.java)
    }

    private fun resolvesTo(resolution: CheckpointResolution) {
        coEvery { mockPurchases.resolveCheckpoint(any(), any()) } returns resolution
    }

    private fun resolvesToWorkflow() {
        resolvesTo(CheckpointResolution.Workflow(mockk(), mockk(), mockk()))
    }

    private fun capturesStartedIntents() {
        every { mockActivity.startActivity(capture(startedIntents)) } just runs
    }

    // Mirrors what CheckpointWorkflowActivity does: read the pending call from the launched Intent's callId,
    // record the outcome, then report the paywall as finished.
    private fun finishPaywall(outcome: CheckpointPaywallOutcome) {
        val callId = startedIntents.last().getStringExtra(CheckpointWorkflowActivity.EXTRA_CALL_ID)!!
        assertThat(CheckpointsCoordinator.resolution(callId)).isNotNull
        CheckpointsCoordinator.recordOutcome(callId, outcome)
        CheckpointsCoordinator.onPaywallFinished(callId)
    }

    private suspend fun checkpoint(params: CheckpointParams? = null): CheckpointResult =
        CheckpointsCoordinator.checkpoint(mockPurchases, checkpointId, params)

    private suspend fun checkpointErrorCode(): PurchasesErrorCode? = try {
        checkpoint()
        null
    } catch (e: PurchasesException) {
        e.code
    }
}
