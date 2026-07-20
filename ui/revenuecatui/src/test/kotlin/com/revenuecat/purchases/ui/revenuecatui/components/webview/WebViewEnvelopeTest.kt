package com.revenuecat.purchases.ui.revenuecatui.components.webview

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class WebViewEnvelopeTest {

    private val componentId = "promo_web_view"

    @Test
    fun `parses a valid message envelope`() {
        val parsed = WebViewEnvelope.parse(
            envelope(
                kind = WebViewEnvelope.KIND_MESSAGE,
                type = WebViewMessageType.RESIZE,
            ),
        )

        assertThat(parsed).isNotNull
        assertThat(parsed!!.kind).isEqualTo(WebViewEnvelope.KIND_MESSAGE)
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
                kind = WebViewEnvelope.KIND_REQUEST,
                type = WebViewMessageType.RESIZE,
                id = "req-1",
                payload = """{"responses":{"selected_plan":"annual"}}""",
            ),
        )

        assertThat(parsed).isNotNull
        assertThat(parsed!!.id).isEqualTo("req-1")
        assertThat(parsed.payload).isNotNull
        assertThat(parsed.payload!!.getJSONObject("responses").getString("selected_plan"))
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
        assertThat(parsed!!.kind).isEqualTo(WebViewEnvelope.KIND_CONNECT)
        assertThat(parsed.componentId).isEmpty()
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
        val kinds = listOf(
            WebViewEnvelope.KIND_CONNECT,
            WebViewEnvelope.KIND_INIT,
            WebViewEnvelope.KIND_REJECT,
            WebViewEnvelope.KIND_MESSAGE,
            WebViewEnvelope.KIND_REQUEST,
            WebViewEnvelope.KIND_RESPONSE,
            WebViewEnvelope.KIND_ERROR,
        )

        kinds.forEach { kind ->
            val parsed = WebViewEnvelope.parse(
                """
                {"channel":"rc-web-components","protocol_version":1,"kind":"$kind","component_id":"$componentId"}
                """.trimIndent(),
            )
            assertThat(parsed).describedAs("kind=$kind").isNotNull
            assertThat(parsed!!.kind).isEqualTo(kind)
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
    fun `build emits required and optional fields`() {
        val payload = JSONObject("""{"responses":{"ok":true}}""")
        val json = WebViewEnvelope.build(
            kind = WebViewEnvelope.KIND_RESPONSE,
            protocolVersion = WebViewEnvelope.DEFAULT_PROTOCOL_VERSION,
            componentId = componentId,
            type = WebViewMessageType.RESIZE,
            id = "req-1",
            payload = payload,
            error = "boom",
        )

        assertThat(json.getString(WebViewMessageField.CHANNEL)).isEqualTo(WebViewEnvelope.CHANNEL)
        assertThat(json.getInt(WebViewMessageField.PROTOCOL_VERSION))
            .isEqualTo(WebViewEnvelope.DEFAULT_PROTOCOL_VERSION)
        assertThat(json.getString(WebViewMessageField.KIND)).isEqualTo(WebViewEnvelope.KIND_RESPONSE)
        assertThat(json.getString(WebViewMessageField.COMPONENT_ID)).isEqualTo(componentId)
        assertThat(json.getString(WebViewMessageField.TYPE)).isEqualTo(WebViewMessageType.RESIZE)
        assertThat(json.getString(WebViewMessageField.ID)).isEqualTo("req-1")
        assertThat(json.getJSONObject(WebViewMessageField.PAYLOAD).toString())
            .isEqualTo(payload.toString())
        assertThat(json.getString(WebViewMessageField.ERROR)).isEqualTo("boom")
    }

    @Test
    fun `build omits null optional fields`() {
        val json = WebViewEnvelope.build(
            kind = WebViewEnvelope.KIND_INIT,
            protocolVersion = WebViewEnvelope.DEFAULT_PROTOCOL_VERSION,
            componentId = componentId,
        )

        assertThat(json.has(WebViewMessageField.TYPE)).isFalse()
        assertThat(json.has(WebViewMessageField.ID)).isFalse()
        assertThat(json.has(WebViewMessageField.PAYLOAD)).isFalse()
        assertThat(json.has(WebViewMessageField.ERROR)).isFalse()
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
