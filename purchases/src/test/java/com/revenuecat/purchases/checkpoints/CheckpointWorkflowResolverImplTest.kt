@file:OptIn(InternalRevenueCatAPI::class, ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.checkpoints

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.audiences.Audience
import com.revenuecat.purchases.common.audiences.AudiencesConfigProvider
import com.revenuecat.purchases.common.audiences.AudiencesSnapshot
import com.revenuecat.purchases.common.checkpoints.CheckpointResponse
import com.revenuecat.purchases.common.checkpoints.CheckpointRule
import com.revenuecat.purchases.common.checkpoints.CheckpointRulesResolution
import com.revenuecat.purchases.common.checkpoints.CheckpointsConfigProvider
import com.revenuecat.purchases.common.localrules.LocalRulesEvaluator
import com.revenuecat.purchases.common.localrules.RulesDimensionNamespace
import com.revenuecat.purchases.common.localrules.RulesDimensionProvider
import com.revenuecat.purchases.common.localrules.RulesDimensionValue
import com.revenuecat.purchases.common.remoteconfig.ConfigTopic
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.common.remoteconfig.RemoteConfiguration
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
import kotlinx.serialization.json.jsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class CheckpointWorkflowResolverImplTest {

    private val checkpointId = "test_checkpoint"

    private companion object {
        const val BACKEND_PREDICATE_HASH = "349OzehoTyCAdiZblj9w0J0yD-Uow8X3"
    }

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
        configureAudiences(
            alwaysMatching("aud_wf1234"),
            alwaysMatching("aud_wf5678"),
            alwaysMatching("aud_wf-ui"),
            alwaysMatching("aud_wf-offering"),
            alwaysMatching("aud_wf-invalid"),
        )
        every { mockCheckpointsConfigProvider.isCurrent(any()) } returns true
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
        coVerify(exactly = 0) { mockAudiencesConfigProvider.getSnapshot() }
    }

    @Test
    fun `checkpoint resolves NoAction with NO_MATCH when it has no rules`() = runTest {
        configureRules()

        assertThat(noActionReason(resolve())).isEqualTo(CheckpointResolution.NoAction.Reason.NO_MATCH)
        assertThat(offeringsFetched).isZero()
        // With no rules to match, there is nothing to read the audiences for.
        coVerify(exactly = 0) { mockAudiencesConfigProvider.getSnapshot() }
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
        configureAudiences(
            Audience("aud_wf5678", "false"),
            Audience("aud_wf1234", "true"),
        )

        val resolution = resolve() as CheckpointResolution.MatchedWorkflow

        assertThat(resolution.workflow).isEqualTo(mockWorkflow)
    }

    @Test
    fun `config changing once during the audiences read is retried`() = runTest {
        val generation = AtomicInteger(1)
        val snapshotReads = AtomicInteger(0)
        resolver = resolverBackedBy(
            stalenessManager(generation) { if (snapshotReads.getAndIncrement() == 0) generation.incrementAndGet() },
        )

        assertThat(resolve()).isInstanceOf(CheckpointResolution.MatchedWorkflow::class.java)
    }

    @Test
    fun `config changing on every audiences read is configuration unavailable after one retry`() = runTest {
        val generation = AtomicInteger(1)
        val snapshotReads = AtomicInteger(0)
        resolver = resolverBackedBy(
            stalenessManager(generation) {
                snapshotReads.incrementAndGet()
                generation.incrementAndGet()
            },
        )

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        // Two resolution attempts, each reading the snapshot twice (its own consistent re-read), and no more: a
        // burst of commits can't keep resolution spinning.
        assertThat(snapshotReads.get()).isEqualTo(4)
    }

    @Test
    fun `config changing once while resolving the workflow is retried`() = runTest {
        var generation = 0
        var workflowFetches = 0
        configureRulesReadAt { generation }
        coEvery { mockWorkflowManager.getWorkflowBody("wf1234") } answers {
            if (workflowFetches++ == 0) generation++
            mockWorkflow
        }

        assertThat(resolve()).isInstanceOf(CheckpointResolution.MatchedWorkflow::class.java)
    }

    @Test
    fun `config changing on every workflow resolution is configuration unavailable after one retry`() = runTest {
        var generation = 0
        configureRulesReadAt { generation }
        coEvery { mockWorkflowManager.getWorkflowBody("wf1234") } answers {
            generation++
            mockWorkflow
        }

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        // Two resolution attempts, no more: a burst of commits can't keep resolution spinning.
        coVerify(exactly = 2) { mockWorkflowManager.getWorkflowBody("wf1234") }
    }

    @Test
    fun `rules after the first match do not need their audience`() = runTest {
        configureRules(rule("wf1234"), rule("wf5678"))
        // aud_wf5678 is absent from the snapshot, so consulting it would fail the resolution.
        configureAudiences(alwaysMatching("aud_wf1234"))

        val resolution = resolve() as CheckpointResolution.MatchedWorkflow

        assertThat(resolution.workflow).isEqualTo(mockWorkflow)
        coVerify(exactly = 1) { mockAudiencesConfigProvider.getSnapshot() }
    }

    @Test
    fun `an unavailable audiences snapshot is configuration unavailable`() = runTest {
        coEvery { mockAudiencesConfigProvider.getSnapshot() } returns null

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        coVerify(exactly = 0) { mockWorkflowManager.getWorkflowBody(any()) }
    }

    @Test
    fun `a missing audience before a match is configuration unavailable`() = runTest {
        configureRules(rule("missing"), rule("wf1234"))
        configureAudiences(alwaysMatching("aud_wf1234"))

        assertThat(noActionReason(resolve()))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
        coVerify(exactly = 0) { mockWorkflowManager.getWorkflowBody(any()) }
    }

    @Test
    fun `false audiences resolve to no match`() = runTest {
        configureAudiences(Audience("aud_wf1234", "false"))

        assertThat(noActionReason(resolve())).isEqualTo(CheckpointResolution.NoAction.Reason.NO_MATCH)
        assertThat(offeringsFetched).isZero()
    }

    @Test
    fun `a malformed audience before a match does not prevent a later matching workflow`() = runTest {
        configureRules(rule("wf5678"), rule("wf1234"))
        configureAudiences(
            Audience("aud_wf5678", "{not json"),
            Audience("aud_wf1234", "true"),
        )

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
        configureAudiences(Audience("aud_wf1234", """{"==": [{"var": "custom.source"}, "settings"]}"""))

        val resolution = resolver.resolve(checkpointId, mapOf("source" to RulesDimensionValue.StringValue("settings")))

        assertThat(resolution).isInstanceOf(CheckpointResolution.MatchedWorkflow::class.java)
    }

    @Test
    fun `a custom variable the audience does not accept resolves NoAction with NO_MATCH`() = runTest {
        configureAudiences(Audience("aud_wf1234", """{"==": [{"var": "custom.source"}, "settings"]}"""))

        assertThat(noActionReason(resolver.resolve(checkpointId, mapOf("source" to RulesDimensionValue.StringValue("onboarding")))))
            .isEqualTo(CheckpointResolution.NoAction.Reason.NO_MATCH)
    }

    @Test
    fun `a custom variable the audience requires but the app omitted is not a NO_MATCH`() = runTest {
        // The audience asks about a variable the call never supplied, so the SDK cannot place this
        // customer inside or outside it. Saying NO_MATCH would claim an answer it does not have.
        configureAudiences(Audience("aud_wf1234", """{"==": [{"var": "custom.source"}, "settings"]}"""))

        assertThat(noActionReason(resolver.resolve(checkpointId, emptyMap())))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
    }

    @Test
    fun `negating an audience on an omitted variable does not manufacture a match`() = runTest {
        // Negation is where an unanswerable comparison does the most damage: a false inner result
        // becomes a match, admitting exactly the customers the audience was written to exclude.
        configureAudiences(Audience("aud_wf1234", """{"!": [{"==": [{"var": "custom.source"}, "settings"]}]}"""))

        val resolution = resolver.resolve(checkpointId, emptyMap())

        assertThat(resolution).isNotInstanceOf(CheckpointResolution.MatchedWorkflow::class.java)
        assertThat(noActionReason(resolution))
            .isEqualTo(CheckpointResolution.NoAction.Reason.CONFIGURATION_UNAVAILABLE)
    }

    @Test
    fun `a backend predicate result the audience reads resolves the workflow`() = runTest {
        configureAudiences(
            Audience(
                "aud_wf1234",
                """{"or": [
                    {"var": ["backend.$BACKEND_PREDICATE_HASH", false]},
                    {"==": [{"var": "custom.country"}, "PL"]}
                ]}""",
            ),
            backendPredicateResults = mapOf(BACKEND_PREDICATE_HASH to RulesDimensionValue.BoolValue(true)),
        )

        assertThat(resolve()).isInstanceOf(CheckpointResolution.MatchedWorkflow::class.java)
    }

    @Test
    fun `a false backend predicate result resolves NoAction with NO_MATCH`() = runTest {
        configureAudiences(
            Audience("aud_wf1234", """{"var": ["backend.$BACKEND_PREDICATE_HASH", false]}"""),
            backendPredicateResults = mapOf(BACKEND_PREDICATE_HASH to RulesDimensionValue.BoolValue(false)),
        )

        assertThat(noActionReason(resolve())).isEqualTo(CheckpointResolution.NoAction.Reason.NO_MATCH)
    }

    @Test
    fun `a non-boolean backend predicate result is compared like any other dimension`() = runTest {
        configureAudiences(
            Audience("aud_wf1234", """{"==": [{"var": ["backend.$BACKEND_PREDICATE_HASH", ""]}, "variant_b"]}"""),
            backendPredicateResults = mapOf(BACKEND_PREDICATE_HASH to RulesDimensionValue.StringValue("variant_b")),
        )

        assertThat(resolve()).isInstanceOf(CheckpointResolution.MatchedWorkflow::class.java)
    }

    @Test
    fun `a backend predicate result absent from the snapshot falls back to the rule's default`() = runTest {
        // A hash this SDK never received (a newer backend, a value shape it skipped) reads as the default the
        // rule was authored with, the same forward-compatibility path as any other unknown dimension.
        configureAudiences(Audience("aud_wf1234", """{"var": ["backend.$BACKEND_PREDICATE_HASH", false]}"""))

        assertThat(noActionReason(resolve())).isEqualTo(CheckpointResolution.NoAction.Reason.NO_MATCH)
    }

    @Test
    fun `a backend predicate read without a default is not a NO_MATCH when the hash is absent`() = runTest {
        configureAudiences(Audience("aud_wf1234", """{"var": "backend.$BACKEND_PREDICATE_HASH"}"""))

        assertThat(noActionReason(resolve()))
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

    private fun alwaysMatching(audienceId: String) = Audience(id = audienceId, rules = "true")

    private fun configureAudiences(
        vararg audiences: Audience,
        backendPredicateResults: Map<String, RulesDimensionValue> = emptyMap(),
    ) {
        coEvery { mockAudiencesConfigProvider.getSnapshot() } returns AudiencesSnapshot(
            audiences = audiences.associateBy { it.id },
            backendPredicateResults = backendPredicateResults,
        )
    }

    private fun configureRules(vararg rules: CheckpointRule) {
        configureResolution(
            CheckpointRulesResolution.Found(
                checkpoint = CheckpointResponse(rules = rules.toList()),
                configGeneration = 0,
            ),
        )
    }

    private fun configureResolution(resolution: CheckpointRulesResolution) {
        coEvery { mockCheckpointsConfigProvider.resolveCheckpoint(checkpointId) } returns resolution
    }

    /**
     * Rules that are always read against whatever [generation] is current, so an attempt only looks stale if the
     * generation moves after its own read.
     */
    private fun configureRulesReadAt(generation: () -> Int) {
        coEvery { mockCheckpointsConfigProvider.resolveCheckpoint(checkpointId) } answers {
            CheckpointRulesResolution.Found(
                checkpoint = CheckpointResponse(rules = listOf(rule("wf1234"))),
                configGeneration = generation(),
            )
        }
        every { mockCheckpointsConfigProvider.isCurrent(any()) } answers {
            firstArg<CheckpointRulesResolution.Found>().configGeneration == generation()
        }
    }

    /** A manager serving one checkpoint and one always-matching audience, running [onSnapshotRead] on each read. */
    private fun stalenessManager(generation: AtomicInteger, onSnapshotRead: () -> Unit): RemoteConfigManager =
        mockk<RemoteConfigManager>().also { manager ->
            every { manager.configGeneration } answers { generation.get() }
            coEvery {
                manager.blobData(
                    RemoteConfigTopic.CheckpointRules,
                    checkpointId,
                    any<(ByteArray) -> CheckpointResponse?>(),
                )
            } returns CheckpointResponse(rules = listOf(rule("wf1234")))
            coEvery {
                manager.blobData(
                    RemoteConfigTopic.Audiences,
                    "default",
                    any<(ByteArray) -> Map<String, Audience>?>(),
                )
            } answers {
                onSnapshotRead()
                mapOf("aud_wf1234" to Audience(id = "aud_wf1234", rules = """{"==":[1,1]}"""))
            }
            coEvery { manager.topic(RemoteConfigTopic.Audiences) } returns ConfigTopic(
                mapOf(
                    "backend_predicate_results" to RemoteConfiguration.ConfigItem(
                        metadata = JsonTools.json.parseToJsonElement("""{"hash": true}""").jsonObject,
                    ),
                ),
            )
        }

    private fun resolverBackedBy(manager: RemoteConfigManager) = CheckpointWorkflowResolverImpl(
        workflowManager = mockWorkflowManager,
        uiConfigProvider = mockUiConfigProvider,
        checkpointsConfigProvider = CheckpointsConfigProvider(manager),
        audiencesConfigProvider = AudiencesConfigProvider(manager),
        localRulesEvaluator = LocalRulesEvaluator(providers = emptyList()),
        getOfferings = { mockOfferings },
    )

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
