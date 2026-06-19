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
        val message = parse("""{"type":"rc:step-loaded","component_id":"promo_web_view"}""")

        assertThat(message).isNotNull
        assertThat(message!!.type).isEqualTo("rc:step-loaded")
        assertThat(message.componentId).isEqualTo("promo_web_view")
        assertThat(message.responses).isNull()
        assertThat(message.error).isNull()
    }

    @Test
    fun `parses rc step-complete with responses`() {
        val message = parse(
            """
            {
              "type":"rc:step-complete",
              "component_id":"promo_web_view",
              "responses":{"selected_plan":"annual","accepted_terms":true,"count":3}
            }
            """.trimIndent(),
        )

        assertThat(message).isNotNull
        val responses = message!!.responses
        assertThat(responses).isNotNull
        assertThat(responses!!["selected_plan"]).isEqualTo(PaywallWebViewValue.String("annual"))
        assertThat(responses["accepted_terms"]).isEqualTo(PaywallWebViewValue.Boolean(true))
        assertThat(responses["count"]).isEqualTo(PaywallWebViewValue.Number(3))
    }

    @Test
    fun `parses rc step-complete without responses as empty map`() {
        val message = parse("""{"type":"rc:step-complete","component_id":"promo_web_view"}""")

        assertThat(message).isNotNull
        assertThat(message!!.responses).isEmpty()
    }

    @Test
    fun `parses rc request-variables`() {
        val message = parse("""{"type":"rc:request-variables","component_id":"promo_web_view"}""")

        assertThat(message).isNotNull
        assertThat(message!!.type).isEqualTo("rc:request-variables")
    }

    @Test
    fun `parses rc error`() {
        val message = parse("""{"type":"rc:error","component_id":"promo_web_view","error":"Boom"}""")

        assertThat(message).isNotNull
        assertThat(message!!.error).isEqualTo("Boom")
    }

    @Test
    fun `rejects message without type`() {
        assertThat(parse("""{"component_id":"promo_web_view"}""")).isNull()
    }

    @Test
    fun `rejects message with non-string type`() {
        assertThat(parse("""{"type":123,"component_id":"promo_web_view"}""")).isNull()
    }

    @Test
    fun `rejects message without component_id`() {
        assertThat(parse("""{"type":"rc:step-loaded"}""")).isNull()
    }

    @Test
    fun `rejects message with wrong component_id`() {
        assertThat(parse("""{"type":"rc:step-loaded","component_id":"other_web_view"}""")).isNull()
    }

    @Test
    fun `rejects rc step-complete with non-object responses`() {
        assertThat(
            parse("""{"type":"rc:step-complete","component_id":"promo_web_view","responses":"nope"}"""),
        ).isNull()
    }

    @Test
    fun `rejects rc error without error string`() {
        assertThat(parse("""{"type":"rc:error","component_id":"promo_web_view"}""")).isNull()
    }

    @Test
    fun `drops unknown message types`() {
        assertThat(parse("""{"type":"rc:something-new","component_id":"promo_web_view"}""")).isNull()
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
        val raw = """{"type":"rc:error","component_id":"promo_web_view","error":"$hugeValue"}"""
        assertThat(parse(raw)).isNull()
    }

    @Test
    fun `rejects excessively nested responses`() {
        // Build responses nested deeper than MAX_NESTING_DEPTH.
        val depth = WebViewMessageParser.MAX_NESTING_DEPTH + 2
        val opening = "{\"a\":".repeat(depth)
        val closing = "}".repeat(depth)
        val nested = opening + "1" + closing
        val raw = """{"type":"rc:step-complete","component_id":"promo_web_view","responses":$nested}"""
        assertThat(parse(raw)).isNull()
    }

    @Test
    fun `accepts null and nested json-compatible values in responses`() {
        val message = parse(
            """
            {
              "type":"rc:step-complete",
              "component_id":"promo_web_view",
              "responses":{"maybe":null,"list":[1,"two",false],"obj":{"k":"v"}}
            }
            """.trimIndent(),
        )

        assertThat(message).isNotNull
        val responses = message!!.responses!!
        assertThat(responses["maybe"]).isEqualTo(PaywallWebViewValue.Null)
        assertThat(responses["list"]).isInstanceOf(PaywallWebViewValue.Array::class.java)
        assertThat(responses["obj"]).isInstanceOf(PaywallWebViewValue.Object::class.java)
    }

    private fun parse(raw: String) = WebViewMessageParser.parse(raw, expectedComponentId = componentId)
}
