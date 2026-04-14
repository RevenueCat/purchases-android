package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

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
        signatureVerificationMode: SignatureVerificationMode = SignatureVerificationMode.Disabled,
        fetchResult: (String) -> String = { error("unexpected CDN fetch") },
    ) = WorkflowDetailResolver(
        WorkflowCdnFetcher { url -> fetchResult(url) },
        signatureVerificationMode,
    )

    private fun inlineWorkflow(): PublishedWorkflow =
        WorkflowJsonParser.parsePublishedWorkflow(minimalWorkflowJson)

    @Test
    fun `inline extracts workflow from data`() {
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
    fun `inline with enrolled_variants passes them through`() {
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
    fun `inline throws when data is missing`() {
        val resolver = createResolver()
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.INLINE,
            data = null,
        )
        assertThatThrownBy { resolver.resolve(response) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("missing data")
    }

    @Test
    fun `use_cdn fetches and parses workflow`() {
        val resolver = createResolver { url ->
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
    fun `use_cdn with enrolled_variants passes them through`() {
        val resolver = createResolver { minimalWorkflowJson }
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn.example.com/wf.json",
            enrolledVariants = mapOf("x" to "y"),
        )
        val result = resolver.resolve(response)
        assertThat(result.enrolledVariants).isEqualTo(mapOf("x" to "y"))
    }

    @Test
    fun `use_cdn throws when url is missing`() {
        val resolver = createResolver()
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = null,
        )
        assertThatThrownBy { resolver.resolve(response) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("missing url")
    }

    @Test
    fun `use_cdn propagates IOException from fetcher`() {
        val resolver = createResolver { throw IOException("network error") }
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn.example.com/wf.json",
        )
        assertThatThrownBy { resolver.resolve(response) }
            .isInstanceOf(IOException::class.java)
    }

    @Test
    fun `use_cdn verifies hash when verification enabled`() {
        val json = minimalWorkflowJson
        val expectedHash = WorkflowDetailResolver.computeCanonicalHash(json)
        val resolver = createResolver(
            signatureVerificationMode = SignatureVerificationMode.Informational(),
        ) { json }
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn.example.com/wf.json",
            hash = expectedHash,
        )
        // Should not throw — hash matches
        val result = resolver.resolve(response)
        assertThat(result.workflow.id).isEqualTo("wf_1")
    }

    @Test
    fun `use_cdn skips hash verification when disabled`() {
        val resolver = createResolver(
            signatureVerificationMode = SignatureVerificationMode.Disabled,
        ) { minimalWorkflowJson }
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn.example.com/wf.json",
            hash = "wrong_hash",
        )
        // Should not log error — verification is disabled
        val result = resolver.resolve(response)
        assertThat(result.workflow.id).isEqualTo("wf_1")
    }

    @Test
    fun `computeCanonicalHash produces stable SHA-256 hex digest`() {
        val json = """{"b":2,"a":1}"""
        val hash = WorkflowDetailResolver.computeCanonicalHash(json)
        assertThat(hash).hasSize(64)
        // Same input → same output
        assertThat(WorkflowDetailResolver.computeCanonicalHash(json)).isEqualTo(hash)
    }

    @Test
    fun `computeCanonicalHash excludes hash field`() {
        val withoutHash = """{"id":"wf_1","steps":{}}"""
        val withHash = """{"id":"wf_1","steps":{},"hash":"abc123"}"""
        assertThat(WorkflowDetailResolver.computeCanonicalHash(withHash))
            .isEqualTo(WorkflowDetailResolver.computeCanonicalHash(withoutHash))
    }
}
