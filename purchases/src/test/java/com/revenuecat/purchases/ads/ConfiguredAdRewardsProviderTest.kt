package com.revenuecat.purchases.ads

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.NoOpLogHandler
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
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
import kotlinx.serialization.SerializationException
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
    private val configured = ConfiguredAdRewards(AdRewardConfiguration.VirtualCurrency("coins", 10), emptyList())
    private val currencyBlob = """{"unit":{"reward":{"type":"virtual_currency","code":"coins","amount":10}}}"""

    @Before
    fun setUp() {
        currentLogHandler = NoOpLogHandler
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
    fun `empty ad unit is a configuration error without reading config`() = runTest {
        val exception = assertFailsWith<PurchasesException> { provider.rewards("") }
        assertThat(exception.code).isEqualTo(PurchasesErrorCode.ConfigurationError)
        assertThat(exception.underlyingErrorMessage).contains("ad unit ID")
        coVerify(exactly = 0) { manager.awaitTopicAndPrefetchBlobsReady(any()) }
    }

    @Test
    fun `disabled remote config reports a configuration error without fetching`() = runTest {
        every { manager.isDisabled } returns true
        val exception = assertFailsWith<PurchasesException> { provider.rewards("unit") }
        assertThat(exception.code).isEqualTo(PurchasesErrorCode.ConfigurationError)
        coVerify(exactly = 0) { manager.awaitTopicAndPrefetchBlobsReady(any()) }
    }

    @Test
    fun `absent topic in committed config means no rewards`() = runTest {
        coEvery { manager.awaitTopicAndPrefetchBlobsReady(RemoteConfigTopic.AdRewards) } returns null
        assertThat(provider.rewards("unit")).isEqualTo(ConfiguredAdRewards(null, emptyList()))
        coVerify(exactly = 0) { blobRead() }
    }

    @Test
    fun `no committed config is an unavailable error rather than empty success`() = runTest {
        coEvery { manager.awaitTopicAndPrefetchBlobsReady(RemoteConfigTopic.AdRewards) } returns null
        coEvery { manager.hasCommittedConfig() } returns false
        val exception = assertFailsWith<PurchasesException> { provider.rewards("unit") }
        assertThat(exception.code).isEqualTo(PurchasesErrorCode.UnknownError)
        assertThat(exception.underlyingErrorMessage).contains("No committed remote configuration")
    }

    @Test
    fun `missing default item or blob reference reports malformed configuration`() = runTest {
        listOf(
            ConfigTopic(emptyMap()),
            ConfigTopic(mapOf("default" to RemoteConfiguration.ConfigItem())),
        ).forEach { topic ->
            coEvery { manager.awaitTopicAndPrefetchBlobsReady(RemoteConfigTopic.AdRewards) } returns topic
            val exception = assertFailsWith<PurchasesException> { provider.rewards("unit") }
            assertThat(exception.code).isEqualTo(PurchasesErrorCode.UnknownError)
            assertThat(exception.underlyingErrorMessage).contains("default blob reference")
        }
        coVerify(exactly = 0) { blobRead() }
    }

    @Test
    fun `unresolved blob is an unavailable error without guessing its underlying cause`() = runTest {
        coEvery { blobRead() } returns null
        val exception = assertFailsWith<PurchasesException> { provider.rewards("unit") }
        assertThat(exception.code).isEqualTo(PurchasesErrorCode.UnknownError)
        assertThat(exception.underlyingErrorMessage).contains("blob could not be resolved")
    }

    @Test
    fun `provider resolves through the shared blob API without a second cache`() = runTest {
        var payload: String? = currencyBlob
        coEvery { blobRead() } answers {
            payload?.let { thirdArg<(ByteArray) -> Result<ConfiguredAdRewards>?>()(it.toByteArray()) }
        }
        assertThat(provider.rewards("unit")).isEqualTo(configured)
        payload = "{}"
        assertThat(provider.rewards("unit")).isEqualTo(ConfiguredAdRewards(null, emptyList()))
        payload = null
        assertFailsWith<PurchasesException> { provider.rewards("unit") }
        coVerify(exactly = 3) { blobRead() }
    }

    @Test
    fun `malformed JSON retains specific decoding diagnostics`() = runTest {
        returnBlob("not JSON")
        val exception = assertFailsWith<PurchasesException> { provider.rewards("unit") }
        assertThat(exception.code).isEqualTo(PurchasesErrorCode.UnknownError)
        assertThat(exception.cause).isInstanceOf(SerializationException::class.java)
        assertThat(exception.underlyingErrorMessage).isNotBlank().isEqualTo(exception.cause?.localizedMessage)
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
            thirdArg<(ByteArray) -> Result<ConfiguredAdRewards>?>()(bytes.toByteArray())
        }
        assertThat(provider.rewards("unit")).isEqualTo(ConfiguredAdRewards(null, emptyList()))
        coVerify(exactly = 2) { blobRead() }
    }

    @Test
    fun `superseded malformed read retries rather than reporting an obsolete error`() = runTest {
        var generation = 0
        every { manager.configGeneration } answers { generation }
        coEvery { blobRead() } answers {
            val bytes = if (generation == 0) "not JSON" else currencyBlob
            generation = 1
            thirdArg<(ByteArray) -> Result<ConfiguredAdRewards>?>()(bytes.toByteArray())
        }
        assertThat(provider.rewards("unit")).isEqualTo(configured)
        coVerify(exactly = 2) { blobRead() }
    }

    @Test
    fun `two superseded reads report unavailable instead of returning stale data`() = runTest {
        var generation = 0
        every { manager.configGeneration } answers { generation }
        coEvery { blobRead() } answers {
            generation++
            thirdArg<(ByteArray) -> Result<ConfiguredAdRewards>?>()(currencyBlob.toByteArray())
        }
        val exception = assertFailsWith<PurchasesException> { provider.rewards("unit") }
        assertThat(exception.code).isEqualTo(PurchasesErrorCode.UnknownError)
        assertThat(exception.underlyingErrorMessage).contains("changed while resolving")
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
            thirdArg<(ByteArray) -> Result<ConfiguredAdRewards>?>()(currencyBlob.toByteArray())
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
    fun `callback delivers each success or failure exactly once`() = runTest {
        val callbackProvider = ConfiguredAdRewardsProvider(
            manager,
            CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        )
        val successes = mutableListOf<ConfiguredAdRewards>()
        val errors = mutableListOf<PurchasesError>()
        var payload: String? = "{}"
        coEvery { blobRead() } answers {
            payload?.let { thirdArg<(ByteArray) -> Result<ConfiguredAdRewards>?>()(it.toByteArray()) }
        }
        callbackProvider.getRewards("unit", { successes.add(it) }, { errors.add(it) })
        payload = null
        callbackProvider.getRewards("unit", { successes.add(it) }, { errors.add(it) })
        payload = "not JSON"
        callbackProvider.getRewards("unit", { successes.add(it) }, { errors.add(it) })
        assertThat(successes).containsExactly(ConfiguredAdRewards(null, emptyList()))
        assertThat(errors.map { it.code }).containsExactly(
            PurchasesErrorCode.UnknownError,
            PurchasesErrorCode.UnknownError,
        )
        callbackProvider.close()
    }

    @Test
    fun `closing the provider does not close the shared config manager`() {
        provider.close()
        verify(exactly = 0) { manager.close() }
    }

    private suspend fun MockKMatcherScope.blobRead(): Result<ConfiguredAdRewards>? =
        manager.blobData(RemoteConfigTopic.AdRewards, "default", any<(ByteArray) -> Result<ConfiguredAdRewards>?>())

    private fun returnBlob(json: String) {
        coEvery { blobRead() } answers {
            thirdArg<(ByteArray) -> Result<ConfiguredAdRewards>?>()(json.toByteArray())
        }
    }
}
