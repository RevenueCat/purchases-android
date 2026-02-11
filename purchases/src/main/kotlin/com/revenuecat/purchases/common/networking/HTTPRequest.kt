package com.revenuecat.purchases.common.networking

import org.json.JSONObject
import java.net.URL

internal data class HTTPRequest(
    public val fullURL: URL,
    public val headers: Map<String, String>,
    public val body: JSONObject?,
) {
    public companion object {
        const val ETAG_HEADER_NAME = "X-RevenueCat-ETag"
        const val ETAG_LAST_REFRESH_NAME = "X-RC-Last-Refresh-Time"
        const val POST_PARAMS_HASH = "X-Post-Params-Hash"
    }
}
