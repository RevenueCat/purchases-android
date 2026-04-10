package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.networking.HTTPResult
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.json.JSONException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@OptIn(InternalRevenueCatAPI::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class WorkflowDetailHttpProcessorTest {

    private val fetcher = WorkflowCdnFetcher { url ->
        when (url) {
            "https://cdn.example/w.json" -> """{"id":"from_cdn"}"""
            else -> error("unexpected url $url")
        }
    }

    private val processor = WorkflowDetailHttpProcessor(fetcher)

    private fun httpResult(responseCode: Int, payload: String) = HTTPResult(
        responseCode = responseCode,
        payload = payload,
        origin = HTTPResult.Origin.BACKEND,
        requestDate = null,
        verificationResult = VerificationResult.NOT_REQUESTED,
        isLoadShedderResponse = false,
        isFallbackURL = false,
    )

    @Test
    fun `returns original result when not successful`() {
        val raw = httpResult(500, "{}")
        val out = processor.process(raw)
        assertThat(out.httpResult).isSameAs(raw)
        assertThat(out.enrolledVariants).isNull()
    }

    @Test
    fun `inline unwraps data and parses enrolled_variants`() {
        val raw = httpResult(
            200,
            """{"action":"inline","data":{"id":"wf1"},"enrolled_variants":{"a":"b"}}""",
        )
        val out = processor.process(raw)
        assertThat(out.httpResult.responseCode).isEqualTo(200)
        assertThat(out.httpResult.payload).isEqualTo("""{"id":"wf1"}""")
        assertThat(out.enrolledVariants).containsExactlyEntriesOf(mapOf("a" to "b"))
    }

    @Test
    fun `use_cdn fetches payload and preserves enrolled_variants`() {
        val raw = httpResult(
            200,
            """{"action":"use_cdn","url":"https://cdn.example/w.json","enrolled_variants":{"x":"y"}}""",
        )
        val out = processor.process(raw)
        assertThat(out.httpResult.payload).isEqualTo("""{"id":"from_cdn"}""")
        assertThat(out.enrolledVariants).containsExactlyEntriesOf(mapOf("x" to "y"))
    }

    @Test
    fun `unknown action throws`() {
        val raw = httpResult(200, """{"action":"other"}""")
        assertThatThrownBy { processor.process(raw) }
            .isInstanceOf(JSONException::class.java)
            .hasMessageContaining("other")
    }

    @Test
    fun `use_cdn propagates IOException from fetcher`() {
        val failing = WorkflowDetailHttpProcessor(
            WorkflowCdnFetcher { throw IOException("network") },
        )
        val raw = httpResult(200, """{"action":"use_cdn","url":"https://x"}""")
        assertThatThrownBy { failing.process(raw) }
            .isInstanceOf(IOException::class.java)
    }
}
