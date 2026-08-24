@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.common.workflows

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.NoOpLogHandler
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.currentLogHandler
import com.revenuecat.purchases.common.uiconfig.UiConfigProvider
import com.revenuecat.purchases.emptyUiConfig
import com.revenuecat.purchases.paywalls.OfferingFontPreDownloader
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.WebViewComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.uiConfigWithFonts
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.PaywallAssetWarming
import com.revenuecat.purchases.utils.collectAssets
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
    private val assetWarming: PaywallAssetWarming = mockk(relaxed = true)
    private val fontPreDownloader: OfferingFontPreDownloader = mockk(relaxed = true)
    private val uiConfig = emptyUiConfig()
    private lateinit var prewarmer: WorkflowAssetPrewarmer

    // Plain JUnit (no Robolectric): swap in a no-op log handler so the failure paths can log without blowing up.
    private val originalLogHandler = currentLogHandler

    @Before
    fun setUp() {
        currentLogHandler = NoOpLogHandler
        coEvery { mockUiConfigProvider.getUiConfig() } returns uiConfig
        every { assetWarming.isAvailable } returns true
        prewarmer = WorkflowAssetPrewarmer(mockUiConfigProvider, assetWarming, fontPreDownloader)
    }

    @After
    fun tearDown() {
        currentLogHandler = originalLogHandler
    }

    // region render path (preDownloadWorkflowAssets)

    @Test
    fun `preDownloadWorkflowAssets skips the component walk but still downloads fonts when disabled`() {
        every { assetWarming.isAvailable } returns false
        val workflow = createWorkflow("wf_1", screens = mapOf("screen_1" to createScreen(emptyComponentsConfig())))

        prewarmer.preDownloadWorkflowAssets(workflow, uiConfig)

        verify(exactly = 0) { assetWarming.warmImages(any()) }
        verify(exactly = 1) { fontPreDownloader.preDownloadFontsIfNeeded(uiConfig.app.fonts.values) }
    }

    @Test
    fun `preDownloadWorkflowAssets downloads screen images and workflow fonts`() {
        val screenConfig = emptyComponentsConfig()
        val font = UiConfig.AppConfig.FontsConfig(
            android = UiConfig.AppConfig.FontsConfig.FontInfo.GoogleFonts("Roboto"),
        )
        val workflow = createWorkflow("wf_1", screens = mapOf("screen_1" to createScreen(screenConfig)))
        val fontsUiConfig = uiConfigWithFonts(mapOf(FontAlias("font_1") to font))

        prewarmer.preDownloadWorkflowAssets(workflow, fontsUiConfig)

        val fontsSlot = slot<Collection<UiConfig.AppConfig.FontsConfig>>()
        verify(exactly = 1) {
            assetWarming.warmImages(screenConfig.collectAssets().imageUris)
            fontPreDownloader.preDownloadFontsIfNeeded(capture(fontsSlot))
        }
        assertThat(fontsSlot.captured).containsExactly(font)
    }

    @Test
    fun `preDownloadWorkflowAssets warms the web_view bundles across a workflow's screens`() {
        val workflow = createWorkflow(
            "wf_1",
            screens = mapOf(
                "screen_1" to createScreen(webViewComponentsConfig("https://a.example.com/i.html")),
                "screen_2" to createScreen(webViewComponentsConfig("https://b.example.com/i.html")),
                "screen_3" to createScreen(emptyComponentsConfig()),
            ),
        )

        prewarmer.preDownloadWorkflowAssets(workflow, emptyUiConfig())

        verify(exactly = 1) {
            assetWarming.warmWebViewUrls(
                setOf("https://a.example.com/i.html", "https://b.example.com/i.html"),
            )
        }
    }

    @Test
    fun `preDownloadWorkflowAssets warms the first page's bundle ahead of later pages`() {
        val workflow = createWorkflow(
            "wf_1",
            screens = linkedMapOf(
                "screen_second" to createScreen(webViewComponentsConfig("https://second.example.com/i.html")),
                "screen_first" to createScreen(webViewComponentsConfig("https://first.example.com/i.html")),
            ),
            steps = mapOf(
                "step_1" to WorkflowStep(
                    id = "step_1",
                    type = "screen",
                    screenId = "screen_first",
                    triggers = listOf(
                        WorkflowTrigger(
                            name = "next",
                            type = WorkflowTriggerType.ON_PRESS,
                            actionId = "action_1",
                            componentId = "component-1",
                        ),
                    ),
                    triggerActions = mapOf("action_1" to WorkflowTriggerAction.Step("step_2")),
                ),
                "step_2" to WorkflowStep(id = "step_2", type = "screen", screenId = "screen_second"),
            ),
        )
        val urls = slot<Collection<String>>()

        prewarmer.preDownloadWorkflowAssets(workflow, emptyUiConfig())

        verify { assetWarming.warmWebViewUrls(capture(urls)) }
        assertThat(urls.captured)
            .containsExactly("https://first.example.com/i.html", "https://second.example.com/i.html")
    }

    @Test
    fun `preDownloadWorkflowAssets finds no web_view bundles when no screen has one`() {
        val workflow = createWorkflow("wf_1", screens = mapOf("screen_1" to createScreen(emptyComponentsConfig())))
        val urls = slot<Collection<String>>()

        prewarmer.preDownloadWorkflowAssets(workflow, emptyUiConfig())

        verify { assetWarming.warmWebViewUrls(capture(urls)) }
        assertThat(urls.captured).isEmpty()
    }

    @Test
    fun `preDownloadWorkflowAssets only downloads each workflow once`() {
        val screenConfig = emptyComponentsConfig()
        val workflow = createWorkflow("wf_1", screens = mapOf("screen_1" to createScreen(screenConfig)))

        prewarmer.preDownloadWorkflowAssets(workflow, uiConfig)
        prewarmer.preDownloadWorkflowAssets(workflow, uiConfig)

        verify(exactly = 1) { assetWarming.warmImages(screenConfig.collectAssets().imageUris) }
    }

    @Test
    fun `preDownloadWorkflowAssets warms every screen's images in a single call`() {
        val workflow = createWorkflow(
            "wf_1",
            screens = mapOf(
                "screen_1" to createScreen(emptyComponentsConfig()),
                "screen_2" to createScreen(emptyComponentsConfig()),
                "screen_3" to createScreen(emptyComponentsConfig()),
            ),
        )

        prewarmer.preDownloadWorkflowAssets(workflow, uiConfig)

        verify(exactly = 1) { assetWarming.warmImages(any()) }
    }

    // endregion render path

    // region load path (onWorkflowLoaded)

    @Test
    fun `onWorkflowLoaded decodes and prewarms the workflow, resolving ui_config once`() = runTest {
        val screenConfig = emptyComponentsConfig()
        val workflow = createWorkflow("wf_1", screens = mapOf("screen_1" to createScreen(screenConfig)))

        prewarmer.onWorkflowLoaded("wf_1") { workflow }

        verify(exactly = 1) { assetWarming.warmImages(screenConfig.collectAssets().imageUris) }
        verify(exactly = 1) { fontPreDownloader.preDownloadFontsIfNeeded(any()) }
        coVerify(exactly = 1) { mockUiConfigProvider.getUiConfig() }
    }

    @Test
    fun `onWorkflowLoaded dedups by id before decoding on a re-warm`() = runTest {
        var decodeCount = 0
        val decode: suspend (String) -> PublishedWorkflow? = { id -> decodeCount++; createWorkflow(id) }

        prewarmer.onWorkflowLoaded("wf_1", decode)
        prewarmer.onWorkflowLoaded("wf_1", decode)

        // Second warm never decodes wf_1 again (dedup happens before the transient decode).
        assertThat(decodeCount).isEqualTo(1)
        verify(exactly = 1) { fontPreDownloader.preDownloadFontsIfNeeded(any()) }
    }

    @Test
    fun `onWorkflowLoaded skips when ui_config is unavailable, then retries on the next warm`() = runTest {
        coEvery { mockUiConfigProvider.getUiConfig() } returns null

        prewarmer.onWorkflowLoaded("wf_1") { id -> createWorkflow(id) }
        verify(exactly = 0) { fontPreDownloader.preDownloadFontsIfNeeded(any()) }

        // ui_config now available: the workflow was not marked warmed, so it is retried.
        coEvery { mockUiConfigProvider.getUiConfig() } returns uiConfig
        prewarmer.onWorkflowLoaded("wf_1") { id -> createWorkflow(id) }
        verify(exactly = 1) { fontPreDownloader.preDownloadFontsIfNeeded(any()) }
    }

    @Test
    fun `onWorkflowLoaded skips a workflow that fails to decode without marking it warmed`() = runTest {
        // First warm: decode returns null (bytes missing / parse fail) -> skipped, not marked.
        prewarmer.onWorkflowLoaded("wf_1") { null }
        verify(exactly = 0) { fontPreDownloader.preDownloadFontsIfNeeded(any()) }

        // Second warm: decode now succeeds -> warmed (proves the failed attempt did not mark it).
        prewarmer.onWorkflowLoaded("wf_1") { id -> createWorkflow(id) }
        verify(exactly = 1) { fontPreDownloader.preDownloadFontsIfNeeded(any()) }
    }

    // endregion load path

    @Test
    fun `does not re-decode a workflow already warmed by the render path`() = runTest {
        // Render path warms wf_1 first...
        prewarmer.preDownloadWorkflowAssets(createWorkflow("wf_1"), uiConfig)

        // ...so the load path skips it before decoding — the concrete win of the shared dedup set.
        var decodeCount = 0
        prewarmer.onWorkflowLoaded("wf_1") { id -> decodeCount++; createWorkflow(id) }

        assertThat(decodeCount).isEqualTo(0)
    }

    private fun createWorkflow(
        id: String,
        screens: Map<String, WorkflowScreen> = emptyMap(),
        steps: Map<String, WorkflowStep> = emptyMap(),
    ): PublishedWorkflow =
        PublishedWorkflow(
            id = id,
            displayName = "Workflow $id",
            initialStepId = "step_1",
            steps = steps,
            screens = screens,
        )

    private fun emptyComponentsConfig(): PaywallComponentsConfig =
        PaywallComponentsConfig(
            stack = StackComponent(components = emptyList()),
            background = Background.Color(ColorScheme(light = ColorInfo.Alias(ColorAlias("")))),
            stickyFooter = null,
        )

    private fun webViewComponentsConfig(url: String): PaywallComponentsConfig =
        PaywallComponentsConfig(
            stack = StackComponent(
                components = listOf(
                    WebViewComponent(
                        url = url,
                        id = "component-1",
                        protocolVersion = WebViewComponent.SUPPORTED_PROTOCOL_VERSION,
                        size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fill),
                    ),
                ),
            ),
            background = Background.Color(ColorScheme(light = ColorInfo.Alias(ColorAlias("")))),
            stickyFooter = null,
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
