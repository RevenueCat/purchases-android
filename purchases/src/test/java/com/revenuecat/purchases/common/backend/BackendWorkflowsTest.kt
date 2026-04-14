package com.revenuecat.purchases.common.backend

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.HTTPClient
import com.revenuecat.purchases.common.SyncDispatcher
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.common.workflows.WorkflowResponseAction
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Fail.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.URL

@OptIn(InternalRevenueCatAPI::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BackendWorkflowsTest {

    private val mockClient: HTTPClient = mockk(relaxed = true)
    private val mockBaseURL = URL("http://mock-api-test.revenuecat.com/")
    private val apiKey = "TEST_API_KEY"
    private val defaultAuthHeaders = mapOf("Authorization" to "Bearer $apiKey")
    private val mockAppConfig: AppConfig = mockk<AppConfig>().apply {
        every { baseURL } returns mockBaseURL
        every { customEntitlementComputation } returns false
        every { fallbackBaseURLs } returns emptyList()
    }
    private val dispatcher = SyncDispatcher()
    private val backendHelper = BackendHelper(apiKey, dispatcher, mockAppConfig, mockClient)
    private val backend = Backend(
        mockAppConfig,
        dispatcher,
        dispatcher,
        mockClient,
        backendHelper,
    )
    private val appUserId = "user_1"

    private val minimalUiConfigJson = """
        "ui_config": {
          "app": { "colors": {}, "fonts": {} },
          "localizations": {},
          "variable_config": {}
        }
    """.trimIndent()

    @Test
    fun `getWorkflows calls performRequest with GetWorkflows endpoint`() {
        val listJson = """
            {
              "workflows": []
            }
        """.trimIndent()
        every {
            mockClient.performRequest(
                baseURL = mockBaseURL,
                endpoint = Endpoint.GetWorkflows(appUserId),
                body = null,
                postFieldsToSign = null,
                requestHeaders = defaultAuthHeaders,
                fallbackBaseURLs = emptyList(),
            )
        } returns httpResult(RCHTTPStatusCodes.SUCCESS, listJson)

        var success = false
        backend.getWorkflows(
            appUserID = appUserId,
            appInBackground = false,
            onSuccess = {
                assertThat(it.workflows).isEmpty()
                success = true
            },
            onError = { fail("unexpected error $it") },
        )
        assertThat(success).isTrue()
        verify(exactly = 1) {
            mockClient.performRequest(
                baseURL = mockBaseURL,
                endpoint = Endpoint.GetWorkflows(appUserId),
                body = null,
                postFieldsToSign = null,
                requestHeaders = defaultAuthHeaders,
                fallbackBaseURLs = emptyList(),
            )
        }
    }

    @Test
    fun `getWorkflow returns deserialized inline envelope`() {
        val workflowJson = """
            {
              "id": "wf_1",
              "display_name": "W",
              "initial_step_id": "step_1",
              "steps": {
                "step_1": {
                  "id": "step_1",
                  "type": "screen",
                  "param_values": {},
                  "triggers": [],
                  "outputs": {},
                  "trigger_actions": {},
                  "metadata": null
                }
              },
              "screens": {},
              $minimalUiConfigJson
            }
        """.trimIndent()
        val envelope = """
            {
              "action": "inline",
              "data": $workflowJson
            }
        """.trimIndent()
        every {
            mockClient.performRequest(
                baseURL = mockBaseURL,
                endpoint = Endpoint.GetWorkflow(appUserId, "wf_1"),
                body = null,
                postFieldsToSign = null,
                requestHeaders = defaultAuthHeaders,
                fallbackBaseURLs = emptyList(),
            )
        } returns httpResult(RCHTTPStatusCodes.SUCCESS, envelope)

        var success = false
        backend.getWorkflow(
            appUserID = appUserId,
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { response ->
                assertThat(response.action).isEqualTo(WorkflowResponseAction.INLINE)
                assertThat(response.data).isNotNull
                assertThat(response.data!!.id).isEqualTo("wf_1")
                assertThat(response.data!!.initialStepId).isEqualTo("step_1")
                assertThat(response.enrolledVariants).isNull()
                success = true
            },
            onError = { fail("unexpected error $it") },
        )
        assertThat(success).isTrue()
    }

    @Test
    fun `getWorkflow returns deserialized use_cdn envelope`() {
        val envelope = """
            {
              "action": "use_cdn",
              "url": "https://cdn.example.com/wf.json",
              "hash": "abc123",
              "enrolled_variants": {"exp_1": "variant_a"}
            }
        """.trimIndent()
        every {
            mockClient.performRequest(
                baseURL = mockBaseURL,
                endpoint = Endpoint.GetWorkflow(appUserId, "wf_cdn"),
                body = null,
                postFieldsToSign = null,
                requestHeaders = defaultAuthHeaders,
                fallbackBaseURLs = emptyList(),
            )
        } returns httpResult(RCHTTPStatusCodes.SUCCESS, envelope)

        var success = false
        backend.getWorkflow(
            appUserID = appUserId,
            workflowId = "wf_cdn",
            appInBackground = false,
            onSuccess = { response ->
                assertThat(response.action).isEqualTo(WorkflowResponseAction.USE_CDN)
                assertThat(response.url).isEqualTo("https://cdn.example.com/wf.json")
                assertThat(response.hash).isEqualTo("abc123")
                assertThat(response.enrolledVariants).isEqualTo(mapOf("exp_1" to "variant_a"))
                assertThat(response.data).isNull()
                success = true
            },
            onError = { fail("unexpected error $it") },
        )
        assertThat(success).isTrue()
    }

    @Test
    fun `getWorkflow propagates HTTP errors`() {
        every {
            mockClient.performRequest(
                baseURL = mockBaseURL,
                endpoint = Endpoint.GetWorkflow(appUserId, "wf_missing"),
                body = null,
                postFieldsToSign = null,
                requestHeaders = defaultAuthHeaders,
                fallbackBaseURLs = emptyList(),
            )
        } returns httpResult(RCHTTPStatusCodes.NOT_FOUND, """{"error":"not found"}""")

        var errorCode: PurchasesErrorCode? = null
        backend.getWorkflow(
            appUserID = appUserId,
            workflowId = "wf_missing",
            appInBackground = false,
            onSuccess = { fail("expected error") },
            onError = { errorCode = it.code },
        )
        assertThat(errorCode).isNotNull
    }

    private fun httpResult(responseCode: Int, payload: String) = HTTPResult(
        responseCode = responseCode,
        payload = payload,
        origin = HTTPResult.Origin.BACKEND,
        requestDate = null,
        verificationResult = VerificationResult.NOT_REQUESTED,
        isLoadShedderResponse = false,
        isFallbackURL = false,
    )
}
