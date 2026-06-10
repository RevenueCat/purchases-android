package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.NoOpLogHandler
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.GetWorkflowsErrorHandlingBehavior
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.utils.WorkflowAssetPreDownloader
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.serialization.SerializationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.Date

class WorkflowManagerTest {

    private val mockBackend: Backend = mockk(relaxed = true)
    private val mockResolver: WorkflowDetailResolver = mockk()
    private val mockAssetPreDownloader: WorkflowAssetPreDownloader = mockk(relaxed = true)
    private val mockDeviceCache: DeviceCache = mockk(relaxed = true)
    private val mockDateProvider: DateProvider = mockk()
    private val mockPrefetchDispatcher: Dispatcher = mockk(relaxed = true)
    private lateinit var workflowsCache: WorkflowsCache
    private lateinit var workflowManager: WorkflowManager
    private lateinit var originalLogHandler: LogHandler

    @Before
    fun setUp() {
        originalLogHandler = currentLogHandler
        currentLogHandler = NoOpLogHandler
        every { mockDateProvider.now } returns Date(0) // ensures cache is always stale by default
        workflowsCache = spyk(WorkflowsCache(deviceCache = mockDeviceCache, dateProvider = mockDateProvider))
        workflowManager = WorkflowManager(
            backend = mockBackend,
            workflowDetailResolver = mockResolver,
            workflowAssetPreDownloader = mockAssetPreDownloader,
            workflowsCache = workflowsCache,
            prefetchDispatcher = mockPrefetchDispatcher,
            scope = CoroutineScope(UnconfinedTestDispatcher()),
        )
    }

    @After
    fun tearDown() {
        currentLogHandler = originalLogHandler
    }

    @Test
    fun `getWorkflow resolves inline response into WorkflowResult`() {
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.INLINE,
            data = mockk(),
        )
        val expectedResult = WorkflowDataResult(
            workflow = response.data!!,
            enrolledVariants = null,
        )
        coEvery { mockResolver.resolve(response) } returns expectedResult

        val successSlot = slot<(WorkflowDetailResponse) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = capture(successSlot),
                onError = any(),
            )
        } answers {
            successSlot.captured(response)
        }

        var result: WorkflowDataResult? = null
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { result = it },
            onError = { fail("unexpected error $it") },
        )
        assertThat(result).isEqualTo(expectedResult)
        verify(exactly = 1) { mockAssetPreDownloader.preDownloadWorkflowAssets(expectedResult.workflow) }
    }

    @Test
    fun `getWorkflow propagates backend errors`() {
        val expectedError = PurchasesError(PurchasesErrorCode.NetworkError, "network error")
        val errorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_missing",
                appInBackground = false,
                onSuccess = any(),
                onError = capture(errorSlot),
            )
        } answers {
            errorSlot.captured(expectedError, GetWorkflowsErrorHandlingBehavior.SHOULD_NOT_FALLBACK)
        }

        var error: PurchasesError? = null
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_missing",
            appInBackground = false,
            onSuccess = { fail("expected error") },
            onError = { error = it },
        )
        assertThat(error).isEqualTo(expectedError)
    }

    @Test
    fun `getWorkflow calls onError when resolver throws`() {
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.INLINE,
            data = null,
        )
        coEvery { mockResolver.resolve(response) } throws IllegalStateException("missing data")

        val successSlot = slot<(WorkflowDetailResponse) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = capture(successSlot),
                onError = any(),
            )
        } answers {
            successSlot.captured(response)
        }

        var error: PurchasesError? = null
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { fail("expected error") },
            onError = { error = it },
        )
        assertThat(error).isNotNull
        assertThat(error!!.code).isEqualTo(PurchasesErrorCode.UnknownError)
    }

    @Test
    fun `getWorkflow calls onError when resolver throws SerializationException`() {
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn.example.com/workflow.json",
        )
        coEvery { mockResolver.resolve(response) } throws SerializationException("malformed compiled workflow json")

        val successSlot = slot<(WorkflowDetailResponse) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = capture(successSlot),
                onError = any(),
            )
        } answers {
            successSlot.captured(response)
        }

        var error: PurchasesError? = null
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { fail("expected error") },
            onError = { error = it },
        )
        assertThat(error).isNotNull
        assertThat(error!!.code).isEqualTo(PurchasesErrorCode.UnknownError)
    }

    @Test
    fun `getWorkflow does not report cancellation as an error`() {
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn.example.com/workflow.json",
        )
        coEvery { mockResolver.resolve(response) } throws CancellationException("scope cancelled")

        val successSlot = slot<(WorkflowDetailResponse) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = capture(successSlot),
                onError = any(),
            )
        } answers {
            successSlot.captured(response)
        }

        var error: PurchasesError? = null
        var result: WorkflowDataResult? = null
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { result = it },
            onError = { error = it },
        )
        assertThat(error).isNull()
        assertThat(result).isNull()
    }

    @Test
    fun `getWorkflow still delivers result when pre-download throws`() {
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.INLINE,
            data = mockk(),
        )
        val expectedResult = WorkflowDataResult(
            workflow = response.data!!,
            enrolledVariants = null,
        )
        coEvery { mockResolver.resolve(response) } returns expectedResult
        every {
            mockAssetPreDownloader.preDownloadWorkflowAssets(any())
        } throws NullPointerException("malformed component data")

        val successSlot = slot<(WorkflowDetailResponse) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = capture(successSlot),
                onError = any(),
            )
        } answers {
            successSlot.captured(response)
        }

        var result: WorkflowDataResult? = null
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { result = it },
            onError = { fail("pre-download failure must not prevent delivering the result") },
        )
        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `getWorkflow calls onError when resolver throws IOException`() {
        val response = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn.example.com/workflow.json",
        )
        coEvery { mockResolver.resolve(response) } throws IOException("CDN fetch failed")

        val successSlot = slot<(WorkflowDetailResponse) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = capture(successSlot),
                onError = any(),
            )
        } answers {
            successSlot.captured(response)
        }

        var error: PurchasesError? = null
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { fail("expected error") },
            onError = { error = it },
        )
        assertThat(error).isNotNull
        assertThat(error!!.code).isEqualTo(PurchasesErrorCode.NetworkError)
    }

    // region detail fetch status-based fallback

    @Test
    fun `getWorkflow falls back to disk envelope on 5xx and delivers success`() {
        val envelope = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn/wf_1.json",
            hash = "h",
        )
        val expectedResult = WorkflowDataResult(workflow = mockk(), enrolledVariants = null)
        coEvery { mockResolver.resolve(envelope) } returns expectedResult

        // Disk holds one envelope for wf_1
        every { mockDeviceCache.getWorkflowDetailEnvelopesCache() } returns
            """{"wf_1":{"action":"use_cdn","url":"https://cdn/wf_1.json","hash":"h"}}"""

        val errorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = any(),
                onError = capture(errorSlot),
            )
        } answers {
            errorSlot.captured(
                PurchasesError(PurchasesErrorCode.NetworkError, "server error"),
                GetWorkflowsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_WORKFLOWS,
            )
        }

        var successResult: WorkflowDataResult? = null
        var errorCalled = false
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { successResult = it },
            onError = { errorCalled = true },
        )

        assertThat(successResult).isEqualTo(expectedResult)
        assertThat(errorCalled).isFalse()
        // Result was cached in memory after re-resolution
        assertThat(workflowsCache.cachedWorkflow("wf_1")).isEqualTo(expectedResult)
    }

    @Test
    fun `getWorkflow does not fall back to disk envelope on 4xx`() {
        val expectedError = PurchasesError(PurchasesErrorCode.UnknownBackendError, "not found")

        // Disk holds an envelope, but we must not serve it on 4xx
        every { mockDeviceCache.getWorkflowDetailEnvelopesCache() } returns
            """{"wf_1":{"action":"use_cdn","url":"https://cdn/wf_1.json","hash":"h"}}"""

        val errorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = any(),
                onError = capture(errorSlot),
            )
        } answers {
            errorSlot.captured(expectedError, GetWorkflowsErrorHandlingBehavior.SHOULD_NOT_FALLBACK)
        }

        var receivedError: PurchasesError? = null
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { fail("unexpected success") },
            onError = { receivedError = it },
        )

        assertThat(receivedError).isEqualTo(expectedError)
        coVerify(exactly = 0) { mockResolver.resolve(any()) }
        verify(exactly = 0) { mockDeviceCache.getWorkflowDetailEnvelopesCache() }
        // Evict the in-memory entry so the next call retries rather than serving a cached value,
        // mirroring the list/offerings 4xx policy.
        verify { workflowsCache.clearWorkflow("wf_1") }
    }

    @Test
    fun `getWorkflow 4xx on the background refresh evicts the workflow so it is not served again`() {
        // A workflow is cached, then the server removes it (4xx). SWR serves the stale value on the
        // render that triggers the refresh (unavoidable), but the retracted workflow must not be
        // served on any subsequent render. A 4xx therefore has to evict the cached value entirely,
        // not just clear its timestamp (which would leave it as a stale-but-present SWR hit).
        val inlineResponse = WorkflowDetailResponse(action = WorkflowResponseAction.INLINE, data = mockk())
        val staleResult = WorkflowDataResult(workflow = inlineResponse.data!!, enrolledVariants = null)
        coEvery { mockResolver.resolve(inlineResponse) } returns staleResult

        val notFound = PurchasesError(PurchasesErrorCode.UnknownBackendError, "not found")
        val successSlot = slot<(WorkflowDetailResponse) -> Unit>()
        val errorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        var call = 0
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = capture(successSlot),
                onError = capture(errorSlot),
            )
        } answers {
            call++
            if (call == 1) {
                successSlot.captured(inlineResponse)
            } else {
                errorSlot.captured(notFound, GetWorkflowsErrorHandlingBehavior.SHOULD_NOT_FALLBACK)
            }
        }

        // Populate at t=0.
        every { mockDateProvider.now } returns Date(0)
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = {},
            onError = { fail("unexpected error $it") },
        )

        // Stale render at t=6min: serves the stale value, the background refresh then 4xxes.
        every { mockDateProvider.now } returns Date(6L * 60 * 1000)
        var served: WorkflowDataResult? = null
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { served = it },
            onError = { fail("unexpected error $it") },
        )
        assertThat(served).isSameAs(staleResult)

        // The 4xx must have evicted the cached value entirely, not just cleared its timestamp.
        assertThat(workflowsCache.cachedWorkflow("wf_1")).isNull()

        // Consequence: the next render does not re-serve the retracted workflow; it surfaces the error.
        var servedAgain: WorkflowDataResult? = null
        var errorAgain: PurchasesError? = null
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { servedAgain = it },
            onError = { errorAgain = it },
        )
        assertThat(servedAgain).isNull()
        assertThat(errorAgain).isEqualTo(notFound)
    }

    @Test
    fun `getWorkflow surfaces error when fallback eligible but no envelope on disk`() {
        val expectedError = PurchasesError(PurchasesErrorCode.NetworkError, "transport error")

        // No envelope on disk
        every { mockDeviceCache.getWorkflowDetailEnvelopesCache() } returns null

        val errorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = any(),
                onError = capture(errorSlot),
            )
        } answers {
            errorSlot.captured(
                expectedError,
                GetWorkflowsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_WORKFLOWS,
            )
        }

        var receivedError: PurchasesError? = null
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { fail("unexpected success") },
            onError = { receivedError = it },
        )

        assertThat(receivedError).isEqualTo(expectedError)
    }

    @Test
    fun `getWorkflow surfaces original error when fallback resolve throws`() {
        val originalError = PurchasesError(PurchasesErrorCode.NetworkError, "server error")
        val envelope = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn/wf_1.json",
            hash = "h",
        )
        coEvery { mockResolver.resolve(envelope) } throws IllegalStateException("cdn unavailable")

        every { mockDeviceCache.getWorkflowDetailEnvelopesCache() } returns
            """{"wf_1":{"action":"use_cdn","url":"https://cdn/wf_1.json","hash":"h"}}"""

        val errorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = any(),
                onError = capture(errorSlot),
            )
        } answers {
            errorSlot.captured(
                originalError,
                GetWorkflowsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_WORKFLOWS,
            )
        }

        var receivedError: PurchasesError? = null
        var successCalled = false
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { successCalled = true },
            onError = { receivedError = it },
        )

        assertThat(receivedError).isEqualTo(originalError)
        assertThat(successCalled).isFalse()
    }

    // endregion detail fetch status-based fallback

    // region getWorkflow cache

    @Test
    fun `getWorkflow caches result on success`() {
        val response = WorkflowDetailResponse(action = WorkflowResponseAction.INLINE, data = mockk())
        val expectedResult = WorkflowDataResult(workflow = response.data!!, enrolledVariants = null)
        coEvery { mockResolver.resolve(response) } returns expectedResult

        val successSlot = slot<(WorkflowDetailResponse) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = capture(successSlot),
                onError = any(),
            )
        } answers { successSlot.captured(response) }

        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = {},
            onError = { fail("unexpected error $it") },
        )

        assertThat(workflowsCache.cachedWorkflow("wf_1")).isSameAs(expectedResult)
    }

    @Test
    fun `getWorkflow returns cached result without calling backend when cache is fresh`() {
        val response = WorkflowDetailResponse(action = WorkflowResponseAction.INLINE, data = mockk())
        val expectedResult = WorkflowDataResult(workflow = response.data!!, enrolledVariants = null)
        coEvery { mockResolver.resolve(response) } returns expectedResult

        val successSlot = slot<(WorkflowDetailResponse) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = capture(successSlot),
                onError = any(),
            )
        } answers { successSlot.captured(response) }

        // First call populates the cache (stamped at t=0).
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = {},
            onError = { fail("unexpected error $it") },
        )

        // Second call within TTL should hit the cache and skip the backend.
        var secondResult: WorkflowDataResult? = null
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { secondResult = it },
            onError = { fail("unexpected error $it") },
        )

        assertThat(secondResult).isSameAs(expectedResult)
        verify(exactly = 1) {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = any(),
                onError = any(),
            )
        }
    }

    @Test
    fun `getWorkflow refreshes in the background and still re-fetches when cache is stale`() {
        val response = WorkflowDetailResponse(action = WorkflowResponseAction.INLINE, data = mockk())
        val firstResult = WorkflowDataResult(workflow = response.data!!, enrolledVariants = null)
        val secondResult = WorkflowDataResult(workflow = response.data!!, enrolledVariants = null)

        val successSlot = slot<(WorkflowDetailResponse) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = capture(successSlot),
                onError = any(),
            )
        } answers { successSlot.captured(response) }

        // First call at t=0 populates the cache.
        every { mockDateProvider.now } returns Date(0)
        coEvery { mockResolver.resolve(response) } returns firstResult
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = {},
            onError = { fail("unexpected error $it") },
        )

        // Second call past the 5-minute foreground TTL serves the stale value and refetches.
        every { mockDateProvider.now } returns Date(6L * 60 * 1000)
        coEvery { mockResolver.resolve(response) } returns secondResult
        var served: WorkflowDataResult? = null
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { served = it },
            onError = { fail("unexpected error $it") },
        )

        // SWR: the caller gets the stale value, and the background refresh still hits the backend.
        assertThat(served).isSameAs(firstResult)
        verify(exactly = 2) {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = any(),
                onError = any(),
            )
        }
    }

    @Test
    fun `getWorkflow stale hit serves the cached value and refreshes the cache in the background`() {
        val response = WorkflowDetailResponse(action = WorkflowResponseAction.INLINE, data = mockk())
        val staleResult = WorkflowDataResult(workflow = response.data!!, enrolledVariants = null)
        val refreshedResult = WorkflowDataResult(workflow = response.data!!, enrolledVariants = null)

        val successSlot = slot<(WorkflowDetailResponse) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = capture(successSlot),
                onError = any(),
            )
        } answers { successSlot.captured(response) }

        // First call at t=0 populates the cache with the stale result.
        every { mockDateProvider.now } returns Date(0)
        coEvery { mockResolver.resolve(response) } returns staleResult
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = {},
            onError = { fail("unexpected error $it") },
        )

        // Move past the 5-minute foreground TTL and make the next resolve produce a different result.
        every { mockDateProvider.now } returns Date(6L * 60 * 1000)
        coEvery { mockResolver.resolve(response) } returns refreshedResult

        var served: WorkflowDataResult? = null
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { served = it },
            onError = { fail("unexpected error $it") },
        )

        // Caller receives the stale cached value, not the background-refreshed one.
        assertThat(served).isSameAs(staleResult)
        // The background refresh ran and updated the cache to the fresh value.
        assertThat(workflowsCache.cachedWorkflow("wf_1")).isSameAs(refreshedResult)
        verify(exactly = 2) {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = any(),
                onError = any(),
            )
        }
    }

    @Test
    fun `getWorkflow stale hit does not surface a failing background refresh to the caller`() {
        val response = WorkflowDetailResponse(action = WorkflowResponseAction.INLINE, data = mockk())
        val staleResult = WorkflowDataResult(workflow = response.data!!, enrolledVariants = null)
        coEvery { mockResolver.resolve(response) } returns staleResult

        // First backend call succeeds (populates the cache); the second fails (background refresh).
        val successSlot = slot<(WorkflowDetailResponse) -> Unit>()
        val errorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        var call = 0
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = capture(successSlot),
                onError = capture(errorSlot),
            )
        } answers {
            call++
            if (call == 1) {
                successSlot.captured(response)
            } else {
                errorSlot.captured(
                    PurchasesError(PurchasesErrorCode.NetworkError, "boom"),
                    GetWorkflowsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_WORKFLOWS,
                )
            }
        }

        // First call at t=0 populates the cache.
        every { mockDateProvider.now } returns Date(0)
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = {},
            onError = { fail("unexpected error $it") },
        )

        // Stale call whose background refresh fails: caller still gets the stale value, no error.
        every { mockDateProvider.now } returns Date(6L * 60 * 1000)
        var served: WorkflowDataResult? = null
        var erroredWith: PurchasesError? = null
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { served = it },
            onError = { erroredWith = it },
        )

        assertThat(served).isSameAs(staleResult)
        assertThat(erroredWith).isNull()
    }

    @Test
    fun `getWorkflow stale hit re-pins from the persisted envelope when the background refresh fails`() {
        // Matches OfferingsManager: a fallback-eligible background-refresh failure recovers the
        // persisted envelope and re-stamps the cache fresh. Known gap vs offerings (re-pinning an
        // older prefetched envelope over a newer on-demand value) is bounded and self-healing, closed
        // by the on-demand envelope persistence + LRU follow-up.
        val inlineResponse = WorkflowDetailResponse(action = WorkflowResponseAction.INLINE, data = mockk())
        val staleResult = WorkflowDataResult(workflow = inlineResponse.data!!, enrolledVariants = null)
        coEvery { mockResolver.resolve(inlineResponse) } returns staleResult

        // A distinct value persisted on disk, which the failed refresh should recover and re-pin.
        val diskEnvelope = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn/wf_1.json",
            hash = "h",
        )
        val diskResult = WorkflowDataResult(workflow = mockk(), enrolledVariants = null)
        coEvery { mockResolver.resolve(diskEnvelope) } returns diskResult
        every { mockDeviceCache.getWorkflowDetailEnvelopesCache() } returns
            """{"wf_1":{"action":"use_cdn","url":"https://cdn/wf_1.json","hash":"h"}}"""

        // First backend call succeeds (populates the cache); the second fails fallback-eligibly.
        val successSlot = slot<(WorkflowDetailResponse) -> Unit>()
        val errorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        var call = 0
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = capture(successSlot),
                onError = capture(errorSlot),
            )
        } answers {
            call++
            if (call == 1) {
                successSlot.captured(inlineResponse)
            } else {
                errorSlot.captured(
                    PurchasesError(PurchasesErrorCode.NetworkError, "boom"),
                    GetWorkflowsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_WORKFLOWS,
                )
            }
        }

        // Populate at t=0 with the in-memory (INLINE) value.
        every { mockDateProvider.now } returns Date(0)
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = {},
            onError = { fail("unexpected error $it") },
        )

        // Stale call at t=6min: serves stale immediately, the background refresh then fails.
        every { mockDateProvider.now } returns Date(6L * 60 * 1000)
        var served: WorkflowDataResult? = null
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { served = it },
            onError = { fail("unexpected error $it") },
        )

        // Caller got the stale in-memory value immediately...
        assertThat(served).isSameAs(staleResult)
        // ...and the failed refresh recovered the disk envelope, re-pinned it, and re-stamped fresh.
        assertThat(workflowsCache.cachedWorkflow("wf_1")).isSameAs(diskResult)
        assertThat(workflowsCache.isWorkflowCacheStale("wf_1", appInBackground = false)).isFalse

        // Consequence: the next render serves the recovered disk value with no further backend call.
        var servedAgain: WorkflowDataResult? = null
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { servedAgain = it },
            onError = { fail("unexpected error $it") },
        )
        assertThat(servedAgain).isSameAs(diskResult)
        verify(exactly = 2) {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = any(),
                onError = any(),
            )
        }
    }

    @Test
    fun `getWorkflow with staleWhileRevalidate false blocks on the refetch instead of serving stale`() {
        val response = WorkflowDetailResponse(action = WorkflowResponseAction.INLINE, data = mockk())
        val staleResult = WorkflowDataResult(workflow = response.data!!, enrolledVariants = null)
        val refreshedResult = WorkflowDataResult(workflow = response.data!!, enrolledVariants = null)

        val successSlot = slot<(WorkflowDetailResponse) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = capture(successSlot),
                onError = any(),
            )
        } answers { successSlot.captured(response) }

        // First call at t=0 populates the cache.
        every { mockDateProvider.now } returns Date(0)
        coEvery { mockResolver.resolve(response) } returns staleResult
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = {},
            onError = { fail("unexpected error $it") },
        )

        // Stale call with SWR disabled: must deliver the freshly-fetched value, not the stale one.
        every { mockDateProvider.now } returns Date(6L * 60 * 1000)
        coEvery { mockResolver.resolve(response) } returns refreshedResult
        var served: WorkflowDataResult? = null
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { served = it },
            onError = { fail("unexpected error $it") },
            staleWhileRevalidate = false,
        )

        assertThat(served).isSameAs(refreshedResult)
        // The blocking fetch also wrote the refreshed value through to the cache.
        assertThat(workflowsCache.cachedWorkflow("wf_1")).isSameAs(refreshedResult)
    }

    // endregion getWorkflow cache

    // region getWorkflowsList

    @Test
    fun `getWorkflowsList calls backend and caches payload in DeviceCache`() {
        val response = WorkflowsListResponse(
            workflows = listOf(
                WorkflowSummary(id = "wf_1", displayName = "Flow A", offeringId = "default", prefetch = false),
            ),
        )
        val successSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(
                appUserID = "user_1",
                appInBackground = false,
                type = "paywall",
                onSuccess = capture(successSlot),
                onError = any(),
            )
        } answers { successSlot.captured(response) }

        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        verify(exactly = 1) {
            mockBackend.getWorkflows(appUserID = "user_1", appInBackground = false, type = "paywall", onSuccess = any(), onError = any())
        }
        verify(exactly = 1) { mockDeviceCache.cacheWorkflowsListResponse(any()) }
        verify(exactly = 0) { mockBackend.getWorkflow(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `getWorkflowsList skips network call when in-memory cache is fresh`() {
        val response = WorkflowsListResponse(workflows = emptyList())
        val successSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = capture(successSlot), onError = any())
        } answers { successSlot.captured(response) }
        // First call at t=0
        every { mockDateProvider.now } returns Date(0)
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        // Second call at t=1ms — still fresh (threshold is 5 minutes)
        every { mockDateProvider.now } returns Date(1)
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        verify(exactly = 1) { mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = any()) }
    }

    @Test
    fun `getWorkflowsList with forceRefresh fetches even when the in-memory cache is fresh`() {
        val response = WorkflowsListResponse(workflows = emptyList())
        val successSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = capture(successSlot), onError = any())
        } answers { successSlot.captured(response) }
        // First call at t=0 populates a fresh cache.
        every { mockDateProvider.now } returns Date(0)
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        // Second call at t=1ms would normally be skipped as fresh, but forceRefresh overrides the TTL.
        every { mockDateProvider.now } returns Date(1)
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false, forceRefresh = true)

        verify(exactly = 2) { mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = any()) }
    }

    @Test
    fun `getWorkflowsList with forceRefresh re-fetches workflow details even when within TTL`() {
        val listResponse = WorkflowsListResponse(
            workflows = listOf(
                WorkflowSummary(id = "wf_1", displayName = "Flow", offeringId = "default", prefetch = true),
            ),
        )
        val envelope = WorkflowDetailResponse(action = WorkflowResponseAction.INLINE, data = mockk())
        coEvery { mockResolver.resolve(envelope) } returns
            WorkflowDataResult(workflow = envelope.data!!, enrolledVariants = null)
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = any())
        } answers {
            @Suppress("UNCHECKED_CAST")
            (args[3] as (WorkflowsListResponse) -> Unit).invoke(listResponse)
        }
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = any(),
                onError = any(),
                callbackDispatcher = mockPrefetchDispatcher,
            )
        } answers {
            @Suppress("UNCHECKED_CAST")
            (args[3] as (WorkflowDetailResponse) -> Unit).invoke(envelope)
        }

        // Initial load at t=0: list + detail fetched and cached (detail is fresh within TTL).
        every { mockDateProvider.now } returns Date(0)
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        // forceRefresh at t=1ms: still within 5-min detail TTL, but forceRefresh clears detail
        // caches so the prefetch is a guaranteed cache miss and goes to the backend.
        every { mockDateProvider.now } returns Date(1)
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false, forceRefresh = true)

        verify(exactly = 2) { mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = any()) }
        verify(exactly = 2) {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = any(),
                onError = any(),
                callbackDispatcher = mockPrefetchDispatcher,
            )
        }
    }

    @Test
    fun `getWorkflowsList with forceRefresh preserves in-memory detail caches when the fetch fails`() {
        val listResponse = WorkflowsListResponse(
            workflows = listOf(
                WorkflowSummary(id = "wf_1", displayName = "Flow", offeringId = "default", prefetch = true),
            ),
        )
        val envelope = WorkflowDetailResponse(action = WorkflowResponseAction.INLINE, data = mockk())
        val expectedResult = WorkflowDataResult(workflow = envelope.data!!, enrolledVariants = null)
        coEvery { mockResolver.resolve(envelope) } returns expectedResult
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = any(),
                onError = any(),
                callbackDispatcher = mockPrefetchDispatcher,
            )
        } answers {
            @Suppress("UNCHECKED_CAST")
            (args[3] as (WorkflowDetailResponse) -> Unit).invoke(envelope)
        }

        val successSlot = slot<(WorkflowsListResponse) -> Unit>()
        val errorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        var listCalls = 0
        every {
            mockBackend.getWorkflows(
                any(),
                any(),
                type = any(),
                onSuccess = capture(successSlot),
                onError = capture(errorSlot),
            )
        } answers {
            listCalls += 1
            when (listCalls) {
                1 -> successSlot.captured(listResponse)
                else -> errorSlot.captured(
                    PurchasesError(PurchasesErrorCode.NetworkError, "fail"),
                    GetWorkflowsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_WORKFLOWS,
                )
            }
        }
        every { mockDeviceCache.getWorkflowsListResponseCache() } returns null
        every { mockDeviceCache.getWorkflowDetailEnvelopesCache() } returns null

        // Initial load at t=0: list + detail fetched and cached.
        every { mockDateProvider.now } returns Date(0)
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        // forceRefresh at t=1ms fails — detail cache must survive intact.
        every { mockDateProvider.now } returns Date(1)
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false, forceRefresh = true)

        // getWorkflow should return the cached result without a new backend call.
        var result: WorkflowDataResult? = null
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { result = it },
            onError = { fail("unexpected error: $it") },
        )

        assertThat(result).isEqualTo(expectedResult)
        verify(exactly = 1) {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = any(),
                onError = any(),
                callbackDispatcher = mockPrefetchDispatcher,
            )
        }
        verify(exactly = 0) {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = any(),
                onError = any(),
            )
        }
    }

    @Test
    fun `getWorkflowsList triggers getWorkflow for each prefetch=true entry only`() {
        val response = WorkflowsListResponse(
            workflows = listOf(
                WorkflowSummary(id = "wf_prefetch", displayName = "A", offeringId = "default", prefetch = true),
                WorkflowSummary(id = "wf_skip", displayName = "B", offeringId = "premium", prefetch = false),
                WorkflowSummary(id = "wf_also_prefetch", displayName = "C", offeringId = "pro", prefetch = true),
            ),
        )
        val successSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = capture(successSlot), onError = any())
        } answers { successSlot.captured(response) }

        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        verify(exactly = 1) {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_prefetch",
                any(),
                any(),
                any(),
                callbackDispatcher = mockPrefetchDispatcher,
            )
        }
        verify(exactly = 0) {
            mockBackend.getWorkflow(appUserID = "user_1", workflowId = "wf_skip", any(), any(), any(), any())
        }
        verify(exactly = 1) {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_also_prefetch",
                any(),
                any(),
                any(),
                callbackDispatcher = mockPrefetchDispatcher,
            )
        }
    }

    @Test
    fun `getWorkflowsList does not prefetch workflows without an offeringId`() {
        val response = WorkflowsListResponse(
            workflows = listOf(
                WorkflowSummary(id = "wf_no_offering", displayName = "A", offeringId = null, prefetch = true),
                WorkflowSummary(id = "wf_with_offering", displayName = "B", offeringId = "default", prefetch = true),
            ),
        )
        val successSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = capture(successSlot), onError = any())
        } answers { successSlot.captured(response) }

        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        verify(exactly = 0) {
            mockBackend.getWorkflow(appUserID = "user_1", workflowId = "wf_no_offering", any(), any(), any(), any())
        }
        verify(exactly = 1) {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_with_offering",
                any(),
                any(),
                any(),
                callbackDispatcher = mockPrefetchDispatcher,
            )
        }
    }

    @Test
    fun `getWorkflowsList does not cache workflows without an offeringId`() {
        val response = WorkflowsListResponse(
            workflows = listOf(
                WorkflowSummary(id = "wf_no_offering", displayName = "A", offeringId = null, prefetch = false),
                WorkflowSummary(id = "wf_with_offering", displayName = "B", offeringId = "default", prefetch = false),
            ),
        )
        val successSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = capture(successSlot), onError = any())
        } answers { successSlot.captured(response) }
        val cachedJson = slot<String>()
        every { mockDeviceCache.cacheWorkflowsListResponse(capture(cachedJson)) } just Runs

        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        assertThat(cachedJson.captured).contains("wf_with_offering")
        assertThat(cachedJson.captured).doesNotContain("wf_no_offering")
    }

    @Test
    fun `getWorkflowsList fetches every prefetch=true workflow`() {
        val workflows = (0 until 10).map {
            WorkflowSummary(id = "wf_$it", displayName = "W$it", offeringId = "off_$it", prefetch = true)
        }
        val response = WorkflowsListResponse(workflows = workflows)
        val listSuccessSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = capture(listSuccessSlot), onError = any())
        } answers { listSuccessSlot.captured(response) }

        // Hold every prefetch in flight by never invoking its callback. The manager does not cap
        // concurrency itself: detail fetches are bounded by prefetchDispatcher's thread pool and CDN
        // downloads by the workflows FileRepository scope, so every prefetch=true workflow is launched.
        every { mockBackend.getWorkflow(any(), any(), any(), any(), any(), any()) } answers { }

        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        verify(exactly = 10) { mockBackend.getWorkflow(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `on-demand getWorkflow runs on the default dispatcher, not the prefetch one`() {
        every { mockBackend.getWorkflow(any(), any(), any(), any(), any(), any()) } answers { }

        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = {},
            onError = {},
        )

        verify(exactly = 1) {
            mockBackend.getWorkflow("user_1", "wf_1", false, any(), any())
        }
    }

    @Test
    fun `getWorkflowsList silently logs error on backend failure`() {
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "network error")
        val errorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = capture(errorSlot))
        } answers { errorSlot.captured(error, GetWorkflowsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_WORKFLOWS) }

        // Should not throw
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        verify(exactly = 0) { mockDeviceCache.cacheWorkflowsListResponse(any()) }
    }

    @Test
    fun `getWorkflowsList restores offeringId map from disk cache on backend failure`() {
        val cachedJson = """{"workflows":[{"id":"wf_1","display_name":"Flow","offering_id":"default","prefetch":false}]}"""
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "network error")
        val errorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = capture(errorSlot))
        } answers { errorSlot.captured(error, GetWorkflowsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_WORKFLOWS) }
        every { mockDeviceCache.getWorkflowsListResponseCache() } returns cachedJson

        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        assertThat(workflowManager.workflowIdForOfferingId("default")).isEqualTo("wf_1")
        // The disk copy is the source we restored from, so the recovery path must not rewrite it.
        verify(exactly = 0) { mockDeviceCache.cacheWorkflowsListResponse(any()) }
    }

    @Test
    fun `getWorkflowsList silently ignores corrupt disk cache on backend failure`() {
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "network error")
        val errorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = capture(errorSlot))
        } answers { errorSlot.captured(error, GetWorkflowsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_WORKFLOWS) }
        every { mockDeviceCache.getWorkflowsListResponseCache() } returns "not valid json"

        // Should not throw
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        assertThat(workflowManager.workflowIdForOfferingId("default")).isNull()
    }

    // endregion getWorkflowsList

    // region workflowIdForOfferingId

    @Test
    fun `workflowIdForOfferingId returns null before list is fetched`() {
        assertThat(workflowManager.workflowIdForOfferingId("default")).isNull()
    }

    @Test
    fun `workflowIdForOfferingId returns workflow id after list is fetched`() {
        val response = WorkflowsListResponse(
            workflows = listOf(
                WorkflowSummary(id = "wf_abc", displayName = "Flow", offeringId = "default", prefetch = false),
            ),
        )
        val successSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = capture(successSlot), onError = any())
        } answers { successSlot.captured(response) }

        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        assertThat(workflowManager.workflowIdForOfferingId("default")).isEqualTo("wf_abc")
        assertThat(workflowManager.workflowIdForOfferingId("premium")).isNull()
    }

    @Test
    fun `workflowIdForOfferingId returns null for workflow with null offering_id`() {
        val response = WorkflowsListResponse(
            workflows = listOf(
                WorkflowSummary(id = "wf_no_offering", displayName = "Flow", offeringId = null, prefetch = false),
            ),
        )
        val successSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = capture(successSlot), onError = any())
        } answers { successSlot.captured(response) }

        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        assertThat(workflowManager.workflowIdForOfferingId("default")).isNull()
    }

    // endregion workflowIdForOfferingId

    // region issue fixes

    // Test A — disk-cache fallback stamps in-memory cache so re-fetch after TTL still fires once more
    @Test
    fun `getWorkflowsList after disk-cache restore does not re-fetch before TTL expires`() {
        val cachedJson = """{"workflows":[{"id":"wf_1","display_name":"Flow","offering_id":"default","prefetch":false}]}"""
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "network error")
        val errorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = capture(errorSlot))
        } answers { errorSlot.captured(error, GetWorkflowsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_WORKFLOWS) }
        every { mockDeviceCache.getWorkflowsListResponseCache() } returns cachedJson

        // First call fails → restores from disk
        every { mockDateProvider.now } returns Date(0)
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)
        assertThat(workflowManager.workflowIdForOfferingId("default")).isEqualTo("wf_1")

        // Second call immediately after — in-memory cache should be considered fresh (t=1ms)
        every { mockDateProvider.now } returns Date(1)
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        // Only one backend call total (the first one that failed)
        verify(exactly = 1) { mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = any()) }
    }

    // Test A (continued) — re-fetch fires again after TTL expires
    @Test
    fun `getWorkflowsList re-fetches after TTL expiry following disk-cache restore`() {
        val cachedJson = """{"workflows":[{"id":"wf_1","display_name":"Flow","offering_id":"default","prefetch":false}]}"""
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "network error")
        val errorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = capture(errorSlot))
        } answers { errorSlot.captured(error, GetWorkflowsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_WORKFLOWS) }
        every { mockDeviceCache.getWorkflowsListResponseCache() } returns cachedJson

        // First call at t=0 — fails, restores from disk, stamps in-memory cache at t=0
        every { mockDateProvider.now } returns Date(0)
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        // Second call well past TTL (6 minutes = 360_000ms) — should fire a new network request
        val sixMinutesMs = 6L * 60 * 1000
        every { mockDateProvider.now } returns Date(sixMinutesMs)
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        // Two backend calls total: first failure + re-fetch after TTL
        verify(exactly = 2) { mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = any()) }
    }

    // Test B — concurrent calls only fire one network request
    @Test
    fun `getWorkflowsList concurrent calls only fire one network request`() {
        // Hold the backend call without resolving it so the first call stays in-flight
        val successSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = capture(successSlot), onError = any())
        } answers { /* intentionally do not call successSlot to simulate in-flight */ }

        every { mockDateProvider.now } returns Date(0)
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)
        // Second call while first is still in-flight
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        verify(exactly = 1) { mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = any()) }
    }

    // Test C — duplicate offeringId: last value wins, no crash
    @Test
    fun `getWorkflowsList with duplicate offeringId keeps last entry`() {
        val response = WorkflowsListResponse(
            workflows = listOf(
                WorkflowSummary(id = "wf_first", displayName = "First", offeringId = "shared", prefetch = false),
                WorkflowSummary(id = "wf_last", displayName = "Last", offeringId = "shared", prefetch = false),
            ),
        )
        val successSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = capture(successSlot), onError = any())
        } answers { successSlot.captured(response) }

        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        assertThat(workflowManager.workflowIdForOfferingId("shared")).isEqualTo("wf_last")
    }

    // Test C (disk-cache path) — duplicate offeringId in restored disk cache: last value wins
    @Test
    fun `getWorkflowsList disk-cache restore with duplicate offeringId keeps last entry`() {
        val cachedJson = """{"workflows":[
            {"id":"wf_first","display_name":"First","offering_id":"shared","prefetch":false},
            {"id":"wf_last","display_name":"Last","offering_id":"shared","prefetch":false}
        ]}"""
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "network error")
        val errorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = capture(errorSlot))
        } answers { errorSlot.captured(error, GetWorkflowsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_WORKFLOWS) }
        every { mockDeviceCache.getWorkflowsListResponseCache() } returns cachedJson

        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        assertThat(workflowManager.workflowIdForOfferingId("shared")).isEqualTo("wf_last")
    }

    // Clearing the workflows cache (e.g. on user switch) must drop the in-memory list cache and
    // offeringId map so the next call re-fetches instead of serving the previous user's workflows.
    @Test
    fun `clearing workflows cache drops offeringId map and forces re-fetch within TTL`() {
        val response = WorkflowsListResponse(
            workflows = listOf(
                WorkflowSummary(id = "wf_1", displayName = "Flow", offeringId = "default", prefetch = false),
            ),
        )
        val successSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = capture(successSlot), onError = any())
        } answers { successSlot.captured(response) }

        // First fetch at t=0 populates the list cache + offeringId map.
        every { mockDateProvider.now } returns Date(0)
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)
        assertThat(workflowManager.workflowIdForOfferingId("default")).isEqualTo("wf_1")

        // Simulate a user switch clearing the cache.
        workflowsCache.clearCache()

        // The offeringId map must be gone.
        assertThat(workflowManager.workflowIdForOfferingId("default")).isNull()

        // The next call within TTL must re-fetch because the in-memory list cache was cleared.
        every { mockDateProvider.now } returns Date(1)
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)
        verify(exactly = 2) {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = any())
        }
    }

    // endregion issue fixes

    // region onComplete callback

    @Test
    fun `getWorkflowsList calls onComplete after backend success with no prefetch workflows`() {
        val response = WorkflowsListResponse(workflows = listOf(
            WorkflowSummary(id = "wf_1", displayName = "Flow", offeringId = "default", prefetch = false),
        ))
        val successSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = capture(successSlot), onError = any())
        } answers { successSlot.captured(response) }

        var completed = false
        workflowManager.getWorkflowsList("user_1", false) { completed = true }

        assertThat(completed).isTrue()
    }

    @Test
    fun `getWorkflowsList calls onComplete immediately when cache is fresh`() {
        val response = WorkflowsListResponse(workflows = emptyList())
        val successSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = capture(successSlot), onError = any())
        } answers { successSlot.captured(response) }

        every { mockDateProvider.now } returns Date(0)
        workflowManager.getWorkflowsList("user_1", false)

        every { mockDateProvider.now } returns Date(1) // still within TTL
        var completed = false
        workflowManager.getWorkflowsList("user_1", false) { completed = true }

        assertThat(completed).isTrue()
        verify(exactly = 1) { mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = any()) }
    }

    @Test
    fun `getWorkflowsList calls onComplete only after all prefetch workflows complete`() {
        val response = WorkflowsListResponse(workflows = listOf(
            WorkflowSummary(id = "wf_a", displayName = "A", offeringId = "default", prefetch = true),
            WorkflowSummary(id = "wf_b", displayName = "B", offeringId = "premium", prefetch = true),
        ))
        val listSuccessSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = capture(listSuccessSlot), onError = any())
        } answers { listSuccessSlot.captured(response) }

        val detailSuccessA = slot<(WorkflowDetailResponse) -> Unit>()
        val detailSuccessB = slot<(WorkflowDetailResponse) -> Unit>()
        every {
            mockBackend.getWorkflow("user_1", "wf_a", false, capture(detailSuccessA), any(), any())
        } just Runs
        every {
            mockBackend.getWorkflow("user_1", "wf_b", false, capture(detailSuccessB), any(), any())
        } just Runs
        coEvery { mockResolver.resolve(any()) } returns mockk()

        var completed = false
        workflowManager.getWorkflowsList("user_1", false) { completed = true }

        assertThat(completed).isFalse()

        val detailResponse = WorkflowDetailResponse(action = WorkflowResponseAction.INLINE, data = mockk())
        detailSuccessA.captured(detailResponse)
        assertThat(completed).isFalse()

        detailSuccessB.captured(detailResponse)
        assertThat(completed).isTrue()
    }

    @Test
    fun `getWorkflowsList second caller waits for in-flight prefetch when list cache is fresh`() {
        val response = WorkflowsListResponse(workflows = listOf(
            WorkflowSummary(id = "wf_a", displayName = "A", offeringId = "default", prefetch = true),
        ))
        val listSuccessSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = capture(listSuccessSlot), onError = any())
        } answers { listSuccessSlot.captured(response) }

        val detailSuccess = slot<(WorkflowDetailResponse) -> Unit>()
        every {
            mockBackend.getWorkflow("user_1", "wf_a", false, capture(detailSuccess), any(), any())
        } just Runs
        coEvery { mockResolver.resolve(any()) } returns mockk()

        var firstCompleted = false
        var secondCompleted = false
        workflowManager.getWorkflowsList("user_1", false) { firstCompleted = true }

        assertThat(firstCompleted).isFalse()

        workflowManager.getWorkflowsList("user_1", false) { secondCompleted = true }

        assertThat(secondCompleted).isFalse()

        detailSuccess.captured(WorkflowDetailResponse(action = WorkflowResponseAction.INLINE, data = mockk()))

        assertThat(firstCompleted).isTrue()
        assertThat(secondCompleted).isTrue()
    }

    @Test
    fun `getWorkflowsList calls onComplete even if a prefetch workflow fails`() {
        val response = WorkflowsListResponse(workflows = listOf(
            WorkflowSummary(id = "wf_a", displayName = "A", offeringId = "default", prefetch = true),
            WorkflowSummary(id = "wf_b", displayName = "B", offeringId = "premium", prefetch = true),
        ))
        val listSuccessSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = capture(listSuccessSlot), onError = any())
        } answers { listSuccessSlot.captured(response) }

        val detailSuccessA = slot<(WorkflowDetailResponse) -> Unit>()
        val detailErrorB = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        every {
            mockBackend.getWorkflow("user_1", "wf_a", false, capture(detailSuccessA), any(), any())
        } just Runs
        every {
            mockBackend.getWorkflow("user_1", "wf_b", false, any(), capture(detailErrorB), any())
        } just Runs
        coEvery { mockResolver.resolve(any()) } returns mockk()

        var completed = false
        workflowManager.getWorkflowsList("user_1", false) { completed = true }

        assertThat(completed).isFalse()

        detailSuccessA.captured(WorkflowDetailResponse(action = WorkflowResponseAction.INLINE, data = mockk()))
        assertThat(completed).isFalse()

        detailErrorB.captured(
            PurchasesError(PurchasesErrorCode.NetworkError, "fail"),
            GetWorkflowsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_WORKFLOWS,
        )
        assertThat(completed).isTrue()
    }

    @Test
    fun `getWorkflowsList calls onComplete when a prefetch workflow resolve throws unexpected exception`() {
        val response = WorkflowsListResponse(workflows = listOf(
            WorkflowSummary(id = "wf_a", displayName = "A", offeringId = "default", prefetch = true),
        ))
        val listSuccessSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = capture(listSuccessSlot), onError = any())
        } answers { listSuccessSlot.captured(response) }

        val detailSuccessA = slot<(WorkflowDetailResponse) -> Unit>()
        every {
            mockBackend.getWorkflow("user_1", "wf_a", false, capture(detailSuccessA), any(), any())
        } just Runs
        // resolve throws an exception type not in the explicit catch list (e.g. malformed CDN json)
        coEvery { mockResolver.resolve(any()) } throws SerializationException("malformed compiled workflow json")

        var completed = false
        workflowManager.getWorkflowsList("user_1", false) { completed = true }

        detailSuccessA.captured(WorkflowDetailResponse(action = WorkflowResponseAction.USE_CDN, url = "https://cdn.example.com/wf.json"))

        assertThat(completed).isTrue()
    }

    @Test
    fun `getWorkflowsList calls onComplete after network error`() {
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "fail")
        val errorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = capture(errorSlot))
        } answers { errorSlot.captured(error, GetWorkflowsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_WORKFLOWS) }

        var completed = false
        workflowManager.getWorkflowsList("user_1", false) { completed = true }

        assertThat(completed).isTrue()
    }

    @Test
    fun `getWorkflowsList concurrent in-flight calls both receive onComplete`() {
        val listSuccessSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = capture(listSuccessSlot), onError = any())
        } just Runs // hold the request in-flight

        var completed1 = false
        var completed2 = false
        workflowManager.getWorkflowsList("user_1", false) { completed1 = true }
        workflowManager.getWorkflowsList("user_1", false) { completed2 = true }

        assertThat(completed1).isFalse()
        assertThat(completed2).isFalse()

        listSuccessSlot.captured(WorkflowsListResponse(workflows = emptyList()))

        assertThat(completed1).isTrue()
        assertThat(completed2).isTrue()
        verify(exactly = 1) { mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = any()) }
    }

    @Test
    fun `getWorkflowsList for a different user does not join an in-flight fetch for the previous user`() {
        val user1Success = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows("user_1", any(), type = any(), onSuccess = capture(user1Success), onError = any())
        } just Runs // hold user_1 in-flight

        val user2Success = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows("user_2", any(), type = any(), onSuccess = capture(user2Success), onError = any())
        } just Runs // hold user_2 in-flight

        var user1Completed = false
        var user2Completed = false
        workflowManager.getWorkflowsList("user_1", false) { user1Completed = true }
        workflowManager.getWorkflowsList("user_2", false) { user2Completed = true }

        // user_2 must start its own fetch rather than join user_1's in-flight request.
        verify(exactly = 1) {
            mockBackend.getWorkflows("user_2", any(), type = any(), onSuccess = any(), onError = any())
        }
        assertThat(user1Completed).isFalse()
        assertThat(user2Completed).isFalse()

        // user_2's fetch landing completes only user_2, not user_1.
        user2Success.captured(WorkflowsListResponse(workflows = emptyList()))
        assertThat(user2Completed).isTrue()
        assertThat(user1Completed).isFalse()

        // user_1's fetch landing then completes user_1.
        user1Success.captured(WorkflowsListResponse(workflows = emptyList()))
        assertThat(user1Completed).isTrue()
    }

    // endregion onComplete callback

    // region envelope persistence

    @Test
    fun `prefetch persists the workflow detail envelope`() {
        val envelope = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn/wf_1.json",
            hash = "h",
        )
        coEvery { mockResolver.resolve(envelope) } returns
            WorkflowDataResult(workflow = mockk(), enrolledVariants = null)

        val listResponse = WorkflowsListResponse(
            workflows = listOf(
                WorkflowSummary(id = "wf_1", displayName = "A", offeringId = "default", prefetch = true),
            ),
        )
        val listSuccess = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = capture(listSuccess), onError = any())
        } answers { listSuccess.captured(listResponse) }

        val detailSuccess = slot<(WorkflowDetailResponse) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                any(),
                onSuccess = capture(detailSuccess),
                onError = any(),
                callbackDispatcher = mockPrefetchDispatcher,
            )
        } answers { detailSuccess.captured(envelope) }

        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        // WorkflowsCache.cacheWorkflowDetailEnvelope -> DeviceCache.cacheWorkflowDetailEnvelopes (the mocked layer)
        val persistedSlot = slot<String>()
        verify(exactly = 1) { mockDeviceCache.cacheWorkflowDetailEnvelopes(capture(persistedSlot)) }
        assertThat(persistedSlot.captured).contains("wf_1")
    }

    @Test
    fun `prefetch persists the envelope even when the workflow is already cached but stale`() {
        // Seed the detail cache at t=0 so the workflow is present but will be stale at prefetch time.
        every { mockDateProvider.now } returns Date(0)
        workflowsCache.cacheWorkflow("wf_1", WorkflowDataResult(workflow = mockk(), enrolledVariants = null))

        // Advance past the 5-minute TTL: the prefetch now sees a stale-but-present cache entry.
        // With SWR disabled on the prefetch path it must still do a real fetch and persist, not serve stale.
        every { mockDateProvider.now } returns Date(6L * 60 * 1000)

        val envelope = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn/wf_1.json",
            hash = "h",
        )
        coEvery { mockResolver.resolve(envelope) } returns
            WorkflowDataResult(workflow = mockk(), enrolledVariants = null)

        val listResponse = WorkflowsListResponse(
            workflows = listOf(
                WorkflowSummary(id = "wf_1", displayName = "A", offeringId = "default", prefetch = true),
            ),
        )
        val listSuccess = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = capture(listSuccess), onError = any())
        } answers { listSuccess.captured(listResponse) }

        val detailSuccess = slot<(WorkflowDetailResponse) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                any(),
                onSuccess = capture(detailSuccess),
                onError = any(),
                callbackDispatcher = mockPrefetchDispatcher,
            )
        } answers { detailSuccess.captured(envelope) }

        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        // A real prefetch fetch happened (the prefetch overload with the prefetch dispatcher)...
        verify(exactly = 1) {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                any(),
                onSuccess = any(),
                onError = any(),
                callbackDispatcher = mockPrefetchDispatcher,
            )
        }
        // ...and the envelope was persisted, proving the stale prefetch took the blocking fetch path.
        val persistedSlot = slot<String>()
        verify(exactly = 1) { mockDeviceCache.cacheWorkflowDetailEnvelopes(capture(persistedSlot)) }
        assertThat(persistedSlot.captured).contains("wf_1")
    }

    @Test
    fun `on-demand getWorkflow does not persist the envelope`() {
        val envelope = WorkflowDetailResponse(action = WorkflowResponseAction.INLINE, data = mockk())
        coEvery { mockResolver.resolve(envelope) } returns
            WorkflowDataResult(workflow = envelope.data!!, enrolledVariants = null)

        val successSlot = slot<(WorkflowDetailResponse) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = capture(successSlot),
                onError = any(),
            )
        } answers { successSlot.captured(envelope) }

        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = {},
            onError = { fail("unexpected error $it") },
        )

        verify(exactly = 0) { mockDeviceCache.cacheWorkflowDetailEnvelopes(any()) }
    }

    @Test
    fun `prefetch does not persist the envelope when resolve fails`() {
        val envelope = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn/wf_1.json",
            hash = "h",
        )
        coEvery { mockResolver.resolve(envelope) } throws IllegalStateException("boom")

        val listResponse = WorkflowsListResponse(
            workflows = listOf(
                WorkflowSummary(id = "wf_1", displayName = "A", offeringId = "default", prefetch = true),
            ),
        )
        val listSuccess = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = capture(listSuccess), onError = any())
        } answers { listSuccess.captured(listResponse) }

        val detailSuccess = slot<(WorkflowDetailResponse) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                any(),
                onSuccess = capture(detailSuccess),
                onError = any(),
                callbackDispatcher = mockPrefetchDispatcher,
            )
        } answers { detailSuccess.captured(envelope) }

        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        verify(exactly = 0) { mockDeviceCache.cacheWorkflowDetailEnvelopes(any()) }
    }

    @Test
    fun `prefetch still completes when persisting the envelope throws`() {
        val envelope = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn/wf_1.json",
            hash = "h",
        )
        val resolved = WorkflowDataResult(workflow = mockk(), enrolledVariants = null)
        coEvery { mockResolver.resolve(envelope) } returns resolved
        // Persisting blows up at the DeviceCache layer.
        every { mockDeviceCache.cacheWorkflowDetailEnvelopes(any()) } throws IOException("disk full")

        val listResponse = WorkflowsListResponse(
            workflows = listOf(
                WorkflowSummary(id = "wf_1", displayName = "A", offeringId = "default", prefetch = true),
            ),
        )
        val listSuccess = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = capture(listSuccess), onError = any())
        } answers { listSuccess.captured(listResponse) }

        val detailSuccess = slot<(WorkflowDetailResponse) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                any(),
                onSuccess = capture(detailSuccess),
                onError = any(),
                callbackDispatcher = mockPrefetchDispatcher,
            )
        } answers { detailSuccess.captured(envelope) }

        var completeCount = 0
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false) { completeCount++ }

        // Persistence failure must not hang the prefetch: onComplete still fires exactly once...
        assertThat(completeCount).isEqualTo(1)
        // ...and the resolved workflow is still cached in memory (cacheWorkflow ran before the failed persist).
        assertThat(workflowsCache.cachedWorkflow("wf_1")).isSameAs(resolved)
    }

    @Test
    fun `prefetch detail fetch falls back to the persisted envelope on a fallback-eligible error`() {
        // The disk fallback applies to every caller, including prefetch: this is the intended
        // backend-down recovery (consistent with restoreWorkflowFromEnvelope). The persisted envelope
        // is re-resolved, cached, and re-stamped fresh.
        val listResponse = WorkflowsListResponse(
            workflows = listOf(
                WorkflowSummary(id = "wf_1", displayName = "W", offeringId = "off_1", prefetch = true),
            ),
        )
        val listSuccessSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = capture(listSuccessSlot), onError = any())
        } answers { listSuccessSlot.captured(listResponse) }

        // The persisted envelope on disk and the value it resolves to.
        val diskEnvelope = WorkflowDetailResponse(
            action = WorkflowResponseAction.USE_CDN,
            url = "https://cdn/wf_1.json",
            hash = "h",
        )
        val diskResult = WorkflowDataResult(workflow = mockk(), enrolledVariants = null)
        coEvery { mockResolver.resolve(diskEnvelope) } returns diskResult
        every { mockDeviceCache.getWorkflowDetailEnvelopesCache() } returns
            """{"wf_1":{"action":"use_cdn","url":"https://cdn/wf_1.json","hash":"h"}}"""

        // The prefetch detail fetch fails fallback-eligibly (5xx) on the prefetch dispatcher.
        val detailErrorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = any(),
                onError = capture(detailErrorSlot),
                callbackDispatcher = mockPrefetchDispatcher,
            )
        } answers {
            detailErrorSlot.captured(
                PurchasesError(PurchasesErrorCode.NetworkError, "server boom"),
                GetWorkflowsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_WORKFLOWS,
            )
        }

        every { mockDateProvider.now } returns Date(0)
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        // The prefetch recovered the persisted envelope and cached it in memory.
        coVerify(exactly = 1) { mockResolver.resolve(diskEnvelope) }
        assertThat(workflowsCache.cachedWorkflow("wf_1")).isSameAs(diskResult)
        // cacheWorkflow re-stamped it fresh, so an on-demand render serves this recovered value
        // without re-hitting the backend until it goes stale again.
        assertThat(workflowsCache.isWorkflowCacheStale("wf_1", appInBackground = false)).isFalse
    }

    // endregion envelope persistence

    // region onError envelope restore

    @Test
    fun `list onError re-resolves persisted envelopes so getWorkflow is a cache hit`() {
        // The resolver is mocked, so INLINE vs USE_CDN is indistinguishable here — the manager just
        // hands the envelope to the resolver. This proves restore -> re-resolve -> cache-hit.
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "network error")
        val errorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = capture(errorSlot))
        } answers { errorSlot.captured(error, GetWorkflowsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_WORKFLOWS) }
        every { mockDeviceCache.getWorkflowsListResponseCache() } returns
            """{"workflows":[{"id":"wf_1","display_name":"Flow","offering_id":"default","prefetch":true}]}"""
        every { mockDeviceCache.getWorkflowDetailEnvelopesCache() } returns
            """{"wf_1":{"action":"use_cdn","url":"https://cdn/wf_1.json","hash":"h"}}"""

        val restored = WorkflowDataResult(workflow = mockk(), enrolledVariants = mapOf("e" to "v"))
        coEvery {
            mockResolver.resolve(
                WorkflowDetailResponse(action = WorkflowResponseAction.USE_CDN, url = "https://cdn/wf_1.json", hash = "h"),
            )
        } returns restored

        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        assertThat(workflowManager.workflowIdForOfferingId("default")).isEqualTo("wf_1")
        var result: WorkflowDataResult? = null
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { result = it },
            onError = { fail("expected cache hit, got $it") },
        )
        assertThat(result).isSameAs(restored)
        verify(exactly = 0) { mockBackend.getWorkflow(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `list onError completes once and isolates a failing envelope re-resolve`() {
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "network error")
        val errorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = capture(errorSlot))
        } answers { errorSlot.captured(error, GetWorkflowsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_WORKFLOWS) }
        every { mockDeviceCache.getWorkflowsListResponseCache() } returns
            """{"workflows":[{"id":"wf_ok","display_name":"OK","offering_id":"a","prefetch":true},{"id":"wf_bad","display_name":"BAD","offering_id":"b","prefetch":true}]}"""
        every { mockDeviceCache.getWorkflowDetailEnvelopesCache() } returns
            """{"wf_ok":{"action":"use_cdn","url":"u_ok","hash":"h"},"wf_bad":{"action":"use_cdn","url":"u_bad","hash":"h"}}"""

        val okResult = WorkflowDataResult(workflow = mockk(), enrolledVariants = null)
        coEvery {
            mockResolver.resolve(WorkflowDetailResponse(action = WorkflowResponseAction.USE_CDN, url = "u_ok", hash = "h"))
        } returns okResult
        coEvery {
            mockResolver.resolve(WorkflowDetailResponse(action = WorkflowResponseAction.USE_CDN, url = "u_bad", hash = "h"))
        } throws IllegalStateException("cdn read failed")

        var completeCount = 0
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false) { completeCount++ }

        assertThat(completeCount).isEqualTo(1)
        assertThat(workflowsCache.cachedWorkflow("wf_ok")).isSameAs(okResult)
        assertThat(workflowsCache.cachedWorkflow("wf_bad")).isNull()
    }

    @Test
    fun `list onError completes immediately when no envelopes are persisted`() {
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "network error")
        val errorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = capture(errorSlot))
        } answers { errorSlot.captured(error, GetWorkflowsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_WORKFLOWS) }
        every { mockDeviceCache.getWorkflowDetailEnvelopesCache() } returns null

        var completeCount = 0
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false) { completeCount++ }

        assertThat(completeCount).isEqualTo(1)
        coVerify(exactly = 0) { mockResolver.resolve(any()) }
    }

    @Test
    fun `list onError completes once when the persisted envelope payload is corrupt`() {
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "network error")
        val errorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = capture(errorSlot))
        } answers { errorSlot.captured(error, GetWorkflowsErrorHandlingBehavior.SHOULD_FALLBACK_TO_CACHED_WORKFLOWS) }
        every { mockDeviceCache.getWorkflowDetailEnvelopesCache() } returns "not valid json"

        var completeCount = 0
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false) { completeCount++ }

        assertThat(completeCount).isEqualTo(1)
    }

    @Test
    fun `getWorkflowsList does not restore from disk on a non-fallback (4xx) error`() {
        val cachedJson = """{"workflows":[{"id":"wf_1","display_name":"Flow","offering_id":"default","prefetch":false}]}"""
        val error = PurchasesError(PurchasesErrorCode.InvalidCredentialsError, "forbidden")
        val errorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = capture(errorSlot))
        } answers { errorSlot.captured(error, GetWorkflowsErrorHandlingBehavior.SHOULD_NOT_FALLBACK) }
        every { mockDeviceCache.getWorkflowsListResponseCache() } returns cachedJson
        every { mockDeviceCache.getWorkflowDetailEnvelopesCache() } returns null

        var completeCount = 0
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false) { completeCount++ }

        // No restore: the offeringId map stays empty, and the disk read is never consulted for restore.
        assertThat(workflowManager.workflowIdForOfferingId("default")).isNull()
        verify(exactly = 0) { mockDeviceCache.getWorkflowsListResponseCache() }
        verify(exactly = 0) { mockDeviceCache.getWorkflowDetailEnvelopesCache() }
        // Offerings delivery is never stranded.
        assertThat(completeCount).isEqualTo(1)
    }

    @Test
    fun `getWorkflowsList invalidates list timestamp on a non-fallback (4xx) error`() {
        // Stamp the in-memory cache as fresh so the cache does not look stale before the 4xx.
        val recentDate = Date(1_000_000)
        every { mockDateProvider.now } returns recentDate
        workflowsCache.cacheWorkflowsListInMemory(
            WorkflowsListResponse(workflows = emptyList()),
            emptyMap(),
        )
        assertThat(workflowsCache.isWorkflowsListCacheStale(appInBackground = false)).isFalse()

        val error = PurchasesError(PurchasesErrorCode.InvalidCredentialsError, "forbidden")
        val errorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = capture(errorSlot))
        } answers { errorSlot.captured(error, GetWorkflowsErrorHandlingBehavior.SHOULD_NOT_FALLBACK) }

        // forceRefresh bypasses the freshness check, matching the real caller (createAndCacheOfferings).
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false, forceRefresh = true) {}

        // Timestamp must be cleared so a subsequent non-forced call retries rather than serving a
        // still-fresh in-memory list — mirrors OfferingsManager.handleErrorFetchingOfferings.
        assertThat(workflowsCache.isWorkflowsListCacheStale(appInBackground = false)).isTrue()
    }

    @Test
    fun `getWorkflowsList completes all concurrent callers exactly once on a non-fallback (4xx) error`() {
        val cachedJson = """{"workflows":[{"id":"wf_1","display_name":"Flow","offering_id":"default","prefetch":false}]}"""
        val error = PurchasesError(PurchasesErrorCode.InvalidCredentialsError, "forbidden")
        val errorSlot = slot<(PurchasesError, GetWorkflowsErrorHandlingBehavior) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = capture(errorSlot))
        } just Runs // hold the request in-flight so the second caller joins before it settles
        every { mockDeviceCache.getWorkflowsListResponseCache() } returns cachedJson
        every { mockDeviceCache.getWorkflowDetailEnvelopesCache() } returns null

        var completeCount1 = 0
        var completeCount2 = 0
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false) { completeCount1++ }
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false) { completeCount2++ }

        // Both callers are pending; the backend was contacted exactly once (dedup).
        assertThat(completeCount1).isEqualTo(0)
        assertThat(completeCount2).isEqualTo(0)
        verify(exactly = 1) { mockBackend.getWorkflows(any(), any(), type = any(), onSuccess = any(), onError = any()) }

        // Settle the single in-flight request with a SHOULD_NOT_FALLBACK error.
        errorSlot.captured(error, GetWorkflowsErrorHandlingBehavior.SHOULD_NOT_FALLBACK)

        // Every caller must complete exactly once — neither stranded nor double-fired.
        assertThat(completeCount1).isEqualTo(1)
        assertThat(completeCount2).isEqualTo(1)
        // No disk restore should have been attempted.
        verify(exactly = 0) { mockDeviceCache.getWorkflowsListResponseCache() }
        verify(exactly = 0) { mockDeviceCache.getWorkflowDetailEnvelopesCache() }
    }

    // endregion onError envelope restore
}
