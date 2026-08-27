@file:OptIn(ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import android.content.Intent
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
    private val startedIntents = mutableListOf<Intent>()

    private lateinit var manager: CheckpointsManager

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        mockkObject(Logger)
        every { Logger.e(any()) } just runs
        startedIntents.clear()
        mockActivity = mockk(relaxed = true)
        capturesStartedIntents()
        mockListener = mockk(relaxed = true)
        mockPurchases = mockk {
            every { currentActivity } returns mockActivity
        }
        manager = CheckpointsManager()
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
            mockListener.onCheckpointHit(OnCheckpointHitContext(invalidIdentifier, emptyMap()))
            mockListener.onCheckpointCompleted(OnCheckpointCompletedContext(invalidIdentifier, emptyMap(), result))
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
            mockListener.onCheckpointHit(OnCheckpointHitContext(checkpointId, emptyMap()))
            mockListener.onCheckpointCompleted(OnCheckpointCompletedContext(checkpointId, emptyMap(), result))
        }
    }

    @Test
    fun `offering checkpoint returns without an activity or presentation`() = runTest(dispatcher) {
        val offering = mockk<Offering>()
        every { mockPurchases.currentActivity } returns null
        resolvesTo(CheckpointResolution.MatchedOffering(offering))

        val result = checkpoint() as CheckpointResult.ReceivedOffering

        assertThat(result.offering).isEqualTo(offering)
        verify(exactly = 0) { mockActivity.startActivity(any()) }
        verifyOrder {
            mockListener.onCheckpointHit(OnCheckpointHitContext(checkpointId, emptyMap()))
            mockListener.onCheckpointCompleted(OnCheckpointCompletedContext(checkpointId, emptyMap(), result))
        }
    }

    @Test
    fun `offering checkpoint completes while a UI checkpoint is being presented`() = runTest(dispatcher) {
        val offering = mockk<Offering>()
        coEvery { mockPurchases.resolveCheckpoint(any(), any()) } returnsMany listOf(
            CheckpointResolution.MatchedWorkflow(mockk(), mockk(), mockk()),
            CheckpointResolution.MatchedOffering(offering),
        )
        val presentedCall = launch { checkpoint() }

        val offeringResult = checkpoint() as CheckpointResult.ReceivedOffering

        assertThat(offeringResult.offering).isEqualTo(offering)
        verify(exactly = 1) { mockActivity.startActivity(any()) }
        presentedCall.cancel()
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
            mockListener.onCheckpointHit(OnCheckpointHitContext(checkpointId, customVariables))
            mockListener.onCheckpointCompleted(OnCheckpointCompletedContext(checkpointId, customVariables, presented))
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
    fun `cancelling the caller finishes the presented paywall`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val presentedActivity = mockk<Activity>(relaxed = true)
        val call = launch { checkpoint() }
        manager.onPresentationStarted(currentCallId(), presentedActivity)

        call.cancel()
        call.join()

        verify { presentedActivity.finish() }
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
            manager.onActivityDestroyed(abandonedCallId, isChangingConfigurations = false)

            assertThat(result).isNull()

            finishPaywall(CheckpointPaywallOutcome.Dismissed)
            secondCall.join()

            assertThat((result as CheckpointResult.PaywallPresented).paywallOutcome)
                .isEqualTo(CheckpointPaywallOutcome.Dismissed)
        }

    @Test
    fun `finishing an unknown callId is a no-op`() {
        manager.onActivityDestroyed("unknown-call-id", isChangingConfigurations = false)
        manager.recordOutcome("unknown-call-id", CheckpointPaywallOutcome.Dismissed)

        assertThat(manager.presentation("unknown-call-id")).isNull()
    }

    @Test
    fun `a destroy for a configuration change keeps the pending call alive`() = runTest(dispatcher) {
        resolvesToWorkflow()
        var result: CheckpointResult? = null
        val call = launch { result = checkpoint() }
        val callId = currentCallId()

        manager.onActivityDestroyed(callId, isChangingConfigurations = true)

        assertThat(result).isNull()
        assertThat(manager.presentation(callId)).isNotNull

        finishPaywall(CheckpointPaywallOutcome.Dismissed)
        call.join()

        assertThat(result).isInstanceOf(CheckpointResult.PaywallPresented::class.java)
    }

    @Test
    fun `a destroy that is not a configuration change releases the pending call`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val customerInfo = mockk<CustomerInfo>()
        val storeTransaction = mockk<StoreTransaction>()
        var result: CheckpointResult? = null
        val call = launch { result = checkpoint() }
        val callId = currentCallId()
        manager.recordOutcome(callId, CheckpointPaywallOutcome.Purchased(customerInfo, storeTransaction))

        manager.onActivityDestroyed(callId, isChangingConfigurations = false)
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

    private fun capturesStartedIntents() {
        every { mockActivity.startActivity(capture(startedIntents)) } just runs
    }

    private fun currentCallId(): String =
        startedIntents.last().getStringExtra(CheckpointWorkflowActivity.EXTRA_CALL_ID)!!

    // Mirrors what CheckpointWorkflowActivity does: read the pending call from the launched Intent's callId,
    // record the outcome, then report the paywall as finished.
    private fun finishPaywall(outcome: CheckpointPaywallOutcome) {
        val callId = currentCallId()
        assertThat(manager.presentation(callId)).isNotNull
        manager.recordOutcome(callId, outcome)
        manager.onActivityDestroyed(callId, isChangingConfigurations = false)
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
