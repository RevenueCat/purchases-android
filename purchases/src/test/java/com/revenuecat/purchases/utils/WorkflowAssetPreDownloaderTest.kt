package com.revenuecat.purchases.utils

import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.NoOpLogHandler
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.common.workflows.WorkflowScreen
import com.revenuecat.purchases.paywalls.OfferingFontPreDownloader
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.URL

class WorkflowAssetPreDownloaderTest {

    private lateinit var imagePreDownloader: PaywallComponentsImagePreDownloader
    private lateinit var fontPreDownloader: OfferingFontPreDownloader
    private lateinit var preDownloader: WorkflowAssetPreDownloader
    private lateinit var previousLogHandler: LogHandler

    @Before
    fun setUp() {
        previousLogHandler = currentLogHandler
        currentLogHandler = NoOpLogHandler
        imagePreDownloader = mockk(relaxed = true)
        fontPreDownloader = mockk(relaxed = true)
        preDownloader = WorkflowAssetPreDownloader(
            paywallComponentsImagePreDownloader = imagePreDownloader,
            offeringFontPreDownloader = fontPreDownloader,
        )
    }

    @After
    fun tearDown() {
        currentLogHandler = previousLogHandler
    }

    @Test
    fun `preDownloadWorkflowAssets downloads screen images and workflow fonts`() {
        val screenConfig = mockk<PaywallComponentsConfig>()
        val localizations = mapOf(
            LocaleId("en_US") to mapOf(
                LocalizationKey("title") to LocalizationData.Text("Title"),
            ),
        )
        val font = UiConfig.AppConfig.FontsConfig(
            android = UiConfig.AppConfig.FontsConfig.FontInfo.GoogleFonts("Roboto"),
        )
        val workflow = createWorkflow(
            screens = mapOf(
                "screen_1" to createScreen(screenConfig, localizations),
            ),
            uiConfig = UiConfig(
                app = UiConfig.AppConfig(
                    fonts = mapOf(FontAlias("font_1") to font),
                ),
            ),
        )

        preDownloader.preDownloadWorkflowAssets(workflow)

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
        val workflow = createWorkflow(
            screens = mapOf("screen_1" to createScreen(screenConfig)),
        )

        preDownloader.preDownloadWorkflowAssets(workflow)
        preDownloader.preDownloadWorkflowAssets(workflow)

        val fontsSlot = slot<Collection<UiConfig.AppConfig.FontsConfig>>()
        verify(exactly = 1) {
            imagePreDownloader.preDownloadImages(screenConfig)
            fontPreDownloader.preDownloadFontsIfNeeded(capture(fontsSlot))
        }
        assertThat(fontsSlot.captured).isEmpty()
    }

    private fun createWorkflow(
        screens: Map<String, WorkflowScreen>,
        uiConfig: UiConfig = UiConfig(),
    ): PublishedWorkflow {
        return PublishedWorkflow(
            id = "workflow_1",
            displayName = "Workflow",
            initialStepId = "step_1",
            steps = emptyMap(),
            screens = screens,
            uiConfig = uiConfig,
        )
    }

    private fun createScreen(
        paywallComponentsConfig: PaywallComponentsConfig,
        localizations: Map<LocaleId, Map<LocalizationKey, LocalizationData>> = emptyMap(),
    ): WorkflowScreen {
        return WorkflowScreen(
            templateName = "template",
            assetBaseURL = URL("https://assets.revenuecat.com"),
            componentsConfig = ComponentsConfig(paywallComponentsConfig),
            componentsLocalizations = localizations,
            defaultLocaleIdentifier = LocaleId("en_US"),
        )
    }

}
