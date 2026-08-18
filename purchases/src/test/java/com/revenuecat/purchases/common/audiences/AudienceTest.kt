package com.revenuecat.purchases.common.audiences

import com.revenuecat.purchases.JsonTools
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

internal class AudienceTest {

    @Test
    fun `decodes nested rules as a compact JSON string and ignores unknown fields`() {
        val audience = JsonTools.json.decodeFromString<Audience>(
            """
            {
              "id": "aud_1",
              "created_via": "dashboard",
              "rules": {
                "in": [
                  { "var": "last_seen.country" },
                  ["US", "CA"]
                ]
              }
            }
            """.trimIndent(),
        )

        assertThat(audience).isEqualTo(
            Audience(
                id = "aud_1",
                rules = """{"in":[{"var":"last_seen.country"},["US","CA"]]}""",
            ),
        )
    }

    @Test
    fun `accepts an empty rules object`() {
        val audience = JsonTools.json.decodeFromString<Audience>(
            """{"id":"aud_1","rules":{}}""",
        )

        assertThat(audience.rules).isEqualTo("{}")
    }

    @Test
    fun `rejects a missing or incorrectly typed id`() {
        assertMalformed("""{"rules":{}}""")
        assertMalformed("""{"id":1,"rules":{}}""")
    }

    @Test
    fun `rejects missing null array and primitive rules`() {
        assertMalformed("""{"id":"aud_1"}""")
        assertMalformed("""{"id":"aud_1","rules":null}""")
        assertMalformed("""{"id":"aud_1","rules":[]}""")
        assertMalformed("""{"id":"aud_1","rules":"rule"}""")
        assertMalformed("""{"id":"aud_1","rules":1}""")
        assertMalformed("""{"id":"aud_1","rules":true}""")
    }

    @Test
    fun `serializes the rules string as a JSON object and round trips`() {
        val audience = Audience(
            id = "aud_1",
            rules = """{"and":[true,{"var":"last_seen.country"}]}""",
        )

        val encoded = JsonTools.json.encodeToString(audience)
        val encodedObject = JsonTools.json.parseToJsonElement(encoded).jsonObject

        assertThat(encodedObject["rules"]).isEqualTo(
            JsonTools.json.parseToJsonElement(audience.rules),
        )
        assertThat(JsonTools.json.decodeFromString<Audience>(encoded)).isEqualTo(audience)
    }

    private fun assertMalformed(payload: String) {
        assertThatThrownBy { JsonTools.json.decodeFromString<Audience>(payload) }
            .isInstanceOf(SerializationException::class.java)
    }
}
