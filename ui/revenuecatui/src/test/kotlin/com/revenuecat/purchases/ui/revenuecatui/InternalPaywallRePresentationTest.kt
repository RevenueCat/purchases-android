package com.revenuecat.purchases.ui.revenuecatui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.WorkflowResolution
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
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
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

    private val offeringId = "test_offering"
    private val screenId1 = "screen-1"
    private val screenId2 = "screen-2"
    private val defaultLocaleId = LocaleId("en_US")

    private lateinit var purchases: PurchasesType

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
            coEvery { resolveWorkflow(offeringId) } returns WorkflowResolution.Found(workflow.id)
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

        assertThat(vm.workflowState.value?.currentStepId).isEqualTo("step-1")
    }
}
