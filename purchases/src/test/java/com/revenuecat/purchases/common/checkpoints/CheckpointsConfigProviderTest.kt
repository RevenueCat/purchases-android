package com.revenuecat.purchases.common.checkpoints

import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.NoOpLogHandler
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.utils.Iso8601Utils
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
        assertThat(RemoteConfigTopic.Checkpoints.wireName).isEqualTo("checkpoint_rules")
    }

    @Test
    fun `getCheckpoint parses the backend payload in evaluation order`() = runTest {
        returnBlob(
            "app_open",
            """
            {
              "id": "chkpt_a1b2c3d4e5f6a7b8",
              "rules": [
                {
                  "id": "chkptrule_first",
                  "audience": "aud_public_1",
                  "workflow_id": "wf_public_1",
                  "schedule": {
                    "start": "2026-11-25T00:00:00Z",
                    "end": "2026-11-30T00:00:00Z"
                  }
                },
                {
                  "id": "chkptrule_second",
                  "audience": "aud_public_2",
                  "workflow_id": "wf_public_2"
                }
              ]
            }
            """.trimIndent(),
        )

        val checkpoint = requireNotNull(provider.getCheckpoint("app_open"))

        assertThat(checkpoint.identifier).isEqualTo("app_open")
        assertThat(checkpoint.id).isEqualTo("chkpt_a1b2c3d4e5f6a7b8")
        assertThat(checkpoint.rules.map { it.workflowId })
            .containsExactly("wf_public_1", "wf_public_2")
        assertThat(checkpoint.rules.first().id).isEqualTo("chkptrule_first")
        assertThat(checkpoint.rules.first().audienceId).isEqualTo("aud_public_1")
        assertThat(checkpoint.rules.first().schedule?.start)
            .isEqualTo(Iso8601Utils.parse("2026-11-25T00:00:00Z"))
        assertThat(checkpoint.rules.first().schedule?.end)
            .isEqualTo(Iso8601Utils.parse("2026-11-30T00:00:00Z"))
        assertThat(checkpoint.rules.last().schedule).isNull()
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
                  "audience": "aud-first",
                  "workflow_id": "wf-first",
                  "unknown_rule_field": "ignored"
                },
                "not-an-object",
                { "id": "missing-audience", "workflow_id": "wf-missing-audience" },
                { "id": "empty-audience", "audience": "", "workflow_id": "wf-empty-audience" },
                { "id": "wrong-audience", "audience": 123, "workflow_id": "wf-wrong-audience" },
                { "id": "missing-workflow", "audience": "aud-missing-workflow" },
                { "id": "empty-workflow", "audience": "aud-empty-workflow", "workflow_id": "" },
                { "id": "wrong-workflow", "audience": "aud-wrong-workflow", "workflow_id": 456 },
                { "id": "last", "audience": "aud-last", "workflow_id": "wf-last" }
              ]
            }
            """.trimIndent(),
        )

        val checkpoint = requireNotNull(provider.getCheckpoint("onboarding"))

        assertThat(checkpoint.rules.map { it.id }).containsExactly("first", "last")
        assertThat(checkpoint.rules.map { it.workflowId }).containsExactly("wf-first", "wf-last")
    }

    @Test
    fun `checkpoint payload deserialization isolates malformed rules`() {
        val payload = JsonTools.json.decodeFromString<CheckpointPayload>(
            """
            {
              "id": "chkpt_1",
              "rules": [
                { "id": "first", "audience": "aud-first", "workflow_id": "wf-first" },
                "not-an-object",
                { "id": "missing-audience", "workflow_id": "wf-missing-audience" },
                { "id": "last", "audience": "aud-last", "workflow_id": "wf-last" }
              ]
            }
            """.trimIndent(),
        )

        val checkpoint = payload.toCheckpointResponse("onboarding")

        assertThat(checkpoint.identifier).isEqualTo("onboarding")
        assertThat(checkpoint.id).isEqualTo("chkpt_1")
        assertThat(checkpoint.rules.map { it.id }).containsExactly("first", "last")
    }

    @Test
    fun `getCheckpoint ignores unsupported frequency cap fields`() = runTest {
        returnBlob(
            "onboarding",
            """
            {
              "rules": [
                {
                  "id": "rule",
                  "audience": "aud-1",
                  "workflow_id": "wf-1",
                  "frequency_cap": { "unsupported": true }
                }
              ]
            }
            """.trimIndent(),
        )

        val checkpoint = requireNotNull(provider.getCheckpoint("onboarding"))

        assertThat(checkpoint.rules.single().id).isEqualTo("rule")
    }

    @Test
    fun `getCheckpoint skips a rule when a supplied schedule bound is malformed`() = runTest {
        returnBlob(
            "onboarding",
            """
            {
              "rules": [
                {
                  "id": "invalid-start",
                  "audience": "aud-1",
                  "workflow_id": "wf-1",
                  "schedule": {
                    "start": "not-a-date",
                    "end": "2026-11-30T00:00:00Z"
                  }
                },
                {
                  "id": "empty-schedule",
                  "audience": "aud-2",
                  "workflow_id": "wf-2",
                  "schedule": {}
                }
              ]
            }
            """.trimIndent(),
        )

        val checkpoint = requireNotNull(provider.getCheckpoint("onboarding"))

        assertThat(checkpoint.rules).isEmpty()
    }

    @Test
    fun `getCheckpoint parses an open-ended schedule`() = runTest {
        returnBlob(
            "onboarding",
            """
            {
              "rules": [
                {
                  "id": "scheduled",
                  "audience": "aud-1",
                  "workflow_id": "wf-1",
                  "schedule": { "start": "2026-11-25T00:00:00.251Z" }
                }
              ]
            }
            """.trimIndent(),
        )

        val schedule = requireNotNull(provider.getCheckpoint("onboarding")?.rules?.single()?.schedule)

        assertThat(schedule.start).isEqualTo(Iso8601Utils.parse("2026-11-25T00:00:00.251Z"))
        assertThat(schedule.end).isNull()
    }

    @Test
    fun `getCheckpoint keeps a checkpoint with no rules`() = runTest {
        returnBlob("onboarding", """{ "id": "chkpt_1" }""")

        val checkpoint = requireNotNull(provider.getCheckpoint("onboarding"))

        assertThat(checkpoint.identifier).isEqualTo("onboarding")
        assertThat(checkpoint.id).isEqualTo("chkpt_1")
        assertThat(checkpoint.rules).isEmpty()
    }

    @Test
    fun `getCheckpoint returns null when the checkpoint is unavailable`() = runTest {
        coEvery {
            manager.blobData(
                RemoteConfigTopic.Checkpoints,
                "missing",
                any<(ByteArray) -> CheckpointPayload?>(),
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
                RemoteConfigTopic.Checkpoints,
                identifier,
                any<(ByteArray) -> CheckpointPayload?>(),
            )
        } answers {
            thirdArg<(ByteArray) -> CheckpointPayload?>().invoke(json.toByteArray())
        }
    }
}
