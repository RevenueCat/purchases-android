package com.revenuecat.purchases.common.backend

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.Delay
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.GetWorkflowsErrorHandlingBehavior
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
import java.io.IOException
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

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
    private val defaultTimeout = 2000L
    private val asyncDispatcher = Dispatcher(
        ThreadPoolExecutor(1, 2, 0, TimeUnit.MILLISECONDS, LinkedBlockingQueue()),
    )
    private val asyncBackendHelper = BackendHelper(apiKey, asyncDispatcher, mockAppConfig, mockClient)
    private val asyncBackend = Backend(
        mockAppConfig,
        asyncDispatcher,
        asyncDispatcher,
        mockClient,
        asyncBackendHelper,
    )

    private val minimalUiConfigJson = """
        "ui_config": {
          "app": { "colors": {}, "fonts": {} },
          "localizations": {},
          "variable_config": {}
        }
    """.trimIndent()

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

    @Test
    fun `getWorkflows returns deserialized list response`() {
        val listJson = """
            {
              "workflows": [
                {"id": "wf_1", "display_name": "Flow A", "offering_id": "default", "prefetch": true},
                {"id": "wf_2", "display_name": "Flow B", "prefetch": true}
              ]
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
            onSuccess = { response ->
                assertThat(response.workflows).hasSize(2)
                assertThat(response.workflows[0].id).isEqualTo("wf_1")
                assertThat(response.workflows[0].offeringId).isEqualTo("default")
                assertThat(response.workflows[1].id).isEqualTo("wf_2")
                assertThat(response.workflows[1].offeringId).isNull()
                success = true
            },
            onError = { e, _ -> fail("unexpected error $e") },
        )
        assertThat(success).isTrue()
    }

    @Test
    fun `getWorkflows with type passes type query parameter`() {
        val listJson = """{"workflows": [{"id": "wf_1", "display_name": "Flow A", "prefetch": false}]}"""
        every {
            mockClient.performRequest(
                baseURL = mockBaseURL,
                endpoint = Endpoint.GetWorkflows(appUserId, type = "paywall"),
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
            type = "paywall",
            onSuccess = { response ->
                assertThat(response.workflows).hasSize(1)
                assertThat(response.workflows[0].id).isEqualTo("wf_1")
                success = true
            },
            onError = { e, _ -> fail("unexpected error $e") },
        )
        assertThat(success).isTrue()
    }

    @Test
    fun `getWorkflows propagates HTTP errors`() {
        every {
            mockClient.performRequest(
                baseURL = mockBaseURL,
                endpoint = Endpoint.GetWorkflows(appUserId),
                body = null,
                postFieldsToSign = null,
                requestHeaders = defaultAuthHeaders,
                fallbackBaseURLs = emptyList(),
            )
        } returns httpResult(RCHTTPStatusCodes.NOT_FOUND, """{"error": "not found"}""")

        var errorCode: PurchasesErrorCode? = null
        backend.getWorkflows(
            appUserID = appUserId,
            appInBackground = false,
            onSuccess = { fail("expected error") },
            onError = { e, _ -> errorCode = e.code },
        )
        assertThat(errorCode).isNotNull
    }

    @Test
    fun `getWorkflows deduplicates concurrent calls`() {
        val listJson = """{"workflows": []}"""
        val httpStarted = CountDownLatch(1)
        val httpProceed = CountDownLatch(1)
        every {
            mockClient.performRequest(
                baseURL = mockBaseURL,
                endpoint = Endpoint.GetWorkflows(appUserId),
                body = null,
                postFieldsToSign = null,
                requestHeaders = defaultAuthHeaders,
                fallbackBaseURLs = emptyList(),
            )
        } answers {
            httpStarted.countDown()
            httpProceed.await(defaultTimeout, TimeUnit.MILLISECONDS)
            httpResult(RCHTTPStatusCodes.SUCCESS, listJson)
        }

        val resultLatch = CountDownLatch(3)
        asyncBackend.getWorkflows(
            appUserID = appUserId,
            appInBackground = false,
            onSuccess = { resultLatch.countDown() },
            onError = { e, _ -> fail("unexpected error $e") },
        )
        assertThat(httpStarted.await(defaultTimeout, TimeUnit.MILLISECONDS)).isTrue()
        repeat(2) {
            asyncBackend.getWorkflows(
                appUserID = appUserId,
                appInBackground = false,
                onSuccess = { resultLatch.countDown() },
                onError = { e, _ -> fail("unexpected error $e") },
            )
        }
        httpProceed.countDown()
        assertThat(resultLatch.await(defaultTimeout, TimeUnit.MILLISECONDS)).isTrue()
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
    fun `getWorkflow runs on the provided callbackDispatcher when one is given`() {
        val mainDispatcher = CountingSyncDispatcher()
        val overrideDispatcher = CountingSyncDispatcher()
        val routedBackend = Backend(
            mockAppConfig,
            mainDispatcher,
            mainDispatcher,
            mockClient,
            BackendHelper(apiKey, mainDispatcher, mockAppConfig, mockClient),
        )
        every {
            mockClient.performRequest(
                baseURL = mockBaseURL,
                endpoint = Endpoint.GetWorkflow(appUserId, "wf_1"),
                body = null,
                postFieldsToSign = null,
                requestHeaders = defaultAuthHeaders,
                fallbackBaseURLs = emptyList(),
            )
        } returns httpResult(RCHTTPStatusCodes.SUCCESS, """{"action":"inline","data":null}""")

        routedBackend.getWorkflow(
            appUserID = appUserId,
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = {},
            onError = {},
            callbackDispatcher = overrideDispatcher,
        )

        assertThat(overrideDispatcher.enqueueCount).isEqualTo(1)
        assertThat(mainDispatcher.enqueueCount).isEqualTo(0)
    }

    @Test
    fun `getWorkflow runs on the default dispatcher when no callbackDispatcher is given`() {
        val mainDispatcher = CountingSyncDispatcher()
        val overrideDispatcher = CountingSyncDispatcher()
        val routedBackend = Backend(
            mockAppConfig,
            mainDispatcher,
            mainDispatcher,
            mockClient,
            BackendHelper(apiKey, mainDispatcher, mockAppConfig, mockClient),
        )
        every {
            mockClient.performRequest(
                baseURL = mockBaseURL,
                endpoint = Endpoint.GetWorkflow(appUserId, "wf_1"),
                body = null,
                postFieldsToSign = null,
                requestHeaders = defaultAuthHeaders,
                fallbackBaseURLs = emptyList(),
            )
        } returns httpResult(RCHTTPStatusCodes.SUCCESS, """{"action":"inline","data":null}""")

        routedBackend.getWorkflow(
            appUserID = appUserId,
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = {},
            onError = {},
        )

        assertThat(mainDispatcher.enqueueCount).isEqualTo(1)
        assertThat(overrideDispatcher.enqueueCount).isEqualTo(0)
    }

    private class CountingSyncDispatcher : Dispatcher(mockk()) {
        var enqueueCount = 0
        override fun enqueue(command: Runnable, delay: Delay) {
            enqueueCount++
            command.run()
        }
        override fun close() = Unit
        override fun isClosed() = false
    }

    @Test
    fun `getWorkflows reports SHOULD_NOT_FALLBACK on 4xx`() {
        every {
            mockClient.performRequest(
                baseURL = mockBaseURL,
                endpoint = Endpoint.GetWorkflows(appUserId),
                body = null,
                postFieldsToSign = null,
                requestHeaders = defaultAuthHeaders,
                fallbackBaseURLs = emptyList(),
            )
        } returns httpResult(RCHTTPStatusCodes.NOT_FOUND, """{"error": "not found"}""")

        var behavior: GetWorkflowsErrorHandlingBehavior? = null
        backend.getWorkflows(
            appUserID = appUserId,
            appInBackground = false,
            onSuccess = { fail("expected error") },
            onError = { _, b -> behavior = b },
        )
        assertThat(behavior).isEqualTo(GetWorkflowsErrorHandlingBehavior.SHOULD_NOT_FALLBACK)
    }

    @Test
    fun `getWorkflows reports SHOULD_FALLBACK on 5xx`() {
        every {
            mockClient.performRequest(
                baseURL = mockBaseURL,
                endpoint = Endpoint.GetWorkflows(appUserId),
                body = null,
                postFieldsToSign = null,
                requestHeaders = defaultAuthHeaders,
                fallbackBaseURLs = emptyList(),
            )
        } returns httpResult(RCHTTPStatusCodes.ERROR, """{"error": "boom"}""")

        var behavior: GetWorkflowsErrorHandlingBehavior? = null
        backend.getWorkflows(
            appUserID = appUserId,
            appInBackground = false,
            onSuccess = { fail("expected error") },
            onError = { _, b -> behavior = b },
        )
        assertThat(behavior).isEqualTo(GetWorkflowsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_WORKFLOWS)
    }

    @Test
    fun `getWorkflows reports SHOULD_FALLBACK on malformed body`() {
        every {
            mockClient.performRequest(
                baseURL = mockBaseURL,
                endpoint = Endpoint.GetWorkflows(appUserId),
                body = null,
                postFieldsToSign = null,
                requestHeaders = defaultAuthHeaders,
                fallbackBaseURLs = emptyList(),
            )
        } returns httpResult(RCHTTPStatusCodes.SUCCESS, "not valid json")

        var behavior: GetWorkflowsErrorHandlingBehavior? = null
        backend.getWorkflows(
            appUserID = appUserId,
            appInBackground = false,
            onSuccess = { fail("expected error") },
            onError = { _, b -> behavior = b },
        )
        assertThat(behavior).isEqualTo(GetWorkflowsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_WORKFLOWS)
    }

    @Test
    fun `getWorkflows reports SHOULD_FALLBACK on transport failure`() {
        every {
            mockClient.performRequest(
                baseURL = mockBaseURL,
                endpoint = Endpoint.GetWorkflows(appUserId),
                body = null,
                postFieldsToSign = null,
                requestHeaders = defaultAuthHeaders,
                fallbackBaseURLs = emptyList(),
            )
        } throws IOException("network down")

        var behavior: GetWorkflowsErrorHandlingBehavior? = null
        backend.getWorkflows(
            appUserID = appUserId,
            appInBackground = false,
            onSuccess = { fail("expected error") },
            onError = { _, b -> behavior = b },
        )
        assertThat(behavior).isEqualTo(GetWorkflowsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_WORKFLOWS)
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
