package com.revenuecat.purchases.ui.revenuecatui.components.webview

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class WebViewEnvelopeTest {

    private val componentId = "promo_web_view"

    @Test
    fun `parses a valid message envelope`() {
        val parsed = WebViewEnvelope.parse(
            envelope(
                kind = "message",
                type = WebViewMessageType.RESIZE,
            ),
        )

        assertThat(parsed).isNotNull
        assertThat(parsed!!.kind).isEqualTo(WebViewEnvelope.Kind.MESSAGE)
        assertThat(parsed.protocolVersion).isEqualTo(1)
        assertThat(parsed.componentId).isEqualTo(componentId)
        assertThat(parsed.type).isEqualTo(WebViewMessageType.RESIZE)
        assertThat(parsed.id).isNull()
        assertThat(parsed.payload).isNull()
        assertThat(parsed.error).isNull()
    }

    @Test
    fun `parses optional envelope fields`() {
        val parsed = WebViewEnvelope.parse(
            envelope(
                kind = "request",
                type = WebViewMessageType.RESIZE,
                id = "req-1",
                payload = """{"responses":{"selected_plan":"annual"}}""",
            ),
        )

        assertThat(parsed).isNotNull
        assertThat(parsed!!.id).isEqualTo("req-1")
        assertThat(parsed.payload).isNotNull
        assertThat(parsed.payload!!.getValue("responses").jsonObject.getValue("selected_plan").jsonPrimitive.content)
            .isEqualTo("annual")
    }

    @Test
    fun `parses handshake connect frames`() {
        val parsed = WebViewEnvelope.parse(
            """
            {"channel":"rc-web-components","protocol_version":1,"kind":"connect","component_id":""}
            """.trimIndent(),
        )

        assertThat(parsed).isNotNull
        assertThat(parsed!!.kind).isEqualTo(WebViewEnvelope.Kind.CONNECT)
        assertThat(parsed.componentId).isEmpty()
    }

    @Test
    fun `parses explicit null optional fields as absent`() {
        val parsed = WebViewEnvelope.parse(
            """
            {"channel":"rc-web-components","protocol_version":1,"kind":"message","component_id":"promo_web_view","type":null,"id":null,"payload":null,"error":null}
            """.trimIndent(),
        )

        assertThat(parsed).isNotNull
        assertThat(parsed!!.type).isNull()
        assertThat(parsed.id).isNull()
        assertThat(parsed.payload).isNull()
        assertThat(parsed.error).isNull()
    }

    @Test
    fun `rejects wrong channel`() {
        assertThat(
            WebViewEnvelope.parse(
                """
                {"channel":"other","protocol_version":1,"kind":"message","component_id":"promo_web_view","type":"rc:step-loaded"}
                """.trimIndent(),
            ),
        ).isNull()
    }

    @Test
    fun `rejects missing channel`() {
        assertThat(
            WebViewEnvelope.parse(
                """
                {"protocol_version":1,"kind":"message","component_id":"promo_web_view","type":"rc:step-loaded"}
                """.trimIndent(),
            ),
        ).isNull()
    }

    @Test
    fun `rejects missing protocol_version`() {
        assertThat(
            WebViewEnvelope.parse(
                """
                {"channel":"rc-web-components","kind":"message","component_id":"promo_web_view","type":"rc:step-loaded"}
                """.trimIndent(),
            ),
        ).isNull()
    }

    @Test
    fun `rejects non-finite protocol_version`() {
        assertThat(
            WebViewEnvelope.parse(
                """
                {"channel":"rc-web-components","protocol_version":NaN,"kind":"message","component_id":"promo_web_view","type":"rc:step-loaded"}
                """.trimIndent(),
            ),
        ).isNull()
    }

    @Test
    fun `rejects non-integer protocol_version`() {
        assertThat(
            WebViewEnvelope.parse(
                """
                {"channel":"rc-web-components","protocol_version":1.5,"kind":"message","component_id":"promo_web_view","type":"rc:step-loaded"}
                """.trimIndent(),
            ),
        ).isNull()
    }

    @Test
    fun `rejects message without component_id`() {
        assertThat(
            WebViewEnvelope.parse(
                """
                {"channel":"rc-web-components","protocol_version":1,"kind":"message","type":"rc:step-loaded"}
                """.trimIndent(),
            ),
        ).isNull()
    }

    @Test
    fun `rejects message with non-string type`() {
        assertThat(
            WebViewEnvelope.parse(
                """
                {"channel":"rc-web-components","protocol_version":1,"kind":"message","component_id":"promo_web_view","type":123}
                """.trimIndent(),
            ),
        ).isNull()
    }

    @Test
    fun `rejects message with non-string id`() {
        assertThat(
            WebViewEnvelope.parse(
                """
                {"channel":"rc-web-components","protocol_version":1,"kind":"request","component_id":"promo_web_view","type":"rc:request-variables","id":1}
                """.trimIndent(),
            ),
        ).isNull()
    }

    @Test
    fun `rejects message with non-string error`() {
        assertThat(
            WebViewEnvelope.parse(
                """
                {"channel":"rc-web-components","protocol_version":1,"kind":"error","component_id":"promo_web_view","error":false}
                """.trimIndent(),
            ),
        ).isNull()
    }

    @Test
    fun `rejects message with non-object payload`() {
        assertThat(
            WebViewEnvelope.parse(
                """
                {"channel":"rc-web-components","protocol_version":1,"kind":"message","component_id":"promo_web_view","type":"rc:step-complete","payload":"nope"}
                """.trimIndent(),
            ),
        ).isNull()
    }

    @Test
    fun `rejects unknown kind outside whitelist`() {
        assertThat(
            WebViewEnvelope.parse(
                """
                {"channel":"rc-web-components","protocol_version":1,"kind":"ping","component_id":"promo_web_view"}
                """.trimIndent(),
            ),
        ).isNull()
    }

    @Test
    fun `accepts every whitelisted kind`() {
        val kinds = mapOf(
            "connect" to WebViewEnvelope.Kind.CONNECT,
            "init" to WebViewEnvelope.Kind.INIT,
            "reject" to WebViewEnvelope.Kind.REJECT,
            "message" to WebViewEnvelope.Kind.MESSAGE,
            "request" to WebViewEnvelope.Kind.REQUEST,
            "response" to WebViewEnvelope.Kind.RESPONSE,
            "error" to WebViewEnvelope.Kind.ERROR,
        )

        kinds.forEach { (kind, expected) ->
            val parsed = WebViewEnvelope.parse(
                """
                {"channel":"rc-web-components","protocol_version":1,"kind":"$kind","component_id":"$componentId"}
                """.trimIndent(),
            )
            assertThat(parsed).describedAs("kind=$kind").isNotNull
            assertThat(parsed!!.kind).isEqualTo(expected)
        }
    }

    @Test
    fun `rejects malformed json`() {
        assertThat(WebViewEnvelope.parse("""not json""")).isNull()
    }

    @Test
    fun `rejects non-object json`() {
        assertThat(WebViewEnvelope.parse("""["a","b"]""")).isNull()
    }

    @Test
    fun `toJsonString emits required and optional fields`() {
        val payload = Json.parseToJsonElement("""{"responses":{"ok":true}}""").jsonObject
        val encoded = WebViewEnvelope(
            kind = WebViewEnvelope.Kind.RESPONSE,
            protocolVersion = WebViewEnvelope.DEFAULT_PROTOCOL_VERSION,
            componentId = componentId,
            type = WebViewMessageType.RESIZE,
            id = "req-1",
            payload = payload,
            error = "boom",
        ).toJsonString()

        val roundTripped = WebViewEnvelope.parse(encoded)
        assertThat(roundTripped).isNotNull
        assertThat(roundTripped!!.kind).isEqualTo(WebViewEnvelope.Kind.RESPONSE)
        assertThat(roundTripped.protocolVersion).isEqualTo(WebViewEnvelope.DEFAULT_PROTOCOL_VERSION)
        assertThat(roundTripped.componentId).isEqualTo(componentId)
        assertThat(roundTripped.type).isEqualTo(WebViewMessageType.RESIZE)
        assertThat(roundTripped.id).isEqualTo("req-1")
        assertThat(roundTripped.payload).isEqualTo(payload)
        assertThat(roundTripped.error).isEqualTo("boom")
    }

    @Test
    fun `toJsonString omits null optional fields`() {
        val encoded = WebViewEnvelope(
            kind = WebViewEnvelope.Kind.INIT,
            protocolVersion = WebViewEnvelope.DEFAULT_PROTOCOL_VERSION,
            componentId = componentId,
        ).toJsonString()

        val json = Json.parseToJsonElement(encoded).jsonObject
        assertThat(json.keys).containsExactlyInAnyOrder("channel", "protocol_version", "kind", "component_id")
    }

    private fun envelope(
        kind: String,
        type: String,
        componentId: String = this.componentId,
        payload: String? = null,
        id: String? = null,
    ): String {
        val payloadField = payload?.let { ""","payload":$it""" } ?: ""
        val idField = id?.let { ""","id":"$it"""" } ?: ""
        return """
            {"channel":"rc-web-components","protocol_version":1,"kind":"$kind","component_id":"$componentId","type":"$type"$payloadField$idField}
            """.trimIndent()
    }
}
