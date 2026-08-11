package com.revenuecat.purchases.common.audiences

import com.revenuecat.purchases.JsonTools
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class AudiencesConfigProviderTest {

    private val manager = mockk<RemoteConfigManager>()
    private val provider = AudiencesConfigProvider(manager)
    private val originalLogHandler = currentLogHandler

    @Before
    fun setUp() {
        every { manager.configGeneration } returns 0
        currentLogHandler = object : LogHandler {
            override fun v(tag: String, msg: String) {}
            override fun d(tag: String, msg: String) {}
            override fun i(tag: String, msg: String) {}
            override fun w(tag: String, msg: String) {}
            override fun e(tag: String, msg: String, throwable: Throwable?) {}
        }
    }

    @After
    fun tearDown() {
        currentLogHandler = originalLogHandler
    }

    @Test
    fun `audiences use the backend audiences topic`() {
        assertThat(RemoteConfigTopic.Audiences.wireName).isEqualTo("audiences")
    }

    @Test
    fun `getAudience decodes and preserves the complete opaque payload`() = runTest {
        returnBlob(
            "aud_123",
            """
            {
              "id": "aud_123",
              "created_via": "dashboard",
              "rules": { "and": [{ "var": "country" }, true] }
            }
            """.trimIndent(),
        )

        val audience = provider.getAudience("aud_123")

        assertThat(audience).isEqualTo(
            JsonTools.json.parseToJsonElement(
                """{"id":"aud_123","created_via":"dashboard","rules":{"and":[{"var":"country"},true]}}""",
            ).jsonObject,
        )
    }

    @Test
    fun `getAudience returns null for malformed or non-object payloads`() = runTest {
        returnBlob("malformed", "not-json")
        returnBlob("array", "[1, 2, 3]")

        assertThat(provider.getAudience("malformed")).isNull()
        assertThat(provider.getAudience("array")).isNull()
    }

    @Test
    fun `warm caches valid prefetched audiences independently and skips non-prefetched audiences`() = runTest {
        // Catches removing the prefetch filter or replacing per-item reads with an all-or-nothing merge.
        coEvery { manager.committedTopicOrNull(RemoteConfigTopic.Audiences) } returns audienceTopic(
            "aud_valid" to configItem(prefetch = true),
            "aud_missing" to configItem(prefetch = true),
            "aud_malformed" to configItem(prefetch = true),
            "aud_array" to configItem(prefetch = true),
            "aud_not_prefetched" to configItem(prefetch = false),
        )
        val validAudience = audience("""{"id":"aud_valid","rules":{"country":"ES"}}""")
        coEvery { blobRead("aud_valid") } returns validAudience
        coEvery { blobRead("aud_missing") } returns null
        returnBlob("aud_malformed", "not-json")
        returnBlob("aud_array", "[1, 2, 3]")

        provider.warm(generation = 0)

        assertThat(provider.getAudience("aud_valid")).isEqualTo(validAudience)
        assertThat(provider.getAudience("aud_array")).isNull()
        coVerify(exactly = 1) { blobRead("aud_valid") }
        coVerify(exactly = 1) { blobRead("aud_missing") }
        coVerify(exactly = 1) { blobRead("aud_malformed") }
        coVerify(exactly = 2) { blobRead("aud_array") }
        coVerify(exactly = 0) { blobRead("aud_not_prefetched") }
    }

    @Test
    fun `warm stores an empty snapshot when the audiences topic is absent`() = runTest {
        // Catches returning before storing the empty topic, which would repeat the committed-topic disk read.
        coEvery { manager.committedTopicOrNull(RemoteConfigTopic.Audiences) } returns null

        provider.warm(generation = 0)
        provider.warm(generation = 0)

        coVerify(exactly = 1) { manager.committedTopicOrNull(RemoteConfigTopic.Audiences) }
    }

    @Test
    fun `onConfigInvalidated forces the next audience lookup through the manager`() = runTest {
        // Catches a missing invalidation callback, which would keep serving the previous user's cached object.
        coEvery { manager.committedTopicOrNull(RemoteConfigTopic.Audiences) } returns audienceTopic(
            "aud_valid" to configItem(prefetch = true),
        )
        val originalAudience = audience("""{"id":"aud_valid","version":1}""")
        val updatedAudience = audience("""{"id":"aud_valid","version":2}""")
        coEvery { blobRead("aud_valid") } returns originalAudience andThen updatedAudience
        provider.warm(generation = 0)
        assertThat(provider.getAudience("aud_valid")).isEqualTo(originalAudience)

        provider.onConfigInvalidated(generation = 1)
        every { manager.configGeneration } returns 1

        assertThat(provider.getAudience("aud_valid")).isEqualTo(updatedAudience)
        coVerify(exactly = 2) { blobRead("aud_valid") }
    }

    @Test
    fun `a generation-2 warm completing last cannot replace generation-5 data`() = runTest {
        // Catches an unguarded cache write that lets slow stale IO overwrite a newer committed snapshot.
        coEvery { manager.committedTopicOrNull(RemoteConfigTopic.Audiences) } returns audienceTopic(
            "aud_valid" to configItem(prefetch = true),
        )
        val generation2Started = CompletableDeferred<Unit>()
        val releaseGeneration2 = CompletableDeferred<Unit>()
        val generation2Audience = audience("""{"id":"aud_valid","generation":2}""")
        val generation5Audience = audience("""{"id":"aud_valid","generation":5}""")
        var readCount = 0
        coEvery { blobRead("aud_valid") } coAnswers {
            when (++readCount) {
                1 -> {
                    generation2Started.complete(Unit)
                    releaseGeneration2.await()
                    generation2Audience
                }
                else -> generation5Audience
            }
        }

        val generation2Warm = async { provider.warm(generation = 2) }
        generation2Started.await()
        provider.warm(generation = 5)
        releaseGeneration2.complete(Unit)
        generation2Warm.await()

        assertThat(provider.getAudience("aud_valid")).isEqualTo(generation5Audience)
    }

    @Test
    fun `getAudience does not serve an object resolved before a concurrent newer invalidation`() = runTest {
        // Catches returning a direct blob result without checking whether its snapshotted generation is stale.
        every { manager.configGeneration } returns 0
        val staleAudience = audience("""{"id":"aud_stale"}""")
        coEvery { blobRead("aud_stale") } answers {
            provider.onConfigInvalidated(generation = 1)
            staleAudience
        }

        assertThat(provider.getAudience("aud_stale")).isNull()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `getAudience does not serve a stale body while a newer commit warm is pending`() = runTest {
        // Catches checking only the cache generation after a direct read while a newer commit warm is suspended.
        var managerGeneration = 0
        every { manager.configGeneration } answers { managerGeneration }
        coEvery { manager.committedTopicOrNull(RemoteConfigTopic.Audiences) } returns audienceTopic(
            "aud_changed" to configItem(prefetch = true),
        )
        val oldLookupStarted = CompletableDeferred<Unit>()
        val releaseOldLookup = CompletableDeferred<Unit>()
        val commitWarmStarted = CompletableDeferred<Unit>()
        val releaseCommitWarm = CompletableDeferred<Unit>()
        val staleAudience = audience("""{"id":"aud_changed","generation":0}""")
        val currentAudience = audience("""{"id":"aud_changed","generation":1}""")
        var readCount = 0
        coEvery { blobRead("aud_changed") } coAnswers {
            when (++readCount) {
                1 -> {
                    oldLookupStarted.complete(Unit)
                    releaseOldLookup.await()
                    staleAudience
                }
                2 -> staleAudience
                3 -> {
                    commitWarmStarted.complete(Unit)
                    releaseCommitWarm.await()
                    currentAudience
                }
                else -> error("Unexpected audience read")
            }
        }
        val commitScopedProvider = AudiencesConfigProvider(manager, this)
        val lookup = async { commitScopedProvider.getAudience("aud_changed") }
        oldLookupStarted.await()
        commitScopedProvider.warm(generation = 0)

        managerGeneration = 1
        commitScopedProvider.onConfigCommitted(generation = 1)
        commitWarmStarted.await()
        releaseOldLookup.complete(Unit)

        assertThat(lookup.await()).isNull()

        releaseCommitWarm.complete(Unit)
        runCurrent()
        assertThat(commitScopedProvider.getAudience("aud_changed")).isEqualTo(currentAudience)
    }

    private suspend fun MockKMatcherScope.blobRead(identifier: String): JsonObject? =
        manager.blobData(
            RemoteConfigTopic.Audiences,
            identifier,
            any<(ByteArray) -> JsonObject?>(),
        )

    private fun returnBlob(identifier: String, json: String) {
        coEvery { blobRead(identifier) } answers {
            thirdArg<(ByteArray) -> JsonObject?>().invoke(json.toByteArray())
        }
    }

    private fun audience(json: String): JsonObject = JsonTools.json.parseToJsonElement(json).jsonObject

    private fun audienceTopic(
        vararg items: Pair<String, RemoteConfiguration.ConfigItem>,
    ): ConfigTopic = ConfigTopic(linkedMapOf(*items))

    private fun configItem(prefetch: Boolean): RemoteConfiguration.ConfigItem =
        RemoteConfiguration.ConfigItem(blobRef = "blob", prefetch = prefetch)
}
