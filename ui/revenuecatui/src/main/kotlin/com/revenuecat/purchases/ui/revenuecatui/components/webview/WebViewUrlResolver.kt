@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.net.Uri

internal object WebViewUrlResolver {

    fun resolve(url: String): String? {
        if (url.contains("{{")) return null

        val uri = Uri.parse(url)
        return url.takeIf {
            uri.scheme == HTTPS_SCHEME && !uri.host.isNullOrBlank()
        }
    }

    private const val HTTPS_SCHEME = "https"
}
