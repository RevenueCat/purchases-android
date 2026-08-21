package com.revenuecat.purchases.common.checkpoints

import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.NoOpLogHandler
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.remoteconfig.ConfigTopic
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.common.remoteconfig.RemoteConfiguration
import io.mockk.MockKAnswerScope
import io.mockk.MockKMatcherScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class CheckpointsConfigProviderTest {
    private val manager = mockk<RemoteConfigManager>()
    private val provider = CheckpointsConfigProvider(manager)
    private val originalLogHandler = currentLogHandler

    @Before
    fun setup() {
        currentLogHandler = NoOpLogHandler
        every { manager.isDisabled } returns false
        every { manager.configGeneration } returns 0
        coEvery { manager.committedTopicOrNull(RemoteConfigTopic.CheckpointRules) } returns null
    }

    @After
    fun tearDown() {
        currentLogHandler = originalLogHandler
    }

    @Test
    fun `checkpoints use the backend checkpoint_rules topic`() {
        assertThat(RemoteConfigTopic.CheckpointRules.wireName).isEqualTo("checkpoint_rules")
    }

    @Test
    fun `resolveCheckpoint returns the parsed backend payload unchanged`() = runTest {
        returnBlob(
            "app_open",
            """
            {
              "rules": [
                {
                  "id": "chkptrule_first",
                  "audience_id": "aud_public_1",
                  "workflow_id": "wf_public_1"
                },
                {
                  "id": "chkptrule_second",
                  "audience_id": "aud_public_2",
                  "workflow_id": "wf_public_2"
                }
              ]
            }
            """.trimIndent(),
        )

        assertThat(checkpoint("app_open")).isEqualTo(
            CheckpointResponse(
                rules = listOf(
                    CheckpointRule(
                        id = "chkptrule_first",
                        audienceId = "aud_public_1",
                        workflowId = "wf_public_1",
                    ),
                    CheckpointRule(
                        id = "chkptrule_second",
                        audienceId = "aud_public_2",
                        workflowId = "wf_public_2",
                    ),
                ),
            ),
        )
    }

    @Test
    fun `resolveCheckpoint isolates malformed rules and preserves valid rule order`() = runTest {
        returnBlob(
            "onboarding",
            """
            {
              "unknown_checkpoint_field": true,
              "rules": [
                {
                  "id": "first",
                  "audience_id": "aud-first",
                  "workflow_id": "wf-first",
                  "unknown_rule_field": "ignored"
                },
                "not-an-object",
                { "id": "missing-audience", "workflow_id": "wf-missing-audience" },
                { "id": "empty-audience", "audience_id": "", "workflow_id": "wf-empty-audience" },
                { "id": "wrong-audience", "audience_id": 123, "workflow_id": "wf-wrong-audience" },
                { "id": "missing-workflow", "audience_id": "aud-missing-workflow" },
                { "id": "empty-workflow", "audience_id": "aud-empty-workflow", "workflow_id": "" },
                { "id": "wrong-workflow", "audience_id": "aud-wrong-workflow", "workflow_id": 456 },
                { "id": "last", "audience_id": "aud-last", "workflow_id": "wf-last" }
              ]
            }
            """.trimIndent(),
        )

        val checkpoint = checkpoint("onboarding")

        assertThat(checkpoint.rules.map { it.id }).containsExactly("first", "last")
        assertThat(checkpoint.rules.map { it.workflowId }).containsExactly("wf-first", "wf-last")
    }

    @Test
    fun `checkpoint response deserialization isolates malformed rules`() {
        val decodedCheckpoint = JsonTools.json.decodeFromString<CheckpointResponse>(
            """
            {
              "rules": [
                { "id": "first", "audience_id": "aud-first", "workflow_id": "wf-first" },
                "not-an-object",
                { "id": "missing-audience", "workflow_id": "wf-missing-audience" },
                { "id": "last", "audience_id": "aud-last", "workflow_id": "wf-last" }
              ]
            }
            """.trimIndent(),
        )

        assertThat(decodedCheckpoint.rules.map { it.id }).containsExactly("first", "last")
    }

    @Test
    fun `resolveCheckpoint ignores the unused checkpoint id`() = runTest {
        returnBlob(
            "onboarding",
            """
            {
              "id": { "unexpected": "shape" },
              "rules": [
                { "id": "rule", "audience_id": "aud-1", "workflow_id": "wf-1" }
              ]
            }
            """.trimIndent(),
        )

        assertThat(checkpoint("onboarding").rules.single().id).isEqualTo("rule")
    }

    @Test
    fun `resolveCheckpoint keeps a checkpoint with no rules`() = runTest {
        returnBlob("onboarding", "{}")

        assertThat(checkpoint("onboarding").rules).isEmpty()
    }

    @Test
    fun `resolveCheckpoint reports Unavailable when the topic is not committed`() = runTest {
        returnNoBlob("missing")

        assertThat(provider.resolveCheckpoint("missing")).isEqualTo(CheckpointRulesResolution.Unavailable)
    }

    @Test
    fun `resolveCheckpoint reports NotConfigured when the topic carries no item for the identifier`() = runTest {
        returnNoBlob("missing")
        commitTopicWith("app_open")

        assertThat(provider.resolveCheckpoint("missing")).isEqualTo(CheckpointRulesResolution.NotConfigured)
    }

    @Test
    fun `resolveCheckpoint reports Disabled when the endpoint is disabled`() = runTest {
        returnNoBlob("app_open")
        every { manager.isDisabled } returns true

        assertThat(provider.resolveCheckpoint("app_open")).isEqualTo(CheckpointRulesResolution.Disabled)
    }

    @Test
    fun `resolveCheckpoint reports Unavailable when a committed checkpoint's blob cannot be resolved`() = runTest {
        returnNoBlob("app_open")
        commitTopicWith("app_open")

        assertThat(provider.resolveCheckpoint("app_open")).isEqualTo(CheckpointRulesResolution.Unavailable)
    }

    @Test
    fun `resolveCheckpoint reports Unavailable for a malformed checkpoint`() = runTest {
        returnBlob("malformed", "not-json")
        commitTopicWith("malformed")

        assertThat(provider.resolveCheckpoint("malformed")).isEqualTo(CheckpointRulesResolution.Unavailable)
    }

    @Test
    fun `resolveCheckpoint reports Unavailable for a checkpoint that is not an object`() = runTest {
        returnBlob("malformed", """["not", "an", "object"]""")
        commitTopicWith("malformed")

        assertThat(provider.resolveCheckpoint("malformed")).isEqualTo(CheckpointRulesResolution.Unavailable)
    }

    @Test
    fun `resolveCheckpoint reads once when the config generation is stable`() = runTest {
        returnBlob("app_open", """{ "rules": [] }""")

        provider.resolveCheckpoint("app_open")

        coVerify(exactly = 1) { blobRead("app_open") }
    }

    @Test
    fun `resolveCheckpoint reads again when the config generation changes during the read`() = runTest {
        every { manager.configGeneration } returnsMany listOf(0, 1)
        returnNoBlobThenBlob(
            "app_open",
            """{ "rules": [{ "id": "rule", "audience_id": "aud-1", "workflow_id": "wf-1" }] }""",
        )

        assertThat(checkpoint("app_open").rules.single().workflowId).isEqualTo("wf-1")
    }

    @Test
    fun `resolveCheckpoint reads at most twice when the config generation changes`() = runTest {
        every { manager.configGeneration } returnsMany listOf(0, 1)
        returnNoBlob("missing")

        assertThat(provider.resolveCheckpoint("missing")).isEqualTo(CheckpointRulesResolution.Unavailable)
        coVerify(exactly = 2) { blobRead("missing") }
    }

    @Test
    fun `resolveCheckpoint reports Unavailable when the config changes during both reads`() = runTest {
        every { manager.configGeneration } returnsMany listOf(0, 1, 1, 2)
        returnBlob("app_open", """{ "rules": [] }""")

        assertThat(provider.resolveCheckpoint("app_open")).isEqualTo(CheckpointRulesResolution.Unavailable)
        coVerify(exactly = 2) { blobRead("app_open") }
    }

    private suspend fun checkpoint(identifier: String): CheckpointResponse =
        (provider.resolveCheckpoint(identifier) as CheckpointRulesResolution.Found).checkpoint

    private fun commitTopicWith(identifier: String) {
        coEvery { manager.committedTopicOrNull(RemoteConfigTopic.CheckpointRules) } returns ConfigTopic(
            mapOf(identifier to RemoteConfiguration.ConfigItem(blobRef = "blob_$identifier")),
        )
    }

    private suspend fun MockKMatcherScope.blobRead(identifier: String): CheckpointResponse? =
        manager.blobData(
            RemoteConfigTopic.CheckpointRules,
            identifier,
            any<(ByteArray) -> CheckpointResponse?>(),
        )

    private fun returnNoBlob(identifier: String) {
        coEvery { blobRead(identifier) } returns null
    }

    private fun returnBlob(identifier: String, json: String) {
        coEvery { blobRead(identifier) } answers { parseBlob(json) }
    }

    private fun returnNoBlobThenBlob(identifier: String, json: String) {
        var reads = 0
        coEvery { blobRead(identifier) } answers { if (reads++ == 0) null else parseBlob(json) }
    }

    private fun MockKAnswerScope<CheckpointResponse?, CheckpointResponse?>.parseBlob(json: String) =
        thirdArg<(ByteArray) -> CheckpointResponse?>().invoke(json.toByteArray())
}
