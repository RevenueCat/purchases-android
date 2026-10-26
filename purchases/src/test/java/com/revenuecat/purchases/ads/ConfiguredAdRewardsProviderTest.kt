package com.revenuecat.purchases.ads

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.remoteconfig.ConfigTopic
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.common.remoteconfig.RemoteConfiguration
import io.mockk.MockKMatcherScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class, ExperimentalCoroutinesApi::class)
class ConfiguredAdRewardsProviderTest {
    private val manager = mockk<RemoteConfigManager>(relaxed = true)
    private val provider = ConfiguredAdRewardsProvider(manager)
    private val originalLogHandler = currentLogHandler
    private val logHandler = mockk<LogHandler>(relaxed = true)
    private val configured = ConfiguredAdRewards(listOf(AdRewardConfiguration.VirtualCurrency("coins", 10)))
    private val currencyBlob = """{"unit":{"reward":{"type":"virtual_currency","code":"coins","amount":10}}}"""

    @Before
    fun setUp() {
        currentLogHandler = logHandler
        every { manager.configGeneration } returns 0
        every { manager.isDisabled } returns false
        coEvery { manager.awaitTopicAndPrefetchBlobsReady(RemoteConfigTopic.AdRewards) } returns ConfigTopic(
            mapOf("default" to RemoteConfiguration.ConfigItem(blobRef = "a".repeat(64))),
        )
        coEvery { manager.hasCommittedConfig() } returns true
    }

    @After
    fun tearDown() {
        provider.close()
        currentLogHandler = originalLogHandler
    }

    @Test
    fun `rewards use the backend ad_rewards topic`() {
        assertThat(RemoteConfigTopic.AdRewards.wireName).isEqualTo("ad_rewards")
    }

    @Test
    fun `empty ad unit returns null without reading config`() = runTest {
        assertThat(provider.rewards("")).isNull()
        verify { logHandler.w(any(), match { it.contains("ad unit ID") }) }
        coVerify(exactly = 0) { manager.awaitTopicAndPrefetchBlobsReady(any()) }
    }

    @Test
    fun `disabled remote config returns null without fetching`() = runTest {
        every { manager.isDisabled } returns true
        assertThat(provider.rewards("unit")).isNull()
        coVerify(exactly = 0) { manager.awaitTopicAndPrefetchBlobsReady(any()) }
    }

    @Test
    fun `absent topic in committed config means no rewards`() = runTest {
        coEvery { manager.awaitTopicAndPrefetchBlobsReady(RemoteConfigTopic.AdRewards) } returns null
        assertThat(provider.rewards("unit")).isEqualTo(ConfiguredAdRewards(emptyList()))
        coVerify(exactly = 0) { blobRead() }
    }

    @Test
    fun `no committed config returns null rather than empty rewards`() = runTest {
        coEvery { manager.awaitTopicAndPrefetchBlobsReady(RemoteConfigTopic.AdRewards) } returns null
        coEvery { manager.hasCommittedConfig() } returns false
        assertThat(provider.rewards("unit")).isNull()
        verify { logHandler.w(any(), match { it.contains("No committed remote configuration") }) }
    }

    @Test
    fun `missing default item or blob reference returns null`() = runTest {
        listOf(
            ConfigTopic(emptyMap()),
            ConfigTopic(mapOf("default" to RemoteConfiguration.ConfigItem())),
        ).forEach { topic ->
            coEvery { manager.awaitTopicAndPrefetchBlobsReady(RemoteConfigTopic.AdRewards) } returns topic
            assertThat(provider.rewards("unit")).isNull()
            verify { logHandler.w(any(), match { it.contains("default blob reference") }) }
        }
        coVerify(exactly = 0) { blobRead() }
    }

    @Test
    fun `unresolved blob returns null`() = runTest {
        coEvery { blobRead() } returns null
        assertThat(provider.rewards("unit")).isNull()
        verify { logHandler.w(any(), match { it.contains("blob could not be resolved") }) }
    }

    @Test
    fun `provider resolves through the shared blob API without a second cache`() = runTest {
        var payload: String? = currencyBlob
        coEvery { blobRead() } answers {
            payload?.toByteArray()
        }
        assertThat(provider.rewards("unit")).isEqualTo(configured)
        payload = "{}"
        assertThat(provider.rewards("unit")).isEqualTo(ConfiguredAdRewards(emptyList()))
        payload = null
        assertThat(provider.rewards("unit")).isNull()
        coVerify(exactly = 3) { blobRead() }
    }

    @Test
    fun `malformed JSON returns null and logs decoding diagnostics`() = runTest {
        returnBlob("not JSON")
        assertThat(provider.rewards("unit")).isNull()
        verify {
            logHandler.w(any(), match {
                it.startsWith("Configured ad rewards unavailable: Configured ad rewards contain invalid JSON:") &&
                    it.contains("Unexpected JSON token")
            })
        }
    }

    @Test
    fun `superseded successful read retries using the shared consistency helper`() = runTest {
        var generation = 0
        every { manager.configGeneration } answers { generation }
        coEvery { blobRead() } answers {
            val bytes = if (generation == 0) {
                generation = 1
                currencyBlob
            } else "{}"
            bytes.toByteArray()
        }
        assertThat(provider.rewards("unit")).isEqualTo(ConfiguredAdRewards(emptyList()))
        coVerify(exactly = 2) { blobRead() }
    }

    @Test
    fun `superseded malformed read logs the failure and retries`() = runTest {
        var generation = 0
        every { manager.configGeneration } answers { generation }
        coEvery { blobRead() } answers {
            val bytes = if (generation == 0) "not JSON" else currencyBlob
            generation = 1
            bytes.toByteArray()
        }
        assertThat(provider.rewards("unit")).isEqualTo(configured)
        coVerify(exactly = 2) { blobRead() }
        verify(exactly = 1) { logHandler.w(any(), match { it.contains("invalid JSON") }) }
    }

    @Test
    fun `two superseded reads return null instead of stale data`() = runTest {
        var generation = 0
        every { manager.configGeneration } answers { generation }
        coEvery { blobRead() } answers {
            generation++
            currencyBlob.toByteArray()
        }
        assertThat(provider.rewards("unit")).isNull()
        coVerify(exactly = 2) { blobRead() }
    }

    @Test
    fun `one suspended read does not block another read`() = runTest {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var reads = 0
        coEvery { blobRead() } coAnswers {
            if (reads++ == 0) {
                started.complete(Unit)
                release.await()
            }
            currencyBlob.toByteArray()
        }
        val first = async { provider.rewards("unit") }
        started.await()
        val second = async { provider.rewards("unit") }
        assertThat(second.await()).isEqualTo(configured)
        assertThat(first.isCompleted).isFalse()
        release.complete(Unit)
        assertThat(first.await()).isEqualTo(configured)
    }

    @Test
    fun `cancellation propagates without becoming an unavailable result`() = runTest {
        coEvery { blobRead() } throws CancellationException("Cancelled read")
        assertFailsWith<CancellationException> { provider.rewards("unit") }
        coVerify(exactly = 1) { blobRead() }
    }

    @Test
    fun `callback delivers each resolved or unavailable result exactly once`() = runTest {
        val callbackProvider = ConfiguredAdRewardsProvider(
            manager,
            CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        )
        val results = mutableListOf<ConfiguredAdRewards?>()
        var payload: String? = "{}"
        coEvery { blobRead() } answers {
            payload?.toByteArray()
        }
        callbackProvider.getRewards("unit") { results.add(it) }
        payload = null
        callbackProvider.getRewards("unit") { results.add(it) }
        payload = "not JSON"
        callbackProvider.getRewards("unit") { results.add(it) }
        assertThat(results).containsExactly(ConfiguredAdRewards(emptyList()), null, null)
        callbackProvider.close()
    }

    @Test
    fun `closing the provider does not close the shared config manager`() {
        provider.close()
        verify(exactly = 0) { manager.close() }
    }

    private suspend fun MockKMatcherScope.blobRead(): ByteArray? =
        manager.blobData(RemoteConfigTopic.AdRewards, "default", any<(ByteArray) -> ByteArray?>())

    private fun returnBlob(json: String) {
        coEvery { blobRead() } answers {
            json.toByteArray()
        }
    }
}
