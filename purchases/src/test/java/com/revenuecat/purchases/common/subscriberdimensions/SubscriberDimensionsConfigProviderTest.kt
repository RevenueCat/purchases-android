@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.subscriberdimensions

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.localrules.RulesDimensionValue
import com.revenuecat.purchases.common.remoteconfig.ConfigTopic
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.common.remoteconfig.RemoteConfiguration
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Date

internal class SubscriberDimensionsConfigProviderTest {

    private val manager = mockk<RemoteConfigManager>()
    private val provider = SubscriberDimensionsConfigProvider(manager)
    private val originalLogHandler = currentLogHandler

    private val spainJson = """{"dimensions":{"country":"ES"},"as_of":100}"""
    private val franceJson = """{"dimensions":{"country":"FR"},"as_of":200}"""
    private val spain = SubscriberDimensionsResolution.Found(
        SubscriberDimensions(mapOf("country" to RulesDimensionValue.StringValue("ES")), Date(100)),
    )
    private val france = SubscriberDimensionsResolution.Found(
        SubscriberDimensions(mapOf("country" to RulesDimensionValue.StringValue("FR")), Date(200)),
    )

    @Before
    fun setUp() {
        currentLogHandler = object : LogHandler {
            override fun v(tag: String, msg: String) {}
            override fun d(tag: String, msg: String) {}
            override fun i(tag: String, msg: String) {}
            override fun w(tag: String, msg: String) {}
            override fun e(tag: String, msg: String, throwable: Throwable?) {}
        }
        every { manager.configGeneration } returns 0
        coEvery { manager.hasCommittedConfig() } returns true
        coEvery { manager.committedTopicOrNull(RemoteConfigTopic.SubscriberDimensions) } returns null
        coEvery { manager.topic(RemoteConfigTopic.SubscriberDimensions) } returns null
    }

    @After
    fun tearDown() {
        currentLogHandler = originalLogHandler
    }

    @Test
    fun `subscriber dimensions use the backend subscriber_dimensions topic`() {
        assertThat(RemoteConfigTopic.SubscriberDimensions.wireName).isEqualTo("subscriber_dimensions")
    }

    @Test
    fun `cachedDimensions is null before any warm`() {
        assertThat(provider.cachedDimensions()).isNull()
    }

    @Test
    fun `warm is a no-op when no config is committed`() = runTest {
        coEvery { manager.hasCommittedConfig() } returns false

        provider.warm(generation = 0)

        assertThat(provider.cachedDimensions()).isNull()
        coVerify(exactly = 0) { manager.committedTopicOrNull(any()) }
    }

    @Test
    fun `a committed config without the topic warms not configured`() = runTest {
        provider.warm(generation = 0)

        assertThat(provider.cachedDimensions()).isEqualTo(SubscriberDimensionsResolution.NotConfigured)
    }

    @Test
    fun `a commit that drops the topic replaces the dimensions with not configured`() = runTest {
        commitTopic(spainJson)
        provider.warm(generation = 0)
        coEvery { manager.committedTopicOrNull(RemoteConfigTopic.SubscriberDimensions) } returns null
        every { manager.configGeneration } returns 1

        provider.warm(generation = 1)

        assertThat(provider.cachedDimensions()).isEqualTo(SubscriberDimensionsResolution.NotConfigured)
    }

    @Test
    fun `warm decodes the default item's inline metadata`() = runTest {
        commitTopic(spainJson)

        provider.warm(generation = 0)

        assertThat(provider.cachedDimensions()).isEqualTo(spain)
    }

    @Test
    fun `a committed topic without a default item warms unavailable`() = runTest {
        coEvery { manager.committedTopicOrNull(RemoteConfigTopic.SubscriberDimensions) } returns ConfigTopic(emptyMap())

        provider.warm(generation = 0)

        assertThat(provider.cachedDimensions()).isEqualTo(SubscriberDimensionsResolution.Unavailable)
    }

    @Test
    fun `an unusable default item warms unavailable`() = runTest {
        commitTopic("""{"dimensions":{"country":"ES"}}""")

        provider.warm(generation = 0)

        assertThat(provider.cachedDimensions()).isEqualTo(SubscriberDimensionsResolution.Unavailable)
    }

    @Test
    fun `onConfigCommitted warms on the provider's scope`() = runTest {
        val provider = SubscriberDimensionsConfigProvider(manager, scope = this)
        commitTopic(spainJson)

        provider.onConfigCommitted(generation = 0)
        advanceUntilIdle()

        assertThat(provider.cachedDimensions()).isEqualTo(spain)
    }

    @Test
    fun `warmAsync warms on the provider's scope`() = runTest {
        val provider = SubscriberDimensionsConfigProvider(manager, scope = this)
        commitTopic(spainJson)

        provider.warmAsync(generation = 0)
        advanceUntilIdle()

        assertThat(provider.cachedDimensions()).isEqualTo(spain)
    }

    @Test
    fun `a re-warm with new dimensions replaces the cached ones`() = runTest {
        commitTopic(spainJson)
        provider.warm(generation = 0)
        commitTopic(franceJson)
        every { manager.configGeneration } returns 1

        provider.warm(generation = 1)

        assertThat(provider.cachedDimensions()).isEqualTo(france)
    }

    @Test
    fun `dimensions warmed at an older generation are not served once the generation advances`() = runTest {
        commitTopic(spainJson)
        provider.warm(generation = 0)

        every { manager.configGeneration } returns 1

        assertThat(provider.cachedDimensions()).isNull()
    }

    @Test
    fun `a warm superseded during its read does not store`() = runTest {
        commitTopic(spainJson)
        every { manager.configGeneration } returns 1

        provider.warm(generation = 0)

        assertThat(provider.cachedDimensions()).isNull()
        provider.warm(generation = 1)
        assertThat(provider.cachedDimensions()).isEqualTo(spain)
    }

    @Test
    fun `onConfigInvalidated drops the warmed dimensions`() = runTest {
        commitTopic(spainJson)
        provider.warm(generation = 0)

        provider.onConfigInvalidated(generation = 1)

        assertThat(provider.cachedDimensions()).isNull()
    }

    @Test
    fun `a lower-generation warm does not clobber a higher-generation value`() = runTest {
        commitTopic(spainJson)
        every { manager.configGeneration } returns 5
        provider.warm(generation = 5)
        commitTopic(franceJson)

        provider.warm(generation = 2)

        assertThat(provider.cachedDimensions()).isEqualTo(spain)
    }

    @Test
    fun `getDimensions serves the warmed dimensions without reading the topic`() = runTest {
        commitTopic(spainJson)
        provider.warm(generation = 0)

        assertThat(provider.getDimensions()).isEqualTo(spain)
        coVerify(exactly = 0) { manager.topic(RemoteConfigTopic.SubscriberDimensions) }
    }

    @Test
    fun `getDimensions serves a warmed not configured without reading the topic`() = runTest {
        provider.warm(generation = 0)

        assertThat(provider.getDimensions()).isEqualTo(SubscriberDimensionsResolution.NotConfigured)
        coVerify(exactly = 0) { manager.topic(RemoteConfigTopic.SubscriberDimensions) }
    }

    @Test
    fun `getDimensions serves a warmed unavailable without reading the topic`() = runTest {
        commitTopic("""{"dimensions":{"country":"ES"}}""")
        provider.warm(generation = 0)

        assertThat(provider.getDimensions()).isEqualTo(SubscriberDimensionsResolution.Unavailable)
        coVerify(exactly = 0) { manager.topic(RemoteConfigTopic.SubscriberDimensions) }
    }

    @Test
    fun `getDimensions reads the topic through the config layer when cold`() = runTest {
        coEvery { manager.topic(RemoteConfigTopic.SubscriberDimensions) } returns topic(franceJson)

        assertThat(provider.getDimensions()).isEqualTo(france)
        coVerify(exactly = 1) { manager.topic(RemoteConfigTopic.SubscriberDimensions) }
    }

    @Test
    fun `getDimensions is not configured when a committed config omits the topic`() = runTest {
        assertThat(provider.getDimensions()).isEqualTo(SubscriberDimensionsResolution.NotConfigured)
    }

    @Test
    fun `getDimensions is unavailable when nothing is committed`() = runTest {
        coEvery { manager.hasCommittedConfig() } returns false

        assertThat(provider.getDimensions()).isEqualTo(SubscriberDimensionsResolution.Unavailable)
    }

    @Test
    fun `getDimensions is unavailable when the committed topic has no default item`() = runTest {
        coEvery { manager.topic(RemoteConfigTopic.SubscriberDimensions) } returns ConfigTopic(emptyMap())

        assertThat(provider.getDimensions()).isEqualTo(SubscriberDimensionsResolution.Unavailable)
    }

    @Test
    fun `getDimensions reads again when the config generation changes during the read`() = runTest {
        every { manager.configGeneration } returnsMany listOf(0, 0, 1)
        coEvery { manager.topic(RemoteConfigTopic.SubscriberDimensions) } returns topic(spainJson)

        assertThat(provider.getDimensions()).isEqualTo(spain)
        coVerify(exactly = 2) { manager.topic(RemoteConfigTopic.SubscriberDimensions) }
    }

    @Test
    fun `getDimensions is unavailable when the config changes during both reads`() = runTest {
        every { manager.configGeneration } returnsMany listOf(0, 0, 1, 1, 2)
        coEvery { manager.topic(RemoteConfigTopic.SubscriberDimensions) } returns topic(spainJson)

        assertThat(provider.getDimensions()).isEqualTo(SubscriberDimensionsResolution.Unavailable)
        coVerify(exactly = 2) { manager.topic(RemoteConfigTopic.SubscriberDimensions) }
    }

    private fun commitTopic(defaultItemJson: String) {
        coEvery { manager.committedTopicOrNull(RemoteConfigTopic.SubscriberDimensions) } returns topic(defaultItemJson)
    }

    private fun topic(defaultItemJson: String) = ConfigTopic(
        mapOf(
            "default" to RemoteConfiguration.ConfigItem(
                metadata = Json.parseToJsonElement(defaultItemJson).jsonObject,
            ),
        ),
    )
}
