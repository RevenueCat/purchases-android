@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.audiences.Audience
import com.revenuecat.purchases.common.audiences.AudiencesConfigProvider
import com.revenuecat.purchases.common.localrules.LocalRulesEvaluator
import com.revenuecat.purchases.common.localrules.RulesDimensionValue
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class BranchResolverTest {

    private lateinit var audiencesConfigProvider: AudiencesConfigProvider
    private lateinit var resolver: BranchResolverImpl

    @Before
    fun setup() {
        audiencesConfigProvider = mockk()
        resolver = BranchResolverImpl(
            audiencesConfigProvider = audiencesConfigProvider,
            localRulesEvaluator = LocalRulesEvaluator(providers = emptyList(), currentAppUserId = { "user" }),
        )
    }

    @Test
    fun `the first matching audience decides the route`() = runTest {
        configureAudiences(neverMatching("aud_a"), alwaysMatching("aud_b"))

        val resolved = resolver.resolve(
            branch(
                routes = listOf("aud_a" to "step_a", "aud_b" to "step_b"),
                fallbackStepId = "step_fallback",
            ),
        )

        assertThat(resolved).isEqualTo("step_b")
    }

    @Test
    fun `no matching audience takes the fallback`() = runTest {
        configureAudiences(neverMatching("aud_a"))

        val resolved = resolver.resolve(branch(listOf("aud_a" to "step_a"), "step_fallback"))

        assertThat(resolved).isEqualTo("step_fallback")
    }

    @Test
    fun `an unreadable audience does not stop a later one from winning`() = runTest {
        configureAudiences(alwaysMatching("aud_b"))

        val resolved = resolver.resolve(
            branch(listOf("aud_missing" to "step_a", "aud_b" to "step_b"), "step_fallback"),
        )

        assertThat(resolved).isEqualTo("step_b")
    }

    @Test
    fun `an unavailable configuration takes the fallback`() = runTest {
        coEvery { audiencesConfigProvider.getAudiences() } returns null

        val resolved = resolver.resolve(branch(listOf("aud_a" to "step_a"), "step_fallback"))

        assertThat(resolved).isEqualTo("step_fallback")
    }

    @Test
    fun `a branch with no routes never reads the configuration`() = runTest {
        val resolved = resolver.resolve(branch(emptyList(), "step_fallback"))

        assertThat(resolved).isEqualTo("step_fallback")
        coVerify(exactly = 0) { audiencesConfigProvider.getAudiences() }
    }

    @Test
    fun `resolveBranches covers every branch on the step`() = runTest {
        configureAudiences(alwaysMatching("aud_a"))

        val resolved = resolver.resolveBranches(workflowWithTwoBranches().steps.getValue("step_1"))

        assertThat(resolved).isEqualTo(mapOf("action-next" to "step_a"))
    }

    // Only the step being entered is resolved, so a later step's branch is not evaluated yet.
    @Test
    fun `resolveBranches ignores other steps branches`() = runTest {
        configureAudiences(alwaysMatching("aud_a"), alwaysMatching("aud_b"))

        val resolved = resolver.resolveBranches(workflowWithTwoBranches().steps.getValue("step_2"))

        assertThat(resolved).isEqualTo(mapOf("action-next" to "step_b"))
    }

    @Test
    fun `resolveBranches returns an empty map for a step with no branches`() = runTest {
        val step = workflowWithTwoBranches().steps.getValue("step_1").copy(triggerActions = emptyMap())

        assertThat(resolver.resolveBranches(step)).isEmpty()
    }

    @Test
    fun `branch audiences can read the caller's custom variables`() = runTest {
        configureAudiences(
            Audience(id = "aud_a", rules = """{"==": [{"var": "custom.tier"}, "gold"]}"""),
        )

        val resolved = resolver.resolve(
            branch(listOf("aud_a" to "step_a"), "step_fallback"),
            customVariables = mapOf("tier" to RulesDimensionValue.StringValue("gold")),
        )

        assertThat(resolved).isEqualTo("step_a")
    }

    @Test
    fun `the disabled resolver always takes the fallback`() = runTest {
        val resolved = DisabledBranchResolver.resolve(branch(listOf("aud_a" to "step_a"), "step_fallback"))

        assertThat(resolved).isEqualTo("step_fallback")
    }

    // region Helpers

    private fun alwaysMatching(audienceId: String) = Audience(id = audienceId, rules = "true")

    private fun neverMatching(audienceId: String) =
        Audience(id = audienceId, rules = """{"==": [1, 0]}""")

    private fun configureAudiences(vararg audiences: Audience) {
        coEvery { audiencesConfigProvider.getAudiences() } returns audiences.associateBy { it.id }
    }

    private fun branch(routes: List<Pair<String, String>>, fallbackStepId: String) =
        WorkflowTriggerAction.Branch(
            branches = routes.map { (audienceId, stepId) ->
                WorkflowTriggerAction.Branch.Route(audienceId = audienceId, stepId = stepId)
            },
            fallbackStepId = fallbackStepId,
        )

    /** `step_1` branches on `aud_a`, `step_2` branches on `aud_b`. */
    private fun workflowWithTwoBranches(): PublishedWorkflow {
        fun step(id: String, audienceId: String, routeStepId: String, fallbackStepId: String) = WorkflowStep(
            id = id,
            type = "screen",
            screenId = "screen_$id",
            triggers = listOf(
                WorkflowTrigger(
                    name = "Next",
                    type = WorkflowTriggerType.ON_PRESS,
                    actionId = "action-next",
                    componentId = "btn-next",
                ),
            ),
            triggerActions = mapOf(
                "action-next" to branch(listOf(audienceId to routeStepId), fallbackStepId),
            ),
        )

        return PublishedWorkflow(
            id = "wf_test",
            displayName = "Test Workflow",
            initialStepId = "step_1",
            steps = mapOf(
                "step_1" to step("step_1", "aud_a", "step_a", "step_fallback_1"),
                "step_2" to step("step_2", "aud_b", "step_b", "step_fallback_2"),
            ),
            screens = emptyMap(),
        )
    }

    // endregion
}
