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
import com.revenuecat.purchases.common.audiences.Audience
import com.revenuecat.purchases.common.audiences.AudiencesConfigProvider
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
import com.revenuecat.purchases.common.workflows.WorkflowStep
import com.revenuecat.purchases.common.workflows.WorkflowTrigger
import com.revenuecat.purchases.common.workflows.WorkflowTriggerAction
import com.revenuecat.purchases.common.workflows.WorkflowTriggerType
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
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
    private lateinit var mockAudiencesConfigProvider: AudiencesConfigProvider
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
        mockAudiencesConfigProvider = mockk()
        mockWorkflow = uiWorkflow("wf1234")
        mockUiConfig = mockk()
        mockOffering = mockk {
            every { identifier } returns "default"
        }
        mockOfferings = mockk()
        coEvery { mockWorkflowManager.offeringIdByWorkflowId() } returns mapOf("wf1234" to "default")
        coEvery { mockWorkflowManager.getWorkflowBody(any()) } answers { uiWorkflow(firstArg()) }
        every { mockWorkflowManager.prewarmWorkflowAssets(any(), any()) } just Runs
        coEvery { mockUiConfigProvider.getUiConfig() } returns mockUiConfig
        every { mockOfferings.all } returns mapOf("default" to mockOffering)
        coEvery { mockAudiencesConfigProvider.getAudience(any()) } answers {
            val audienceId = firstArg<String>()
            Audience(id = audienceId, rules = "true")
        }
        configureRules(rule("wf1234"))
        resolver = CheckpointWorkflowResolverImpl(
            workflowManager = mockWorkflowManager,
            uiConfigProvider = mockUiConfigProvider,
            checkpointsConfigProvider = mockCheckpointsConfigProvider,
            audiencesConfigProvider = mockAudiencesConfigProvider,
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
    fun `checkpoint resolves NoAction with CONFIGURATION_UNAVAILABLE when workflows are disabled`() = runTest {
        resolver = CheckpointWorkflowResolverImpl(
            workflowManager = null,
            uiConfigProvider = null,
            checkpointsConfigProvider = null,
            audiencesConfigProvider = null,
            localRulesEvaluator = LocalRulesEvaluator(providers = emptyList()),
            getOfferings = { mockOfferings },
        )

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
    }

    @Test
    fun `checkpoint resolves NoAction with CONFIGURATION_UNAVAILABLE when remote config is disabled`() = runTest {
        configureResolution(CheckpointRulesResolution.Disabled)

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        coVerify(exactly = 0) { mockAudiencesConfigProvider.getAudience(any()) }
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
        coVerify(exactly = 0) { mockAudiencesConfigProvider.getAudience(any()) }
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
        coEvery { mockWorkflowManager.getWorkflowBody("wf1234") } throws PurchasesException(
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
    fun `checkpoint resolves MatchedWorkflow with the workflow and its offering`() = runTest {
        val resolution = resolve() as CheckpointResolution.MatchedWorkflow

        assertThat(resolution.workflow).isEqualTo(mockWorkflow)
        assertThat(resolution.uiConfig).isEqualTo(mockUiConfig)
        assertThat(resolution.offering).isEqualTo(mockOffering)
        verify(exactly = 1) { mockWorkflowManager.prewarmWorkflowAssets(mockWorkflow, mockUiConfig) }
    }

    @Test
    fun `the first matching audience determines the workflow`() = runTest {
        configureRules(rule("wf5678"), rule("wf1234"))
        coEvery { mockAudiencesConfigProvider.getAudience("aud_wf5678") } returns
            Audience("aud_wf5678", "false")
        coEvery { mockAudiencesConfigProvider.getAudience("aud_wf1234") } returns
            Audience("aud_wf1234", "true")

        val resolution = resolve() as CheckpointResolution.MatchedWorkflow

        assertThat(resolution.workflow).isEqualTo(mockWorkflow)
    }

    @Test
    fun `audiences after the first match are not loaded`() = runTest {
        configureRules(rule("wf1234"), rule("wf5678"))

        val resolution = resolve() as CheckpointResolution.MatchedWorkflow

        assertThat(resolution.workflow).isEqualTo(mockWorkflow)
        coVerify(exactly = 0) { mockAudiencesConfigProvider.getAudience("aud_wf5678") }
    }

    @Test
    fun `a missing audience before a match is configuration unavailable`() = runTest {
        configureRules(rule("missing"), rule("wf1234"))
        coEvery { mockAudiencesConfigProvider.getAudience("aud_missing") } returns null

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        coVerify(exactly = 0) { mockAudiencesConfigProvider.getAudience("aud_wf1234") }
    }

    @Test
    fun `false audiences resolve to no match`() = runTest {
        coEvery { mockAudiencesConfigProvider.getAudience("aud_wf1234") } returns
            Audience("aud_wf1234", "false")

        assertThat(noActionReason(resolve())).isEqualTo(CheckpointResolution.NoAction.Reason.NO_MATCH)
        assertThat(offeringsFetched).isZero()
    }

    @Test
    fun `a malformed audience before a match does not prevent a later matching workflow`() = runTest {
        configureRules(rule("wf5678"), rule("wf1234"))
        coEvery { mockAudiencesConfigProvider.getAudience("aud_wf5678") } returns
            Audience("aud_wf5678", "{not json")
        coEvery { mockAudiencesConfigProvider.getAudience("aud_wf1234") } returns
            Audience("aud_wf1234", "true")

        val resolution = resolve() as CheckpointResolution.MatchedWorkflow

        assertThat(resolution.workflow).isEqualTo(mockWorkflow)
        coVerify(exactly = 0) { mockWorkflowManager.getWorkflowBody("wf5678") }
    }

    @Test
    fun `a matched rule whose workflow is not mapped to an offering does not fall through`() = runTest {
        configureRules(rule("wf5678"), rule("wf1234"))
        coEvery { mockWorkflowManager.offeringIdByWorkflowId() } returns mapOf("wf1234" to "default")

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        coVerify(exactly = 0) { mockWorkflowManager.getWorkflowBody("wf1234") }
    }

    @Test
    fun `a matched rule whose offering is missing from offerings does not fall through`() = runTest {
        configureRules(rule("wf5678"), rule("wf1234"))
        coEvery { mockWorkflowManager.offeringIdByWorkflowId() } returns
            mapOf("wf5678" to "missing", "wf1234" to "default")

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        coVerify(exactly = 0) { mockWorkflowManager.getWorkflowBody("wf1234") }
    }

    @Test
    fun `a matched rule whose workflow fails to load does not fall through`() = runTest {
        configureRules(rule("wf5678"), rule("wf1234"))
        coEvery { mockWorkflowManager.offeringIdByWorkflowId() } returns
            mapOf("wf5678" to "default", "wf1234" to "default")
        coEvery { mockWorkflowManager.getWorkflowBody("wf5678") } throws PurchasesException(
            PurchasesError(PurchasesErrorCode.UnknownError, "Workflow unavailable."),
        )

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        coVerify(exactly = 0) { mockWorkflowManager.getWorkflowBody("wf1234") }
    }

    @Test
    fun `checkpoint resolves NoAction with CONFIGURATION_UNAVAILABLE when audiences cannot be evaluated`() =
        runTest {
            resolver = CheckpointWorkflowResolverImpl(
                workflowManager = mockWorkflowManager,
                uiConfigProvider = mockUiConfigProvider,
                checkpointsConfigProvider = mockCheckpointsConfigProvider,
                audiencesConfigProvider = mockAudiencesConfigProvider,
                localRulesEvaluator = LocalRulesEvaluator(providers = listOf(FailingDimensionProvider)),
                getOfferings = { mockOfferings },
            )

            assertThat(noActionReason(resolve()))
                .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
            coVerify(exactly = 0) { mockWorkflowManager.getWorkflowBody(any()) }
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
    fun `a custom variable the audience requires resolves the workflow`() = runTest {
        coEvery { mockAudiencesConfigProvider.getAudience("aud_wf1234") } returns
            Audience("aud_wf1234", """{"==": [{"var": "custom.source"}, "settings"]}""")

        val resolution = resolver.resolve(checkpointId, mapOf("source" to RulesDimensionValue.StringValue("settings")))

        assertThat(resolution).isInstanceOf(CheckpointResolution.MatchedWorkflow::class.java)
    }

    @Test
    fun `a custom variable the audience does not accept resolves NoAction with NO_MATCH`() = runTest {
        coEvery { mockAudiencesConfigProvider.getAudience("aud_wf1234") } returns
            Audience("aud_wf1234", """{"==": [{"var": "custom.source"}, "settings"]}""")

        assertThat(noActionReason(resolver.resolve(checkpointId, mapOf("source" to RulesDimensionValue.StringValue("onboarding")))))
            .isEqualTo(CheckpointResolution.NoAction.Reason.NO_MATCH)
    }

    @Test
    fun `a custom variable the audience requires but the app omitted is not a NO_MATCH`() = runTest {
        // The audience asks about a variable the call never supplied, so the SDK cannot place this
        // customer inside or outside it. Saying NO_MATCH would claim an answer it does not have.
        coEvery { mockAudiencesConfigProvider.getAudience("aud_wf1234") } returns
            Audience("aud_wf1234", """{"==": [{"var": "custom.source"}, "settings"]}""")

        assertThat(noActionReason(resolver.resolve(checkpointId, emptyMap())))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
    }

    @Test
    fun `negating an audience on an omitted variable does not manufacture a match`() = runTest {
        // Negation is where an unanswerable comparison does the most damage: a false inner result
        // becomes a match, admitting exactly the customers the audience was written to exclude.
        coEvery { mockAudiencesConfigProvider.getAudience("aud_wf1234") } returns
            Audience("aud_wf1234", """{"!": [{"==": [{"var": "custom.source"}, "settings"]}]}""")

        val resolution = resolver.resolve(checkpointId, emptyMap())

        assertThat(resolution).isNotInstanceOf(CheckpointResolution.MatchedWorkflow::class.java)
        assertThat(noActionReason(resolution))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
    }

    @Test
    fun `offerings and ui config are resolved once`() = runTest {
        configureRules(rule("wf1234"), rule("wf5678"))
        coEvery { mockWorkflowManager.offeringIdByWorkflowId() } returns
            mapOf("wf1234" to "default", "wf5678" to "default")

        assertThat(resolve()).isInstanceOf(CheckpointResolution.MatchedWorkflow::class.java)
        assertThat(offeringsFetched).isEqualTo(1)
        coVerify(exactly = 1) { mockUiConfigProvider.getUiConfig() }
        coVerify(exactly = 1) { mockWorkflowManager.offeringIdByWorkflowId() }
    }

    @Test
    fun `offering checkpoint resolves CONFIGURATION_UNAVAILABLE when no UI config provider is installed`() = runTest {
        coEvery { mockWorkflowManager.getWorkflowBody("wf1234") } returns offeringWorkflow("wf1234", "default")
        resolver = CheckpointWorkflowResolverImpl(
            workflowManager = mockWorkflowManager,
            uiConfigProvider = null,
            checkpointsConfigProvider = mockCheckpointsConfigProvider,
            audiencesConfigProvider = mockAudiencesConfigProvider,
            localRulesEvaluator = LocalRulesEvaluator(providers = emptyList()),
            getOfferings = {
                offeringsFetched++
                mockOfferings
            },
        )

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        assertThat(offeringsFetched).isZero()
        coVerify(exactly = 0) { mockWorkflowManager.getWorkflowBody(any()) }
    }

    @Test
    fun `terminal offering workflow requires ui config`() = runTest {
        coEvery { mockWorkflowManager.getWorkflowBody("wf1234") } returns offeringWorkflow("wf1234", "default")

        val resolution = resolve() as CheckpointResolution.MatchedOffering

        assertThat(resolution.offering).isEqualTo(mockOffering)
        assertThat(offeringsFetched).isEqualTo(1)
        coVerify(exactly = 0) { mockWorkflowManager.offeringIdByWorkflowId() }
        coVerify(exactly = 1) { mockUiConfigProvider.getUiConfig() }
        verify(exactly = 0) { mockWorkflowManager.prewarmWorkflowAssets(any(), any()) }
    }

    @Test
    fun `offering checkpoint resolves CONFIGURATION_UNAVAILABLE when ui config is unavailable`() = runTest {
        coEvery { mockWorkflowManager.getWorkflowBody("wf1234") } returns offeringWorkflow("wf1234", "default")
        coEvery { mockUiConfigProvider.getUiConfig() } returns null

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        assertThat(offeringsFetched).isZero()
        coVerify(exactly = 0) { mockWorkflowManager.getWorkflowBody(any()) }
    }

    @Test
    fun `offering step ancillary fields do not prevent resolution`() = runTest {
        val step = offeringStep("default").copy(
            screenId = "screen-id",
            triggers = listOf(
                WorkflowTrigger(
                    name = "continue",
                    type = WorkflowTriggerType.ON_PRESS,
                    actionId = "action-id",
                    componentId = "component-id",
                ),
            ),
            outputs = mapOf("selected" to JsonPrimitive(true)),
            triggerActions = mapOf("action-id" to WorkflowTriggerAction.Unknown),
            metadata = JsonPrimitive("metadata"),
        )
        coEvery { mockWorkflowManager.getWorkflowBody("wf1234") } returns workflow("wf1234", step)

        assertThat(resolve()).isInstanceOf(CheckpointResolution.MatchedOffering::class.java)
    }

    @Test
    fun `unsupported workflow shapes do not fall through to a later rule`() = runTest {
        val baseOfferingStep = offeringStep("default")
        val invalidWorkflows = listOf(
            workflow(
                "multiple-offering-steps",
                baseOfferingStep,
                baseOfferingStep.copy(id = "second-offering-step"),
            ),
            workflow(
                "mixed-offering-and-ui",
                baseOfferingStep,
                WorkflowStep("screen-step", "screen", screenId = "screen-id"),
            ),
            workflow(
                "mixed-ui-and-offering",
                WorkflowStep("screen-step", "screen", screenId = "screen-id"),
                baseOfferingStep,
            ),
            workflow("missing-initial-step", baseOfferingStep).copy(initialStepId = "missing-step"),
            workflow("mismatched-step-map-key", baseOfferingStep).copy(
                steps = mapOf("different-key" to baseOfferingStep),
            ),
            workflow("offering-without-identifier", baseOfferingStep.copy(paramValues = emptyMap())),
            workflow(
                "offering-with-non-string-identifier",
                baseOfferingStep.copy(paramValues = mapOf("offering_identifier" to JsonPrimitive(42))),
            ),
            workflow(
                "offering-with-null-identifier",
                baseOfferingStep.copy(paramValues = mapOf("offering_identifier" to JsonNull)),
            ),
            workflow("offering-with-blank-identifier", offeringStep("  ")),
        )
        configureRules(rule("wf-invalid"), rule("wf1234"))
        coEvery { mockWorkflowManager.getWorkflowBody("wf1234") } returns offeringWorkflow("wf1234", "default")

        invalidWorkflows.forEach { invalidWorkflow ->
            coEvery { mockWorkflowManager.getWorkflowBody("wf-invalid") } returns invalidWorkflow

            assertThat(noActionReason(resolve()))
                .describedAs(invalidWorkflow.displayName)
                .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        }
        assertThat(offeringsFetched).isZero()
        coVerify(exactly = 0) { mockWorkflowManager.getWorkflowBody("wf1234") }
    }

    @Test
    fun `UI rule without UI config does not fall through to a later offering rule`() = runTest {
        configureRules(rule("wf-ui"), rule("wf-offering"))
        coEvery { mockWorkflowManager.getWorkflowBody("wf-ui") } returns uiWorkflow("wf-ui")
        coEvery { mockWorkflowManager.getWorkflowBody("wf-offering") } returns
            offeringWorkflow("wf-offering", "default")
        coEvery { mockWorkflowManager.offeringIdByWorkflowId() } returns mapOf("wf-ui" to "default")
        coEvery { mockUiConfigProvider.getUiConfig() } returns null

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        coVerify(exactly = 1) { mockUiConfigProvider.getUiConfig() }
        coVerify(exactly = 0) { mockWorkflowManager.offeringIdByWorkflowId() }
        coVerify(exactly = 0) { mockWorkflowManager.getWorkflowBody(any()) }
        assertThat(offeringsFetched).isZero()
    }

    @Test
    fun `missing offering for offering rule does not fall through to a later UI rule`() = runTest {
        configureRules(rule("wf-offering"), rule("wf-ui"))
        coEvery { mockWorkflowManager.getWorkflowBody("wf-offering") } returns
            offeringWorkflow("wf-offering", "missing")
        coEvery { mockWorkflowManager.getWorkflowBody("wf-ui") } returns uiWorkflow("wf-ui")
        coEvery { mockWorkflowManager.offeringIdByWorkflowId() } returns mapOf("wf-ui" to "default")

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        assertThat(offeringsFetched).isEqualTo(1)
        coVerify(exactly = 0) { mockWorkflowManager.getWorkflowBody("wf-ui") }
    }

    @Test
    fun `offerings cancellation propagates`() = runTest {
        coEvery { mockWorkflowManager.getWorkflowBody("wf1234") } returns offeringWorkflow("wf1234", "default")
        resolver = CheckpointWorkflowResolverImpl(
            workflowManager = mockWorkflowManager,
            uiConfigProvider = mockUiConfigProvider,
            checkpointsConfigProvider = mockCheckpointsConfigProvider,
            audiencesConfigProvider = mockAudiencesConfigProvider,
            localRulesEvaluator = LocalRulesEvaluator(providers = emptyList()),
            getOfferings = { throw CancellationException("cancelled") },
        )

        val thrown = runCatching { resolve() }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(CancellationException::class.java)
    }

    private object FailingDimensionProvider : RulesDimensionProvider {
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

    private fun uiWorkflow(id: String): PublishedWorkflow = PublishedWorkflow(
        id = id,
        displayName = "UI workflow",
        initialStepId = "screen-step",
        steps = mapOf("screen-step" to WorkflowStep("screen-step", "screen", screenId = "screen-id")),
        screens = emptyMap(),
    )

    private fun offeringWorkflow(id: String, offeringIdentifier: String): PublishedWorkflow = PublishedWorkflow(
        id = id,
        displayName = "Offering workflow",
        initialStepId = "offering-step",
        steps = mapOf("offering-step" to offeringStep(offeringIdentifier)),
        screens = emptyMap(),
    )

    private fun workflow(id: String, vararg steps: WorkflowStep): PublishedWorkflow = PublishedWorkflow(
        id = id,
        displayName = id,
        initialStepId = steps.first().id,
        steps = steps.associateBy { it.id },
        screens = emptyMap(),
    )

    private fun offeringStep(offeringIdentifier: String): WorkflowStep = WorkflowStep(
        id = "offering-step",
        type = "offering",
        paramValues = mapOf("offering_identifier" to JsonPrimitive(offeringIdentifier)),
    )
}
