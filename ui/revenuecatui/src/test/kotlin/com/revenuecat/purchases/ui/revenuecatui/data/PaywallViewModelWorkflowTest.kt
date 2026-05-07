@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
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
import com.revenuecat.purchases.paywalls.components.common.ExitOffer
import com.revenuecat.purchases.paywalls.components.common.ExitOffers
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PaywallViewModelWorkflowTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

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

    // Two-package fixture: monthly (default) + annual, for propagation tests.
    private val annualPackageComponent = PackageComponent(
        packageId = PackageType.ANNUAL.identifier!!,
        isSelectedByDefault = false,
        stack = StackComponent(components = emptyList()),
    )
    // PackageComponent uses @Poko (not a data class), so reconstruct rather than .copy().
    private val monthlyPackageComponentDefault = PackageComponent(
        packageId = PackageType.MONTHLY.identifier!!,
        isSelectedByDefault = true,
        stack = StackComponent(components = emptyList()),
    )
    private val twoPackageComponentsConfig = ComponentsConfig(
        base = PaywallComponentsConfig(
            stack = StackComponent(
                components = listOf(monthlyPackageComponentDefault, annualPackageComponent),
            ),
            background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
            stickyFooter = null,
        ),
    )

    // Component config with NO PackageComponent — simulates an early "info" screen.
    private val noPackagesComponentsConfig = ComponentsConfig(
        base = PaywallComponentsConfig(
            stack = StackComponent(components = emptyList()),
            background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
            stickyFooter = null,
        ),
    )

    private fun makeTwoPackageWorkflow(): Pair<WorkflowDataResult, Offerings> {
        val screen1 = makeScreen(screenId1).copy(componentsConfig = twoPackageComponentsConfig)
        val screen2 = makeScreen(screenId2).copy(componentsConfig = twoPackageComponentsConfig)
        val wfl = workflow.copy(
            screens = mapOf(screenId1 to screen1, screenId2 to screen2),
        )
        val offering = Offering(
            identifier = offeringId,
            serverDescription = "",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
            paywallComponents = null,
            webCheckoutURL = null,
        )
        val offerings = Offerings(offering, mapOf(offeringId to offering))
        return WorkflowDataResult(workflow = wfl, enrolledVariants = null) to offerings
    }

    private fun makeContextPackageWorkflow(): Pair<WorkflowDataResult, Offerings> {
        val earlyScreen = makeScreen(screenId1).copy(componentsConfig = noPackagesComponentsConfig)
        val terminalScreen = makeScreen(screenId2).copy(componentsConfig = twoPackageComponentsConfig)

        val earlyStep = WorkflowStep(
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
            paramValues = emptyMap(),
        )
        val terminalStep = WorkflowStep(
            id = "step-2",
            type = "screen",
            screenId = screenId2,
            triggers = emptyList(),
            triggerActions = emptyMap(),
            paramValues = emptyMap(),
        )
        val wfl = workflow.copy(
            steps = mapOf("step-1" to earlyStep, "step-2" to terminalStep),
            screens = mapOf(screenId1 to earlyScreen, screenId2 to terminalScreen),
            singleStepFallbackId = "step-2",
        )
        val offering = Offering(
            identifier = offeringId,
            serverDescription = "",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
            paywallComponents = null,
            webCheckoutURL = null,
        )
        val offerings = Offerings(offering, mapOf(offeringId to offering))
        return WorkflowDataResult(workflow = wfl, enrolledVariants = null) to offerings
    }

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

    private val exitOfferingId = "exit-offering-id"
    private val exitOffering = Offering(
        identifier = exitOfferingId,
        serverDescription = "",
        metadata = emptyMap(),
        availablePackages = listOf(TestData.Packages.monthly),
        paywallComponents = null,
        webCheckoutURL = null,
    )
    private val testOfferingsWithExitOffer = Offerings(
        testOffering,
        mapOf(offeringId to testOffering, exitOfferingId to exitOffering),
    )

    private fun makeScreenWithExitOffer(screenId: String) = WorkflowScreen(
        name = screenId,
        templateName = "template_v2",
        revision = 1,
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = componentsConfig,
        componentsLocalizations = localizations,
        defaultLocaleIdentifier = defaultLocaleId,
        offeringIdentifier = offeringId,
        exitOffers = ExitOffers(dismiss = ExitOffer(offeringId = exitOfferingId)),
    )

    private val workflowWithExitOffer = PublishedWorkflow(
        id = "wfl-test-exit",
        displayName = "Test Exit",
        initialStepId = "step-1",
        steps = mapOf("step-1" to step1, "step-2" to step2),
        screens = mapOf(
            screenId1 to makeScreen(screenId1),
            screenId2 to makeScreenWithExitOffer(screenId2),
        ),
        uiConfig = UiConfig(),
        metadata = emptyMap(),
    )
    private val fetchResultWithExitOffer = WorkflowDataResult(
        workflow = workflowWithExitOffer,
        enrolledVariants = null,
    )

    private val singleStep = WorkflowStep(
        id = "step-only",
        type = "screen",
        screenId = screenId1,
        triggers = emptyList(),
        triggerActions = emptyMap(),
    )

    private val singleStepWorkflowWithExitOffer = PublishedWorkflow(
        id = "wfl-single",
        displayName = "Single Step",
        initialStepId = "step-only",
        steps = mapOf("step-only" to singleStep),
        screens = mapOf(screenId1 to makeScreenWithExitOffer(screenId1)),
        uiConfig = UiConfig(),
        metadata = emptyMap(),
    )
    private val singleStepFetchResultWithExitOffer = WorkflowDataResult(
        workflow = singleStepWorkflowWithExitOffer,
        enrolledVariants = null,
    )

    // Exit offer is on step-1's screen, not step-2's (the terminal step). Without
    // single_step_fallback_id, the traversal would land on step-2 and miss the exit offer.
    private val workflowWithFallbackPointingToFirstStep = PublishedWorkflow(
        id = "wfl-fallback",
        displayName = "Fallback Test",
        initialStepId = "step-1",
        steps = mapOf("step-1" to step1, "step-2" to step2),
        screens = mapOf(
            screenId1 to makeScreenWithExitOffer(screenId1),
            screenId2 to makeScreen(screenId2),
        ),
        uiConfig = UiConfig(),
        metadata = emptyMap(),
        singleStepFallbackId = "step-1",
    )
    private val fetchResultWithFallback = WorkflowDataResult(
        workflow = workflowWithFallbackPointingToFirstStep,
        enrolledVariants = null,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
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
        Dispatchers.resetMain()
        clearAllMocks()
    }

    private fun createVm(
        dismissRequestWithExitOffering: ((Offering?, PaywallResult?) -> Unit)? = null,
    ): PaywallViewModelImpl {
        val builder = PaywallOptions.Builder(dismissRequest = {})
        dismissRequestWithExitOffering?.let { builder.setDismissRequestWithExitOffering(it) }
        return PaywallViewModelImpl(
            resourceProvider = MockResourceProvider(),
            purchases = purchases,
            options = builder.build(),
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false,
            shouldDisplayBlock = null,
        )
    }

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

    @Test
    fun `back navigation returns step state with the selection the user left on it`() {
        val (fetchResult2, offerings2) = makeTwoPackageWorkflow()
        val vm = createVm()
        vm.updateStateFromWorkflow(fetchResult2, offerings2, null)

        // Switch step-1 to annual before navigating forward.
        val step1State = vm.workflowState.value?.stepStates?.get("step-1")!!
        step1State.update(PackageType.ANNUAL.identifier!!)

        // Navigate to step-2 and complete the transition.
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        vm.onTransitionComplete(vm.workflowState.value!!.pendingTransition!!.id)

        // Navigate back to step-1.
        vm.handleBackNavigation()

        // The cached step-1 state object must be the exact same instance with annual still selected.
        val step1StateAfterBack = vm.workflowState.value?.stepStates?.get("step-1")
        assertThat(step1StateAfterBack).isSameAs(step1State)
        assertThat(step1StateAfterBack?.selectedPackageInfo?.uniqueId)
            .isEqualTo(PackageType.ANNUAL.identifier)
    }

    @Test
    fun `two rapid forward navigations from same step do not corrupt state`() {
        val vm = createVm()
        vm.updateStateFromWorkflow(fetchResult, testOfferings, null)

        // Two calls before any transition completes.
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        val firstPendingId = vm.workflowState.value?.pendingTransition?.id
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)

        // The second tap is a no-op: currentStep is already step-2, which has no triggers.
        // The pending transition ID must not have changed.
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-2")
        assertThat(vm.workflowState.value?.pendingTransition?.id).isEqualTo(firstPendingId)
    }

    @Test
    fun `custom variables from options are visible on step 2`() {
        val customVars = mapOf("highlight_color" to CustomVariableValue.String("gold"))
        val vmWithVars = PaywallViewModelImpl(
            resourceProvider = MockResourceProvider(),
            purchases = purchases,
            options = PaywallOptions.Builder(dismissRequest = {})
                .setCustomVariables(customVars)
                .build(),
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false,
            shouldDisplayBlock = null,
        )
        vmWithVars.updateStateFromWorkflow(fetchResult, testOfferings, null)

        vmWithVars.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)

        val step2State = vmWithVars.workflowState.value?.stepStates?.get("step-2")
        assertThat(step2State).isNotNull()
        assertThat(
            (step2State!!.mergedCustomVariables["highlight_color"] as? CustomVariableValue.String)?.value,
        ).isEqualTo("gold")
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

    // region context package

    @Test
    fun `singleStepFallbackId package is applied as context on step with no own packages`() {
        val (result, offerings) = makeContextPackageWorkflow()
        val vm = createVm()

        vm.updateStateFromWorkflow(result, offerings, null)

        val step1State = vm.workflowState.value?.stepStates?.get("step-1")
        assertThat(step1State).isNotNull()
        // Step-1 has no PackageComponents → ownSelection is null → defaultPackageInfo from
        // singleStepFallbackId (step-2) is used. Step-2 defaults to MONTHLY.
        assertThat(step1State!!.selectedPackageInfo?.rcPackage?.identifier)
            .isEqualTo(PackageType.MONTHLY.identifier)
    }

    @Test
    fun `back navigation does not overwrite early step context on return`() {
        val (result, offerings) = makeContextPackageWorkflow()
        val vm = createVm()
        vm.updateStateFromWorkflow(result, offerings, null)

        // Step-1 initial context is MONTHLY (from singleStepFallbackId → step-2 default).
        val step1StateBefore = vm.workflowState.value?.stepStates?.get("step-1")
        assertThat(step1StateBefore?.selectedPackageInfo?.rcPackage?.identifier)
            .isEqualTo(PackageType.MONTHLY.identifier)

        // Navigate to terminal step (step-2) and select ANNUAL (different from default MONTHLY).
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        vm.onTransitionComplete(vm.workflowState.value!!.pendingTransition!!.id)
        val terminalState = vm.workflowState.value?.stepStates?.get("step-2")
        assertThat(terminalState).isNotNull()
        terminalState!!.update(PackageType.ANNUAL.identifier!!)

        // Navigate back — step-1's context must remain the initial MONTHLY default.
        vm.handleBackNavigation()
        val step1StateAfter = vm.workflowState.value?.stepStates?.get("step-1")
        assertThat(step1StateAfter?.selectedPackageInfo?.rcPackage?.identifier)
            .isEqualTo(PackageType.MONTHLY.identifier)
    }

    @Test
    fun `back navigation preserves initial context on packageless steps`() {
        // Build a three-step workflow:
        // step-A: no packages, screen-A
        // step-B: no packages, screen-B
        // step-C: terminal with two packages (MONTHLY default + ANNUAL), screen-C
        // singleStepFallbackId = "step-C"
        val screenAId = "screen-A"
        val screenBId = "screen-B"
        val screenCId = "screen-C"
        val threeStepOfferingId = "three_step_offering"

        val earlyScreen1 = WorkflowScreen(
            name = screenAId,
            templateName = "template_v2",
            revision = 1,
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = noPackagesComponentsConfig,
            componentsLocalizations = localizations,
            defaultLocaleIdentifier = defaultLocaleId,
            offeringIdentifier = threeStepOfferingId,
        )
        val earlyScreen2 = WorkflowScreen(
            name = screenBId,
            templateName = "template_v2",
            revision = 1,
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = noPackagesComponentsConfig,
            componentsLocalizations = localizations,
            defaultLocaleIdentifier = defaultLocaleId,
            offeringIdentifier = threeStepOfferingId,
        )
        val terminalScreen3 = WorkflowScreen(
            name = screenCId,
            templateName = "template_v2",
            revision = 1,
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = twoPackageComponentsConfig,
            componentsLocalizations = localizations,
            defaultLocaleIdentifier = defaultLocaleId,
            offeringIdentifier = threeStepOfferingId,
        )

        val stepA = WorkflowStep(
            id = "step-A",
            type = "screen",
            screenId = screenAId,
            triggers = listOf(
                WorkflowTrigger(
                    name = "Next",
                    type = WorkflowTriggerType.ON_PRESS,
                    actionId = "action-next",
                    componentId = "btn-next",
                ),
            ),
            triggerActions = mapOf("action-next" to WorkflowTriggerAction.Step(stepId = "step-B")),
            paramValues = emptyMap(),
        )
        val stepB = WorkflowStep(
            id = "step-B",
            type = "screen",
            screenId = screenBId,
            triggers = listOf(
                WorkflowTrigger(
                    name = "Next",
                    type = WorkflowTriggerType.ON_PRESS,
                    actionId = "action-next",
                    componentId = "btn-next",
                ),
            ),
            triggerActions = mapOf("action-next" to WorkflowTriggerAction.Step(stepId = "step-C")),
            paramValues = emptyMap(),
        )
        val stepC = WorkflowStep(
            id = "step-C",
            type = "screen",
            screenId = screenCId,
            triggers = emptyList(),
            triggerActions = emptyMap(),
            paramValues = emptyMap(),
        )
        val threeStepWorkflow = PublishedWorkflow(
            id = "wfl-three-step",
            displayName = "Three Step",
            initialStepId = "step-A",
            steps = mapOf("step-A" to stepA, "step-B" to stepB, "step-C" to stepC),
            screens = mapOf(screenAId to earlyScreen1, screenBId to earlyScreen2, screenCId to terminalScreen3),
            uiConfig = UiConfig(),
            metadata = emptyMap(),
            singleStepFallbackId = "step-C",
        )
        val threeStepOffering = Offering(
            identifier = threeStepOfferingId,
            serverDescription = "",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly, TestData.Packages.annual),
            paywallComponents = null,
            webCheckoutURL = null,
        )
        val threeStepOfferings = Offerings(threeStepOffering, mapOf(threeStepOfferingId to threeStepOffering))
        val wflResult = WorkflowDataResult(workflow = threeStepWorkflow, enrolledVariants = null)

        val vm = createVm()
        vm.updateStateFromWorkflow(wflResult, threeStepOfferings, null)

        // Navigate step-A → step-B → step-C.
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        vm.onTransitionComplete(vm.workflowState.value!!.pendingTransition!!.id)
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        vm.onTransitionComplete(vm.workflowState.value!!.pendingTransition!!.id)

        // Select ANNUAL on terminal step-C (different from the MONTHLY default the packageless steps use).
        val terminalState = vm.workflowState.value?.stepStates?.get("step-C")!!
        terminalState.update(PackageType.ANNUAL.identifier!!)

        // Back step-C → step-B: context must stay at initial MONTHLY, not change to ANNUAL.
        vm.handleBackNavigation()
        assertThat(vm.workflowState.value?.stepStates?.get("step-B")?.selectedPackageInfo?.rcPackage?.identifier)
            .isEqualTo(PackageType.MONTHLY.identifier)

        // Complete the transition then back step-B → step-A: still MONTHLY, not ANNUAL.
        vm.onTransitionComplete(vm.workflowState.value!!.pendingTransition!!.id)
        vm.handleBackNavigation()
        assertThat(vm.workflowState.value?.stepStates?.get("step-A")?.selectedPackageInfo?.rcPackage?.identifier)
            .isEqualTo(PackageType.MONTHLY.identifier)
    }

    @Test
    fun `own package selection on packaged step is preserved through back-and-forward navigation`() {
        val (result, offerings) = makeContextPackageWorkflow()
        val vm = createVm()
        vm.updateStateFromWorkflow(result, offerings, null)

        // Navigate to terminal step-2 (has packages, default is MONTHLY).
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        vm.onTransitionComplete(vm.workflowState.value!!.pendingTransition!!.id)

        val step2State = vm.workflowState.value?.stepStates?.get("step-2")
        assertThat(step2State).isNotNull()
        assertThat(step2State!!.selectedPackageInfo?.rcPackage?.identifier)
            .isEqualTo(PackageType.MONTHLY.identifier)

        // Select ANNUAL, then navigate back.
        step2State.update(PackageType.ANNUAL.identifier!!)
        vm.handleBackNavigation()

        // Navigate forward to step-2 again — cached selection (ANNUAL) must be preserved.
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        vm.onTransitionComplete(vm.workflowState.value!!.pendingTransition!!.id)

        val step2StateAgain = vm.workflowState.value?.stepStates?.get("step-2")
        assertThat(step2StateAgain?.selectedPackageInfo?.rcPackage?.identifier)
            .isEqualTo(PackageType.ANNUAL.identifier)
    }

    @Test
    fun `singleStepFallbackId pointing to missing step produces no context and does not crash`() {
        val (result, offerings) = makeContextPackageWorkflow()
        val brokenResult = result.copy(
            workflow = result.workflow.copy(singleStepFallbackId = "non-existent-step"),
        )
        val vm = createVm()

        vm.updateStateFromWorkflow(brokenResult, offerings, null)

        val step1State = vm.workflowState.value?.stepStates?.get("step-1")
        assertThat(step1State).isNotNull()
        assertThat(step1State!!.selectedPackageInfo).isNull()
    }

    // endregion

    // region exit offers

    @Test
    fun `preloadExitOffering reads exit offer from last workflow step`() = runTest {
        coEvery { purchases.awaitOfferings() } returns testOfferingsWithExitOffer

        val vm = createVm()
        vm.updateStateFromWorkflow(fetchResultWithExitOffer, testOfferingsWithExitOffer, null)
        vm.preloadExitOffering()
        advanceUntilIdle()

        assertThat(vm.preloadedExitOffering.value?.identifier).isEqualTo(exitOfferingId)
    }

    @Test
    fun `preloadExitOffering with no exit offer on last step does not set preloaded offering`() = runTest {
        val vm = createVm()
        vm.updateStateFromWorkflow(fetchResult, testOfferings, null)
        vm.preloadExitOffering()
        advanceUntilIdle()

        assertThat(vm.preloadedExitOffering.value).isNull()
    }

    @Test
    fun `preloadExitOffering with unknown exit offering id leaves preloaded offering null`() = runTest {
        // testOfferings does NOT include exitOfferingId
        coEvery { purchases.awaitOfferings() } returns testOfferings

        val vm = createVm()
        vm.updateStateFromWorkflow(fetchResultWithExitOffer, testOfferings, null)
        vm.preloadExitOffering()
        advanceUntilIdle()

        assertThat(vm.preloadedExitOffering.value).isNull()
    }

    @Test
    fun `closePaywall with preloaded exit offer calls dismissRequestWithExitOffering`() = runTest {
        coEvery { purchases.awaitOfferings() } returns testOfferingsWithExitOffer

        var receivedExitOffering: Offering? = null
        val vm = createVm(
            dismissRequestWithExitOffering = { offering, _ ->
                receivedExitOffering = offering
            },
        )
        vm.updateStateFromWorkflow(fetchResultWithExitOffer, testOfferingsWithExitOffer, null)
        vm.preloadExitOffering()
        advanceUntilIdle()

        // Confirm the offering was preloaded before testing the close behaviour.
        assertThat(vm.preloadedExitOffering.value?.identifier).isEqualTo(exitOfferingId)

        vm.closePaywall()

        assertThat(receivedExitOffering?.identifier).isEqualTo(exitOfferingId)
    }

    @Test
    fun `preloadExitOffering called before workflow data arrives still preloads once data is set`() = runTest {
        // Mirrors PaywallActivity's LaunchedEffect ordering: preloadExitOffering() fires before
        // the async workflow fetch has populated currentWorkflowResult.
        coEvery { purchases.awaitOfferings() } returns testOfferingsWithExitOffer

        val vm = createVm()
        vm.preloadExitOffering()
        vm.updateStateFromWorkflow(fetchResultWithExitOffer, testOfferingsWithExitOffer, null)
        advanceUntilIdle()

        assertThat(vm.preloadedExitOffering.value?.identifier).isEqualTo(exitOfferingId)
    }

    @Test
    fun `preloadExitOffering uses singleStepFallbackId to locate exit offer`() = runTest {
        coEvery { purchases.awaitOfferings() } returns testOfferingsWithExitOffer

        val vm = createVm()
        vm.updateStateFromWorkflow(fetchResultWithFallback, testOfferingsWithExitOffer, null)
        vm.preloadExitOffering()
        advanceUntilIdle()

        assertThat(vm.preloadedExitOffering.value?.identifier).isEqualTo(exitOfferingId)
    }

    @Test
    fun `preloadExitOffering reads exit offer from single-step workflow`() = runTest {
        coEvery { purchases.awaitOfferings() } returns testOfferingsWithExitOffer

        val vm = createVm()
        vm.updateStateFromWorkflow(singleStepFetchResultWithExitOffer, testOfferingsWithExitOffer, null)
        vm.preloadExitOffering()
        advanceUntilIdle()

        assertThat(vm.preloadedExitOffering.value?.identifier).isEqualTo(exitOfferingId)
    }

    // endregion exit offers
}
