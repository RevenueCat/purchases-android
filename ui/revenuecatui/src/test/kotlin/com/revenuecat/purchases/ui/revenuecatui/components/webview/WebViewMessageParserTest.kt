package com.revenuecat.purchases.ui.revenuecatui.components.webview

import com.revenuecat.purchases.ui.revenuecatui.PaywallWebViewValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class WebViewMessageParserTest {

    private val componentId = "promo_web_view"

    @Test
    fun `parses rc step-loaded`() {
        val parsed = parse(
            envelope(
                kind = WebViewEnvelope.KIND_MESSAGE,
                type = WebViewMessageType.STEP_LOADED,
            ),
        )

        assertThat(parsed).isNotNull
        val message = parsed!!.message
        assertThat(message.type).isEqualTo("rc:step-loaded")
        assertThat(message.componentId).isEqualTo("promo_web_view")
        assertThat(message.responses).isNull()
        assertThat(message.error).isNull()
    }

    @Test
    fun `parses rc step-complete with responses in payload`() {
        val parsed = parse(
            envelope(
                kind = WebViewEnvelope.KIND_MESSAGE,
                type = WebViewMessageType.STEP_COMPLETE,
                payload = """{"responses":{"selected_plan":"annual","accepted_terms":true,"count":3}}""",
            ),
        )

        assertThat(parsed).isNotNull
        val responses = parsed!!.message.responses
        assertThat(responses).isNotNull
        assertThat(responses!!["selected_plan"]).isEqualTo(PaywallWebViewValue.String("annual"))
        assertThat(responses["accepted_terms"]).isEqualTo(PaywallWebViewValue.Boolean(true))
        assertThat(responses["count"]).isEqualTo(PaywallWebViewValue.Number(3))
    }

    @Test
    fun `parses rc step-complete without responses as empty map`() {
        val parsed = parse(
            envelope(
                kind = WebViewEnvelope.KIND_MESSAGE,
                type = WebViewMessageType.STEP_COMPLETE,
            ),
        )

        assertThat(parsed).isNotNull
        assertThat(parsed!!.message.responses).isEmpty()
    }

    @Test
    fun `parses rc request-variables as message`() {
        val parsed = parse(
            envelope(
                kind = WebViewEnvelope.KIND_MESSAGE,
                type = WebViewMessageType.REQUEST_VARIABLES,
            ),
        )

        assertThat(parsed).isNotNull
        assertThat(parsed!!.message.type).isEqualTo("rc:request-variables")
        assertThat(parsed.requestId).isNull()
    }

    @Test
    fun `parses rc request-variables as transport request`() {
        val parsed = parse(
            envelope(
                kind = WebViewEnvelope.KIND_REQUEST,
                type = WebViewMessageType.REQUEST_VARIABLES,
                id = "req-1",
            ),
        )

        assertThat(parsed).isNotNull
        assertThat(parsed!!.message.type).isEqualTo("rc:request-variables")
        assertThat(parsed.requestId).isEqualTo("req-1")
    }

    @Test
    fun `parses rc error from payload`() {
        val parsed = parse(
            envelope(
                kind = WebViewEnvelope.KIND_MESSAGE,
                type = WebViewMessageType.ERROR,
                payload = """{"error":"Boom"}""",
            ),
        )

        assertThat(parsed).isNotNull
        assertThat(parsed!!.message.error).isEqualTo("Boom")
    }

    @Test
    fun `rejects message without type`() {
        assertThat(
            parse(
                """{"channel":"rc-web-components","protocol_version":1,"kind":"message","component_id":"promo_web_view"}""",
            ),
        ).isNull()
    }

    @Test
    fun `rejects message with non-string type`() {
        assertThat(
            parse(
                """
                {"channel":"rc-web-components","protocol_version":1,"kind":"message","component_id":"promo_web_view","type":123}
                """.trimIndent(),
            ),
        ).isNull()
    }

    @Test
    fun `rejects message without component_id`() {
        assertThat(
            parse(
                """
                {"channel":"rc-web-components","protocol_version":1,"kind":"message","type":"rc:step-loaded"}
                """.trimIndent(),
            ),
        ).isNull()
    }

    @Test
    fun `rejects message with wrong component_id`() {
        assertThat(
            envelope(
                kind = WebViewEnvelope.KIND_MESSAGE,
                type = WebViewMessageType.STEP_LOADED,
                componentId = "other_web_view",
            ).let(::parse),
        ).isNull()
    }

    @Test
    fun `rejects rc step-complete with non-object responses`() {
        assertThat(
            parse(
                envelope(
                    kind = WebViewEnvelope.KIND_MESSAGE,
                    type = WebViewMessageType.STEP_COMPLETE,
                    payload = """{"responses":"nope"}""",
                ),
            ),
        ).isNull()
    }

    @Test
    fun `rejects rc step-complete payload with transport fields and no responses object`() {
        assertThat(
            parse(
                envelope(
                    kind = WebViewEnvelope.KIND_MESSAGE,
                    type = WebViewMessageType.STEP_COMPLETE,
                    payload = """{"type":"rc:step-complete","selected_plan":"annual"}""",
                ),
            ),
        ).isNull()
    }

    @Test
    fun `rejects rc error without error string`() {
        assertThat(
            parse(
                envelope(
                    kind = WebViewEnvelope.KIND_MESSAGE,
                    type = WebViewMessageType.ERROR,
                ),
            ),
        ).isNull()
    }

    @Test
    fun `drops unknown message types`() {
        assertThat(
            parse(
                envelope(
                    kind = WebViewEnvelope.KIND_MESSAGE,
                    type = "rc:something-new",
                ),
            ),
        ).isNull()
    }

    @Test
    fun `rejects malformed json`() {
        assertThat(parse("""not json""")).isNull()
    }

    @Test
    fun `rejects non-object json`() {
        assertThat(parse("""["a","b"]""")).isNull()
    }

    @Test
    fun `rejects oversized payload`() {
        val hugeValue = "x".repeat(WebViewMessageParser.MAX_PAYLOAD_BYTES + 1)
        val raw = envelope(
            kind = WebViewEnvelope.KIND_MESSAGE,
            type = WebViewMessageType.ERROR,
            payload = """{"error":"$hugeValue"}""",
        )
        assertThat(parse(raw)).isNull()
    }

    @Test
    fun `rejects excessively nested responses`() {
        val depth = WebViewMessageParser.MAX_NESTING_DEPTH + 2
        val opening = "{\"a\":".repeat(depth)
        val closing = "}".repeat(depth)
        val nested = opening + "1" + closing
        val raw = envelope(
            kind = WebViewEnvelope.KIND_MESSAGE,
            type = WebViewMessageType.STEP_COMPLETE,
            payload = """{"responses":$nested}""",
        )
        assertThat(parse(raw)).isNull()
    }

    @Test
    fun `accepts null and nested json-compatible values in responses`() {
        val parsed = parse(
            envelope(
                kind = WebViewEnvelope.KIND_MESSAGE,
                type = WebViewMessageType.STEP_COMPLETE,
                payload = """{"responses":{"maybe":null,"list":[1,"two",false],"obj":{"k":"v"}}}""",
            ),
        )

        assertThat(parsed).isNotNull
        val responses = parsed!!.message.responses!!
        assertThat(responses["maybe"]).isEqualTo(PaywallWebViewValue.Null)
        assertThat(responses["list"]).isInstanceOf(PaywallWebViewValue.Array::class.java)
        assertThat(responses["obj"]).isInstanceOf(PaywallWebViewValue.Object::class.java)
    }

    @Test
    fun `accepts frame depth 17 before recursive parsing`() {
        val depth = WebViewMessageParser.MAX_NESTING_DEPTH + 1

        assertThat(WebViewEnvelope.exceedsMaxDepth(nestedArray(depth))).isFalse()
    }

    @Test
    fun `rejects frame depth 18 before recursive parsing`() {
        val depth = WebViewMessageParser.MAX_NESTING_DEPTH + 2

        assertThat(WebViewEnvelope.exceedsMaxDepth(nestedArray(depth))).isTrue()
    }

    @Test
    fun `rejects a hostile deeply nested frame before recursive parsing`() {
        // ~30k nesting levels fit inside the 64 KiB frame limit; the pre-parse structural scan
        // must reject this before org.json's recursive tokenizer ever runs (stack-overflow guard).
        val depth = 30_000
        val nested = "[".repeat(depth) + "]".repeat(depth)

        val parsed = parse(
            envelope(
                kind = WebViewEnvelope.KIND_MESSAGE,
                type = WebViewMessageType.STEP_COMPLETE,
                payload = """{"responses":{"deep":$nested}}""",
            ),
        )

        assertThat(parsed).isNull()
    }

    @Test
    fun `ignores handshake frames`() {
        assertThat(
            parse(
                """
                {"channel":"rc-web-components","protocol_version":1,"kind":"connect","component_id":""}
                """.trimIndent(),
            ),
        ).isNull()
    }

    private fun parse(raw: String) = WebViewMessageParser.parse(raw, expectedComponentId = componentId)

    private fun nestedArray(depth: Int): String = "[".repeat(depth) + "0" + "]".repeat(depth)

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
