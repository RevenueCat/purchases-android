package com.revenuecat.purchases.common.audiences

import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import io.mockk.MockKMatcherScope
import io.mockk.coEvery
import io.mockk.mockk
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
}
