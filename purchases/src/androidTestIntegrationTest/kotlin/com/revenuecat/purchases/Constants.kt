package com.revenuecat.purchases

object Constants {
    const val API_KEY = "REVENUECAT_API_KEY"
    const val PROXY_URL = "NO_PROXY_URL"
    const val USER_ID = "integrationTest"
    const val GOOGLE_PURCHASE_TOKEN = "GOOGLE_PURCHASE_TOKEN"
    const val PRODUCT_ID_TO_PURCHASE = "PRODUCT_ID_TO_PURCHASE"

    fun canRunIntegrationTests() = API_KEY != "REVENUECAT_API_KEY" &&
        GOOGLE_PURCHASE_TOKEN != "GOOGLE_PURCHASE_TOKEN" &&
        PRODUCT_ID_TO_PURCHASE != "PRODUCT_ID_TO_PURCHASE"
}
