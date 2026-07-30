@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.NoOpLogHandler
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.uiconfig.UiConfigProvider
import com.revenuecat.purchases.emptyUiConfig
import com.revenuecat.purchases.paywalls.OfferingFontPreDownloader
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.uiConfigWithFonts
import com.revenuecat.purchases.utils.PaywallComponentsImagePreDownloader
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.URL

class WorkflowAssetPrewarmerTest {

    private val mockUiConfigProvider: UiConfigProvider = mockk()
    private val imagePreDownloader: PaywallComponentsImagePreDownloader = mockk(relaxed = true)
    private val fontPreDownloader: OfferingFontPreDownloader = mockk(relaxed = true)
    private val uiConfig = emptyUiConfig()
    private lateinit var prewarmer: WorkflowAssetPrewarmer

    // Plain JUnit (no Robolectric): swap in a no-op log handler so the failure paths can log without blowing up.
    private val originalLogHandler = currentLogHandler

    @Before
    fun setUp() {
        currentLogHandler = NoOpLogHandler
        coEvery { mockUiConfigProvider.getUiConfig() } returns uiConfig
        prewarmer = WorkflowAssetPrewarmer(mockUiConfigProvider, imagePreDownloader, fontPreDownloader)
    }

    @After
    fun tearDown() {
        currentLogHandler = originalLogHandler
    }

    // region render path (preDownloadWorkflowAssets)

    @Test
    fun `preDownloadWorkflowAssets downloads screen images and workflow fonts`() {
        val screenConfig = mockk<PaywallComponentsConfig>()
        val font = UiConfig.AppConfig.FontsConfig(
            android = UiConfig.AppConfig.FontsConfig.FontInfo.GoogleFonts("Roboto"),
        )
        val workflow = createWorkflow("wf_1", screens = mapOf("screen_1" to createScreen(screenConfig)))
        val fontsUiConfig = uiConfigWithFonts(mapOf(FontAlias("font_1") to font))

        prewarmer.preDownloadWorkflowAssets(workflow, fontsUiConfig)

        val fontsSlot = slot<Collection<UiConfig.AppConfig.FontsConfig>>()
        verify(exactly = 1) {
            imagePreDownloader.preDownloadImages(screenConfig)
            fontPreDownloader.preDownloadFontsIfNeeded(capture(fontsSlot))
        }
        assertThat(fontsSlot.captured).containsExactly(font)
    }

    @Test
    fun `preDownloadWorkflowAssets only downloads each workflow once`() {
        val screenConfig = mockk<PaywallComponentsConfig>()
        val workflow = createWorkflow("wf_1", screens = mapOf("screen_1" to createScreen(screenConfig)))

        prewarmer.preDownloadWorkflowAssets(workflow, uiConfig)
        prewarmer.preDownloadWorkflowAssets(workflow, uiConfig)

        verify(exactly = 1) { imagePreDownloader.preDownloadImages(screenConfig) }
    }

    // endregion render path

    // region load path (onCurrentWorkflowLoaded)

    @Test
    fun `onCurrentWorkflowLoaded decodes and prewarms the workflow, resolving ui_config once`() = runTest {
        val screenConfig = mockk<PaywallComponentsConfig>()
        val workflow = createWorkflow("wf_1", screens = mapOf("screen_1" to createScreen(screenConfig)))

        prewarmer.onCurrentWorkflowLoaded("wf_1") { workflow }

        verify(exactly = 1) { imagePreDownloader.preDownloadImages(screenConfig) }
        verify(exactly = 1) { fontPreDownloader.preDownloadFontsIfNeeded(any()) }
        coVerify(exactly = 1) { mockUiConfigProvider.getUiConfig() }
    }

    @Test
    fun `onCurrentWorkflowLoaded dedups by id before decoding on a re-warm`() = runTest {
        var decodeCount = 0
        val decode: suspend (String) -> PublishedWorkflow? = { id -> decodeCount++; createWorkflow(id) }

        prewarmer.onCurrentWorkflowLoaded("wf_1", decode)
        prewarmer.onCurrentWorkflowLoaded("wf_1", decode)

        // Second warm never decodes wf_1 again (dedup happens before the transient decode).
        assertThat(decodeCount).isEqualTo(1)
        verify(exactly = 1) { fontPreDownloader.preDownloadFontsIfNeeded(any()) }
    }

    @Test
    fun `onCurrentWorkflowLoaded skips when ui_config is unavailable, then retries on the next warm`() = runTest {
        coEvery { mockUiConfigProvider.getUiConfig() } returns null

        prewarmer.onCurrentWorkflowLoaded("wf_1") { id -> createWorkflow(id) }
        verify(exactly = 0) { fontPreDownloader.preDownloadFontsIfNeeded(any()) }

        // ui_config now available: the workflow was not marked warmed, so it is retried.
        coEvery { mockUiConfigProvider.getUiConfig() } returns uiConfig
        prewarmer.onCurrentWorkflowLoaded("wf_1") { id -> createWorkflow(id) }
        verify(exactly = 1) { fontPreDownloader.preDownloadFontsIfNeeded(any()) }
    }

    @Test
    fun `onCurrentWorkflowLoaded skips a workflow that fails to decode without marking it warmed`() = runTest {
        // First warm: decode returns null (bytes missing / parse fail) -> skipped, not marked.
        prewarmer.onCurrentWorkflowLoaded("wf_1") { null }
        verify(exactly = 0) { fontPreDownloader.preDownloadFontsIfNeeded(any()) }

        // Second warm: decode now succeeds -> warmed (proves the failed attempt did not mark it).
        prewarmer.onCurrentWorkflowLoaded("wf_1") { id -> createWorkflow(id) }
        verify(exactly = 1) { fontPreDownloader.preDownloadFontsIfNeeded(any()) }
    }

    // endregion load path

    @Test
    fun `does not re-decode a workflow already warmed by the render path`() = runTest {
        // Render path warms wf_1 first...
        prewarmer.preDownloadWorkflowAssets(createWorkflow("wf_1"), uiConfig)

        // ...so the load path skips it before decoding — the concrete win of the shared dedup set.
        var decodeCount = 0
        prewarmer.onCurrentWorkflowLoaded("wf_1") { id -> decodeCount++; createWorkflow(id) }

        assertThat(decodeCount).isEqualTo(0)
    }

    private fun createWorkflow(id: String, screens: Map<String, WorkflowScreen> = emptyMap()): PublishedWorkflow =
        PublishedWorkflow(
            id = id,
            displayName = "Workflow $id",
            initialStepId = "step_1",
            steps = emptyMap(),
            screens = screens,
        )

    private fun createScreen(paywallComponentsConfig: PaywallComponentsConfig): WorkflowScreen =
        WorkflowScreen(
            templateName = "template",
            assetBaseURL = URL("https://assets.revenuecat.com"),
            componentsConfig = ComponentsConfig(paywallComponentsConfig),
            componentsLocalizations = emptyMap<LocaleId, Map<LocalizationKey, LocalizationData>>(),
            defaultLocaleIdentifier = LocaleId("en_US"),
        )
}
