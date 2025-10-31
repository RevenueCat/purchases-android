package com.revenuecat.purchases

object Constants {
    const val apiKey = "REVENUECAT_API_KEY"
    const val proxyUrl = "NO_PROXY_URL"
    const val googlePurchaseToken = "GOOGLE_PURCHASE_TOKEN"
    const val productIdToPurchase = "PRODUCT_ID_TO_PURCHASE"
    const val basePlanIdToPurchase = "BASE_PLAN_ID_TO_PURCHASE"

    // comma separated list of active entitlements to verify
    const val activeEntitlementIdsToVerify = "ACTIVE_ENTITLEMENT_IDS_TO_VERIFY"

    private const val backendEnvironmentString = "TEST_BACKEND_ENVIRONMENT_INTEGRATION_TESTS"
    val backendEnvironment: BackendEnvironment = BackendEnvironment.valueForString(backendEnvironmentString)

    enum class BackendEnvironment {
        PRODUCTION,
        LOAD_SHEDDER,
        LOAD_SHEDDER_US_EAST_2,
        ;

        companion object {
            fun valueForString(backendEnvironmentString: String): BackendEnvironment {
                return when (backendEnvironmentString) {
                    "loadshedder" -> LOAD_SHEDDER
                    "loadshedder-us-east-2" -> LOAD_SHEDDER_US_EAST_2
                    "production" -> PRODUCTION
                    else -> error("Expected valid backend_environment value. Got $backendEnvironmentString")
                }
            }
        }
    }
}
