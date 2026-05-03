package com.revenuecat.purchases.common.workflows

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.NoOpLogHandler
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.DateProvider
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.utils.WorkflowAssetPreDownloader
import com.revenuecat.purchases.utils.add
import io.mockk.coEvery
import io.mockk.every
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
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.Date
import kotlin.time.Duration.Companion.minutes

@OptIn(InternalRevenueCatAPI::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class WorkflowManagerTest {

    private val initialDate = Date(1685098228L)
    private lateinit var currentDate: Date
    private lateinit var dateProvider: DateProvider

    private val mockBackend: Backend = mockk(relaxed = true)
    private val mockResolver: WorkflowDetailResolver = mockk()
    private val mockAssetPreDownloader: WorkflowAssetPreDownloader = mockk(relaxed = true)
    private lateinit var workflowsCache: WorkflowsCache
    private lateinit var workflowManager: WorkflowManager
    private lateinit var originalLogHandler: LogHandler

    @Before
    fun setUp() {
        originalLogHandler = currentLogHandler
        currentLogHandler = NoOpLogHandler
        currentDate = initialDate
        dateProvider = object : DateProvider {
            override val now: Date
                get() = currentDate
        }
        workflowsCache = WorkflowsCache(dateProvider = dateProvider)
        workflowManager = WorkflowManager(
            backend = mockBackend,
            workflowDetailResolver = mockResolver,
            workflowAssetPreDownloader = mockAssetPreDownloader,
            workflowsCache = workflowsCache,
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

    @Test
    fun `getWorkflow caches result on success`() {
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

        // First call populates the cache.
        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = {},
            onError = { fail("unexpected error $it") },
        )

        // Second call should hit the cache.
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
    fun `getWorkflow re-fetches when cache is stale`() {
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

        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = {},
            onError = { fail("unexpected error $it") },
        )

        currentDate = currentDate.add(6.minutes)

        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = {},
            onError = { fail("unexpected error $it") },
        )

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
    fun `getWorkflow does not cache on backend error`() {
        val expectedError = PurchasesError(PurchasesErrorCode.NetworkError, "network error")
        val errorSlot = slot<(PurchasesError) -> Unit>()
        every {
            mockBackend.getWorkflow(
                appUserID = "user_1",
                workflowId = "wf_1",
                appInBackground = false,
                onSuccess = any(),
                onError = capture(errorSlot),
            )
        } answers {
            errorSlot.captured(expectedError)
        }

        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { fail("expected error") },
            onError = {},
        )

        assertThat(workflowsCache.cachedWorkflow("wf_1")).isNull()
    }

    @Test
    fun `getWorkflow does not cache when resolver throws`() {
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

        workflowManager.getWorkflow(
            appUserID = "user_1",
            workflowId = "wf_1",
            appInBackground = false,
            onSuccess = { fail("expected error") },
            onError = {},
        )

        assertThat(workflowsCache.cachedWorkflow("wf_1")).isNull()
    }
}
