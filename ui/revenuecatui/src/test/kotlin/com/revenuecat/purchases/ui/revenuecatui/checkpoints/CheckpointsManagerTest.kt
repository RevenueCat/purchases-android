@file:OptIn(ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.checkpoints.CheckpointResolution
import com.revenuecat.purchases.common.localrules.RulesDimensionValue
import com.revenuecat.purchases.common.workflows.WorkflowStep
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.PaywallDismissReason
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import com.revenuecat.purchases.ui.revenuecatui.workflow.CheckpointWorkflowGateController
import com.revenuecat.purchases.ui.revenuecatui.workflow.WorkflowGateDecision
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class CheckpointsManagerTest {

    private val checkpointId = "test_checkpoint"
    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var mockPurchases: Purchases
    private lateinit var customerInfoBefore: CustomerInfo
    private lateinit var mockActivity: Activity
    private lateinit var mockPresenter: CheckpointWorkflowPresenter
    private var defaultPresenter: DefaultPaywallPresenter? = null
    private val defaultErrorPresenters = mutableListOf<DefaultErrorPresenter>()
    private val defaultPresentedErrors = mutableListOf<ErrorPresenter.Params>()
    private val presentedCallIds = mutableListOf<String>()
    private val presentedModes = mutableListOf<FlowPresentationMode>()
    private val defaultPresenterModes = mutableListOf<FlowPresentationMode>()
    private val results = mutableListOf<CheckpointResult>()

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
        defaultPresenter = null
        defaultErrorPresenters.clear()
        defaultPresentedErrors.clear()
        customerInfoBefore = customerInfoWithActive()
        mockPurchases = mockk {
            every { appUserID } returns "customer-a"
            every { currentActivity } returns mockActivity
            every { getCustomerInfo(CacheFetchPolicy.CACHED_OR_FETCHED, any()) } answers {
                secondArg<ReceiveCustomerInfoCallback>().onReceived(customerInfoBefore)
            }
            every { getCustomerInfo(CacheFetchPolicy.CACHE_ONLY, any()) } answers {
                secondArg<ReceiveCustomerInfoCallback>()
                    .onError(PurchasesError(PurchasesErrorCode.CustomerInfoError, "No cache."))
            }
        }
        manager = CheckpointsManager(
            presenterFactory = { callId, _, mode ->
                presentedCallIds += callId
                presentedModes += mode
                mockPresenter
            },
            defaultPresenterFactory = { purchases, errorPresenter ->
                DefaultPaywallPresenter(purchases, errorPresenter) { _, _, mode ->
                    defaultPresenterModes += mode
                    mockPresenter
                }.also { defaultPresenter = it }
            },
            defaultErrorPresenterFactory = { _ ->
                // The SDK's own dialog needs a real activity; here it acknowledges every error right away.
                mockk<DefaultErrorPresenter> {
                    every { present(any(), any()) } answers {
                        defaultPresentedErrors += firstArg<ErrorPresenter.Params>()
                        secondArg<ErrorPresenter.Completion>().complete()
                    }
                }.also { defaultErrorPresenters += it }
            },
        )
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
    fun `invalid checkpoint identifier is presented, logged, and skips resolution`() = runTest(dispatcher) {
        val invalidIdentifier = " checkout😀"
        val message = CheckpointIdentifierValidator.invalidIdentifierLogMessage(invalidIdentifier)

        checkpoint(identifier = invalidIdentifier)

        assertThat(results).isEmpty()
        assertThat(defaultPresentedErrors).containsExactly(
            ErrorPresenter.Params(
                PurchasesError(PurchasesErrorCode.ConfigurationError, message),
                invalidIdentifier,
                emptyMap(),
                CheckpointFailureKind.FLOW_COULD_NOT_BE_SHOWN,
            ),
        )
        coVerify(exactly = 0) { mockPurchases.internalResolveCp(any(), any()) }
        verify(exactly = 1) { Logger.e(message) }
    }

    @Test
    fun `a failure waits for the app's error UI before the run ends`() = runTest(dispatcher) {
        var completion: ErrorPresenter.Completion? = null
        manager.errorPresenter = ErrorPresenter { _, c -> completion = c }
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE))
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }

        assertThat(run).isNull()
        completion!!.complete()
        call.join()

        assertThat(run!!.failed).isTrue
        assertThat(run!!.flowOutcome).isNull()
    }

    @Test
    fun `unavailable configuration is logged, presented and does not pass`() = runTest(dispatcher) {
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE))
        val message = "The configuration needed to serve checkpoint '$checkpointId' could not be read."

        checkpoint()

        assertThat(results).isEmpty()
        assertThat(defaultPresentedErrors.single().error).isEqualTo(
            PurchasesError(PurchasesErrorCode.ConfigurationError, message),
        )
        verify(exactly = 1) { Logger.e(message) }
    }

    @Test
    fun `a failure whose error presenter throws still ends the run`() = runTest(dispatcher) {
        manager.errorPresenter = ErrorPresenter { _, _ -> error("Simulated.") }
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE))

        checkpoint()

        assertThat(results).isEmpty()
        verify(exactly = 1) { Logger.e(match { it.startsWith("Error presenter failed") }) }
    }

    @Test
    fun `unmatched checkpoint passes a result without a flow`() =
        runTest(dispatcher) {
            resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.UNKNOWN_CHECKPOINT))

            checkpoint()

            assertThat(results).containsExactly(CheckpointResult(flow = null, customer = customerInfoBefore))
        }

    @Test
    fun `the customer's information is read once resolved and before anything is presented`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val call = launch { runCheckpoint() }

        coVerifyOrder {
            mockPurchases.internalResolveCp(any(), any())
            mockPurchases.getCustomerInfo(CacheFetchPolicy.CACHED_OR_FETCHED, any())
            mockPresenter.show(any())
        }
        verify(exactly = 1) { mockPurchases.getCustomerInfo(CacheFetchPolicy.CACHED_OR_FETCHED, any()) }
        verify(exactly = 0) { mockPurchases.getCustomerInfo(CacheFetchPolicy.CACHE_ONLY, any()) }
        finishPaywall(CheckpointFlowOutcome.Dismissed)
        call.join()
    }

    @Test
    fun `customer information that cannot be read is logged, presented and does not pass`() = runTest(dispatcher) {
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "Simulated.")
        customerInfoReadFailsWith(error)
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.NO_MATCH))

        val run = runCheckpoint()

        assertThat(run.failed).isTrue
        assertThat(run.customerInfoBefore).isNull()
        assertThat(defaultPresentedErrors).containsExactly(
            ErrorPresenter.Params(error, checkpointId, emptyMap(), CheckpointFailureKind.FLOW_COULD_NOT_BE_SHOWN),
        )
        coVerify(exactly = 1) { mockPurchases.internalResolveCp(any(), any()) }
        verify(exactly = 1) { Logger.e(match { it.contains(error.toString()) }) }
    }

    @Test
    fun `a run carries the offerings the resolved checkpoint can present`() = runTest(dispatcher) {
        val monthly = product("monthly")
        val annual = product("annual")
        syncedCustomerInfoIs(customerInfoWithActive())
        resolvesTo(
            CheckpointResolution.MatchedOffering(offering(monthly), checkpointRuleId = null, traceId = "trace_id"),
        )
        var run: CheckpointRun? = null
        var call = launch { run = runCheckpoint() }
        finishDefaultPaywall()
        call.join()

        assertThat(run!!.presentedProducts).containsExactly(monthly)

        resolvesToWorkflow(annual)
        call = launch { run = runCheckpoint() }
        finishPaywall(CheckpointFlowOutcome.Dismissed)
        call.join()

        assertThat(run!!.presentedProducts).containsExactly(annual)

        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.NO_MATCH))

        assertThat(runCheckpoint().presentedOfferings).isEmpty()
    }

    @Test
    fun `a run carries the customer's information read before it`() = runTest(dispatcher) {
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.NO_MATCH))

        assertThat(runCheckpoint().customerInfoBefore).isSameAs(customerInfoBefore)

        resolvesToWorkflow()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }
        finishPaywall(CheckpointFlowOutcome.Dismissed)
        call.join()

        assertThat(run!!.customerInfoBefore).isSameAs(customerInfoBefore)
    }

    @Test
    fun `offering checkpoint presents the fallback paywall through the SDK's own presenter`() = runTest(dispatcher) {
        val offering = offering()
        val customerInfo = mockk<CustomerInfo>()
        syncedCustomerInfoIs(customerInfo)
        resolvesTo(CheckpointResolution.MatchedOffering(offering, checkpointRuleId = null, traceId = "trace_id"))

        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }

        assertThat(run).isNull()
        assertThat(presentedCallIds).isEmpty()
        verify(exactly = 1) { mockPresenter.show(mockActivity) }
        assertThat(defaultPresenter!!.paywallOptions("") {}!!.offeringSelection.offering).isEqualTo(offering)

        finishDefaultPaywall()
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Finished(customerInfo))
        assertThat(run!!.backedOut).isFalse
    }

    @Test
    fun `backing out of the SDK's own paywall is backed out without syncing`() = runTest(dispatcher) {
        resolvesTo(CheckpointResolution.MatchedOffering(offering(), checkpointRuleId = null, traceId = "trace_id"))
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }

        finishDefaultPaywall(navigatedBack = true)
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Dismissed)
        assertThat(run!!.backedOut).isTrue
        verify(exactly = 0) { mockPurchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, any()) }
    }

    @Test
    fun `a granting restore in the SDK's own paywall followed by back is not backed out`() = runTest(dispatcher) {
        val customerInfo = mockk<CustomerInfo>()
        syncedCustomerInfoIs(customerInfo)
        resolvesTo(CheckpointResolution.MatchedOffering(offering(), checkpointRuleId = null, traceId = "trace_id"))
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }
        val restored = mockk<CustomerInfo> { every { entitlements.active } returns mapOf("pro" to mockk()) }

        defaultPresenter!!.paywallOptions("") {}!!.listener!!.onRestoreCompleted(restored)
        finishDefaultPaywall(navigatedBack = true)
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Finished(customerInfo))
        assertThat(run!!.backedOut).isFalse
        verify(exactly = 1) { mockPurchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, any()) }
    }

    @Test
    fun `the workflow window's options carry the workflow and report its outcomes`() = runTest(dispatcher) {
        val resolution = CheckpointResolution.MatchedWorkflow(
            mockk(),
            mockk(),
            offerings(),
            checkpointRuleId = null,
            traceId = "trace-id",
            workflowBlobRef = "blob-ref",
        )
        resolvesTo(resolution)
        val customerInfo = mockk<CustomerInfo>()
        val storeTransaction = mockk<StoreTransaction>()
        val error = PurchasesError(PurchasesErrorCode.ConfigurationError, "Step misconfigured")
        val dismissals = mutableListOf<Boolean>()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }

        val options = manager.paywallOptions(currentCallId()) { dismissals += it }!!
        assertThat(options.injectedWorkflow).isSameAs(resolution.workflow)
        assertThat(options.injectedWorkflowTraceId).isEqualTo(resolution.traceId)
        assertThat(options.injectedWorkflowBlobRef).isEqualTo(resolution.workflowBlobRef)
        assertThat(options.injectedWorkflowCheckpointIdentifier).isEqualTo(checkpointId)
        options.listener!!.onPurchaseCompleted(customerInfo, storeTransaction)
        options.dismissRequestWithExitOffering!!(null, PaywallResult.Error(error), PaywallDismissReason.NAVIGATED_BACK)
        assertThat(dismissals).containsExactly(true)
        assertThat(manager.paywallOptions("unknown-call-id") {}).isNull()

        finishPaywall(outcome = null, navigatedBack = true)
        call.join()

        // The purchase wins over the later error, and a purchase is never backed out.
        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Purchased(customerInfo, storeTransaction))
        assertThat(run!!.backedOut).isFalse
    }

    @Test
    fun `workflow gate context carries the customer's active entitlements through copied options`() = runTest(dispatcher) {
        customerInfoBeforeHasActive("plus", "pro")
        resolvesToWorkflow()
        val call = launch { runCheckpoint() }

        val options = manager.paywallOptions(currentCallId()) {}!!

        assertThat(options.checkpointGateContext!!.initialActiveEntitlementIds).containsExactlyInAnyOrder("plus", "pro")
        assertThat(options.copy().checkpointGateContext).isSameAs(options.checkpointGateContext)
        assertThat(manager.paywallOptions("unknown-call-id") {}).isNull()
        call.cancel()
        call.join()
    }

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `workflow snapshot keeps initiating identity when customer changes during the customer read`() = runTest(dispatcher) {
        every { mockPurchases.appUserID } returns "customer-a"
        val cacheResult = slot<ReceiveCustomerInfoCallback>()
        every { mockPurchases.getCustomerInfo(CacheFetchPolicy.CACHED_OR_FETCHED, capture(cacheResult)) } just runs
        resolvesToWorkflow()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }
        every { mockPurchases.appUserID } returns "customer-b"
        val customerInfo = customerInfoWithActive("pro")
        cacheResult.captured.onReceived(customerInfo)
        val controller = CheckpointWorkflowGateController { mockPurchases.appUserID }
        controller.reset(manager.paywallOptions(currentCallId()) {}!!.checkpointGateContext)
        val step = WorkflowStep(
            id = "hard", type = "screen", screenId = "screen",
            paramValues = mapOf("gating" to Json.parseToJsonElement(
                """{"type":"hard","entitlement_identifiers":["pro"]}""",
            ).jsonObject),
        )

        assertThat(controller.evaluate(step)).isEqualTo(WorkflowGateDecision.BLOCKED)
        finishPaywall(outcome = CheckpointFlowOutcome.Restored(customerInfo), navigatedBack = true)
        call.join()

        assertThat(run!!.backedOut).isTrue()
    }

    @Test
    fun `an abandoned gate callback cannot back out the replacement call`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val firstCall = launch { runCheckpoint() }
        val abandonedContext = manager.paywallOptions(currentCallId()) {}!!.checkpointGateContext!!
        firstCall.cancel()
        firstCall.join()
        var run: CheckpointRun? = null
        val secondCall = launch { run = runCheckpoint() }

        abandonedContext.onOutcomeChanged(CheckpointGateOutcome.NOT_PASSED)
        finishPaywall(outcome = null)
        secondCall.join()

        assertThat(run!!.backedOut).isFalse
    }

    @Test
    fun `an abandoned gate callback cannot pass the replacement call`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val firstCall = launch { runCheckpoint() }
        val abandonedContext = manager.paywallOptions(currentCallId()) {}!!.checkpointGateContext!!
        firstCall.cancel()
        firstCall.join()
        var run: CheckpointRun? = null
        val secondCall = launch { run = runCheckpoint() }
        manager.paywallOptions(currentCallId()) {}!!.checkpointGateContext!!
            .onOutcomeChanged(CheckpointGateOutcome.NOT_PASSED)

        abandonedContext.onOutcomeChanged(CheckpointGateOutcome.PASSED)
        finishPaywall(outcome = null)
        secondCall.join()

        assertThat(run!!.backedOut).isTrue
    }

    @Test
    fun `a not-passed gate backs out despite a recorded purchase`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val purchased = CheckpointFlowOutcome.Purchased(mockk(), mockk())
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }
        manager.recordOutcome(currentCallId(), purchased)

        manager.paywallOptions(currentCallId()) {}!!.checkpointGateContext!!
            .onOutcomeChanged(CheckpointGateOutcome.NOT_PASSED)
        finishPaywall(outcome = null, navigatedBack = true)
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(purchased)
        assertThat(run!!.backedOut).isTrue
    }

    @Test
    fun `a not-passed gate backs out on close despite a recorded restore`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val restored = CheckpointFlowOutcome.Restored(mockk())
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }
        manager.recordOutcome(currentCallId(), restored)

        manager.paywallOptions(currentCallId()) {}!!.checkpointGateContext!!
            .onOutcomeChanged(CheckpointGateOutcome.NOT_PASSED)
        finishPaywall(outcome = null)
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(restored)
        assertThat(run!!.backedOut).isTrue
    }

    @Test
    fun `a failed presentation with a not-passed gate backs out despite a recorded purchase`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val purchased = CheckpointFlowOutcome.Purchased(mockk(), mockk())
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }
        val callId = currentCallId()
        manager.recordOutcome(callId, purchased)
        manager.paywallOptions(callId) {}!!.checkpointGateContext!!
            .onOutcomeChanged(CheckpointGateOutcome.NOT_PASSED)

        manager.onPresentationFailed(callId, PurchasesError(PurchasesErrorCode.ConfigurationError, "Re-present failed"))
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(purchased)
        assertThat(run!!.backedOut).isTrue
    }

    @Test
    fun `a passed gate is not backed out without a recorded purchase or restore`() = runTest(dispatcher) {
        resolvesToWorkflow()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }
        val context = manager.paywallOptions(currentCallId()) {}!!.checkpointGateContext!!
        context.onOutcomeChanged(CheckpointGateOutcome.NOT_PASSED)

        context.onOutcomeChanged(CheckpointGateOutcome.PASSED)
        finishPaywall(outcome = null, navigatedBack = true)
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Dismissed)
        assertThat(run!!.backedOut).isFalse
    }

    @Test
    fun `a not-applicable gate preserves purchase passage on back`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val purchased = CheckpointFlowOutcome.Purchased(mockk(), mockk())
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }
        val context = manager.paywallOptions(currentCallId()) {}!!.checkpointGateContext!!
        context.onOutcomeChanged(CheckpointGateOutcome.NOT_PASSED)

        context.onOutcomeChanged(CheckpointGateOutcome.NOT_APPLICABLE)
        finishPaywall(purchased, navigatedBack = true)
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(purchased)
        assertThat(run!!.backedOut).isFalse
    }

    @Test
    fun `a workflow restore that grants nothing new is not an outcome`() = runTest(dispatcher) {
        customerInfoBeforeHasActive("plus")
        resolvesToWorkflow()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }

        manager.paywallOptions(currentCallId()) {}!!.listener!!.onRestoreCompleted(customerInfoWithActive("plus"))
        finishPaywall(outcome = null, navigatedBack = true)
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Dismissed)
        assertThat(run!!.backedOut).isTrue
    }

    @Test
    fun `a workflow restore that grants a new entitlement is recorded`() = runTest(dispatcher) {
        customerInfoBeforeHasActive("plus")
        resolvesToWorkflow()
        val restored = customerInfoWithActive("plus", "pro")
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }

        manager.paywallOptions(currentCallId()) {}!!.listener!!.onRestoreCompleted(restored)
        finishPaywall(outcome = null, navigatedBack = true)
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Restored(restored))
        assertThat(run!!.backedOut).isFalse
    }

    @Test
    fun `the SDK's own paywall without a started activity fails and presents nothing`() = runTest(dispatcher) {
        every { mockPurchases.currentActivity } returns null
        resolvesTo(CheckpointResolution.MatchedOffering(offering(), checkpointRuleId = null, traceId = "trace_id"))

        assertThat(runCheckpoint().failed).isTrue
        assertThat(defaultPresentedErrors.single().kind).isEqualTo(CheckpointFailureKind.FLOW_COULD_NOT_BE_SHOWN)

        verify(exactly = 0) { mockPresenter.show(any()) }
        verify {
            Logger.e(
                match {
                    it.contains("Paywall presenter failed") &&
                        it.contains("Cannot present checkpoint paywall: no started Activity found.")
                },
            )
        }
    }

    @Test
    fun `cancelling the caller leaves the SDK's own paywall to the user and ignores its report`() =
        runTest(dispatcher) {
            resolvesTo(CheckpointResolution.MatchedOffering(offering(), checkpointRuleId = null, traceId = "trace_id"))
            val call = launch { runCheckpoint() }

            call.cancel()
            call.join()
            finishDefaultPaywall()

            verify(exactly = 0) { mockPresenter.abandon() }
            verify(exactly = 0) { mockPurchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, any()) }
        }

    @Test
    fun `offering checkpoint cannot present while a UI checkpoint is being presented`() = runTest(dispatcher) {
        coEvery { mockPurchases.internalResolveCp(any(), any()) } returnsMany listOf(
            CheckpointResolution.MatchedWorkflow(
                mockk(),
                mockk(),
                offerings(),
                checkpointRuleId = null,
                traceId = "trace-id",
                workflowBlobRef = null,
            ),
            CheckpointResolution.MatchedOffering(offering(), checkpointRuleId = null, traceId = "trace_id"),
        )
        val presentedCall = launch { runCheckpoint() }

        val run = runCheckpoint()

        assertThat(run.blockedByPresentedFlow).isTrue
        assertThat(run.flowOutcome).isNull()
        assertThat(presentedCallIds).hasSize(1)
        presentedCall.cancel()
    }

    @Test
    fun `resolution failure is presented and does not pass`() = runTest(dispatcher) {
        val error = PurchasesError(PurchasesErrorCode.ConfigurationError, "Simulated.")
        coEvery { mockPurchases.internalResolveCp(any(), any()) } throws PurchasesException(error)

        checkpoint()

        assertThat(results).isEmpty()
        assertThat(defaultPresentedErrors).containsExactly(
            ErrorPresenter.Params(error, checkpointId, emptyMap(), CheckpointFailureKind.FLOW_COULD_NOT_BE_SHOWN),
        )
    }

    @Test
    fun `a presentation failure is presented and does not pass without throwing`() = runTest(dispatcher) {
        every { mockPurchases.currentActivity } returns null
        resolvesToWorkflow()

        checkpoint()

        assertThat(results).isEmpty()
        assertThat(defaultPresentedErrors.single().kind).isEqualTo(CheckpointFailureKind.FLOW_COULD_NOT_BE_SHOWN)
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

        assertThat(results)
            .containsExactly(CheckpointResult(FlowResult(emptySet(), emptySet()), customerInfoBefore))
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
    fun `a workflow is presented as a sheet unless the params say otherwise`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val call = launch { runCheckpoint() }

        assertThat(presentedModes).containsExactly(FlowPresentationMode.MODAL_SHEET)

        finishPaywall(CheckpointFlowOutcome.Dismissed)
        call.join()
    }

    @Test
    fun `the default presentation mode is the sheet`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val call = launch {
            runCheckpoint(CheckpointParams { presentationMode(FlowPresentationMode.DEFAULT) })
        }

        assertThat(presentedModes).containsExactly(FlowPresentationMode.MODAL_SHEET)

        finishPaywall(CheckpointFlowOutcome.Dismissed)
        call.join()
    }

    @Test
    fun `the presentation mode reaches the workflow window`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val call = launch {
            runCheckpoint(CheckpointParams { presentationMode(FlowPresentationMode.MODAL_FULL_SCREEN) })
        }

        assertThat(presentedModes).containsExactly(FlowPresentationMode.MODAL_FULL_SCREEN)

        finishPaywall(CheckpointFlowOutcome.Dismissed)
        call.join()
    }

    @Test
    fun `the presentation mode reaches the SDK's own offering presenter`() = runTest(dispatcher) {
        resolvesTo(CheckpointResolution.MatchedOffering(offering(), checkpointRuleId = null, traceId = "trace_id"))
        syncedCustomerInfoIs(mockk())
        val call = launch {
            runCheckpoint(CheckpointParams { presentationMode(FlowPresentationMode.MODAL_FULL_SCREEN) })
        }

        assertThat(defaultPresenterModes).containsExactly(FlowPresentationMode.MODAL_FULL_SCREEN)

        finishDefaultPaywall()
        call.join()
    }

    @Test
    fun `an app presenter receives the resolved presentation mode`() = runTest(dispatcher) {
        syncedCustomerInfoIs(mockk())
        resolvesTo(CheckpointResolution.MatchedOffering(offering(), checkpointRuleId = null, traceId = "trace_id"))
        val receivedModes = mutableListOf<FlowPresentationMode>()
        manager.paywallPresenter = PaywallPresenter { params, completion ->
            receivedModes += params.presentationMode
            completion.complete(PaywallPresenter.Completion.Result.Closed)
        }

        runCheckpoint()
        runCheckpoint(CheckpointParams { presentationMode(FlowPresentationMode.DEFAULT) })
        runCheckpoint(CheckpointParams { presentationMode(FlowPresentationMode.MODAL_FULL_SCREEN) })
        runCheckpoint(CheckpointParams { presentationMode(FlowPresentationMode.MODAL_SHEET) })

        assertThat(receivedModes).containsExactly(
            FlowPresentationMode.MODAL_SHEET,
            FlowPresentationMode.MODAL_SHEET,
            FlowPresentationMode.MODAL_FULL_SCREEN,
            FlowPresentationMode.MODAL_SHEET,
        )
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
    fun `a later error does not replace a recorded restore`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val customerInfo = mockk<CustomerInfo>()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }
        manager.recordOutcome(currentCallId(), CheckpointFlowOutcome.Restored(customerInfo))

        finishPaywall(CheckpointFlowOutcome.Error(mockk()))
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Restored(customerInfo))
    }

    @Test
    fun `a later web checkout does not replace a recorded purchase`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val customerInfo = mockk<CustomerInfo>()
        val storeTransaction = mockk<StoreTransaction>()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }
        manager.recordOutcome(currentCallId(), CheckpointFlowOutcome.Purchased(customerInfo, storeTransaction))

        finishPaywall(CheckpointFlowOutcome.WebCheckoutOpened)
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Purchased(customerInfo, storeTransaction))
    }

    @Test
    fun `a later purchase replaces a recorded restore`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val customerInfo = mockk<CustomerInfo>()
        val storeTransaction = mockk<StoreTransaction>()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }
        manager.recordOutcome(currentCallId(), CheckpointFlowOutcome.Restored(mockk()))

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
        assertThat(run.customerInfoBefore).isNull()
        firstCall.cancel()
    }

    @Test
    fun `a checkpoint blocked by the presented flow never invokes its callback`() = runTest(dispatcher) {
        resolvesToWorkflow()
        checkpoint()

        checkpoint()

        assertThat(results).isEmpty()
        finishPaywall(CheckpointFlowOutcome.Dismissed)
        assertThat(results)
            .containsExactly(CheckpointResult(FlowResult(emptySet(), emptySet()), customerInfoBefore))
        assertThat(presentedCallIds).hasSize(1)
    }

    @Test
    fun `an unmatched checkpoint still passes without a flow while another flow is presented`() = runTest(dispatcher) {
        resolvesToWorkflow()
        checkpoint()
        resolvesTo(CheckpointResolution.NoAction(CheckpointResolution.NoAction.Reason.NO_MATCH))

        checkpoint()

        assertThat(results).containsExactly(CheckpointResult(flow = null, customer = customerInfoBefore))
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
    fun `a flow that ended in an error does not pass`() = runTest(dispatcher) {
        resolvesToWorkflow()

        checkpoint()
        finishPaywall(CheckpointFlowOutcome.Error(PurchasesError(PurchasesErrorCode.ConfigurationError, "boom")))

        assertThat(results).isEmpty()
    }

    @Test
    fun `a failed re-present is presented through the call's error presenter and ends with the error`() =
        runTest(dispatcher) {
            resolvesToWorkflow()
            val presented = mutableListOf<ErrorPresenter.Params>()
            var completion: ErrorPresenter.Completion? = null
            manager.errorPresenter = ErrorPresenter { params, c ->
                presented += params
                completion = c
            }
            val error = PurchasesError(PurchasesErrorCode.ConfigurationError, "Re-present failed.")
            var run: CheckpointRun? = null
            val call = launch { run = runCheckpoint() }

            manager.onPresentationFailed(currentCallId(), error)

            assertThat(presented).containsExactly(
                ErrorPresenter.Params(error, checkpointId, emptyMap(), CheckpointFailureKind.FLOW_ACTION_FAILED),
            )
            assertThat(run).isNull()
            completion!!.complete()
            call.join()
            assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Error(error))
        }

    @Test
    fun `a failed re-present after a purchase keeps the purchase and presents nothing`() = runTest(dispatcher) {
        resolvesToWorkflow()
        var presented = false
        manager.errorPresenter = ErrorPresenter { _, _ -> presented = true }
        val customerInfo = mockk<CustomerInfo>()
        val storeTransaction = mockk<StoreTransaction>()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }
        manager.recordOutcome(currentCallId(), CheckpointFlowOutcome.Purchased(customerInfo, storeTransaction))

        manager.onPresentationFailed(currentCallId(), PurchasesError(PurchasesErrorCode.ConfigurationError, "boom"))
        call.join()

        assertThat(presented).isFalse
        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Purchased(customerInfo, storeTransaction))
    }

    @Test
    fun `a purchase error in the workflow is not an outcome`() = runTest(dispatcher) {
        resolvesToWorkflow()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }

        val listener = manager.paywallOptions(currentCallId()) {}!!.listener!!
        listener.onPurchaseError(PurchasesError(PurchasesErrorCode.StoreProblemError, "boom"))
        listener.onRestoreError(PurchasesError(PurchasesErrorCode.NetworkError, "boom"))
        finishPaywall(outcome = null)
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Dismissed)
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

    @Test
    fun `a registered presenter presents a matched offering and its continued report resolves the run`() =
        runTest(dispatcher) {
            val offering = offering()
            val customerInfo = mockk<CustomerInfo>()
            syncedCustomerInfoIs(customerInfo)
            var presented: Offering? = null
            var presentedIdentifier: String? = null
            var completion: PaywallPresenter.Completion? = null
            manager.paywallPresenter = PaywallPresenter { params, presentation ->
                presented = params.offering
                presentedIdentifier = params.checkpointIdentifier
                completion = presentation
            }
            resolvesTo(CheckpointResolution.MatchedOffering(offering, checkpointRuleId = null, traceId = "trace_id"))

            var run: CheckpointRun? = null
            val call = launch { run = runCheckpoint() }

            assertThat(presented).isEqualTo(offering)
            assertThat(presentedIdentifier).isEqualTo(checkpointId)
            assertThat(run).isNull()
            assertThat(presentedCallIds).isEmpty()
            assertThat(defaultPresenter).isNull()
            verify(exactly = 0) { mockPurchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, any()) }

            completion!!.complete(PaywallPresenter.Completion.Result.Continued)
            call.join()

            assertThat(run!!.flowOutcome)
                .isEqualTo(CheckpointFlowOutcome.Finished(customerInfo))
            assertThat(run!!.backedOut).isFalse
        }

    @Test
    fun `a closed report resolves as finished`() = runTest(dispatcher) {
        val customerInfo = mockk<CustomerInfo>()
        syncedCustomerInfoIs(customerInfo)
        val completion = presentThroughRegisteredPresenter()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }

        completion()!!.complete(PaywallPresenter.Completion.Result.Closed)
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Finished(customerInfo))
        assertThat(run!!.backedOut).isFalse
    }

    @Test
    fun `a navigated-back report resolves as dismissed and backed out without syncing`() = runTest(dispatcher) {
        val completion = presentThroughRegisteredPresenter()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }

        completion()!!.complete(PaywallPresenter.Completion.Result.NavigatedBack)
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Dismissed)
        assertThat(run!!.backedOut).isTrue
        verify(exactly = 0) { mockPurchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, any()) }
    }

    @Test
    fun `a failed report resolves with the error without syncing`() = runTest(dispatcher) {
        val error = PurchasesError(PurchasesErrorCode.ConfigurationError, "No packages available")
        val completion = presentThroughRegisteredPresenter()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }

        completion()!!.complete(PaywallPresenter.Completion.Result.Failed(error))
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Error(error))
        assertThat(run!!.backedOut).isFalse
        verify(exactly = 0) { mockPurchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, any()) }
    }

    @Test
    fun `a continued report resolves like a closed one`() = runTest(dispatcher) {
        val customerInfo = mockk<CustomerInfo>()
        syncedCustomerInfoIs(customerInfo)
        val completion = presentThroughRegisteredPresenter()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }

        completion()!!.complete(PaywallPresenter.Completion.Result.Continued)
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Finished(customerInfo))
        assertThat(run!!.backedOut).isFalse
    }

    @Test
    fun `an unknown result is treated as closed and logged`() = runTest(dispatcher) {
        val customerInfo = mockk<CustomerInfo>()
        syncedCustomerInfoIs(customerInfo)
        val completion = presentThroughRegisteredPresenter()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }

        completion()!!.complete(object : PaywallPresenter.Completion.Result() {})
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Finished(customerInfo))
        verify(exactly = 1) { Logger.e(match { it.startsWith("Unknown paywall presenter result") }) }
    }

    @Test
    fun `a sync failure after a closed report is presented and resolves as unsynced once completed`() =
        runTest(dispatcher) {
            val error = PurchasesError(PurchasesErrorCode.NetworkError, "Simulated.")
            syncFailsWith(error)
            val presented = mutableListOf<ErrorPresenter.Params>()
            var errorCompletion: ErrorPresenter.Completion? = null
            manager.errorPresenter = ErrorPresenter { params, completion ->
                presented += params
                errorCompletion = completion
            }
            val completion = presentThroughRegisteredPresenter()
            var run: CheckpointRun? = null
            val call = launch { run = runCheckpoint() }

            completion()!!.complete(PaywallPresenter.Completion.Result.Closed)

            assertThat(presented).containsExactly(
                ErrorPresenter.Params(error, checkpointId, emptyMap(), CheckpointFailureKind.FLOW_RESULT_SYNC_FAILED),
            )
            assertThat(run).isNull()
            errorCompletion!!.complete()
            errorCompletion!!.complete()
            call.join()
            assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.SyncFailed)
        }

    @Test
    fun `overlapping sync failures reach the error presenter one at a time`() = runTest(dispatcher) {
        syncFailsWith(PurchasesError(PurchasesErrorCode.NetworkError, "Simulated."))
        val presented = mutableListOf<ErrorPresenter.Params>()
        val errorCompletions = mutableListOf<ErrorPresenter.Completion>()
        manager.errorPresenter = ErrorPresenter { params, completion ->
            presented += params
            errorCompletions += completion
        }
        val completion = presentThroughRegisteredPresenter()
        var first: CheckpointRun? = null
        val firstCall = launch { first = runCheckpoint() }
        completion()!!.complete(PaywallPresenter.Completion.Result.Closed)
        var second: CheckpointRun? = null
        val secondCall = launch { second = runCheckpoint() }
        completion()!!.complete(PaywallPresenter.Completion.Result.Closed)

        assertThat(presented).hasSize(1)
        assertThat(first).isNull()
        assertThat(second).isNull()

        errorCompletions[0].complete()
        firstCall.join()

        assertThat(first!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.SyncFailed)
        assertThat(presented).hasSize(2)
        assertThat(second).isNull()

        errorCompletions[1].complete()
        secondCall.join()

        assertThat(second!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.SyncFailed)
    }

    @Test
    fun `a sync failure whose app presenter throws is presented through the SDK's dialog`() = runTest(dispatcher) {
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "Simulated.")
        syncFailsWith(error)
        manager.errorPresenter = ErrorPresenter { _, _ -> error("Simulated.") }
        val completion = presentThroughRegisteredPresenter()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }

        completion()!!.complete(PaywallPresenter.Completion.Result.Closed)
        call.join()

        assertThat(defaultPresentedErrors).containsExactly(
            ErrorPresenter.Params(error, checkpointId, emptyMap(), CheckpointFailureKind.FLOW_RESULT_SYNC_FAILED),
        )
        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.SyncFailed)
        verify(exactly = 1) { Logger.e(match { it.startsWith("Error presenter failed") }) }
    }

    @Test
    fun `only the presenter's first report counts`() = runTest(dispatcher) {
        val customerInfo = mockk<CustomerInfo>()
        syncedCustomerInfoIs(customerInfo)
        val completion = presentThroughRegisteredPresenter()
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }

        completion()!!.complete(PaywallPresenter.Completion.Result.Closed)
        completion()!!.complete(PaywallPresenter.Completion.Result.Continued)
        completion()!!.complete(PaywallPresenter.Completion.Result.NavigatedBack)
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Finished(customerInfo))
        verify(exactly = 1) { mockPurchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, any()) }
    }

    @Test
    fun `an app-owned presentation claims the one-presentation slot`() = runTest(dispatcher) {
        manager.paywallPresenter = PaywallPresenter { _, _ -> }
        coEvery { mockPurchases.internalResolveCp(any(), any()) } returnsMany listOf(
            CheckpointResolution.MatchedOffering(offering(), checkpointRuleId = null, traceId = "trace_id"),
            CheckpointResolution.MatchedWorkflow(
                mockk(),
                mockk(),
                offerings(),
                checkpointRuleId = null,
                traceId = "trace-id",
                workflowBlobRef = null,
            ),
        )
        val presenterCall = launch { runCheckpoint() }

        val run = runCheckpoint()

        assertThat(run.blockedByPresentedFlow).isTrue
        assertThat(run.flowOutcome).isNull()
        assertThat(presentedCallIds).isEmpty()
        presenterCall.cancel()
    }

    @Test
    fun `a throwing presenter fails and releases the slot`() = runTest(dispatcher) {
        manager.paywallPresenter = PaywallPresenter { _, _ -> error("Simulated.") }
        resolvesTo(CheckpointResolution.MatchedOffering(offering(), checkpointRuleId = null, traceId = "trace_id"))

        checkpointFailsWith(
            PurchasesErrorCode.ConfigurationError,
            "Paywall presenter failed: java.lang.IllegalStateException: Simulated.",
        )

        manager.paywallPresenter = null
        val customerInfo = mockk<CustomerInfo>()
        syncedCustomerInfoIs(customerInfo)
        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint() }
        finishDefaultPaywall()
        call.join()
        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Finished(customerInfo))
    }

    @Test
    fun `cancelling an app-owned presentation releases the slot and ignores a late report`() = runTest(dispatcher) {
        val completion = presentThroughRegisteredPresenter()
        val presenterCall = launch { runCheckpoint() }

        presenterCall.cancel()
        presenterCall.join()
        // The pending call died with the caller, so this report must be a no-op, sync included.
        completion()!!.complete(PaywallPresenter.Completion.Result.Closed)
        verify(exactly = 0) { mockPurchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, any()) }

        manager.paywallPresenter = null
        val customerInfo = mockk<CustomerInfo>()
        syncedCustomerInfoIs(customerInfo)
        var run: CheckpointRun? = null
        val secondCall = launch { run = runCheckpoint() }
        finishDefaultPaywall()
        secondCall.join()
        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Finished(customerInfo))
    }

    @Test
    fun `a per-call presenter presents a matched offering when none is registered`() = runTest(dispatcher) {
        val offering = offering()
        val customerInfo = mockk<CustomerInfo>()
        syncedCustomerInfoIs(customerInfo)
        resolvesTo(CheckpointResolution.MatchedOffering(offering, checkpointRuleId = null, traceId = "trace_id"))
        var presented: Offering? = null
        var completion: PaywallPresenter.Completion? = null
        val params = CheckpointParams {
            paywallPresenter { presenterParams, presentation ->
                presented = presenterParams.offering
                completion = presentation
            }
        }

        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint(params) }

        assertThat(presented).isEqualTo(offering)
        assertThat(presentedCallIds).isEmpty()
        assertThat(defaultPresenter).isNull()
        completion!!.complete(PaywallPresenter.Completion.Result.Closed)
        call.join()

        assertThat(run!!.flowOutcome).isEqualTo(CheckpointFlowOutcome.Finished(customerInfo))
    }

    @Test
    fun `a per-call presenter takes precedence over the registered one`() = runTest(dispatcher) {
        val registeredCompletion = presentThroughRegisteredPresenter()
        var completion: PaywallPresenter.Completion? = null
        val params = CheckpointParams { paywallPresenter { _, presentation -> completion = presentation } }

        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint(params) }

        assertThat(registeredCompletion()).isNull()
        assertThat(completion).isNotNull
        completion!!.complete(PaywallPresenter.Completion.Result.NavigatedBack)
        call.join()

        assertThat(run!!.backedOut).isTrue
    }

    @Test
    fun `params without a presenter fall back to the registered one`() = runTest(dispatcher) {
        val registeredCompletion = presentThroughRegisteredPresenter()
        val params = CheckpointParams { customVariables { "source" to "test" } }

        var run: CheckpointRun? = null
        val call = launch { run = runCheckpoint(params) }

        registeredCompletion()!!.complete(PaywallPresenter.Completion.Result.NavigatedBack)
        call.join()

        assertThat(run!!.backedOut).isTrue
    }

    @Test
    fun `the workflow window's options hand errors to the registered error presenter with the checkpoint's context`() =
        runTest(dispatcher) {
            resolvesToWorkflow()
            val presented = mutableListOf<ErrorPresenter.Params>()
            manager.errorPresenter = ErrorPresenter { params, _ -> presented += params }
            val params = CheckpointParams { customVariables { "source" to "test" } }
            val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "boom")
            val call = launch { runCheckpoint(params) }

            val options = manager.paywallOptions(currentCallId()) {}!!
            options.errorPresenter!!.present(error, CheckpointFailureKind.FLOW_ACTION_FAILED) {}

            assertThat(presented).containsExactly(
                ErrorPresenter.Params(
                    error,
                    checkpointId,
                    params.customVariables,
                    CheckpointFailureKind.FLOW_ACTION_FAILED,
                ),
            )
            finishPaywall(outcome = null)
            call.join()
        }

    @Test
    fun `a workflow paywall error whose app presenter throws is presented through the SDK's dialog`() =
        runTest(dispatcher) {
            resolvesToWorkflow()
            manager.errorPresenter = ErrorPresenter { _, _ -> error("Simulated.") }
            val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "boom")
            var completed = false
            val call = launch { runCheckpoint() }

            val options = manager.paywallOptions(currentCallId()) {}!!
            options.errorPresenter!!.present(error, CheckpointFailureKind.FLOW_ACTION_FAILED) { completed = true }

            assertThat(defaultPresentedErrors).containsExactly(
                ErrorPresenter.Params(error, checkpointId, emptyMap(), CheckpointFailureKind.FLOW_ACTION_FAILED),
            )
            assertThat(completed).isTrue
            verify(exactly = 1) { Logger.e(match { it.startsWith("Error presenter failed") }) }
            finishPaywall(outcome = null)
            call.join()
        }

    @Test
    fun `without an error presenter the workflow window's errors go to the SDK's own`() = runTest(dispatcher) {
        resolvesToWorkflow()
        val call = launch { runCheckpoint() }

        assertThat(manager.paywallOptions(currentCallId()) {}!!.errorPresenter).isNotNull
        assertThat(defaultErrorPresenters).hasSize(1)

        finishPaywall(outcome = null)
        call.join()
    }

    @Test
    fun `without an error presenter the SDK's own paywall's errors go to the SDK's own`() = runTest(dispatcher) {
        resolvesTo(CheckpointResolution.MatchedOffering(offering(), checkpointRuleId = null, traceId = "trace_id"))
        val call = launch { runCheckpoint() }

        assertThat(defaultPresenter!!.paywallOptions("") {}!!.errorPresenter).isNotNull
        assertThat(defaultErrorPresenters).hasSize(1)

        finishDefaultPaywall(navigatedBack = true)
        call.join()
    }

    @Test
    fun `a registered error presenter leaves the SDK's own out`() = runTest(dispatcher) {
        resolvesToWorkflow()
        manager.errorPresenter = ErrorPresenter { _, _ -> }
        val call = launch { runCheckpoint() }

        assertThat(defaultErrorPresenters).isEmpty()

        finishPaywall(outcome = null)
        call.join()
    }

    @Test
    fun `an app paywall presenter's call creates no error presenter`() = runTest(dispatcher) {
        val completion = presentThroughRegisteredPresenter()
        val call = launch { runCheckpoint() }

        assertThat(defaultErrorPresenters).isEmpty()

        completion()!!.complete(PaywallPresenter.Completion.Result.NavigatedBack)
        call.join()
    }

    @Test
    fun `a per-call error presenter takes precedence over the registered one`() = runTest(dispatcher) {
        resolvesToWorkflow()
        var registeredAsked = false
        var perCallAsked = false
        manager.errorPresenter = ErrorPresenter { _, _ -> registeredAsked = true }
        val params = CheckpointParams { errorPresenter { _, _ -> perCallAsked = true } }
        val call = launch { runCheckpoint(params) }

        manager.paywallOptions(currentCallId()) {}!!.errorPresenter!!
            .present(PurchasesError(PurchasesErrorCode.StoreProblemError), CheckpointFailureKind.FLOW_ACTION_FAILED) {}

        assertThat(perCallAsked).isTrue
        assertThat(registeredAsked).isFalse
        finishPaywall(outcome = null)
        call.join()
    }

    @Test
    fun `the SDK's own paywall hands its errors to the error presenter`() = runTest(dispatcher) {
        resolvesTo(CheckpointResolution.MatchedOffering(offering(), checkpointRuleId = null, traceId = "trace_id"))
        val presented = mutableListOf<ErrorPresenter.Params>()
        manager.errorPresenter = ErrorPresenter { params, _ -> presented += params }
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "boom")
        val call = launch { runCheckpoint() }

        defaultPresenter!!.paywallOptions("") {}!!.errorPresenter!!
            .present(error, CheckpointFailureKind.FLOW_COULD_NOT_BE_SHOWN) {}

        assertThat(presented).containsExactly(
            ErrorPresenter.Params(error, checkpointId, emptyMap(), CheckpointFailureKind.FLOW_COULD_NOT_BE_SHOWN),
        )
        finishDefaultPaywall(navigatedBack = true)
        call.join()
    }

    // Registers a presenter for a matched offering and returns an accessor for the completion it was handed.
    private fun presentThroughRegisteredPresenter(): () -> PaywallPresenter.Completion? {
        var completion: PaywallPresenter.Completion? = null
        manager.paywallPresenter = PaywallPresenter { _, presentation ->
            completion = presentation
        }
        resolvesTo(CheckpointResolution.MatchedOffering(offering(), checkpointRuleId = null, traceId = "trace_id"))
        return { completion }
    }

    private fun customerInfoBeforeHasActive(vararg identifiers: String) {
        every { mockPurchases.getCustomerInfo(CacheFetchPolicy.CACHED_OR_FETCHED, any()) } answers {
            secondArg<ReceiveCustomerInfoCallback>().onReceived(customerInfoWithActive(*identifiers))
        }
    }

    private fun customerInfoReadFailsWith(error: PurchasesError) {
        every { mockPurchases.getCustomerInfo(CacheFetchPolicy.CACHED_OR_FETCHED, any()) } answers {
            secondArg<ReceiveCustomerInfoCallback>().onError(error)
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

    private val CheckpointRun.presentedProducts: List<StoreProduct>
        get() = presentedOfferings.flatMap { offering -> offering.availablePackages.map { it.product } }

    private fun offering(vararg products: StoreProduct): Offering = mockk {
        every { availablePackages } returns products.map { p -> mockk<Package> { every { product } returns p } }
    }

    private fun offerings(vararg products: StoreProduct): Offerings = mockk {
        every { all } returns mapOf("default" to offering(*products))
    }

    private fun CustomerInfo.purchased(vararg identifiers: String, date: Date = Date(1_000)): CustomerInfo = apply {
        identifiers.forEach { identifier -> every { getPurchaseDateForProductId(identifier) } returns date }
    }

    private fun syncedCustomerInfoIs(customerInfo: CustomerInfo) {
        every { mockPurchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, any()) } answers {
            secondArg<ReceiveCustomerInfoCallback>().onReceived(customerInfo)
        }
    }

    private fun syncFailsWith(error: PurchasesError) {
        every { mockPurchases.getCustomerInfo(CacheFetchPolicy.FETCH_CURRENT, any()) } answers {
            secondArg<ReceiveCustomerInfoCallback>().onError(error)
        }
    }

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

    private fun currentCallId(): String = presentedCallIds.last()

    // Mirrors what CheckpointWorkflowPresenter does for a workflow: read the pending call for the presented callId,
    // record the outcome, then report the paywall as finished.
    private fun finishPaywall(outcome: CheckpointFlowOutcome?, navigatedBack: Boolean = false) {
        val callId = currentCallId()
        assertThat(manager.presentation(callId)).isNotNull
        outcome?.let { manager.recordOutcome(callId, it) }
        manager.onPresentationFinished(callId, navigatedBack)
    }

    // Mirrors what CheckpointWorkflowPresenter does for the SDK's own presenter: report how the window went away.
    private fun finishDefaultPaywall(navigatedBack: Boolean = false) {
        defaultPresenter!!.onPresentationFinished("", navigatedBack)
    }

    // The callback API; completes synchronously here because the manager's scope runs on the unconfined main
    // dispatcher, except while a presented paywall is waiting to finish.
    private fun checkpoint(params: CheckpointParams? = null, identifier: String = checkpointId) {
        manager.checkpoint(mockPurchases, identifier, params) { results += it }
    }

    private suspend fun runCheckpoint(params: CheckpointParams? = null): CheckpointRun =
        manager.runCheckpoint(mockPurchases, checkpointId, params)

    // Presentation failures are logged and presented rather than returned: the run just fails.
    private suspend fun checkpointFailsWith(code: PurchasesErrorCode, message: String) {
        val run = runCheckpoint()
        assertThat(run.failed).isTrue
        assertThat(run.flowOutcome).isNull()
        assertThat(defaultPresentedErrors.last().error).isEqualTo(PurchasesError(code, message))
        verify { Logger.e(PurchasesError(code, message).toString()) }
    }
}
