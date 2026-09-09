@file:OptIn(ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.Offering
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
class CheckpointPassedCallbackTest {

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
        presentedCallIds.clear()
        results.clear()
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
    fun `a no-action checkpoint invokes the callback exactly once with null`() = runTest(dispatcher) {
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.UNKNOWN_CHECKPOINT))

        checkpoint()

        assertThat(results).containsExactly(null)
    }

    @Test
    fun `an invalid identifier invokes the callback with null instead of throwing`() = runTest(dispatcher) {
        checkpoint(identifier = " bad😀")

        assertThat(results).containsExactly(null)
    }

    @Test
    fun `a presented workflow delivers the grants obtained during it`() = runTest(dispatcher) {
        cachedCustomerInfoHasActive("plus")
        resolvesToWorkflow()

        checkpoint()
        assertThat(results).isEmpty()

        finishPaywall(CheckpointFlowOutcome.Purchased(customerInfoWithActive("plus", "pro"), mockk()))

        assertThat(results.obtained).containsExactly(setOf("pro"))
    }

    @Test
    fun `an offering checkpoint presents the fallback paywall and delivers what the user obtained`() =
        runTest(dispatcher) {
            resolvesTo(CheckpointResolution.MatchedOffering(mockk(), checkpointRuleId = null))

            checkpoint()
            assertThat(results).isEmpty()

            finishPaywall(CheckpointFlowOutcome.Purchased(customerInfoWithActive("pro"), mockk()))

            assertThat(results.obtained).containsExactly(setOf("pro"))
        }

    @Test
    fun `a dismissed paywall delivers an empty result`() = runTest(dispatcher) {
        resolvesToWorkflow()

        checkpoint()
        finishPaywall(CheckpointFlowOutcome.Dismissed)

        assertThat(results.obtained).containsExactly(emptySet<String>())
    }

    @Test
    fun `a paywall the user backed out of never invokes the callback and releases the slot`() = runTest(dispatcher) {
        resolvesToWorkflow()

        checkpoint()
        finishPaywall(outcome = null, navigatedBack = true)

        assertThat(results).isEmpty()

        checkpoint()

        assertThat(presentedCallIds).hasSize(2)
    }

    @Test
    fun `a purchase followed by backing out still invokes the callback with the grants`() = runTest(dispatcher) {
        resolvesToWorkflow()

        checkpoint()
        finishPaywall(CheckpointFlowOutcome.Purchased(customerInfoWithActive("pro"), mockk()), navigatedBack = true)

        assertThat(results.obtained).containsExactly(setOf("pro"))
    }

    @Test
    fun `an app-owned presentation delivers the grants the SDK finds after syncing`() = runTest(dispatcher) {
        cachedCustomerInfoHasActive("plus")
        every { mockPurchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, any()) } answers {
            secondArg<ReceiveCustomerInfoCallback>().onReceived(customerInfoWithActive("plus", "pro"))
        }
        val completion = presentThroughRegisteredPresenter()

        checkpoint()
        assertThat(results).isEmpty()

        completion()!!.complete(PaywallPresenter.Completion.Result.Closed)

        assertThat(results.obtained).containsExactly(setOf("pro"))
    }

    @Test
    fun `an app-owned presentation the user backed out of never invokes the callback`() = runTest(dispatcher) {
        val completion = presentThroughRegisteredPresenter()

        checkpoint()
        completion()!!.complete(PaywallPresenter.Completion.Result.NavigatedBack)

        assertThat(results).isEmpty()
        verify(exactly = 0) { mockPurchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, any()) }
    }

    @Test
    fun `a missing cached customer info counts every entitlement active afterwards as granted`() =
        runTest(dispatcher) {
            noCachedCustomerInfo()
            resolvesToWorkflow()

            checkpoint()
            finishPaywall(CheckpointFlowOutcome.Restored(customerInfoWithActive("pro")))

            assertThat(results.obtained).containsExactly(setOf("pro"))
        }

    @Test
    fun `a presentation failure invokes the callback with null`() = runTest(dispatcher) {
        every { mockPurchases.currentActivity } returns null
        resolvesToWorkflow()

        checkpoint()

        assertThat(results).containsExactly(null)
    }

    @Test
    fun `a paywall that ended in an error invokes the callback with null`() = runTest(dispatcher) {
        resolvesToWorkflow()

        checkpoint()
        finishPaywall(CheckpointFlowOutcome.Error(PurchasesError(PurchasesErrorCode.StoreProblemError, "boom")))

        assertThat(results).containsExactly(null)
    }

    private fun checkpoint(identifier: String = checkpointId) {
        manager.checkpoint(mockPurchases, identifier, null) { results += it }
    }

    private val List<FlowResult?>.obtained: List<Set<String>?>
        get() = map { result -> result?.obtainedEntitlements?.map { it.entitlementInfo.identifier }?.toSet() }

    // Registers a presenter for a matched offering and returns an accessor for the completion it was handed.
    private fun presentThroughRegisteredPresenter(offering: Offering = mockk()): () -> PaywallPresenter.Completion? {
        var completion: PaywallPresenter.Completion? = null
        manager.paywallPresenter = PaywallPresenter { _, presentation ->
            completion = presentation
        }
        resolvesTo(CheckpointResolution.MatchedOffering(offering, checkpointRuleId = null))
        return { completion }
    }

    private fun resolvesTo(resolution: CheckpointResolution) {
        coEvery { mockPurchases.internalResolveCp(any(), any()) } returns resolution
    }

    private fun resolvesToWorkflow() {
        resolvesTo(CheckpointResolution.MatchedWorkflow(mockk(), mockk(), mockk(), checkpointRuleId = null))
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

    private fun customerInfoWithActive(vararg identifiers: String): CustomerInfo {
        val active = identifiers.associateWith { id -> mockk<EntitlementInfo> { every { identifier } returns id } }
        return mockk { every { entitlements.active } returns active }
    }

    // Mirrors what CheckpointWorkflowPresenter does: record the outcome for the presented call, then report the
    // paywall as finished.
    private fun finishPaywall(outcome: CheckpointFlowOutcome?, navigatedBack: Boolean = false) {
        val callId = presentedCallIds.last()
        outcome?.let { manager.recordOutcome(callId, it) }
        manager.onPresentationFinished(callId, navigatedBack)
    }
}
