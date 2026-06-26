@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.data

import android.app.Activity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PurchaseResult
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.WorkflowDataResult
import com.revenuecat.purchases.common.workflows.WorkflowScreen
import com.revenuecat.purchases.common.workflows.WorkflowScreenType
import com.revenuecat.purchases.common.workflows.WorkflowStep
import com.revenuecat.purchases.common.workflows.WorkflowTrigger
import com.revenuecat.purchases.common.workflows.WorkflowTriggerAction
import com.revenuecat.purchases.common.workflows.WorkflowTriggerType
import com.revenuecat.purchases.common.events.FeatureEvent
import com.revenuecat.purchases.common.workflows.events.WorkflowEvent
import com.revenuecat.purchases.paywalls.events.ExitOfferType
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.paywalls.events.PaywallEventType
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
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.PaywallPurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.PaywallPurchaseLogicParams
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicResult
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
import com.revenuecat.purchases.ui.revenuecatui.utils.Resumable
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
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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

    private fun screenTypeMetadata(vararg types: String): JsonObject =
        JsonObject(mapOf(WorkflowScreenType.METADATA_KEY to JsonArray(types.map { JsonPrimitive(it) })))

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
            // Context (non-paywall) step: tagged with an empty screen_type so it suppresses paywall events.
            metadata = screenTypeMetadata(),
        )
        val terminalStep = WorkflowStep(
            id = "step-2",
            type = "screen",
            screenId = screenId2,
            triggers = emptyList(),
            triggerActions = emptyMap(),
            paramValues = emptyMap(),
            // Paywall step: tagged so it reports paywall events.
            metadata = screenTypeMetadata(WorkflowScreenType.PAYWALL),
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
        singleStepFallbackId = "step-1",
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
        singleStepFallbackId = "step-2",
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
        singleStepFallbackId = "step-only",
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

    // An offering with no packages — validateStep passes but calculateState returns PaywallState.Error.
    private val emptyOfferingId = "empty_offering"
    private val emptyOffering = Offering(
        identifier = emptyOfferingId,
        serverDescription = "",
        metadata = emptyMap(),
        availablePackages = emptyList(),
        paywallComponents = null,
        webCheckoutURL = null,
    )
    private val testOfferingsWithEmpty = Offerings(
        testOffering,
        mapOf(offeringId to testOffering, emptyOfferingId to emptyOffering),
    )

    private val screenEmptyId = "screen-empty"
    private val screenWithEmptyOffering = WorkflowScreen(
        name = "screen-empty",
        templateName = "template_v2",
        revision = 1,
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = componentsConfig,
        componentsLocalizations = localizations,
        defaultLocaleIdentifier = defaultLocaleId,
        offeringIdentifier = emptyOfferingId,
    )

    private val step3EmptyOffering = WorkflowStep(
        id = "step-3",
        type = "screen",
        screenId = screenEmptyId,
        triggers = emptyList(),
        triggerActions = emptyMap(),
    )

    private val step1ToStep3 = WorkflowStep(
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
        triggerActions = mapOf("action-next" to WorkflowTriggerAction.Step(stepId = "step-3")),
    )

    private val screenWithEmptyOffering2 = WorkflowScreen(
        name = "screen-empty-initial",
        templateName = "template_v2",
        revision = 1,
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = componentsConfig,
        componentsLocalizations = localizations,
        defaultLocaleIdentifier = defaultLocaleId,
        offeringIdentifier = emptyOfferingId,
    )

    private val step1WithEmptyOffering = WorkflowStep(
        id = "step-1",
        type = "screen",
        screenId = "screen-empty-initial",
        triggers = emptyList(),
        triggerActions = emptyMap(),
    )

    private val workflowFailingInitial = PublishedWorkflow(
        id = "wfl-failing",
        displayName = "Failing",
        initialStepId = "step-1",
        steps = mapOf("step-1" to step1WithEmptyOffering),
        screens = mapOf("screen-empty-initial" to screenWithEmptyOffering2),
        uiConfig = UiConfig(),
        metadata = emptyMap(),
    )
    private val fetchResultFailingInitial = WorkflowDataResult(workflow = workflowFailingInitial, enrolledVariants = null)

    private val workflowToError = PublishedWorkflow(
        id = "wfl-to-error",
        displayName = "Test",
        initialStepId = "step-1",
        steps = mapOf("step-1" to step1ToStep3, "step-3" to step3EmptyOffering),
        screens = mapOf(screenId1 to makeScreen(screenId1), screenEmptyId to screenWithEmptyOffering),
        uiConfig = UiConfig(),
        metadata = emptyMap(),
    )
    private val fetchResultToError = WorkflowDataResult(workflow = workflowToError, enrolledVariants = null)


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
        listener: PaywallListener? = null,
    ): PaywallViewModelImpl {
        val builder = PaywallOptions.Builder(dismissRequest = {})
        dismissRequestWithExitOffering?.let { builder.setDismissRequestWithExitOffering(it) }
        listener?.let { builder.setListener(it) }
        return PaywallViewModelImpl(
            resourceProvider = MockResourceProvider(),
            purchases = purchases,
            options = builder.build(),
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false,
            shouldDisplayBlock = null,
            // Run the step-state pre-warm on the test scheduler so advanceUntilIdle() awaits it
            // instead of it escaping to Dispatchers.Default and resuming after resetMain().
            backgroundDispatcher = testDispatcher,
        )
    }

    private fun makeListener(): PaywallListener = mockk {
        every { onPurchasePackageInitiated(any(), any()) } answers {
            val resume = secondArg<Resumable>()
            resume(true)
        }
        every { onRestoreInitiated(any()) } answers {
            val resume = firstArg<Resumable>()
            resume(true)
        }
        every { onPurchaseStarted(any()) } just runs
        every { onPurchaseCompleted(any(), any()) } just runs
        every { onPurchaseCancelled() } just runs
        every { onPurchaseError(any()) } just runs
        every { onRestoreStarted() } just runs
        every { onRestoreCompleted(any()) } just runs
        every { onRestoreError(any()) } just runs
    }

    // region forward navigation

    @Test
    fun `forward navigation sets FORWARD on pendingTransition`() {
        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)

        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)

        assertThat(vm.workflowState.value?.pendingTransition?.direction)
            .isEqualTo(NavigationDirection.FORWARD)
    }

    @Test
    fun `forward navigation sets fromStepId to the step navigated away from`() {
        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)

        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)

        assertThat(vm.workflowState.value?.pendingTransition?.fromStepId).isEqualTo("step-1")
    }

    // endregion

    // region back navigation

    @Test
    fun `back navigation sets BACKWARD on pendingTransition`() {
        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        vm.onTransitionComplete(vm.workflowState.value!!.pendingTransition!!.id)

        vm.handleBackNavigation()

        assertThat(vm.workflowState.value?.pendingTransition?.direction)
            .isEqualTo(NavigationDirection.BACKWARD)
    }

    @Test
    fun `back navigation sets fromStepId to the step navigated away from`() {
        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)

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
        vm.startWorkflowPresentationFromResult(fetchResult2, offerings2, null)

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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)

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
            // Run the step-state pre-warm on the test scheduler so advanceUntilIdle() awaits it
            // instead of it escaping to Dispatchers.Default and resuming after resetMain().
            backgroundDispatcher = testDispatcher,
        )
        vmWithVars.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)

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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        val id = vm.workflowState.value!!.pendingTransition!!.id

        vm.onTransitionComplete(id)

        assertThat(vm.workflowState.value?.pendingTransition).isNull()
    }

    @Test
    fun `onTransitionComplete with stale id does not clear a newer transition`() {
        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
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

        vm.startWorkflowPresentationFromResult(result, offerings, null)

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
        vm.startWorkflowPresentationFromResult(result, offerings, null)

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
        vm.startWorkflowPresentationFromResult(wflResult, threeStepOfferings, null)

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
        vm.startWorkflowPresentationFromResult(result, offerings, null)

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

        vm.startWorkflowPresentationFromResult(brokenResult, offerings, null)

        val step1State = vm.workflowState.value?.stepStates?.get("step-1")
        assertThat(step1State).isNotNull()
        assertThat(step1State!!.selectedPackageInfo).isNull()
    }

    @Test
    fun `singleStepFallbackId equal to initialStepId does not crash and own selection is used`() {
        // Edge case: the fallback step IS the initial step — no earlier packageless steps exist,
        // so the pre-computation guard skips re-building the step. The step's own selection
        // (ownSelection) must still win over the self-referential defaultPackageInfo.
        val (twoPackageResult, twoPackageOfferings) = makeTwoPackageWorkflow()
        val singleFallbackResult = twoPackageResult.copy(
            workflow = twoPackageResult.workflow.copy(singleStepFallbackId = "step-1"),
        )
        val vm = createVm()

        vm.startWorkflowPresentationFromResult(singleFallbackResult, twoPackageOfferings, null)

        val step1State = vm.workflowState.value?.stepStates?.get("step-1")
        assertThat(step1State).isNotNull()
        // monthlyPackageComponentDefault has isSelectedByDefault = true → MONTHLY wins.
        assertThat(step1State!!.selectedPackageInfo?.rcPackage?.identifier)
            .isEqualTo(PackageType.MONTHLY.identifier)
    }

    // endregion

    // region exit offers

    @Test
    fun `preloadExitOffering reads exit offer from singleStepFallbackId step`() = runTest {
        coEvery { purchases.awaitOfferings() } returns testOfferingsWithExitOffer

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResultWithExitOffer, testOfferingsWithExitOffer, null)
        vm.preloadExitOffering()
        advanceUntilIdle()

        assertThat(vm.preloadedExitOffering?.identifier).isEqualTo(exitOfferingId)
    }

    @Test
    fun `preloadExitOffering without singleStepFallbackId does not set preloaded offering`() = runTest {
        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        vm.preloadExitOffering()
        advanceUntilIdle()

        assertThat(vm.preloadedExitOffering).isNull()
    }

    @Test
    fun `preloadExitOffering with unknown exit offering id leaves preloaded offering null`() = runTest {
        // testOfferings does NOT include exitOfferingId
        coEvery { purchases.awaitOfferings() } returns testOfferings

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResultWithExitOffer, testOfferings, null)
        vm.preloadExitOffering()
        advanceUntilIdle()

        assertThat(vm.preloadedExitOffering).isNull()
    }

    @Test
    fun `preloadExitOffering not-found result is not re-attempted when same offerings are re-set`() = runTest {
        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResultWithExitOffer, testOfferings, null)
        vm.preloadExitOffering()
        assertThat(vm.preloadedExitOffering).isNull()

        // Simulate locale/colour/options refresh pushing the same workflow data again.
        vm.startWorkflowPresentationFromResult(fetchResultWithExitOffer, testOfferings, null)

        // Still null — carry-forward prevented a redundant lookup and a duplicate error log.
        assertThat(vm.preloadedExitOffering).isNull()
    }

    @Test
    fun `preloadExitOffering re-resolves when offerings are refreshed and now contain the exit offering`() = runTest {
        val vm = createVm()
        // First update: exit offering absent.
        vm.startWorkflowPresentationFromResult(fetchResultWithExitOffer, testOfferings, null)
        vm.preloadExitOffering()
        assertThat(vm.preloadedExitOffering).isNull()

        // Second update: fresh offerings that now include the exit offering.
        vm.startWorkflowPresentationFromResult(fetchResultWithExitOffer, testOfferingsWithExitOffer, null)

        assertThat(vm.preloadedExitOffering?.identifier).isEqualTo(exitOfferingId)
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
        vm.startWorkflowPresentationFromResult(fetchResultWithExitOffer, testOfferingsWithExitOffer, null)
        vm.preloadExitOffering()
        advanceUntilIdle()
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)

        // Confirm the offering was preloaded before testing the close behaviour.
        assertThat(vm.preloadedExitOffering?.identifier).isEqualTo(exitOfferingId)

        vm.closePaywall()

        assertThat(receivedExitOffering?.identifier).isEqualTo(exitOfferingId)
    }

    @Test
    fun `closePaywall before exit offer workflow step does not call dismissRequestWithExitOffering with offering`() = runTest {
        coEvery { purchases.awaitOfferings() } returns testOfferingsWithExitOffer

        var receivedExitOffering: Offering? = exitOffering
        val vm = createVm(
            dismissRequestWithExitOffering = { offering, _ ->
                receivedExitOffering = offering
            },
        )
        vm.startWorkflowPresentationFromResult(fetchResultWithExitOffer, testOfferingsWithExitOffer, null)
        vm.preloadExitOffering()
        advanceUntilIdle()

        // The exit offer is preloaded, but the current workflow step is still step-1.
        assertThat(vm.preloadedExitOffering?.identifier).isEqualTo(exitOfferingId)

        vm.closePaywall()

        assertThat(receivedExitOffering).isNull()
    }

    @Test
    fun `preloadExitOffering called before workflow data arrives still preloads once data is set`() = runTest {
        // Mirrors PaywallActivity's LaunchedEffect ordering: preloadExitOffering() fires before
        // the async workflow fetch has populated currentWorkflowResult.
        coEvery { purchases.awaitOfferings() } returns testOfferingsWithExitOffer

        val vm = createVm()
        vm.preloadExitOffering()
        vm.startWorkflowPresentationFromResult(fetchResultWithExitOffer, testOfferingsWithExitOffer, null)
        advanceUntilIdle()

        assertThat(vm.preloadedExitOffering?.identifier).isEqualTo(exitOfferingId)
    }

    @Test
    fun `startWorkflowPresentationFromResult with same workflow data reloads preloaded exit offering`() = runTest {
        val vm = createVm()
        vm.preloadExitOffering()
        vm.startWorkflowPresentationFromResult(fetchResultWithExitOffer, testOfferingsWithExitOffer, null)
        assertThat(vm.preloadedExitOffering?.identifier).isEqualTo(exitOfferingId)

        vm.startWorkflowPresentationFromResult(fetchResultWithExitOffer, testOfferingsWithExitOffer, null)
        assertThat(vm.preloadedExitOffering?.identifier).isEqualTo(exitOfferingId)
    }

    @Test
    fun `preloadExitOffering uses singleStepFallbackId to locate exit offer`() = runTest {
        coEvery { purchases.awaitOfferings() } returns testOfferingsWithExitOffer

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResultWithFallback, testOfferingsWithExitOffer, null)
        vm.preloadExitOffering()
        advanceUntilIdle()

        assertThat(vm.preloadedExitOffering?.identifier).isEqualTo(exitOfferingId)
    }

    @Test
    fun `preloadExitOffering reads exit offer from single-step workflow`() = runTest {
        coEvery { purchases.awaitOfferings() } returns testOfferingsWithExitOffer

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(singleStepFetchResultWithExitOffer, testOfferingsWithExitOffer, null)
        vm.preloadExitOffering()
        advanceUntilIdle()

        assertThat(vm.preloadedExitOffering?.identifier).isEqualTo(exitOfferingId)
    }

    // endregion exit offers

    // region event tracking

    @Test
    fun `workflow load tracks StepStarted with start entryReason`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers {
            captured.add(firstArg())
        }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)

        val started = captured.filterIsInstance<WorkflowEvent.StepStarted>()
        assertThat(started).hasSize(1)
        assertThat(started.first().workflowId).isEqualTo("wfl-test")
        assertThat(started.first().stepId).isEqualTo("step-1")
        assertThat(started.first().entryReason).isEqualTo("start")
        assertThat(started.first().fromStepId).isNull()
        assertThat(started.first().isFirstStep).isTrue
        assertThat(started.first().isLastStep).isFalse
        assertThat(started.first().traceId).isNotEmpty()
    }

    @Test
    fun `color scheme rebuild during workflow does not track another StepStarted`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        val initialTraceId = captured.filterIsInstance<WorkflowEvent.StepStarted>().single().traceId
        captured.clear()

        vm.refreshStateIfColorsChanged(
            colorScheme = TestData.Constants.currentColorScheme.copy(primary = Color.Black),
            isDark = true,
        )

        assertThat(captured.filterIsInstance<WorkflowEvent>()).isEmpty()
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")

        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)

        val navigationTraceIds = captured.filterIsInstance<WorkflowEvent>().map { it.traceId }.distinct()
        assertThat(navigationTraceIds).isEqualTo(listOf(initialTraceId))
    }

    @Test
    fun `failed initial step after successful fallback cache build does not complete fallback step`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }
        val workflowWithFailingInitialAndSuccessfulFallback = PublishedWorkflow(
            id = "wfl-failing-with-fallback",
            displayName = "Failing with fallback",
            initialStepId = "step-1",
            steps = mapOf("step-1" to step1WithEmptyOffering, "step-2" to step2),
            screens = mapOf(
                "screen-empty-initial" to screenWithEmptyOffering2,
                screenId2 to makeScreen(screenId2),
            ),
            uiConfig = UiConfig(),
            metadata = emptyMap(),
            singleStepFallbackId = "step-2",
        )

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(
            WorkflowDataResult(workflowWithFailingInitialAndSuccessfulFallback, enrolledVariants = null),
            testOfferingsWithEmpty,
            null,
        )

        assertThat(captured.filterIsInstance<WorkflowEvent>()).isEmpty()
        assertThat(vm.workflowState.value).isNull()
        assertThat(vm.state.value).isInstanceOf(PaywallState.Error::class.java)
    }

    @Test
    fun `forward navigation fires StepCompleted for current step and StepStarted for next`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        captured.clear()

        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)

        val workflowEvents = captured.filterIsInstance<WorkflowEvent>()
        // Expect: StepCompleted(step-1, to=step-2), StepStarted(step-2, forward)
        assertThat(workflowEvents).hasSize(2)
        val completed = workflowEvents[0] as WorkflowEvent.StepCompleted
        assertThat(completed.stepId).isEqualTo("step-1")
        assertThat(completed.toStepId).isEqualTo("step-2")
        assertThat(completed.isFirstStep).isTrue
        assertThat(completed.isLastStep).isFalse
        val nextStarted = workflowEvents[1] as WorkflowEvent.StepStarted
        assertThat(nextStarted.stepId).isEqualTo("step-2")
        assertThat(nextStarted.entryReason).isEqualTo("forward")
        assertThat(nextStarted.fromStepId).isEqualTo("step-1")
        assertThat(nextStarted.isFirstStep).isFalse
        assertThat(nextStarted.isLastStep).isTrue
        assertThat(completed.traceId).isEqualTo(nextStarted.traceId)
    }

    @Test
    fun `back navigation fires StepCompleted for current step and StepStarted for previous with back reason`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        vm.onTransitionComplete(vm.workflowState.value!!.pendingTransition!!.id)
        captured.clear() // clear load + forward nav events

        vm.handleBackNavigation()

        val workflowEvents = captured.filterIsInstance<WorkflowEvent>()
        assertThat(workflowEvents).hasSize(2)
        val completed = workflowEvents[0] as WorkflowEvent.StepCompleted
        assertThat(completed.stepId).isEqualTo("step-2")
        assertThat(completed.toStepId).isEqualTo("step-1")
        assertThat(completed.isFirstStep).isFalse
        assertThat(completed.isLastStep).isTrue
        val started = workflowEvents[1] as WorkflowEvent.StepStarted
        assertThat(started.stepId).isEqualTo("step-1")
        assertThat(started.entryReason).isEqualTo("back")
        assertThat(started.fromStepId).isEqualTo("step-2")
        assertThat(started.isFirstStep).isTrue
        assertThat(started.isLastStep).isFalse
    }

    @Test
    fun `closePaywall during workflow fires StepCompleted with null toStepId`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        captured.clear() // clear load event

        vm.closePaywall(result = null)

        val completed = captured.filterIsInstance<WorkflowEvent.StepCompleted>().single()
        assertThat(completed.stepId).isEqualTo("step-1")
        assertThat(completed.toStepId).isNull()
    }

    @Test
    fun `closePaywall without a purchase fires workflows Close for the current step`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        captured.clear() // clear load event

        vm.closePaywall(result = null)

        val close = captured.filterIsInstance<WorkflowEvent.Close>().single()
        assertThat(close.workflowId).isEqualTo(workflow.id)
        assertThat(close.stepId).isEqualTo("step-1")
        assertThat(close.isFirstStep).isTrue
        // step-1 has a trigger to step-2, so it is not the terminal step.
        assertThat(close.isLastStep).isFalse
    }

    @Test
    fun `closePaywall after navigating forward fires workflows Close for the non-initial step`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        // Navigate step-1 → step-2 (the terminal step) and settle the transition.
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        vm.onTransitionComplete(vm.workflowState.value!!.pendingTransition!!.id)
        captured.clear() // clear load + forward nav events

        vm.closePaywall(result = null)

        val close = captured.filterIsInstance<WorkflowEvent.Close>().single()
        assertThat(close.workflowId).isEqualTo(workflow.id)
        assertThat(close.stepId).isEqualTo("step-2")
        // step-2 is reached via a trigger and has none of its own, so it is the last (terminal) step.
        assertThat(close.isFirstStep).isFalse
        assertThat(close.isLastStep).isTrue
    }

    @Test
    fun `closePaywall fires Close even on a non-paywall step that suppresses paywall events`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }
        // step-1 is a context (non-paywall) step: paywall_close is suppressed, but workflows_close
        // is a workflow-level abandonment signal and must still fire.
        val (result, offerings) = makeContextPackageWorkflow()

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(result, offerings, null)
        captured.clear()

        vm.closePaywall(result = null)

        assertThat(captured.filterIsInstance<PaywallEvent>().filter { it.type == PaywallEventType.CLOSE })
            .isEmpty()
        val close = captured.filterIsInstance<WorkflowEvent.Close>().single()
        assertThat(close.stepId).isEqualTo("step-1")
    }

    @Test
    fun `closePaywall clears workflowState`() {
        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        assertThat(vm.workflowState.value).isNotNull()

        vm.closePaywall(result = null)

        assertThat(vm.workflowState.value).isNull()
    }

    @Test
    fun `impression after closePaywall reuse does not carry stale workflowId`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        vm.trackPaywallImpressionIfNeeded()
        vm.closePaywall(result = null)
        captured.clear()

        vm.trackPaywallImpressionIfNeeded()

        val impressions = captured.filterIsInstance<PaywallEvent>()
            .filter { it.type == PaywallEventType.IMPRESSION }
        assertThat(impressions).isNotEmpty()
        assertThat(impressions.first().data.workflowId).isNull()
    }

    @Test
    fun `impression after closePaywall from non-paywall step is not suppressed`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }
        val (result, offerings) = makeContextPackageWorkflow()

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(result, offerings, null)
        // step-1 is a context (non-paywall) step, so currentWorkflowStepTracksPaywallEvents = false
        vm.trackPaywallImpressionIfNeeded()
        assertThat(captured.filterIsInstance<PaywallEvent>()).isEmpty()

        vm.closePaywall(result = null)
        captured.clear()

        // After close, simulate the VM being reused: navigate to step-2 (paywall step) via a new
        // workflow presentation and verify impression is tracked normally (not suppressed by stale state).
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        vm.trackPaywallImpressionIfNeeded()

        val impressions = captured.filterIsInstance<PaywallEvent>()
            .filter { it.type == PaywallEventType.IMPRESSION }
        assertThat(impressions).isNotEmpty()
    }

    @Test
    fun `RevenueCat purchase completion during workflow fires StepCompleted with null toStepId`() = runTest {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }
        coEvery { purchases.awaitPurchase(any()) } returns PurchaseResult(
            storeTransaction = mockk<StoreTransaction>(),
            customerInfo = mockk<CustomerInfo>(),
        )

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        advanceUntilIdle()
        captured.clear()

        vm.handlePackagePurchase(activity = mockk<Activity>(), pkg = TestData.Packages.monthly)

        val workflowEvents = captured.filterIsInstance<WorkflowEvent>()
        val completed = workflowEvents.filterIsInstance<WorkflowEvent.StepCompleted>()
        assertThat(completed).hasSize(1)
        assertThat(completed[0].stepId).isEqualTo("step-1")
        assertThat(completed[0].toStepId).isNull()
    }

    @Test
    fun `closePaywall after a completed purchase does not fire workflows Close`() = runTest {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }
        coEvery { purchases.awaitPurchase(any()) } returns PurchaseResult(
            storeTransaction = mockk<StoreTransaction>(),
            customerInfo = mockk<CustomerInfo>(),
        )

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        advanceUntilIdle()
        vm.handlePackagePurchase(activity = mockk<Activity>(), pkg = TestData.Packages.monthly)
        captured.clear()

        // A close after the purchase completed is not an abandonment, so no workflows_close.
        vm.closePaywall(result = null)

        assertThat(captured.filterIsInstance<WorkflowEvent.Close>()).isEmpty()
    }

    @Test
    fun `closePaywall after a successful restore does not fire workflows Close`() = runTest {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }
        coEvery { purchases.awaitRestore() } returns mockk<CustomerInfo>()

        // shouldDisplayBlock is null in createVm, so a successful REVENUECAT restore does NOT auto-dismiss
        // and does NOT set purchaseCompleted: the paywall stays up and the user dismisses manually.
        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        advanceUntilIdle()
        vm.handleRestorePurchases()
        advanceUntilIdle()
        captured.clear()

        // Premise: the purchase-completed gate alone would not have suppressed Close here.
        assertThat(vm.purchaseCompleted.value).isFalse

        // A successful restore is a natural exit, not an abandonment, so no workflows_close.
        vm.closePaywall(result = null)

        assertThat(captured.filterIsInstance<WorkflowEvent.Close>()).isEmpty()
        // The workflow was still live (StepCompleted fires on close), so Close was specifically
        // suppressed by the restore flag, not because there was no current step to report.
        assertThat(captured.filterIsInstance<WorkflowEvent.StepCompleted>()).hasSize(1)
    }

    @Test
    fun `closePaywall after restore then a re-presentation refresh does not fire workflows Close`() = runTest {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }
        coEvery { purchases.awaitRestore() } returns mockk<CustomerInfo>()

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        advanceUntilIdle()
        // Successful restore with shouldDisplayBlock null: the paywall stays open, completion recorded.
        vm.handleRestorePurchases()
        advanceUntilIdle()

        // A refresh re-presents the SAME open session (e.g. updateOptions -> presentWorkflow ->
        // startWorkflowPresentation). The user has NOT dismissed, so the restore completion must survive
        // and still suppress workflows_close on the eventual manual dismiss.
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        advanceUntilIdle()
        captured.clear()

        vm.closePaywall(result = null)

        assertThat(captured.filterIsInstance<WorkflowEvent.Close>()).isEmpty()
    }

    @Test
    fun `closePaywall on a new session after an earlier purchase-dismiss still fires workflows Close`() = runTest {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }
        every { purchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.MY_APP
        coEvery { purchases.awaitSyncPurchases() } returns mockk<CustomerInfo> {
            every { activeSubscriptions } returns setOf()
            every { nonSubscriptionTransactions } returns listOf()
        }
        val myAppPurchaseLogic = object : PaywallPurchaseLogic {
            override suspend fun performPurchase(activity: Activity, params: PaywallPurchaseLogicParams) =
                PurchaseLogicResult.Success
            override suspend fun performRestore(customerInfo: CustomerInfo) = PurchaseLogicResult.Success
        }

        val vm = PaywallViewModelImpl(
            resourceProvider = MockResourceProvider(),
            purchases = purchases,
            options = PaywallOptions.Builder(dismissRequest = {}).setPurchaseLogic(myAppPurchaseLogic).build(),
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false,
            shouldDisplayBlock = null,
            backgroundDispatcher = testDispatcher,
        )
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        advanceUntilIdle()
        // MY_APP purchase completes and auto-dismisses the paywall (closePaywall -> clearWorkflowState),
        // which ends the session.
        vm.handlePackagePurchase(activity = mockk<Activity>(), pkg = TestData.Packages.monthly)
        advanceUntilIdle()

        // The same ViewModel later presents a NEW workflow session. _purchaseCompleted stays true (sticky),
        // but this session had no purchase, so abandoning it must still emit workflows_close.
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        advanceUntilIdle()
        captured.clear()

        vm.closePaywall(result = null)

        val close = captured.filterIsInstance<WorkflowEvent.Close>().single()
        assertThat(close.stepId).isEqualTo("step-1")
    }

    @Test
    fun `closePaywall on a new session after a REVENUECAT purchase-dismiss still fires workflows Close`() = runTest {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }
        coEvery { purchases.awaitPurchase(any()) } returns PurchaseResult(
            storeTransaction = mockk<StoreTransaction>(),
            customerInfo = mockk<CustomerInfo>(),
        )

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        advanceUntilIdle()
        // REVENUECAT purchase completes and dismisses via options.dismissRequest() (not closePaywall),
        // which ends the session on an embedded ViewModel that is not destroyed.
        vm.handlePackagePurchase(activity = mockk<Activity>(), pkg = TestData.Packages.monthly)
        advanceUntilIdle()

        // The same ViewModel later presents a NEW workflow session with no purchase, so abandoning it
        // must still emit workflows_close (the completion must not stick past the dismiss).
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        advanceUntilIdle()
        captured.clear()

        vm.closePaywall(result = null)

        val close = captured.filterIsInstance<WorkflowEvent.Close>().single()
        assertThat(close.stepId).isEqualTo("step-1")
    }

    @Test
    fun `MY_APP purchase success during workflow fires StepCompleted with null toStepId`() = runTest {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }
        every { purchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.MY_APP
        coEvery { purchases.awaitSyncPurchases() } returns mockk<CustomerInfo> {
            every { activeSubscriptions } returns setOf()
            every { nonSubscriptionTransactions } returns listOf()
        }

        val myAppPurchaseLogic = object : PaywallPurchaseLogic {
            override suspend fun performPurchase(
                activity: Activity,
                params: PaywallPurchaseLogicParams,
            ): PurchaseLogicResult = PurchaseLogicResult.Success

            override suspend fun performRestore(customerInfo: CustomerInfo): PurchaseLogicResult =
                PurchaseLogicResult.Success
        }

        val vm = PaywallViewModelImpl(
            resourceProvider = MockResourceProvider(),
            purchases = purchases,
            options = PaywallOptions.Builder(dismissRequest = {})
                .setPurchaseLogic(myAppPurchaseLogic)
                .build(),
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false,
            shouldDisplayBlock = null,
            backgroundDispatcher = testDispatcher,
        )
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        advanceUntilIdle()
        captured.clear()

        vm.handlePackagePurchase(activity = mockk<Activity>(), pkg = TestData.Packages.monthly)

        val workflowEvents = captured.filterIsInstance<WorkflowEvent>()
        val completed = workflowEvents.filterIsInstance<WorkflowEvent.StepCompleted>()
        assertThat(completed).hasSize(1)
        assertThat(completed[0].stepId).isEqualTo("step-1")
        assertThat(completed[0].toStepId).isNull()
    }

    @Test
    fun `RevenueCat restore dismiss during workflow fires StepCompleted with null toStepId`() = runTest {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }
        coEvery { purchases.awaitRestore() } returns mockk<CustomerInfo>()

        val vm = PaywallViewModelImpl(
            resourceProvider = MockResourceProvider(),
            purchases = purchases,
            options = PaywallOptions.Builder(dismissRequest = {}).build(),
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false,
            shouldDisplayBlock = { false },
            backgroundDispatcher = testDispatcher,
        )
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        advanceUntilIdle()
        captured.clear()

        vm.handleRestorePurchases()
        advanceUntilIdle()

        val completed = captured.filterIsInstance<WorkflowEvent.StepCompleted>()
        assertThat(completed).hasSize(1)
        assertThat(completed[0].stepId).isEqualTo("step-1")
        assertThat(completed[0].toStepId).isNull()
    }

    @Test
    fun `failed initial workflow load does not fire StepStarted`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResultFailingInitial, testOfferingsWithEmpty, null)

        val started = captured.filterIsInstance<WorkflowEvent.StepStarted>()
        assertThat(started).isEmpty()
    }

    @Test
    fun `error during navigation clears workflow state and fires StepCompleted with null toStepId`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResultToError, testOfferingsWithEmpty, null)
        captured.clear() // clear load event

        // Navigate to step-3 — computeStateForStep returns Error since its offering has no packages
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)

        val workflowEvents = captured.filterIsInstance<WorkflowEvent>()
        // StepCompleted for step-1 must be fired when _workflowState is cleared due to error
        val completed = workflowEvents.filterIsInstance<WorkflowEvent.StepCompleted>()
        assertThat(completed).hasSize(1)
        assertThat(completed[0].stepId).isEqualTo("step-1")
        assertThat(completed[0].toStepId).isNull()
    }

    @Test
    fun `all events within one impression share the same traceId, new impression gets a new one`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)

        val firstImpressionTraceId = captured.filterIsInstance<WorkflowEvent>().map { it.traceId }.distinct().single()

        captured.clear()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)

        // The abandoned step's StepCompleted carries the old trace ID; the new StepStarted gets a fresh one.
        val secondImpressionEvents = captured.filterIsInstance<WorkflowEvent>()
        val abandoned = secondImpressionEvents.filterIsInstance<WorkflowEvent.StepCompleted>().single()
        assertThat(abandoned.traceId).isEqualTo(firstImpressionTraceId)
        val newStarted = secondImpressionEvents.filterIsInstance<WorkflowEvent.StepStarted>().single()
        assertThat(newStarted.traceId).isNotEqualTo(firstImpressionTraceId)
    }

    @Test
    fun `workflows Close carries the same traceId as the StepStarted from the same impression`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        val startedTraceId = captured.filterIsInstance<WorkflowEvent.StepStarted>().single().traceId

        vm.closePaywall(result = null)

        // The abandonment Close belongs to the same impression, so it shares the load's trace ID.
        val close = captured.filterIsInstance<WorkflowEvent.Close>().single()
        assertThat(close.traceId).isEqualTo(startedTraceId)
        // And the whole impression (StepStarted, StepCompleted, Close) shares one trace ID.
        assertThat(captured.filterIsInstance<WorkflowEvent>().map { it.traceId }.distinct())
            .containsExactly(startedTraceId)
    }

    @Test
    fun `re-presenting workflow while on step-2 fires StepCompleted for the abandoned step`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        captured.clear()

        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)

        val workflowEvents = captured.filterIsInstance<WorkflowEvent>()
        val completed = workflowEvents.filterIsInstance<WorkflowEvent.StepCompleted>().single()
        assertThat(completed.stepId).isEqualTo("step-2")
        assertThat(completed.toStepId).isNull()
        val started = workflowEvents.filterIsInstance<WorkflowEvent.StepStarted>().single()
        assertThat(started.stepId).isEqualTo("step-1")
    }

    @Test
    fun `first workflow presentation with no prior step does not fire a spurious StepCompleted`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)

        assertThat(captured.filterIsInstance<WorkflowEvent.StepCompleted>()).isEmpty()
    }

    @Test
    fun `workflow impression event carries workflowId from loaded workflow`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)
        vm.trackPaywallImpressionIfNeeded()

        val impressions = captured.filterIsInstance<PaywallEvent>()
            .filter { it.type == PaywallEventType.IMPRESSION }
        assertThat(impressions).isNotEmpty()
        assertThat(impressions.first().data.workflowId).isEqualTo(fetchResult.workflow.id)
    }

    @Test
    fun `purchase initiated after same paywall workflow re-presentation carries current step metadata`() = runTest {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }
        coEvery { purchases.awaitPurchase(any()) } returns PurchaseResult(
            storeTransaction = mockk<StoreTransaction>(),
            customerInfo = mockk(relaxed = true),
        )
        val vm = createVm()

        // Both steps render the same screen, so the visual presentation fingerprint is identical
        // across the re-presentation and the impression is de-duped. Only the workflow step id
        // changes, which is exactly what withCurrentWorkflowMetadata must refresh on the
        // already-tracked presentation data.
        val sharedScreenSteps = mapOf(
            "step-1" to step1.copy(screenId = screenId2),
            "step-2" to step2.copy(screenId = screenId2),
        )
        val step1Result = WorkflowDataResult(
            workflow = workflow.copy(
                initialStepId = "step-1",
                singleStepFallbackId = "step-1",
                steps = sharedScreenSteps,
            ),
            enrolledVariants = null,
        )
        val step2Result = WorkflowDataResult(
            workflow = workflow.copy(
                initialStepId = "step-2",
                singleStepFallbackId = "step-2",
                steps = sharedScreenSteps,
            ),
            enrolledVariants = null,
        )

        vm.startWorkflowPresentationFromResult(step1Result, testOfferings, null)
        vm.trackPaywallImpressionIfNeeded()
        captured.clear()

        vm.startWorkflowPresentationFromResult(step2Result, testOfferings, null)
        vm.trackPaywallImpressionIfNeeded()
        // Same visual fingerprint, so no new impression is emitted: this proves the de-dup branch
        // (the one withCurrentWorkflowMetadata lives in) was taken, not a fresh re-creation.
        assertThat(
            captured.filterIsInstance<PaywallEvent>().none { it.type == PaywallEventType.IMPRESSION },
        ).isTrue
        vm.handlePackagePurchase(activity = mockk<Activity>(), pkg = TestData.Packages.monthly)
        advanceUntilIdle()

        val purchaseInitiated = captured.filterIsInstance<PaywallEvent>()
            .single { it.type == PaywallEventType.PURCHASE_INITIATED }
        assertThat(purchaseInitiated.data.workflowId).isEqualTo(step2Result.workflow.id)
        assertThat(purchaseInitiated.data.stepId).isEqualTo("step-2")
    }

    @Test
    fun `exit offer after same paywall workflow re-presentation carries current step metadata`() = runTest {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }
        val vm = createVm()

        // Both steps render the same screen, so the visual presentation fingerprint is identical
        // across the re-presentation and the impression is de-duped. Only the workflow step id
        // changes, which is exactly what withCurrentWorkflowMetadata must refresh on the
        // already-tracked presentation data.
        val sharedScreenSteps = mapOf(
            "step-1" to step1.copy(screenId = screenId2),
            "step-2" to step2.copy(screenId = screenId2),
        )
        val step1Result = WorkflowDataResult(
            workflow = workflow.copy(
                initialStepId = "step-1",
                singleStepFallbackId = "step-1",
                steps = sharedScreenSteps,
            ),
            enrolledVariants = null,
        )
        val step2Result = WorkflowDataResult(
            workflow = workflow.copy(
                initialStepId = "step-2",
                singleStepFallbackId = "step-2",
                steps = sharedScreenSteps,
            ),
            enrolledVariants = null,
        )

        vm.startWorkflowPresentationFromResult(step1Result, testOfferings, null)
        vm.trackPaywallImpressionIfNeeded()
        captured.clear()

        vm.startWorkflowPresentationFromResult(step2Result, testOfferings, null)
        vm.trackPaywallImpressionIfNeeded()
        // Same visual fingerprint, so no new impression is emitted: this proves the de-dup branch
        // (the one withCurrentWorkflowMetadata lives in) was taken, not a fresh re-creation.
        assertThat(
            captured.filterIsInstance<PaywallEvent>().none { it.type == PaywallEventType.IMPRESSION },
        ).isTrue
        vm.trackExitOffer(ExitOfferType.DISMISS, exitOfferingIdentifier = "exit-offering-id")
        advanceUntilIdle()

        val exitOffer = captured.filterIsInstance<PaywallEvent>()
            .single { it.type == PaywallEventType.EXIT_OFFER }
        assertThat(exitOffer.data.workflowId).isEqualTo(step2Result.workflow.id)
        assertThat(exitOffer.data.stepId).isEqualTo("step-2")
    }

    @Test
    fun `packageless workflow steps do not emit paywall events`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }
        val (result, offerings) = makeContextPackageWorkflow()

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(result, offerings, null)
        vm.trackPaywallImpressionIfNeeded()

        assertThat(captured.filterIsInstance<PaywallEvent>()).isEmpty()

        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        vm.trackPaywallImpressionIfNeeded()

        val impressions = captured.filterIsInstance<PaywallEvent>()
            .filter { it.type == PaywallEventType.IMPRESSION }
        val closes = captured.filterIsInstance<PaywallEvent>()
            .filter { it.type == PaywallEventType.CLOSE }
        assertThat(impressions).hasSize(1)
        assertThat(closes).isEmpty()

        vm.onTransitionComplete(vm.workflowState.value!!.pendingTransition!!.id)
        vm.handleBackNavigation()
        vm.trackPaywallImpressionIfNeeded()

        val paywallEvents = captured.filterIsInstance<PaywallEvent>()
        assertThat(paywallEvents.filter { it.type == PaywallEventType.IMPRESSION }).hasSize(1)
        assertThat(paywallEvents.filter { it.type == PaywallEventType.CLOSE }).isEmpty()

        vm.closePaywall(result = null)

        assertThat(captured.filterIsInstance<PaywallEvent>().filter { it.type == PaywallEventType.IMPRESSION })
            .hasSize(1)
        assertThat(captured.filterIsInstance<PaywallEvent>().filter { it.type == PaywallEventType.CLOSE })
            .isEmpty()
    }

    // endregion

    // region purchase/restore callbacks

    @Test
    fun `onPurchaseStarted fires from initial workflow step`() = runTest {
        val listener = makeListener()
        val transaction = mockk<StoreTransaction>()
        coEvery { purchases.awaitPurchase(any()) } returns PurchaseResult(transaction, mockk(relaxed = true))
        val activity = mockk<Activity>()
        val vm = createVm(listener = listener)
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)

        vm.handlePackagePurchase(activity, TestData.Packages.monthly)
        advanceUntilIdle()

        verify { listener.onPurchaseStarted(TestData.Packages.monthly) }
    }

    @Test
    fun `onPurchaseCompleted fires from initial workflow step`() = runTest {
        val listener = makeListener()
        val transaction = mockk<StoreTransaction>()
        val customerInfo = mockk<CustomerInfo>(relaxed = true)
        coEvery { purchases.awaitPurchase(any()) } returns PurchaseResult(transaction, customerInfo)
        val activity = mockk<Activity>()
        val vm = createVm(listener = listener)
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)

        vm.handlePackagePurchase(activity, TestData.Packages.monthly)
        advanceUntilIdle()

        verifyOrder {
            listener.onPurchaseStarted(TestData.Packages.monthly)
            listener.onPurchaseCompleted(customerInfo, transaction)
        }
    }

    @Test
    fun `onPurchaseCancelled fires from initial workflow step`() = runTest {
        val listener = makeListener()
        val cancelError = PurchasesError(PurchasesErrorCode.PurchaseCancelledError)
        coEvery { purchases.awaitPurchase(any()) } throws PurchasesException(cancelError)
        val activity = mockk<Activity>()
        val vm = createVm(listener = listener)
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)

        vm.handlePackagePurchase(activity, TestData.Packages.monthly)
        advanceUntilIdle()

        verify { listener.onPurchaseCancelled() }
        verify(exactly = 0) { listener.onPurchaseError(any()) }
    }

    @Test
    fun `onPurchaseError fires from initial workflow step`() = runTest {
        val listener = makeListener()
        val purchaseError = PurchasesError(PurchasesErrorCode.ProductNotAvailableForPurchaseError)
        coEvery { purchases.awaitPurchase(any()) } throws PurchasesException(purchaseError)
        val activity = mockk<Activity>()
        val vm = createVm(listener = listener)
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)

        vm.handlePackagePurchase(activity, TestData.Packages.monthly)
        advanceUntilIdle()

        verify { listener.onPurchaseError(purchaseError) }
        verify(exactly = 0) { listener.onPurchaseCancelled() }
    }

    @Test
    fun `onRestoreStarted fires from initial workflow step`() = runTest {
        val listener = makeListener()
        coEvery { purchases.awaitRestore() } returns mockk(relaxed = true)
        val vm = createVm(listener = listener)
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)

        vm.handleRestorePurchases()
        advanceUntilIdle()

        verify { listener.onRestoreStarted() }
    }

    @Test
    fun `onRestoreCompleted fires from initial workflow step`() = runTest {
        val listener = makeListener()
        val customerInfo = mockk<CustomerInfo>(relaxed = true)
        coEvery { purchases.awaitRestore() } returns customerInfo
        val vm = createVm(listener = listener)
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)

        vm.handleRestorePurchases()
        advanceUntilIdle()

        verifyOrder {
            listener.onRestoreStarted()
            listener.onRestoreCompleted(customerInfo)
        }
    }

    @Test
    fun `onRestoreError fires from initial workflow step`() = runTest {
        val listener = makeListener()
        val restoreError = PurchasesError(PurchasesErrorCode.NetworkError)
        coEvery { purchases.awaitRestore() } throws PurchasesException(restoreError)
        val vm = createVm(listener = listener)
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null)

        vm.handleRestorePurchases()
        advanceUntilIdle()

        verify { listener.onRestoreError(restoreError) }
        verify(exactly = 0) { listener.onRestoreCompleted(any()) }
    }

    // endregion purchase/restore callbacks

    // region screen_type paywall-event gating

    private fun singleStepScreenTypeWorkflow(metadata: JsonObject?): WorkflowDataResult {
        val step = WorkflowStep(
            id = "step-only",
            type = "screen",
            screenId = screenId1,
            triggers = emptyList(),
            triggerActions = emptyMap(),
            metadata = metadata,
        )
        return WorkflowDataResult(
            workflow = workflow.copy(
                initialStepId = "step-only",
                steps = mapOf("step-only" to step),
                screens = mapOf(screenId1 to makeScreen(screenId1)),
                singleStepFallbackId = "step-only",
            ),
            enrolledVariants = null,
        )
    }

    private fun List<FeatureEvent>.paywallEventsOfType(type: PaywallEventType) =
        filterIsInstance<PaywallEvent>().filter { it.type == type }

    @Test
    fun `step tagged paywall reports impression and close`() = runTest {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(
            singleStepScreenTypeWorkflow(screenTypeMetadata(WorkflowScreenType.PAYWALL)),
            testOfferings,
            null,
        )
        vm.trackPaywallImpressionIfNeeded()
        assertThat(captured.paywallEventsOfType(PaywallEventType.IMPRESSION)).hasSize(1)

        vm.closePaywall(result = null)
        assertThat(captured.paywallEventsOfType(PaywallEventType.CLOSE)).hasSize(1)
    }

    @Test
    fun `step tagged non-paywall suppresses impression and close`() = runTest {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        // Empty screen_type means "explicitly not a paywall".
        vm.startWorkflowPresentationFromResult(
            singleStepScreenTypeWorkflow(screenTypeMetadata()),
            testOfferings,
            null,
        )
        vm.trackPaywallImpressionIfNeeded()
        vm.closePaywall(result = null)

        assertThat(captured.filterIsInstance<PaywallEvent>()).isEmpty()
    }

    @Test
    fun `untagged fallback step reports impression to preserve behavior`() = runTest {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        // No metadata → stepScreenType is null (untagged / pre-rollout) → falls back to the structural
        // inference `id == singleStepFallbackId`. Here the only step is the fallback, so it reports.
        vm.startWorkflowPresentationFromResult(
            singleStepScreenTypeWorkflow(metadata = null),
            testOfferings,
            null,
        )
        vm.trackPaywallImpressionIfNeeded()

        assertThat(captured.paywallEventsOfType(PaywallEventType.IMPRESSION)).hasSize(1)
    }

    @Test
    fun `untagged non-fallback step suppresses impression matching pre-rollout behavior`() = runTest {
        // When no step carries screen_type (pre-rollout / legacy backend), gating falls back to the
        // prior structural inference: only the singleStepFallbackId step reports. step-1 (initial) is
        // NOT the fallback here (step-2 is), so it suppresses — matching the behavior on main before the
        // screen_type rollout, rather than over-reporting on every step.
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        // Base workflow steps carry no screen_type; point the fallback at step-2 so step-1 is a
        // non-fallback initial step.
        val untaggedMultiStep = WorkflowDataResult(
            workflow = workflow.copy(singleStepFallbackId = "step-2"),
            enrolledVariants = null,
        )
        vm.startWorkflowPresentationFromResult(untaggedMultiStep, testOfferings, null)
        vm.trackPaywallImpressionIfNeeded()

        assertThat(captured.paywallEventsOfType(PaywallEventType.IMPRESSION)).isEmpty()
    }

    // endregion
}
