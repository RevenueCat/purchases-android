package com.revenuecat.purchases

object Constants {
    const val API_KEY = "REVENUECAT_API_KEY"
    const val USER_ID = "integrationTest"
    const val GOOGLE_PURCHASE_TOKEN = "GOOGLE_PURCHASE_TOKEN"

    fun canRunIntegrationTests() = API_KEY != "REVENUECAT_API_KEY" && GOOGLE_PURCHASE_TOKEN != "GOOGLE_PURCHASE_TOKEN"
}
