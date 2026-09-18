package com.revenuecat.purchases.common.audiences

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
import kotlinx.coroutines.test.runTest
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
        currentLogHandler = object : LogHandler {
            override fun v(tag: String, msg: String) {}
            override fun d(tag: String, msg: String) {}
            override fun i(tag: String, msg: String) {}
            override fun w(tag: String, msg: String) {}
            override fun e(tag: String, msg: String, throwable: Throwable?) {}
        }
        every { manager.configGeneration } returns 0
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
    fun `the default blob decodes typed audiences and ignores unknown fields`() = runTest {
        returnDefaultBlob(
            """
            {
              "aud_123": {
                "id": "aud_123",
                "created_via": "dashboard",
                "rules": { "and": [{ "var": "country" }, true] }
              },
              "aud_456": {
                "id": "aud_456",
                "rules": { "==": [1, 1] }
              }
            }
            """.trimIndent(),
        )

        assertThat(provider.getAudiences()).isEqualTo(
            mapOf(
                "aud_123" to Audience(id = "aud_123", rules = """{"and":[{"var":"country"},true]}"""),
                "aud_456" to Audience(id = "aud_456", rules = """{"==":[1,1]}"""),
            ),
        )
    }

    @Test
    fun `a missing default blob makes the audiences unavailable`() = runTest {
        returnNoDefaultBlob()

        assertThat(provider.getAudiences()).isNull()
    }

    @Test
    fun `a blob that is not a JSON object makes the audiences unavailable`() = runTest {
        returnDefaultBlob("""[{"id":"aud_123"}]""")

        assertThat(provider.getAudiences()).isNull()
    }

    @Test
    fun `a malformed blob makes the audiences unavailable`() = runTest {
        returnDefaultBlob("{not json")

        assertThat(provider.getAudiences()).isNull()
    }

    @Test
    fun `a malformed audience is dropped without dropping the others`() = runTest {
        returnDefaultBlob(
            """
            {
              "missing-id": { "rules": { "==": [1, 1] } },
              "array-rules": { "id": "array-rules", "rules": [1, 2, 3] },
              "valid": { "id": "valid", "rules": { "==": [1, 1] } }
            }
            """.trimIndent(),
        )

        assertThat(provider.getAudiences()).isEqualTo(
            mapOf("valid" to Audience(id = "valid", rules = """{"==":[1,1]}""")),
        )
    }

    @Test
    fun `getAudiences reads again when the config generation changes during the read`() = runTest {
        every { manager.configGeneration } returnsMany listOf(0, 1)
        returnDefaultBlob("""{"aud_123":{"id":"aud_123","rules":{"==":[1,1]}}}""")

        assertThat(provider.getAudiences()).isEqualTo(
            mapOf("aud_123" to Audience(id = "aud_123", rules = """{"==":[1,1]}""")),
        )
        coVerify(exactly = 2) {
            manager.blobData(RemoteConfigTopic.Audiences, "default", any<(ByteArray) -> Map<String, Audience>?>())
        }
    }

    @Test
    fun `getAudiences returns null when the config changes during both reads`() = runTest {
        every { manager.configGeneration } returnsMany listOf(0, 1, 1, 2)
        returnDefaultBlob("""{"aud_123":{"id":"aud_123","rules":{"==":[1,1]}}}""")

        assertThat(provider.getAudiences()).isNull()
        coVerify(exactly = 2) {
            manager.blobData(RemoteConfigTopic.Audiences, "default", any<(ByteArray) -> Map<String, Audience>?>())
        }
    }

    @Test
    fun `warm is a no-op and never reads the blob when the topic is not committed`() = runTest {
        coEvery { manager.committedTopicOrNull(RemoteConfigTopic.Audiences) } returns null

        provider.warm(generation = 0)

        coVerify(exactly = 0) { blobRead() }
    }

    @Test
    fun `getAudiences serves the warmed audiences from memory without re-reading the blob`() = runTest {
        commitTopic()
        returnDefaultBlob("""{"aud_123":{"id":"aud_123","rules":{"==":[1,1]}}}""")

        provider.warm(generation = 0)
        val first = provider.getAudiences()
        val second = provider.getAudiences()

        assertThat(first).isEqualTo(mapOf("aud_123" to Audience(id = "aud_123", rules = """{"==":[1,1]}""")))
        assertThat(second).isSameAs(first)
        coVerify(exactly = 1) { blobRead() }
    }

    @Test
    fun `warm leaves the cache cold when the blob is unavailable so getAudiences falls through`() = runTest {
        commitTopic()
        returnNoDefaultBlob()
        provider.warm(generation = 0)
        returnDefaultBlob("""{"aud_123":{"id":"aud_123","rules":{"==":[1,1]}}}""")

        assertThat(provider.getAudiences()).containsOnlyKeys("aud_123")
        coVerify(exactly = 2) { blobRead() }
    }

    @Test
    fun `audiences warmed at an older generation are not served once the config generation advances`() = runTest {
        commitTopic()
        returnDefaultBlob("""{"aud_123":{"id":"aud_123","rules":{"==":[1,1]}}}""")
        provider.warm(generation = 0)
        every { manager.configGeneration } returns 1

        provider.getAudiences()

        coVerify(exactly = 2) { blobRead() }
    }

    @Test
    fun `onConfigInvalidated drops the warmed audiences`() = runTest {
        commitTopic()
        returnDefaultBlob("""{"aud_123":{"id":"aud_123","rules":{"==":[1,1]}}}""")
        provider.warm(generation = 0)

        provider.onConfigInvalidated(generation = 1)
        every { manager.configGeneration } returns 1
        provider.getAudiences()

        coVerify(exactly = 2) { blobRead() }
    }

    @Test
    fun `a lower-generation warm neither re-reads nor clobbers a higher-generation value`() = runTest {
        commitTopic()
        returnDefaultBlob("""{"newer":{"id":"newer","rules":{"==":[1,1]}}}""")
        every { manager.configGeneration } returns 5
        provider.warm(generation = 5)
        returnDefaultBlob("""{"older":{"id":"older","rules":{"==":[1,1]}}}""")

        provider.warm(generation = 2)

        assertThat(provider.getAudiences()).containsOnlyKeys("newer")
        coVerify(exactly = 1) { blobRead() }
    }

    private fun commitTopic() {
        coEvery { manager.committedTopicOrNull(RemoteConfigTopic.Audiences) } returns ConfigTopic(
            mapOf("default" to RemoteConfiguration.ConfigItem(blobRef = "blob_default")),
        )
    }

    private suspend fun MockKMatcherScope.blobRead(): Map<String, Audience>? =
        manager.blobData(RemoteConfigTopic.Audiences, "default", any<(ByteArray) -> Map<String, Audience>?>())

    private fun returnDefaultBlob(json: String) {
        coEvery { blobRead() } answers {
            thirdArg<(ByteArray) -> Map<String, Audience>?>()(json.toByteArray())
        }
    }

    private fun returnNoDefaultBlob() {
        coEvery { blobRead() } returns null
    }
}
