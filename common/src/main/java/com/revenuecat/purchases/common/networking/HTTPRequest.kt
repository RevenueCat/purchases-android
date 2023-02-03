package com.revenuecat.purchases.common.networking

import org.json.JSONObject
import java.net.URL

internal data class HTTPRequest(
    val fullURL: URL,
    val headers: Map<String, String>,
    val body: JSONObject?,
    val gzipRequest: Boolean
)
