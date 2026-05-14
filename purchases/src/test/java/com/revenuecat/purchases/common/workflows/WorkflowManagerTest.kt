package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.NoOpLogHandler
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.utils.WorkflowAssetPreDownloader
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class WorkflowManagerTest {

    private val mockBackend: Backend = mockk(relaxed = true)
    private val mockResolver: WorkflowDetailResolver = mockk()
    private val mockAssetPreDownloader: WorkflowAssetPreDownloader = mockk(relaxed = true)
    private lateinit var workflowManager: WorkflowManager
    private lateinit var originalLogHandler: LogHandler

    @Before
    fun setUp() {
        originalLogHandler = currentLogHandler
        currentLogHandler = NoOpLogHandler
        workflowManager = WorkflowManager(
            backend = mockBackend,
            workflowDetailResolver = mockResolver,
            workflowAssetPreDownloader = mockAssetPreDownloader,
            scope = CoroutineScope(UnconfinedTestDispatcher()),
        )
        every {
            mockBackend.getWorkflow(any(), any(), any(), any(), any())
        } answers {
            arg<(PurchasesError) -> Unit>(4).invoke(
                PurchasesError(PurchasesErrorCode.UnknownError, "default stub"),
            )
        }
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
    fun `fetchWorkflowsForAllOfferings fetches workflow for each offering identifier`() = runTest {
        var completeCalled = false
        workflowManager.fetchWorkflowsForAllOfferings(
            appUserID = "user1",
            offeringIdentifiers = listOf("offering_a", "offering_b"),
            appInBackground = false,
        ) {
            completeCalled = true
        }
        testScheduler.advanceUntilIdle()
        assertTrue(completeCalled)
        verify { mockBackend.getWorkflow(any(), "offering_a", any(), any(), any()) }
        verify { mockBackend.getWorkflow(any(), "offering_b", any(), any(), any()) }
    }

    @Test
    fun `fetchWorkflowsForAllOfferings calls onComplete even when one fetch fails`() = runTest {
        val goodResponse = WorkflowDetailResponse(
            action = WorkflowResponseAction.INLINE,
            data = mockk(),
        )
        val goodResult = WorkflowDataResult(workflow = goodResponse.data!!, enrolledVariants = null)
        coEvery { mockResolver.resolve(goodResponse) } returns goodResult

        val goodSuccessSlot = slot<(WorkflowDetailResponse) -> Unit>()
        every {
            mockBackend.getWorkflow(any(), "offering_good", any(), onSuccess = capture(goodSuccessSlot), onError = any())
        } answers {
            goodSuccessSlot.captured(goodResponse)
        }

        every {
            mockBackend.getWorkflow(any(), "offering_bad", any(), any(), any())
        } answers {
            arg<(PurchasesError) -> Unit>(4).invoke(
                PurchasesError(PurchasesErrorCode.NetworkError, "fail"),
            )
        }

        var completeCalled = false
        workflowManager.fetchWorkflowsForAllOfferings(
            appUserID = "user1",
            offeringIdentifiers = listOf("offering_good", "offering_bad"),
            appInBackground = false,
        ) {
            completeCalled = true
        }
        testScheduler.advanceUntilIdle()
        assertTrue(completeCalled)
        verify { mockBackend.getWorkflow(any(), "offering_good", any(), any(), any()) }
        verify { mockBackend.getWorkflow(any(), "offering_bad", any(), any(), any()) }
    }

    @Test
    fun `fetchWorkflowsForAllOfferings calls onComplete immediately when list is empty`() = runTest {
        var completeCalled = false
        workflowManager.fetchWorkflowsForAllOfferings(
            appUserID = "user1",
            offeringIdentifiers = emptyList(),
            appInBackground = false,
        ) {
            completeCalled = true
        }
        testScheduler.advanceUntilIdle()
        assertTrue(completeCalled)
        verify(exactly = 0) { mockBackend.getWorkflow(any(), any(), any(), any(), any()) }
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
}
