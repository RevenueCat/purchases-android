package com.revenuecat.purchases.flatbuffers

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlatBuffersPayloadParserTest {

    private val config = byteArrayOf(0x01, 0x02, 0x03, 0x04)
    private val blobs = listOf(
        byteArrayOf(0x61) to byteArrayOf(10, 20, 30),
        byteArrayOf(0x6B, 0x65, 0x79) to byteArrayOf(-1, -2, 127, -128),
    )

    private fun bodyWithPayload(base64: String): JSONObject =
        JSONObject().apply { put("payload_fb", base64) }

    @Test
    fun `parses a base64 FlatBuffers payload embedded in a JSON body`() {
        val body = bodyWithPayload(encodePayloadBase64(config, blobs))

        val result = FlatBuffersPayloadParser.parse(body)

        assertThat(result).isNotNull
        assertThat(result!!.config).isEqualTo(config)
        assertThat(result.blobs).hasSize(2)

        assertThat(result.blobs[0].key).isEqualTo(byteArrayOf(0x61))
        assertThat(result.blobs[0].value).isEqualTo(byteArrayOf(10, 20, 30))
        assertThat(result.blobs[1].key).isEqualTo(byteArrayOf(0x6B, 0x65, 0x79))
        assertThat(result.blobs[1].value).isEqualTo(byteArrayOf(-1, -2, 127, -128))
    }

    @Test
    fun `parses an empty config and no blobs`() {
        val result = FlatBuffersPayloadParser.parse(bodyWithPayload(encodePayloadBase64(ByteArray(0), emptyList())))

        assertThat(result).isNotNull
        assertThat(result!!.config).isEmpty()
        assertThat(result.blobs).isEmpty()
    }

    @Test
    fun `returns null when the payload field is absent`() {
        val result = FlatBuffersPayloadParser.parse(JSONObject().apply { put("other", 1) })

        assertThat(result).isNull()
    }

    @Test
    fun `returns null without throwing when the payload is malformed`() {
        val result = FlatBuffersPayloadParser.parse(bodyWithPayload("not-valid-base64-flatbuffer!!"))

        assertThat(result).isNull()
    }
}
