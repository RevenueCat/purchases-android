package com.revenuecat.purchases

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

internal object JsonTools {

    @OptIn(ExperimentalSerializationApi::class)
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
}
