package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.webkit.Profile
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.WebViewOutcomeReceiver
import androidx.webkit.WebViewStartUpConfig
import androidx.webkit.WebViewStartUpResult
import androidx.webkit.WebViewStartupException
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException

private typealias StartUpReceiver = WebViewOutcomeReceiver<WebViewStartUpResult, WebViewStartupException>

@RunWith(AndroidJUnit4::class)
internal class PaywallWebViewStartUpTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val directExecutor = Executor { runnable -> runnable.run() }

    private fun startUp() = PaywallWebViewStartUp.startUp(context, directExecutor)

    @Before
    fun setUp() {
        mockkStatic(WebViewCompat::class)
        every { WebViewCompat.startUpWebView(any(), any(), any<StartUpReceiver>()) } returns Unit
    }

    @After
    fun tearDown() {
        PaywallWebViewStartUp.resetForTesting()
        unmockkAll()
    }

    @Test
    fun `loads only the profile the component will actually use`() {
        val config = slot<WebViewStartUpConfig>()
        every { WebViewCompat.startUpWebView(any(), capture(config), any<StartUpReceiver>()) } returns Unit
        mockkStatic(WebViewFeature::class)
        every { WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) } returns true

        startUp()

        assertThat(config.captured.profilesToLoadDuringStartup).containsExactly(PAYWALL_PROFILE_NAME)
    }

    @Test
    fun `falls back to the default profile when MULTI_PROFILE is unsupported`() {
        val config = slot<WebViewStartUpConfig>()
        every { WebViewCompat.startUpWebView(any(), capture(config), any<StartUpReceiver>()) } returns Unit
        mockkStatic(WebViewFeature::class)
        every { WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) } returns false

        startUp()

        assertThat(config.captured.profilesToLoadDuringStartup).containsExactly(Profile.DEFAULT_PROFILE_NAME)
    }

    @Test
    fun `touches no webkit API on the calling thread`() {
        mockkStatic(WebViewFeature::class)
        every { WebViewFeature.isFeatureSupported(any()) } returns true
        val deferred = mutableListOf<Runnable>()

        PaywallWebViewStartUp.startUp(context) { runnable -> deferred.add(runnable) }

        verify(exactly = 0) { WebViewFeature.isFeatureSupported(any()) }
        verify(exactly = 0) { WebViewCompat.startUpWebView(any(), any(), any<StartUpReceiver>()) }
        assertThat(deferred).hasSize(1)
    }

    @Test
    fun `triggers startUpWebView on the first call`() {
        startUp()

        verify(exactly = 1) { WebViewCompat.startUpWebView(any(), any(), any<StartUpReceiver>()) }
    }

    @Test
    fun `is a no-op on a second call after a successful startup`() {
        val receiver = slot<StartUpReceiver>()
        every { WebViewCompat.startUpWebView(any(), any(), capture(receiver)) } returns Unit

        startUp()
        receiver.captured.onResult(mockk(relaxed = true))
        startUp()

        verify(exactly = 1) { WebViewCompat.startUpWebView(any(), any(), any<StartUpReceiver>()) }
    }

    // androidx: "If a failure has been reported to the callback, calling any other WebView APIs is likely to
    // throw an exception or immediately crash."
    @Test
    fun `does not retry after the outcome receiver reports an error`() {
        val receiver = slot<StartUpReceiver>()
        every { WebViewCompat.startUpWebView(any(), any(), capture(receiver)) } returns Unit

        startUp()
        receiver.captured.onError(mockk(relaxed = true))
        startUp()

        verify(exactly = 1) { WebViewCompat.startUpWebView(any(), any(), any<StartUpReceiver>()) }
    }

    @Test
    fun `the background executor swallows a failure from the task it runs`() {
        val executor = PaywallWebViewStartUp.guarded { runnable -> runnable.run() }

        executor.execute { throw RuntimeException("missing WebView package") }
    }

    @Test
    fun `the background executor swallows an Error from the task it runs`() {
        val executor = PaywallWebViewStartUp.guarded { runnable -> runnable.run() }

        executor.execute { throw NoClassDefFoundError("org.chromium.WebViewChromiumFactoryProvider") }
        executor.execute { throw UnsatisfiedLinkError("libwebviewchromium.so") }
    }

    @Test
    fun `allows a retry when the submission itself is rejected`() {
        every {
            WebViewCompat.startUpWebView(any(), any(), any<StartUpReceiver>())
        } throws RejectedExecutionException("rejected")

        startUp()

        every { WebViewCompat.startUpWebView(any(), any(), any<StartUpReceiver>()) } returns Unit
        startUp()

        verify(exactly = 2) { WebViewCompat.startUpWebView(any(), any(), any<StartUpReceiver>()) }
    }
}
