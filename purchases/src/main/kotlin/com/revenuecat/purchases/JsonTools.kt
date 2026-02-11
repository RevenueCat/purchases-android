package com.revenuecat.purchases

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

internal object JsonTools {

    @OptIn(ExperimentalSerializationApi::class)
    public val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
}
