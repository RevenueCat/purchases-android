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
        configureAudiences(alwaysMatching("aud_a"), alwaysMatching("aud_b"))

        val resolved = resolver.resolve(
            branch(
                routes = listOf("aud_a" to "step_a", "aud_b" to "step_b"),
                fallbackStepId = "step_fallback",
            ),
        )

        assertThat(resolved).isEqualTo("step_a")
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
        configureAudiences(alwaysMatching("aud_a"), alwaysMatching("aud_b"))

        val resolved = resolver.resolveBranches(stepWithTwoBranchActions())

        assertThat(resolved).isEqualTo(mapOf("btn_one" to "step_a", "btn_two" to "step_b"))
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

    // region Helpers

    private fun alwaysMatching(audienceId: String) = Audience(id = audienceId, rules = "true")

    private fun neverMatching(audienceId: String) =
        Audience(id = audienceId, rules = """{"==": [1, 0]}""")

    private fun configureAudiences(vararg audiences: Audience) {
        coEvery { audiencesConfigProvider.getAudiences() } returns audiences.associateBy { it.id }
    }

    private fun branch(routes: List<Pair<String, String>>, fallbackStepId: String) =
        WorkflowTriggerAction.Branch(
            routes = routes.map { (audienceId, stepId) ->
                WorkflowTriggerAction.Branch.Route(audienceId = audienceId, stepId = stepId)
            },
            fallbackStepId = fallbackStepId,
        )

    /** One step whose two buttons each carry their own branch. */
    private fun stepWithTwoBranchActions() = WorkflowStep(
        id = "step_1",
        type = "screen",
        screenId = "screen_1",
        triggers = listOf(
            WorkflowTrigger(
                name = "One",
                type = WorkflowTriggerType.ON_PRESS,
                actionId = "btn_one",
                componentId = "btn_one",
            ),
            WorkflowTrigger(
                name = "Two",
                type = WorkflowTriggerType.ON_PRESS,
                actionId = "btn_two",
                componentId = "btn_two",
            ),
        ),
        triggerActions = mapOf(
            "btn_one" to branch(listOf("aud_a" to "step_a"), "step_fallback_1"),
            "btn_two" to branch(listOf("aud_b" to "step_b"), "step_fallback_2"),
        ),
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
