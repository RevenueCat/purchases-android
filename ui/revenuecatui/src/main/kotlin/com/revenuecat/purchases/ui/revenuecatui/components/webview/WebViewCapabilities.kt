@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.webkit.PermissionRequest
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.WebViewComponent

/**
 * Pure decision functions for [WebViewComponent.Capabilities] enforcement. Extracted here so the
 * logic can be unit tested without an Android `WebView`/`WebChromeClient`, and so it operates only
 * on immutable inputs — important because [isRequestAllowed] is consumed from
 * `WebViewClient.shouldInterceptRequest`, which runs on a background thread.
 */

/**
 * Returns whether a request to [host] is permitted given the configured [allowedDomains].
 *
 * - `null` [allowedDomains] means no restriction → always allowed.
 * - An empty list blocks everything (no domain can match).
 * - A host matches a domain on exact (case-insensitive) equality, or as a subdomain. The leading
 *   `.` separator in the suffix check guards against prefix collisions, e.g. `notexample.com` does
 *   not match the rule for `example.com`.
 */
@JvmSynthetic
internal fun isRequestAllowed(host: String, allowedDomains: List<String>?): Boolean {
    if (allowedDomains == null) return true
    return allowedDomains.any { domain ->
        host.equals(domain, ignoreCase = true) ||
            host.endsWith(".$domain", ignoreCase = true)
    }
}

/**
 * Filters the [requested] permission resources down to those granted by [capabilities]. Video
 * capture is granted only when `camera == true`, audio capture only when `microphone == true`, and
 * any unknown/unsupported resource is denied.
 */
@JvmSynthetic
@OptIn(InternalRevenueCatAPI::class)
internal fun grantedResources(
    requested: Array<String>,
    capabilities: WebViewComponent.Capabilities?,
): Array<String> =
    requested.filter { resource ->
        when (resource) {
            PermissionRequest.RESOURCE_VIDEO_CAPTURE -> capabilities?.camera == true
            PermissionRequest.RESOURCE_AUDIO_CAPTURE -> capabilities?.microphone == true
            else -> false
        }
    }.toTypedArray()

/**
 * Returns whether geolocation should be granted. Only `true` when [capabilities] explicitly enables
 * geolocation; `null` capabilities or a `null`/`false` flag default to denied.
 */
@JvmSynthetic
@OptIn(InternalRevenueCatAPI::class)
internal fun shouldGrantGeolocation(capabilities: WebViewComponent.Capabilities?): Boolean =
    capabilities?.geolocation == true
