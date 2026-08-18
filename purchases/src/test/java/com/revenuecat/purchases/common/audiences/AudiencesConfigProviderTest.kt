package com.revenuecat.purchases.common.audiences

import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import io.mockk.MockKMatcherScope
import io.mockk.coEvery
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

        assertThat(provider.getAudience("aud_123")).isEqualTo(
            Audience(
                id = "aud_123",
                rules = """{"and":[{"var":"country"},true]}""",
            ),
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
    fun `a malformed audience does not prevent reading another audience`() = runTest {
        returnBlob("invalid", """{"id":"invalid","rules":[]}""")
        returnBlob("valid", """{"id":"valid","rules":{"==":[1,1]}}""")

        assertThat(provider.getAudience("invalid")).isNull()
        assertThat(provider.getAudience("valid")).isEqualTo(
            Audience(id = "valid", rules = """{"==":[1,1]}"""),
        )
    }

    private suspend fun MockKMatcherScope.blobRead(identifier: String): Audience? =
        manager.blobData(
            RemoteConfigTopic.Audiences,
            identifier,
            any<(ByteArray) -> Audience?>(),
        )

    private fun returnBlob(identifier: String, json: String) {
        coEvery { blobRead(identifier) } answers {
            thirdArg<(ByteArray) -> Audience?>().invoke(json.toByteArray())
        }
    }
}
