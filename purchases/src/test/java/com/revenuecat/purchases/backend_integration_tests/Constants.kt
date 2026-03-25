package com.revenuecat.purchases.backend_integration_tests

object Constants {
    val apiKey: String = System.getProperty("BACKEND_INTEGRATION_API_KEY")?.takeIf { it.isNotEmpty() }
        ?: "BACKEND_INTEGRATION_API_KEY"
    val loadShedderApiKey: String = System.getProperty("BACKEND_INTEGRATION_LOAD_SHEDDER_API_KEY")?.takeIf { it.isNotEmpty() }
        ?: "BACKEND_INTEGRATION_LOAD_SHEDDER_API_KEY"
}
