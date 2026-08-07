package com.revenuecat.purchases.common.checkpoints

import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.NoOpLogHandler
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import io.mockk.coEvery
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
    fun `getCheckpoint parses the backend payload in evaluation order`() = runTest {
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

        val checkpoint = requireNotNull(provider.getCheckpoint("app_open"))

        assertThat(checkpoint.identifier).isEqualTo("app_open")
        assertThat(checkpoint.rules.map { it.workflowId })
            .containsExactly("wf_public_1", "wf_public_2")
        assertThat(checkpoint.rules.first().id).isEqualTo("chkptrule_first")
        assertThat(checkpoint.rules.first().audienceId).isEqualTo("aud_public_1")
    }

    @Test
    fun `getCheckpoint isolates malformed rules and preserves valid rule order`() = runTest {
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

        val checkpoint = requireNotNull(provider.getCheckpoint("onboarding"))

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

        val checkpoint = decodedCheckpoint.copy(identifier = "onboarding")

        assertThat(decodedCheckpoint.identifier).isEmpty()
        assertThat(checkpoint.identifier).isEqualTo("onboarding")
        assertThat(checkpoint.rules.map { it.id }).containsExactly("first", "last")
    }

    @Test
    fun `getCheckpoint ignores the unused checkpoint id`() = runTest {
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

        val checkpoint = requireNotNull(provider.getCheckpoint("onboarding"))

        assertThat(checkpoint.rules.single().id).isEqualTo("rule")
    }

    @Test
    fun `getCheckpoint keeps a checkpoint with no rules`() = runTest {
        returnBlob("onboarding", "{}")

        val checkpoint = requireNotNull(provider.getCheckpoint("onboarding"))

        assertThat(checkpoint.identifier).isEqualTo("onboarding")
        assertThat(checkpoint.rules).isEmpty()
    }

    @Test
    fun `getCheckpoint returns null when the checkpoint is unavailable`() = runTest {
        coEvery {
            manager.blobData(
                RemoteConfigTopic.CheckpointRules,
                "missing",
                any<(ByteArray) -> CheckpointResponse?>(),
            )
        } returns null

        assertThat(provider.getCheckpoint("missing")).isNull()
    }

    @Test
    fun `getCheckpoint returns null for a malformed checkpoint`() = runTest {
        returnBlob("malformed", "not-json")

        assertThat(provider.getCheckpoint("malformed")).isNull()
    }

    @Test
    fun `getCheckpoint returns null for a checkpoint that is not an object`() = runTest {
        returnBlob("malformed", """["not", "an", "object"]""")

        assertThat(provider.getCheckpoint("malformed")).isNull()
    }

    private fun returnBlob(identifier: String, json: String) {
        coEvery {
            manager.blobData(
                RemoteConfigTopic.CheckpointRules,
                identifier,
                any<(ByteArray) -> CheckpointResponse?>(),
            )
        } answers {
            thirdArg<(ByteArray) -> CheckpointResponse?>().invoke(json.toByteArray())
        }
    }
}
