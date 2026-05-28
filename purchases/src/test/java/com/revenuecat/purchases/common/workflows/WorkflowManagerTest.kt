package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.NoOpLogHandler
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.utils.WorkflowAssetPreDownloader
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
    private lateinit var workflowManager: WorkflowManager
    private lateinit var originalLogHandler: LogHandler

    @Before
    fun setUp() {
        originalLogHandler = currentLogHandler
        currentLogHandler = NoOpLogHandler
        every { mockDateProvider.now } returns Date(0) // ensures cache is always stale by default
        workflowManager = WorkflowManager(
            backend = mockBackend,
            workflowDetailResolver = mockResolver,
            workflowAssetPreDownloader = mockAssetPreDownloader,
            deviceCache = mockDeviceCache,
            dateProvider = mockDateProvider,
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
        val errorSlot = slot<(PurchasesError) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_missing",
                appInBackground = false,
                onSuccess = any(),
                onError = capture(errorSlot),
            )
        } answers {
            errorSlot.captured(expectedError)
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
                onSuccess = capture(successSlot),
                onError = any(),
            )
        } answers { successSlot.captured(response) }

        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        verify(exactly = 1) {
            mockBackend.getWorkflows(appUserID = "user_1", appInBackground = false, onSuccess = any(), onError = any())
        }
        verify(exactly = 1) { mockDeviceCache.cacheWorkflowsListResponse(any()) }
        verify(exactly = 0) { mockBackend.getWorkflow(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `getWorkflowsList skips network call when in-memory cache is fresh`() {
        val response = WorkflowsListResponse(workflows = emptyList())
        val successSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), onSuccess = capture(successSlot), onError = any())
        } answers { successSlot.captured(response) }
        // First call at t=0
        every { mockDateProvider.now } returns Date(0)
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        // Second call at t=1ms — still fresh (threshold is 5 minutes)
        every { mockDateProvider.now } returns Date(1)
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        verify(exactly = 1) { mockBackend.getWorkflows(any(), any(), onSuccess = any(), onError = any()) }
    }

    @Test
    fun `getWorkflowsList triggers getWorkflow for each prefetch=true entry only`() {
        val response = WorkflowsListResponse(
            workflows = listOf(
                WorkflowSummary(id = "wf_prefetch", displayName = "A", prefetch = true),
                WorkflowSummary(id = "wf_skip", displayName = "B", prefetch = false),
                WorkflowSummary(id = "wf_also_prefetch", displayName = "C", prefetch = true),
            ),
        )
        val successSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), onSuccess = capture(successSlot), onError = any())
        } answers { successSlot.captured(response) }

        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        verify(exactly = 1) {
            mockBackend.getWorkflow(appUserID = "user_1", workflowId = "wf_prefetch", any(), any(), any())
        }
        verify(exactly = 0) {
            mockBackend.getWorkflow(appUserID = "user_1", workflowId = "wf_skip", any(), any(), any())
        }
        verify(exactly = 1) {
            mockBackend.getWorkflow(appUserID = "user_1", workflowId = "wf_also_prefetch", any(), any(), any())
        }
    }

    @Test
    fun `getWorkflowsList silently logs error on backend failure`() {
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "network error")
        val errorSlot = slot<(PurchasesError) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), onSuccess = any(), onError = capture(errorSlot))
        } answers { errorSlot.captured(error) }

        // Should not throw
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        verify(exactly = 0) { mockDeviceCache.cacheWorkflowsListResponse(any()) }
    }

    @Test
    fun `getWorkflowsList restores offeringId map from disk cache on backend failure`() {
        val cachedJson = """{"workflows":[{"id":"wf_1","display_name":"Flow","offering_id":"default","prefetch":false}]}"""
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "network error")
        val errorSlot = slot<(PurchasesError) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), onSuccess = any(), onError = capture(errorSlot))
        } answers { errorSlot.captured(error) }
        every { mockDeviceCache.getWorkflowsListResponseCache() } returns cachedJson

        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        assertThat(workflowManager.workflowIdForOfferingId("default")).isEqualTo("wf_1")
    }

    @Test
    fun `getWorkflowsList silently ignores corrupt disk cache on backend failure`() {
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "network error")
        val errorSlot = slot<(PurchasesError) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), onSuccess = any(), onError = capture(errorSlot))
        } answers { errorSlot.captured(error) }
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
            mockBackend.getWorkflows(any(), any(), onSuccess = capture(successSlot), onError = any())
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
            mockBackend.getWorkflows(any(), any(), onSuccess = capture(successSlot), onError = any())
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
        val errorSlot = slot<(PurchasesError) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), onSuccess = any(), onError = capture(errorSlot))
        } answers { errorSlot.captured(error) }
        every { mockDeviceCache.getWorkflowsListResponseCache() } returns cachedJson

        // First call fails → restores from disk
        every { mockDateProvider.now } returns Date(0)
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)
        assertThat(workflowManager.workflowIdForOfferingId("default")).isEqualTo("wf_1")

        // Second call immediately after — in-memory cache should be considered fresh (t=1ms)
        every { mockDateProvider.now } returns Date(1)
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        // Only one backend call total (the first one that failed)
        verify(exactly = 1) { mockBackend.getWorkflows(any(), any(), onSuccess = any(), onError = any()) }
    }

    // Test A (continued) — re-fetch fires again after TTL expires
    @Test
    fun `getWorkflowsList re-fetches after TTL expiry following disk-cache restore`() {
        val cachedJson = """{"workflows":[{"id":"wf_1","display_name":"Flow","offering_id":"default","prefetch":false}]}"""
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "network error")
        val errorSlot = slot<(PurchasesError) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), onSuccess = any(), onError = capture(errorSlot))
        } answers { errorSlot.captured(error) }
        every { mockDeviceCache.getWorkflowsListResponseCache() } returns cachedJson

        // First call at t=0 — fails, restores from disk, stamps in-memory cache at t=0
        every { mockDateProvider.now } returns Date(0)
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        // Second call well past TTL (6 minutes = 360_000ms) — should fire a new network request
        val sixMinutesMs = 6L * 60 * 1000
        every { mockDateProvider.now } returns Date(sixMinutesMs)
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        // Two backend calls total: first failure + re-fetch after TTL
        verify(exactly = 2) { mockBackend.getWorkflows(any(), any(), onSuccess = any(), onError = any()) }
    }

    // Test B — concurrent calls only fire one network request
    @Test
    fun `getWorkflowsList concurrent calls only fire one network request`() {
        // Hold the backend call without resolving it so the first call stays in-flight
        val successSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), onSuccess = capture(successSlot), onError = any())
        } answers { /* intentionally do not call successSlot to simulate in-flight */ }

        every { mockDateProvider.now } returns Date(0)
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)
        // Second call while first is still in-flight
        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        verify(exactly = 1) { mockBackend.getWorkflows(any(), any(), onSuccess = any(), onError = any()) }
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
            mockBackend.getWorkflows(any(), any(), onSuccess = capture(successSlot), onError = any())
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
        val errorSlot = slot<(PurchasesError) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), onSuccess = any(), onError = capture(errorSlot))
        } answers { errorSlot.captured(error) }
        every { mockDeviceCache.getWorkflowsListResponseCache() } returns cachedJson

        workflowManager.getWorkflowsList(appUserID = "user_1", appInBackground = false)

        assertThat(workflowManager.workflowIdForOfferingId("shared")).isEqualTo("wf_last")
    }

    // Test D — BuildConfig.USE_WORKFLOWS_ENDPOINT is compile-time true in the test variant;
    // testing the false branch requires making it injectable. Tracked as a future improvement.

    // endregion issue fixes

    // region onComplete callback

    @Test
    fun `getWorkflowsList calls onComplete after backend success with no prefetch workflows`() {
        val response = WorkflowsListResponse(workflows = listOf(
            WorkflowSummary(id = "wf_1", displayName = "Flow", offeringId = "default", prefetch = false),
        ))
        val successSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), onSuccess = capture(successSlot), onError = any())
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
            mockBackend.getWorkflows(any(), any(), onSuccess = capture(successSlot), onError = any())
        } answers { successSlot.captured(response) }

        every { mockDateProvider.now } returns Date(0)
        workflowManager.getWorkflowsList("user_1", false)

        every { mockDateProvider.now } returns Date(1) // still within TTL
        var completed = false
        workflowManager.getWorkflowsList("user_1", false) { completed = true }

        assertThat(completed).isTrue()
        verify(exactly = 1) { mockBackend.getWorkflows(any(), any(), onSuccess = any(), onError = any()) }
    }

    @Test
    fun `getWorkflowsList calls onComplete only after all prefetch workflows complete`() {
        val response = WorkflowsListResponse(workflows = listOf(
            WorkflowSummary(id = "wf_a", displayName = "A", prefetch = true),
            WorkflowSummary(id = "wf_b", displayName = "B", prefetch = true),
        ))
        val listSuccessSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), onSuccess = capture(listSuccessSlot), onError = any())
        } answers { listSuccessSlot.captured(response) }

        val detailSuccessA = slot<(WorkflowDetailResponse) -> Unit>()
        val detailSuccessB = slot<(WorkflowDetailResponse) -> Unit>()
        every {
            mockBackend.getWorkflow("user_1", "wf_a", false, capture(detailSuccessA), any())
        } just Runs
        every {
            mockBackend.getWorkflow("user_1", "wf_b", false, capture(detailSuccessB), any())
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
    fun `getWorkflowsList calls onComplete even if a prefetch workflow fails`() {
        val response = WorkflowsListResponse(workflows = listOf(
            WorkflowSummary(id = "wf_a", displayName = "A", prefetch = true),
            WorkflowSummary(id = "wf_b", displayName = "B", prefetch = true),
        ))
        val listSuccessSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), onSuccess = capture(listSuccessSlot), onError = any())
        } answers { listSuccessSlot.captured(response) }

        val detailSuccessA = slot<(WorkflowDetailResponse) -> Unit>()
        val detailErrorB = slot<(PurchasesError) -> Unit>()
        every {
            mockBackend.getWorkflow("user_1", "wf_a", false, capture(detailSuccessA), any())
        } just Runs
        every {
            mockBackend.getWorkflow("user_1", "wf_b", false, any(), capture(detailErrorB))
        } just Runs
        coEvery { mockResolver.resolve(any()) } returns mockk()

        var completed = false
        workflowManager.getWorkflowsList("user_1", false) { completed = true }

        assertThat(completed).isFalse()

        detailSuccessA.captured(WorkflowDetailResponse(action = WorkflowResponseAction.INLINE, data = mockk()))
        assertThat(completed).isFalse()

        detailErrorB.captured(PurchasesError(PurchasesErrorCode.NetworkError, "fail"))
        assertThat(completed).isTrue()
    }

    @Test
    fun `getWorkflowsList calls onComplete after network error`() {
        val error = PurchasesError(PurchasesErrorCode.NetworkError, "fail")
        val errorSlot = slot<(PurchasesError) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), onSuccess = any(), onError = capture(errorSlot))
        } answers { errorSlot.captured(error) }

        var completed = false
        workflowManager.getWorkflowsList("user_1", false) { completed = true }

        assertThat(completed).isTrue()
    }

    @Test
    fun `getWorkflowsList concurrent in-flight calls both receive onComplete`() {
        val listSuccessSlot = slot<(WorkflowsListResponse) -> Unit>()
        every {
            mockBackend.getWorkflows(any(), any(), onSuccess = capture(listSuccessSlot), onError = any())
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
        verify(exactly = 1) { mockBackend.getWorkflows(any(), any(), onSuccess = any(), onError = any()) }
    }

    // endregion onComplete callback
}
