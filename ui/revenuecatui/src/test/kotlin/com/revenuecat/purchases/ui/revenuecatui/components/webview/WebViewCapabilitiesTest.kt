package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.webkit.PermissionRequest
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.WebViewComponent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(InternalRevenueCatAPI::class)
class WebViewCapabilitiesTest {

    // region isRequestAllowed

    @Test
    fun `isRequestAllowed allows everything when allowedDomains is null`() {
        assertThat(isRequestAllowed("anything.example.com", null)).isTrue()
        assertThat(isRequestAllowed("", null)).isTrue()
    }

    @Test
    fun `isRequestAllowed blocks everything when allowedDomains is empty`() {
        assertThat(isRequestAllowed("example.com", emptyList())).isFalse()
        assertThat(isRequestAllowed("api.example.com", emptyList())).isFalse()
    }

    @Test
    fun `isRequestAllowed allows exact host match`() {
        assertThat(isRequestAllowed("example.com", listOf("example.com"))).isTrue()
    }

    @Test
    fun `isRequestAllowed allows subdomain of an allowed domain`() {
        assertThat(isRequestAllowed("api.example.com", listOf("example.com"))).isTrue()
    }

    @Test
    fun `isRequestAllowed blocks a different domain`() {
        assertThat(isRequestAllowed("other.com", listOf("example.com"))).isFalse()
    }

    @Test
    fun `isRequestAllowed matches case-insensitively`() {
        assertThat(isRequestAllowed("API.EXAMPLE.COM", listOf("example.com"))).isTrue()
        assertThat(isRequestAllowed("example.com", listOf("EXAMPLE.COM"))).isTrue()
    }

    @Test
    fun `isRequestAllowed allows deep subdomains`() {
        assertThat(isRequestAllowed("a.b.example.com", listOf("example.com"))).isTrue()
    }

    @Test
    fun `isRequestAllowed does not match prefix collisions`() {
        // "notexample.com" must NOT match the rule for "example.com".
        assertThat(isRequestAllowed("notexample.com", listOf("example.com"))).isFalse()
    }

    // endregion isRequestAllowed

    // region grantedResources

    @Test
    fun `grantedResources grants video capture when camera is true`() {
        val result = grantedResources(
            arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE),
            capabilities(camera = true),
        )
        assertThat(result).containsExactly(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
    }

    @Test
    fun `grantedResources denies video capture when camera is false`() {
        val result = grantedResources(
            arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE),
            capabilities(camera = false),
        )
        assertThat(result).isEmpty()
    }

    @Test
    fun `grantedResources denies video capture when capabilities is null`() {
        val result = grantedResources(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE), null)
        assertThat(result).isEmpty()
    }

    @Test
    fun `grantedResources grants audio capture only when microphone is true`() {
        assertThat(
            grantedResources(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE), capabilities(microphone = true)),
        ).containsExactly(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
        assertThat(
            grantedResources(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE), capabilities(microphone = false)),
        ).isEmpty()
    }

    @Test
    fun `grantedResources grants both video and audio only when both enabled`() {
        val requested = arrayOf(
            PermissionRequest.RESOURCE_VIDEO_CAPTURE,
            PermissionRequest.RESOURCE_AUDIO_CAPTURE,
        )
        val result = grantedResources(requested, capabilities(camera = true, microphone = true))
        assertThat(result).containsExactlyInAnyOrder(
            PermissionRequest.RESOURCE_VIDEO_CAPTURE,
            PermissionRequest.RESOURCE_AUDIO_CAPTURE,
        )
    }

    @Test
    fun `grantedResources grants only audio when only microphone enabled`() {
        val requested = arrayOf(
            PermissionRequest.RESOURCE_VIDEO_CAPTURE,
            PermissionRequest.RESOURCE_AUDIO_CAPTURE,
        )
        val result = grantedResources(requested, capabilities(camera = false, microphone = true))
        assertThat(result).containsExactly(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
    }

    @Test
    fun `grantedResources always denies unknown resource types`() {
        val result = grantedResources(
            arrayOf("android.webkit.resource.MIDI_SYSEX"),
            capabilities(camera = true, microphone = true),
        )
        assertThat(result).isEmpty()
    }

    @Test
    fun `grantedResources returns empty for empty requested array`() {
        val result = grantedResources(emptyArray(), capabilities(camera = true, microphone = true))
        assertThat(result).isEmpty()
    }

    // endregion grantedResources

    // region shouldGrantGeolocation

    @Test
    fun `shouldGrantGeolocation is true only when geolocation is true`() {
        assertThat(shouldGrantGeolocation(capabilities(geolocation = true))).isTrue()
    }

    @Test
    fun `shouldGrantGeolocation is false when geolocation is false`() {
        assertThat(shouldGrantGeolocation(capabilities(geolocation = false))).isFalse()
    }

    @Test
    fun `shouldGrantGeolocation is false when geolocation is null`() {
        assertThat(shouldGrantGeolocation(capabilities(geolocation = null))).isFalse()
    }

    @Test
    fun `shouldGrantGeolocation is false when capabilities is null`() {
        assertThat(shouldGrantGeolocation(null)).isFalse()
    }

    // endregion shouldGrantGeolocation

    private fun capabilities(
        camera: Boolean? = null,
        microphone: Boolean? = null,
        geolocation: Boolean? = null,
    ) = WebViewComponent.Capabilities(
        camera = camera,
        microphone = microphone,
        geolocation = geolocation,
    )
}
