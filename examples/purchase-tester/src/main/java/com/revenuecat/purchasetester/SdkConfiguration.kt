package com.revenuecat.purchasetester

enum class Store {
    GOOGLE,
    AMAZON,
    GALAXY,
    ;

    companion object {
        fun fromName(value: String?): Store? =
            value?.let { name ->
                values().firstOrNull { it.name == name }
            }
    }
}

data class SdkConfiguration(
    val apiKey: String,
    val proxyUrl: String?,
    val store: Store,
)
