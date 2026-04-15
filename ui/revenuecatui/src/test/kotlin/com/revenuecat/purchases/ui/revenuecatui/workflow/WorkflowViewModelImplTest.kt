@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.workflow

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.WorkflowFetchResult
import com.revenuecat.purchases.common.workflows.WorkflowScreen
import com.revenuecat.purchases.common.workflows.WorkflowStep
import com.revenuecat.purchases.paywalls.components.PackageComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.UiConfig
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(AndroidJUnit4::class)
class WorkflowViewModelImplTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val workflowId = "workflow_123"
    private val stepId = "step_1"
    private val screenId = "screen_1"
    private val offeringId = "offering_id"
    private val defaultLocaleId = LocaleId("en_US")

    private lateinit var purchases: PurchasesType
    private lateinit var resourceProvider: MockResourceProvider

    private val componentsConfig = ComponentsConfig(
        base = PaywallComponentsConfig(
            stack = StackComponent(
                components = listOf(
                    PackageComponent(
                        packageId = PackageType.MONTHLY.identifier!!,
                        isSelectedByDefault = false,
                        stack = StackComponent(components = emptyList()),
                    ),
                ),
            ),
            background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.White.toArgb()))),
            stickyFooter = null,
        ),
    )

    private val localizations = mapOf(
        defaultLocaleId to mapOf(
            LocalizationKey("key") to LocalizationData.Text("value"),
        ),
    )

    private val validScreen = WorkflowScreen(
        name = "Test Screen",
        templateName = "template_v2",
        revision = 1,
        assetBaseURL = URL("https://assets.pawwalls.com"),
        componentsConfig = componentsConfig,
        componentsLocalizations = localizations,
        defaultLocaleIdentifier = defaultLocaleId,
        offeringId = offeringId,
    )

    private val validStep = WorkflowStep(
        id = stepId,
        type = "screen",
        screenId = screenId,
    )

    private val validWorkflow = PublishedWorkflow(
        id = workflowId,
        displayName = "Test Workflow",
        initialStepId = stepId,
        steps = mapOf(stepId to validStep),
        screens = mapOf(screenId to validScreen),
        uiConfig = UiConfig(),
    )

    private val validFetchResult = WorkflowFetchResult(
        workflow = validWorkflow,
        enrolledVariants = null,
    )

    private val validOffering = Offering(
        identifier = offeringId,
        serverDescription = "Test offering",
        metadata = emptyMap(),
        availablePackages = listOf(TestData.Packages.monthly),
    )

    @Before
    fun setUp() {
        purchases = mockk()
        resourceProvider = MockResourceProvider()
        every { purchases.preferredUILocaleOverride } returns null
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // region Happy path

    @Test
    fun `happy path emits Loading then Loaded`() = runTest {
        coEvery { purchases.awaitGetWorkflow(workflowId) } returns validFetchResult
        coEvery { purchases.awaitOfferings() } returns Offerings(validOffering, mapOf(offeringId to validOffering))
        coEvery { purchases.storefrontCountryCode } returns "US"

        val viewModel = createViewModel()

        val loaded = viewModel.state.first { it is WorkflowState.Loaded } as WorkflowState.Loaded
        assertThat(loaded.paywallState).isInstanceOf(PaywallState.Loaded.Components::class.java)
        assertThat(loaded.paywallState.offering.identifier).isEqualTo(offeringId)
    }

    // endregion

    // region Error: workflow fetch failures

    @Test
    fun `network error during workflow fetch emits Error`() = runTest {
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "Network failed")
        coEvery { purchases.awaitGetWorkflow(workflowId) } throws PurchasesException(error)

        val viewModel = createViewModel()

        val state = viewModel.state.first { it !is WorkflowState.Loading }
        assertThat(state).isInstanceOf(WorkflowState.Error::class.java)
    }

    @Test
    fun `unknown workflow identifier emits Error`() = runTest {
        val error = PurchasesError(PurchasesErrorCode.UnknownBackendError, "Not found")
        coEvery { purchases.awaitGetWorkflow(workflowId) } throws PurchasesException(error)

        val viewModel = createViewModel()

        val state = viewModel.state.first { it !is WorkflowState.Loading }
        assertThat(state).isInstanceOf(WorkflowState.Error::class.java)
    }

    // endregion

    // region Error: step/screen resolution failures

    @Test
    fun `initial step not found in steps map emits Error`() = runTest {
        val workflowMissingStep = validWorkflow.copy(
            initialStepId = "nonexistent_step",
        )
        coEvery { purchases.awaitGetWorkflow(workflowId) } returns validFetchResult.copy(workflow = workflowMissingStep)

        val viewModel = createViewModel()

        val state = viewModel.state.first { it !is WorkflowState.Loading }
        assertThat(state).isInstanceOf(WorkflowState.Error::class.java)
        assertThat((state as WorkflowState.Error).message).contains("nonexistent_step")
    }

    @Test
    fun `initial step has no screen_id emits Error`() = runTest {
        val stepWithoutScreen = validStep.copy(screenId = null)
        val workflowNoScreen = validWorkflow.copy(steps = mapOf(stepId to stepWithoutScreen))
        coEvery { purchases.awaitGetWorkflow(workflowId) } returns validFetchResult.copy(workflow = workflowNoScreen)

        val viewModel = createViewModel()

        val state = viewModel.state.first { it !is WorkflowState.Loading }
        assertThat(state).isInstanceOf(WorkflowState.Error::class.java)
    }

    @Test
    fun `screen not found in screens map emits Error`() = runTest {
        val workflowMissingScreen = validWorkflow.copy(screens = emptyMap())
        coEvery { purchases.awaitGetWorkflow(workflowId) } returns validFetchResult.copy(workflow = workflowMissingScreen)

        val viewModel = createViewModel()

        val state = viewModel.state.first { it !is WorkflowState.Loading }
        assertThat(state).isInstanceOf(WorkflowState.Error::class.java)
        assertThat((state as WorkflowState.Error).message).contains(screenId)
    }

    @Test
    fun `empty steps map emits Error`() = runTest {
        val workflowNoSteps = validWorkflow.copy(steps = emptyMap())
        coEvery { purchases.awaitGetWorkflow(workflowId) } returns validFetchResult.copy(workflow = workflowNoSteps)

        val viewModel = createViewModel()

        val state = viewModel.state.first { it !is WorkflowState.Loading }
        assertThat(state).isInstanceOf(WorkflowState.Error::class.java)
    }

    // endregion

    // region Error: offering resolution failure

    @Test
    fun `offering not found emits Error`() = runTest {
        coEvery { purchases.awaitGetWorkflow(workflowId) } returns validFetchResult
        coEvery { purchases.awaitOfferings() } returns Offerings(null, emptyMap())

        val viewModel = createViewModel()

        val state = viewModel.state.first { it !is WorkflowState.Loading }
        assertThat(state).isInstanceOf(WorkflowState.Error::class.java)
        assertThat((state as WorkflowState.Error).message).contains(offeringId)
    }

    @Test
    fun `screen with no offering_id emits Error`() = runTest {
        val screenWithoutOffering = validScreen.copy(offeringId = null)
        val workflowNoOfferingId = validWorkflow.copy(screens = mapOf(screenId to screenWithoutOffering))
        coEvery { purchases.awaitGetWorkflow(workflowId) } returns validFetchResult.copy(workflow = workflowNoOfferingId)

        val viewModel = createViewModel()

        val state = viewModel.state.first { it !is WorkflowState.Loading }
        assertThat(state).isInstanceOf(WorkflowState.Error::class.java)
    }

    // endregion

    private fun createViewModel(): WorkflowViewModelImpl =
        WorkflowViewModelImpl(
            workflowId = workflowId,
            purchases = purchases,
            resourceProvider = resourceProvider,
        )
}
