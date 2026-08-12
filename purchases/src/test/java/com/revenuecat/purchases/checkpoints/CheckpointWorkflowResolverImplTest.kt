@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.checkpoints

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.checkpoints.CheckpointResponse
import com.revenuecat.purchases.common.checkpoints.CheckpointRule
import com.revenuecat.purchases.common.checkpoints.CheckpointRulesResolution
import com.revenuecat.purchases.common.checkpoints.CheckpointsConfigProvider
import com.revenuecat.purchases.common.localrules.LocalRulesEvaluator
import com.revenuecat.purchases.common.localrules.RulesDimensionNamespace
import com.revenuecat.purchases.common.localrules.RulesDimensionProvider
import com.revenuecat.purchases.common.localrules.RulesDimensionValue
import com.revenuecat.purchases.common.uiconfig.UiConfigProvider
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.WorkflowManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class CheckpointWorkflowResolverImplTest {

    private val checkpointId = "test_checkpoint"

    private lateinit var mockWorkflowManager: WorkflowManager
    private lateinit var mockUiConfigProvider: UiConfigProvider
    private lateinit var mockCheckpointsConfigProvider: CheckpointsConfigProvider
    private lateinit var mockWorkflow: PublishedWorkflow
    private lateinit var mockUiConfig: UiConfig
    private lateinit var mockOffering: Offering
    private lateinit var mockOfferings: Offerings
    private var offeringsFetchError: PurchasesError? = null
    private var offeringsFetched = 0

    private lateinit var resolver: CheckpointWorkflowResolverImpl

    @Before
    fun setup() {
        mockWorkflowManager = mockk()
        mockUiConfigProvider = mockk()
        mockCheckpointsConfigProvider = mockk()
        mockWorkflow = mockk()
        mockUiConfig = mockk()
        mockOffering = mockk {
            every { identifier } returns "default"
        }
        mockOfferings = mockk()
        coEvery { mockWorkflowManager.offeringIdByWorkflowId() } returns mapOf("wf1234" to "default")
        coEvery { mockWorkflowManager.getWorkflow("wf1234") } returns mockWorkflow
        coEvery { mockUiConfigProvider.getUiConfig() } returns mockUiConfig
        every { mockOfferings.all } returns mapOf("default" to mockOffering)
        configureRules(rule("wf1234"))
        resolver = CheckpointWorkflowResolverImpl(
            workflowManager = mockWorkflowManager,
            uiConfigProvider = mockUiConfigProvider,
            checkpointsConfigProvider = mockCheckpointsConfigProvider,
            localRulesEvaluator = LocalRulesEvaluator(providers = emptyList()),
            getOfferings = {
                offeringsFetched++
                offeringsFetchError?.let { throw PurchasesException(it) }
                mockOfferings
            },
        )
    }

    @Test
    fun `simulated error checkpoint throws ConfigurationError`() = runTest {
        val errorCode = try {
            resolver.resolve("error_checkpoint", emptyMap())
            null
        } catch (e: PurchasesException) {
            e.code
        }

        assertThat(errorCode).isEqualTo(PurchasesErrorCode.ConfigurationError)
    }

    @Test
    fun `checkpoint resolves NoAction with DISABLED when workflows are disabled`() = runTest {
        resolver = CheckpointWorkflowResolverImpl(
            workflowManager = null,
            uiConfigProvider = null,
            checkpointsConfigProvider = null,
            localRulesEvaluator = LocalRulesEvaluator(providers = emptyList()),
            getOfferings = { mockOfferings },
        )

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.DISABLED)
    }

    @Test
    fun `checkpoint resolves NoAction with DISABLED when remote config is disabled`() = runTest {
        configureResolution(CheckpointRulesResolution.Disabled)

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.DISABLED)
    }

    @Test
    fun `checkpoint resolves NoAction with UNKNOWN_CHECKPOINT when it is not configured`() = runTest {
        configureResolution(CheckpointRulesResolution.NotConfigured)

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.UNKNOWN_CHECKPOINT)
    }

    @Test
    fun `checkpoint resolves NoAction with CONFIGURATION_UNAVAILABLE when its rules cannot be read`() = runTest {
        configureResolution(CheckpointRulesResolution.Unavailable)

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
    }

    @Test
    fun `checkpoint resolves NoAction with NO_MATCH when it has no rules`() = runTest {
        configureRules()

        assertThat(noActionReason(resolve())).isEqualTo(CheckpointResolution.NoAction.Reason.NO_MATCH)
        assertThat(offeringsFetched).isZero()
    }

    @Test
    fun `checkpoint resolves NoAction with CONFIGURATION_UNAVAILABLE when no workflows exist`() = runTest {
        coEvery { mockWorkflowManager.offeringIdByWorkflowId() } returns emptyMap()

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        assertThat(offeringsFetched).isZero()
    }

    @Test
    fun `checkpoint resolves NoAction with CONFIGURATION_UNAVAILABLE when the workflow fails to load`() = runTest {
        coEvery { mockWorkflowManager.getWorkflow("wf1234") } throws PurchasesException(
            PurchasesError(PurchasesErrorCode.UnknownError, "Workflow unavailable."),
        )

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
    }

    @Test
    fun `checkpoint resolves NoAction with CONFIGURATION_UNAVAILABLE when ui config is unavailable`() = runTest {
        coEvery { mockUiConfigProvider.getUiConfig() } returns null

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
    }

    @Test
    fun `checkpoint resolves Workflow with the workflow and its offering`() = runTest {
        val resolution = resolve() as CheckpointResolution.Workflow

        assertThat(resolution.workflow).isEqualTo(mockWorkflow)
        assertThat(resolution.uiConfig).isEqualTo(mockUiConfig)
        assertThat(resolution.offering).isEqualTo(mockOffering)
    }

    @Test
    fun `checkpoint resolves the workflow of the first rule`() = runTest {
        configureRules(rule("wf1234"), rule("wf5678"))

        val resolution = resolve() as CheckpointResolution.Workflow

        assertThat(resolution.workflow).isEqualTo(mockWorkflow)
        coVerify(exactly = 0) { mockWorkflowManager.getWorkflow("wf5678") }
    }

    @Test
    fun `a matched rule whose workflow is not mapped to an offering does not fall through`() = runTest {
        configureRules(rule("wf5678"), rule("wf1234"))
        coEvery { mockWorkflowManager.offeringIdByWorkflowId() } returns mapOf("wf1234" to "default")

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        coVerify(exactly = 0) { mockWorkflowManager.getWorkflow("wf1234") }
    }

    @Test
    fun `a matched rule whose offering is missing from offerings does not fall through`() = runTest {
        configureRules(rule("wf5678"), rule("wf1234"))
        coEvery { mockWorkflowManager.offeringIdByWorkflowId() } returns
            mapOf("wf5678" to "missing", "wf1234" to "default")

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        coVerify(exactly = 0) { mockWorkflowManager.getWorkflow("wf1234") }
    }

    @Test
    fun `a matched rule whose workflow fails to load does not fall through`() = runTest {
        configureRules(rule("wf5678"), rule("wf1234"))
        coEvery { mockWorkflowManager.offeringIdByWorkflowId() } returns
            mapOf("wf5678" to "default", "wf1234" to "default")
        coEvery { mockWorkflowManager.getWorkflow("wf5678") } throws PurchasesException(
            PurchasesError(PurchasesErrorCode.UnknownError, "Workflow unavailable."),
        )

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        coVerify(exactly = 0) { mockWorkflowManager.getWorkflow("wf1234") }
    }

    @Test
    fun `checkpoint resolves NoAction with CONFIGURATION_UNAVAILABLE when audiences cannot be evaluated`() =
        runTest {
            resolver = CheckpointWorkflowResolverImpl(
                workflowManager = mockWorkflowManager,
                uiConfigProvider = mockUiConfigProvider,
                checkpointsConfigProvider = mockCheckpointsConfigProvider,
                localRulesEvaluator = LocalRulesEvaluator(providers = listOf(FailingDimensionProvider)),
                getOfferings = { mockOfferings },
            )

            assertThat(noActionReason(resolve()))
                .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
            coVerify(exactly = 0) { mockWorkflowManager.getWorkflow(any()) }
        }

    @Test
    fun `checkpoint resolves NoAction with CONFIGURATION_UNAVAILABLE when the offerings fetch fails`() = runTest {
        offeringsFetchError = PurchasesError(PurchasesErrorCode.NetworkError, "Offline.")

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
    }

    @Test
    fun `checkpoint resolves NoAction with CONFIGURATION_UNAVAILABLE when the fetched offerings lack the identifier`() =
        runTest {
            every { mockOfferings.all } returns emptyMap()

            assertThat(noActionReason(resolve()))
                .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        }

    @Test
    fun `offerings and ui config are resolved once`() = runTest {
        configureRules(rule("wf1234"), rule("wf5678"))
        coEvery { mockWorkflowManager.offeringIdByWorkflowId() } returns
            mapOf("wf1234" to "default", "wf5678" to "default")

        assertThat(resolve()).isInstanceOf(CheckpointResolution.Workflow::class.java)
        assertThat(offeringsFetched).isEqualTo(1)
        coVerify(exactly = 1) { mockUiConfigProvider.getUiConfig() }
    }

    private object FailingDimensionProvider : RulesDimensionProvider {
        override val identifier = "failing"
        override val namespace = RulesDimensionNamespace.Device
        override suspend fun dimensions(date: Date): Map<String, RulesDimensionValue> =
            throw IllegalStateException("no dimensions")
    }

    private fun rule(workflowId: String) = CheckpointRule(
        id = "rule_$workflowId",
        audienceId = "aud_$workflowId",
        workflowId = workflowId,
    )

    private fun configureRules(vararg rules: CheckpointRule) {
        configureResolution(CheckpointRulesResolution.Found(CheckpointResponse(rules = rules.toList())))
    }

    private fun configureResolution(resolution: CheckpointRulesResolution) {
        coEvery { mockCheckpointsConfigProvider.resolveCheckpoint(checkpointId) } returns resolution
    }

    private suspend fun resolve(): CheckpointResolution = resolver.resolve(checkpointId, emptyMap())

    private fun noActionReason(resolution: CheckpointResolution): CheckpointResolution.NoAction.Reason =
        (resolution as CheckpointResolution.NoAction).reason
}
