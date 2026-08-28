package com.revenuecat.purchases.common.audiences

import com.revenuecat.purchases.JsonTools
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
import kotlinx.coroutines.test.runTest
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
        currentLogHandler = object : LogHandler {
            override fun v(tag: String, msg: String) {}
            override fun d(tag: String, msg: String) {}
            override fun i(tag: String, msg: String) {}
            override fun w(tag: String, msg: String) {}
            override fun e(tag: String, msg: String, throwable: Throwable?) {}
        }
        every { manager.configGeneration } returns 0
        returnTopicItems()
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
    fun `the snapshot decodes typed audiences and ignores unknown fields`() = runTest {
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

        assertThat(provider.getSnapshot()?.audiences).isEqualTo(
            mapOf(
                "aud_123" to Audience(id = "aud_123", rules = """{"and":[{"var":"country"},true]}"""),
                "aud_456" to Audience(id = "aud_456", rules = """{"==":[1,1]}"""),
            ),
        )
    }

    @Test
    fun `a missing default blob makes the snapshot unavailable without reading backend results`() = runTest {
        returnNoDefaultBlob()

        assertThat(provider.getSnapshot()).isNull()
        coVerify(exactly = 0) { manager.topic(RemoteConfigTopic.Audiences) }
    }

    @Test
    fun `a blob that is not a JSON object makes the snapshot unavailable`() = runTest {
        returnDefaultBlob("""[{"id":"aud_123"}]""")

        assertThat(provider.getSnapshot()).isNull()
    }

    @Test
    fun `a malformed blob makes the snapshot unavailable`() = runTest {
        returnDefaultBlob("{not json")

        assertThat(provider.getSnapshot()).isNull()
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

        assertThat(provider.getSnapshot()?.audiences).isEqualTo(
            mapOf("valid" to Audience(id = "valid", rules = """{"==":[1,1]}""")),
        )
    }

    @Test
    fun `boolean backend predicate results are read from the topic item`() = runTest {
        returnDefaultBlob("{}")
        returnTopicItems(
            "backend_predicate_results" to """
            {
              "349OzehoTyCAdiZblj9w0J0yD-Uow8X3": false,
              "PROg2cJoAVWa3sWx-6djaRxQQbDPpWwW": true
            }
            """.trimIndent(),
        )

        assertThat(provider.getSnapshot()?.backendPredicateResults).isEqualTo(
            mapOf(
                "349OzehoTyCAdiZblj9w0J0yD-Uow8X3" to false,
                "PROg2cJoAVWa3sWx-6djaRxQQbDPpWwW" to true,
            ),
        )
    }

    @Test
    fun `a non-boolean backend predicate result is dropped without dropping the others`() = runTest {
        returnDefaultBlob("{}")
        returnTopicItems(
            "backend_predicate_results" to """
            {
              "string-boolean": "true",
              "number": 1,
              "null-result": null,
              "object": { "value": true },
              "kept": true
            }
            """.trimIndent(),
        )

        assertThat(provider.getSnapshot()?.backendPredicateResults).isEqualTo(mapOf("kept" to true))
    }

    @Test
    fun `no backend predicate results item leaves the results empty rather than unavailable`() = runTest {
        returnDefaultBlob("""{"aud_123":{"id":"aud_123","rules":{"==":[1,1]}}}""")
        returnTopicItems("unrelated_item" to """{"some":"metadata"}""")

        val snapshot = provider.getSnapshot()

        assertThat(snapshot?.audiences).containsOnlyKeys("aud_123")
        assertThat(snapshot?.backendPredicateResults).isEmpty()
    }

    @Test
    fun `a topic that disappears mid-read leaves the results empty rather than unavailable`() = runTest {
        returnDefaultBlob("{}")
        coEvery { manager.topic(RemoteConfigTopic.Audiences) } returns null

        assertThat(provider.getSnapshot()?.backendPredicateResults).isEmpty()
    }

    @Test
    fun `getSnapshot reads again when the config generation changes during the read`() = runTest {
        every { manager.configGeneration } returnsMany listOf(0, 1)
        returnDefaultBlob("""{"aud_123":{"id":"aud_123","rules":{"==":[1,1]}}}""")

        assertThat(provider.getSnapshot()?.audiences).isEqualTo(
            mapOf("aud_123" to Audience(id = "aud_123", rules = """{"==":[1,1]}""")),
        )
        coVerify(exactly = 2) {
            manager.blobData(RemoteConfigTopic.Audiences, "default", any<(ByteArray) -> Map<String, Audience>?>())
        }
    }

    @Test
    fun `getSnapshot returns null when the config changes during both reads`() = runTest {
        every { manager.configGeneration } returnsMany listOf(0, 1, 1, 2)
        returnDefaultBlob("""{"aud_123":{"id":"aud_123","rules":{"==":[1,1]}}}""")

        assertThat(provider.getSnapshot()).isNull()
        coVerify(exactly = 2) {
            manager.blobData(RemoteConfigTopic.Audiences, "default", any<(ByteArray) -> Map<String, Audience>?>())
        }
    }

    private fun returnDefaultBlob(json: String) {
        coEvery {
            manager.blobData(RemoteConfigTopic.Audiences, "default", any<(ByteArray) -> Map<String, Audience>?>())
        } answers {
            thirdArg<(ByteArray) -> Map<String, Audience>?>()(json.toByteArray())
        }
    }

    private fun returnNoDefaultBlob() {
        coEvery {
            manager.blobData(RemoteConfigTopic.Audiences, "default", any<(ByteArray) -> Map<String, Audience>?>())
        } returns null
    }

    private fun returnTopicItems(vararg items: Pair<String, String>) {
        coEvery { manager.topic(RemoteConfigTopic.Audiences) } returns ConfigTopic(
            items.associate { (key, json) ->
                key to RemoteConfiguration.ConfigItem(
                    metadata = JsonTools.json.parseToJsonElement(json).jsonObject,
                )
            },
        )
    }
}
