package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.webkit.Profile
import androidx.webkit.ProfileStore
import androidx.webkit.WebStorageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class WebViewProfileTest {

    private val webView = mockk<WebView>(relaxed = true)
    private val cookieManager = mockk<CookieManager>(relaxed = true)
    private val webStorage = mockk<WebStorage>(relaxed = true)
    private val profile = mockk<Profile>(relaxed = true)
    private val profileStore = mockk<ProfileStore>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(WebViewFeature::class, WebViewCompat::class, ProfileStore::class, WebStorageCompat::class)
        every { ProfileStore.getInstance() } returns profileStore
        every { profileStore.getProfile(PAYWALL_PROFILE_NAME) } returns profile
        every { profile.cookieManager } returns cookieManager
        every { profile.webStorage } returns webStorage
        every { WebStorageCompat.deleteBrowsingData(any(), any<Runnable>()) } just Runs
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
    fun `clears the profile's browsing data when deleting browsing data is supported`() {
        every { WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) } returns true
        every { WebViewFeature.isFeatureSupported(WebViewFeature.DELETE_BROWSING_DATA) } returns true

        clearPaywallProfileStorage()

        verify { WebStorageCompat.deleteBrowsingData(webStorage, any<Runnable>()) }
    }

    @Test
    fun `falls back to cookies and web storage when deleting browsing data is unsupported`() {
        every { WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) } returns true
        every { WebViewFeature.isFeatureSupported(WebViewFeature.DELETE_BROWSING_DATA) } returns false

        val removalDone = slot<ValueCallback<Boolean>>()
        every { cookieManager.removeAllCookies(capture(removalDone)) } just Runs

        clearPaywallProfileStorage()

        // Flushing before the removal reports back would persist the outgoing user's cookies.
        verify(exactly = 0) { cookieManager.flush() }
        removalDone.captured.onReceiveValue(true)
        verify { cookieManager.flush() }
        verify { webStorage.deleteAllData() }
        verify(exactly = 0) { WebStorageCompat.deleteBrowsingData(any(), any<Runnable>()) }
    }

    @Test
    fun `leaves storage alone when multi-profile is unsupported`() {
        every { WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) } returns false

        clearPaywallProfileStorage()

        verify(exactly = 0) { profileStore.getProfile(any()) }
    }

    @Test
    fun `does nothing when the profile was never created`() {
        every { WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) } returns true
        every { profileStore.getProfile(PAYWALL_PROFILE_NAME) } returns null

        clearPaywallProfileStorage()

        verify(exactly = 0) { WebStorageCompat.deleteBrowsingData(any(), any<Runnable>()) }
        verify(exactly = 0) { cookieManager.removeAllCookies(any()) }
    }

    @Test
    fun `does not throw when clearing storage fails`() {
        every { WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) } returns true
        every { profileStore.getProfile(any()) } throws IllegalStateException("profile deleted")

        clearPaywallProfileStorage()

        verify { profileStore.getProfile(PAYWALL_PROFILE_NAME) }
    }

    @Test
    fun `does not fail the render when profile setup throws`() {
        every { WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) } returns true
        every { profileStore.getOrCreateProfile(any()) } throws IllegalArgumentException("invalid profile name")

        webView.applyPaywallProfile()

        verify { profileStore.getOrCreateProfile(PAYWALL_PROFILE_NAME) }
    }
}
