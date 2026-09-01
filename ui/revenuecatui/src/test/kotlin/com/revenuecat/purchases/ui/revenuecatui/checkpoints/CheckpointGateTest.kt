@file:OptIn(ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.checkpoints.CheckpointResolution
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class CheckpointGateTest {

    private val checkpointId = "test_checkpoint"
    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var mockPurchases: Purchases
    private lateinit var mockActivity: Activity
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
        mockPurchases = mockk {
            every { currentActivity } returns mockActivity
        }
        cachedCustomerInfoHasActive()
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
    fun `a no-action checkpoint invokes the callback exactly once with the mapped reason`() = runTest(dispatcher) {
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.UNKNOWN_CHECKPOINT))

        val gateResults = checkpointGate()

        assertThat(gateResults).hasSize(1)
        assertThat(gateResults.single().noWorkflowReason)
            .isEqualTo(CheckpointGateResult.NoWorkflowReason.UNKNOWN_CHECKPOINT)
    }

    @Test
    fun `an invalid identifier folds into the result instead of throwing`() = runTest(dispatcher) {
        val gateResults = mutableListOf<CheckpointGateResult>()

        manager.checkpointGate(mockPurchases, " bad😀", null) { gateResults += it }

        assertThat(gateResults.single().noWorkflowReason)
            .isEqualTo(CheckpointGateResult.NoWorkflowReason.INVALID_CHECKPOINT_IDENTIFIER)
    }

    @Test
    fun `a presented workflow delivers the grants obtained during it`() = runTest(dispatcher) {
        cachedCustomerInfoHasActive("plus")
        resolvesToWorkflow()

        val gateResults = mutableListOf<CheckpointGateResult>()
        checkpointGate(gateResults)
        assertThat(gateResults).isEmpty()

        finishPaywall(CheckpointPaywallOutcome.Purchased(customerInfoWithActive("plus", "pro"), mockk()))

        assertThat(gateResults.single().entitlements)
            .containsExactly(EntitlementGrant("pro"))
        assertThat(gateResults.single().noWorkflowReason).isNull()
    }

    @Test
    fun `an offering checkpoint presents the fallback paywall and delivers what the user obtained`() =
        runTest(dispatcher) {
            resolvesTo(CheckpointResolution.MatchedOffering(mockk()))

            val gateResults = mutableListOf<CheckpointGateResult>()
            checkpointGate(gateResults)
            assertThat(gateResults).isEmpty()

            finishPaywall(CheckpointPaywallOutcome.Purchased(customerInfoWithActive("pro"), mockk()))

            assertThat(gateResults.single().entitlements)
                .containsExactly(EntitlementGrant("pro"))
            assertThat(gateResults.single().noWorkflowReason).isNull()
            assertThat(gateResults.single().error).isNull()
        }

    @Test
    fun `a missing cached customer info counts every entitlement active afterwards as granted`() =
        runTest(dispatcher) {
            noCachedCustomerInfo()
            resolvesToWorkflow()

            val gateResults = mutableListOf<CheckpointGateResult>()
            checkpointGate(gateResults)

            finishPaywall(CheckpointPaywallOutcome.Restored(customerInfoWithActive("pro")))

            assertThat(gateResults.single().entitlements)
                .containsExactly(EntitlementGrant("pro"))
        }

    @Test
    fun `a presentation failure folds into the result instead of throwing`() = runTest(dispatcher) {
        every { mockPurchases.currentActivity } returns null
        resolvesToWorkflow()

        val gateResults = checkpointGate()

        assertThat(gateResults.single().noWorkflowReason).isEqualTo(CheckpointGateResult.NoWorkflowReason.ERROR)
        assertThat(gateResults.single().error?.code).isEqualTo(PurchasesErrorCode.ConfigurationError)
    }

    @Test
    fun `the gate call fires the same listener events as the suspend call`() = runTest(dispatcher) {
        val mockListener = mockk<CheckpointListener>(relaxed = true)
        manager.checkpointListener = mockListener
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.NO_MATCH))

        checkpointGate()

        verify(exactly = 1) { mockListener.onCheckpointHit(match { it.identifier == checkpointId }) }
        verify(exactly = 1) { mockListener.onCheckpointCompleted(match { it.identifier == checkpointId }) }
    }

    private fun checkpointGate(
        gateResults: MutableList<CheckpointGateResult> = mutableListOf(),
    ): List<CheckpointGateResult> {
        manager.checkpointGate(mockPurchases, checkpointId, null) { gateResults += it }
        return gateResults
    }

    private fun resolvesTo(resolution: CheckpointResolution) {
        coEvery { mockPurchases.resolveCheckpoint(any(), any()) } returns resolution
    }

    private fun resolvesToWorkflow() {
        resolvesTo(CheckpointResolution.MatchedWorkflow(mockk(), mockk(), mockk()))
    }

    private fun cachedCustomerInfoHasActive(vararg identifiers: String) {
        every { mockPurchases.getCustomerInfo(CacheFetchPolicy.CACHE_ONLY, any()) } answers {
            secondArg<ReceiveCustomerInfoCallback>().onReceived(customerInfoWithActive(*identifiers))
        }
    }

    private fun noCachedCustomerInfo() {
        every { mockPurchases.getCustomerInfo(CacheFetchPolicy.CACHE_ONLY, any()) } answers {
            secondArg<ReceiveCustomerInfoCallback>()
                .onError(PurchasesError(PurchasesErrorCode.CustomerInfoError, "No cache."))
        }
    }

    private fun customerInfoWithActive(vararg identifiers: String): CustomerInfo = mockk {
        every { entitlements.active } returns identifiers.associateWith { mockk() }
    }

    // Mirrors what CheckpointWorkflowPresenter does: record the outcome for the presented call, then report the
    // paywall as finished.
    private fun finishPaywall(outcome: CheckpointPaywallOutcome) {
        val callId = presentedCallIds.last()
        manager.recordOutcome(callId, outcome)
        manager.onPresentationFinished(callId)
    }
}
