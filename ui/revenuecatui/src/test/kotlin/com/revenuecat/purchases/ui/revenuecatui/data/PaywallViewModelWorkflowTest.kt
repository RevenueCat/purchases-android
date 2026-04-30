@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.WorkflowDataResult
import com.revenuecat.purchases.common.workflows.WorkflowScreen
import com.revenuecat.purchases.common.workflows.WorkflowStep
import com.revenuecat.purchases.common.workflows.WorkflowTrigger
import com.revenuecat.purchases.common.workflows.WorkflowTriggerAction
import com.revenuecat.purchases.common.workflows.WorkflowTriggerType
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import com.revenuecat.purchases.ui.revenuecatui.workflow.NavigationDirection
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(AndroidJUnit4::class)
class PaywallViewModelWorkflowTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var purchases: PurchasesType

    // Both steps share one offering; screen1 and screen2 differ only by ID.
    private val offeringId = "test_offering"
    private val screenId1 = "screen-1"
    private val screenId2 = "screen-2"

    private val defaultLocaleId = LocaleId("en_US")
    private val localizations = mapOf(
        defaultLocaleId to mapOf(
            LocalizationKey("dummy_text") to LocalizationData.Text("dummy"),
        ),
    )
    private val componentsConfig = ComponentsConfig(
        base = PaywallComponentsConfig(
            // At least one PackageComponent is required for calculateState to produce
            // PaywallState.Loaded.Components instead of PaywallState.Error.
            stack = StackComponent(components = listOf(TestData.Components.monthlyPackageComponent)),
            background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
            stickyFooter = null,
        ),
    )

    private fun makeScreen(screenId: String) = WorkflowScreen(
        name = screenId,
        templateName = "template_v2",
        revision = 1,
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = componentsConfig,
        componentsLocalizations = localizations,
        defaultLocaleIdentifier = defaultLocaleId,
        offeringIdentifier = offeringId,
    )

    private val step1 = WorkflowStep(
        id = "step-1",
        type = "screen",
        screenId = screenId1,
        triggers = listOf(
            WorkflowTrigger(
                name = "Next",
                type = WorkflowTriggerType.ON_PRESS,
                actionId = "action-next",
                componentId = "btn-next",
            ),
        ),
        triggerActions = mapOf("action-next" to WorkflowTriggerAction.Step(stepId = "step-2")),
    )
    private val step2 = WorkflowStep(
        id = "step-2",
        type = "screen",
        screenId = screenId2,
        triggers = emptyList(),
        triggerActions = emptyMap(),
    )

    private val workflow = PublishedWorkflow(
        id = "wfl-test",
        displayName = "Test",
        initialStepId = "step-1",
        steps = mapOf("step-1" to step1, "step-2" to step2),
        screens = mapOf(screenId1 to makeScreen(screenId1), screenId2 to makeScreen(screenId2)),
        uiConfig = UiConfig(),
        metadata = emptyMap(),
    )
    private val fetchResult = WorkflowDataResult(workflow = workflow, enrolledVariants = null)

    private val testOffering = Offering(
        identifier = offeringId,
        serverDescription = "",
        metadata = emptyMap(),
        availablePackages = listOf(TestData.Packages.monthly),
        paywallComponents = null,
        webCheckoutURL = null,
    )
    private val testOfferings = Offerings(testOffering, mapOf(offeringId to testOffering))

    @Before
    fun setUp() {
        purchases = mockk {
            every { storefrontCountryCode } returns "US"
            every { preferredUILocaleOverride } returns null
            every { purchasesAreCompletedBy } returns PurchasesAreCompletedBy.REVENUECAT
            every { track(any()) } just Runs
            coEvery { awaitOfferings() } returns testOfferings
            coEvery { awaitCustomerInfo(any()) } returns mockk {
                every { activeSubscriptions } returns setOf()
                every { nonSubscriptionTransactions } returns listOf()
            }
        }
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun createVm(): PaywallViewModelImpl = PaywallViewModelImpl(
        resourceProvider = MockResourceProvider(),
        purchases = purchases,
        options = PaywallOptions.Builder(dismissRequest = {}).build(),
        colorScheme = TestData.Constants.currentColorScheme,
        isDarkMode = false,
        shouldDisplayBlock = null,
    )

    // region forward navigation

    @Test
    fun `forward navigation sets FORWARD on pendingTransition`() {
        val vm = createVm()
        vm.updateStateFromWorkflow(fetchResult, testOfferings, null)

        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)

        assertThat(vm.workflowState.value?.pendingTransition?.direction)
            .isEqualTo(NavigationDirection.FORWARD)
    }

    @Test
    fun `forward navigation sets fromStepId to the step navigated away from`() {
        val vm = createVm()
        vm.updateStateFromWorkflow(fetchResult, testOfferings, null)

        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)

        assertThat(vm.workflowState.value?.pendingTransition?.fromStepId).isEqualTo("step-1")
    }

    // endregion

    // region back navigation

    @Test
    fun `back navigation sets BACKWARD on pendingTransition`() {
        val vm = createVm()
        vm.updateStateFromWorkflow(fetchResult, testOfferings, null)
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        vm.onTransitionComplete(vm.workflowState.value!!.pendingTransition!!.id)

        vm.handleBackNavigation()

        assertThat(vm.workflowState.value?.pendingTransition?.direction)
            .isEqualTo(NavigationDirection.BACKWARD)
    }

    @Test
    fun `back navigation sets fromStepId to the step navigated away from`() {
        val vm = createVm()
        vm.updateStateFromWorkflow(fetchResult, testOfferings, null)
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        vm.onTransitionComplete(vm.workflowState.value!!.pendingTransition!!.id)

        vm.handleBackNavigation()

        assertThat(vm.workflowState.value?.pendingTransition?.fromStepId).isEqualTo("step-2")
    }

    // endregion

    // region cache

    @Test
    fun `second visit to a step returns the cached state instance`() {
        val vm = createVm()
        vm.updateStateFromWorkflow(fetchResult, testOfferings, null)

        // First visit to step-2: state is computed and cached.
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        val firstState = vm.workflowState.value?.stepStates?.get("step-2")
        assertThat(firstState).isNotNull()

        // Navigate back to step-1, then forward to step-2 again.
        vm.onTransitionComplete(vm.workflowState.value!!.pendingTransition!!.id)
        vm.handleBackNavigation()
        vm.onTransitionComplete(vm.workflowState.value!!.pendingTransition!!.id)
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)

        assertThat(vm.workflowState.value?.stepStates?.get("step-2")).isSameAs(firstState)
    }

    // endregion

    // region onTransitionComplete

    @Test
    fun `onTransitionComplete clears pendingTransition for the matching id`() {
        val vm = createVm()
        vm.updateStateFromWorkflow(fetchResult, testOfferings, null)
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        val id = vm.workflowState.value!!.pendingTransition!!.id

        vm.onTransitionComplete(id)

        assertThat(vm.workflowState.value?.pendingTransition).isNull()
    }

    @Test
    fun `onTransitionComplete with stale id does not clear a newer transition`() {
        val vm = createVm()
        vm.updateStateFromWorkflow(fetchResult, testOfferings, null)
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        val staleId = vm.workflowState.value!!.pendingTransition!!.id
        vm.onTransitionComplete(staleId)

        // A new transition starts (back navigation).
        vm.handleBackNavigation()
        val currentId = vm.workflowState.value!!.pendingTransition!!.id
        assertThat(currentId).isNotEqualTo(staleId)

        vm.onTransitionComplete(staleId)

        assertThat(vm.workflowState.value?.pendingTransition?.id).isEqualTo(currentId)
    }

    // endregion
}
