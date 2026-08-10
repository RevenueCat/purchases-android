package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.common.events.FeatureEvent
import com.revenuecat.purchases.common.workflows.WorkflowResolution
import com.revenuecat.purchases.common.workflows.WorkflowTriggerType
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.paywalls.events.PaywallEventType
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import com.revenuecat.purchases.ui.revenuecatui.testfixtures.TwoStepWorkflowFixture
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

/**
 * Composition-level coverage for re-presenting a retained [PaywallViewModelImpl].
 *
 * The ViewModel is scoped to the host's `ViewModelStoreOwner`, so an embedded or dialog paywall can be
 * dismissed and shown again on the same instance. These tests drive that through real composition
 * (mount, dismiss, unmount, remount) rather than by calling the presentation hook directly, so they also
 * cover the wiring in [InternalPaywall] that fires it.
 */
@RunWith(AndroidJUnit4::class)
class InternalPaywallRePresentationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val workflow = TwoStepWorkflowFixture.workflow
    private val uiConfig = UiConfig()

    private lateinit var purchases: PurchasesType

    @Before
    fun setUp() {
        purchases = mockk {
            every { storefrontCountryCode } returns "US"
            every { preferredUILocaleOverride } returns null
            every { purchasesAreCompletedBy } returns PurchasesAreCompletedBy.REVENUECAT
            every { track(any()) } just Runs
            coEvery { awaitOfferings() } returns TwoStepWorkflowFixture.offerings
            coEvery { awaitCustomerInfo(any()) } returns mockk {
                every { activeSubscriptions } returns setOf()
                every { nonSubscriptionTransactions } returns listOf()
            }
            coEvery {
                resolveWorkflow(TwoStepWorkflowFixture.OFFERING_ID)
            } returns WorkflowResolution.Found(workflow.id)
            coEvery { awaitGetWorkflow(workflow.id) } returns workflow
            coEvery { awaitGetUiConfig() } returns uiConfig
        }
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createVm(options: PaywallOptions) = PaywallViewModelImpl(
        resourceProvider = MockResourceProvider(),
        purchases = purchases,
        options = options,
        colorScheme = TestData.Constants.currentColorScheme,
        isDarkMode = false,
        shouldDisplayBlock = null,
    )

    @Test
    fun `re-presenting a dismissed paywall restarts the retained workflow at its initial step`() {
        var presented by mutableStateOf(true)
        // The ViewModel and the composable must share one PaywallOptions so closePaywall's dismissRequest
        // is the one that unmounts the paywall, the way a real host wires it.
        val options = PaywallOptions.Builder(dismissRequest = { presented = false }).build()
        val vm = createVm(options)

        composeTestRule.setContent {
            if (presented) {
                InternalPaywall(options = options, viewModel = vm)
            }
        }

        composeTestRule.waitUntil { vm.workflowState.value?.currentStepId == "step-1" }

        // Record every state the paywall passes through from here on. Unconfined collects on the emitting
        // thread, so a transient Loading between dismiss and re-present cannot slip by unobserved.
        val observedStates = mutableListOf<PaywallState>()
        val collectorScope = CoroutineScope(Dispatchers.Unconfined)
        collectorScope.launch { vm.state.collect { observedStates.add(it) } }

        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        composeTestRule.waitForIdle()
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-2")

        // The user taps close on step two. The host unmounts the paywall, the ViewModel survives.
        vm.closePaywall(result = null)
        composeTestRule.waitForIdle()
        assertThat(presented).isFalse()

        // The host shows the same paywall again on the same retained ViewModel.
        presented = true
        composeTestRule.waitForIdle()
        collectorScope.cancel()

        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
        // The retained last frame is what carries the host's exit animation, and the replay is
        // synchronous, so the paywall must never fall back to the loading skeleton on the way.
        assertThat(observedStates).doesNotContain(PaywallState.Loading)
    }

    /**
     * Characterizes the limitation documented on `PaywallViewModel.onPaywallPresented`: the replay hangs
     * off composition entry, so a host that dismisses without unmounting never fires it. This test exists
     * to make that boundary explicit and to fail loudly if a future change quietly moves it, in either
     * direction.
     */
    @Test
    fun `a host that never unmounts the paywall does not get the workflow replayed`() {
        val options = PaywallOptions.Builder(dismissRequest = {}).build()
        val vm = createVm(options)

        composeTestRule.setContent { InternalPaywall(options = options, viewModel = vm) }

        composeTestRule.waitUntil { vm.workflowState.value?.currentStepId == "step-1" }
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        composeTestRule.waitForIdle()
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-2")
        val stepTwoState = vm.state.value

        // dismissRequest is a no-op here, so InternalPaywall stays composed across the dismiss.
        vm.closePaywall(result = null)
        composeTestRule.waitForIdle()

        // No composition entry, so no replay: workflow state is gone and _state is still the very same
        // step-two state, which InternalPaywall renders through the non-workflow branch.
        assertThat(vm.workflowState.value).isNull()
        assertThat(vm.state.value).isSameAs(stepTwoState)
    }

    @Test
    fun `re-presenting a dismissed paywall tracks one impression, not one per stale frame`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        var presented by mutableStateOf(true)
        val options = PaywallOptions.Builder(dismissRequest = { presented = false }).build()
        val vm = createVm(options)

        composeTestRule.setContent {
            if (presented) {
                InternalPaywall(options = options, viewModel = vm)
            }
        }

        composeTestRule.waitUntil { vm.workflowState.value?.currentStepId == "step-1" }
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        composeTestRule.waitForIdle()

        vm.closePaywall(result = null)
        composeTestRule.waitForIdle()
        captured.clear()

        presented = true
        composeTestRule.waitForIdle()

        // _state deliberately still holds the dismissed step when the paywall re-enters composition, so
        // if the replay landed after the first frame was read, the stale step would bill its own
        // impression before step one billed the real one.
        val impressions = captured.filterIsInstance<PaywallEvent>()
            .filter { it.type == PaywallEventType.IMPRESSION }
        assertThat(impressions).hasSize(1)
        // Counting alone would still pass if the one impression were the stale step's: that one is billed
        // outside a workflow presentation, so it carries no workflowId and no traceId.
        val impression = impressions.single()
        assertThat(impression.data.workflowId).isEqualTo(workflow.id)
        assertThat(impression.data.traceId).isNotNull()
    }
}
