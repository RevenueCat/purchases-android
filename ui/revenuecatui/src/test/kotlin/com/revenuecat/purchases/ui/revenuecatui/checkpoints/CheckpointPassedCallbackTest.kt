@file:OptIn(ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.checkpoints.CheckpointResolution
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.ui.revenuecatui.PaywallDismissReason
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class CheckpointPassedCallbackTest {

    private val checkpointId = "test_checkpoint"
    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var mockPurchases: Purchases
    private lateinit var mockActivity: Activity
    private lateinit var mockPresenter: CheckpointWorkflowPresenter
    private var defaultPresenter: DefaultPaywallPresenter? = null
    private val presentedCallIds = mutableListOf<String>()
    private val results = mutableListOf<CheckpointResult>()
    private val errorPresenter = RecordingErrorPresenter()
    private var defaultErrorPresenterFactory: (Purchases) -> DefaultErrorPresenter = { _ ->
        // The SDK's own dialog needs a real activity; by default it acknowledges every error right away.
        mockk<DefaultErrorPresenter> {
            every { present(any(), any()) } answers { secondArg<ErrorPresenter.Completion>().complete() }
        }
    }

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
        defaultPresenter = null
        mockPurchases = mockk {
            every { currentActivity } returns mockActivity
        }
        cachedCustomerInfoHasActive()
        manager = CheckpointsManager(
            presenterFactory = { callId, _, _ ->
                presentedCallIds += callId
                mockPresenter
            },
            defaultPresenterFactory = { purchases, errorPresenter ->
                DefaultPaywallPresenter(purchases, errorPresenter) { _, _, _ -> mockPresenter }
                    .also { defaultPresenter = it }
            },
            defaultErrorPresenterFactory = { defaultErrorPresenterFactory(it) },
        )
    }

    @After
    fun tearDown() {
        unmockkObject(Logger)
        Dispatchers.resetMain()
    }

    @Test
    fun `a no-action checkpoint invokes the callback exactly once without a flow`() = runTest(dispatcher) {
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.UNKNOWN_CHECKPOINT))

        checkpoint()

        assertThat(results).containsExactly(CheckpointResult(flow = null))
    }

    @Test
    fun `an unknown checkpoint invokes the callback without a flow and no error`() = runTest(dispatcher) {
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.NO_MATCH))
        manager.errorPresenter = errorPresenter

        checkpoint()

        assertThat(results).containsExactly(CheckpointResult(flow = null))
        assertThat(errorPresenter.kinds).isEmpty()
    }

    @Test
    fun `unavailable configuration is presented and the callback is not invoked`() = runTest(dispatcher) {
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE))
        manager.errorPresenter = errorPresenter

        checkpoint()
        errorPresenter.complete()

        assertThat(results).isEmpty()
        assertThat(errorPresenter.kinds).containsExactly(CheckpointFailureKind.FLOW_COULD_NOT_BE_SHOWN)
    }

    @Test
    fun `a resolution that throws is presented and the callback is not invoked`() = runTest(dispatcher) {
        val error = PurchasesError(PurchasesErrorCode.ConfigurationError, "Simulated.")
        coEvery { mockPurchases.internalResolveCp(any(), any()) } throws PurchasesException(error)
        manager.errorPresenter = errorPresenter

        checkpoint()
        errorPresenter.complete()

        assertThat(results).isEmpty()
        assertThat(errorPresenter.errors).containsExactly(error)
        assertThat(errorPresenter.kinds).containsExactly(CheckpointFailureKind.FLOW_COULD_NOT_BE_SHOWN)
    }

    @Test
    fun `an invalid identifier is presented, the callback is not invoked, and nothing is resolved`() =
        runTest(dispatcher) {
            manager.errorPresenter = errorPresenter

            checkpoint(identifier = " bad😀")
            errorPresenter.complete()

            assertThat(results).isEmpty()
            assertThat(errorPresenter.kinds).containsExactly(CheckpointFailureKind.FLOW_COULD_NOT_BE_SHOWN)
            coVerify(exactly = 0) { mockPurchases.internalResolveCp(any(), any()) }
        }

    @Test
    fun `a missing activity is presented and the callback is not invoked`() = runTest(dispatcher) {
        every { mockPurchases.currentActivity } returns null
        resolvesToWorkflow()
        manager.errorPresenter = errorPresenter

        checkpoint()
        errorPresenter.complete()

        assertThat(results).isEmpty()
        assertThat(errorPresenter.kinds).containsExactly(CheckpointFailureKind.FLOW_COULD_NOT_BE_SHOWN)
    }

    @Test
    fun `a workflow that fails to show is presented and the callback is not invoked`() = runTest(dispatcher) {
        every { mockPresenter.show(any()) } throws IllegalStateException("show failed")
        resolvesToWorkflow()
        manager.errorPresenter = errorPresenter

        checkpoint()
        errorPresenter.complete()

        assertThat(results).isEmpty()
        assertThat(errorPresenter.kinds).containsExactly(CheckpointFailureKind.FLOW_COULD_NOT_BE_SHOWN)
    }

    @Test
    fun `the SDK's own dialog shows customer-facing copy and the callback is not invoked once acknowledged`() =
        runTest(dispatcher) {
            val controller = Robolectric.buildActivity(Activity::class.java).setup()
            every { mockPurchases.currentActivity } returns controller.get()
            every { mockPurchases.preferredUILocaleOverride } returns null
            val messages = mutableListOf<String>()
            var acknowledge: (() -> Unit)? = null
            defaultErrorPresenterFactory = { purchases ->
                DefaultErrorPresenter(purchases) { activity, _, message, onAcknowledged ->
                    messages += message
                    acknowledge = onAcknowledged
                    View(activity)
                }
            }
            coEvery { mockPurchases.internalResolveCp(any(), any()) } throws
                PurchasesException(PurchasesError(PurchasesErrorCode.ConfigurationError, "Simulated."))

            checkpoint()

            assertThat(messages)
                .containsExactly(controller.get().getString(R.string.revenuecatui_checkpoint_error_load))
            assertThat(ShadowDialog.getLatestDialog().isShowing).isTrue
            acknowledge!!()
            assertThat(ShadowDialog.getLatestDialog().isShowing).isFalse
            assertThat(results).isEmpty()
        }

    @Test
    fun `a purchase error followed by a close delivers an empty result`() = runTest(dispatcher) {
        resolvesToWorkflow()

        checkpoint()
        workflowListener().onPurchaseError(PurchasesError(PurchasesErrorCode.StoreProblemError, "boom"))
        finishPaywall(outcome = null)

        assertThat(results.obtained).containsExactly(emptySet<String>())
    }

    @Test
    fun `a workflow that fails to come back after a configuration change is presented and does not pass`() =
        runTest(dispatcher) {
            resolvesToWorkflow()
            manager.errorPresenter = errorPresenter

            checkpoint()
            manager.onPresentationFailed(
                presentedCallIds.last(),
                PurchasesError(PurchasesErrorCode.ConfigurationError, "Re-present failed."),
            )
            errorPresenter.complete()

            assertThat(results).isEmpty()
            assertThat(errorPresenter.kinds).containsExactly(CheckpointFailureKind.FLOW_ACTION_FAILED)
        }

    @Test
    fun `the SDK's own paywall that cannot load does not pass`() = runTest(dispatcher) {
        resolvesTo(CheckpointResolution.MatchedOffering(mockk(), checkpointRuleId = null))

        checkpoint()
        defaultPresenter!!.paywallOptions("") {}!!.dismissRequestWithExitOffering!!(
            null,
            PaywallResult.Error(PurchasesError(PurchasesErrorCode.ConfigurationError, "No packages available")),
            PaywallDismissReason.CLOSE,
        )
        defaultPresenter!!.onPresentationFinished("")

        assertThat(results).isEmpty()
        verify(exactly = 0) { mockPurchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, any()) }
    }

    @Test
    fun `the SDK's own paywall that fails to come back after a configuration change is presented and does not pass`() =
        runTest(dispatcher) {
            resolvesTo(CheckpointResolution.MatchedOffering(mockk(), checkpointRuleId = null))
            manager.errorPresenter = errorPresenter

            checkpoint()
            defaultPresenter!!.onPresentationFailed(
                "",
                PurchasesError(PurchasesErrorCode.ConfigurationError, "Re-present failed."),
            )
            errorPresenter.complete()

            assertThat(results).isEmpty()
            assertThat(errorPresenter.kinds).containsExactly(CheckpointFailureKind.FLOW_ACTION_FAILED)
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
            every { mockPurchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, any()) } answers {
                secondArg<ReceiveCustomerInfoCallback>().onReceived(customerInfoWithActive("pro"))
            }

            checkpoint()
            assertThat(results).isEmpty()

            defaultPresenter!!.onPresentationFinished("")

            assertThat(results.obtained).containsExactly(setOf("pro"))
        }

    @Test
    fun `a workflow restore that unlocks nothing does not pass the checkpoint on back`() = runTest(dispatcher) {
        cachedCustomerInfoHasActive("plus")
        resolvesToWorkflow()

        checkpoint()
        workflowListener().onRestoreCompleted(customerInfoWithActive("plus"))
        finishPaywall(outcome = null, navigatedBack = true)

        assertThat(results).isEmpty()
    }

    @Test
    fun `a workflow restore that unlocks an entitlement passes the checkpoint on back`() = runTest(dispatcher) {
        cachedCustomerInfoHasActive("plus")
        resolvesToWorkflow()

        checkpoint()
        workflowListener().onRestoreCompleted(customerInfoWithActive("plus", "pro"))
        finishPaywall(outcome = null, navigatedBack = true)

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
    fun `a paywall that ended in an error does not invoke the callback`() = runTest(dispatcher) {
        resolvesToWorkflow()

        checkpoint()
        finishPaywall(CheckpointFlowOutcome.Error(PurchasesError(PurchasesErrorCode.ConfigurationError, "boom")))

        assertThat(results).isEmpty()
    }

    @Test
    fun `a sync failure after an app-owned presentation presents the error and then passes with an empty result`() =
        runTest(dispatcher) {
            syncFails()
            val kinds = mutableListOf<CheckpointFailureKind>()
            var errorCompletion: ErrorPresenter.Completion? = null
            manager.errorPresenter = ErrorPresenter { params, completion ->
                kinds += params.kind
                errorCompletion = completion
            }
            val completion = presentThroughRegisteredPresenter()

            checkpoint()
            completion()!!.complete(PaywallPresenter.Completion.Result.Closed)

            assertThat(kinds).containsExactly(CheckpointFailureKind.FLOW_RESULT_SYNC_FAILED)
            assertThat(results).isEmpty()
            errorCompletion!!.complete()
            assertThat(results.obtained).containsExactly(emptySet<String>())
        }

    @Test
    fun `a sync failure the SDK's own dialog cannot show still passes with an empty result`() = runTest(dispatcher) {
        syncFails()
        every { mockPurchases.currentActivity } returns null
        val completion = presentThroughRegisteredPresenter()

        checkpoint()
        completion()!!.complete(PaywallPresenter.Completion.Result.Closed)

        assertThat(results.obtained).containsExactly(emptySet<String>())
    }

    private class RecordingErrorPresenter : ErrorPresenter {
        val errors = mutableListOf<PurchasesError>()
        val kinds = mutableListOf<CheckpointFailureKind>()
        private val completions = mutableListOf<ErrorPresenter.Completion>()

        override fun present(params: ErrorPresenter.Params, completion: ErrorPresenter.Completion) {
            errors += params.error
            kinds += params.kind
            completions += completion
        }

        fun complete() = completions.last().complete()
    }

    @Test
    fun `a presenter holding a single pending request sees overlapping failures one at a time`() =
        runTest(dispatcher) {
            syncFails()
            val presenter = SingleRequestErrorPresenter()
            manager.errorPresenter = presenter
            val completion = presentThroughRegisteredPresenter()

            checkpoint()
            completion()!!.complete(PaywallPresenter.Completion.Result.Closed)
            checkpoint()
            completion()!!.complete(PaywallPresenter.Completion.Result.Closed)

            assertThat(presenter.calls).isEqualTo(1)
            assertThat(results).isEmpty()

            presenter.finish()

            assertThat(results.obtained).containsExactly(emptySet<String>())
            assertThat(presenter.calls).isEqualTo(2)

            presenter.finish()

            assertThat(results.obtained).containsExactly(emptySet<String>(), emptySet<String>())
        }

    // The shape of the sample apps' presenters: a new request overwrites the pending one without completing it.
    private class SingleRequestErrorPresenter : ErrorPresenter {
        var calls = 0
        private var request: ErrorPresenter.Completion? = null

        override fun present(params: ErrorPresenter.Params, completion: ErrorPresenter.Completion) {
            calls++
            request = completion
        }

        fun finish() {
            val pending = request
            request = null
            pending?.complete()
        }
    }

    private fun syncFails() {
        every { mockPurchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, any()) } answers {
            secondArg<ReceiveCustomerInfoCallback>()
                .onError(PurchasesError(PurchasesErrorCode.NetworkError, "Simulated."))
        }
    }

    private fun checkpoint(identifier: String = checkpointId) {
        manager.checkpoint(mockPurchases, identifier, null) { results += it }
    }

    private val List<CheckpointResult>.obtained: List<Set<String>?>
        get() = map { result -> result.flow?.obtainedEntitlements?.map { it.entitlementInfo.identifier }?.toSet() }

    // Registers a presenter for a matched offering and returns an accessor for the completion it was handed.
    private fun presentThroughRegisteredPresenter(offering: Offering = mockk()): () -> PaywallPresenter.Completion? {
        var completion: PaywallPresenter.Completion? = null
        manager.paywallPresenter = PaywallPresenter { _, presentation ->
            completion = presentation
        }
        resolvesTo(CheckpointResolution.MatchedOffering(offering, checkpointRuleId = null))
        return { completion }
    }

    // The paywall listener the workflow window would install, as the SDK's paywall reports through it.
    private fun workflowListener() = manager.paywallOptions(presentedCallIds.last()) {}!!.listener!!

    private fun resolvesTo(resolution: CheckpointResolution) {
        coEvery { mockPurchases.internalResolveCp(any(), any()) } returns resolution
    }

    private fun resolvesToWorkflow() {
        resolvesTo(CheckpointResolution.MatchedWorkflow(
            mockk(),
            mockk(),
            mockk(),
            checkpointRuleId = null,
            traceId = "trace-id",
            workflowBlobRef = null,
        ))
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

    // Mirrors what CheckpointWorkflowPresenter does for a workflow: record the outcome for the presented call, then
    // report the paywall as finished.
    private fun finishPaywall(outcome: CheckpointFlowOutcome?, navigatedBack: Boolean = false) {
        val callId = presentedCallIds.last()
        outcome?.let { manager.recordOutcome(callId, it) }
        manager.onPresentationFinished(callId, navigatedBack)
    }
}
