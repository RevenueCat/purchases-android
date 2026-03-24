package com.revenuecat.purchases.backend_integration_tests

object Constants {
    val apiKey: String = System.getProperty("REVENUECAT_API_KEY")?.takeIf { it.isNotEmpty() }
        ?: "REVENUECAT_API_KEY"
    val loadShedderApiKey: String = System.getProperty("LOAD_SHEDDER_API_KEY")?.takeIf { it.isNotEmpty() }
        ?: "LOAD_SHEDDER_API_KEY"
}
