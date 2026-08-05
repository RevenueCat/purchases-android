package com.revenuecat.purchases.common.checkpoints

import com.revenuecat.purchases.NoOpLogHandler
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.remoteconfig.ConfigTopic
import com.revenuecat.purchases.common.remoteconfig.RemoteConfiguration
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
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
    fun `getCheckpoint decodes an inline checkpoint item`() = runTest {
        coEvery { manager.topic(RemoteConfigTopic.Checkpoints) } returns ConfigTopic(
            mapOf(
                "onboarding" to RemoteConfiguration.ConfigItem(metadata = buildJsonObject { }),
            ),
        )

        assertThat(provider.getCheckpoint("onboarding")).isNotNull
        coVerify(exactly = 0) {
            manager.blobData(
                RemoteConfigTopic.Checkpoints,
                "onboarding",
                any<(ByteArray) -> CheckpointResponse?>(),
            )
        }
    }

    @Test
    fun `getCheckpoint decodes a blob-backed checkpoint item`() = runTest {
        coEvery { manager.topic(RemoteConfigTopic.Checkpoints) } returns ConfigTopic(
            mapOf(
                "onboarding" to RemoteConfiguration.ConfigItem(blobRef = "checkpoint-blob"),
            ),
        )
        coEvery {
            manager.blobData(
                RemoteConfigTopic.Checkpoints,
                "onboarding",
                any<(ByteArray) -> CheckpointResponse?>(),
            )
        } answers {
            thirdArg<(ByteArray) -> CheckpointResponse?>().invoke("{}".toByteArray())
        }

        assertThat(provider.getCheckpoint("onboarding")).isNotNull
    }

    @Test
    fun `getCheckpoint returns null when the checkpoint is unavailable`() = runTest {
        coEvery { manager.topic(RemoteConfigTopic.Checkpoints) } returns ConfigTopic(emptyMap())

        assertThat(provider.getCheckpoint("missing")).isNull()
    }

    @Test
    fun `getCheckpoint returns null for a malformed checkpoint`() = runTest {
        coEvery { manager.topic(RemoteConfigTopic.Checkpoints) } returns ConfigTopic(
            mapOf(
                "malformed" to RemoteConfiguration.ConfigItem(blobRef = "checkpoint-blob"),
            ),
        )
        coEvery {
            manager.blobData(
                RemoteConfigTopic.Checkpoints,
                "malformed",
                any<(ByteArray) -> CheckpointResponse?>(),
            )
        } answers {
            thirdArg<(ByteArray) -> CheckpointResponse?>().invoke("not-json".toByteArray())
        }

        assertThat(provider.getCheckpoint("malformed")).isNull()
    }
}
