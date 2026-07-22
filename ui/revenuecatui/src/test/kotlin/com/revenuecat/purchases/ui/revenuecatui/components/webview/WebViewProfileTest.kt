package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class WebViewProfileTest {

    private val webView = mockk<WebView>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(WebViewFeature::class, WebViewCompat::class, ProfileStore::class)
        every { ProfileStore.getInstance() } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `assigns the dedicated profile when multi-profile is supported`() {
        every { WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) } returns true

        webView.applyPaywallProfile()

        verify { WebViewCompat.setProfile(webView, PAYWALL_PROFILE_NAME) }
    }

    @Test
    fun `keeps the default profile when multi-profile is unsupported`() {
        every { WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) } returns false

        webView.applyPaywallProfile()

        verify(exactly = 0) { WebViewCompat.setProfile(any(), any()) }
    }

    @Test
    fun `does not fail the render when profile setup throws`() {
        val profileStore = mockk<ProfileStore>()
        every { ProfileStore.getInstance() } returns profileStore
        every { WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) } returns true
        every { profileStore.getOrCreateProfile(any()) } throws IllegalArgumentException("invalid profile name")

        webView.applyPaywallProfile()

        verify { profileStore.getOrCreateProfile(PAYWALL_PROFILE_NAME) }
    }
}
