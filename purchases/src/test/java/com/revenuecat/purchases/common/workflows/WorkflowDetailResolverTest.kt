package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.verification.SignatureVerificationException
import com.revenuecat.purchases.common.verification.SignatureVerificationMode
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
        signatureVerificationMode: SignatureVerificationMode = SignatureVerificationMode.Disabled,
        fetchResult: suspend (String) -> String = { error("unexpected CDN fetch") },
    ) = WorkflowDetailResolver(
        WorkflowCdnFetcher { url -> fetchResult(url) },
        signatureVerificationMode,
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
    fun `use_cdn with enrolled_variants passes them through`() = runTest {
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
        val resolver = createResolver { throw IOException("network error") }
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn.example.com/wf.json",
        )
        assertFailsWith<IOException> { resolver.resolve(response) }
    }

    @Test
    fun `use_cdn verifies hash when verification enabled`() = runTest {
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
        val result = resolver.resolve(response)
        assertThat(result.workflow.id).isEqualTo("wf_1")
    }

    @Test
    fun `use_cdn throws SignatureVerificationException when hash mismatches in Enforced mode`() = runTest {
        val resolver = createResolver(
            signatureVerificationMode = SignatureVerificationMode.Enforced(),
        ) { minimalWorkflowJson }
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn.example.com/wf.json",
            hash = "wrong_hash",
        )
        assertFailsWith<SignatureVerificationException> { resolver.resolve(response) }
    }

    @Test
    fun `use_cdn does not throw when hash matches in Enforced mode`() = runTest {
        val json = minimalWorkflowJson
        val expectedHash = WorkflowDetailResolver.computeCanonicalHash(json)
        val resolver = createResolver(
            signatureVerificationMode = SignatureVerificationMode.Enforced(),
        ) { json }
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn.example.com/wf.json",
            hash = expectedHash,
        )
        val result = resolver.resolve(response)
        assertThat(result.workflow.id).isEqualTo("wf_1")
    }

    @Test
    fun `use_cdn does not throw when hash mismatches in Informational mode`() = runTest {
        val resolver = createResolver(
            signatureVerificationMode = SignatureVerificationMode.Informational(),
        ) { minimalWorkflowJson }
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn.example.com/wf.json",
            hash = "wrong_hash",
        )
        val result = resolver.resolve(response)
        assertThat(result.workflow.id).isEqualTo("wf_1")
    }

    @Test
    fun `use_cdn skips hash verification when disabled`() = runTest {
        val resolver = createResolver(
            signatureVerificationMode = SignatureVerificationMode.Disabled,
        ) { minimalWorkflowJson }
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn.example.com/wf.json",
            hash = "wrong_hash",
        )
        val result = resolver.resolve(response)
        assertThat(result.workflow.id).isEqualTo("wf_1")
    }

    @Test
    fun `computeCanonicalHash produces stable SHA-256 hex digest`() {
        val json = """{"a":1,"b":2}"""
        val hash = WorkflowDetailResolver.computeCanonicalHash(json)
        assertThat(hash).hasSize(64)
        assertThat(WorkflowDetailResolver.computeCanonicalHash(json)).isEqualTo(hash)
    }
}
