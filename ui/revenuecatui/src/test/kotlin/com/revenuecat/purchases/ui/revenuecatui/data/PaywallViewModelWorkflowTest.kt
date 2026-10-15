@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.data

import android.app.Activity
import android.os.Looper
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.PurchaseResult
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.WorkflowResolution
import com.revenuecat.purchases.common.workflows.WorkflowScreen
import com.revenuecat.purchases.common.workflows.WorkflowScreenType
import com.revenuecat.purchases.common.workflows.WorkflowStep
import com.revenuecat.purchases.common.workflows.WorkflowTrigger
import com.revenuecat.purchases.common.workflows.WorkflowTriggerAction
import com.revenuecat.purchases.common.workflows.WorkflowTriggerType
import com.revenuecat.purchases.common.events.FeatureEvent
import com.revenuecat.purchases.common.workflows.events.WorkflowEvent
import com.revenuecat.purchases.paywalls.events.ExitOfferType
import com.revenuecat.purchases.paywalls.events.PaywallComponentType
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
import com.revenuecat.purchases.ui.revenuecatui.PaywallDismissReason
import com.revenuecat.purchases.ui.revenuecatui.PaywallErrorPresenter
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.PaywallPurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.PaywallPurchaseLogicParams
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicResult
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointFailureKind
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.ErrorPresenter
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointGateContext
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.CheckpointGateOutcome
import com.revenuecat.purchases.ui.revenuecatui.components.style.WebViewComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.webview.webViewContextSnapshot
import com.revenuecat.purchases.ui.revenuecatui.utils.Resumable
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallLegacyComponentInteraction
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import com.revenuecat.purchases.ui.revenuecatui.workflow.NavigationDirection
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import com.revenuecat.purchases.common.workflows.WorkflowStepID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import com.revenuecat.purchases.paywalls.components.common.StateDeclaration
import com.revenuecat.purchases.withPresentedCheckpointIdentifier
import kotlinx.serialization.json.JsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

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
        paramValues = offeringParams(offeringId),
    )
    private val step2 = WorkflowStep(
        id = "step-2",
        type = "screen",
        screenId = screenId2,
        triggers = emptyList(),
        triggerActions = emptyMap(),
        paramValues = offeringParams(offeringId),
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

    @Test
    fun `a step that only branches does not report itself as the last step`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        // Counting Step actions alone would read this as terminal.
        val branchStep = step1.copy(
            triggerActions = mapOf(
                "action-next" to WorkflowTriggerAction.Branch(
                    routes = emptyList(),
                    fallbackStepId = "step-2",
                ),
            ),
        )
        val wfl = workflow.copy(steps = mapOf("step-1" to branchStep, "step-2" to step2))

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(wfl, testOfferings, null, uiConfig)

        val started = captured.filterIsInstance<WorkflowEvent.StepStarted>()
        assertThat(started).hasSize(1)
        assertThat(started.first().isLastStep).isFalse
    }

    @Test
    fun `a colour rebuild does not resolve a step's branches again`() {
        var resolves = 0
        coEvery { purchases.resolveBranches(any(), any()) } answers {
            resolves++
            mapOf("action-next" to "step-2")
        }
        val branchStep = step1.copy(
            triggerActions = mapOf(
                "action-next" to WorkflowTriggerAction.Branch(routes = emptyList(), fallbackStepId = "step-2"),
            ),
        )
        val wfl = workflow.copy(steps = mapOf("step-1" to branchStep, "step-2" to step2))

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(wfl, testOfferings, null, uiConfig)
        testDispatcher.scheduler.advanceUntilIdle()
        assertThat(resolves).isOne()

        // The same visit: re-resolving here would cancel the answer in flight and could route elsewhere.
        vm.refreshStateIfColorsChanged(
            colorScheme = TestData.Constants.currentColorScheme.copy(primary = Color.Black),
            isDark = true,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(resolves).isOne()
    }

    @Test
    fun `an audience-routed first step shows a skeleton until the branch resolves`() {
        val gate = CompletableDeferred<WorkflowStepID>()
        coEvery { purchases.resolveBranch(any(), any()) } coAnswers { gate.await() }
        val wfl = workflow.copy(
            initialTrigger = WorkflowTriggerAction.Branch(routes = emptyList(), fallbackStepId = "step-1"),
        )

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(wfl, testOfferings, null, uiConfig)
        testDispatcher.scheduler.advanceUntilIdle()

        // Nothing is known yet, so the fallback renders as a skeleton and does not count as a real visit.
        assertThat(vm.workflowState.value?.isSkeleton).isTrue
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")

        gate.complete("step-2")
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.workflowState.value?.isSkeleton).isFalse
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-2")
    }

    @Test
    fun `a first step with no branch renders straight away`() {
        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.workflowState.value?.isSkeleton).isFalse
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
        coVerify(exactly = 0) { purchases.resolveBranch(any(), any()) }
    }

    @Test
    fun `a skeleton does not record a paywall impression for a step nobody reached`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }
        val gate = CompletableDeferred<WorkflowStepID>()
        coEvery { purchases.resolveBranch(any(), any()) } coAnswers { gate.await() }
        val wfl = workflow.copy(
            initialTrigger = WorkflowTriggerAction.Branch(routes = emptyList(), fallbackStepId = "step-1"),
        )

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(wfl, testOfferings, null, uiConfig)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.trackPaywallImpressionIfNeeded()

        assertThat(captured.filterIsInstance<PaywallEvent>()).isEmpty()

        // The branch lands on its own fallback, which is the usual no-match outcome.
        gate.complete("step-1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Composition asks again once the real step is on screen, and that one must land.
        vm.trackPaywallImpressionIfNeeded()
        assertThat(captured.filterIsInstance<PaywallEvent>()).hasSize(1)
    }

    @Test
    fun `a colour rebuild during the wait keeps the skeleton`() {
        val gate = CompletableDeferred<WorkflowStepID>()
        var resolves = 0
        coEvery { purchases.resolveBranch(any(), any()) } coAnswers { resolves++; gate.await() }
        val wfl = workflow.copy(
            initialTrigger = WorkflowTriggerAction.Branch(routes = emptyList(), fallbackStepId = "step-1"),
        )

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(wfl, testOfferings, null, uiConfig)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.refreshStateIfColorsChanged(
            colorScheme = TestData.Constants.currentColorScheme.copy(primary = Color.Black),
            isDark = true,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // The live fallback takes taps on a screen the audience can still route away from.
        assertThat(vm.workflowState.value?.isSkeleton).isTrue
        assertThat(resolves).isOne()

        gate.complete("step-1")
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `a dismiss while the skeleton is up attributes no step to the fallback`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }
        val gate = CompletableDeferred<WorkflowStepID>()
        coEvery { purchases.resolveBranch(any(), any()) } coAnswers { gate.await() }
        val wfl = workflow.copy(
            initialTrigger = WorkflowTriggerAction.Branch(routes = emptyList(), fallbackStepId = "step-1"),
        )

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(wfl, testOfferings, null, uiConfig)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.closePaywall()
        testDispatcher.scheduler.advanceUntilIdle()

        // Nobody saw step-1, so it completed nothing.
        assertThat(captured.filterIsInstance<WorkflowEvent.StepCompleted>()).isEmpty()

        gate.complete("step-1")
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `the step a branch picks reports itself as the first step`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }
        coEvery { purchases.resolveBranch(any(), any()) } returns "step-2"
        val wfl = workflow.copy(
            initialTrigger = WorkflowTriggerAction.Branch(routes = emptyList(), fallbackStepId = "step-1"),
        )

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(wfl, testOfferings, null, uiConfig)
        testDispatcher.scheduler.advanceUntilIdle()

        // step-2 is not initialStepId, but it is the first screen the user sees.
        val started = captured.filterIsInstance<WorkflowEvent.StepStarted>().single()
        assertThat(started.stepId).isEqualTo("step-2")
        assertThat(started.isFirstStep).isTrue
    }

    @Test
    fun `a step routed past the package step still gets its default package`() {
        val (base, offerings) = makeContextPackageWorkflow()
        // The package step is also the first step, so nothing else prebuilds it.
        val wfl = base.copy(
            initialStepId = "step-2",
            initialTrigger = WorkflowTriggerAction.Branch(routes = emptyList(), fallbackStepId = "step-2"),
        )
        coEvery { purchases.resolveBranch(any(), any()) } returns "step-1"

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(wfl, offerings, null, uiConfig)
        testDispatcher.scheduler.advanceUntilIdle()

        val routed = vm.workflowState.value?.stepStates?.get("step-1")
        assertThat(routed?.selectedPackageInfo?.rcPackage?.identifier)
            .isEqualTo(PackageType.MONTHLY.identifier)
    }

    @Test
    fun `a broken fallback step does not stop the step the branch picks`() {
        // step-1 has no screen, so it cannot render. The audience routes past it.
        val wfl = workflow.copy(
            screens = mapOf(screenId2 to makeScreen(screenId2)),
            initialTrigger = WorkflowTriggerAction.Branch(routes = emptyList(), fallbackStepId = "step-1"),
        )
        coEvery { purchases.resolveBranch(any(), any()) } returns "step-2"

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(wfl, testOfferings, null, uiConfig)
        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-2")
        assertThat(vm.workflowState.value?.isSkeleton).isFalse
    }

    @Test
    fun `a key two screens declare differently resolves by screen id, not by build order`() {
        // screen-1 sorts first, so its default wins wherever the workflow starts.
        val declared = { value: String ->
            mapOf("tier" to StateDeclaration(StateDeclaration.ValueType.STRING, JsonPrimitive(value)))
        }
        // Starting on step-2, and it is also the package step, so nothing builds screen-1 first.
        val wfl = workflow.copy(
            initialStepId = "step-2",
            singleStepFallbackId = "step-2",
            screens = mapOf(
                screenId1 to makeScreen(screenId1).copy(stateDeclarations = declared("one")),
                screenId2 to makeScreen(screenId2).copy(stateDeclarations = declared("two")),
            ),
        )

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(wfl, testOfferings, null, uiConfig)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.workflowState.value?.stepStates?.get("step-2") as PaywallState.Loaded.Components
        assertThat(state.stateStore.currentValueOrDefault("tier")).isEqualTo(JsonPrimitive("one"))
    }

    private fun makeTwoPackageWorkflow(): Pair<PublishedWorkflow, Offerings> {
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
        return wfl to offerings
    }

    private fun offeringParams(offeringId: String): Map<String, JsonObject> =
        mapOf("offering" to JsonObject(mapOf("identifier" to JsonPrimitive(offeringId))))

    private fun screenTypeMetadata(vararg types: String): JsonObject =
        JsonObject(mapOf(WorkflowScreenType.METADATA_KEY to JsonArray(types.map { JsonPrimitive(it) })))

    private fun makeContextPackageWorkflow(): Pair<PublishedWorkflow, Offerings> {
        val earlyScreen = makeScreen(screenId1).copy(
            componentsConfig = noPackagesComponentsConfig,
            offeringIdentifier = null,
        )
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
            // Neither this step nor its screen names an offering, so it renders without one.
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
            paramValues = offeringParams(offeringId),
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
        return wfl to offerings
    }

    // PublishedWorkflow no longer carries its own ui_config: startWorkflowPresentationFromResult now takes it
    // as a separate argument (mirroring purchases.awaitGetUiConfig() in the real fetch path). Reuse the same
    // fixture across every direct-injection call site below so screens keep resolving their variable
    // localizations the way they did when this lived on the workflow itself.
    private val uiConfig = UiConfig()

    private val workflow = PublishedWorkflow(
        id = "wfl-test",
        displayName = "Test",
        initialStepId = "step-1",
        steps = mapOf("step-1" to step1, "step-2" to step2),
        screens = mapOf(screenId1 to makeScreen(screenId1), screenId2 to makeScreen(screenId2)),
        metadata = emptyMap(),
        singleStepFallbackId = "step-1",
    )
    private val fetchResult = workflow

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
        metadata = emptyMap(),
        singleStepFallbackId = "step-2",
    )
    private val fetchResultWithExitOffer = workflowWithExitOffer

    private val singleStep = WorkflowStep(
        id = "step-only",
        type = "screen",
        screenId = screenId1,
        triggers = emptyList(),
        triggerActions = emptyMap(),
        paramValues = offeringParams(offeringId),
    )

    private val singleStepWorkflowWithExitOffer = PublishedWorkflow(
        id = "wfl-single",
        displayName = "Single Step",
        initialStepId = "step-only",
        steps = mapOf("step-only" to singleStep),
        screens = mapOf(screenId1 to makeScreenWithExitOffer(screenId1)),
        metadata = emptyMap(),
        singleStepFallbackId = "step-only",
    )
    private val singleStepFetchResultWithExitOffer = singleStepWorkflowWithExitOffer

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
        metadata = emptyMap(),
        singleStepFallbackId = "step-1",
    )
    private val fetchResultWithFallback = workflowWithFallbackPointingToFirstStep

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
    )

    private val step3EmptyOffering = WorkflowStep(
        id = "step-3",
        type = "screen",
        screenId = screenEmptyId,
        triggers = emptyList(),
        triggerActions = emptyMap(),
        paramValues = offeringParams(emptyOfferingId),
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
        paramValues = offeringParams(offeringId),
    )

    private val screenWithEmptyOffering2 = WorkflowScreen(
        name = "screen-empty-initial",
        templateName = "template_v2",
        revision = 1,
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = componentsConfig,
        componentsLocalizations = localizations,
        defaultLocaleIdentifier = defaultLocaleId,
    )

    private val step1WithEmptyOffering = WorkflowStep(
        id = "step-1",
        type = "screen",
        screenId = "screen-empty-initial",
        triggers = emptyList(),
        triggerActions = emptyMap(),
        paramValues = offeringParams(emptyOfferingId),
    )

    private val workflowFailingInitial = PublishedWorkflow(
        id = "wfl-failing",
        displayName = "Failing",
        initialStepId = "step-1",
        steps = mapOf("step-1" to step1WithEmptyOffering),
        screens = mapOf("screen-empty-initial" to screenWithEmptyOffering2),
        metadata = emptyMap(),
    )
    private val fetchResultFailingInitial = workflowFailingInitial

    private val workflowToError = PublishedWorkflow(
        id = "wfl-to-error",
        displayName = "Test",
        initialStepId = "step-1",
        steps = mapOf("step-1" to step1ToStep3, "step-3" to step3EmptyOffering),
        screens = mapOf(screenId1 to makeScreen(screenId1), screenEmptyId to screenWithEmptyOffering),
        metadata = emptyMap(),
    )
    private val fetchResultToError = workflowToError


    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        purchases = mockk {
            every { appUserID } returns "customer-a"
            every { storefrontCountryCode } returns "US"
            every { preferredUILocaleOverride } returns null
            every { purchasesAreCompletedBy } returns PurchasesAreCompletedBy.REVENUECAT
            every { track(any()) } just Runs
            coEvery { awaitOfferings() } returns testOfferings
            coEvery { awaitWorkflowBlobRef(any()) } returns null
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
        dismissRequest: () -> Unit = {},
        dismissRequestWithExitOffering: ((Offering?, PaywallResult?, PaywallDismissReason) -> Unit)? = null,
        listener: PaywallListener? = null,
        errorPresenter: PaywallErrorPresenter? = null,
        gateContext: CheckpointGateContext? = null,
        purchaseLogic: PaywallPurchaseLogic? = null,
        shouldDisplayBlock: ((CustomerInfo) -> Boolean)? = null,
    ): PaywallViewModelImpl {
        val builder = PaywallOptions.Builder(dismissRequest = dismissRequest)
        dismissRequestWithExitOffering?.let { builder.setDismissRequestWithExitOffering(it) }
        listener?.let { builder.setListener(it) }
        errorPresenter?.let { builder.setErrorPresenter(it) }
        builder.setCheckpointGateContext(gateContext)
        builder.setPurchaseLogic(purchaseLogic)
        return PaywallViewModelImpl(
            resourceProvider = MockResourceProvider(),
            purchases = purchases,
            options = builder.build(),
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false,
            shouldDisplayBlock = shouldDisplayBlock,
            // Run the step-state pre-warm on the test scheduler so advanceUntilIdle() awaits it
            // instead of it escaping to Dispatchers.Default and resuming after resetMain().
            backgroundDispatcher = testDispatcher,
        )
    }

    private fun TestScope.createInjectedWorkflowVm(
        traceId: String?,
        injectedWorkflow: PublishedWorkflow = workflow,
        workflowBlobRef: String? = null,
    ): PaywallViewModelImpl {
        val vm = PaywallViewModelImpl(
            resourceProvider = MockResourceProvider(),
            purchases = purchases,
            options = PaywallOptions.Builder(dismissRequest = {})
                .injectedWorkflow(injectedWorkflow, testOfferings, uiConfig, traceId, workflowBlobRef)
                .build(),
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false,
            shouldDisplayBlock = null,
            backgroundDispatcher = testDispatcher,
        )
        advanceUntilIdle()
        return vm
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

    private fun WorkflowStep.withHardGate() = copy(paramValues = paramValues + ("gating" to JsonObject(mapOf(
        "type" to JsonPrimitive("hard"),
        "entitlement_identifiers" to JsonArray(listOf(JsonPrimitive("pro"), JsonPrimitive("premium"))),
    ))))

    private fun gatedCustomerInfo(entitlement: String): CustomerInfo = mockk {
        every { entitlements.active } returns mapOf(entitlement to mockk())
    }

    private fun PaywallViewModelImpl.startHardGate() = startWorkflowPresentationFromResult(
        workflow.copy(steps = mapOf(step1.id to step1.withHardGate(), step2.id to step2)),
        testOfferings, null, uiConfig,
    )

    @Test
    fun `cached entitlements from previous identity cannot pass a later hard gate`() {
        val outcomes = mutableListOf<CheckpointGateOutcome>()
        val reasons = mutableListOf<PaywallDismissReason>()
        val vm = createVm(
            gateContext = CheckpointGateContext("customer-a", setOf("pro"), outcomes::add),
            dismissRequestWithExitOffering = { _, _, reason -> reasons.add(reason) },
        )
        vm.startWorkflowPresentationFromResult(
            workflow.copy(steps = mapOf(step1.id to step1, step2.id to step2.withHardGate())),
            testOfferings, null, uiConfig,
        )
        every { purchases.appUserID } returns "customer-b"

        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)

        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-2")
        assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.NOT_PASSED)
        assertThat(reasons).isEmpty()
        vm.closePaywall()
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
        vm.closePaywall()
        assertThat(reasons).containsExactly(PaywallDismissReason.NAVIGATED_BACK)
        assertThat(outcomes).doesNotContain(CheckpointGateOutcome.PASSED)
    }

    @Test
    fun `cached entitlements from previous identity cannot pass initial hard gate`() {
        val outcomes = mutableListOf<CheckpointGateOutcome>()
        val vm = createVm(gateContext = CheckpointGateContext("customer-a", setOf("pro"), outcomes::add))
        every { purchases.appUserID } returns "customer-b"

        vm.startHardGate()

        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
        assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.NOT_PASSED)
    }

    @Test
    fun `restore from previous identity on earlier soft step cannot seed later hard gate`() = runTest {
        val result = CompletableDeferred<CustomerInfo>()
        coEvery { purchases.awaitRestore() } coAnswers { result.await() }
        val outcomes = mutableListOf<CheckpointGateOutcome>()
        var dismissals = 0
        val vm = createVm(
            gateContext = CheckpointGateContext("customer-a", emptySet(), outcomes::add),
            dismissRequest = { dismissals++ },
        )
        vm.startWorkflowPresentationFromResult(
            workflow.copy(steps = mapOf(step1.id to step1, step2.id to step2.withHardGate())),
            testOfferings, null, uiConfig,
        )
        val operation = launch { vm.handleRestorePurchases() }
        runCurrent()
        every { purchases.appUserID } returns "customer-b"
        result.complete(gatedCustomerInfo("pro"))
        operation.join()

        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)

        assertThat(dismissals).isZero()
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-2")
        assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.NOT_PASSED)
    }

    @Test
    fun `restore listener changing identity cannot complete hard gate for new customer`() = runTest {
        coEvery { purchases.awaitRestore() } returns gatedCustomerInfo("pro")
        val outcomes = mutableListOf<CheckpointGateOutcome>()
        val events = mutableListOf<FeatureEvent>()
        every { purchases.track(capture(events)) } just Runs
        val listener = makeListener()
        every { listener.onRestoreCompleted(any()) } answers {
            every { purchases.appUserID } returns "customer-b"
        }
        var dismissals = 0
        val vm = createVm(
            gateContext = CheckpointGateContext("customer-a", emptySet(), outcomes::add),
            dismissRequest = { dismissals++ }, listener = listener,
        )
        vm.startHardGate()

        vm.handleRestorePurchases()

        assertThat(dismissals).isZero()
        assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.NOT_PASSED)
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
        vm.closePaywall()
        assertThat(events.filterIsInstance<WorkflowEvent.Close>()).hasSize(1)
    }

    @Test
    fun `entry outcome callback changing identity renders hard gate for new customer`() {
        val outcomes = mutableListOf<CheckpointGateOutcome>()
        var dismissals = 0
        val vm = createVm(
            gateContext = CheckpointGateContext("customer-a", setOf("pro")) { outcome ->
                outcomes.add(outcome)
                if (outcome == CheckpointGateOutcome.PASSED) every { purchases.appUserID } returns "customer-b"
            },
            dismissRequest = { dismissals++ },
        )

        vm.startHardGate()

        assertThat(dismissals).isZero()
        assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.NOT_PASSED)
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
    }

    @Test
    fun `identity invalidation during close cannot dismiss replacement presentation`() = runTest {
        coEvery { purchases.awaitRestore() } returns gatedCustomerInfo("pro")
        var passed = false
        var dismissals = 0
        lateinit var vm: PaywallViewModelImpl
        val listener = makeListener()
        every { listener.onRestoreCompleted(any()) } answers {
            every { purchases.appUserID } returns "customer-b"
            vm.closePaywall()
        }
        vm = createVm(
            gateContext = CheckpointGateContext("customer-a", emptySet()) { outcome ->
                if (outcome == CheckpointGateOutcome.PASSED) passed = true
                if (passed && outcome == CheckpointGateOutcome.NOT_PASSED) {
                    vm.startWorkflowPresentationFromResult(workflow, testOfferings, null, uiConfig)
                }
            },
            dismissRequest = { dismissals++ }, listener = listener,
        )
        vm.startHardGate()

        vm.handleRestorePurchases()

        assertThat(dismissals).isZero()
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
    }

    @Test
    fun `RevenueCat purchase from previous identity cannot complete hard gate`() = runTest {
        assertPreviousIdentityCompletion(isPurchase = true)
    }

    @Test
    fun `RevenueCat restore from previous identity cannot complete hard gate`() = runTest {
        assertPreviousIdentityCompletion(isPurchase = false)
    }

    @Test
    fun `MY_APP purchase from previous identity cannot complete hard gate`() = runTest {
        assertPreviousIdentityCompletion(isPurchase = true, myApp = true)
    }

    @Test
    fun `MY_APP restore from previous identity cannot complete hard gate`() = runTest {
        assertPreviousIdentityCompletion(isPurchase = false, myApp = true)
    }

    @Test
    fun `RevenueCat purchase after identity change preserves soft completion`() = runTest {
        assertPreviousIdentityCompletion(isPurchase = true, hardGate = false)
    }

    @Test
    fun `RevenueCat restore after identity change preserves soft completion`() = runTest {
        assertPreviousIdentityCompletion(isPurchase = false, hardGate = false)
    }

    @Test
    fun `MY_APP purchase after identity change preserves soft completion`() = runTest {
        assertPreviousIdentityCompletion(isPurchase = true, myApp = true, hardGate = false)
    }

    @Test
    fun `MY_APP restore after identity change preserves soft completion`() = runTest {
        assertPreviousIdentityCompletion(isPurchase = false, myApp = true, hardGate = false)
    }

    private suspend fun TestScope.assertPreviousIdentityCompletion(
        isPurchase: Boolean,
        myApp: Boolean = false,
        hardGate: Boolean = true,
    ) {
        val result = CompletableDeferred<CustomerInfo>()
        val transaction = mockk<StoreTransaction>()
        coEvery { purchases.awaitPurchase(any()) } coAnswers { PurchaseResult(transaction, result.await()) }
        coEvery { purchases.awaitRestore() } coAnswers { result.await() }
        coEvery { purchases.awaitSyncPurchases() } coAnswers { result.await() }
        if (myApp) every { purchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.MY_APP
        val listener = makeListener()
        val outcomes = mutableListOf<CheckpointGateOutcome>()
        val events = mutableListOf<FeatureEvent>()
        every { purchases.track(capture(events)) } just Runs
        var dismissals = 0
        val vm = createVm(
            dismissRequest = { dismissals++ }, listener = listener,
            gateContext = CheckpointGateContext("customer-a", emptySet(), outcomes::add),
            purchaseLogic = if (myApp) successfulPurchaseLogic else null,
        )
        if (hardGate) vm.startHardGate()
        else vm.startWorkflowPresentationFromResult(workflow, testOfferings, null, uiConfig)
        val operation = launch {
            if (isPurchase) vm.handlePackagePurchase(mockk<Activity>(), TestData.Packages.monthly)
            else vm.handleRestorePurchases()
        }
        runCurrent()
        every { purchases.appUserID } returns "customer-b"
        val customerInfo = gatedCustomerInfo("pro")
        result.complete(customerInfo)
        operation.join()

        verify(exactly = if (!myApp && isPurchase) 1 else 0) {
            listener.onPurchaseCompleted(customerInfo, transaction)
        }
        verify(exactly = if (!myApp && !isPurchase) 1 else 0) { listener.onRestoreCompleted(customerInfo) }
        if (hardGate) {
            assertThat(dismissals).isZero()
            assertThat(outcomes).doesNotContain(CheckpointGateOutcome.PASSED)
            assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
            assertThat(events.filterIsInstance<WorkflowEvent.StepCompleted>()).isEmpty()
            // A fresh operation belongs to B even when CustomerInfo's original ID is an alias of A.
            every { customerInfo.originalAppUserId } returns "customer-a"
            vm.handleRestorePurchases()
            assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.PASSED)
            assertThat(dismissals).isEqualTo(1)
        } else {
            assertThat(dismissals).isEqualTo(if (isPurchase) 1 else 0)
            if (!isPurchase) vm.closePaywall()
            assertThat(events.filterIsInstance<WorkflowEvent.Close>()).isEmpty()
        }
    }

    @Test
    fun `restore on earlier soft step does not suppress later blocked hard gate abandonment`() = runTest {
        coEvery { purchases.awaitRestore() } returns gatedCustomerInfo("unrelated")
        val outcomes = mutableListOf<CheckpointGateOutcome>()
        val events = mutableListOf<FeatureEvent>()
        every { purchases.track(capture(events)) } just Runs
        val reasons = mutableListOf<PaywallDismissReason>()
        val vm = createVm(
            gateContext = CheckpointGateContext("customer-a", emptySet(), outcomes::add),
            dismissRequestWithExitOffering = { _, _, reason -> reasons.add(reason) },
        )
        vm.startWorkflowPresentationFromResult(
            workflow.copy(steps = mapOf(step1.id to step1, step2.id to step2.withHardGate())),
            testOfferings, null, uiConfig,
        )
        vm.handleRestorePurchases()
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        vm.closePaywall()
        vm.closePaywall()

        assertThat(events.filterIsInstance<WorkflowEvent.Close>().map { it.stepId }).containsExactly("step-1")
        assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.NOT_PASSED)
        assertThat(reasons).containsExactly(PaywallDismissReason.NAVIGATED_BACK)
    }

    @Test
    fun `RevenueCat qualifying purchase passes hard gate and completes visible step once`() = runTest {
        val customerInfo = gatedCustomerInfo("pro")
        val transaction = mockk<StoreTransaction>()
        coEvery { purchases.awaitPurchase(any()) } returns PurchaseResult(transaction, customerInfo)
        val listener = makeListener()
        val outcomes = mutableListOf<CheckpointGateOutcome>()
        val events = mutableListOf<FeatureEvent>()
        every { purchases.track(capture(events)) } just Runs
        var dismissals = 0
        val vm = createVm(
            dismissRequest = { dismissals++ }, listener = listener,
            gateContext = CheckpointGateContext("customer-a", emptySet(), outcomes::add),
        )
        vm.startHardGate()

        vm.handlePackagePurchase(mockk<Activity>(), TestData.Packages.monthly)

        verify(exactly = 1) { listener.onPurchaseCompleted(customerInfo, transaction) }
        assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.PASSED)
        assertThat(dismissals).isEqualTo(1)
        assertThat(vm.purchaseCompleted.value).isTrue()
        assertThat(vm.workflowState.value).isNull()
        assertThat(events.filterIsInstance<WorkflowEvent.StepCompleted>().map { it.stepId })
            .containsExactly("step-1")
        assertThat(events.filterIsInstance<WorkflowEvent.Close>()).isEmpty()
    }

    @Test
    fun `RevenueCat unrelated purchase succeeds but keeps hard gate unresolved`() = runTest {
        val customerInfo = gatedCustomerInfo("unrelated")
        val transaction = mockk<StoreTransaction>()
        coEvery { purchases.awaitPurchase(any()) } returns PurchaseResult(transaction, customerInfo)
        val listener = makeListener()
        val outcomes = mutableListOf<CheckpointGateOutcome>()
        val events = mutableListOf<FeatureEvent>()
        every { purchases.track(capture(events)) } just Runs
        var dismissals = 0
        val vm = createVm(
            dismissRequest = { dismissals++ }, listener = listener,
            gateContext = CheckpointGateContext("customer-a", emptySet(), outcomes::add),
        )
        vm.startHardGate()

        vm.handlePackagePurchase(mockk<Activity>(), TestData.Packages.monthly)

        verify(exactly = 1) { listener.onPurchaseCompleted(customerInfo, transaction) }
        assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.NOT_PASSED)
        assertThat(dismissals).isZero()
        assertThat(vm.purchaseCompleted.value).isTrue()
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
        assertThat(events.filterIsInstance<WorkflowEvent.StepCompleted>()).isEmpty()
        vm.closePaywall()
        assertThat(events.filterIsInstance<WorkflowEvent.Close>()).hasSize(1)
    }

    @Test
    fun `RevenueCat qualifying restore passes hard gate without display predicate`() = runTest {
        val customerInfo = gatedCustomerInfo("pro")
        coEvery { purchases.awaitRestore() } returns customerInfo
        val listener = makeListener()
        val outcomes = mutableListOf<CheckpointGateOutcome>()
        val events = mutableListOf<FeatureEvent>()
        every { purchases.track(capture(events)) } just Runs
        var dismissals = 0
        val vm = createVm(
            dismissRequest = { dismissals++ }, listener = listener,
            gateContext = CheckpointGateContext("customer-a", emptySet(), outcomes::add),
        )
        vm.startHardGate()

        vm.handleRestorePurchases()

        verify(exactly = 1) { listener.onRestoreCompleted(customerInfo) }
        assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.PASSED)
        assertThat(dismissals).isEqualTo(1)
        assertThat(vm.workflowState.value).isNull()
        assertThat(events.filterIsInstance<WorkflowEvent.StepCompleted>().map { it.stepId })
            .containsExactly("step-1")
        assertThat(events.filterIsInstance<WorkflowEvent.Close>()).isEmpty()
    }

    @Test
    fun `RevenueCat unrelated restore succeeds but does not complete hard gate session`() = runTest {
        val customerInfo = gatedCustomerInfo("unrelated")
        coEvery { purchases.awaitRestore() } returns customerInfo
        val listener = makeListener()
        val outcomes = mutableListOf<CheckpointGateOutcome>()
        val events = mutableListOf<FeatureEvent>()
        every { purchases.track(capture(events)) } just Runs
        var dismissals = 0
        val vm = createVm(
            dismissRequest = { dismissals++ }, listener = listener,
            gateContext = CheckpointGateContext("customer-a", emptySet(), outcomes::add),
        )
        vm.startHardGate()

        vm.handleRestorePurchases()

        verify(exactly = 1) { listener.onRestoreCompleted(customerInfo) }
        assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.NOT_PASSED)
        assertThat(dismissals).isZero()
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
        assertThat(events.filterIsInstance<WorkflowEvent.StepCompleted>()).isEmpty()
        vm.closePaywall()
        assertThat(events.filterIsInstance<WorkflowEvent.Close>()).hasSize(1)
    }

    @Test
    fun `RevenueCat purchase from older presentation cannot complete replacement hard gate`() = runTest {
        assertOldCompletionCannotPassGate(isPurchase = true)
    }

    @Test
    fun `RevenueCat restore from older presentation cannot complete replacement hard gate`() = runTest {
        assertOldCompletionCannotPassGate(isPurchase = false)
    }

    private suspend fun TestScope.assertOldCompletionCannotPassGate(
        isPurchase: Boolean,
        myApp: Boolean = false,
    ) {
        val result = CompletableDeferred<CustomerInfo>()
        val transaction = mockk<StoreTransaction>()
        coEvery { purchases.awaitPurchase(any()) } coAnswers { PurchaseResult(transaction, result.await()) }
        coEvery { purchases.awaitRestore() } coAnswers { result.await() }
        coEvery { purchases.awaitSyncPurchases() } coAnswers { result.await() }
        if (myApp) every { purchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.MY_APP
        val listener = makeListener()
        val outcomes = mutableListOf<CheckpointGateOutcome>()
        val events = mutableListOf<FeatureEvent>()
        every { purchases.track(capture(events)) } just Runs
        var dismissals = 0
        val vm = createVm(
            dismissRequest = { dismissals++ }, listener = listener,
            gateContext = CheckpointGateContext("customer-a", emptySet(), outcomes::add),
            purchaseLogic = if (myApp) successfulPurchaseLogic else null,
        )
        vm.startHardGate()
        val operation = launch {
            if (isPurchase) vm.handlePackagePurchase(mockk<Activity>(), TestData.Packages.monthly)
            else vm.handleRestorePurchases()
        }
        runCurrent()
        vm.closePaywall()
        vm.startHardGate()
        events.clear()
        outcomes.clear()
        val customerInfo = gatedCustomerInfo("pro")
        result.complete(customerInfo)
        operation.join()

        if (!myApp) {
            if (isPurchase) verify(exactly = 1) { listener.onPurchaseCompleted(customerInfo, transaction) }
            else verify(exactly = 1) { listener.onRestoreCompleted(customerInfo) }
        }
        assertThat(dismissals).isEqualTo(1)
        assertThat(outcomes).doesNotContain(CheckpointGateOutcome.PASSED)
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
        assertThat(events.filterIsInstance<WorkflowEvent.StepCompleted>()).isEmpty()
        vm.closePaywall()
        assertThat(events.filterIsInstance<WorkflowEvent.Close>()).hasSize(1)
    }

    @Test
    fun `purchase errors and cancellation do not pass hard gate`() = runTest {
        val listener = makeListener()
        val outcomes = mutableListOf<CheckpointGateOutcome>()
        var dismissals = 0
        val vm = createVm(
            dismissRequest = { dismissals++ }, listener = listener,
            gateContext = CheckpointGateContext("customer-a", emptySet(), outcomes::add),
        )
        vm.startHardGate()
        val error = PurchasesError(PurchasesErrorCode.NetworkError)
        coEvery { purchases.awaitPurchase(any()) } throws PurchasesException(error)
        vm.handlePackagePurchase(mockk<Activity>(), TestData.Packages.monthly)
        coEvery { purchases.awaitPurchase(any()) } throws PurchasesException(
            PurchasesError(PurchasesErrorCode.PurchaseCancelledError),
        )
        vm.handlePackagePurchase(mockk<Activity>(), TestData.Packages.monthly)

        verify(exactly = 1) { listener.onPurchaseError(error) }
        verify(exactly = 1) { listener.onPurchaseCancelled() }
        assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.NOT_PASSED)
        assertThat(dismissals).isZero()
        assertThat(vm.purchaseCompleted.value).isFalse()
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
    }

    @Test
    fun `restore error does not pass hard gate`() = runTest {
        val listener = makeListener()
        val outcomes = mutableListOf<CheckpointGateOutcome>()
        var dismissals = 0
        val vm = createVm(
            dismissRequest = { dismissals++ }, listener = listener,
            gateContext = CheckpointGateContext("customer-a", emptySet(), outcomes::add),
        )
        vm.startHardGate()
        val error = PurchasesError(PurchasesErrorCode.NetworkError)
        coEvery { purchases.awaitRestore() } throws PurchasesException(error)
        vm.handleRestorePurchases()

        verify(exactly = 1) { listener.onRestoreError(error) }
        assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.NOT_PASSED)
        assertThat(dismissals).isZero()
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
    }

    @Test
    fun `web checkout notification does not pass hard gate`() {
        val outcomes = mutableListOf<CheckpointGateOutcome>()
        var dismissals = 0
        var notifications = 0
        val listener = object : PaywallListener {
            override fun onWebCheckoutOpened() { notifications++ }
        }
        val vm = createVm(
            dismissRequest = { dismissals++ }, listener = listener,
            gateContext = CheckpointGateContext("customer-a", emptySet(), outcomes::add),
        )
        vm.startHardGate()
        vm.notifyWebCheckoutOpened()

        assertThat(notifications).isEqualTo(1)
        assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.NOT_PASSED)
        assertThat(dismissals).isZero()
        assertThat(vm.purchaseCompleted.value).isFalse()
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
    }

    private val successfulPurchaseLogic = object : PaywallPurchaseLogic {
        override suspend fun performPurchase(activity: Activity, params: PaywallPurchaseLogicParams) =
            PurchaseLogicResult.Success
        override suspend fun performRestore(customerInfo: CustomerInfo) = PurchaseLogicResult.Success
    }

    @Test
    fun `MY_APP qualifying purchase passes hard gate and completes visible step once`() = runTest {
        assertMyAppGateCompletion(isPurchase = true, entitlement = "pro", shouldPass = true)
    }

    @Test
    fun `MY_APP unrelated purchase succeeds but keeps hard gate unresolved`() = runTest {
        assertMyAppGateCompletion(isPurchase = true, entitlement = "unrelated", shouldPass = false)
    }

    @Test
    fun `MY_APP qualifying restore passes hard gate without display predicate`() = runTest {
        assertMyAppGateCompletion(isPurchase = false, entitlement = "pro", shouldPass = true)
    }

    @Test
    fun `MY_APP unrelated restore succeeds but does not complete hard gate session`() = runTest {
        assertMyAppGateCompletion(isPurchase = false, entitlement = "unrelated", shouldPass = false)
    }

    private suspend fun assertMyAppGateCompletion(isPurchase: Boolean, entitlement: String, shouldPass: Boolean) {
        every { purchases.purchasesAreCompletedBy } returns PurchasesAreCompletedBy.MY_APP
        coEvery { purchases.awaitSyncPurchases() } returns gatedCustomerInfo(entitlement)
        val outcomes = mutableListOf<CheckpointGateOutcome>()
        val events = mutableListOf<FeatureEvent>()
        every { purchases.track(capture(events)) } just Runs
        var dismissals = 0
        val vm = createVm(
            dismissRequest = { dismissals++ }, purchaseLogic = successfulPurchaseLogic,
            gateContext = CheckpointGateContext("customer-a", emptySet(), outcomes::add),
        )
        vm.startHardGate()
        if (isPurchase) vm.handlePackagePurchase(mockk<Activity>(), TestData.Packages.monthly)
        else vm.handleRestorePurchases()

        assertThat(vm.purchaseCompleted.value).isEqualTo(isPurchase)
        if (shouldPass) {
            assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.PASSED)
            assertThat(dismissals).isEqualTo(1)
            assertThat(vm.workflowState.value).isNull()
            assertThat(events.filterIsInstance<WorkflowEvent.StepCompleted>().map { it.stepId })
                .containsExactly("step-1")
            assertThat(events.filterIsInstance<WorkflowEvent.Close>()).isEmpty()
        } else {
            assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.NOT_PASSED)
            assertThat(dismissals).isZero()
            assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
            assertThat(events.filterIsInstance<WorkflowEvent.StepCompleted>()).isEmpty()
            vm.closePaywall()
            assertThat(events.filterIsInstance<WorkflowEvent.Close>()).hasSize(1)
        }
    }

    @Test
    fun `MY_APP purchase from older presentation cannot complete replacement hard gate`() = runTest {
        assertOldCompletionCannotPassGate(isPurchase = true, myApp = true)
    }

    @Test
    fun `MY_APP restore from older presentation cannot complete replacement hard gate`() = runTest {
        assertOldCompletionCannotPassGate(isPurchase = false, myApp = true)
    }

    @Test
    fun `restore display predicate cannot bypass an unresolved hard gate`() = runTest {
        coEvery { purchases.awaitRestore() } returns gatedCustomerInfo("unrelated")
        var dismissals = 0
        val outcomes = mutableListOf<CheckpointGateOutcome>()
        val vm = createVm(
            dismissRequest = { dismissals++ }, shouldDisplayBlock = { false },
            gateContext = CheckpointGateContext("customer-a", emptySet(), outcomes::add),
        )
        vm.startHardGate()
        vm.handleRestorePurchases()

        assertThat(dismissals).isZero()
        assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.NOT_PASSED)
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
    }

    @Test
    fun `operation gate outcome callback cannot dismiss replacement presentation`() = runTest {
        coEvery { purchases.awaitRestore() } returns gatedCustomerInfo("pro")
        var dismissals = 0
        lateinit var vm: PaywallViewModelImpl
        vm = createVm(
            dismissRequest = { dismissals++ },
            gateContext = CheckpointGateContext("customer-a", emptySet()) { outcome ->
                if (outcome == CheckpointGateOutcome.PASSED) {
                    vm.startWorkflowPresentationFromResult(workflow, testOfferings, null, uiConfig)
                }
            },
        )
        vm.startHardGate()
        vm.handleRestorePurchases()

        assertThat(dismissals).isZero()
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
    }

    @Test
    fun `successful soft restore listener can close without reporting workflow abandonment`() = runTest {
        coEvery { purchases.awaitRestore() } returns mockk<CustomerInfo>()
        val events = mutableListOf<FeatureEvent>()
        every { purchases.track(capture(events)) } just Runs
        lateinit var vm: PaywallViewModelImpl
        val listener = object : PaywallListener {
            override fun onRestoreCompleted(customerInfo: CustomerInfo) { vm.closePaywall() }
        }
        vm = createVm(listener = listener)
        vm.startWorkflowPresentationFromResult(workflow, testOfferings, null, uiConfig)
        vm.handleRestorePurchases()

        assertThat(events.filterIsInstance<WorkflowEvent.Close>()).isEmpty()
        assertThat(events.filterIsInstance<WorkflowEvent.StepCompleted>().map { it.stepId })
            .containsExactly("step-1")
    }

    @Test
    fun `RevenueCat restore display predicate cannot dismiss replacement hard gate`() = runTest {
        assertRestorePredicateCannotDismissReplacement(PurchasesAreCompletedBy.REVENUECAT)
    }

    @Test
    fun `MY_APP restore display predicate cannot dismiss replacement hard gate`() = runTest {
        assertRestorePredicateCannotDismissReplacement(PurchasesAreCompletedBy.MY_APP)
    }

    private suspend fun assertRestorePredicateCannotDismissReplacement(completedBy: PurchasesAreCompletedBy) {
        every { purchases.purchasesAreCompletedBy } returns completedBy
        val customerInfo = gatedCustomerInfo("unrelated")
        coEvery { purchases.awaitRestore() } returns customerInfo
        coEvery { purchases.awaitSyncPurchases() } returns customerInfo
        val listener = makeListener()
        val outcomes = mutableListOf<CheckpointGateOutcome>()
        val events = mutableListOf<FeatureEvent>()
        every { purchases.track(capture(events)) } just Runs
        var dismissals = 0
        var predicateCalls = 0
        lateinit var vm: PaywallViewModelImpl
        vm = createVm(
            dismissRequest = { dismissals++ }, listener = listener,
            purchaseLogic = if (completedBy == PurchasesAreCompletedBy.MY_APP) successfulPurchaseLogic else null,
            gateContext = CheckpointGateContext("customer-a", emptySet(), outcomes::add),
            shouldDisplayBlock = {
                predicateCalls++
                vm.closePaywall()
                vm.startHardGate()
                events.clear()
                false
            },
        )
        vm.startWorkflowPresentationFromResult(workflow, testOfferings, null, uiConfig)
        vm.handleRestorePurchases()

        if (completedBy == PurchasesAreCompletedBy.REVENUECAT) {
            verify(exactly = 1) { listener.onRestoreCompleted(customerInfo) }
        }
        assertThat(predicateCalls).isEqualTo(1)
        assertThat(dismissals).isEqualTo(1)
        assertThat(vm.purchaseCompleted.value).isFalse()
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
        assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.NOT_PASSED)
        assertThat(events.filterIsInstance<WorkflowEvent.StepCompleted>()).isEmpty()
        assertThat(events.filterIsInstance<WorkflowEvent.Close>()).isEmpty()
        vm.closePaywall()
        assertThat(events.filterIsInstance<WorkflowEvent.Close>()).hasSize(1)
    }

    @Test
    fun `satisfied initial hard gate dismisses without rendering or step and paywall events`() {
        var dismissals = 0
        val events = mutableListOf<FeatureEvent>()
        every { purchases.track(capture(events)) } just Runs
        val outcomes = mutableListOf<CheckpointGateOutcome>()
        val vm = createVm(
            dismissRequest = { dismissals++ },
            gateContext = CheckpointGateContext("customer-a", setOf("premium"), outcomes::add),
        )
        vm.startWorkflowPresentationFromResult(
            workflow.copy(steps = mapOf(step1.id to step1.withHardGate(), step2.id to step2)),
            testOfferings, null, uiConfig,
        )
        vm.trackPaywallImpressionIfNeeded()
        assertThat(dismissals).isEqualTo(1)
        assertThat(vm.workflowState.value).isNull()
        assertThat(events).isEmpty()
        assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.PASSED)
    }

    @Test
    fun `unsatisfied initial hard gate renders and reports not passed`() {
        val outcomes = mutableListOf<CheckpointGateOutcome>()
        val vm = createVm(gateContext = CheckpointGateContext("customer-a", null, outcomes::add))
        vm.startWorkflowPresentationFromResult(
            workflow.copy(steps = mapOf(step1.id to step1.withHardGate(), step2.id to step2)),
            testOfferings, null, uiConfig,
        )
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
        assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.NOT_PASSED)
    }

    @Test
    fun `satisfied later hard gate completes only the prior visible step`() {
        var dismissals = 0
        val events = mutableListOf<FeatureEvent>()
        every { purchases.track(capture(events)) } just Runs
        val vm = createVm(
            dismissRequest = { dismissals++ },
            gateContext = CheckpointGateContext("customer-a", setOf("pro")) {},
        )
        vm.startWorkflowPresentationFromResult(
            workflow.copy(steps = mapOf(step1.id to step1, step2.id to step2.withHardGate())),
            testOfferings, null, uiConfig,
        )
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        assertThat(dismissals).isEqualTo(1)
        assertThat(vm.workflowState.value).isNull()
        assertThat(events.filterIsInstance<WorkflowEvent.StepStarted>().map { it.stepId }).containsExactly("step-1")
        assertThat(events.filterIsInstance<WorkflowEvent.StepCompleted>().map { it.stepId }).containsExactly("step-1")
        assertThat(events.filterIsInstance<WorkflowEvent.Close>()).isEmpty()
        assertThat(events.filterIsInstance<PaywallEvent>()).isEmpty()
    }

    @Test
    fun `invalid gate fails closed like a hard gate that never passes`() {
        val outcomes = mutableListOf<CheckpointGateOutcome>()
        val invalidStep = step1.copy(paramValues = step1.paramValues + ("gating" to JsonNull))
        val vm = createVm(gateContext = CheckpointGateContext("customer-a", setOf("pro")) { outcomes += it })
        val invalidWorkflow = workflow.copy(steps = mapOf(step1.id to invalidStep, step2.id to step2))
        vm.startWorkflowPresentationFromResult(invalidWorkflow, testOfferings, null, uiConfig)
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
        assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.NOT_PASSED)
    }

    @Test
    fun `blocked initial hard step ignores forward triggers`() {
        val vm = createVm(gateContext = CheckpointGateContext("customer-a", emptySet()) {})
        vm.startWorkflowPresentationFromResult(
            workflow.copy(steps = mapOf(step1.id to step1.withHardGate(), step2.id to step2)),
            testOfferings, null, uiConfig,
        )
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
    }

    @Test
    fun `close on blocked initial step dismisses as navigated back without exit offer`() {
        val reasons = mutableListOf<PaywallDismissReason>()
        val exitOfferings = mutableListOf<Offering?>()
        val vm = createVm(
            dismissRequestWithExitOffering = { offering, _, reason -> exitOfferings.add(offering); reasons.add(reason) },
            gateContext = CheckpointGateContext("customer-a", null) {},
        )
        vm.startWorkflowPresentationFromResult(
            workflow.copy(steps = mapOf(step1.id to step1.withHardGate(), step2.id to step2)),
            testOfferings, null, uiConfig,
        )
        vm.closePaywall()
        assertThat(reasons).containsExactly(PaywallDismissReason.NAVIGATED_BACK)
        assertThat(exitOfferings).containsExactly(null)
        assertThat(vm.workflowState.value).isNull()
    }

    @Test
    fun `close on blocked later step goes back and closing earlier cannot bypass gate`() {
        val reasons = mutableListOf<PaywallDismissReason>()
        val outcomes = mutableListOf<CheckpointGateOutcome>()
        val vm = createVm(
            dismissRequestWithExitOffering = { _, _, reason -> reasons.add(reason) },
            gateContext = CheckpointGateContext("customer-a", emptySet(), outcomes::add),
        )
        vm.startWorkflowPresentationFromResult(
            workflow.copy(steps = mapOf(step1.id to step1, step2.id to step2.withHardGate())),
            testOfferings, null, uiConfig,
        )
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        vm.closePaywall()
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
        assertThat(reasons).isEmpty()
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-2")
        vm.closePaywall()
        vm.closePaywall()
        assertThat(reasons).containsExactly(PaywallDismissReason.NAVIGATED_BACK)
        assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.NOT_PASSED)
    }

    @Test
    fun `system back on a blocked gate stays available and preserves the unresolved outcome`() {
        val outcomes = mutableListOf<CheckpointGateOutcome>()
        val vm = createVm(gateContext = CheckpointGateContext("customer-a", emptySet(), outcomes::add))
        vm.startWorkflowPresentationFromResult(
            workflow.copy(steps = mapOf(step1.id to step1, step2.id to step2.withHardGate())),
            testOfferings, null, uiConfig,
        )
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        assertThat(vm.handleBackNavigation()).isTrue()
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
        assertThat(vm.handleBackNavigation()).isFalse()
        assertThat(outcomes.last()).isEqualTo(CheckpointGateOutcome.NOT_PASSED)
    }

    @Test
    fun `gate completion callback cannot dismiss a replacement presentation`() {
        var dismissals = 0
        lateinit var vm: PaywallViewModelImpl
        vm = createVm(
            dismissRequest = { dismissals++ },
            gateContext = CheckpointGateContext("customer-a", setOf("pro")) { outcome ->
                if (outcome == CheckpointGateOutcome.PASSED) {
                    vm.startWorkflowPresentationFromResult(workflow, testOfferings, null, uiConfig)
                }
            },
        )
        vm.startWorkflowPresentationFromResult(
            workflow.copy(steps = mapOf(step1.id to step1.withHardGate(), step2.id to step2)),
            testOfferings, null, uiConfig,
        )
        assertThat(dismissals).isZero()
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
    }

    // region forward navigation

    @Test
    fun `forward navigation sets FORWARD on pendingTransition`() {
        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)

        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)

        assertThat(vm.workflowState.value?.pendingTransition?.direction)
            .isEqualTo(NavigationDirection.FORWARD)
    }

    @Test
    fun `forward navigation sets fromStepId to the step navigated away from`() {
        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)

        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)

        assertThat(vm.workflowState.value?.pendingTransition?.fromStepId).isEqualTo("step-1")
    }

    // endregion

    // region back navigation

    @Test
    fun `back navigation sets BACKWARD on pendingTransition`() {
        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        vm.onTransitionComplete(vm.workflowState.value!!.pendingTransition!!.id)

        vm.handleBackNavigation()

        assertThat(vm.workflowState.value?.pendingTransition?.direction)
            .isEqualTo(NavigationDirection.BACKWARD)
    }

    @Test
    fun `back navigation sets fromStepId to the step navigated away from`() {
        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)

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
    fun `each step's state carries the identity of the step it renders`() {
        val wfl = workflow.copy(
            steps = mapOf(
                "step-1" to step1.copy(metadata = screenTypeMetadata(WorkflowScreenType.PAYWALL)),
                "step-2" to step2.copy(type = "survey"),
            ),
        )
        val vm = createVm()
        vm.startWorkflowPresentationFromResult(wfl, testOfferings, null, uiConfig)

        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        vm.onTransitionComplete(vm.workflowState.value!!.pendingTransition!!.id)

        assertThat(vm.workflowScreenOf("step-1")).isEqualTo(
            WorkflowScreenContext(
                workflowId = "wfl-test",
                stepId = "step-1",
                stepType = "screen",
                screenType = listOf(WorkflowScreenType.PAYWALL),
            ),
        )
        assertThat(vm.workflowScreenOf("step-2")).isEqualTo(
            WorkflowScreenContext(
                workflowId = "wfl-test",
                stepId = "step-2",
                stepType = "survey",
                screenType = null,
            ),
        )
    }

    @Test
    fun `back navigation returns step state with the selection the user left on it`() {
        val (fetchResult2, offerings2) = makeTwoPackageWorkflow()
        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult2, offerings2, null, uiConfig)

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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)

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
        vmWithVars.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)

        vmWithVars.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)

        val step2State = vmWithVars.workflowState.value?.stepStates?.get("step-2")
        assertThat(step2State).isNotNull()
        assertThat(
            (step2State!!.mergedCustomVariables["highlight_color"] as? CustomVariableValue.String)?.value,
        ).isEqualTo("gold")
    }

    @Test
    fun `swapping workflow sessions before the previous prewarm starts does not pollute the new cache`() = runTest {
        val vm = createVm()

        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        val staleStore = (vm.state.value as PaywallState.Loaded.Components).stateStore

        // A second session starts before the first session's prewarm job has run at all: cancel() marks the
        // still-unscheduled job so its body (and any writes tied to staleStore) never executes.
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        val currentStore = (vm.state.value as PaywallState.Loaded.Components).stateStore
        assertThat(currentStore).isNotSameAs(staleStore)

        advanceUntilIdle()

        val cachedStep2Store = vm.workflowState.value?.stepStates?.get("step-2")?.stateStore
        assertThat(cachedStep2Store).isSameAs(currentStore)
    }

    // endregion

    // region onTransitionComplete

    @Test
    fun `onTransitionComplete clears pendingTransition for the matching id`() {
        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        val id = vm.workflowState.value!!.pendingTransition!!.id

        vm.onTransitionComplete(id)

        assertThat(vm.workflowState.value?.pendingTransition).isNull()
    }

    @Test
    fun `onTransitionComplete with stale id does not clear a newer transition`() {
        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
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

        vm.startWorkflowPresentationFromResult(result, offerings, null, uiConfig)

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
        vm.startWorkflowPresentationFromResult(result, offerings, null, uiConfig)

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
            paramValues = offeringParams(threeStepOfferingId),
        )
        val threeStepWorkflow = PublishedWorkflow(
            id = "wfl-three-step",
            displayName = "Three Step",
            initialStepId = "step-A",
            steps = mapOf("step-A" to stepA, "step-B" to stepB, "step-C" to stepC),
            screens = mapOf(screenAId to earlyScreen1, screenBId to earlyScreen2, screenCId to terminalScreen3),
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
        val wflResult = threeStepWorkflow

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(wflResult, threeStepOfferings, null, uiConfig)

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
        vm.startWorkflowPresentationFromResult(result, offerings, null, uiConfig)

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
        val brokenResult = result.copy(singleStepFallbackId = "non-existent-step")
        val vm = createVm()

        vm.startWorkflowPresentationFromResult(brokenResult, offerings, null, uiConfig)

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
        val singleFallbackResult = twoPackageResult.copy(singleStepFallbackId = "step-1")
        val vm = createVm()

        vm.startWorkflowPresentationFromResult(singleFallbackResult, twoPackageOfferings, null, uiConfig)

        val step1State = vm.workflowState.value?.stepStates?.get("step-1")
        assertThat(step1State).isNotNull()
        // monthlyPackageComponentDefault has isSelectedByDefault = true → MONTHLY wins.
        assertThat(step1State!!.selectedPackageInfo?.rcPackage?.identifier)
            .isEqualTo(PackageType.MONTHLY.identifier)
    }

    // endregion

    // region memory-first single-path (no Loading flash)

    @Test
    fun `Unavailable workflow resolution falls back to the default paywall instead of erroring`() = runTest {
        // This models a workflow offering whose paywall data was skipped during the workflows-enabled parse,
        // with no cached workflows topic available.
        coEvery { purchases.resolveWorkflow(offeringId) } returns WorkflowResolution.Unavailable

        val vm = createVm()
        advanceUntilIdle()

        assertThat(vm.state.value).isInstanceOf(PaywallState.Loaded.Legacy::class.java)
        assertThat(vm.state.value).isNotInstanceOf(PaywallState.Error::class.java)
        assertThat(vm.workflowState.value).isNull()
        coVerify(exactly = 0) { purchases.awaitGetWorkflow(any()) }
    }

    @Test
    fun `an offering whose topic workflow is an offering step renders its own paywall`() = runTest {
        // A checkpoint's terminal offering workflow is mapped to its offering in the workflows topic, but it has
        // no screen to render: presenting the offering must not try to render it as a UI workflow.
        val offeringStep = WorkflowStep(id = "offering-step", type = "offering")
        val offeringWorkflow = PublishedWorkflow(
            id = "wfl-offering",
            displayName = "Offering",
            initialStepId = offeringStep.id,
            steps = mapOf(offeringStep.id to offeringStep),
            screens = emptyMap(),
            metadata = emptyMap(),
            singleStepFallbackId = null,
        )
        coEvery { purchases.resolveWorkflow(offeringId) } returns WorkflowResolution.Found(offeringWorkflow.id)
        coEvery { purchases.awaitGetWorkflow(offeringWorkflow.id) } returns offeringWorkflow
        // Fetched alongside the workflow body; the fallback decision cancels them.
        coEvery { purchases.awaitGetUiConfig() } returns uiConfig
        coEvery { purchases.awaitOfferings() } returns testOfferings

        val vm = createVm()
        advanceUntilIdle()

        assertThat(vm.state.value).isInstanceOf(PaywallState.Loaded.Legacy::class.java)
        assertThat(vm.state.value).isNotInstanceOf(PaywallState.Error::class.java)
        assertThat(vm.workflowState.value).isNull()
    }

    @Test
    fun `warm cache renders the workflow on the first composition with no Loading state`() {
        // Model Dispatchers.Main.immediate with an unconfined main dispatcher so viewModelScope.launch runs
        // eagerly, as in production. On a warm cache every suspend read resolves without suspending (mockk
        // stubs don't suspend), so the single presentWorkflow path completes inside init and sets Loaded
        // before the first composition would read state — no LoadingPaywall frame.
        val immediateMain = UnconfinedTestDispatcher()
        Dispatchers.setMain(immediateMain)
        try {
            coEvery { purchases.resolveWorkflow(offeringId) } returns WorkflowResolution.Found(workflow.id)
            coEvery { purchases.awaitGetWorkflow(workflow.id) } returns fetchResult
            coEvery { purchases.awaitGetUiConfig() } returns uiConfig
            coEvery { purchases.awaitOfferings() } returns testOfferings

            val vm = createVm()

            assertThat(vm.state.value).isInstanceOf(PaywallState.Loaded.Components::class.java)
            assertThat(vm.workflowState.value).isNotNull
            // It is the same single suspend path — the reads are invoked, they just resolve synchronously.
            coVerify(exactly = 1) { purchases.awaitGetWorkflow(workflow.id) }
        } finally {
            Dispatchers.setMain(testDispatcher)
        }
    }

    @Test
    fun `warm cache pre-warm does not write step state inside the composition snapshot`() {
        // Real Looper-backed Main, so viewModelScope behaves as on a device.
        Dispatchers.resetMain()
        val uncaught = mutableListOf<Throwable>()
        val mainThread = Thread.currentThread()
        val previousHandler = mainThread.uncaughtExceptionHandler
        mainThread.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e -> uncaught += e }
        val backgroundDispatcher = FinishBeforeReturnDispatcher()
        try {
            coEvery { purchases.resolveWorkflow(offeringId) } returns WorkflowResolution.Found(workflow.id)
            coEvery { purchases.awaitGetWorkflow(workflow.id) } returns fetchResult
            coEvery { purchases.awaitGetUiConfig() } returns uiConfig

            // Mirrors Recomposer.composing(): viewModel() constructs the VM inside a mutable snapshot.
            val snapshot = Snapshot.takeMutableSnapshot()
            val vm = snapshot.enter {
                PaywallViewModelImpl(
                    resourceProvider = MockResourceProvider(),
                    purchases = purchases,
                    options = PaywallOptions.Builder(dismissRequest = {}).build(),
                    colorScheme = TestData.Constants.currentColorScheme,
                    isDarkMode = false,
                    shouldDisplayBlock = null,
                    backgroundDispatcher = backgroundDispatcher,
                )
            }
            snapshot.apply().check()
            snapshot.dispose()
            shadowOf(Looper.getMainLooper()).idle()

            assertThat(uncaught).isEmpty()
            assertThat(vm.workflowState.value?.stepStates?.keys).containsExactlyInAnyOrder("step-1", "step-2")
        } finally {
            mainThread.uncaughtExceptionHandler = previousHandler
            backgroundDispatcher.close()
            Dispatchers.setMain(testDispatcher)
        }
    }

    @Test
    fun `a cold cache shows Loading then the loaded workflow after the async read`() = runTest {
        // A cold read really suspends; the same single path shows Loading until it resolves.
        val gate = CompletableDeferred<Unit>()
        coEvery { purchases.resolveWorkflow(any()) } returns WorkflowResolution.Found(workflow.id)
        coEvery { purchases.awaitGetWorkflow(any()) } coAnswers { gate.await(); fetchResult }
        coEvery { purchases.awaitGetUiConfig() } returns uiConfig
        coEvery { purchases.awaitOfferings() } returns testOfferings

        val vm = createVm()
        advanceUntilIdle()
        assertThat(vm.state.value).isEqualTo(PaywallState.Loading)

        gate.complete(Unit)
        advanceUntilIdle()
        assertThat(vm.state.value).isInstanceOf(PaywallState.Loaded.Components::class.java)
    }

    // endregion

    // region exit offers

    @Test
    fun `preloadExitOffering reads exit offer from singleStepFallbackId step`() = runTest {
        coEvery { purchases.awaitOfferings() } returns testOfferingsWithExitOffer

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResultWithExitOffer, testOfferingsWithExitOffer, null, uiConfig)
        vm.preloadExitOffering()
        advanceUntilIdle()

        assertThat(vm.preloadedExitOffering?.identifier).isEqualTo(exitOfferingId)
    }

    @Test
    fun `preloadExitOffering without singleStepFallbackId does not set preloaded offering`() = runTest {
        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        vm.preloadExitOffering()
        advanceUntilIdle()

        assertThat(vm.preloadedExitOffering).isNull()
    }

    @Test
    fun `preloadExitOffering with unknown exit offering id leaves preloaded offering null`() = runTest {
        // testOfferings does NOT include exitOfferingId
        coEvery { purchases.awaitOfferings() } returns testOfferings

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResultWithExitOffer, testOfferings, null, uiConfig)
        vm.preloadExitOffering()
        advanceUntilIdle()

        assertThat(vm.preloadedExitOffering).isNull()
    }

    @Test
    fun `preloadExitOffering not-found result is not re-attempted when same offerings are re-set`() = runTest {
        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResultWithExitOffer, testOfferings, null, uiConfig)
        vm.preloadExitOffering()
        assertThat(vm.preloadedExitOffering).isNull()

        // Simulate locale/colour/options refresh pushing the same workflow data again.
        vm.startWorkflowPresentationFromResult(fetchResultWithExitOffer, testOfferings, null, uiConfig)

        // Still null — carry-forward prevented a redundant lookup and a duplicate error log.
        assertThat(vm.preloadedExitOffering).isNull()
    }

    @Test
    fun `preloadExitOffering re-resolves when offerings are refreshed and now contain the exit offering`() = runTest {
        val vm = createVm()
        // First update: exit offering absent.
        vm.startWorkflowPresentationFromResult(fetchResultWithExitOffer, testOfferings, null, uiConfig)
        vm.preloadExitOffering()
        assertThat(vm.preloadedExitOffering).isNull()

        // Second update: fresh offerings that now include the exit offering.
        vm.startWorkflowPresentationFromResult(fetchResultWithExitOffer, testOfferingsWithExitOffer, null, uiConfig)

        assertThat(vm.preloadedExitOffering?.identifier).isEqualTo(exitOfferingId)
    }

    @Test
    fun `closePaywall with preloaded exit offer calls dismissRequestWithExitOffering`() = runTest {
        coEvery { purchases.awaitOfferings() } returns testOfferingsWithExitOffer

        var receivedExitOffering: Offering? = null
        val vm = createVm(
            dismissRequestWithExitOffering = { offering, _, _ ->
                receivedExitOffering = offering
            },
        )
        vm.startWorkflowPresentationFromResult(fetchResultWithExitOffer, testOfferingsWithExitOffer, null, uiConfig)
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
            dismissRequestWithExitOffering = { offering, _, _ ->
                receivedExitOffering = offering
            },
        )
        vm.startWorkflowPresentationFromResult(fetchResultWithExitOffer, testOfferingsWithExitOffer, null, uiConfig)
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
        vm.startWorkflowPresentationFromResult(fetchResultWithExitOffer, testOfferingsWithExitOffer, null, uiConfig)
        advanceUntilIdle()

        assertThat(vm.preloadedExitOffering?.identifier).isEqualTo(exitOfferingId)
    }

    @Test
    fun `startWorkflowPresentationFromResult with same workflow data reloads preloaded exit offering`() = runTest {
        val vm = createVm()
        vm.preloadExitOffering()
        vm.startWorkflowPresentationFromResult(fetchResultWithExitOffer, testOfferingsWithExitOffer, null, uiConfig)
        assertThat(vm.preloadedExitOffering?.identifier).isEqualTo(exitOfferingId)

        vm.startWorkflowPresentationFromResult(fetchResultWithExitOffer, testOfferingsWithExitOffer, null, uiConfig)
        assertThat(vm.preloadedExitOffering?.identifier).isEqualTo(exitOfferingId)
    }

    @Test
    fun `preloadExitOffering uses singleStepFallbackId to locate exit offer`() = runTest {
        coEvery { purchases.awaitOfferings() } returns testOfferingsWithExitOffer

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResultWithFallback, testOfferingsWithExitOffer, null, uiConfig)
        vm.preloadExitOffering()
        advanceUntilIdle()

        assertThat(vm.preloadedExitOffering?.identifier).isEqualTo(exitOfferingId)
    }

    @Test
    fun `preloadExitOffering reads exit offer from single-step workflow`() = runTest {
        coEvery { purchases.awaitOfferings() } returns testOfferingsWithExitOffer

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(singleStepFetchResultWithExitOffer, testOfferingsWithExitOffer, null, uiConfig)
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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)

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
    fun `workflow load from a checkpoint offering uses the checkpoint trace id`() = runTest {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers {
            captured.add(firstArg())
        }
        val checkpointContext = testOfferings.all.values.first()
            .withPresentedCheckpointIdentifier("app_start", traceId = "hit_trace")
            .availablePackages.first().presentedOfferingContext

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, checkpointContext, uiConfig)
        advanceUntilIdle()

        assertThat(captured.filterIsInstance<WorkflowEvent.StepStarted>().single().traceId).isEqualTo("hit_trace")
        val loadedOffering = (vm.state.value as PaywallState.Loaded.Components).offering
        assertThat(loadedOffering.availablePackages.first().presentedOfferingContext.traceId).isEqualTo("hit_trace")
    }

    @Test
    fun `color scheme rebuild during workflow does not track another StepStarted`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
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
            metadata = emptyMap(),
            singleStepFallbackId = "step-2",
        )

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(
            workflowWithFailingInitialAndSuccessfulFallback,
            testOfferingsWithEmpty,
            null,
            uiConfig,
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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        captured.clear() // clear load event

        vm.closePaywall(result = null)

        val completed = captured.filterIsInstance<WorkflowEvent.StepCompleted>().single()
        assertThat(completed.stepId).isEqualTo("step-1")
        assertThat(completed.toStepId).isNull()
    }

    @Test
    fun `no experiment is reported without the workflow blob ref`() {
        val experimentStep1 = step1.copy(
            paramValues = mapOf(
                "experiment_id" to JsonPrimitive("exp_abc"),
                "experiment_variant" to JsonPrimitive("b"),
            ),
        )
        val experimentWorkflow = workflow.copy(steps = mapOf("step-1" to experimentStep1, "step-2" to step2))
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(experimentWorkflow, testOfferings, null, uiConfig)

        val started = captured.filterIsInstance<WorkflowEvent.StepStarted>().single()
        assertThat(started.experiment).isNull()
    }

    @Test
    fun `step events carry the step experiment data and the workflow blob ref`() {
        val experimentStep1 = step1.copy(
            paramValues = mapOf(
                "experiment_id" to JsonPrimitive("exp_abc"),
                "experiment_variant" to JsonPrimitive("b"),
            ),
        )
        val experimentWorkflow = workflow.copy(steps = mapOf("step-1" to experimentStep1, "step-2" to step2))
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(
            experimentWorkflow,
            testOfferings,
            null,
            uiConfig,
            workflowBlobRef = "blob-ref-1",
        )
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        vm.onTransitionComplete(vm.workflowState.value!!.pendingTransition!!.id)
        vm.handleBackNavigation()
        vm.closePaywall(result = null)

        val started = captured.filterIsInstance<WorkflowEvent.StepStarted>()
        val completed = captured.filterIsInstance<WorkflowEvent.StepCompleted>()
        val close = captured.filterIsInstance<WorkflowEvent.Close>().single()

        val step1Started = started.filter { it.stepId == "step-1" }
        assertThat(step1Started).hasSize(2)
        assertThat(step1Started.map { it.experiment?.experimentId }).containsOnly("exp_abc")
        assertThat(step1Started.map { it.experiment?.experimentVariant }).containsOnly("b")
        assertThat(step1Started.map { it.workflowBlobRef }).containsOnly("blob-ref-1")
        // step-1 completes twice: once on forward navigation, once from closePaywall.
        val step1Completed = completed.filter { it.stepId == "step-1" }
        assertThat(step1Completed).hasSize(2)
        assertThat(step1Completed.map { it.experiment?.experimentId }).containsOnly("exp_abc")
        assertThat(step1Completed.map { it.experiment?.experimentVariant }).containsOnly("b")
        assertThat(close.stepId).isEqualTo("step-1")
        assertThat(close.experiment?.experimentId).isEqualTo("exp_abc")
        assertThat(close.experiment?.experimentVariant).isEqualTo("b")
        assertThat(close.workflowBlobRef).isEqualTo("blob-ref-1")

        val step2Started = started.single { it.stepId == "step-2" }
        assertThat(step2Started.experiment).isNull()
        val step2Completed = completed.single { it.stepId == "step-2" }
        assertThat(step2Completed.experiment).isNull()
    }

    @Test
    fun `step events carry the workflow blob ref outside an experiment`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(
            fetchResult,
            testOfferings,
            null,
            uiConfig,
            workflowBlobRef = "blob-ref-1",
        )
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        vm.onTransitionComplete(vm.workflowState.value!!.pendingTransition!!.id)
        vm.closePaywall(result = null)

        val workflowEvents = captured.filterIsInstance<WorkflowEvent>()
        assertThat(workflowEvents).isNotEmpty
        workflowEvents.forEach { event ->
            val (blobRef, experiment) = when (event) {
                is WorkflowEvent.StepStarted -> event.workflowBlobRef to event.experiment
                is WorkflowEvent.StepCompleted -> event.workflowBlobRef to event.experiment
                is WorkflowEvent.Close -> event.workflowBlobRef to event.experiment
            }
            assertThat(blobRef).`as`(event::class.simpleName).isEqualTo("blob-ref-1")
            assertThat(experiment).`as`(event::class.simpleName).isNull()
        }
    }

    @Test
    fun `fallback copy events report the raw step id and the original step id`() {
        val copyStepId = "step-2~f"
        val entryStep = step1.copy(
            triggerActions = mapOf("action-next" to WorkflowTriggerAction.Step(stepId = copyStepId)),
        )
        val copyStep = step2.copy(
            id = copyStepId,
            metadata = JsonObject(mapOf("fallback_original_step_id" to JsonPrimitive("step-2"))),
        )
        val fallbackWorkflow = workflow.copy(steps = mapOf("step-1" to entryStep, copyStepId to copyStep))
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(
            fallbackWorkflow,
            testOfferings,
            null,
            uiConfig,
            workflowBlobRef = "blob-ref-1",
        )
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        vm.onTransitionComplete(vm.workflowState.value!!.pendingTransition!!.id)
        vm.closePaywall(result = null)

        val entryCompleted = captured.filterIsInstance<WorkflowEvent.StepCompleted>().first { it.stepId == "step-1" }
        assertThat(entryCompleted.toStepId).isEqualTo(copyStepId)
        assertThat(entryCompleted.fallbackOriginalStepId).isNull()

        val copyStarted = captured.filterIsInstance<WorkflowEvent.StepStarted>().single { it.stepId == copyStepId }
        assertThat(copyStarted.fromStepId).isEqualTo("step-1")
        assertThat(copyStarted.fallbackOriginalStepId).isEqualTo("step-2")
        assertThat(copyStarted.workflowBlobRef).isEqualTo("blob-ref-1")
        assertThat(copyStarted.experiment).isNull()

        val close = captured.filterIsInstance<WorkflowEvent.Close>().single()
        assertThat(close.stepId).isEqualTo(copyStepId)
        assertThat(close.fallbackOriginalStepId).isEqualTo("step-2")
    }

    @Test
    fun `closePaywall without a purchase fires workflows Close for the current step`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
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
        // step-1 is a context (non-paywall) step: paywall_close is suppressed, but workflow_close
        // is a workflow-level abandonment signal and must still fire.
        val (result, offerings) = makeContextPackageWorkflow()

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(result, offerings, null, uiConfig)
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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        assertThat(vm.workflowState.value).isNotNull()

        vm.closePaywall(result = null)

        assertThat(vm.workflowState.value).isNull()
    }

    @Test
    fun `dismissed retained ViewModel resets to Loading then reopens at initial step`() = runTest {
        coEvery { purchases.resolveWorkflow(offeringId) } returns WorkflowResolution.Found(workflow.id)
        coEvery { purchases.awaitGetWorkflow(workflow.id) } returns workflow
        coEvery { purchases.awaitGetUiConfig() } returns uiConfig

        val vm = createVm()
        advanceUntilIdle()
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")

        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-2")
        val renderedState = vm.state.value

        vm.closePaywall(result = null)
        assertThat(vm.state.value).isSameAs(renderedState)

        vm.onPaywallDismissed()
        assertThat(vm.state.value).isEqualTo(PaywallState.Loading)

        vm.onPaywallPresented()
        advanceUntilIdle()

        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
    }

    @Test
    fun `refresh requested during teardown cannot overwrite Loading after disposal`() = runTest {
        val refreshGate = CompletableDeferred<Unit>()
        coEvery { purchases.awaitOfferings() } coAnswers {
            refreshGate.await()
            Offerings(
                current = TestData.template1Offering,
                all = mapOf(TestData.template1Offering.identifier to TestData.template1Offering),
            )
        }
        val vm = PaywallViewModelImpl(
            resourceProvider = MockResourceProvider(),
            purchases = purchases,
            options = PaywallOptions.Builder(dismissRequest = {})
                .setOffering(TestData.template1Offering)
                .build(),
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false,
            shouldDisplayBlock = null,
            backgroundDispatcher = testDispatcher,
        )
        advanceUntilIdle()
        assertThat(vm.state.value).isInstanceOf(PaywallState.Loaded::class.java)

        vm.closePaywall(result = null)
        vm.updateOptions(PaywallOptions.Builder(dismissRequest = {}).build())
        runCurrent()
        vm.onPaywallDismissed()

        assertThat(vm.state.value).isEqualTo(PaywallState.Loading)
        refreshGate.complete(Unit)
        advanceUntilIdle()
        assertThat(vm.state.value).isEqualTo(PaywallState.Loading)
    }

    @Test
    fun `impression is not tracked from stale state after closePaywall`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        vm.trackPaywallImpressionIfNeeded()
        vm.closePaywall(result = null)
        captured.clear()

        vm.trackPaywallImpressionIfNeeded()

        val impressions = captured.filterIsInstance<PaywallEvent>()
            .filter { it.type == PaywallEventType.IMPRESSION }
        assertThat(impressions).isEmpty()
    }

    @Test
    fun `impression after closePaywall from non-paywall step is not suppressed`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }
        val (result, offerings) = makeContextPackageWorkflow()

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(result, offerings, null, uiConfig)
        // step-1 is a context (non-paywall) step, so currentWorkflowStepTracksPaywallEvents = false
        vm.trackPaywallImpressionIfNeeded()
        assertThat(captured.filterIsInstance<PaywallEvent>()).isEmpty()

        vm.closePaywall(result = null)
        captured.clear()

        // After close, simulate the VM being re-presented at step-2 and verify impression is tracked normally
        // (not suppressed by stale state).
        vm.onPaywallPresented()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        vm.trackPaywallImpressionIfNeeded()

        val impressions = captured.filterIsInstance<PaywallEvent>()
            .filter { it.type == PaywallEventType.IMPRESSION }
        assertThat(impressions).isNotEmpty()
    }

    @Test
    fun `a failed purchase during a workflow is handed to the error presenter`() = runTest {
        coEvery { purchases.awaitPurchase(any()) } throws
            PurchasesException(PurchasesError(PurchasesErrorCode.StoreProblemError))
        val presented = mutableListOf<CheckpointFailureKind>()
        val vm = createVm(errorPresenter = { _, kind, _ -> presented += kind })
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        advanceUntilIdle()

        vm.handlePackagePurchase(activity = mockk<Activity>(), pkg = TestData.Packages.monthly)

        assertThat(presented).containsExactly(CheckpointFailureKind.FLOW_ACTION_FAILED)
        assertThat(vm.actionError.value).isNull()
    }

    @Test
    fun `completing the error presenter mid-flow keeps the current step`() = runTest {
        coEvery { purchases.awaitPurchase(any()) } throws
            PurchasesException(PurchasesError(PurchasesErrorCode.StoreProblemError))
        var completion: ErrorPresenter.Completion? = null
        val dismissals = mutableListOf<PaywallDismissReason>()
        val vm = createVm(
            dismissRequestWithExitOffering = { _, _, reason -> dismissals += reason },
            errorPresenter = { _, _, c -> completion = c },
        )
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        vm.onTransitionComplete(vm.workflowState.value!!.pendingTransition!!.id)
        advanceUntilIdle()
        vm.handlePackagePurchase(activity = mockk<Activity>(), pkg = TestData.Packages.monthly)

        completion!!.complete()
        advanceUntilIdle()

        assertThat(dismissals).isEmpty()
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-2")
        assertThat(vm.workflowState.value?.pendingTransition).isNull()
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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        advanceUntilIdle()
        vm.handlePackagePurchase(activity = mockk<Activity>(), pkg = TestData.Packages.monthly)
        captured.clear()

        // A close after the purchase completed is not an abandonment, so no workflow_close.
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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        advanceUntilIdle()
        vm.handleRestorePurchases()
        advanceUntilIdle()
        captured.clear()

        // Premise: the purchase-completed gate alone would not have suppressed Close here.
        assertThat(vm.purchaseCompleted.value).isFalse

        // A successful restore is a natural exit, not an abandonment, so no workflow_close.
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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        advanceUntilIdle()
        // Successful restore with shouldDisplayBlock null: the paywall stays open, completion recorded.
        vm.handleRestorePurchases()
        advanceUntilIdle()

        // A refresh re-presents the SAME open session (e.g. updateOptions -> presentWorkflow ->
        // startWorkflowPresentation). The user has NOT dismissed, so the restore completion must survive
        // and still suppress workflow_close on the eventual manual dismiss.
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        advanceUntilIdle()
        // MY_APP purchase completes and auto-dismisses the paywall (closePaywall -> clearWorkflowState),
        // which ends the session.
        vm.handlePackagePurchase(activity = mockk<Activity>(), pkg = TestData.Packages.monthly)
        advanceUntilIdle()

        // The same ViewModel later presents a NEW workflow session. _purchaseCompleted stays true (sticky),
        // but this session had no purchase, so abandoning it must still emit workflow_close.
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        advanceUntilIdle()
        // REVENUECAT purchase completes and dismisses via options.dismissRequest() (not closePaywall),
        // which ends the session on an embedded ViewModel that is not destroyed.
        vm.handlePackagePurchase(activity = mockk<Activity>(), pkg = TestData.Packages.monthly)
        advanceUntilIdle()

        // The same ViewModel later presents a NEW workflow session with no purchase, so abandoning it
        // must still emit workflow_close (the completion must not stick past the dismiss).
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
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
        vm.startWorkflowPresentationFromResult(fetchResultFailingInitial, testOfferingsWithEmpty, null, uiConfig)

        val started = captured.filterIsInstance<WorkflowEvent.StepStarted>()
        assertThat(started).isEmpty()
    }

    @Test
    fun `a step that cannot be shown keeps the current step and fires no navigation events`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResultToError, testOfferingsWithEmpty, null, uiConfig)
        captured.clear() // clear load event

        // Navigate to step-3: computeStateForStep returns Error since its offering has no packages
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)

        assertThat(captured.filterIsInstance<WorkflowEvent>()).isEmpty()
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
        assertThat(vm.state.value).isInstanceOf(PaywallState.Loaded.Components::class.java)
        assertThat(vm.actionError.value?.underlyingErrorMessage).contains("No packages available")
    }

    @Test
    fun `initial step without an offering uses its screen's offering`() {
        val stepWithoutOffering = step1.copy(paramValues = emptyMap())
        val workflowWithoutOffering = workflow.copy(steps = mapOf("step-1" to stepWithoutOffering, "step-2" to step2))

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(workflowWithoutOffering, testOfferings, null, uiConfig)

        val loaded = vm.state.value as PaywallState.Loaded.Components
        assertThat(loaded.workflowScreen?.hasOffering).isTrue
        assertThat(loaded.offering.identifier).isEqualTo(offeringId)
        assertThat(loaded.offering.availablePackages).isEqualTo(testOffering.availablePackages)
    }

    @Test
    fun `step's own offering takes precedence over its screen's offering`() {
        val stepWithOwnOffering = step1.copy(paramValues = offeringParams(exitOfferingId))
        val wfl = workflow.copy(steps = mapOf("step-1" to stepWithOwnOffering, "step-2" to step2))

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(wfl, testOfferingsWithExitOffer, null, uiConfig)

        val loaded = vm.state.value as PaywallState.Loaded.Components
        assertThat(loaded.offering.identifier).isEqualTo(exitOfferingId)
    }

    @Test
    fun `initial step renders without an offering when neither the step nor its screen names one`() {
        val stepWithoutOffering = step1.copy(paramValues = emptyMap())
        val wfl = workflow.copy(
            steps = mapOf("step-1" to stepWithoutOffering, "step-2" to step2),
            screens = workflow.screens + (screenId1 to makeScreen(screenId1).copy(offeringIdentifier = null)),
        )

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(wfl, testOfferings, null, uiConfig)

        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
        val loaded = vm.state.value as PaywallState.Loaded.Components
        assertThat(loaded.workflowScreen?.hasOffering).isFalse
        assertThat(loaded.offering.availablePackages).isEmpty()
        every { purchases.store } returns Store.PLAY_STORE
        val webViewStyle = mockk<WebViewComponentStyle> { every { rcPackage } returns null }
        assertThat(webViewContextSnapshot(loaded, webViewStyle, darkMode = false).getValue("offering"))
            .isEqualTo(JsonNull)
    }

    @Test
    fun `a step without an offering gets the fallback step's default package as context`() {
        val (base, offerings) = makeContextPackageWorkflow()
        val vm = createVm()
        vm.startWorkflowPresentationFromResult(base, offerings, null, uiConfig)

        val step1State = vm.workflowState.value?.stepStates?.get("step-1")
        assertThat(step1State).isNotNull
        assertThat(step1State!!.workflowScreen?.hasOffering).isFalse
        assertThat(step1State.offering.availablePackages).isEmpty()
        assertThat(step1State.selectedPackageInfo?.rcPackage?.identifier).isEqualTo(PackageType.MONTHLY.identifier)
    }

    @Test
    fun `a step without an offering emits no paywall events even when tagged as a paywall`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }
        val (base, offerings) = makeContextPackageWorkflow()
        val taggedStep1 = base.steps.getValue("step-1").copy(metadata = screenTypeMetadata(WorkflowScreenType.PAYWALL))
        val wfl = base.copy(
            steps = base.steps + ("step-1" to taggedStep1),
        )

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(wfl, offerings, null, uiConfig)
        vm.trackPaywallImpressionIfNeeded()

        assertThat(captured.filterIsInstance<PaywallEvent>()).isEmpty()

        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        vm.trackPaywallImpressionIfNeeded()

        val impressions = captured.filterIsInstance<PaywallEvent>().filter { it.type == PaywallEventType.IMPRESSION }
        assertThat(impressions).hasSize(1)
    }

    @Test
    fun `initial step whose offering is not in offerings errors instead of rendering`() {
        val stepWithUnknownOffering = step1.copy(paramValues = offeringParams("unknown_offering"))
        val workflowWithUnknownOffering =
            workflow.copy(steps = mapOf("step-1" to stepWithUnknownOffering, "step-2" to step2))

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(workflowWithUnknownOffering, testOfferings, null, uiConfig)

        assertThat(vm.workflowState.value).isNull()
        val error = vm.state.value as PaywallState.Error
        assertThat(error.errorMessage).contains("Offering 'unknown_offering' not found for step 'step-1'")
    }

    @Test
    fun `an injected workflow resolves each step's offering from the injected offerings`() = runTest {
        val secondOfferingId = "second_offering"
        val secondOffering = Offering(
            identifier = secondOfferingId,
            serverDescription = "",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly),
            paywallComponents = null,
            webCheckoutURL = null,
        )
        val twoOfferingWorkflow = workflow.copy(
            steps = mapOf("step-1" to step1, "step-2" to step2.copy(paramValues = offeringParams(secondOfferingId))),
        )
        val offerings = Offerings(
            testOffering,
            mapOf(offeringId to testOffering, secondOfferingId to secondOffering),
        )

        val vm = PaywallViewModelImpl(
            resourceProvider = MockResourceProvider(),
            purchases = purchases,
            options = PaywallOptions.Builder(dismissRequest = {})
                .injectedWorkflow(twoOfferingWorkflow, offerings, uiConfig, traceId = null, workflowBlobRef = null)
                .build(),
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false,
            shouldDisplayBlock = null,
            backgroundDispatcher = testDispatcher,
        )
        advanceUntilIdle()
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)

        assertThat(vm.workflowState.value?.stepStates?.get("step-1")?.offering?.identifier).isEqualTo(offeringId)
        assertThat(vm.workflowState.value?.stepStates?.get("step-2")?.offering?.identifier)
            .isEqualTo(secondOfferingId)
        coVerify(exactly = 0) { purchases.awaitOfferings() }
    }

    @Test
    fun `a checkpoint workflow adds its identifier only to the offering resolved for a step`() = runTest {
        val checkpointIdentifier = "test_checkpoint"
        val unrelatedOffering = Offering(
            identifier = "unrelated_offering",
            serverDescription = "",
            metadata = emptyMap(),
            availablePackages = listOf(TestData.Packages.monthly),
        )
        val sourceOfferings = Offerings(
            current = testOffering,
            all = mapOf(
                testOffering.identifier to testOffering,
                unrelatedOffering.identifier to unrelatedOffering,
            ),
        )
        val vm = PaywallViewModelImpl(
            resourceProvider = MockResourceProvider(),
            purchases = purchases,
            options = PaywallOptions.Builder(dismissRequest = {})
                .injectedWorkflow(
                    workflow,
                    sourceOfferings,
                    uiConfig,
                    traceId = null,
                    workflowBlobRef = null,
                    checkpointIdentifier = checkpointIdentifier,
                )
                .build(),
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false,
            shouldDisplayBlock = null,
            backgroundDispatcher = testDispatcher,
        )
        advanceUntilIdle()

        val loadedOffering = (vm.state.value as PaywallState.Loaded.Components).offering
        assertThat(loadedOffering.presentedOfferingContext?.checkpointIdentifierForTest())
            .isEqualTo(checkpointIdentifier)
        assertThat(sourceOfferings.all.values.map { it.presentedOfferingContext?.checkpointIdentifierForTest() })
            .containsOnlyNulls()
    }

    @Test
    fun `navigation to a step whose offering is not in offerings keeps the current step with the error`() {
        val stepWithUnknownOffering = step2.copy(paramValues = offeringParams("unknown_offering"))
        val workflowToStepWithUnknownOffering =
            workflow.copy(steps = mapOf("step-1" to step1, "step-2" to stepWithUnknownOffering))

        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }
        val vm = createVm()
        vm.startWorkflowPresentationFromResult(workflowToStepWithUnknownOffering, testOfferings, null, uiConfig)
        captured.clear()

        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)

        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
        assertThat(vm.workflowState.value?.pendingTransition).isNull()
        val error = vm.actionError.value!!
        assertThat(error.code).isEqualTo(PurchasesErrorCode.ConfigurationError)
        assertThat(error.underlyingErrorMessage).contains("Offering 'unknown_offering' not found for step 'step-2'")
        assertThat(captured.filterIsInstance<WorkflowEvent>()).isEmpty()
    }

    private fun PresentedOfferingContext.checkpointIdentifierForTest(): String? {
        val field = PresentedOfferingContext::class.java.getDeclaredField("checkpointIdentifier")
        field.isAccessible = true
        return field.get(this) as String?
    }

    @Test
    fun `a step that cannot be shown is handed to the error presenter as an error during the flow`() {
        val stepWithUnknownOffering = step2.copy(paramValues = offeringParams("unknown_offering"))
        val workflowToStepWithUnknownOffering =
            workflow.copy(steps = mapOf("step-1" to step1, "step-2" to stepWithUnknownOffering))
        val presented = mutableListOf<CheckpointFailureKind>()
        var completion: ErrorPresenter.Completion? = null
        val dismissals = mutableListOf<PaywallDismissReason>()
        val vm = createVm(
            dismissRequestWithExitOffering = { _, _, reason -> dismissals += reason },
            errorPresenter = { _, kind, c ->
                presented += kind
                completion = c
            },
        )
        vm.startWorkflowPresentationFromResult(workflowToStepWithUnknownOffering, testOfferings, null, uiConfig)

        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        completion!!.complete()

        assertThat(presented).containsExactly(CheckpointFailureKind.FLOW_ACTION_FAILED)
        assertThat(vm.actionError.value).isNull()
        assertThat(dismissals).isEmpty()
        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
    }

    @Test
    fun `navigation to a step whose screen is missing keeps the current step with the error`() {
        val stepWithMissingScreen = step2.copy(screenId = "missing-screen")
        val workflowToMissingScreen =
            workflow.copy(steps = mapOf("step-1" to step1, "step-2" to stepWithMissingScreen))

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(workflowToMissingScreen, testOfferings, null, uiConfig)

        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)

        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
        assertThat(vm.actionError.value?.underlyingErrorMessage).contains("Screen 'missing-screen' not found")
    }

    @Test
    fun `all events within one impression share the same traceId, new impression gets a new one`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)

        val firstImpressionTraceId = captured.filterIsInstance<WorkflowEvent>().map { it.traceId }.distinct().single()

        captured.clear()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)

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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
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
    fun `paywall impression carries the same traceId as the StepStarted from the same impression`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        vm.trackPaywallImpressionIfNeeded()

        // The equality is the load-bearing half: traceId defaults to null on PaywallEvent.Data, so
        // dropping the wiring would compile clean and silently ship trace_id: null.
        val startedTraceId = captured.filterIsInstance<WorkflowEvent.StepStarted>().single().traceId
        val impression = captured.filterIsInstance<PaywallEvent>()
            .single { it.type == PaywallEventType.IMPRESSION }
        assertThat(impression.data.traceId).isNotNull()
        assertThat(impression.data.traceId).isEqualTo(startedTraceId)
    }

    @Test
    fun `an injected workflow reports the trace id it was given`() = runTest {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createInjectedWorkflowVm(traceId = "checkpoint-trace")
        vm.trackPaywallImpressionIfNeeded()

        assertThat(captured.filterIsInstance<WorkflowEvent.StepStarted>().single().traceId)
            .isEqualTo("checkpoint-trace")
        val impression = captured.filterIsInstance<PaywallEvent>()
            .single { it.type == PaywallEventType.IMPRESSION }
        assertThat(impression.data.traceId).isEqualTo("checkpoint-trace")
    }

    @Test
    fun `an injected workflow's step events carry its blob ref and experiment data`() = runTest {
        val experimentStep1 = step1.copy(
            paramValues = mapOf(
                "experiment_id" to JsonPrimitive("exp_abc"),
                "experiment_variant" to JsonPrimitive("b"),
            ),
        )
        val experimentWorkflow = workflow.copy(steps = mapOf("step-1" to experimentStep1, "step-2" to step2))
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createInjectedWorkflowVm(
            traceId = "checkpoint-trace",
            injectedWorkflow = experimentWorkflow,
            workflowBlobRef = "blob-ref-1",
        )
        vm.closePaywall(result = null)

        val workflowEvents = captured.filterIsInstance<WorkflowEvent>()
        assertThat(workflowEvents.map { it::class }).containsExactlyInAnyOrder(
            WorkflowEvent.StepStarted::class,
            WorkflowEvent.StepCompleted::class,
            WorkflowEvent.Close::class,
        )
        workflowEvents.forEach { event ->
            val (blobRef, experiment) = when (event) {
                is WorkflowEvent.StepStarted -> event.workflowBlobRef to event.experiment
                is WorkflowEvent.StepCompleted -> event.workflowBlobRef to event.experiment
                is WorkflowEvent.Close -> event.workflowBlobRef to event.experiment
            }
            assertThat(blobRef).`as`(event::class.simpleName).isEqualTo("blob-ref-1")
            assertThat(experiment?.experimentId).`as`(event::class.simpleName).isEqualTo("exp_abc")
            assertThat(experiment?.experimentVariant).`as`(event::class.simpleName).isEqualTo("b")
        }
    }

    @Test
    fun `an injected workflow without a trace id still gets one`() = runTest {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        createInjectedWorkflowVm(traceId = null)

        assertThat(captured.filterIsInstance<WorkflowEvent.StepStarted>().single().traceId).isNotBlank()
    }

    @Test
    fun `RevenueCat purchase auto-dismiss starts a fresh paywall event session on re-presentation`() = runTest {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }
        coEvery { purchases.awaitPurchase(any()) } returns PurchaseResult(
            storeTransaction = mockk<StoreTransaction>(),
            customerInfo = mockk<CustomerInfo>(),
        )

        lateinit var vm: PaywallViewModelImpl
        var stateWhenDismissRequested: PaywallState? = null
        vm = createVm(
            dismissRequest = {
                stateWhenDismissRequested = vm.state.value
                vm.onPaywallDismissed()
            },
        )
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        vm.trackPaywallImpressionIfNeeded()
        val firstImpression = captured.filterIsInstance<PaywallEvent>()
            .single { it.type == PaywallEventType.IMPRESSION }

        vm.handlePackagePurchase(activity = mockk<Activity>(), pkg = TestData.Packages.monthly)
        assertThat(stateWhenDismissRequested).isInstanceOf(PaywallState.Loaded::class.java)
        assertThat(vm.state.value).isEqualTo(PaywallState.Loading)
        vm.onPaywallPresented()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        vm.trackPaywallImpressionIfNeeded()

        val impressions = captured.filterIsInstance<PaywallEvent>()
            .filter { it.type == PaywallEventType.IMPRESSION }
        assertThat(impressions).hasSize(2)
        val secondImpression = impressions.last()
        val secondStepStarted = captured.filterIsInstance<WorkflowEvent.StepStarted>().last()
        assertThat(secondImpression.data.sessionIdentifier)
            .isNotEqualTo(firstImpression.data.sessionIdentifier)
        assertThat(secondImpression.data.workflowId).isEqualTo(fetchResult.id)
        assertThat(secondImpression.data.stepId).isEqualTo("step-1")
        assertThat(secondImpression.data.traceId).isEqualTo(secondStepStarted.traceId)
        assertThat(secondImpression.data.traceId).isNotEqualTo(firstImpression.data.traceId)
    }

    @Test
    fun `RevenueCat restore auto-dismiss starts a fresh paywall event session on re-presentation`() = runTest {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }
        coEvery { purchases.awaitRestore() } returns mockk<CustomerInfo>()
        lateinit var vm: PaywallViewModelImpl
        var stateWhenDismissRequested: PaywallState? = null
        vm = PaywallViewModelImpl(
            resourceProvider = MockResourceProvider(),
            purchases = purchases,
            options = PaywallOptions.Builder(
                dismissRequest = {
                    stateWhenDismissRequested = vm.state.value
                    vm.onPaywallDismissed()
                },
            ).build(),
            colorScheme = TestData.Constants.currentColorScheme,
            isDarkMode = false,
            shouldDisplayBlock = { false },
            backgroundDispatcher = testDispatcher,
        )
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        vm.trackPaywallImpressionIfNeeded()
        val firstImpression = captured.filterIsInstance<PaywallEvent>()
            .single { it.type == PaywallEventType.IMPRESSION }

        vm.handleRestorePurchases()
        assertThat(stateWhenDismissRequested).isInstanceOf(PaywallState.Loaded::class.java)
        assertThat(vm.state.value).isEqualTo(PaywallState.Loading)
        vm.onPaywallPresented()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        vm.trackPaywallImpressionIfNeeded()

        val impressions = captured.filterIsInstance<PaywallEvent>()
            .filter { it.type == PaywallEventType.IMPRESSION }
        assertThat(impressions).hasSize(2)
        val secondImpression = impressions.last()
        val secondStepStarted = captured.filterIsInstance<WorkflowEvent.StepStarted>().last()
        assertThat(secondImpression.data.sessionIdentifier)
            .isNotEqualTo(firstImpression.data.sessionIdentifier)
        assertThat(secondImpression.data.workflowId).isEqualTo(fetchResult.id)
        assertThat(secondImpression.data.stepId).isEqualTo("step-1")
        assertThat(secondImpression.data.traceId).isEqualTo(secondStepStarted.traceId)
        assertThat(secondImpression.data.traceId).isNotEqualTo(firstImpression.data.traceId)
    }

    @Test
    fun `paywall impression traceId survives forward navigation within one presentation`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        vm.trackPaywallImpressionIfNeeded()
        val firstTraceId = captured.filterIsInstance<PaywallEvent>()
            .single { it.type == PaywallEventType.IMPRESSION }.data.traceId

        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        vm.trackPaywallImpressionIfNeeded()

        // The trace id identifies the traversal, not the step, so every paywall event in the
        // presentation reports the same one.
        val traceIds = captured.filterIsInstance<PaywallEvent>().map { it.data.traceId }.distinct()
        assertThat(firstTraceId).isNotNull()
        assertThat(traceIds).containsExactly(firstTraceId)
    }

    @Test
    fun `a new workflow presentation refreshes the traceId on retained paywall event data`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        vm.trackPaywallImpressionIfNeeded()
        val firstTraceId = captured.filterIsInstance<PaywallEvent>()
            .single { it.type == PaywallEventType.IMPRESSION }.data.traceId

        // Re-presenting the same step keeps the same visual fingerprint, so no second impression fires
        // and the event data is retained. It still has to pick up the new traversal's trace id, which
        // withCurrentWorkflowMetadata is responsible for; later events read that retained data.
        captured.clear()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        vm.trackPaywallImpressionIfNeeded()
        val secondStartedTraceId = captured.filterIsInstance<WorkflowEvent.StepStarted>().single().traceId
        assertThat(secondStartedTraceId).isNotEqualTo(firstTraceId)

        vm.trackComponentInteraction(
            componentType = PaywallComponentType.BUTTON,
            componentName = PaywallLegacyComponentInteraction.RESTORE_BUTTON_NAME,
            componentValue = PaywallLegacyComponentInteraction.Value.RESTORE_PURCHASES,
        )

        val interaction = captured.filterIsInstance<PaywallEvent>()
            .single { it.type == PaywallEventType.COMPONENT_INTERACTION }
        assertThat(interaction.data.traceId).isEqualTo(secondStartedTraceId)
    }

    @Test
    fun `paywall impression outside a workflow presentation carries no traceId`() = runTest {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        vm.trackPaywallImpressionIfNeeded()
        val renderedWorkflowOffering = (vm.state.value as PaywallState.Loaded.Components).offering
        vm.closePaywall(result = null)
        captured.clear()

        val standaloneOffering = Offering(
            identifier = "standalone-offering",
            serverDescription = "",
            metadata = emptyMap(),
            availablePackages = renderedWorkflowOffering.availablePackages,
            paywallComponents = renderedWorkflowOffering.paywallComponents,
            webCheckoutURL = null,
        )
        coEvery {
            purchases.resolveWorkflow(standaloneOffering.identifier)
        } returns WorkflowResolution.NoWorkflow
        vm.onPaywallPresented()
        vm.updateOptions(
            PaywallOptions.Builder(dismissRequest = {})
                .setOffering(standaloneOffering)
                .build(),
        )
        advanceUntilIdle()
        vm.trackPaywallImpressionIfNeeded()

        // The backing trace id field is always populated, so an ungated read would leak a meaningless
        // one here and force presented_offering_context to serialize on standalone paywall traffic.
        val impression = captured.filterIsInstance<PaywallEvent>()
            .first { it.type == PaywallEventType.IMPRESSION }
        assertThat(impression.data.workflowId).isNull()
        assertThat(impression.data.traceId).isNull()
    }

    @Test
    fun `re-presenting workflow while on step-2 fires StepCompleted for the abandoned step`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        vm.handleWorkflowAction("btn-next", WorkflowTriggerType.ON_PRESS)
        captured.clear()

        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)

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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)

        assertThat(captured.filterIsInstance<WorkflowEvent.StepCompleted>()).isEmpty()
    }

    @Test
    fun `workflow impression event carries workflowId from loaded workflow`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)
        vm.trackPaywallImpressionIfNeeded()

        val impressions = captured.filterIsInstance<PaywallEvent>()
            .filter { it.type == PaywallEventType.IMPRESSION }
        assertThat(impressions).isNotEmpty()
        assertThat(impressions.first().data.workflowId).isEqualTo(fetchResult.id)
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
        val step1Result = workflow.copy(
                initialStepId = "step-1",
                singleStepFallbackId = "step-1",
                steps = sharedScreenSteps,
            )
        val step2Result = workflow.copy(
                initialStepId = "step-2",
                singleStepFallbackId = "step-2",
                steps = sharedScreenSteps,
            )

        vm.startWorkflowPresentationFromResult(step1Result, testOfferings, null, uiConfig)
        vm.trackPaywallImpressionIfNeeded()
        captured.clear()

        vm.startWorkflowPresentationFromResult(step2Result, testOfferings, null, uiConfig)
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
        assertThat(purchaseInitiated.data.workflowId).isEqualTo(step2Result.id)
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
        val step1Result = workflow.copy(
                initialStepId = "step-1",
                singleStepFallbackId = "step-1",
                steps = sharedScreenSteps,
            )
        val step2Result = workflow.copy(
                initialStepId = "step-2",
                singleStepFallbackId = "step-2",
                steps = sharedScreenSteps,
            )

        vm.startWorkflowPresentationFromResult(step1Result, testOfferings, null, uiConfig)
        vm.trackPaywallImpressionIfNeeded()
        captured.clear()

        vm.startWorkflowPresentationFromResult(step2Result, testOfferings, null, uiConfig)
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
        assertThat(exitOffer.data.workflowId).isEqualTo(step2Result.id)
        assertThat(exitOffer.data.stepId).isEqualTo("step-2")
    }

    @Test
    fun `packageless workflow steps do not emit paywall events`() {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }
        val (result, offerings) = makeContextPackageWorkflow()

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(result, offerings, null, uiConfig)
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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)

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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)

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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)

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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)

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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)

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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)

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
        vm.startWorkflowPresentationFromResult(fetchResult, testOfferings, null, uiConfig)

        vm.handleRestorePurchases()
        advanceUntilIdle()

        verify { listener.onRestoreError(restoreError) }
        verify(exactly = 0) { listener.onRestoreCompleted(any()) }
    }

    // endregion purchase/restore callbacks

    // region screen_type paywall-event gating

    private fun singleStepScreenTypeWorkflow(metadata: JsonObject?): PublishedWorkflow {
        val step = WorkflowStep(
            id = "step-only",
            type = "screen",
            screenId = screenId1,
            triggers = emptyList(),
            triggerActions = emptyMap(),
            paramValues = offeringParams(offeringId),
            metadata = metadata,
        )
        return workflow.copy(
            initialStepId = "step-only",
            steps = mapOf("step-only" to step),
            screens = mapOf(screenId1 to makeScreen(screenId1)),
            singleStepFallbackId = "step-only",
        )
    }

    private fun List<FeatureEvent>.paywallEventsOfType(type: PaywallEventType) =
        filterIsInstance<PaywallEvent>().filter { it.type == type }

    private fun PaywallViewModelImpl.workflowScreenOf(stepId: String): WorkflowScreenContext? =
        workflowState.value?.stepStates?.get(stepId)?.workflowScreen

    @Test
    fun `step tagged paywall reports impression and close`() = runTest {
        val captured = mutableListOf<FeatureEvent>()
        every { purchases.track(any()) } answers { captured.add(firstArg()) }

        val vm = createVm()
        vm.startWorkflowPresentationFromResult(
            singleStepScreenTypeWorkflow(screenTypeMetadata(WorkflowScreenType.PAYWALL)),
            testOfferings,
            null,
            uiConfig,
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
            uiConfig,
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
            uiConfig,
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
        val untaggedMultiStep = workflow.copy(singleStepFallbackId = "step-2")
        vm.startWorkflowPresentationFromResult(untaggedMultiStep, testOfferings, null, uiConfig)
        vm.trackPaywallImpressionIfNeeded()

        assertThat(captured.paywallEventsOfType(PaywallEventType.IMPRESSION)).isEmpty()
    }

    // endregion

    /**
     * Returns from dispatch() only after the block has run on another thread, so withContext's result is ready
     * before its caller reaches the suspension point and it returns without suspending.
     */
    private class FinishBeforeReturnDispatcher : CoroutineDispatcher(), AutoCloseable {
        private val executor = Executors.newSingleThreadExecutor()
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            executor.submit(block).get(10, TimeUnit.SECONDS)
        }
        override fun close() = executor.shutdown()
    }
}
