@file:OptIn(ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.checkpoints.CheckpointResolution
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.TestStoreProduct
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
import java.util.Date

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class CheckpointPassedCallbackTest {

    private val checkpointId = "test_checkpoint"
    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var mockPurchases: Purchases
    private lateinit var customerInfoBefore: CustomerInfo
    private lateinit var cachedCustomerInfo: CustomerInfo
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
            every { appUserID } returns "customer-a"
            every { currentActivity } returns mockActivity
        }
        customerInfoBeforeHasActive()
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

        assertThat(results).containsExactly(CheckpointResult(flow = null, customer = cachedCustomerInfo))
    }

    @Test
    fun `an unknown checkpoint invokes the callback without a flow and no error`() = runTest(dispatcher) {
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.NO_MATCH))
        manager.errorPresenter = errorPresenter

        checkpoint()

        assertThat(results).containsExactly(CheckpointResult(flow = null, customer = cachedCustomerInfo))
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
        resolvesTo(CheckpointResolution.MatchedOffering(offering(), checkpointRuleId = null, traceId = "trace_id"))

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
            resolvesTo(CheckpointResolution.MatchedOffering(offering(), checkpointRuleId = null, traceId = "trace_id"))
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
        customerInfoBeforeHasActive("plus")
        resolvesToWorkflow()

        checkpoint()
        assertThat(results).isEmpty()

        finishPaywall(CheckpointFlowOutcome.Purchased(customerInfoWithActive("plus", "pro"), mockk()))

        assertThat(results.obtained).containsExactly(setOf("pro"))
    }

    @Test
    fun `an offering checkpoint presents the fallback paywall and delivers what the user obtained`() =
        runTest(dispatcher) {
            resolvesTo(CheckpointResolution.MatchedOffering(offering(), checkpointRuleId = null, traceId = "trace_id"))
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
        customerInfoBeforeHasActive("plus")
        resolvesToWorkflow()

        checkpoint()
        workflowListener().onRestoreCompleted(customerInfoWithActive("plus"))
        finishPaywall(outcome = null, navigatedBack = true)

        assertThat(results).isEmpty()
    }

    @Test
    fun `a workflow restore that unlocks an entitlement passes the checkpoint on back`() = runTest(dispatcher) {
        customerInfoBeforeHasActive("plus")
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
    fun `a not-passed gate suppresses the callback despite a recorded purchase`() = runTest(dispatcher) {
        resolvesToWorkflow()
        checkpoint()
        val callId = presentedCallIds.last()
        manager.recordOutcome(callId, CheckpointFlowOutcome.Purchased(customerInfoWithActive("unrelated"), mockk()))

        manager.paywallOptions(callId) {}!!.checkpointGateContext!!
            .onOutcomeChanged(CheckpointGateOutcome.NOT_PASSED)
        finishPaywall(outcome = null, navigatedBack = true)

        assertThat(results).isEmpty()
    }

    @Test
    fun `a not-passed gate suppresses the callback on close despite a granting restore`() = runTest(dispatcher) {
        resolvesToWorkflow()
        checkpoint()
        workflowListener().onRestoreCompleted(customerInfoWithActive("unrelated"))

        manager.paywallOptions(presentedCallIds.last()) {}!!.checkpointGateContext!!
            .onOutcomeChanged(CheckpointGateOutcome.NOT_PASSED)
        finishPaywall(outcome = null)

        assertThat(results).isEmpty()
    }

    @Test
    fun `a failed presentation with a not-passed gate suppresses the callback despite a purchase`() =
        runTest(dispatcher) {
            resolvesToWorkflow()
            checkpoint()
            val callId = presentedCallIds.last()
            manager.recordOutcome(callId, CheckpointFlowOutcome.Purchased(customerInfoWithActive("unrelated"), mockk()))
            manager.paywallOptions(callId) {}!!.checkpointGateContext!!
                .onOutcomeChanged(CheckpointGateOutcome.NOT_PASSED)

            manager.onPresentationFailed(
                callId,
                PurchasesError(PurchasesErrorCode.ConfigurationError, "Re-present failed"),
            )

            assertThat(results).isEmpty()
        }

    @Test
    fun `a passed gate invokes the callback on back without a recorded outcome`() = runTest(dispatcher) {
        resolvesToWorkflow()
        checkpoint()

        manager.paywallOptions(presentedCallIds.last()) {}!!.checkpointGateContext!!
            .onOutcomeChanged(CheckpointGateOutcome.PASSED)
        finishPaywall(outcome = null, navigatedBack = true)

        assertThat(results.obtained).containsExactly(emptySet<String>())
    }

    @Test
    fun `a not-applicable gate invokes the callback with purchase grants on back`() = runTest(dispatcher) {
        resolvesToWorkflow()
        checkpoint()

        manager.paywallOptions(presentedCallIds.last()) {}!!.checkpointGateContext!!
            .onOutcomeChanged(CheckpointGateOutcome.NOT_APPLICABLE)
        finishPaywall(CheckpointFlowOutcome.Purchased(customerInfoWithActive("pro"), mockk()), navigatedBack = true)

        assertThat(results.obtained).containsExactly(setOf("pro"))
    }

    @Test
    fun `an app-owned presentation delivers the grants the SDK finds after syncing`() = runTest(dispatcher) {
        customerInfoBeforeHasActive("plus")
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
    fun `customer information that cannot be read is presented and the callback is not invoked`() =
        runTest(dispatcher) {
            val error = PurchasesError(PurchasesErrorCode.NetworkError, "Simulated.")
            customerInfoReadFailsWith(error)
            resolvesToWorkflow()
            manager.errorPresenter = errorPresenter

            checkpoint()
            errorPresenter.complete()

            assertThat(results).isEmpty()
            assertThat(errorPresenter.errors).containsExactly(error)
            assertThat(errorPresenter.kinds).containsExactly(CheckpointFailureKind.FLOW_COULD_NOT_BE_SHOWN)
            verify(exactly = 0) { mockPresenter.show(any()) }
        }

    @Test
    fun `a purchase delivers the customer information it produced`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val purchased = customerInfoWithActive("pro")

        checkpoint()
        finishPaywall(CheckpointFlowOutcome.Purchased(purchased, mockk()))

        assertThat(results.single().customer).isSameAs(purchased)
        verify(exactly = 0) { mockPurchases.getCustomerInfo(CacheFetchPolicy.CACHE_ONLY, any()) }
    }

    @Test
    fun `a granting restore delivers the customer information it produced`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val restored = customerInfoWithActive("pro")

        checkpoint()
        workflowListener().onRestoreCompleted(restored)
        finishPaywall(outcome = null)

        assertThat(results.single().customer).isSameAs(restored)
    }

    @Test
    fun `an app-owned presentation delivers the synced customer information`() = runTest(dispatcher) {
        val synced = customerInfoWithActive("pro")
        every { mockPurchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, any()) } answers {
            secondArg<ReceiveCustomerInfoCallback>().onReceived(synced)
        }
        val completion = presentThroughRegisteredPresenter()

        checkpoint()
        completion()!!.complete(PaywallPresenter.Completion.Result.Closed)

        assertThat(results.single().customer).isSameAs(synced)
        verify(exactly = 0) { mockPurchases.getCustomerInfo(CacheFetchPolicy.CACHE_ONLY, any()) }
    }

    @Test
    fun `a dismissed paywall delivers the latest cached customer information`() = runTest(dispatcher) {
        resolvesToWorkflow()

        checkpoint()
        finishPaywall(CheckpointFlowOutcome.Dismissed)

        assertThat(results.single().customer).isSameAs(cachedCustomerInfo)
        verify(exactly = 1) { mockPurchases.getCustomerInfo(CacheFetchPolicy.CACHE_ONLY, any()) }
    }

    @Test
    fun `a web checkout delivers the latest cached customer information`() = runTest(dispatcher) {
        resolvesToWorkflow()

        checkpoint()
        workflowListener().onWebCheckoutOpened()
        finishPaywall(outcome = null)

        assertThat(results.single().customer).isSameAs(cachedCustomerInfo)
    }

    @Test
    fun `a sync failure delivers the latest cached customer information`() = runTest(dispatcher) {
        syncFails()
        val completion = presentThroughRegisteredPresenter()

        checkpoint()
        completion()!!.complete(PaywallPresenter.Completion.Result.Closed)

        assertThat(results.single().customer).isSameAs(cachedCustomerInfo)
    }

    @Test
    fun `the customer information read before the flow is delivered when the cache is gone afterwards`() =
        runTest(dispatcher) {
            noCachedCustomerInfo()
            resolvesToWorkflow()

            checkpoint()
            finishPaywall(CheckpointFlowOutcome.Dismissed)

            assertThat(results.single().customer).isSameAs(customerInfoBefore)
        }

    @Test
    fun `an offering checkpoint reports the product the paywall sold`() = runTest(dispatcher) {
        val monthly = product("monthly")
        resolvesTo(
            CheckpointResolution.MatchedOffering(offering(monthly), checkpointRuleId = null, traceId = "trace_id"),
        )
        every { mockPurchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, any()) } answers {
            secondArg<ReceiveCustomerInfoCallback>().onReceived(customerInfoWithActive("pro").purchased("monthly"))
        }

        checkpoint()
        defaultPresenter!!.onPresentationFinished("")

        val result = results.single()
        assertThat(result.flow!!.obtainedProducts).containsExactly(ObtainedProduct(monthly))
        assertThat(result.obtainedProduct("monthly")!!.product).isSameAs(monthly)
        assertThat(result.didObtainProduct("monthly")).isTrue
        assertThat(result.didObtainEntitlement("pro")).isTrue
    }

    @Test
    fun `a presented workflow reports a product from any of its offerings and none from outside them`() =
        runTest(dispatcher) {
            val annual = product("annual")
            val boughtFrom = PresentedOfferingContext("workflow_offering")
            resolvesToWorkflow(annual)

            checkpoint()
            finishPaywall(
                CheckpointFlowOutcome.Purchased(
                    customerInfoWithActive().purchased("annual", "other"),
                    transaction("annual", presentedOfferingContext = boughtFrom),
                ),
            )

            val result = results.single()
            assertThat(result.flow!!.obtainedProducts).containsExactly(ObtainedProduct(annual))
            assertThat(result.obtainedProduct("annual")!!.product.presentedOfferingContext).isEqualTo(boughtFrom)
            assertThat(result.didObtainProduct("other")).isFalse
        }

    @Test
    fun `a restore that only brought back a presented product reports it on close`() = runTest(dispatcher) {
        val monthly = product("monthly")
        resolvesToWorkflow(monthly)
        cachedCustomerInfoHasActive().purchased("monthly")

        checkpoint()
        workflowListener().onRestoreCompleted(customerInfoWithActive().purchased("monthly"))
        finishPaywall(outcome = null)

        val result = results.single()
        assertThat(result.didObtainProduct("monthly")).isTrue
        assertThat(result.flow!!.obtainedEntitlements).isEmpty()
        assertThat(result.customer).isSameAs(cachedCustomerInfo)
    }

    @Test
    fun `a restore that only brought back a presented product is not enough to pass on back`() =
        runTest(dispatcher) {
            val monthly = product("monthly")
            resolvesToWorkflow(monthly)
            cachedCustomerInfoHasActive().purchased("monthly")

            checkpoint()
            workflowListener().onRestoreCompleted(customerInfoWithActive().purchased("monthly"))
            finishPaywall(outcome = null, navigatedBack = true)

            assertThat(results).isEmpty()
        }

    @Test
    fun `a result without a flow obtains no entitlement or product`() = runTest(dispatcher) {
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.NO_MATCH))

        checkpoint()

        val result = results.single()
        assertThat(result.flow).isNull()
        assertThat(result.obtainedEntitlement("pro")).isNull()
        assertThat(result.didObtainEntitlement("pro")).isFalse
        assertThat(result.obtainedProduct("monthly")).isNull()
        assertThat(result.didObtainProduct("monthly")).isFalse
    }

    @Test
    fun `a no-action checkpoint delivers the customer information read before when the cache is gone`() =
        runTest(dispatcher) {
            noCachedCustomerInfo()
            resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.NO_MATCH))

            checkpoint()

            assertThat(results).containsExactly(CheckpointResult(flow = null, customer = customerInfoBefore))
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
    private fun presentThroughRegisteredPresenter(offering: Offering = offering()): () -> PaywallPresenter.Completion? {
        var completion: PaywallPresenter.Completion? = null
        manager.paywallPresenter = PaywallPresenter { _, presentation ->
            completion = presentation
        }
        resolvesTo(CheckpointResolution.MatchedOffering(offering, checkpointRuleId = null, traceId = "trace_id"))
        return { completion }
    }

    // The paywall listener the workflow window would install, as the SDK's paywall reports through it.
    private fun workflowListener() = manager.paywallOptions(presentedCallIds.last()) {}!!.listener!!

    private fun resolvesTo(resolution: CheckpointResolution) {
        coEvery { mockPurchases.internalResolveCp(any(), any()) } returns resolution
    }

    private fun resolvesToWorkflow(vararg products: StoreProduct) {
        resolvesTo(CheckpointResolution.MatchedWorkflow(
            mockk(),
            mockk(),
            offerings(*products),
            checkpointRuleId = null,
            traceId = "trace-id",
            workflowBlobRef = null,
        ))
    }

    private fun customerInfoBeforeHasActive(vararg identifiers: String) {
        customerInfoBefore = customerInfoWithActive(*identifiers)
        every { mockPurchases.getCustomerInfo(CacheFetchPolicy.CACHED_OR_FETCHED, any()) } answers {
            secondArg<ReceiveCustomerInfoCallback>().onReceived(customerInfoBefore)
        }
    }

    private fun customerInfoReadFailsWith(error: PurchasesError) {
        every { mockPurchases.getCustomerInfo(CacheFetchPolicy.CACHED_OR_FETCHED, any()) } answers {
            secondArg<ReceiveCustomerInfoCallback>().onError(error)
        }
    }

    private fun cachedCustomerInfoHasActive(vararg identifiers: String): CustomerInfo {
        cachedCustomerInfo = customerInfoWithActive(*identifiers)
        every { mockPurchases.getCustomerInfo(CacheFetchPolicy.CACHE_ONLY, any()) } answers {
            secondArg<ReceiveCustomerInfoCallback>().onReceived(cachedCustomerInfo)
        }
        return cachedCustomerInfo
    }

    private fun noCachedCustomerInfo() {
        every { mockPurchases.getCustomerInfo(CacheFetchPolicy.CACHE_ONLY, any()) } answers {
            secondArg<ReceiveCustomerInfoCallback>()
                .onError(PurchasesError(PurchasesErrorCode.CustomerInfoError, "No cache."))
        }
    }

    private fun customerInfoWithActive(vararg identifiers: String): CustomerInfo {
        val active = identifiers.associateWith { id -> mockk<EntitlementInfo> { every { identifier } returns id } }
        return mockk {
            every { entitlements.active } returns active
            every { getPurchaseDateForProductId(any()) } returns null
        }
    }

    private fun product(identifier: String): StoreProduct =
        TestStoreProduct(identifier, "name", "title", "description", Price("$1.99", 1_990_000, "USD"))

    private fun transaction(vararg productIds: String, presentedOfferingContext: PresentedOfferingContext?) =
        mockk<StoreTransaction> {
            every { this@mockk.productIds } returns productIds.toList()
            every { this@mockk.presentedOfferingContext } returns presentedOfferingContext
        }

    private fun offering(vararg products: StoreProduct): Offering = mockk {
        every { availablePackages } returns products.map { p -> mockk<Package> { every { product } returns p } }
    }

    private fun offerings(vararg products: StoreProduct): Offerings = mockk {
        every { all } returns mapOf("default" to offering(*products))
    }

    private fun CustomerInfo.purchased(vararg identifiers: String, date: Date = Date(1_000)): CustomerInfo = apply {
        identifiers.forEach { identifier -> every { getPurchaseDateForProductId(identifier) } returns date }
    }

    // Mirrors what CheckpointWorkflowPresenter does for a workflow: record the outcome for the presented call, then
    // report the paywall as finished.
    private fun finishPaywall(outcome: CheckpointFlowOutcome?, navigatedBack: Boolean = false) {
        val callId = presentedCallIds.last()
        outcome?.let { manager.recordOutcome(callId, it) }
        manager.onPresentationFinished(callId, navigatedBack)
    }
}
