package com.revenuecat.purchases.common.sdksettings

import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.common.currentLogHandler
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

internal class SdkSettingsConfigProviderTest {

    private val manager = mockk<RemoteConfigManager>()
    private val provider = SdkSettingsConfigProvider(manager)
    private val notified = mutableListOf<SdkSettings>()
    private val originalLogHandler = currentLogHandler

    private val diagnosticsOn = SdkSettings(diagnostics = DiagnosticsSettings(enabled = true))
    private val diagnosticsOff = SdkSettings(diagnostics = DiagnosticsSettings(enabled = false))

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
        coEvery { manager.committedTopicOrNull(RemoteConfigTopic.SdkSettings) } returns null
        coEvery { manager.topic(RemoteConfigTopic.SdkSettings) } returns null
        provider.listener = SdkSettingsListener { notified.add(it) }
    }

    @After
    fun tearDown() {
        currentLogHandler = originalLogHandler
    }

    @Test
    fun `sdk settings use the backend sdk_settings topic`() {
        assertThat(RemoteConfigTopic.SdkSettings.wireName).isEqualTo("sdk_settings")
    }

    @Test
    fun `cachedSettings is null before any warm`() {
        assertThat(provider.cachedSettings()).isNull()
    }

    @Test
    fun `warm is a no-op when no config is committed`() = runTest {
        coEvery { manager.hasCommittedConfig() } returns false

        provider.warm(generation = 0)

        assertThat(provider.cachedSettings()).isNull()
        assertThat(notified).isEmpty()
        coVerify(exactly = 0) { manager.committedTopicOrNull(any()) }
    }

    @Test
    fun `a committed config without the topic warms the default settings`() = runTest {
        provider.warm(generation = 0)

        assertThat(provider.cachedSettings()).isEqualTo(SdkSettings.DEFAULT)
        assertThat(notified).containsExactly(SdkSettings.DEFAULT)
    }

    @Test
    fun `a commit that drops the topic notifies the default settings`() = runTest {
        commitTopic("""{"diagnostics":{"enabled":true}}""")
        provider.warm(generation = 0)
        coEvery { manager.committedTopicOrNull(RemoteConfigTopic.SdkSettings) } returns null

        provider.warm(generation = 1)

        assertThat(notified).containsExactly(diagnosticsOn, SdkSettings.DEFAULT)
        every { manager.configGeneration } returns 1
        assertThat(provider.cachedSettings()).isEqualTo(SdkSettings.DEFAULT)
    }

    @Test
    fun `onConfigCommitted warms and notifies the listener`() = runTest {
        val provider = SdkSettingsConfigProvider(manager, scope = this)
        provider.listener = SdkSettingsListener { notified.add(it) }
        commitTopic("""{"diagnostics":{"enabled":true}}""")

        provider.onConfigCommitted(generation = 0)
        advanceUntilIdle()

        assertThat(provider.cachedSettings()).isEqualTo(diagnosticsOn)
        assertThat(notified).containsExactly(diagnosticsOn)
    }

    @Test
    fun `warm decodes the default item's inline metadata`() = runTest {
        commitTopic("""{"diagnostics":{"enabled":true}}""")

        provider.warm(generation = 0)

        assertThat(provider.cachedSettings()).isEqualTo(diagnosticsOn)
        assertThat(notified).containsExactly(diagnosticsOn)
    }

    @Test
    fun `a committed topic without a default item warms the default settings`() = runTest {
        coEvery { manager.committedTopicOrNull(RemoteConfigTopic.SdkSettings) } returns ConfigTopic(emptyMap())

        provider.warm(generation = 0)

        assertThat(provider.cachedSettings()).isEqualTo(SdkSettings.DEFAULT)
        assertThat(notified).containsExactly(SdkSettings.DEFAULT)
    }

    @Test
    fun `a re-warm with the same settings does not notify again`() = runTest {
        commitTopic("""{"diagnostics":{"enabled":true}}""")
        provider.warm(generation = 0)

        provider.warm(generation = 1)

        assertThat(notified).containsExactly(diagnosticsOn)
    }

    @Test
    fun `a re-warm after an invalidation with the same settings does not notify again`() = runTest {
        commitTopic("""{"diagnostics":{"enabled":true}}""")
        provider.warm(generation = 0)
        provider.onConfigInvalidated(generation = 1)

        provider.warm(generation = 2)

        assertThat(notified).containsExactly(diagnosticsOn)
        every { manager.configGeneration } returns 2
        assertThat(provider.cachedSettings()).isEqualTo(diagnosticsOn)
    }

    @Test
    fun `a listener attached after the first warm is notified on the next warm`() = runTest {
        provider.listener = null
        commitTopic("""{"diagnostics":{"enabled":true}}""")
        provider.warm(generation = 0)
        provider.listener = SdkSettingsListener { notified.add(it) }

        provider.warm(generation = 1)

        assertThat(notified).containsExactly(diagnosticsOn)
    }

    @Test
    fun `a re-warm with different settings notifies the listener`() = runTest {
        commitTopic("""{"diagnostics":{"enabled":true}}""")
        provider.warm(generation = 0)
        commitTopic("""{"diagnostics":{"enabled":false}}""")

        provider.warm(generation = 1)

        assertThat(notified).containsExactly(diagnosticsOn, diagnosticsOff)
        every { manager.configGeneration } returns 1
        assertThat(provider.cachedSettings()).isEqualTo(diagnosticsOff)
    }

    @Test
    fun `settings warmed at an older generation are not served once the generation advances`() = runTest {
        commitTopic("""{"diagnostics":{"enabled":true}}""")
        provider.warm(generation = 0)

        every { manager.configGeneration } returns 1

        assertThat(provider.cachedSettings()).isNull()
    }

    @Test
    fun `onConfigInvalidated drops the warmed settings`() = runTest {
        commitTopic("""{"diagnostics":{"enabled":true}}""")
        provider.warm(generation = 0)

        provider.onConfigInvalidated(generation = 1)

        assertThat(provider.cachedSettings()).isNull()
    }

    @Test
    fun `a lower-generation warm neither notifies nor clobbers a higher-generation value`() = runTest {
        commitTopic("""{"diagnostics":{"enabled":true}}""")
        every { manager.configGeneration } returns 5
        provider.warm(generation = 5)
        commitTopic("""{"diagnostics":{"enabled":false}}""")

        provider.warm(generation = 2)

        assertThat(provider.cachedSettings()).isEqualTo(diagnosticsOn)
        assertThat(notified).containsExactly(diagnosticsOn)
    }

    @Test
    fun `getSettings serves the warmed settings without reading the topic`() = runTest {
        commitTopic("""{"diagnostics":{"enabled":true}}""")
        provider.warm(generation = 0)

        assertThat(provider.getSettings()).isEqualTo(diagnosticsOn)
        coVerify(exactly = 0) { manager.topic(RemoteConfigTopic.SdkSettings) }
    }

    @Test
    fun `getSettings reads the topic through the config layer when cold`() = runTest {
        coEvery { manager.topic(RemoteConfigTopic.SdkSettings) } returns topic("""{"diagnostics":{"enabled":false}}""")

        assertThat(provider.getSettings()).isEqualTo(diagnosticsOff)
        coVerify(exactly = 1) { manager.topic(RemoteConfigTopic.SdkSettings) }
    }

    @Test
    fun `getSettings returns the default settings when the topic is absent`() = runTest {
        assertThat(provider.getSettings()).isEqualTo(SdkSettings.DEFAULT)
    }

    @Test
    fun `getSettings reads again when the config generation changes during the read`() = runTest {
        every { manager.configGeneration } returnsMany listOf(0, 0, 1)
        coEvery { manager.topic(RemoteConfigTopic.SdkSettings) } returns topic("""{"diagnostics":{"enabled":true}}""")

        assertThat(provider.getSettings()).isEqualTo(diagnosticsOn)
        coVerify(exactly = 2) { manager.topic(RemoteConfigTopic.SdkSettings) }
    }

    @Test
    fun `getSettings returns the default settings when the config changes during both reads`() = runTest {
        every { manager.configGeneration } returnsMany listOf(0, 0, 1, 1, 2)
        coEvery { manager.topic(RemoteConfigTopic.SdkSettings) } returns topic("""{"diagnostics":{"enabled":true}}""")

        assertThat(provider.getSettings()).isEqualTo(SdkSettings.DEFAULT)
        coVerify(exactly = 2) { manager.topic(RemoteConfigTopic.SdkSettings) }
    }

    private fun commitTopic(defaultItemJson: String) {
        coEvery { manager.committedTopicOrNull(RemoteConfigTopic.SdkSettings) } returns topic(defaultItemJson)
    }

    private fun topic(defaultItemJson: String) = ConfigTopic(
        mapOf(
            "default" to RemoteConfiguration.ConfigItem(
                metadata = Json.parseToJsonElement(defaultItemJson).jsonObject,
            ),
        ),
    )
}
