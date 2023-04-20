package com.revenuecat.purchases.common.networking

import org.json.JSONObject
import java.net.URL

internal data class HTTPRequest(
    val fullURL: URL,
    val headers: Map<String, String>,
    val body: JSONObject?
) {
    companion object {
        const val ETAG_HEADER_NAME = "X-RevenueCat-ETag"
        const val ETAG_LAST_REFRESH_NAME = "X-RC-Last-Refresh-Time"
    }
}
