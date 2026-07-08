package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.uiconfig.UiConfigProvider
import com.revenuecat.purchases.utils.WorkflowAssetPreDownloader
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * [WorkflowManager] is now a thin adapter over [WorkflowsConfigProvider], so these tests exercise the
 * consumer-facing seam (`getWorkflow`/`workflowIdForOfferingId`/`onPaywallConfigReady`) against a mocked
 * provider. The provider's own behavior (topic reads, blob resolution) is covered by
 * [WorkflowsConfigIntegrationTest].
 */
class WorkflowManagerTest {

    private val mockProvider: WorkflowsConfigProvider = mockk()
    private val mockUiConfigProvider: UiConfigProvider = mockk()
    private val mockAssetPreDownloader: WorkflowAssetPreDownloader = mockk()
    private val uiConfig = UiConfig()
    private val testScope = TestScope(UnconfinedTestDispatcher())
    private lateinit var workflowManager: WorkflowManager

    // This is a plain JUnit test (no Robolectric), so the default log handler's android.util.Log calls aren't
    // mocked. Swap in a no-op handler so the prewarm-failure path can log without blowing up.
    private val originalLogHandler = currentLogHandler

    @Before
    fun setUp() {
        currentLogHandler = object : LogHandler {
            override fun v(tag: String, msg: String) {}
            override fun d(tag: String, msg: String) {}
            override fun i(tag: String, msg: String) {}
            override fun w(tag: String, msg: String) {}
            override fun e(tag: String, msg: String, throwable: Throwable?) {}
        }
        coEvery { mockUiConfigProvider.getUiConfig() } returns uiConfig
        every { mockAssetPreDownloader.preDownloadWorkflowAssets(any(), any()) } just Runs
        workflowManager = WorkflowManager(
            mockProvider,
            mockUiConfigProvider,
            mockAssetPreDownloader,
            scope = testScope,
        )
    }

    @After
    fun tearDown() {
        currentLogHandler = originalLogHandler
    }

    @Test
    fun `getWorkflow resolves by workflow id when the provider has no offering mapping`() {
        val expectedResult = mockk<PublishedWorkflow>(relaxed = true)
        coEvery { mockProvider.workflowIdForOfferingId("wf_1") } returns null
        coEvery { mockProvider.getWorkflow("wf_1") } returns expectedResult

        var result: PublishedWorkflow? = null
        workflowManager.getWorkflow(
            workflowOrOfferingId = "wf_1",
            onSuccess = { result = it },
            onError = { fail("unexpected error $it") },
        )

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `getWorkflow resolves an offering id to its workflow id before fetching`() {
        val offeringId = "my-offering"
        val workflowId = "wfl-real-id"
        val expectedResult = mockk<PublishedWorkflow>(relaxed = true)
        coEvery { mockProvider.workflowIdForOfferingId(offeringId) } returns workflowId
        coEvery { mockProvider.getWorkflow(workflowId) } returns expectedResult

        var result: PublishedWorkflow? = null
        workflowManager.getWorkflow(
            workflowOrOfferingId = offeringId,
            onSuccess = { result = it },
            onError = { fail("unexpected error $it") },
        )

        assertThat(result).isEqualTo(expectedResult)
        coVerify(exactly = 0) { mockProvider.getWorkflow(offeringId) }
    }

    @Test
    fun `getWorkflow calls onError when the provider cannot resolve the workflow`() {
        coEvery { mockProvider.workflowIdForOfferingId("wf_missing") } returns null
        coEvery { mockProvider.getWorkflow("wf_missing") } returns null

        var error: PurchasesError? = null
        workflowManager.getWorkflow(
            workflowOrOfferingId = "wf_missing",
            onSuccess = { fail("expected error") },
            onError = { error = it },
        )

        assertThat(error).isNotNull
        assertThat(error!!.code).isEqualTo(PurchasesErrorCode.UnknownError)
    }

    @Test
    fun `getWorkflow pre-downloads the workflow's assets with the ui_config on success`() {
        val expectedResult = mockk<PublishedWorkflow>(relaxed = true)
        coEvery { mockProvider.workflowIdForOfferingId("wf_1") } returns null
        coEvery { mockProvider.getWorkflow("wf_1") } returns expectedResult

        workflowManager.getWorkflow(
            workflowOrOfferingId = "wf_1",
            onSuccess = {},
            onError = { fail("unexpected error $it") },
        )
        testScope.testScheduler.advanceUntilIdle()

        verify(exactly = 1) {
            mockAssetPreDownloader.preDownloadWorkflowAssets(expectedResult, uiConfig)
        }
    }

    @Test
    fun `getWorkflow does not pre-download assets when the workflow cannot be resolved`() {
        coEvery { mockProvider.workflowIdForOfferingId("wf_missing") } returns null
        coEvery { mockProvider.getWorkflow("wf_missing") } returns null

        workflowManager.getWorkflow(
            workflowOrOfferingId = "wf_missing",
            onSuccess = { fail("expected error") },
            onError = {},
        )
        testScope.testScheduler.advanceUntilIdle()

        verify(exactly = 0) { mockAssetPreDownloader.preDownloadWorkflowAssets(any(), any()) }
    }

    @Test
    fun `getWorkflow still delivers the workflow when pre-downloading assets fails`() {
        val expectedResult = mockk<PublishedWorkflow>(relaxed = true)
        coEvery { mockProvider.workflowIdForOfferingId("wf_1") } returns null
        coEvery { mockProvider.getWorkflow("wf_1") } returns expectedResult
        every { mockAssetPreDownloader.preDownloadWorkflowAssets(any(), any()) } throws RuntimeException("boom")

        var result: PublishedWorkflow? = null
        workflowManager.getWorkflow(
            workflowOrOfferingId = "wf_1",
            onSuccess = { result = it },
            onError = { fail("unexpected error $it") },
        )
        testScope.testScheduler.advanceUntilIdle()

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `workflowIdForOfferingId delegates to the provider`() = runTest {
        coEvery { mockProvider.workflowIdForOfferingId("off_1") } returns "wf_1"

        val result = workflowManager.workflowIdForOfferingId("off_1")

        assertThat(result).isEqualTo("wf_1")
    }

    @Test
    fun `onPaywallConfigReady invokes onComplete after the workflows topic syncs and ui_config resolves`() {
        val mockProvider = mockk<WorkflowsConfigProvider>()
        coEvery { mockProvider.awaitReady() } just Runs
        val manager = WorkflowManager(mockProvider, mockUiConfigProvider, mockAssetPreDownloader, scope = testScope)

        var completed = false
        manager.onPaywallConfigReady { completed = true }
        testScope.testScheduler.advanceUntilIdle()

        assertThat(completed).isTrue()
        coVerify(exactly = 1) { mockProvider.awaitReady() }
        coVerify(exactly = 1) { mockUiConfigProvider.getUiConfig() }
    }

    @Test
    fun `onPaywallConfigReady still completes when ui_config resolution fails`() {
        val mockProvider = mockk<WorkflowsConfigProvider>()
        coEvery { mockProvider.awaitReady() } just Runs
        coEvery { mockUiConfigProvider.getUiConfig() } throws RuntimeException("boom")
        val manager = WorkflowManager(mockProvider, mockUiConfigProvider, mockAssetPreDownloader, scope = testScope)

        var completed = false
        manager.onPaywallConfigReady { completed = true }
        testScope.testScheduler.advanceUntilIdle()

        assertThat(completed).isTrue()
    }

    @Test
    fun `onPaywallConfigReady still completes when the workflows sync fails`() {
        val mockProvider = mockk<WorkflowsConfigProvider>()
        coEvery { mockProvider.awaitReady() } throws RuntimeException("boom")
        val manager = WorkflowManager(mockProvider, mockUiConfigProvider, mockAssetPreDownloader, scope = testScope)

        var completed = false
        manager.onPaywallConfigReady { completed = true }
        testScope.testScheduler.advanceUntilIdle()

        assertThat(completed).isTrue()
        // A failure to ready one step must not cancel the other.
        coVerify(exactly = 1) { mockUiConfigProvider.getUiConfig() }
    }
}
