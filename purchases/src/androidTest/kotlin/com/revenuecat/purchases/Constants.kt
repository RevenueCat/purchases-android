package com.revenuecat.purchases

public object Constants {
    const val apiKey = "REVENUECAT_API_KEY"
    const val proxyUrl = "NO_PROXY_URL"
    const val googlePurchaseToken = "GOOGLE_PURCHASE_TOKEN"
    const val productIdToPurchase = "PRODUCT_ID_TO_PURCHASE"
    const val basePlanIdToPurchase = "BASE_PLAN_ID_TO_PURCHASE"

    // comma separated list of active entitlements to verify
    const val activeEntitlementIdsToVerify = "ACTIVE_ENTITLEMENT_IDS_TO_VERIFY"

    private const val backendEnvironmentString = "TEST_BACKEND_ENVIRONMENT_INTEGRATION_TESTS"
    public val backendEnvironment: BackendEnvironment = BackendEnvironment.valueForString(backendEnvironmentString)

    public enum class BackendEnvironment {
        PRODUCTION,
        LOAD_SHEDDER_US_EAST_1,
        LOAD_SHEDDER_US_EAST_2,
        ;

        companion object {
            public fun valueForString(backendEnvironmentString: String): BackendEnvironment {
                return when (backendEnvironmentString) {
                    "load_shedder_us_east_1" -> LOAD_SHEDDER_US_EAST_1
                    "load_shedder_us_east_2" -> LOAD_SHEDDER_US_EAST_2
                    "production" -> PRODUCTION
                    else -> error("Expected valid backend_environment value. Got $backendEnvironmentString")
                }
            }
        }
    }
}
