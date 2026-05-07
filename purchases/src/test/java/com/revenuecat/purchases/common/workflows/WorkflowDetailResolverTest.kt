package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.models.Checksum
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import kotlin.test.assertFailsWith

@OptIn(InternalRevenueCatAPI::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class WorkflowDetailResolverTest {

    private val minimalUiConfigJson = """
        "ui_config": {
          "app": { "colors": {}, "fonts": {} },
          "localizations": {},
          "variable_config": {}
        }
    """.trimIndent()

    private val minimalWorkflowJson = """
        {
          "id": "wf_1",
          "display_name": "Test",
          "initial_step_id": "step_1",
          "steps": {
            "step_1": {
              "id": "step_1",
              "type": "screen"
            }
          },
          "screens": {},
          $minimalUiConfigJson
        }
    """.trimIndent()

    private fun createResolver(
        fetchResult: suspend (String, Checksum?) -> String = { _, _ -> error("unexpected CDN fetch") },
    ) = WorkflowDetailResolver(
        WorkflowCdnFetcher { url, checksum -> fetchResult(url, checksum) },
    )

    private fun inlineWorkflow(): PublishedWorkflow =
        WorkflowJsonParser.parsePublishedWorkflow(minimalWorkflowJson)

    @Test
    fun `inline extracts workflow from data`() = runTest {
        val resolver = createResolver()
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.INLINE,
            data = inlineWorkflow(),
        )
        val result = resolver.resolve(response)
        assertThat(result.workflow.id).isEqualTo("wf_1")
        assertThat(result.enrolledVariants).isNull()
    }

    @Test
    fun `inline with enrolled_variants passes them through`() = runTest {
        val resolver = createResolver()
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.INLINE,
            data = inlineWorkflow(),
            enrolledVariants = mapOf("exp_1" to "variant_a"),
        )
        val result = resolver.resolve(response)
        assertThat(result.enrolledVariants).isEqualTo(mapOf("exp_1" to "variant_a"))
    }

    @Test
    fun `inline throws when data is missing`() = runTest {
        val resolver = createResolver()
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.INLINE,
            data = null,
        )
        val ex = assertFailsWith<IllegalStateException> { resolver.resolve(response) }
        assertThat(ex.message).contains("missing data")
    }

    @Test
    fun `use_cdn fetches and parses workflow`() = runTest {
        val resolver = createResolver { url, _ ->
            assertThat(url).isEqualTo("https://cdn.example.com/wf.json")
            minimalWorkflowJson
        }
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn.example.com/wf.json",
        )
        val result = resolver.resolve(response)
        assertThat(result.workflow.id).isEqualTo("wf_1")
    }

    @Test
    fun `use_cdn with enrolled_variants passes them through`() = runTest {
        val resolver = createResolver { _, _ -> minimalWorkflowJson }
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn.example.com/wf.json",
            enrolledVariants = mapOf("x" to "y"),
        )
        val result = resolver.resolve(response)
        assertThat(result.enrolledVariants).isEqualTo(mapOf("x" to "y"))
    }

    @Test
    fun `use_cdn throws when url is missing`() = runTest {
        val resolver = createResolver()
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = null,
        )
        val ex = assertFailsWith<IllegalStateException> { resolver.resolve(response) }
        assertThat(ex.message).contains("missing url")
    }

    @Test
    fun `use_cdn propagates IOException from fetcher`() = runTest {
        val resolver = createResolver { _, _ -> throw IOException("network error") }
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn.example.com/wf.json",
        )
        assertFailsWith<IOException> { resolver.resolve(response) }
    }

    @Test
    fun `use_cdn passes sha256 checksum to fetcher when hash is present`() = runTest {
        var capturedChecksum: Checksum? = null
        val resolver = createResolver { _, checksum ->
            capturedChecksum = checksum
            minimalWorkflowJson
        }
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn.example.com/wf.json",
            hash = "abc123",
        )
        resolver.resolve(response)
        assertThat(capturedChecksum).isEqualTo(Checksum(Checksum.Algorithm.SHA256, "abc123"))
    }

    @Test
    fun `use_cdn passes null checksum to fetcher when hash is absent`() = runTest {
        var capturedChecksum: Checksum? = Checksum(Checksum.Algorithm.SHA256, "sentinel")
        val resolver = createResolver { _, checksum ->
            capturedChecksum = checksum
            minimalWorkflowJson
        }
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn.example.com/wf.json",
            hash = null,
        )
        resolver.resolve(response)
        assertThat(capturedChecksum).isNull()
    }

    @Test
    fun `single_step_fallback_id is parsed when present`() {
        val json = minimalWorkflowJson.trimEnd('}') + """, "single_step_fallback_id": "step_1" }"""
        val workflow = WorkflowJsonParser.parsePublishedWorkflow(json)
        assertThat(workflow.singleStepFallbackId).isEqualTo("step_1")
    }

    @Test
    fun `single_step_fallback_id defaults to null when absent`() {
        val workflow = WorkflowJsonParser.parsePublishedWorkflow(minimalWorkflowJson)
        assertThat(workflow.singleStepFallbackId).isNull()
    }
}
