package com.revenuecat.purchases.common.audiences

import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.remoteconfig.ConfigTopic
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import com.revenuecat.purchases.common.remoteconfig.RemoteConfiguration
import io.mockk.coEvery
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
    fun `getAudience decodes a typed audience and ignores unknown fields`() = runTest {
        returnMetadata(
            "aud_123" to """
            {
              "id": "aud_123",
              "created_via": "dashboard",
              "rules": { "and": [{ "var": "country" }, true] }
            }
            """.trimIndent(),
        )

        assertThat(provider.getAudience("aud_123")).isEqualTo(
            Audience(
                id = "aud_123",
                rules = """{"and":[{"var":"country"},true]}""",
            ),
        )
    }

    @Test
    fun `getAudience returns null for missing or invalid metadata`() = runTest {
        returnMetadata(
            "missing-id" to """{"rules":{"==":[1,1]}}""",
            "array-rules" to """{"id":"array-rules","rules":[1,2,3]}""",
        )

        assertThat(provider.getAudience("missing-id")).isNull()
        assertThat(provider.getAudience("array-rules")).isNull()
        assertThat(provider.getAudience("unknown")).isNull()
    }

    @Test
    fun `a malformed audience does not prevent reading another audience`() = runTest {
        returnMetadata(
            "invalid" to """{"id":"invalid","rules":[]}""",
            "valid" to """{"id":"valid","rules":{"==":[1,1]}}""",
        )

        assertThat(provider.getAudience("invalid")).isNull()
        assertThat(provider.getAudience("valid")).isEqualTo(
            Audience(id = "valid", rules = """{"==":[1,1]}"""),
        )
    }

    private fun returnMetadata(vararg audiences: Pair<String, String>) {
        val items = audiences.associate { (identifier, json) ->
            identifier to RemoteConfiguration.ConfigItem(
                metadata = JsonTools.json.parseToJsonElement(json).jsonObject,
            )
        }
        coEvery { manager.topic(RemoteConfigTopic.Audiences) } returns ConfigTopic(items)
    }
}
